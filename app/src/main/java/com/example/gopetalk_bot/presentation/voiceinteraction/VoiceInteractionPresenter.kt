package com.example.gopetalk_bot.presentation.voiceinteraction

import android.os.Handler
import android.os.Looper
import com.example.gopetalk_bot.data.datasources.remote.dto.BackendResponse
import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.usecases.GetRecordedAudioUseCase
import com.example.gopetalk_bot.domain.usecases.MonitorAudioLevelUseCase
import com.example.gopetalk_bot.domain.usecases.PauseAudioRecordingUseCase
import com.example.gopetalk_bot.domain.usecases.ResumeAudioRecordingUseCase
import com.example.gopetalk_bot.domain.usecases.SendAudioCommandUseCase
import com.example.gopetalk_bot.domain.usecases.SetTtsListenerUseCase
import com.example.gopetalk_bot.domain.usecases.ShutdownTtsUseCase
import com.example.gopetalk_bot.domain.usecases.SpeakTextUseCase
import com.example.gopetalk_bot.domain.usecases.StartAudioMonitoringUseCase
import com.example.gopetalk_bot.domain.usecases.StopAudioMonitoringUseCase
import com.example.gopetalk_bot.domain.usecases.ConnectWebSocketUseCase
import com.example.gopetalk_bot.domain.usecases.DisconnectWebSocketUseCase
import com.example.gopetalk_bot.domain.usecases.PlayAudioFileUseCase
import com.example.gopetalk_bot.domain.usecases.UpdateWebSocketChannelUseCase
import com.example.gopetalk_bot.domain.usecases.PollAudioUseCase
import com.example.gopetalk_bot.domain.repositories.WebSocketRepository
import com.example.gopetalk_bot.domain.repositories.AudioPlayerRepository
import com.example.gopetalk_bot.presentation.common.AudioRmsMonitor
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    private val userPreferences: com.example.gopetalk_bot.data.datasources.local.UserPreferences
) : VoiceInteractionContract.Presenter {

    private val presenterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private val gson = Gson()
    
    @Volatile
    private var isMicrophoneBlocked = false
    
    @Volatile
    private var currentChannel: String? = null
    
    private var pollingJob: Job? = null
    private val pollingIntervalMs = 2000L // 2 segundos
    
    private var waitingMessageJob: Job? = null
    private val waitingMessageDelayMs = 3000L // 3 segundos antes de decir el mensaje de espera

    override fun start() {
        view.logInfo("Presenter started.")
        
        connectToWebSocket()

        setTtsListenerUseCase.execute(
            onStart = {
                view.logInfo("TTS started. Pausing audio recording.")
                pauseAudioRecordingUseCase.execute()
            },
            onDone = {
                view.logInfo("TTS finished. Resuming audio recording.")
                if (!isMicrophoneBlocked) {
                    resumeAudioRecordingUseCase.execute()
                }
            },
            onError = {
                view.logError("TTS error. Resuming audio recording.", null)
                if (!isMicrophoneBlocked) {
                    resumeAudioRecordingUseCase.execute()
                }
            }
        )

        startAudioMonitoringUseCase.execute()
        
        presenterScope.launch {
            monitorAudioLevelUseCase.execute().collect { audioLevel ->
                view.logInfo("Current audio level (dB): ${audioLevel.rmsDb}")
                AudioRmsMonitor.updateRmsDb(audioLevel.rmsDb)
            }
        }
        
        presenterScope.launch {
            getRecordedAudioUseCase.execute().collect { audioData ->
                view.logInfo("Sending audio file to backend: ${audioData.file.path}")
                sendAudioToBackend(audioData)
            }
        }
        
        startAudioPolling()
    }

    private fun sendAudioToBackend(audioData: AudioData) {
        // Programar mensaje de espera después de 3 segundos
        scheduleWaitingMessage()
        
        sendAudioCommandUseCase.execute(audioData) { response ->
            // Cancelar el mensaje de espera si la respuesta llega antes
            cancelWaitingMessage()
            mainThreadHandler.post {
                when (response) {
                    is ApiResponse.Success -> {
                        view.logInfo("Audio sent successfully. Code: ${response.statusCode}")
                        
                        if (response.audioFile != null) {
                            view.logInfo("Audio file received from another user, playing...")
                            playReceivedAudioFile(response.audioFile!!)
                            return@post
                        }
                        
                        if (response.statusCode == 204) {
                            view.logInfo("Message relayed by backend.")
                            return@post
                        }
                        
                        val trimmedBody = response.body.trim()
                        if (trimmedBody.isNotBlank()) {
                            if (trimmedBody.startsWith("{") && trimmedBody.endsWith("}")) {
                                try {
                                    val backendResponse = gson.fromJson(response.body, BackendResponse::class.java)
                                    handleBackendResponse(backendResponse)
                                } catch (e: Exception) {
                                    view.logError("Failed to parse backend JSON response", e)
                                    speakTextUseCase.execute("No entendí la respuesta del servidor.", UUID.randomUUID().toString())
                                }
                            } else {
                                view.logInfo("Speaking plain text response: $trimmedBody")
                                speakTextUseCase.execute(trimmedBody, UUID.randomUUID().toString())
                            }
                        }
                    }
                    is ApiResponse.Error -> {
                        view.logError("API Error: ${response.message}", response.exception)
                    }
                }
            }
        }
    }
    
    private fun scheduleWaitingMessage() {
        waitingMessageJob?.cancel()
        waitingMessageJob = presenterScope.launch {
            delay(waitingMessageDelayMs)
            view.logInfo("Starting waiting message loop every ${waitingMessageDelayMs}ms")
            
            // Loop infinito hasta que se cancele el job
            while (isActive) {
                view.logInfo("Saying waiting message")
                speakTextUseCase.execute("Trayendo tu respuesta, espera", "waiting_message_${System.currentTimeMillis()}")
                delay(waitingMessageDelayMs)
            }
        }
    }
    
    private fun cancelWaitingMessage() {
        waitingMessageJob?.cancel()
        waitingMessageJob = null
    }

    private fun handleBackendResponse(response: BackendResponse) {
        view.logInfo("Backend response: $response")
        
        if (!response.channel.isNullOrBlank() && response.channel != currentChannel) {
            view.logInfo("Backend confirmed channel: ${response.channel}")
            updateChannel(response.channel)
        }
        
        var fullResponse = response.text
        if (fullResponse.isBlank()) {
            fullResponse = when (response.action) {
                "list_channels" -> "La lista de canales es: ${response.channels.joinToString(", ")}"
                "list_users" -> "La lista de usuarios es: ${response.users.joinToString(", ")}"
                else -> "Acción no reconocida"
            }
        }

        if (fullResponse.isNotBlank()) {
            val utteranceId = UUID.randomUUID().toString()
            speakTextUseCase.execute(fullResponse, utteranceId)
        }
    }
    
    private fun connectToWebSocket() {
        val wsUrl = "ws://159.223.150.185/ws"
        val authToken = userPreferences.authToken
        view.logInfo("Connecting to WebSocket: $wsUrl with authToken=${authToken?.take(20)}..., channel=$currentChannel")
        
        connectWebSocketUseCase.execute(wsUrl, authToken, currentChannel, object : WebSocketRepository.MicrophoneControlListener {
            override fun onMicrophoneStart() {
                view.logInfo("✅ WebSocket: Microphone START signal received - Unblocking microphone")
                view.logInfo("   Previous state: isMicrophoneBlocked=$isMicrophoneBlocked")
                isMicrophoneBlocked = false
                view.logInfo("   New state: isMicrophoneBlocked=$isMicrophoneBlocked")
                resumeAudioRecordingUseCase.execute()
                view.logInfo("   Audio recording resumed")
            }
            
            override fun onMicrophoneStop() {
                view.logInfo("🛑 WebSocket: Microphone STOP signal received - Blocking microphone")
                view.logInfo("   Previous state: isMicrophoneBlocked=$isMicrophoneBlocked")
                isMicrophoneBlocked = true
                view.logInfo("   New state: isMicrophoneBlocked=$isMicrophoneBlocked")
                pauseAudioRecordingUseCase.execute()
                view.logInfo("   Audio recording paused")
            }
            
            override fun onConnectionEstablished() {
                view.logInfo("🔌 WebSocket connection established successfully")
                view.logInfo("   Connected to: $wsUrl")
                view.logInfo("   AuthToken: ${authToken?.take(20)}..., Channel: ${currentChannel ?: "none"}")
            }
            
            override fun onConnectionClosed() {
                view.logInfo("🔌 WebSocket connection closed")
            }
            
            override fun onError(error: String) {
                view.logError("❌ WebSocket error: $error", null)
            }
        })
    }
    
    private fun playReceivedAudioFile(audioFile: File) {
        view.logInfo("Playing received audio file from another user: ${audioFile.path}")
        playAudioFileUseCase.execute(audioFile, object : AudioPlayerRepository.PlaybackListener {
            override fun onPlaybackStarted() {
                view.logInfo("Received audio playback started")
            }
            
            override fun onPlaybackCompleted() {
                view.logInfo("Received audio playback completed")
                // Clean up temp file
                audioFile.delete()
            }
            
            override fun onPlaybackError(error: String) {
                view.logError("Received audio playback error: $error", null)
                // Clean up temp file
                audioFile.delete()
            }
        })
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
        val message = "Bienvenido $username, indícame el canal al que te quieras unir"
        speakTextUseCase.execute(message, "welcome_message")
        view.logInfo("Speaking welcome message for user: $username")
    }
    
    private fun startAudioPolling() {
        view.logInfo("🔄 Starting audio polling every ${pollingIntervalMs}ms")
        
        pollingJob = presenterScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    pollAudioUseCase.execute(
                        onAudioReceived = { audioFile, fromUserId, channel ->
                            mainThreadHandler.post {
                                view.logInfo("📥 Audio received via polling from user $fromUserId in channel $channel")
                                playReceivedAudioFile(audioFile)
                            }
                        },
                        onNoAudio = {
                            // No hacer nada, es normal que no haya audio pendiente
                        },
                        onError = { error ->
                            mainThreadHandler.post {
                                view.logError("❌ Polling error: $error", null)
                            }
                        }
                    )
                } catch (e: Exception) {
                    mainThreadHandler.post {
                        view.logError("❌ Polling exception: ${e.message}", e)
                    }
                }
                
                delay(pollingIntervalMs)
            }
        }
    }
    
    private fun stopAudioPolling() {
        view.logInfo("🛑 Stopping audio polling")
        pollingJob?.cancel()
        pollingJob = null
    }
}
