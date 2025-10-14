package com.example.gopetalk_bot.voiceinteraction

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechRecognizerManager(
    private val context: Context,
    private val listener: SpeechRecognitionListener
) {

    private var speechRecognizer: SpeechRecognizer? = null

    interface SpeechRecognitionListener {
        fun onHotwordDetected()
        fun onSpeechEnded()
        fun onError(error: Int)
        fun onRmsChanged(rmsdB: Float)
    }

    companion object {
        private const val HOTWORD = "gopebot"
    }

    fun create() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY) // Or a custom error
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                private var isHotwordDetected = false

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    listener.onRmsChanged(rmsdB)
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    if (!isHotwordDetected && text.lowercase(Locale.getDefault()).contains(HOTWORD)) {
                        isHotwordDetected = true
                        listener.onHotwordDetected()
                    }
                }

                override fun onEndOfSpeech() {
                    if (isHotwordDetected) {
                        listener.onSpeechEnded()
                    }
                }

                override fun onResults(results: Bundle?) {
                    isHotwordDetected = false
                    startListening()
                }

                override fun onError(error: Int) {
                    if (error != SpeechRecognizer.ERROR_CLIENT && error != SpeechRecognizer.ERROR_NO_MATCH) {
                        listener.onError(error)
                    }
                    isHotwordDetected = false
                    startListening()
                }
            })
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            // Log error
        }
    }

    fun destroy() {
        speechRecognizer?.destroy()
    }
}
