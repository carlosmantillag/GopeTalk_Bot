package com.example.gopetalk_bot

import com.example.gopetalk_bot.data.datasources.local.UserPreferencesTest
import com.example.gopetalk_bot.data.repositories.*
import com.example.gopetalk_bot.domain.entities.*
import com.example.gopetalk_bot.domain.usecases.*
import com.example.gopetalk_bot.presentation.authentication.AuthenticationPresenterTest
import com.example.gopetalk_bot.presentation.common.AudioRmsMonitorTest
import com.example.gopetalk_bot.presentation.main.MainPresenterTest
import com.example.gopetalk_bot.presentation.voiceinteraction.VoiceInteractionPresenterTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Domain Layer - Use Cases
    SendAudioCommandUseCaseTest::class,
    SpeakTextUseCaseTest::class,
    StartAudioMonitoringUseCaseTest::class,
    StopAudioMonitoringUseCaseTest::class,
    DisconnectWebSocketUseCaseTest::class,
    ShutdownTtsUseCaseTest::class,
    SetTtsListenerUseCaseTest::class,
    SendAuthenticationUseCaseTest::class,
    ConnectWebSocketUseCaseTest::class,
    UpdateWebSocketChannelUseCaseTest::class,
    CheckPermissionsUseCaseTest::class,
    PauseAudioRecordingUseCaseTest::class,
    ResumeAudioRecordingUseCaseTest::class,
    GetRecordedAudioUseCaseTest::class,
    MonitorAudioLevelUseCaseTest::class,
    PlayAudioFileUseCaseTest::class,
    PollAudioUseCaseTest::class,
    
    // Domain Layer - Entities
    ApiResponseTest::class,
    AudioDataTest::class,
    AudioLevelTest::class,
    AudioFormatTest::class,
    PermissionStatusTest::class,
    
    // Data Layer - DataSources
    UserPreferencesTest::class,
    
    // Data Layer - Repositories
    UserRepositoryImplTest::class,
    PermissionRepositoryImplTest::class,
    ApiRepositoryImplTest::class,
    TextToSpeechRepositoryImplTest::class,
    WebSocketRepositoryImplTest::class,
    AudioPlayerRepositoryImplTest::class,
    AudioRepositoryImplTest::class,
    
    // Presentation Layer - Presenters
    AuthenticationPresenterTest::class,
    MainPresenterTest::class,
    VoiceInteractionPresenterTest::class,
    
    // Presentation Layer - Common
    AudioRmsMonitorTest::class
)
class AllUnitTestsSuite
