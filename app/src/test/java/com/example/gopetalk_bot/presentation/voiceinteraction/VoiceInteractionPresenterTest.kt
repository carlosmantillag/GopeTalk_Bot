package com.example.gopetalk_bot.presentation.voiceinteraction

import android.os.Handler
import android.os.Looper
import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.entities.AudioLevel
import com.example.gopetalk_bot.domain.repositories.AudioPlayerRepository
import com.example.gopetalk_bot.domain.repositories.WebSocketRepository
import com.example.gopetalk_bot.domain.usecases.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceInteractionPresenterTest {

    private lateinit var view: VoiceInteractionContract.View
    private lateinit var startAudioMonitoringUseCase: StartAudioMonitoringUseCase
    private lateinit var stopAudioMonitoringUseCase: StopAudioMonitoringUseCase
    private lateinit var pauseAudioRecordingUseCase: PauseAudioRecordingUseCase
    private lateinit var resumeAudioRecordingUseCase: ResumeAudioRecordingUseCase
    private lateinit var monitorAudioLevelUseCase: MonitorAudioLevelUseCase
    private lateinit var getRecordedAudioUseCase: GetRecordedAudioUseCase
    private lateinit var sendAudioCommandUseCase: SendAudioCommandUseCase
    private lateinit var speakTextUseCase: SpeakTextUseCase
    private lateinit var setTtsListenerUseCase: SetTtsListenerUseCase
    private lateinit var shutdownTtsUseCase: ShutdownTtsUseCase
    private lateinit var connectWebSocketUseCase: ConnectWebSocketUseCase
    private lateinit var disconnectWebSocketUseCase: DisconnectWebSocketUseCase
    private lateinit var playAudioFileUseCase: PlayAudioFileUseCase
    private lateinit var updateWebSocketChannelUseCase: UpdateWebSocketChannelUseCase
    private lateinit var pollAudioUseCase: PollAudioUseCase
    private lateinit var userPreferences: UserPreferences
    private lateinit var presenter: VoiceInteractionPresenter

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock Android Looper and Handler
        mockkStatic(Looper::class)
        val mockLooper = mockk<Looper>(relaxed = true)
        every { Looper.getMainLooper() } returns mockLooper
        
        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } answers {
            firstArg<Runnable>().run()
            true
        }
        every { anyConstructed<Handler>().postDelayed(any(), any()) } answers {
            firstArg<Runnable>().run()
            true
        }

        view = mockk(relaxed = true)
        startAudioMonitoringUseCase = mockk(relaxed = true)
        stopAudioMonitoringUseCase = mockk(relaxed = true)
        pauseAudioRecordingUseCase = mockk(relaxed = true)
        resumeAudioRecordingUseCase = mockk(relaxed = true)
        monitorAudioLevelUseCase = mockk(relaxed = true)
        getRecordedAudioUseCase = mockk(relaxed = true)
        sendAudioCommandUseCase = mockk(relaxed = true)
        speakTextUseCase = mockk(relaxed = true)
        setTtsListenerUseCase = mockk(relaxed = true)
        shutdownTtsUseCase = mockk(relaxed = true)
        connectWebSocketUseCase = mockk(relaxed = true)
        disconnectWebSocketUseCase = mockk(relaxed = true)
        playAudioFileUseCase = mockk(relaxed = true)
        updateWebSocketChannelUseCase = mockk(relaxed = true)
        pollAudioUseCase = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)

        every { monitorAudioLevelUseCase.execute() } returns flowOf(AudioLevel(0.0f))
        every { getRecordedAudioUseCase.execute() } returns flowOf()
        every { userPreferences.authToken } returns "test-token"

        presenter = VoiceInteractionPresenter(
            view,
            startAudioMonitoringUseCase,
            stopAudioMonitoringUseCase,
            pauseAudioRecordingUseCase,
            resumeAudioRecordingUseCase,
            monitorAudioLevelUseCase,
            getRecordedAudioUseCase,
            sendAudioCommandUseCase,
            speakTextUseCase,
            setTtsListenerUseCase,
            shutdownTtsUseCase,
            connectWebSocketUseCase,
            disconnectWebSocketUseCase,
            playAudioFileUseCase,
            updateWebSocketChannelUseCase,
            pollAudioUseCase,
            userPreferences
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Looper::class)
        unmockkConstructor(Handler::class)
        clearAllMocks()
    }

    @Test
    fun `start should initialize all components`() {
        presenter.start()

        verify { connectWebSocketUseCase.execute(any(), any(), any(), any()) }
        verify { setTtsListenerUseCase.execute(any(), any(), any()) }
        verify { startAudioMonitoringUseCase.execute() }
        verify { view.logInfo("Presenter started.") }
    }

    @Test
    fun `stop should cleanup all resources`() {
        presenter.stop()

        verify { disconnectWebSocketUseCase.execute() }
        verify { stopAudioMonitoringUseCase.execute() }
        verify { shutdownTtsUseCase.execute() }
        verify { view.logInfo("Presenter stopped.") }
    }

    @Test
    fun `speakWelcome should speak welcome message with username`() {
        presenter.speakWelcome("TestUser")

        verify { speakTextUseCase.execute(match { it.contains("TestUser") }, any()) }
    }

    @Test
    fun `updateChannel should update channel and websocket`() {
        presenter.updateChannel("general")

        verify { updateWebSocketChannelUseCase.execute("test-token", "general") }
        verify { view.logInfo("Channel updated to: general") }
    }

    @Test
    fun `updateChannel with null should update to null`() {
        presenter.updateChannel(null)

        verify { updateWebSocketChannelUseCase.execute("test-token", null) }
        verify { view.logInfo("Channel updated to: null") }
    }

    @Test
    fun `TTS listeners should be set up on start`() {
        presenter.start()

        verify { setTtsListenerUseCase.execute(any(), any(), any()) }
    }

    @Test
    fun `WebSocket microphone start should resume recording`() {
        var listener: WebSocketRepository.MicrophoneControlListener? = null

        every { connectWebSocketUseCase.execute(any(), any(), any(), any()) } answers {
            listener = arg(3)
        }

        presenter.start()
        listener?.onMicrophoneStart()

        verify { resumeAudioRecordingUseCase.execute() }
        verify { view.logInfo("WebSocket: Microphone START") }
    }

    @Test
    fun `WebSocket microphone stop should pause recording`() {
        var listener: WebSocketRepository.MicrophoneControlListener? = null

        every { connectWebSocketUseCase.execute(any(), any(), any(), any()) } answers {
            listener = arg(3)
        }

        presenter.start()
        listener?.onMicrophoneStop()

        verify { pauseAudioRecordingUseCase.execute() }
        verify { view.logInfo("WebSocket: Microphone STOP") }
    }

    @Test
    fun `start should setup audio monitoring`() {
        presenter.start()

        verify { startAudioMonitoringUseCase.execute() }
        verify { monitorAudioLevelUseCase.execute() }
    }

    @Test
    fun `handleApiResponse should handle success with audio file`() = runTest {
        var capturedCallback: ((ApiResponse) -> Unit)? = null
        val mockFile = mockk<File>(relaxed = true)
        
        every { sendAudioCommandUseCase.execute(any(), any()) } answers {
            capturedCallback = secondArg()
        }
        
        val mockAudioData = AudioData(mockk(relaxed = true), 16000, 1, com.example.gopetalk_bot.domain.entities.AudioFormat.PCM_16BIT)
        every { getRecordedAudioUseCase.execute() } returns flowOf(mockAudioData)
        
        presenter.start()
        advanceUntilIdle()
        
        capturedCallback?.invoke(ApiResponse.Success(200, "OK", mockFile))
        
        verify { playAudioFileUseCase.execute(mockFile, any()) }
    }

    @Test
    fun `handleApiResponse should handle 204 no content`() = runTest {
        var capturedCallback: ((ApiResponse) -> Unit)? = null
        
        every { sendAudioCommandUseCase.execute(any(), any()) } answers {
            capturedCallback = secondArg()
        }
        
        val mockAudioData = AudioData(mockk(relaxed = true), 16000, 1, com.example.gopetalk_bot.domain.entities.AudioFormat.PCM_16BIT)
        every { getRecordedAudioUseCase.execute() } returns flowOf(mockAudioData)
        
        presenter.start()
        advanceUntilIdle()
        
        capturedCallback?.invoke(ApiResponse.Success(204, "", null))
        
        verify(exactly = 0) { speakTextUseCase.execute(any(), any()) }
    }

    @Test
    fun `handleApiResponse should handle blank response body`() = runTest {
        var capturedCallback: ((ApiResponse) -> Unit)? = null
        
        every { sendAudioCommandUseCase.execute(any(), any()) } answers {
            capturedCallback = secondArg()
        }
        
        val mockAudioData = AudioData(mockk(relaxed = true), 16000, 1, com.example.gopetalk_bot.domain.entities.AudioFormat.PCM_16BIT)
        every { getRecordedAudioUseCase.execute() } returns flowOf(mockAudioData)
        
        presenter.start()
        advanceUntilIdle()
        
        capturedCallback?.invoke(ApiResponse.Success(200, "   ", null))
        
        verify(exactly = 0) { speakTextUseCase.execute(any(), any()) }
    }

    @Test
    fun `handleApiResponse should handle JSON response`() = runTest {
        var capturedCallback: ((ApiResponse) -> Unit)? = null
        
        every { sendAudioCommandUseCase.execute(any(), any()) } answers {
            capturedCallback = secondArg()
        }
        
        val mockAudioData = AudioData(mockk(relaxed = true), 16000, 1, com.example.gopetalk_bot.domain.entities.AudioFormat.PCM_16BIT)
        every { getRecordedAudioUseCase.execute() } returns flowOf(mockAudioData)
        
        presenter.start()
        advanceUntilIdle()
        
        val jsonResponse = """{"message":"Hello","channel":"test"}"""
        capturedCallback?.invoke(ApiResponse.Success(200, jsonResponse, null))
        
        verify { view.logInfo(any()) }
    }

    @Test
    fun `handleApiResponse should handle plain text response`() = runTest {
        var capturedCallback: ((ApiResponse) -> Unit)? = null
        
        every { sendAudioCommandUseCase.execute(any(), any()) } answers {
            capturedCallback = secondArg()
        }
        
        val mockAudioData = AudioData(mockk(relaxed = true), 16000, 1, com.example.gopetalk_bot.domain.entities.AudioFormat.PCM_16BIT)
        every { getRecordedAudioUseCase.execute() } returns flowOf(mockAudioData)
        
        presenter.start()
        advanceUntilIdle()
        
        capturedCallback?.invoke(ApiResponse.Success(200, "Hello world", null))
        
        verify { speakTextUseCase.execute("Hello world", any()) }
    }

    @Test
    fun `handleApiResponse should handle error response`() = runTest {
        var capturedCallback: ((ApiResponse) -> Unit)? = null
        
        every { sendAudioCommandUseCase.execute(any(), any()) } answers {
            capturedCallback = secondArg()
        }
        
        val mockAudioData = AudioData(mockk(relaxed = true), 16000, 1, com.example.gopetalk_bot.domain.entities.AudioFormat.PCM_16BIT)
        every { getRecordedAudioUseCase.execute() } returns flowOf(mockAudioData)
        
        presenter.start()
        advanceUntilIdle()
        
        capturedCallback?.invoke(ApiResponse.Error("Network error", 500, null))
        
        verify { view.logError("API Error: Network error", null) }
    }

    @Test
    fun `TTS onStart should pause recording`() {
        var onStart: (() -> Unit)? = null
        
        every { setTtsListenerUseCase.execute(any(), any(), any()) } answers {
            onStart = firstArg()
        }
        
        presenter.start()
        onStart?.invoke()
        
        verify { pauseAudioRecordingUseCase.execute() }
    }

    @Test
    fun `TTS onDone should resume recording when not blocked`() {
        var onDone: (() -> Unit)? = null
        
        every { setTtsListenerUseCase.execute(any(), any(), any()) } answers {
            onDone = secondArg()
        }
        
        presenter.start()
        onDone?.invoke()
        
        verify { resumeAudioRecordingUseCase.execute() }
    }

    @Test
    fun `TTS onError should resume recording when not blocked`() {
        var onError: (() -> Unit)? = null
        
        every { setTtsListenerUseCase.execute(any(), any(), any()) } answers {
            onError = thirdArg()
        }
        
        presenter.start()
        onError?.invoke()
        
        verify { resumeAudioRecordingUseCase.execute() }
    }

    @Test
    fun `stop should cleanup resources`() {
        presenter.start()
        presenter.stop()
        
        verify { stopAudioMonitoringUseCase.execute() }
        verify { disconnectWebSocketUseCase.execute() }
        verify { shutdownTtsUseCase.execute() }
    }
}
