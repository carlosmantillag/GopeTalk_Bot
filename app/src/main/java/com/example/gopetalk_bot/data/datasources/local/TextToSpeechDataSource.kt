package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Interface para el motor TTS - permite testing sin Robolectric
 */
interface TtsEngine {
    fun speak(text: String, queueMode: Int, params: Bundle?, utteranceId: String): Int
    fun stop(): Int
    fun shutdown()
    fun setLanguage(locale: Locale): Int
    fun setOnUtteranceProgressListener(listener: UtteranceProgressListener?)
}

/**
 * Implementación real del TtsEngine usando TextToSpeech de Android
 */
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

/**
 * Factory para crear TtsEngine - permite inyectar mocks en tests
 */
interface TtsEngineFactory {
    fun create(context: Context, onInit: (Int) -> Unit): TtsEngine
}

class AndroidTtsEngineFactory : TtsEngineFactory {
    override fun create(context: Context, onInit: (Int) -> Unit): TtsEngine {
        val tts = TextToSpeech(context, onInit)
        return AndroidTtsEngine(tts)
    }
}

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
