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

/**
 * Tests adicionales para aumentar cobertura de branch y line
 * en VoiceInteractionPresenter
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceInteractionPresenterEdgeCasesTest {

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

    // ==================== Backend Response Edge Cases ====================

    @Test
    fun `handleBackendResponse with empty text should not speak`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        
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

        verify(exactly = 0) { speakTextUseCase.execute("", any()) }
        
        presenter.stop()
    }

    @Test
    fun `handleBackendResponse with message should speak it`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        
        val jsonResponse = """{"message":"Conectado al canal 1","text":"","action":""}"""
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

        verify { speakTextUseCase.execute("Conectado al canal 1", any()) }
        
        presenter.stop()
    }

    @Test
    fun `handleBackendResponse with whitespace text should not speak`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        
        val jsonResponse = """{"text":"   ","action":""}"""
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

        verify(exactly = 0) { speakTextUseCase.execute(match { it.isBlank() }, any()) }
        
        presenter.stop()
    }

    // Test comentado - requiere manejo específico de JSON con null
    // @Test
    // fun `handleBackendResponse with channel null should update to null`() = runTest {
    //     // Test que valida actualización de canal a null
    // }

    @Test
    fun `handleBackendResponse with empty channels list should speak empty message`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        
        val jsonResponse = """{"action":"list_channels","channels":[],"text":""}"""
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

        verify { speakTextUseCase.execute(match { it.contains("canales") || it.contains("disponibles") }, any()) }
        
        presenter.stop()
    }

    @Test
    fun `handleBackendResponse with empty users list should speak empty message`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        
        val jsonResponse = """{"action":"list_users","users":[],"text":""}"""
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

        verify { speakTextUseCase.execute(match { it.contains("usuarios") || it.contains("conectados") }, any()) }
        
        presenter.stop()
    }

    // ==================== TTS Listener Edge Cases ====================

    @Test
    fun `TTS onStart with utteranceId should pause recording`() {
        presenter.start()
        
        onTtsStartSlot.captured.invoke("test-utterance-id")

        verify { pauseAudioRecordingUseCase.execute() }
        
        presenter.stop()
    }

    @Test
    fun `TTS onDone with utteranceId should resume recording`() {
        presenter.start()
        
        onTtsDoneSlot.captured.invoke("test-utterance-id")

        verify { resumeAudioRecordingUseCase.execute() }
        
        presenter.stop()
    }

    @Test
    fun `TTS onError with utteranceId should resume recording`() {
        presenter.start()
        
        onTtsErrorSlot.captured.invoke("test-utterance-id")

        verify { resumeAudioRecordingUseCase.execute() }
        
        presenter.stop()
    }

    // ==================== WebSocket Edge Cases ====================

    @Test
    fun `WebSocket onConnectionEstablished multiple times should log each time`() {
        presenter.start()
        
        webSocketListenerSlot.captured.onConnectionEstablished()
        webSocketListenerSlot.captured.onConnectionEstablished()
        webSocketListenerSlot.captured.onConnectionEstablished()

        verify(exactly = 3) { view.logInfo("WebSocket connection established") }
        
        presenter.stop()
    }

    @Test
    fun `WebSocket onConnectionClosed multiple times should log each time`() {
        presenter.start()
        
        webSocketListenerSlot.captured.onConnectionClosed()
        webSocketListenerSlot.captured.onConnectionClosed()

        verify(exactly = 2) { view.logInfo("WebSocket connection closed") }
        
        presenter.stop()
    }

    @Test
    fun `WebSocket onError with empty message should still log`() {
        presenter.start()
        
        webSocketListenerSlot.captured.onError("")

        verify { view.logError("WebSocket error: ", null) }
        
        presenter.stop()
    }

    @Test
    fun `WebSocket onError with long message should log full message`() {
        presenter.start()
        
        val longError = "This is a very long error message that contains multiple words and describes a complex error scenario"
        webSocketListenerSlot.captured.onError(longError)

        verify { view.logError("WebSocket error: $longError", null) }
        
        presenter.stop()
    }

    // ==================== Audio Playback Edge Cases ====================

    @Test
    fun `playback onPlaybackCompleted should delete file even if delete fails`() = runTest {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        val mockResponseFile = mockk<File>(relaxed = true)
        every { mockResponseFile.delete() } returns false
        
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
        
        presenter.stop()
    }

    // ==================== Lifecycle Edge Cases ====================

    @Test
    fun `multiple start calls should be handled gracefully`() = runTest {
        presenter.start()
        advanceUntilIdle()
        presenter.start()
        advanceUntilIdle()
        presenter.start()
        advanceUntilIdle()

        verify(atLeast = 3) { view.logInfo("Presenter started.") }
        
        presenter.stop()
    }

    @Test
    fun `stop without start should not throw exception`() {
        presenter.stop()

        verify { disconnectWebSocketUseCase.execute() }
    }

    @Test
    fun `multiple stop calls should be idempotent`() {
        presenter.start()
        presenter.stop()
        presenter.stop()
        presenter.stop()

        verify(atLeast = 3) { disconnectWebSocketUseCase.execute() }
    }

    // ==================== Channel Update Edge Cases ====================

    @Test
    fun `updateChannel with empty string should update to empty`() {
        presenter.start()
        
        presenter.updateChannel("")

        verify { view.logInfo("Channel updated to: ") }
        verify { updateWebSocketChannelUseCase.execute("test-token", "") }
        
        presenter.stop()
    }

    @Test
    fun `updateChannel with special characters should work`() {
        presenter.start()
        
        presenter.updateChannel("test-channel-123_@#")

        verify { view.logInfo("Channel updated to: test-channel-123_@#") }
        verify { updateWebSocketChannelUseCase.execute("test-token", "test-channel-123_@#") }
        
        presenter.stop()
    }

    // ==================== Welcome Message Edge Cases ====================

    @Test
    fun `speakWelcome with empty username should still speak`() {
        presenter.speakWelcome("")

        verify { speakTextUseCase.execute(match { it.contains("canal") }, any()) }
    }

    @Test
    fun `speakWelcome with long username should speak full name`() {
        val longName = "Carlos Alberto Rodriguez Martinez"
        presenter.speakWelcome(longName)

        verify { speakTextUseCase.execute(match { it.contains(longName) }, any()) }
    }

    @Test
    fun `speakWelcome with special characters should work`() {
        presenter.speakWelcome("José María")

        verify { speakTextUseCase.execute(match { it.contains("José María") }, any()) }
    }

    // ==================== Audio Level Monitoring Edge Cases ====================

    @Test
    fun `monitorAudioLevel should handle zero rmsDb`() = runTest {
        val audioLevel = AudioLevel(0.0f)
        every { monitorAudioLevelUseCase.execute() } returns flowOf(audioLevel)
        
        presenter.start()
        advanceUntilIdle()

        assertThat(AudioRmsMonitor.rmsDbFlow.value).isEqualTo(0.0f)
        
        presenter.stop()
    }

    @Test
    fun `monitorAudioLevel should handle negative rmsDb`() = runTest {
        val audioLevel = AudioLevel(-10.0f)
        every { monitorAudioLevelUseCase.execute() } returns flowOf(audioLevel)
        
        presenter.start()
        advanceUntilIdle()

        assertThat(AudioRmsMonitor.rmsDbFlow.value).isEqualTo(-10.0f)
        
        presenter.stop()
    }

    @Test
    fun `monitorAudioLevel should handle very high rmsDb`() = runTest {
        val audioLevel = AudioLevel(999.9f)
        every { monitorAudioLevelUseCase.execute() } returns flowOf(audioLevel)
        
        presenter.start()
        advanceUntilIdle()

        assertThat(AudioRmsMonitor.rmsDbFlow.value).isEqualTo(999.9f)
        
        presenter.stop()
    }
}
