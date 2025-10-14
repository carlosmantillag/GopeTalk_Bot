package com.example.gopetalk_bot.voiceinteraction

import com.example.gopetalk_bot.network.ApiService
import com.example.gopetalk_bot.network.BackendResponse
import java.io.File
import java.io.IOException

class VoiceInteractionPresenter(private val view: VoiceInteractionContract.View) : VoiceInteractionContract.Presenter {

    private val apiService = ApiService()

    override fun start() {
        view.logInfo("Presenter started. Ready for hotword detection.")
    }

    override fun stop() {
        // No specific cleanup needed for the presenter itself anymore
    }

    override fun onHotwordDetected() {
        view.logInfo("Hotword detected! Starting command recording.")
        view.startCommandRecording()
    }

    override fun onSpeechEnded() {
        view.logInfo("Speech ended. Stopping command recording.")
        val audioFile = view.stopCommandRecording()
        if (audioFile != null) {
            onCommandAudioAvailable(audioFile)
        } else {
            view.logError("Could not get audio file to send.", null)
            view.startListeningForHotword()
        }
    }

    override fun onCommandAudioAvailable(audioFile: File) {
        view.logInfo("Sending audio file via ApiService.")
        apiService.sendAudioCommand(audioFile, object : ApiService.ApiCallback {
            override fun onSuccess(response: BackendResponse) {
                handleBackendResponse(response)
                audioFile.delete() // Clean up the audio file
                view.startListeningForHotword()
            }

            override fun onFailure(e: IOException) {
                view.logError("ApiService failed to send audio.", e)
                view.speak("Error de comunicación con el servidor.")
                audioFile.delete() // Clean up the audio file
                view.startListeningForHotword()
            }
        })
    }

    private fun handleBackendResponse(response: BackendResponse) {
        view.logInfo("Backend response: $response")
        var fullResponse = response.text
        if (response.channels.isNotEmpty()) {
            fullResponse += ". Los canales son: ${response.channels.joinToString(", ")}"
        }
        view.speak(fullResponse)
    }
}
