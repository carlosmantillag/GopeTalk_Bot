import android.os.Looper
import com.example.gopetalk_bot.data.datasources.remote.dto.BackendResponse
import com.example.gopetalk_bot.domain.entities.ApiResponse
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
import com.example.gopetalk_bot.presentation.common.AudioRmsMonitor
import com.example.gopetalk_bot.presentation.voiceinteraction.VoiceInteractionContract
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.logging.Handler

/**
 * Presenter for Voice Interaction following MVP + Clean Architecture
 */
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
    private val userId: String = "1" // TODO: Get from user session
) : VoiceInteractionContract.Presenter {

    private val presenterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainThreadHandler = android.os.Handler(Looper.getMainLooper())
    private val gson = Gson()

    override fun start() {
        view.logInfo("Presenter started.")

        // Set up TTS listener to pause/resume recording
        setTtsListenerUseCase.execute(
            onStart = {
                view.logInfo("TTS started. Pausing audio recording.")
                pauseAudioRecordingUseCase.execute()
            },
            onDone = {
                view.logInfo("TTS finished. Resuming audio recording.")
                resumeAudioRecordingUseCase.execute()
            },
            onError = {
                view.logError("TTS error. Resuming audio recording.", null)
                resumeAudioRecordingUseCase.execute()
            }
        )

        // Start audio monitoring
        startAudioMonitoringUseCase.execute()
        
        // Observe audio levels
        presenterScope.launch {
            monitorAudioLevelUseCase.execute().collect { audioLevel ->
                view.logInfo("Current audio level (dB): ${audioLevel.rmsDb}")
                AudioRmsMonitor.updateRmsDb(audioLevel.rmsDb)
            }
        }
        
        // Observe recorded audio
        presenterScope.launch {
            getRecordedAudioUseCase.execute().collect { audioData ->
                view.logInfo("Sending audio file to backend: ${audioData.file.path}")
                sendAudioToBackend(audioData)
            }
        }
    }

    private fun sendAudioToBackend(audioData: com.example.gopetalk_bot.domain.entities.AudioData) {
        sendAudioCommandUseCase.execute(audioData, userId) { response ->
            mainThreadHandler.post {
                when (response) {
                    is ApiResponse.Success -> {
                        view.logInfo("Audio enviado correctamente. Código: ${response.statusCode}. Respuesta: ${response.body}")

                        // Reproducir respuesta del servidor con TTS
                        if (response.body.isNotBlank()) {
                            view.logInfo("Reproduciendo respuesta: ${response.body}")
                        }
                        view.logInfo("Audio sent successfully. Code: ${response.statusCode}. Response: ${response.body}")
                        if (response.statusCode == 204) {
                            view.logInfo("Message relayed by backend.")
                            // Optionally, provide feedback to the user
                            // speakTextUseCase.execute("Mensaje enviado", UUID.randomUUID().toString())
                            return@post
                        }
                        
                        // Check if response is JSON or plain text
                        val trimmedBody = response.body.trim()
                        if (trimmedBody.startsWith("{") && trimmedBody.endsWith("}")) {
                            // Response is JSON, parse it
                            try {
                                val backendResponse = gson.fromJson(response.body, BackendResponse::class.java)
                                handleBackendResponse(backendResponse)
                            } catch (e: Exception) {
                                view.logError("Failed to parse backend JSON response", e)
                                speakTextUseCase.execute("No entendí la respuesta del servidor.", UUID.randomUUID().toString())
                            }
                        } else {
                            // Response is plain text, speak it directly
                            view.logInfo("Speaking plain text response: $trimmedBody")
                            speakTextUseCase.execute(trimmedBody, UUID.randomUUID().toString())
                        }
                    }
                    is ApiResponse.Error -> {
                        view.logError("API Error: ${response.message}", response.exception)
                    }
                }
            }
        }
    }

    private fun handleBackendResponse(response: BackendResponse) {
        view.logInfo("Backend response: $response")
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

    override fun stop() {
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
}
