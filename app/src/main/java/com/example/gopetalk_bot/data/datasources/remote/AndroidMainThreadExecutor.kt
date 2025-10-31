package com.example.gopetalk_bot.data.datasources.remote

import android.os.Handler
import android.os.Looper

class AndroidMainThreadExecutor : MainThreadExecutor {
    private val handler = Handler(Looper.getMainLooper())

    override fun post(runnable: Runnable) {
        handler.post(runnable)
    }
}
