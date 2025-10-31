package com.example.gopetalk_bot.data.datasources.local

import android.os.Bundle
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

interface TtsEngine {
    fun speak(text: String, queueMode: Int, params: Bundle?, utteranceId: String): Int
    fun stop(): Int
    fun shutdown()
    fun setLanguage(locale: Locale): Int
    fun setOnUtteranceProgressListener(listener: UtteranceProgressListener?)
}
