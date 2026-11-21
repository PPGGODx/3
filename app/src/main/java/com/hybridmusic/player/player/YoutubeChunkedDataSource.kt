package com.hybridmusic.player.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import java.io.IOException
import kotlin.math.min

@OptIn(UnstableApi::class)
class YoutubeChunkedDataSource(
    private val upstream: HttpDataSource
) : DataSource {

    companion object {
        private const val CHUNK_SIZE_BYTES = 262_144L
        private const val LENGTH_UNSET = C.LENGTH_UNSET.toLong()
    }

    private var rootSpec: DataSpec? = null
    private var currentPosition: Long = 0L
    private var totalRemaining: Long = LENGTH_UNSET
    private var chunkRemaining: Long = 0L
    private var chunkOpened: Boolean = false
    private var serverSupportsRange: Boolean = false
    private var resolvedUri: Uri? = null

    class Factory(
        private val upstreamFactory: HttpDataSource.Factory
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return YoutubeChunkedDataSource(upstreamFactory.createDataSource())
        }
    }

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        rootSpec = dataSpec
        currentPosition = dataSpec.position
        totalRemaining = dataSpec.length
        resolvedUri = dataSpec.uri

        val isGoogleVideo = dataSpec.uri.host?.contains("googlevideo.com") == true
        if (!isGoogleVideo) {
            val len = upstream.open(dataSpec)
            chunkOpened = true
            chunkRemaining = len
            totalRemaining = len
            serverSupportsRange = false
            resolvedUri = upstream.uri ?: dataSpec.uri
            return len
        }

        openNextChunk()
        return totalRemaining
    }

    @Throws(IOException::class)
    private fun openNextChunk() {
        if (chunkOpened) {
            runCatching { upstream.close() }
            chunkOpened = false
        }

        val base = rootSpec ?: throw IOException("DataSpec is null")
        val requestLen = if (totalRemaining == LENGTH_UNSET) {
            CHUNK_SIZE_BYTES
        } else {
            min(totalRemaining, CHUNK_SIZE_BYTES)
        }

        if (requestLen <= 0L) {
            chunkRemaining = 0L
            return
        }

        val chunkSpec = base.buildUpon()
            .setPosition(currentPosition)
            .setLength(requestLen)
            .build()

        val openedLen = upstream.open(chunkSpec)
        chunkOpened = true
        chunkRemaining = openedLen
        resolvedUri = upstream.uri ?: base.uri

        val contentRange = upstream.responseHeaders["Content-Range"]?.firstOrNull()
            ?: upstream.responseHeaders["content-range"]?.firstOrNull()
        serverSupportsRange = contentRange != null

        if (totalRemaining == LENGTH_UNSET) {
            val totalSize = contentRange?.substringAfterLast('/')?.trim()?.toLongOrNull()
            if (totalSize != null && totalSize > currentPosition) {
                totalRemaining = totalSize - currentPosition
            } else if (!serverSupportsRange && openedLen != LENGTH_UNSET) {
                totalRemaining = openedLen
            }
        }
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (totalRemaining == 0L) return C.RESULT_END_OF_INPUT

        val base = rootSpec ?: return C.RESULT_END_OF_INPUT
        if (base.uri.host?.contains("googlevideo.com") != true) {
            return upstream.read(buffer, offset, length)
        }

        if (!chunkOpened || chunkRemaining == 0L) {
            if (totalRemaining == 0L) return C.RESULT_END_OF_INPUT
            openNextChunk()
            if (chunkRemaining == 0L) return C.RESULT_END_OF_INPUT
        }

        val toRead = if (chunkRemaining != LENGTH_UNSET) {
            min(length.toLong(), chunkRemaining).toInt()
        } else {
            length
        }

        val bytesRead = upstream.read(buffer, offset, toRead)
        if (bytesRead == C.RESULT_END_OF_INPUT) {
            if (!serverSupportsRange) {
                totalRemaining = 0L
                return C.RESULT_END_OF_INPUT
            }
            if (totalRemaining == LENGTH_UNSET || totalRemaining > 0L) {
                openNextChunk()
                if (chunkRemaining == 0L) return C.RESULT_END_OF_INPUT
                return read(buffer, offset, length)
            }
            return C.RESULT_END_OF_INPUT
        }

        currentPosition += bytesRead
        if (chunkRemaining != LENGTH_UNSET) {
            chunkRemaining -= bytesRead
        }
        if (totalRemaining != LENGTH_UNSET) {
            totalRemaining = (totalRemaining - bytesRead).coerceAtLeast(0L)
        }
        return bytesRead
    }

    override fun getUri(): Uri? = resolvedUri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    @Throws(IOException::class)
    override fun close() {
        resolvedUri = null
        rootSpec = null
        if (chunkOpened) {
            chunkOpened = false
            upstream.close()
        }
    }
}
