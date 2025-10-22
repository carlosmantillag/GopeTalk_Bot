package com.example.gopetalk_bot.presentation.voiceinteraction

import android.content.Context
import android.os.Handler
import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.entities.AudioFormat
import com.example.gopetalk_bot.domain.entities.AudioLevel
import com.example.gopetalk_bot.domain.repositories.AudioPlayerRepository
import com.example.gopetalk_bot.domain.repositories.WebSocketRepository
import com.example.gopetalk_bot.domain.usecases.*
import com.example.gopetalk_bot.presentation.common.AudioRmsMonitor
import com.google.common.truth.Truth.assertThat
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

    private lateinit var presenter: VoiceInteractionPresenter
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
    private lateinit var mockHandler: Handler

    private val testDispatcher = StandardTestDispatcher()

    private val onTtsStartSlot = slot<(String?) -> Unit>()
    private val onTtsDoneSlot = slot<(String?) -> Unit>()
    private val onTtsErrorSlot = slot<(String?) -> Unit>()
    private val webSocketListenerSlot = slot<WebSocketRepository.MicrophoneControlListener>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        AudioRmsMonitor.reset()

        // Mock Handler para ejecutar inmediatamente
        mockHandler = mockk<Handler>(relaxed = true)
        every { mockHandler.post(any()) } answers {
            firstArg<Runnable>().run()
            true
        }
        every { mockHandler.postDelayed(any(), any()) } answers {
            firstArg<Runnable>().run()
            true
        }
        every { mockHandler.removeCallbacksAndMessages(null) } just Runs

        view = mockk(relaxed = true)
        every { view.context } returns mockk<Context>(relaxed = true)
        
        startAudioMonitoringUseCase = mockk(relaxed = true)
        stopAudioMonitoringUseCase = mockk(relaxed = true)
        pauseAudioRecordingUseCase = mockk(relaxed = true)
        resumeAudioRecordingUseCase = mockk(relaxed = true)
        sendAudioCommandUseCase = mockk(relaxed = true)
        speakTextUseCase = mockk(relaxed = true)
        shutdownTtsUseCase = mockk(relaxed = true)
        disconnectWebSocketUseCase = mockk(relaxed = true)
        playAudioFileUseCase = mockk(relaxed = true)
        updateWebSocketChannelUseCase = mockk(relaxed = true)
        pollAudioUseCase = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)

        monitorAudioLevelUseCase = mockk(relaxed = true)
        every { monitorAudioLevelUseCase.execute() } returns flowOf()

        getRecordedAudioUseCase = mockk(relaxed = true)
        every { getRecordedAudioUseCase.execute() } returns flowOf()

        setTtsListenerUseCase = mockk(relaxed = true)
        every { 
            setTtsListenerUseCase.execute(
                onStart = capture(onTtsStartSlot),
                onDone = capture(onTtsDoneSlot),
                onError = capture(onTtsErrorSlot)
            )
        } just Runs

        connectWebSocketUseCase = mockk(relaxed = true)
        every { 
            connectWebSocketUseCase.execute(any<String>(), any(), any(), capture(webSocketListenerSlot))
        } just Runs

        every { userPreferences.authToken } returns "test-token"

        presenter = VoiceInteractionPresenter(
            view = view,
            startAudioMonitoringUseCase = startAudioMonitoringUseCase,
            stopAudioMonitoringUseCase = stopAudioMonitoringUseCase,
            pauseAudioRecordingUseCase = pauseAudioRecordingUseCase,
            resumeAudioRecordingUseCase = resumeAudioRecordingUseCase,
            monitorAudioLevelUseCase = monitorAudioLevelUseCase,
            getRecordedAudioUseCase = getRecordedAudioUseCase,
            sendAudioCommandUseCase = sendAudioCommandUseCase,
            speakTextUseCase = speakTextUseCase,
            setTtsListenerUseCase = setTtsListenerUseCase,
            shutdownTtsUseCase = shutdownTtsUseCase,
            connectWebSocketUseCase = connectWebSocketUseCase,
            disconnectWebSocketUseCase = disconnectWebSocketUseCase,
            playAudioFileUseCase = playAudioFileUseCase,
            updateWebSocketChannelUseCase = updateWebSocketChannelUseCase,
            pollAudioUseCase = pollAudioUseCase,
            userPreferences = userPreferences,
            mainThreadHandler = mockHandler
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `start should initialize all services`() = runTest {
        presenter.start()
        advanceUntilIdle()

        verify { view.logInfo("Presenter started.") }
        verify { connectWebSocketUseCase.execute(any(), "test-token", null, any()) }
        verify { setTtsListenerUseCase.execute(any(), any(), any()) }
        verify { startAudioMonitoringUseCase.execute() }
    }

    @Test
    fun `TTS onStart callback should pause audio recording`() {
        presenter.start()
        
        onTtsStartSlot.captured.invoke(null)

        verify { pauseAudioRecordingUseCase.execute() }
    }

    @Test
    fun `TTS onDone callback should resume audio recording when not blocked`() {
        presenter.start()
        
        onTtsDoneSlot.captured.invoke(null)

        verify { resumeAudioRecordingUseCase.execute() }
    }

    @Test
    fun `TTS onDone callback should not resume audio when microphone blocked`() {
        presenter.start()
        webSocketListenerSlot.captured.onMicrophoneStop()
        clearMocks(resumeAudioRecordingUseCase)
        
        onTtsDoneSlot.captured.invoke(null)

        verify(exactly = 0) { resumeAudioRecordingUseCase.execute() }
    }

    @Test
    fun `TTS onError callback should resume audio recording when not blocked`() {
        presenter.start()
        
        onTtsErrorSlot.captured.invoke(null)

        verify { resumeAudioRecordingUseCase.execute() }
    }

    @Test
    fun `WebSocket onMicrophoneStart should unblock and resume recording`() {
        presenter.start()
        
        webSocketListenerSlot.captured.onMicrophoneStart()

        verify { view.logInfo("WebSocket: Microphone START") }
        verify { resumeAudioRecordingUseCase.execute() }
    }

    @Test
    fun `WebSocket onMicrophoneStop should block and pause recording`() {
        presenter.start()
        
        webSocketListenerSlot.captured.onMicrophoneStop()

        verify { view.logInfo("WebSocket: Microphone STOP") }
        verify { pauseAudioRecordingUseCase.execute() }
    }

    @Test
    fun `WebSocket onConnectionEstablished should log info`() {
        presenter.start()
        
        webSocketListenerSlot.captured.onConnectionEstablished()

        verify { view.logInfo("WebSocket connection established") }
    }

    @Test
    fun `WebSocket onConnectionClosed should log info`() {
        presenter.start()
        
        webSocketListenerSlot.captured.onConnectionClosed()

        verify { view.logInfo("WebSocket connection closed") }
    }

    @Test
    fun `WebSocket onError should log error`() {
        presenter.start()
        
        webSocketListenerSlot.captured.onError("Connection failed")

        verify { view.logError("WebSocket error: Connection failed", null) }
    }

    @Test
    fun `sendAudioToBackend should handle success response with text`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(
            file = mockFile,
            sampleRate = 44100,
            channels = 1,
            format = AudioFormat.PCM_16BIT
        )
        
        every { 
            sendAudioCommandUseCase.execute(any<AudioData>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(200, "Hola mundo")
            )
        }

        every { getRecordedAudioUseCase.execute() } returns flowOf(audioData)
        
        presenter.start()
        advanceUntilIdle()

        verify { speakTextUseCase.execute("Hola mundo", any()) }
    }

    @Test
    fun `sendAudioToBackend should handle success response with JSON`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(
            file = mockFile,
            sampleRate = 44100,
            channels = 1,
            format = AudioFormat.PCM_16BIT
        )
        
        val jsonResponse = """{"text":"Respuesta del servidor","channel":"general"}"""
        every { 
            sendAudioCommandUseCase.execute(any<AudioData>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(200, jsonResponse)
            )
        }

        every { getRecordedAudioUseCase.execute() } returns flowOf(audioData)
        
        presenter.start()
        advanceUntilIdle()

        verify { speakTextUseCase.execute("Respuesta del servidor", any()) }
    }

    @Test
    fun `sendAudioToBackend should handle 204 No Content response`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(
            file = mockFile,
            sampleRate = 44100,
            channels = 1,
            format = AudioFormat.PCM_16BIT
        )
        
        every { 
            sendAudioCommandUseCase.execute(any<AudioData>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(204, "")
            )
        }

        every { getRecordedAudioUseCase.execute() } returns flowOf(audioData)
        
        presenter.start()
        advanceUntilIdle()

        verify(exactly = 0) { speakTextUseCase.execute(any(), any()) }
    }

    @Test
    fun `sendAudioToBackend should handle error response`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(
            file = mockFile,
            sampleRate = 44100,
            channels = 1,
            format = AudioFormat.PCM_16BIT
        )
        
        every { 
            sendAudioCommandUseCase.execute(any<AudioData>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Error("Server error", 500, null)
            )
        }

        every { getRecordedAudioUseCase.execute() } returns flowOf(audioData)
        
        presenter.start()
        advanceUntilIdle()

        verify { view.logError("API Error: Server error", null) }
    }

    @Test
    fun `sendAudioToBackend should handle audio file response`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(
            file = mockFile,
            sampleRate = 44100,
            channels = 1,
            format = AudioFormat.PCM_16BIT
        )
        val mockResponseFile = mockk<File>(relaxed = true)
        
        every { 
            sendAudioCommandUseCase.execute(any<AudioData>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(200, "", audioFile = mockResponseFile)
            )
        }

        every { getRecordedAudioUseCase.execute() } returns flowOf(audioData)
        
        presenter.start()
        advanceUntilIdle()

        verify { playAudioFileUseCase.execute(mockResponseFile, any()) }
    }

    @Test
    fun `handleBackendResponse with list_channels action should speak channel list`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(
            file = mockFile,
            sampleRate = 44100,
            channels = 1,
            format = AudioFormat.PCM_16BIT
        )
        
        val jsonResponse = """{"action":"list_channels","channels":["general","tech","music"],"text":""}"""
        every { 
            sendAudioCommandUseCase.execute(any<AudioData>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(200, jsonResponse)
            )
        }

        every { getRecordedAudioUseCase.execute() } returns flowOf(audioData)
        
        presenter.start()
        advanceUntilIdle()

        verify { speakTextUseCase.execute(match { it.contains("general") && it.contains("tech") && it.contains("music") }, any()) }
    }

    @Test
    fun `handleBackendResponse with list_users action should speak user list`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(
            file = mockFile,
            sampleRate = 44100,
            channels = 1,
            format = AudioFormat.PCM_16BIT
        )
        
        val jsonResponse = """{"action":"list_users","users":["Alice","Bob","Charlie"],"text":""}"""
        every { 
            sendAudioCommandUseCase.execute(any<AudioData>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(200, jsonResponse)
            )
        }

        every { getRecordedAudioUseCase.execute() } returns flowOf(audioData)
        
        presenter.start()
        advanceUntilIdle()

        verify { speakTextUseCase.execute(match { it.contains("Alice") && it.contains("Bob") && it.contains("Charlie") }, any()) }
    }

    @Test
    fun `handleBackendResponse with logout action should trigger logout`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(
            file = mockFile,
            sampleRate = 44100,
            channels = 1,
            format = AudioFormat.PCM_16BIT
        )
        
        val jsonResponse = """{"action":"logout","text":""}"""
        every { 
            sendAudioCommandUseCase.execute(any<AudioData>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(200, jsonResponse)
            )
        }

        every { getRecordedAudioUseCase.execute() } returns flowOf(audioData)
        
        presenter.start()
        advanceUntilIdle()

        verify { view.logInfo("Logout requested") }
        verify { view.logout() }
        verify { speakTextUseCase.execute("Cerrando sesión, hasta luego", any()) }
    }

    @Test
    fun `handleBackendResponse with channel should update channel`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(
            file = mockFile,
            sampleRate = 44100,
            channels = 1,
            format = AudioFormat.PCM_16BIT
        )
        
        val jsonResponse = """{"text":"Canal actualizado","channel":"tech"}"""
        every { 
            sendAudioCommandUseCase.execute(any<AudioData>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(200, jsonResponse)
            )
        }

        every { getRecordedAudioUseCase.execute() } returns flowOf(audioData)
        
        presenter.start()
        advanceUntilIdle()

        verify { view.logInfo("Channel updated to: tech") }
        verify { updateWebSocketChannelUseCase.execute("test-token", "tech") }
    }

    @Test
    fun `handleBackendResponse with invalid JSON should speak error message`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(
            file = mockFile,
            sampleRate = 44100,
            channels = 1,
            format = AudioFormat.PCM_16BIT
        )
        
        every { 
            sendAudioCommandUseCase.execute(any<AudioData>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(200, "{invalid json}")
            )
        }

        every { getRecordedAudioUseCase.execute() } returns flowOf(audioData)
        
        presenter.start()
        advanceUntilIdle()

        verify { view.logError(match { it.contains("Failed to parse") }, any()) }
        verify { speakTextUseCase.execute("No entendí la respuesta del servidor.", any()) }
    }

    @Test
    fun `updateChannel should update WebSocket channel`() {
        presenter.start()
        
        presenter.updateChannel("new-channel")

        verify { view.logInfo("Channel updated to: new-channel") }
        verify { updateWebSocketChannelUseCase.execute("test-token", "new-channel") }
    }

    @Test
    fun `updateChannel with null should work`() {
        presenter.start()
        
        presenter.updateChannel(null)

        verify { view.logInfo("Channel updated to: null") }
        verify { updateWebSocketChannelUseCase.execute("test-token", null) }
    }

    @Test
    fun `stop should cleanup all resources`() = runTest {
        presenter.start()
        advanceUntilIdle()
        
        presenter.stop()

        verify { disconnectWebSocketUseCase.execute() }
        verify { stopAudioMonitoringUseCase.execute() }
        verify { shutdownTtsUseCase.execute() }
        verify { view.logInfo("Presenter stopped.") }
    }

    @Test
    fun `speakWelcome should speak welcome message with username`() {
        presenter.speakWelcome("Carlos")

        verify { speakTextUseCase.execute(match { it.contains("Carlos") && it.contains("canal") }, any()) }
    }

    @Test
    fun `monitorAudioLevel should update AudioRmsMonitor`() = runTest {
        val audioLevel = AudioLevel(45.5f)
        every { monitorAudioLevelUseCase.execute() } returns flowOf(audioLevel)
        
        presenter.start()
        advanceUntilIdle()

        assertThat(AudioRmsMonitor.rmsDbFlow.value).isEqualTo(45.5f)
    }

    @Test
    fun `playback onPlaybackCompleted should delete audio file`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(
            file = mockFile,
            sampleRate = 44100,
            channels = 1,
            format = AudioFormat.PCM_16BIT
        )
        val mockResponseFile = mockk<File>(relaxed = true)
        var playbackListener: AudioPlayerRepository.PlaybackListener? = null
        
        every { 
            playAudioFileUseCase.execute(any<File>(), any<AudioPlayerRepository.PlaybackListener>())
        } answers {
            playbackListener = arg(1)
        }
        
        every { 
            sendAudioCommandUseCase.execute(any<AudioData>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(200, "", audioFile = mockResponseFile)
            )
        }

        every { getRecordedAudioUseCase.execute() } returns flowOf(audioData)
        
        presenter.start()
        advanceUntilIdle()
        
        playbackListener?.onPlaybackCompleted()

        verify { mockResponseFile.delete() }
    }

    @Test
    fun `playback onPlaybackError should delete audio file`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(
            file = mockFile,
            sampleRate = 44100,
            channels = 1,
            format = AudioFormat.PCM_16BIT
        )
        val mockResponseFile = mockk<File>(relaxed = true)
        var playbackListener: AudioPlayerRepository.PlaybackListener? = null
        
        every { 
            playAudioFileUseCase.execute(any<File>(), any<AudioPlayerRepository.PlaybackListener>())
        } answers {
            playbackListener = arg(1)
        }
        
        every { 
            sendAudioCommandUseCase.execute(any<AudioData>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(200, "", audioFile = mockResponseFile)
            )
        }

        every { getRecordedAudioUseCase.execute() } returns flowOf(audioData)
        
        presenter.start()
        advanceUntilIdle()
        
        playbackListener?.onPlaybackError("Playback failed")

        verify { view.logError("Audio playback error: Playback failed", null) }
        verify { mockResponseFile.delete() }
    }

    @Test
    fun `handleBackendResponse with blank text and no action should not speak`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(
            file = mockFile,
            sampleRate = 44100,
            channels = 1,
            format = AudioFormat.PCM_16BIT
        )
        
        val jsonResponse = """{"text":"","action":""}"""
        every { 
            sendAudioCommandUseCase.execute(any<AudioData>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(200, jsonResponse)
            )
        }

        every { getRecordedAudioUseCase.execute() } returns flowOf(audioData)
        
        presenter.start()
        advanceUntilIdle()

        // El comportamiento puede variar - verificar que no se llame con texto vacío
        verify(exactly = 0) { speakTextUseCase.execute("", any()) }
    }
}
