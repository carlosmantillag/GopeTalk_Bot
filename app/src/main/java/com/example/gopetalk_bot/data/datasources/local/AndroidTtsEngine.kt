package com.example.gopetalk_bot.data.datasources.local

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AndroidTtsEngine(private val tts: TextToSpeech) : TtsEngine {
    override fun speak(text: String, queueMode: Int, params: Bundle?, utteranceId: String): Int {
        return tts.speak(text, queueMode, params, utteranceId)
    }

    override fun stop(): Int = tts.stop()

    override fun shutdown() = tts.shutdown()

    override fun setLanguage(locale: Locale): Int {
        return tts.setLanguage(locale)
    }

    override fun setOnUtteranceProgressListener(listener: UtteranceProgressListener?) {
        tts.setOnUtteranceProgressListener(listener)
    }
}
