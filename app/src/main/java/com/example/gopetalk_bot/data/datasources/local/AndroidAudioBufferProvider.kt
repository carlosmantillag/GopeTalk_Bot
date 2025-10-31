package com.example.gopetalk_bot.data.datasources.local

import android.media.AudioRecord

class AndroidAudioBufferProvider : AudioBufferProvider {
    override fun getMinBufferSize(sampleRate: Int, channelConfig: Int, audioFormat: Int): Int {
        return AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    }
}
