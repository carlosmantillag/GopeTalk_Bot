package com.example.gopetalk_bot.di

import android.content.Context
import com.example.gopetalk_bot.data.datasources.local.AudioDataSource
import com.example.gopetalk_bot.data.datasources.local.AudioPlayerDataSource
import com.example.gopetalk_bot.data.datasources.local.PermissionDataSource
import com.example.gopetalk_bot.data.datasources.local.SpeechRecognizerDataSource
import com.example.gopetalk_bot.data.datasources.local.TextToSpeechDataSource
import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.data.datasources.remote.RemoteDataSource
import com.example.gopetalk_bot.data.datasources.remote.WebSocketDataSource
import com.example.gopetalk_bot.data.repositories.ApiRepositoryImpl
import com.example.gopetalk_bot.data.repositories.AudioPlayerRepositoryImpl
import com.example.gopetalk_bot.data.repositories.AudioRepositoryImpl
import com.example.gopetalk_bot.data.repositories.PermissionRepositoryImpl
import com.example.gopetalk_bot.data.repositories.TextToSpeechRepositoryImpl
import com.example.gopetalk_bot.data.repositories.UserRepositoryImpl
import com.example.gopetalk_bot.data.repositories.WebSocketRepositoryImpl
import com.example.gopetalk_bot.domain.repositories.AudioRepository
import com.example.gopetalk_bot.domain.repositories.PermissionRepository
import com.example.gopetalk_bot.domain.repositories.TextToSpeechRepository
import com.example.gopetalk_bot.domain.repositories.UserRepository
import com.example.gopetalk_bot.domain.repositories.WebSocketRepository
import com.example.gopetalk_bot.domain.usecases.CheckPermissionsUseCase
import com.example.gopetalk_bot.domain.usecases.ConnectWebSocketUseCase
import com.example.gopetalk_bot.domain.usecases.DisconnectWebSocketUseCase
import com.example.gopetalk_bot.domain.usecases.GetRecordedAudioUseCase
import com.example.gopetalk_bot.domain.usecases.MonitorAudioLevelUseCase
import com.example.gopetalk_bot.domain.usecases.PauseAudioRecordingUseCase
import com.example.gopetalk_bot.domain.usecases.PlayAudioFileUseCase
import com.example.gopetalk_bot.domain.usecases.PollAudioUseCase
import com.example.gopetalk_bot.domain.usecases.ResumeAudioRecordingUseCase
import com.example.gopetalk_bot.domain.usecases.SendAudioCommandUseCase
import com.example.gopetalk_bot.domain.usecases.SendAuthenticationUseCase
import com.example.gopetalk_bot.domain.usecases.SetTtsListenerUseCase
import com.example.gopetalk_bot.domain.usecases.ShutdownTtsUseCase
import com.example.gopetalk_bot.domain.usecases.SpeakTextUseCase
import com.example.gopetalk_bot.domain.usecases.StartAudioMonitoringUseCase
import com.example.gopetalk_bot.domain.usecases.StopAudioMonitoringUseCase
import com.example.gopetalk_bot.domain.usecases.UpdateWebSocketChannelUseCase
import com.example.gopetalk_bot.presentation.authentication.AuthenticationContract
import com.example.gopetalk_bot.presentation.authentication.AuthenticationPresenter
import com.example.gopetalk_bot.presentation.main.MainContract
import com.example.gopetalk_bot.presentation.main.MainPresenter
import com.example.gopetalk_bot.presentation.voiceinteraction.VoiceInteractionContract
import com.example.gopetalk_bot.presentation.voiceinteraction.VoiceInteractionPresenter

/**
 * Simple service locator that wires the production graph without affecting runtime behaviour.
 */
object ServiceLocator {

    fun provideAuthenticationPresenter(view: AuthenticationContract.View): AuthenticationContract.Presenter {
        val context = view.context
        val userPreferences = provideUserPreferences(context)
        val permissionRepository = providePermissionRepository(context)
        val checkPermissionsUseCase = CheckPermissionsUseCase(permissionRepository)

        val speechRecognizerDataSource = SpeechRecognizerDataSource(context)
        val apiRepository = provideApiRepository(userPreferences)
        val sendAuthenticationUseCase = SendAuthenticationUseCase(apiRepository)

        val ttsRepository = provideTextToSpeechRepository(context) { message ->
            view.logError("TTS Error: $message")
        }
        val speakTextUseCase = SpeakTextUseCase(ttsRepository)
        val setTtsListenerUseCase = SetTtsListenerUseCase(ttsRepository)
        val shutdownTtsUseCase = ShutdownTtsUseCase(ttsRepository)

        return AuthenticationPresenter(
            view = view,
            speechRecognizerDataSource = speechRecognizerDataSource,
            sendAuthenticationUseCase = sendAuthenticationUseCase,
            speakTextUseCase = speakTextUseCase,
            setTtsListenerUseCase = setTtsListenerUseCase,
            shutdownTtsUseCase = shutdownTtsUseCase,
            userPreferences = userPreferences,
            checkPermissionsUseCase = checkPermissionsUseCase
        )
    }

    fun provideMainPresenter(view: MainContract.View): MainContract.Presenter {
        val context = view.context
        val permissionRepository = providePermissionRepository(context)
        val checkPermissionsUseCase = CheckPermissionsUseCase(permissionRepository)
        val userRepository = provideUserRepository(context)
        return MainPresenter(view, checkPermissionsUseCase, userRepository)
    }

    fun provideVoiceInteractionPresenter(
        view: VoiceInteractionContract.View
    ): VoiceInteractionContract.Presenter {
        val context = view.context
        val userPreferences = provideUserPreferences(context)
        val audioRepository = provideAudioRepository(context)
        val apiRepository = provideApiRepository(userPreferences)
        val ttsRepository = provideTextToSpeechRepository(context) { message ->
            view.logError("TTS Error: $message")
        }
        val webSocketRepository = provideWebSocketRepository()
        val audioPlayerRepository = provideAudioPlayerRepository()

        return VoiceInteractionPresenter(
            view = view,
            startAudioMonitoringUseCase = StartAudioMonitoringUseCase(audioRepository),
            stopAudioMonitoringUseCase = StopAudioMonitoringUseCase(audioRepository),
            pauseAudioRecordingUseCase = PauseAudioRecordingUseCase(audioRepository),
            resumeAudioRecordingUseCase = ResumeAudioRecordingUseCase(audioRepository),
            monitorAudioLevelUseCase = MonitorAudioLevelUseCase(audioRepository),
            getRecordedAudioUseCase = GetRecordedAudioUseCase(audioRepository),
            sendAudioCommandUseCase = SendAudioCommandUseCase(apiRepository),
            speakTextUseCase = SpeakTextUseCase(ttsRepository),
            setTtsListenerUseCase = SetTtsListenerUseCase(ttsRepository),
            shutdownTtsUseCase = ShutdownTtsUseCase(ttsRepository),
            connectWebSocketUseCase = ConnectWebSocketUseCase(webSocketRepository),
            disconnectWebSocketUseCase = DisconnectWebSocketUseCase(webSocketRepository),
            playAudioFileUseCase = PlayAudioFileUseCase(audioPlayerRepository),
            updateWebSocketChannelUseCase = UpdateWebSocketChannelUseCase(webSocketRepository),
            pollAudioUseCase = PollAudioUseCase(apiRepository),
            userPreferences = userPreferences
        )
    }

    private fun provideUserPreferences(context: Context) = UserPreferences(context)

    private fun providePermissionRepository(context: Context): PermissionRepository {
        val permissionDataSource = PermissionDataSource(context)
        return PermissionRepositoryImpl(permissionDataSource)
    }

    private fun provideUserRepository(context: Context): UserRepository {
        return UserRepositoryImpl(provideUserPreferences(context))
    }

    private fun provideTextToSpeechRepository(
        context: Context,
        onInitError: (String) -> Unit
    ): TextToSpeechRepository {
        val ttsDataSource = TextToSpeechDataSource(
            context = context,
            onInitError = onInitError
        )
        return TextToSpeechRepositoryImpl(ttsDataSource)
    }

    private fun provideAudioRepository(context: Context): AudioRepository {
        val audioDataSource = AudioDataSource(context)
        return AudioRepositoryImpl(audioDataSource)
    }

    private fun provideAudioPlayerRepository() =
        AudioPlayerRepositoryImpl(AudioPlayerDataSource())

    private fun provideRemoteDataSource() = RemoteDataSource()

    private fun provideApiRepository(userPreferences: UserPreferences) =
        ApiRepositoryImpl(provideRemoteDataSource(), userPreferences)

    private fun provideWebSocketRepository(): WebSocketRepository {
        val webSocketDataSource = WebSocketDataSource()
        return WebSocketRepositoryImpl(webSocketDataSource)
    }
}
