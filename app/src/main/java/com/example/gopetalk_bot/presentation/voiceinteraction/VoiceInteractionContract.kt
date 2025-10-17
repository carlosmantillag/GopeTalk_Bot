package com.example.gopetalk_bot.presentation.voiceinteraction

import android.content.Context

interface VoiceInteractionContract {
    interface View {
        val context: Context
        fun logInfo(message: String)
        fun logError(message: String, t: Throwable? = null)
    }

    interface Presenter {
        fun start()
        fun stop()
        fun speakWelcome(username: String)
        fun updateChannel(channel: String?)
    }
}
