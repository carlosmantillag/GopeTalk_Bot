package com.example.gopetalk_bot.voiceinteraction

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.UtteranceProgressListener
import com.example.gopetalk_bot.main.AudioRmsMonitor
import com.example.gopetalk_bot.network.BackendResponse
import java.util.*

class VoiceInteractionPresenter(private val view: VoiceInteractionContract.View) : VoiceInteractionContract.Presenter, RecognitionListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var ttsManager: TextToSpeechManager
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    override fun start(ttsManager: TextToSpeechManager) {
        this.ttsManager = ttsManager
        view.logInfo("Presenter started.")
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(view.context)
        speechRecognizer.setRecognitionListener(this)

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")

        this.ttsManager.setUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainThreadHandler.post {
                    AudioRmsMonitor.updateRmsDb(10f)
                }
            }

            override fun onDone(utteranceId: String?) {
                mainThreadHandler.post {
                    AudioRmsMonitor.updateRmsDb(0f)
                    speechRecognizer.startListening(recognizerIntent)
                }
            }

            override fun onError(utteranceId: String?) {
                mainThreadHandler.post {
                    AudioRmsMonitor.updateRmsDb(0f)
                    speechRecognizer.startListening(recognizerIntent)
                }
            }
        })
    }

    override fun stop() {
        speechRecognizer.destroy()
    }

    override fun onHotwordDetected() {
        speechRecognizer.startListening(recognizerIntent)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null && matches.isNotEmpty()) {
            val commandText = matches[0]
            view.logInfo("Recognized command: $commandText")
            handleCommand(commandText)
        }
    }

    private fun handleCommand(commandText: String) {
        val simulatedResponse = when {
            Regex("lista.*canales", RegexOption.IGNORE_CASE).containsMatchIn(commandText) -> BackendResponse(
                text = "la lista de canales es: canal 1, canal 2",
                action = "list_channels",
                channels = listOf("canal 1", "canal 2")
            )
            Regex("lista.*usuarios", RegexOption.IGNORE_CASE).containsMatchIn(commandText) -> BackendResponse(
                text = "la lista de usuarios es: usuario 1, usuario 2",
                action = "list_users",
                users = listOf("usuario 1", "usuario 2")
            )
            Regex("conectar.*canal.*general", RegexOption.IGNORE_CASE).containsMatchIn(commandText) -> BackendResponse(
                text = "Conectando al canal general",
                action = "connect_to_channel"
            )
            else -> BackendResponse(text = "No te entiendo")
        }
        handleBackendResponse(simulatedResponse)
    }

    private fun handleBackendResponse(response: BackendResponse) {
        view.logInfo("Backend response: $response")
        var fullResponse = response.text
        when (response.action) {
            "list_channels" -> {
                fullResponse = "La lista de canales es: ${response.channels.joinToString(", ")}"
            }
            "list_users" -> {
                fullResponse = "La lista de usuarios es: ${response.users.joinToString(", ")}"
            }
            "connect_to_channel" -> {
                fullResponse = response.text
            }
        }
        val utteranceId = UUID.randomUUID().toString()
        view.speak(fullResponse, utteranceId)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        view.logInfo("Ready for speech.")
    }

    override fun onBeginningOfSpeech() {
        view.logInfo("Beginning of speech.")
    }

    override fun onRmsChanged(rmsdB: Float) {
        AudioRmsMonitor.updateRmsDb(rmsdB)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        view.logInfo("End of speech.")
        AudioRmsMonitor.updateRmsDb(0f)
    }

    override fun onError(error: Int) {
        view.logError("Speech recognizer error: $error", null)
        AudioRmsMonitor.updateRmsDb(0f)
        speechRecognizer.startListening(recognizerIntent)
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
