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

    @Test
    fun `handleSpeechResult should process name input`() {
        var capturedOnResult: ((String) -> Unit)? = null
        
        every { speechRecognizerDataSource.startListening(any(), any()) } answers {
            capturedOnResult = firstArg()
        }
        
        presenter.start()
        capturedOnResult?.invoke("Juan")

        verify { speechRecognizerDataSource.stopListening() }
        verify(atLeast = 1) { speakTextUseCase.execute(any(), any()) }
    }

    @Test
    fun `handleSpeechResult should ignore blank name`() {
        var capturedOnResult: ((String) -> Unit)? = null
        
        every { speechRecognizerDataSource.startListening(any(), any()) } answers {
            capturedOnResult = firstArg()
        }
        
        presenter.start()
        val initialCalls = mutableListOf<String>()
        every { speakTextUseCase.execute(any(), any()) } answers {
            initialCalls.add(firstArg())
        }
        
        capturedOnResult?.invoke("")

        verify { speechRecognizerDataSource.stopListening() }
    }

    @Test
    fun `handleSpeechError should retry on No match error`() {
        var capturedOnError: ((String) -> Unit)? = null
        
        every { speechRecognizerDataSource.startListening(any(), any()) } answers {
            capturedOnError = secondArg()
        }
        
        presenter.start()
        capturedOnError?.invoke("No match")

        verify(atLeast = 1) { speechRecognizerDataSource.startListening(any(), any()) }
    }

    @Test
    fun `handleSpeechError should retry on No speech error`() {
        var capturedOnError: ((String) -> Unit)? = null
        
        every { speechRecognizerDataSource.startListening(any(), any()) } answers {
            capturedOnError = secondArg()
        }
        
        presenter.start()
        capturedOnError?.invoke("No speech")

        verify(atLeast = 1) { speechRecognizerDataSource.startListening(any(), any()) }
    }

    @Test
    fun `handleSpeechError should speak retry message on other errors`() {
        var capturedOnError: ((String) -> Unit)? = null
        
        every { speechRecognizerDataSource.startListening(any(), any()) } answers {
            capturedOnError = secondArg()
        }
        
        presenter.start()
        capturedOnError?.invoke("Unknown error")

        verify { speakTextUseCase.execute(any(), "auth_error") }
    }

    @Test
    fun `TTS onStart should stop listening`() {
        var capturedOnStart: (() -> Unit)? = null
        
        every { setTtsListenerUseCase.execute(any(), any(), any()) } answers {
            capturedOnStart = firstArg()
        }
        
        presenter.start()
        capturedOnStart?.invoke()

        verify { speechRecognizerDataSource.stopListening() }
    }

    @Test
    fun `TTS onDone should start listening after delay`() {
        var capturedOnDone: (() -> Unit)? = null
        
        every { setTtsListenerUseCase.execute(any(), any(), any()) } answers {
            capturedOnDone = secondArg()
        }
        
        presenter.start()
        capturedOnDone?.invoke()

        verify(atLeast = 1) { speechRecognizerDataSource.startListening(any(), any()) }
    }

    @Test
    fun `TTS onError should log error`() {
        var capturedOnError: (() -> Unit)? = null
        
        every { setTtsListenerUseCase.execute(any(), any(), any()) } answers {
            capturedOnError = thirdArg()
        }
        
        presenter.start()
        capturedOnError?.invoke()

        verify { view.logError("TTS error.", null) }
    }

    @Test
    fun `sendAuthentication should handle success response`() {
        var capturedCallback: ((ApiResponse) -> Unit)? = null
        
        every { sendAuthenticationUseCase.execute(any(), any(), any()) } answers {
            capturedCallback = thirdArg()
        }
        
        // Simulate full auth flow to reach sendAuthentication
        var onResult: ((String) -> Unit)? = null
        every { speechRecognizerDataSource.startListening(any(), any()) } answers {
            onResult = firstArg()
        }
        
        presenter.start()
        
        // Enter name
        onResult?.invoke("Juan")
        
        // Enter PIN
        onResult?.invoke("1234")
        
        // Confirm PIN
        onResult?.invoke("sí")
        
        // Simulate success response
        capturedCallback?.invoke(ApiResponse.Success(200, "OK", null))

        verify { userPreferences.username = any() }
        verify { userPreferences.authToken = any() }
        verify { view.navigateToMainActivity() }
    }

    @Test
    fun `sendAuthentication should handle 401 error`() {
        var capturedCallback: ((ApiResponse) -> Unit)? = null
        
        every { sendAuthenticationUseCase.execute(any(), any(), any()) } answers {
            capturedCallback = thirdArg()
        }
        
        var onResult: ((String) -> Unit)? = null
        every { speechRecognizerDataSource.startListening(any(), any()) } answers {
            onResult = firstArg()
        }
        
        presenter.start()
        onResult?.invoke("Juan")
        onResult?.invoke("1234")
        onResult?.invoke("sí")
        
        capturedCallback?.invoke(ApiResponse.Error("Unauthorized", 401, null))

        verify { speakTextUseCase.execute(any(), "auth_invalid_credentials") }
    }

    @Test
    fun `sendAuthentication should handle generic error`() {
        var capturedCallback: ((ApiResponse) -> Unit)? = null
        
        every { sendAuthenticationUseCase.execute(any(), any(), any()) } answers {
            capturedCallback = thirdArg()
        }
        
        var onResult: ((String) -> Unit)? = null
        every { speechRecognizerDataSource.startListening(any(), any()) } answers {
            onResult = firstArg()
        }
        
        presenter.start()
        onResult?.invoke("Juan")
        onResult?.invoke("1234")
        onResult?.invoke("sí")
        
        capturedCallback?.invoke(ApiResponse.Error("Server error", 500, null))

        verify { speakTextUseCase.execute(any(), "auth_error") }
    }

    @Test
    fun `handlePinInput should reject invalid PIN`() {
        var onResult: ((String) -> Unit)? = null
        every { speechRecognizerDataSource.startListening(any(), any()) } answers {
            onResult = firstArg()
        }
        
        presenter.start()
        onResult?.invoke("Juan")
        onResult?.invoke("123") // Invalid PIN (only 3 digits)

        verify { speakTextUseCase.execute(any(), "auth_invalid_pin") }
    }

    @Test
    fun `handlePinInput should accept valid PIN`() {
        var onResult: ((String) -> Unit)? = null
        every { speechRecognizerDataSource.startListening(any(), any()) } answers {
            onResult = firstArg()
        }
        
        presenter.start()
        onResult?.invoke("Juan")
        onResult?.invoke("1234")

        verify { speakTextUseCase.execute(any(), "auth_confirm_pin") }
    }

    @Test
    fun `handlePinConfirmation should handle no response`() {
        var onResult: ((String) -> Unit)? = null
        every { speechRecognizerDataSource.startListening(any(), any()) } answers {
            onResult = firstArg()
        }
        
        presenter.start()
        onResult?.invoke("Juan")
        onResult?.invoke("1234")
        onResult?.invoke("no")

        verify { speakTextUseCase.execute(any(), "auth_ask_pin") }
    }

    @Test
    fun `handlePinConfirmation should handle unclear response`() {
        var onResult: ((String) -> Unit)? = null
        every { speechRecognizerDataSource.startListening(any(), any()) } answers {
            onResult = firstArg()
        }
        
        presenter.start()
        onResult?.invoke("Juan")
        onResult?.invoke("1234")
        onResult?.invoke("maybe")

        verify { speakTextUseCase.execute(any(), "auth_unclear") }
    }
}
