package com.example.gopetalk_bot.voiceinteraction

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.UtteranceProgressListener
import com.example.gopetalk_bot.network.BackendResponse
import java.util.*

class VoiceInteractionPresenter(private val view: VoiceInteractionContract.View) : VoiceInteractionContract.Presenter, RecognitionListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var ttsManager: TextToSpeechManager


    override fun start(ttsManager: TextToSpeechManager) {
        this.ttsManager = ttsManager
        view.logInfo("Presenter started.")
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(view.context)
        speechRecognizer.setRecognitionListener(this)

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")

        this.ttsManager.setUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                // Do not restart listening here, it will be restarted in onEndOfSpeech or onResults
            }

            override fun onError(utteranceId: String?) {
                // Do not restart listening here, it will be restarted in onError of RecognitionListener
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
        speechRecognizer.startListening(recognizerIntent) // Restart listening after results
    }

    private fun handleCommand(commandText: String) {
        val simulatedResponse = when {
            commandText.contains("lista de canales") -> BackendResponse(
                text = "la lista de canales es: canal 1, canal 2",
                action = "list_channels",
                channels = listOf("canal 1", "canal 2")
            )
            commandText.contains("lista de usuarios") -> BackendResponse(
                text = "la lista de usuarios es: usuario 1, usuario 2",
                action = "list_users",
                users = listOf("usuario 1", "usuario 2")
            )
            commandText.contains("conectar al canal general") -> BackendResponse(
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
        if (response.action == "list_channels") {
            fullResponse = "La lista de canales es: ${response.channels.joinToString(", ")}"
        } else if (response.action == "list_users") {
            fullResponse = "La lista de usuarios es: ${response.users.joinToString(", ")}"
        } else if (response.action == "connect_to_channel") {
            fullResponse = "Los canales son: ${response.channels.joinToString(", ")}"
        }
        val utteranceId = UUID.randomUUID().toString()
        view.speak(fullResponse, utteranceId)
    }

    // RecognitionListener methods
    override fun onReadyForSpeech(params: Bundle?) {
        view.logInfo("Ready for speech.")
    }

    override fun onBeginningOfSpeech() {
        view.logInfo("Beginning of speech.")
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        view.logInfo("End of speech.")
    }

    override fun onError(error: Int) {
        view.logError("Speech recognizer error: $error", null)
        speechRecognizer.startListening(recognizerIntent) // Restart listening after error
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
