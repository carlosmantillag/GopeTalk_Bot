package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TextToSpeechDataSource(
    context: Context,
    private val onInitError: (String) -> Unit = {},
    private val ttsFactory: TtsEngineFactory = AndroidTtsEngineFactory()
) {
    private var ttsEngine: TtsEngine? = null
    private var isInitialized = false
    private val pendingText = mutableMapOf<String, String>()

    init {
        ttsEngine = ttsFactory.create(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsEngine?.setLanguage(Locale.forLanguageTag("es-MX"))
                isInitialized = true
                pendingText.forEach { (text, utteranceId) -> speak(text, utteranceId) }
                pendingText.clear()
            } else {
                onInitError("Failed to initialize TextToSpeech.")
            }
        }
    }

    fun setUtteranceProgressListener(listener: UtteranceProgressListener) {
        ttsEngine?.setOnUtteranceProgressListener(listener)
    }

    fun speak(text: String, utteranceId: String) {
        if (isInitialized) {
            ttsEngine?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
        } else {
            pendingText[text] = utteranceId
        }
    }

    fun shutdown() {
        ttsEngine?.stop()
        ttsEngine?.shutdown()
    }

    fun isInitialized(): Boolean = isInitialized
}
