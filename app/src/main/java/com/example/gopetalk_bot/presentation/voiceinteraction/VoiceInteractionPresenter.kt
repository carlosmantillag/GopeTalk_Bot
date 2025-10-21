package com.example.gopetalk_bot.presentation.voiceinteraction

import android.os.Handler
import android.os.Looper
import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.data.datasources.remote.dto.BackendResponse
import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.repositories.AudioPlayerRepository
import com.example.gopetalk_bot.domain.repositories.WebSocketRepository
import com.example.gopetalk_bot.domain.usecases.*
import com.example.gopetalk_bot.presentation.common.AudioRmsMonitor
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID

class VoiceInteractionPresenter(
    private val view: VoiceInteractionContract.View,
    private val startAudioMonitoringUseCase: StartAudioMonitoringUseCase,
    private val stopAudioMonitoringUseCase: StopAudioMonitoringUseCase,
    private val pauseAudioRecordingUseCase: PauseAudioRecordingUseCase,
    private val resumeAudioRecordingUseCase: ResumeAudioRecordingUseCase,
    private val monitorAudioLevelUseCase: MonitorAudioLevelUseCase,
    private val getRecordedAudioUseCase: GetRecordedAudioUseCase,
    private val sendAudioCommandUseCase: SendAudioCommandUseCase,
    private val speakTextUseCase: SpeakTextUseCase,
    private val setTtsListenerUseCase: SetTtsListenerUseCase,
    private val shutdownTtsUseCase: ShutdownTtsUseCase,
    private val connectWebSocketUseCase: ConnectWebSocketUseCase,
    private val disconnectWebSocketUseCase: DisconnectWebSocketUseCase,
    private val playAudioFileUseCase: PlayAudioFileUseCase,
    private val updateWebSocketChannelUseCase: UpdateWebSocketChannelUseCase,
    private val pollAudioUseCase: PollAudioUseCase,
    private val userPreferences: UserPreferences
) : VoiceInteractionContract.Presenter {

    private companion object {
        const val WEBSOCKET_URL = "ws://159.223.150.185/ws"
        const val POLLING_INTERVAL_MS = 2000L
        const val WAITING_MESSAGE_DELAY_MS = 3000L
        const val WAITING_MESSAGE = "Trayendo tu respuesta, espera"
        const val STATUS_CODE_NO_CONTENT = 204
    }

    private val presenterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private val gson = Gson()
    
    @Volatile
    private var isMicrophoneBlocked = false
    
    @Volatile
    private var currentChannel: String? = null
    
    private var pollingJob: Job? = null
    private var waitingMessageJob: Job? = null

    override fun start() {
        view.logInfo("Presenter started.")
        connectToWebSocket()
        setupTtsListeners()
        startAudioMonitoring()
        startAudioPolling()
    }

    private fun setupTtsListeners() {
        setTtsListenerUseCase.execute(
            onStart = { pauseAudioRecordingUseCase.execute() },
            onDone = { resumeAudioIfNotBlocked() },
            onError = { resumeAudioIfNotBlocked() }
        )
    }

    private fun resumeAudioIfNotBlocked() {
        if (!isMicrophoneBlocked) {
            resumeAudioRecordingUseCase.execute()
        }
    }

    private fun startAudioMonitoring() {
        startAudioMonitoringUseCase.execute()
        
        presenterScope.launch {
            monitorAudioLevelUseCase.execute().collect { audioLevel ->
                AudioRmsMonitor.updateRmsDb(audioLevel.rmsDb)
            }
        }
        
        presenterScope.launch {
            getRecordedAudioUseCase.execute().collect { audioData ->
                sendAudioToBackend(audioData)
            }
        }
    }

    private fun sendAudioToBackend(audioData: AudioData) {
        scheduleWaitingMessage()
        
        sendAudioCommandUseCase.execute(audioData) { response ->
            cancelWaitingMessage()
            mainThreadHandler.post {
                handleApiResponse(response)
            }
        }
    }

    private fun handleApiResponse(response: ApiResponse) {
        when (response) {
            is ApiResponse.Success -> handleSuccessResponse(response)
            is ApiResponse.Error -> view.logError("API Error: ${response.message}", response.exception)
        }
    }

    private fun handleSuccessResponse(response: ApiResponse.Success) {
        response.audioFile?.let {
            playReceivedAudioFile(it)
            return
        }
        
        if (response.statusCode == STATUS_CODE_NO_CONTENT) return
        
        val trimmedBody = response.body.trim()
        if (trimmedBody.isNotBlank()) {
            processResponseBody(trimmedBody)
        }
    }

    private fun processResponseBody(body: String) {
        if (isJsonResponse(body)) {
            parseAndHandleJsonResponse(body)
        } else {
            speak(body)
        }
    }

    private fun isJsonResponse(body: String): Boolean {
        return body.startsWith("{") && body.endsWith("}")
    }

    private fun parseAndHandleJsonResponse(body: String) {
        try {
            val backendResponse = gson.fromJson(body, BackendResponse::class.java)
            handleBackendResponse(backendResponse)
        } catch (e: Exception) {
            view.logError("Failed to parse backend JSON response", e)
            speak("No entendí la respuesta del servidor.")
        }
    }

    private fun speak(text: String) {
        speakTextUseCase.execute(text, UUID.randomUUID().toString())
    }
    
    private fun scheduleWaitingMessage() {
        waitingMessageJob?.cancel()
        waitingMessageJob = presenterScope.launch {
            delay(WAITING_MESSAGE_DELAY_MS)
            while (isActive) {
                speak(WAITING_MESSAGE)
                delay(WAITING_MESSAGE_DELAY_MS)
            }
        }
    }
    
    private fun cancelWaitingMessage() {
        waitingMessageJob?.cancel()
        waitingMessageJob = null
    }

    private fun handleBackendResponse(response: BackendResponse) {
        updateChannelIfChanged(response.channel)
        
        val responseText = response.text.ifBlank {
            getActionResponseText(response)
        }

        if (responseText.isNotBlank()) {
            speak(responseText)
        }
    }

    private fun updateChannelIfChanged(newChannel: String?) {
        if (!newChannel.isNullOrBlank() && newChannel != currentChannel) {
            updateChannel(newChannel)
        }
    }

    private fun getActionResponseText(response: BackendResponse): String {
        return when (response.action) {
            "list_channels" -> "La lista de canales es: ${response.channels.joinToString(", ")}"
            "list_users" -> "La lista de usuarios es: ${response.users.joinToString(", ")}"
            "logout" -> {
                handleLogout()
                "Cerrando sesión, hasta luego"
            }
            else -> "Acción no reconocida"
        }
    }
    
    private fun handleLogout() {
        view.logInfo("Logout requested")
        mainThreadHandler.postDelayed({
            view.logout()
        }, 2000) // Esperar 2 segundos para que termine de hablar
    }
    
    private fun connectToWebSocket() {
        val authToken = userPreferences.authToken
        view.logInfo("Connecting to WebSocket: $WEBSOCKET_URL")
        
        connectWebSocketUseCase.execute(WEBSOCKET_URL, authToken, currentChannel, createWebSocketListener())
    }

    private fun createWebSocketListener() = object : WebSocketRepository.MicrophoneControlListener {
        override fun onMicrophoneStart() {
            view.logInfo("WebSocket: Microphone START")
            isMicrophoneBlocked = false
            resumeAudioRecordingUseCase.execute()
        }
        
        override fun onMicrophoneStop() {
            view.logInfo("WebSocket: Microphone STOP")
            isMicrophoneBlocked = true
            pauseAudioRecordingUseCase.execute()
        }
        
        override fun onConnectionEstablished() {
            view.logInfo("WebSocket connection established")
        }
        
        override fun onConnectionClosed() {
            view.logInfo("WebSocket connection closed")
        }
        
        override fun onError(error: String) {
            view.logError("WebSocket error: $error", null)
        }
    }
    
    private fun playReceivedAudioFile(audioFile: File) {
        playAudioFileUseCase.execute(audioFile, createPlaybackListener(audioFile))
    }

    private fun createPlaybackListener(audioFile: File) = object : AudioPlayerRepository.PlaybackListener {
        override fun onPlaybackStarted() {
            view.logInfo("Audio playback started")
        }
        
        override fun onPlaybackCompleted() {
            view.logInfo("Audio playback completed")
            audioFile.delete()
        }
        
        override fun onPlaybackError(error: String) {
            view.logError("Audio playback error: $error", null)
            audioFile.delete()
        }
    }
    
    override fun updateChannel(channel: String?) {
        currentChannel = channel
        view.logInfo("Channel updated to: $channel")
        updateWebSocketChannelUseCase.execute(userPreferences.authToken, channel)
    }

    override fun stop() {
        stopAudioPolling()
        disconnectWebSocketUseCase.execute()
        stopAudioMonitoringUseCase.execute()
        shutdownTtsUseCase.execute()
        presenterScope.cancel()
        view.logInfo("Presenter stopped.")
    }

    override fun speakWelcome(username: String) {
        speak("Bienvenido $username, indícame el canal al que te quieras unir")
    }
    
    private fun startAudioPolling() {
        view.logInfo("Starting audio polling")
        
        pollingJob = presenterScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    pollAudioUseCase.execute(
                        onAudioReceived = { audioFile, fromUserId, channel ->
                            mainThreadHandler.post {
                                view.logInfo("Audio received from user $fromUserId in channel $channel")
                                playReceivedAudioFile(audioFile)
                            }
                        },
                        onNoAudio = {},
                        onError = { error ->
                            mainThreadHandler.post {
                                view.logError("Polling error: $error", null)
                            }
                        }
                    )
                } catch (e: Exception) {
                    mainThreadHandler.post {
                        view.logError("Polling exception: ${e.message}", e)
                    }
                }
                
                delay(POLLING_INTERVAL_MS)
            }
        }
    }
    
    private fun stopAudioPolling() {
        view.logInfo("Stopping audio polling")
        pollingJob?.cancel()
        pollingJob = null
    }
}
