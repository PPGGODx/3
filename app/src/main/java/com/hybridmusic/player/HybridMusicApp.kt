package com.hybridmusic.player

import android.app.Application
import com.hybridmusic.player.di.AppContainer
import java.io.File

class HybridMusicApp : Application() {
    val container: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                File(filesDir, "last_crash.txt").writeText(throwable.stackTraceToString())
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
