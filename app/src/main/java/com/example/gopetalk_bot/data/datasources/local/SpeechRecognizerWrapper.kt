package com.example.gopetalk_bot.data.datasources.local

import android.content.Intent
import android.speech.RecognitionListener

interface SpeechRecognizerWrapper {
    fun setRecognitionListener(listener: RecognitionListener)
    fun startListening(intent: Intent)
    fun stopListening()
    fun destroy()
}
