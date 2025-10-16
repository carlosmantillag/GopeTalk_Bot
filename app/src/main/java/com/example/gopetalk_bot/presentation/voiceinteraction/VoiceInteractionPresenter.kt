package com.example.gopetalk_bot.presentation.voiceinteraction

import android.os.Handler
import android.os.Looper
import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.usecases.GetRecordedAudioUseCase
import com.example.gopetalk_bot.domain.usecases.MonitorAudioLevelUseCase
import com.example.gopetalk_bot.domain.usecases.SendAudioCommandUseCase
import com.example.gopetalk_bot.domain.usecases.StartAudioMonitoringUseCase
import com.example.gopetalk_bot.domain.usecases.StopAudioMonitoringUseCase
import com.example.gopetalk_bot.presentation.common.AudioRmsMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Presenter for Voice Interaction following MVP + Clean Architecture
 */
class VoiceInteractionPresenter(
    private val view: VoiceInteractionContract.View,
    private val startAudioMonitoringUseCase: StartAudioMonitoringUseCase,
    private val stopAudioMonitoringUseCase: StopAudioMonitoringUseCase,
    private val monitorAudioLevelUseCase: MonitorAudioLevelUseCase,
    private val getRecordedAudioUseCase: GetRecordedAudioUseCase,
    private val sendAudioCommandUseCase: SendAudioCommandUseCase,
    private val userId: String = "1" // TODO: Get from user session
) : VoiceInteractionContract.Presenter {

    private val presenterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    override fun start() {
        view.logInfo("Presenter started.")
        
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
                        view.logInfo("Audio enviado correctamente. Código: ${response.statusCode}")
                    }
                    is ApiResponse.Error -> {
                        view.logError("API Error: ${response.message}", response.exception)
                    }
                }
            }
        }
    }

    override fun stop() {
        stopAudioMonitoringUseCase.execute()
        presenterScope.cancel()
        view.logInfo("Presenter stopped.")
    }
}
