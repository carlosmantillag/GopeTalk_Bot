package com.example.gopetalk_bot.voiceinteraction

import java.io.File

interface VoiceInteractionContract {
    interface View {
        fun speak(text: String)
        fun startListeningForHotword()
        fun startCommandRecording()
        fun stopCommandRecording(): File?
        fun logInfo(message: String)
        fun logError(message: String, t: Throwable? = null)
        fun onRmsChanged(rmsDb: Float)
    }

    interface Presenter {
        fun start()
        fun stop()
        fun onHotwordDetected()
        fun onSpeechEnded()
        fun onCommandAudioAvailable(audioFile: File)
    }
}