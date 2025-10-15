package com.example.gopetalk_bot.voiceinteraction

import android.os.Handler
import android.os.Looper
import android.speech.tts.UtteranceProgressListener
import com.example.gopetalk_bot.main.AudioRmsMonitor
import com.example.gopetalk_bot.network.ApiService
import com.example.gopetalk_bot.network.BackendResponse
import java.io.File
import java.io.IOException
import java.util.UUID

class VoiceInteractionPresenter(private val view: VoiceInteractionContract.View) : VoiceInteractionContract.Presenter {

    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var audioRecordingManager: AudioRecordingManager
    private lateinit var apiService: ApiService
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    override fun start(ttsManager: TextToSpeechManager) {
        this.ttsManager = ttsManager
        view.logInfo("Presenter started.")

        apiService = ApiService()
        audioRecordingManager = AudioRecordingManager(
            context = view.context,
            onRecordingStopped = { audioFile ->
                sendAudioFileToBackend(audioFile)
            },
            logInfo = { message -> view.logInfo(message) },
            logError = { message, throwable -> view.logError(message, throwable) }
        )

        audioRecordingManager.startMonitoring()

        this.ttsManager.setUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainThreadHandler.post {
                    AudioRmsMonitor.updateRmsDb(10f) // Simulate sound for visual feedback
                }
            }

            override fun onDone(utteranceId: String?) {
                mainThreadHandler.post {
                    AudioRmsMonitor.updateRmsDb(0f)
                }
            }

            override fun onError(utteranceId: String?) {
                mainThreadHandler.post {
                    AudioRmsMonitor.updateRmsDb(0f)
                }
            }
        })
    }

    private fun sendAudioFileToBackend(audioFile: File) {
        view.logInfo("Sending audio file to backend: ${audioFile.path}")
        apiService.sendAudioCommand(audioFile, object : ApiService.ApiCallback {
            override fun onSuccess(response: BackendResponse) {
                mainThreadHandler.post { handleBackendResponse(response) }
            }

            override fun onFailure(e: IOException) {
                mainThreadHandler.post { view.logError("API Error", e) }
            }
        })
    }

    override fun stop() {
        audioRecordingManager.release()
        ttsManager.shutdown()
        view.logInfo("Presenter stopped.")
    }

    override fun onHotwordDetected() {
        // This might be used in the future to trigger a specific action,
        // but for now, the continuous listening handles everything.
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
}