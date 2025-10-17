package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TextToSpeechDataSource(
    context: Context,
    private val onInitError: (String) -> Unit
) {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private val pendingText = mutableMapOf<String, String>()

    init {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.forLanguageTag("es-MX")
                isInitialized = true
                pendingText.forEach { (text, utteranceId) -> speak(text, utteranceId) }
                pendingText.clear()
            } else {
                onInitError("Failed to initialize TextToSpeech.")
            }
        }
    }

    fun setUtteranceProgressListener(listener: UtteranceProgressListener) {
        textToSpeech?.setOnUtteranceProgressListener(listener)
    }

    fun speak(text: String, utteranceId: String) {
        if (isInitialized) {
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
        } else {
            pendingText[text] = utteranceId
        }
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    fun isInitialized(): Boolean = isInitialized
}
