package com.example.gopetalk_bot.data.datasources.local

import android.content.Intent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer

class AndroidSpeechRecognizerWrapper(
    private val recognizer: SpeechRecognizer
) : SpeechRecognizerWrapper {
    override fun setRecognitionListener(listener: RecognitionListener) {
        recognizer.setRecognitionListener(listener)
    }

    override fun startListening(intent: Intent) {
        recognizer.startListening(intent)
    }

    override fun stopListening() {
        recognizer.stopListening()
    }

    override fun destroy() {
        recognizer.destroy()
    }
}
