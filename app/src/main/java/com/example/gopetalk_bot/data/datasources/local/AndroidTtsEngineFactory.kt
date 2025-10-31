package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.speech.tts.TextToSpeech

class AndroidTtsEngineFactory : TtsEngineFactory {
    override fun create(context: Context, onInit: (Int) -> Unit): TtsEngine {
        val tts = TextToSpeech(context, onInit)
        return AndroidTtsEngine(tts)
    }
}
