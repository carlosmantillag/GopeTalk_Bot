package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.content.Intent
import android.speech.SpeechRecognizer

interface SpeechRecognizerFactory {
    fun isRecognitionAvailable(context: Context): Boolean
    fun createRecognizer(context: Context): SpeechRecognizerWrapper
    fun createRecognitionIntent(): Intent
}
