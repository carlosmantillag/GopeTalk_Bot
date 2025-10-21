package com.example.gopetalk_bot.presentation.authentication

import android.os.Handler
import android.os.Looper
import com.example.gopetalk_bot.data.datasources.local.SpeechRecognizerDataSource
import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.PermissionStatus
import com.example.gopetalk_bot.domain.usecases.*
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthenticationPresenterTest {

    private lateinit var view: AuthenticationContract.View
    private lateinit var speechRecognizerDataSource: SpeechRecognizerDataSource
    private lateinit var sendAuthenticationUseCase: SendAuthenticationUseCase
    private lateinit var speakTextUseCase: SpeakTextUseCase
    private lateinit var setTtsListenerUseCase: SetTtsListenerUseCase
    private lateinit var shutdownTtsUseCase: ShutdownTtsUseCase
    private lateinit var userPreferences: UserPreferences
    private lateinit var checkPermissionsUseCase: CheckPermissionsUseCase
    private lateinit var presenter: AuthenticationPresenter

    @Before
    fun setup() {
        // Mock Android Looper for unit testing
        mockkStatic(Looper::class)
        val mockLooper = mockk<Looper>(relaxed = true)
        every { Looper.getMainLooper() } returns mockLooper
        
        // Mock Handler to execute runnables immediately for testing
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
        speechRecognizerDataSource = mockk(relaxed = true)
        sendAuthenticationUseCase = mockk(relaxed = true)
        speakTextUseCase = mockk(relaxed = true)
        setTtsListenerUseCase = mockk(relaxed = true)
        shutdownTtsUseCase = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        checkPermissionsUseCase = mockk(relaxed = true)

        presenter = AuthenticationPresenter(
            view,
            speechRecognizerDataSource,
            sendAuthenticationUseCase,
            speakTextUseCase,
            setTtsListenerUseCase,
            shutdownTtsUseCase,
            userPreferences,
            checkPermissionsUseCase
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Looper::class)
        unmockkConstructor(Handler::class)
        clearAllMocks()
    }

    @Test
    fun `start should setup TTS listeners`() {
        presenter.start()

        verify { setTtsListenerUseCase.execute(any(), any(), any()) }
    }

    @Test
    fun `start should log info`() {
        presenter.start()

        verify { view.logInfo(any()) }
    }

    @Test
    fun `start should speak welcome message`() {
        presenter.start()

        verify { speakTextUseCase.execute(any(), any()) }
    }

    @Test
    fun `stop should shutdown TTS`() {
        presenter.stop()

        verify { shutdownTtsUseCase.execute() }
    }

    @Test
    fun `stop should stop listening`() {
        presenter.stop()

        verify { speechRecognizerDataSource.stopListening() }
    }

    @Test
    fun `onViewCreated should start when all permissions granted`() {
        val permissionStatus = PermissionStatus(
            allGranted = true,
            permissions = listOf("android.permission.RECORD_AUDIO")
        )
        every { checkPermissionsUseCase.execute() } returns permissionStatus

        presenter.onViewCreated()

        verify { setTtsListenerUseCase.execute(any(), any(), any()) }
        verify(exactly = 0) { view.requestPermissions(any()) }
    }

    @Test
    fun `onViewCreated should request permissions when not all granted`() {
        val permissions = listOf("android.permission.RECORD_AUDIO")
        val permissionStatus = PermissionStatus(
            allGranted = false,
            permissions = permissions
        )
        every { checkPermissionsUseCase.execute() } returns permissionStatus

        presenter.onViewCreated()

        verify { view.requestPermissions(permissions.toTypedArray()) }
        verify(exactly = 0) { setTtsListenerUseCase.execute(any(), any(), any()) }
    }

    @Test
    fun `onPermissionsResult should start when all granted`() {
        presenter.onPermissionsResult(true)

        verify { setTtsListenerUseCase.execute(any(), any(), any()) }
    }

    @Test
    fun `onPermissionsResult should show error when not all granted`() {
        presenter.onPermissionsResult(false)

        verify { view.showPermissionsRequiredError() }
        verify(exactly = 0) { setTtsListenerUseCase.execute(any(), any(), any()) }
    }
}
