package com.example.gopetalk_bot.voiceinteraction

import com.example.gopetalk_bot.network.ApiService
import org.json.JSONObject
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
            view.logError("Could not get audio file to send.")
            view.startListeningForHotword()
        }
    }

    override fun onCommandAudioAvailable(audioFile: File) {
        view.logInfo("Sending audio file via ApiService.")
        apiService.sendAudioCommand(audioFile, object : ApiService.ApiCallback {
            override fun onSuccess(response: String) {
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

    private fun handleBackendResponse(responseBody: String) {
        try {
            view.logInfo("Backend response: $responseBody")
            val jsonResponse = JSONObject(responseBody)
            val responseText = jsonResponse.optString("text", "No se recibió texto.")
            val channels = jsonResponse.optJSONArray("channels")

            var fullResponse = responseText
            if (channels != null && channels.length() > 0) {
                val channelList = (0 until channels.length()).map { channels.getString(it) }
                // TODO: Display channels in the UI instead of just speaking them.
                fullResponse += ". Los canales son: ${channelList.joinToString(", ")}"
            }

            view.speak(fullResponse)
        } catch (e: Exception) {
            view.logError("Error parsing backend response.", e)
            view.speak("Recibí una respuesta inválida del servidor.")
        }
    }
}
