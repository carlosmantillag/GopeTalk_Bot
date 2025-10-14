package com.example.gopetalk_bot.voiceinteraction

import android.content.Context

interface VoiceInteractionContract {
    interface View {
        val context: Context
        fun speak(text: String, utteranceId: String)
        fun logInfo(message: String)
        fun logError(message: String, t: Throwable? = null)
    }

    interface Presenter {
        fun start(ttsManager: TextToSpeechManager)
        fun stop()
        fun onHotwordDetected()
    }
}
