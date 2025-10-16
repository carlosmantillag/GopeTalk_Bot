package com.example.gopetalk_bot.presentation.voiceinteraction

import android.content.Context

/**
 * Contract for Voice Interaction screen (MVP pattern)
 */
interface VoiceInteractionContract {
    interface View {
        val context: Context
        fun logInfo(message: String)
        fun logError(message: String, t: Throwable? = null)
    }

    interface Presenter {
        fun start()
        fun stop()
    }
}
