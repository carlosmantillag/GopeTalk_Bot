package com.example.gopetalk_bot.presentation.voiceinteraction

import android.os.Handler
import android.os.Looper
import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.data.datasources.remote.dto.BackendResponse
import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.repositories.AudioPlayerRepository
import com.example.gopetalk_bot.domain.repositories.WebSocketRepository
import com.example.gopetalk_bot.domain.usecases.ConnectWebSocketUseCase
import com.example.gopetalk_bot.domain.usecases.DisconnectWebSocketUseCase
import com.example.gopetalk_bot.domain.usecases.GetRecordedAudioUseCase
import com.example.gopetalk_bot.domain.usecases.MonitorAudioLevelUseCase
import com.example.gopetalk_bot.domain.usecases.PauseAudioRecordingUseCase
import com.example.gopetalk_bot.domain.usecases.PlayAudioFileUseCase
import com.example.gopetalk_bot.domain.usecases.PollAudioUseCase
import com.example.gopetalk_bot.domain.usecases.ResumeAudioRecordingUseCase
import com.example.gopetalk_bot.domain.usecases.SendAudioCommandUseCase
import com.example.gopetalk_bot.domain.usecases.SetTtsListenerUseCase
import com.example.gopetalk_bot.domain.usecases.ShutdownTtsUseCase
import com.example.gopetalk_bot.domain.usecases.SpeakTextUseCase
import com.example.gopetalk_bot.domain.usecases.StartAudioMonitoringUseCase
import com.example.gopetalk_bot.domain.usecases.StopAudioMonitoringUseCase
import com.example.gopetalk_bot.domain.usecases.UpdateWebSocketChannelUseCase
import com.example.gopetalk_bot.presentation.common.AudioRmsMonitor
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
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
    private val userPreferences: UserPreferences,
    private val mainThreadHandler: Handler = Handler(Looper.getMainLooper()),
    private val waitingMessageDelayMs: Long = WAITING_MESSAGE_DELAY_MS,
    private val waitingMessageText: String = WAITING_MESSAGE,
    private val waitingMessageMaxRepeats: Int = Int.MAX_VALUE,
    private val isAudioPollingEnabled: Boolean = true,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : VoiceInteractionContract.Presenter {

    companion object {
        const val WEBSOCKET_URL = "ws://159.223.150.185/ws"
        const val POLLING_INTERVAL_MS = 2000L
        const val WAITING_MESSAGE_DELAY_MS = 4000L
        const val WAITING_MESSAGE = "Trayendo tu respuesta, espera"
        const val AUDIO_DURATION_LIMIT_MS = 60_000L
        const val AUDIO_DURATION_LIMIT_MESSAGE = "Lo siento, límite de audio superado"
        private const val WAV_HEADER_BYTES = 44
        private const val PCM_16BIT_BYTES_PER_SAMPLE = 2
        const val STATUS_CODE_NO_CONTENT = 204
        const val SERVER_OUTAGE_STREAM_MESSAGE = "Estamos reiniciando el servicio de audio, vuelve a intentarlo en unos momentos."
        const val SERVER_OUTAGE_NETWORK_MESSAGE = "No pudimos conectar con el servidor, revisa tu conexión o inténtalo nuevamente más tarde."
        const val SERVER_OUTAGE_UNAVAILABLE_MESSAGE = "El servidor está en mantenimiento, vuelve a intentarlo pronto."
        const val SERVER_OUTAGE_TIMEOUT_MESSAGE = "La conexión está tardando demasiado, intentemos de nuevo en un momento."
        private val SERVER_DOWN_PATTERNS = listOf(
            "unexpected end of stream" to SERVER_OUTAGE_STREAM_MESSAGE,
            "connection refused" to SERVER_OUTAGE_NETWORK_MESSAGE,
            "failed to connect" to SERVER_OUTAGE_NETWORK_MESSAGE,
            "host unreachable" to SERVER_OUTAGE_NETWORK_MESSAGE,
            "unable to resolve host" to SERVER_OUTAGE_NETWORK_MESSAGE,
            "503" to SERVER_OUTAGE_UNAVAILABLE_MESSAGE,
            "502" to SERVER_OUTAGE_UNAVAILABLE_MESSAGE,
            "504" to SERVER_OUTAGE_UNAVAILABLE_MESSAGE,
            "service unavailable" to SERVER_OUTAGE_UNAVAILABLE_MESSAGE,
            "gateway timeout" to SERVER_OUTAGE_TIMEOUT_MESSAGE,
            "timed out" to SERVER_OUTAGE_TIMEOUT_MESSAGE,
            "timeout" to SERVER_OUTAGE_TIMEOUT_MESSAGE
        )
    }

    private val presenterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val gson = Gson()
    
    @Volatile
    private var isMicrophoneBlocked = false
    
    @Volatile
    private var currentChannel: String? = null
    
    private var pollingJob: Job? = null
    private var waitingMessageJob: Job? = null
    @Volatile
    private var lastServerOutageMessage: String? = null

    override fun start() {
        view.logInfo("Presenter started.")
        connectToWebSocket()
        setupTtsListeners()
        startAudioMonitoring()
        if (isAudioPollingEnabled) {
            startAudioPolling()
        }
    }

    private fun isAudioDurationExceeded(audioData: AudioData): Boolean {
        val durationMs = calculateAudioDurationMs(audioData)
        return durationMs != null && durationMs > AUDIO_DURATION_LIMIT_MS
    }

    private fun calculateAudioDurationMs(audioData: AudioData): Long? {
        if (!audioData.file.exists()) return null
        val audioBytes = (audioData.file.length() - WAV_HEADER_BYTES).coerceAtLeast(0L)
        val bytesPerSecond = when (audioData.format) {
            com.example.gopetalk_bot.domain.entities.AudioFormat.PCM_16BIT ->
                audioData.sampleRate * audioData.channels * PCM_16BIT_BYTES_PER_SAMPLE
        }
        if (bytesPerSecond <= 0) return null
        return (audioBytes * 1000L) / bytesPerSecond
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
        if (isAudioDurationExceeded(audioData)) {
            view.logInfo("Audio duration exceeded the limit, skipping send.")
            cancelWaitingMessage()
            audioData.file.delete()
            speak(AUDIO_DURATION_LIMIT_MESSAGE)
            return
        }
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
            is ApiResponse.Error -> {
                view.logError("API Error: ${response.message}", response.exception)
                handleServerOutageIfNeeded(response.message)
            }
        }
    }

    private fun handleSuccessResponse(response: ApiResponse.Success) {
        lastServerOutageMessage = null
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
            delay(waitingMessageDelayMs)
            var remainingMessages = waitingMessageMaxRepeats
            while (isActive && remainingMessages > 0) {
                speak(waitingMessageText)
                remainingMessages--
                if (!isActive || remainingMessages == 0) break
                delay(waitingMessageDelayMs)
            }
        }
    }
    
    private fun cancelWaitingMessage() {
        waitingMessageJob?.cancel()
        waitingMessageJob = null
    }

    private fun handleBackendResponse(response: BackendResponse) {
        val channelFromResponse = response.data?.channel ?: response.channel
        updateChannelIfChanged(channelFromResponse)

        when (response.action.lowercase()) {
            "list_channels" -> {
                handleListChannels(response.channels)
                return
            }
            "list_users" -> {
                handleListUsers(response.users)
                return
            }
            "logout" -> {
                handleLogout()
                return
            }
        }

        val responseText = sequenceOf(
            response.message,
            response.text,
        ).firstOrNull { it.isNotBlank() }.orEmpty()

        if (responseText.isNotBlank()) {
            speak(responseText)
        }
    }

    private fun handleListChannels(channels: List<String>) {
        if (channels.isEmpty()) {
            speak("Por ahora no hay canales disponibles.")
        } else {
            val channelList = channels.joinToString(", ")
            speak("Estos son los canales disponibles: $channelList")
        }
    }

    private fun handleListUsers(users: List<String>) {
        if (users.isEmpty()) {
            speak("No hay usuarios conectados en este momento.")
        } else {
            val userList = users.joinToString(", ")
            speak("Estos son los usuarios conectados: $userList")
        }
    }

    private fun handleLogout() {
        view.logInfo("Logout requested")
        view.logout()
        speak("Cerrando sesión, hasta luego")
    }

    private fun updateChannelIfChanged(newChannel: String?) {
        if (!newChannel.isNullOrBlank() && newChannel != currentChannel) {
            updateChannel(newChannel)
        }
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
        speak("Bienvenido $username, indícame el canal al que te quieras unir, dí algo como: conéctame al canal y luego un número del 1 al 5")
    }
    
    private fun startAudioPolling() {
        view.logInfo("Starting audio polling")
        
        pollingJob = presenterScope.launch(ioDispatcher) {
            while (isActive) {
                try {
                    pollAudioUseCase.execute(
                        onAudioReceived = { audioFile, fromUserId, channel ->
                            mainThreadHandler.post {
                                lastServerOutageMessage = null
                                view.logInfo("Audio received from user $fromUserId in channel $channel")
                                playReceivedAudioFile(audioFile)
                            }
                        },
                        onNoAudio = { lastServerOutageMessage = null },
                        onError = { error ->
                            mainThreadHandler.post {
                                view.logError("Polling error: $error", null)
                                handleServerOutageIfNeeded(error)
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

    private fun handleServerOutageIfNeeded(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        val lowerMessage = message.lowercase()
        val outageMessage = SERVER_DOWN_PATTERNS.firstOrNull { (keyword, _) ->
            lowerMessage.contains(keyword)
        }?.second ?: return false

        if (lastServerOutageMessage == outageMessage) return false
        lastServerOutageMessage = outageMessage
        speak(outageMessage)
        return true
    }
}
