package com.example.gopetalk_bot.data.datasources.local

interface AudioBufferProvider {
    fun getMinBufferSize(sampleRate: Int, channelConfig: Int, audioFormat: Int): Int
}
