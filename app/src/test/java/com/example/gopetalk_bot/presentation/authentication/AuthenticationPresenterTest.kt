package com.example.gopetalk_bot.presentation.authentication

import android.content.Context
import android.os.Handler
import com.example.gopetalk_bot.data.datasources.local.SpeechRecognizerDataSource
import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.PermissionStatus
import com.example.gopetalk_bot.domain.usecases.*
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthenticationPresenterTest {

    private lateinit var presenter: AuthenticationPresenter
    private lateinit var view: AuthenticationContract.View
    private lateinit var speechRecognizerDataSource: SpeechRecognizerDataSource
    private lateinit var sendAuthenticationUseCase: SendAuthenticationUseCase
    private lateinit var speakTextUseCase: SpeakTextUseCase
    private lateinit var setTtsListenerUseCase: SetTtsListenerUseCase
    private lateinit var shutdownTtsUseCase: ShutdownTtsUseCase
    private lateinit var userPreferences: UserPreferences
    private lateinit var checkPermissionsUseCase: CheckPermissionsUseCase
    private lateinit var mockHandler: Handler

    private val onTtsStartSlot = slot<(String?) -> Unit>()
    private val onTtsDoneSlot = slot<(String?) -> Unit>()
    private val onTtsErrorSlot = slot<(String?) -> Unit>()

    @Before
    fun setup() {
        
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
        
        speechRecognizerDataSource = mockk(relaxed = true)
        sendAuthenticationUseCase = mockk(relaxed = true)
        speakTextUseCase = mockk(relaxed = true)
        shutdownTtsUseCase = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        checkPermissionsUseCase = mockk(relaxed = true)
        
        setTtsListenerUseCase = mockk(relaxed = true)
        every { 
            setTtsListenerUseCase.execute(
                onStart = capture(onTtsStartSlot),
                onDone = capture(onTtsDoneSlot),
                onError = capture(onTtsErrorSlot)
            )
        } just Runs

        presenter = AuthenticationPresenter(
            view = view,
            speechRecognizerDataSource = speechRecognizerDataSource,
            sendAuthenticationUseCase = sendAuthenticationUseCase,
            speakTextUseCase = speakTextUseCase,
            setTtsListenerUseCase = setTtsListenerUseCase,
            shutdownTtsUseCase = shutdownTtsUseCase,
            userPreferences = userPreferences,
            checkPermissionsUseCase = checkPermissionsUseCase,
            mainThreadHandler = mockHandler
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onViewCreated should start when all permissions granted`() {
        val permissionStatus = PermissionStatus(
            allGranted = true,
            permissions = emptyList()
        )
        every { checkPermissionsUseCase.execute() } returns permissionStatus

        presenter.onViewCreated()

        verify { view.logInfo("Authentication Presenter started.") }
        verify { setTtsListenerUseCase.execute(any(), any(), any()) }
        verify { speakTextUseCase.execute(any(), any()) }
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
        verify(exactly = 0) { view.logInfo("Authentication Presenter started.") }
    }

    @Test
    fun `onPermissionsResult should start when all granted`() {
        presenter.onPermissionsResult(allGranted = true)

        verify { view.logInfo("Authentication Presenter started.") }
        verify { setTtsListenerUseCase.execute(any(), any(), any()) }
    }

    @Test
    fun `onPermissionsResult should show error when not all granted`() {
        presenter.onPermissionsResult(allGranted = false)

        verify { view.showPermissionsRequiredError() }
        verify(exactly = 0) { view.logInfo("Authentication Presenter started.") }
    }

    @Test
    fun `start should setup TTS listeners and start authentication flow`() {
        presenter.start()

        verify { view.logInfo("Authentication Presenter started.") }
        verify { setTtsListenerUseCase.execute(any(), any(), any()) }
        verify { view.logInfo("Starting authentication flow") }
        verify { speakTextUseCase.execute(any(), "auth_ask_name") }
    }

    @Test
    fun `TTS onStart callback should stop listening`() {
        presenter.start()
        
        onTtsStartSlot.captured.invoke(null)

        verify { speechRecognizerDataSource.stopListening() }
    }

    @Test
    fun `TTS onDone callback should start listening`() {
        presenter.start()
        
        onTtsDoneSlot.captured.invoke(null)

        verify { speechRecognizerDataSource.startListening(any(), any()) }
    }

    @Test
    fun `TTS onError callback should log error`() {
        presenter.start()
        
        onTtsErrorSlot.captured.invoke(null)

        verify { view.logError("TTS error.", null) }
    }

    @Test
    fun `handleNameInput should capture name and ask for PIN`() {
        var onResultCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = captureLambda(),
                onError = any()
            )
        } answers {
            onResultCallback = lambda<(String) -> Unit>().captured
        }

        presenter.start()
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("Carlos")

        verify { view.logInfo(match { it.contains("Carlos") }) }
        verify { speakTextUseCase.execute(match { it.contains("Carlos") }, "auth_welcome_name") }
        verify { speakTextUseCase.execute(match { it.contains("PIN") }, "auth_ask_pin") }
    }

    @Test
    fun `handlePinInput should validate and confirm valid PIN`() {
        var onResultCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = captureLambda(),
                onError = any()
            )
        } answers {
            onResultCallback = lambda<(String) -> Unit>().captured
        }

        presenter.start()
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("Carlos")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("1234")

        verify {
            speakTextUseCase.execute(
                match { spokenText -> spokenText.filter(Char::isDigit) == "1234" },
                "auth_confirm_pin"
            )
        }
    }

    @Test
    fun `handlePinInput should reject invalid PIN`() {
        var onResultCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = captureLambda(),
                onError = any()
            )
        } answers {
            onResultCallback = lambda<(String) -> Unit>().captured
        }

        presenter.start()
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("Carlos")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("123")

        verify { view.logError(match { it.contains("Invalid PIN") }, null) }
        verify { speakTextUseCase.execute(match { it.contains("4 dígitos") }, "auth_invalid_pin") }
    }

    @Test
    fun `handlePinConfirmation with yes should send authentication`() {
        var onResultCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = captureLambda(),
                onError = any()
            )
        } answers {
            onResultCallback = lambda<(String) -> Unit>().captured
        }

        val authResponse = """{"message":"Bienvenido","token":"test-token"}"""
        every { 
            sendAuthenticationUseCase.execute(any(), any(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(200, authResponse)
            )
        }

        presenter.start()
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("Carlos")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("1234")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("sí")

        verify { sendAuthenticationUseCase.execute("Carlos", 1234, any()) }
        verify { userPreferences.authToken = "test-token" }
        verify { userPreferences.username = "Carlos" }
        verify { view.navigateToMainActivity() }
    }

    @Test
    fun `handlePinConfirmation with no should retry PIN input`() {
        var onResultCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = captureLambda(),
                onError = any()
            )
        } answers {
            onResultCallback = lambda<(String) -> Unit>().captured
        }

        presenter.start()
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("Carlos")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("1234")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("no")

        verify { view.logInfo("User rejected PIN, asking again") }
        verify(atLeast = 2) { speakTextUseCase.execute(match { it.contains("PIN") }, any()) }
    }

    @Test
    fun `handlePinConfirmation with unclear response should ask for clarification`() {
        var onResultCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = captureLambda(),
                onError = any()
            )
        } answers {
            onResultCallback = lambda<(String) -> Unit>().captured
        }

        presenter.start()
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("Carlos")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("1234")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("maybe")

        verify { speakTextUseCase.execute(match { it.contains("entendí") }, "auth_unclear") }
    }

    @Test
    fun `authentication error 401 should show invalid credentials`() {
        var onResultCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = captureLambda(),
                onError = any()
            )
        } answers {
            onResultCallback = lambda<(String) -> Unit>().captured
        }

        every { 
            sendAuthenticationUseCase.execute(any<String>(), any<Int>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Error("Unauthorized", 401, null)
            )
        }

        presenter.start()
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("Carlos")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("1234")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("sí")

        verify { view.showAuthenticationError(match { it.contains("incorrecto") }) }
        verify { speakTextUseCase.execute(match { it.contains("incorrecto") }, "auth_invalid_credentials") }
    }

    @Test
    fun `authentication error non-401 should retry authentication flow`() {
        var onResultCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = captureLambda(),
                onError = any()
            )
        } answers {
            onResultCallback = lambda<(String) -> Unit>().captured
        }

        every { 
            sendAuthenticationUseCase.execute(any<String>(), any<Int>(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Error("Server error", 500, null)
            )
        }

        presenter.start()
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("Carlos")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("1234")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("sí")

        verify { view.showAuthenticationError("Error en la autenticación") }
        verify { speakTextUseCase.execute(match { it.contains("Error") }, "auth_failed") }
    }

    @Test
    fun `speech error with No match should retry listening`() {
        var onErrorCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = any(),
                onError = captureLambda()
            )
        } answers {
            onErrorCallback = lambda<(String) -> Unit>().captured
        }

        presenter.start()
        onTtsDoneSlot.captured.invoke(null)
        onErrorCallback?.invoke("No match")

        verify { view.logError(match { it.contains("No match") }, null) }
        verify(atLeast = 2) { speechRecognizerDataSource.startListening(any(), any()) }
    }

    @Test
    fun `speech error without No match should speak retry message`() {
        var onErrorCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = any(),
                onError = captureLambda()
            )
        } answers {
            onErrorCallback = lambda<(String) -> Unit>().captured
        }

        presenter.start()
        onTtsDoneSlot.captured.invoke(null)
        onErrorCallback?.invoke("Network error")

        verify { view.logError(match { it.contains("Network error") }, null) }
        verify { speakTextUseCase.execute(match { it.contains("repite") }, "auth_error") }
    }

    @Test
    fun `convertWordsToNumbers should convert Spanish words to digits`() {
        var onResultCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = captureLambda(),
                onError = any()
            )
        } answers {
            onResultCallback = lambda<(String) -> Unit>().captured
        }

        presenter.start()
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("Carlos")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("uno dos tres cuatro")

        verify {
            speakTextUseCase.execute(
                match { spokenText -> spokenText.filter(Char::isDigit) == "1234" },
                "auth_confirm_pin"
            )
        }
    }

    @Test
    fun `stop should release resources`() {
        presenter.stop()

        verify { speechRecognizerDataSource.stopListening() }
        verify { speechRecognizerDataSource.release() }
        verify { shutdownTtsUseCase.execute() }
        verify { view.logInfo("Authentication Presenter stopped.") }
    }

    @Test
    fun `blank name input should be ignored`() {
        var onResultCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = captureLambda(),
                onError = any()
            )
        } answers {
            onResultCallback = lambda<(String) -> Unit>().captured
        }

        presenter.start()
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("   ")

        verify(exactly = 0) { speakTextUseCase.execute(match { it.contains("Bienvenido") }, "auth_welcome_name") }
    }

    @Test
    fun `blank PIN input should be ignored`() {
        var onResultCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = captureLambda(),
                onError = any()
            )
        } answers {
            onResultCallback = lambda<(String) -> Unit>().captured
        }

        presenter.start()
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("Carlos")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("   ")

        verify(exactly = 0) {
            speakTextUseCase.execute(
                match { spokenText -> spokenText.filter(Char::isDigit) == "1234" },
                "auth_confirm_pin"
            )
        }
    }

    @Test
    fun `authentication success with invalid JSON should show error and retry`() {
        var onResultCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = captureLambda(),
                onError = any()
            )
        } answers {
            onResultCallback = lambda<(String) -> Unit>().captured
        }

        every { 
            sendAuthenticationUseCase.execute(any(), any(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(200, "invalid json")
            )
        }

        presenter.start()
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("Carlos")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("1234")
        onTtsDoneSlot.captured.invoke(null)
        onResultCallback?.invoke("sí")

        verify { view.logError(match { it.contains("Error parsing") }, any()) }
        verify { view.showAuthenticationError("Error en la autenticación") }
    }

    @Test
    fun `isConfirmation should recognize various affirmative responses`() {
        var onResultCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = captureLambda(),
                onError = any()
            )
        } answers {
            onResultCallback = lambda<(String) -> Unit>().captured
        }

        val authResponse = """{"message":"Bienvenido","token":"test-token"}"""
        every { 
            sendAuthenticationUseCase.execute(any(), any(), captureLambda())
        } answers {
            lambda<(ApiResponse) -> Unit>().captured.invoke(
                ApiResponse.Success(200, authResponse)
            )
        }

        val affirmativeResponses = listOf("sí", "si", "yes", "correcto", "afirmativo")
        
        affirmativeResponses.forEach { response ->
            clearMocks(sendAuthenticationUseCase, answers = false)
            
            presenter.start()
            onTtsDoneSlot.captured.invoke(null)
            onResultCallback?.invoke("Carlos")
            onTtsDoneSlot.captured.invoke(null)
            onResultCallback?.invoke("1234")
            onTtsDoneSlot.captured.invoke(null)
            onResultCallback?.invoke(response)

            verify { sendAuthenticationUseCase.execute("Carlos", 1234, any()) }
        }
    }
}
