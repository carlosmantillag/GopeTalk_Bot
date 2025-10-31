package com.example.gopetalk_bot.data.datasources.remote

interface MainThreadExecutor {
    fun post(runnable: Runnable)
}
