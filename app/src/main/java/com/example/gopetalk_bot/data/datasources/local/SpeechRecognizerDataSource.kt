package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Interface para el reconocedor de voz - permite testing sin Robolectric
 */
interface SpeechRecognizerWrapper {
    fun setRecognitionListener(listener: RecognitionListener)
    fun startListening(intent: Intent)
    fun stopListening()
    fun destroy()
}

/**
 * Implementación real del SpeechRecognizerWrapper
 */
class AndroidSpeechRecognizerWrapper(private val recognizer: SpeechRecognizer) : SpeechRecognizerWrapper {
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

/**
 * Factory para crear SpeechRecognizerWrapper e Intent
 */
interface SpeechRecognizerFactory {
    fun isRecognitionAvailable(context: Context): Boolean
    fun createRecognizer(context: Context): SpeechRecognizerWrapper
    fun createRecognitionIntent(): Intent
}

class AndroidSpeechRecognizerFactory : SpeechRecognizerFactory {
    override fun isRecognitionAvailable(context: Context): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    override fun createRecognizer(context: Context): SpeechRecognizerWrapper {
        return AndroidSpeechRecognizerWrapper(SpeechRecognizer.createSpeechRecognizer(context))
    }
    
    override fun createRecognitionIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
    }
}

class SpeechRecognizerDataSource(
    private val context: Context,
    private val factory: SpeechRecognizerFactory = AndroidSpeechRecognizerFactory()
) {
    private var speechRecognizer: SpeechRecognizerWrapper? = null
    private var isListening = false

    companion object {
        private const val TAG = "SpeechRecognizerDS"
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isListening) {
            return
        }

        if (!factory.isRecognitionAvailable(context)) {
            onError("Speech recognition not available")
            return
        }

        speechRecognizer = factory.createRecognizer(context)
        
        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                onError(errorMessage)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val recognizedText = matches[0]
                    onResult(recognizedText)
                } else {
                    onError("No results")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(recognitionListener)

        val intent = factory.createRecognitionIntent()
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        }
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }
}
