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


class AuthenticationPresenterEdgeCasesTest {

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
    fun `handlePinInput should accept 4-digit PIN with leading zeros`() {
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
        onResultCallback?.invoke("0123")

        verify {
            speakTextUseCase.execute(
                match { spokenText -> spokenText.filter(Char::isDigit) == "0123" },
                "auth_confirm_pin"
            )
        }
    }

    @Test
    fun `handlePinInput should reject PIN with 3 digits`() {
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
    fun `handlePinInput should reject PIN with 5 digits`() {
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
        onResultCallback?.invoke("12345")

        verify { view.logError(match { it.contains("Invalid PIN") }, null) }
    }

    @Test
    fun `handlePinInput should use first 4 digits when transcription has multiple PINs`() {
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
        onResultCallback?.invoke("9999 2222 3333 4444")

        verify {
            speakTextUseCase.execute(
                match { spokenText -> spokenText.filter(Char::isDigit) == "4444" },
                "auth_confirm_pin"
            )
        }
    }

    @Test
    fun `handlePinInput should accept repeated PIN even if final attempt is incomplete`() {
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
        onResultCallback?.invoke("4444 4444 444")

        verify {
            speakTextUseCase.execute(
                match { spokenText -> spokenText.filter(Char::isDigit) == "4444" },
                "auth_confirm_pin"
            )
        }
    }

    @Test
    fun `handlePinInput should accept repeated Spanish number words for each digit`() {
        var onResultCallback: ((String) -> Unit)? = null
        every { 
            speechRecognizerDataSource.startListening(
                onResult = captureLambda(),
                onError = any()
            )
        } answers {
            onResultCallback = lambda<(String) -> Unit>().captured
        }

        val cases = listOf(
            "uno" to "1111",
            "dos" to "2222",
            "tres" to "3333",
            "cuatro" to "4444",
            "cinco" to "5555",
            "seis" to "6666",
            "siete" to "7777",
            "ocho" to "8888",
            "nueve" to "9999"
        )

        cases.forEach { (word, expectedDigits) ->
            onResultCallback = null
            clearMocks(speakTextUseCase, view, answers = false)

            presenter.start()
            onTtsDoneSlot.captured.invoke(null)
            onResultCallback?.invoke("Carlos")
            onTtsDoneSlot.captured.invoke(null)
            onResultCallback?.invoke(List(4) { word }.joinToString(" "))

            verify(exactly = 1) {
                speakTextUseCase.execute(
                    match { spokenText -> spokenText.filter(Char::isDigit) == expectedDigits },
                    "auth_confirm_pin"
                )
            }
        }
    }

    

    @Test
    fun `convertWordsToNumbers should handle mixed Spanish and digits`() {
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
        onResultCallback?.invoke("uno 2 tres 4")

        verify {
            speakTextUseCase.execute(
                match { spokenText -> spokenText.filter(Char::isDigit) == "1234" },
                "auth_confirm_pin"
            )
        }
    }

    @Test
    fun `convertWordsToNumbers should handle uppercase Spanish words`() {
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
        onResultCallback?.invoke("UNO DOS TRES CUATRO")

        verify {
            speakTextUseCase.execute(
                match { spokenText -> spokenText.filter(Char::isDigit) == "1234" },
                "auth_confirm_pin"
            )
        }
    }

    
    
    
    
    

    

    @Test
    fun `handlePinConfirmation should accept 'si' without accent`() {
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
        onResultCallback?.invoke("si")

        verify { sendAuthenticationUseCase.execute("Carlos", 1234, any()) }
    }

    @Test
    fun `handlePinConfirmation should accept 'yes'`() {
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
        onResultCallback?.invoke("yes")

        verify { sendAuthenticationUseCase.execute("Carlos", 1234, any()) }
    }

    @Test
    fun `handlePinConfirmation should accept 'correcto'`() {
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
        onResultCallback?.invoke("correcto")

        verify { sendAuthenticationUseCase.execute("Carlos", 1234, any()) }
    }

    @Test
    fun `handlePinConfirmation should handle empty response`() {
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
        onResultCallback?.invoke("")

        verify { speakTextUseCase.execute(match { it.contains("entendí") }, "auth_unclear") }
    }

    

    
    
    
    
    

    @Test
    fun `authentication should handle empty response body`() {
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
                ApiResponse.Success(200, "")
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
    }

    

    @Test
    fun `speech error with 'No speech' should retry`() {
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
        onErrorCallback?.invoke("No speech input")

        verify { view.logError(match { it.contains("No speech") }, null) }
        verify(atLeast = 2) { speechRecognizerDataSource.startListening(any(), any()) }
    }

    @Test
    fun `speech error with 'Network' should speak retry message`() {
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
    fun `multiple start calls should be idempotent`() {
        presenter.start()
        presenter.start()
        presenter.start()

        verify(exactly = 3) { view.logInfo("Authentication Presenter started.") }
    }

    @Test
    fun `stop should be callable multiple times`() {
        presenter.stop()
        presenter.stop()
        presenter.stop()

        verify(atLeast = 3) { shutdownTtsUseCase.execute() }
    }

    @Test
    fun `onViewCreated with partial permissions should request missing ones`() {
        val permissions = listOf("android.permission.RECORD_AUDIO", "android.permission.POST_NOTIFICATIONS")
        val permissionStatus = PermissionStatus(
            allGranted = false,
            permissions = permissions
        )
        every { checkPermissionsUseCase.execute() } returns permissionStatus

        presenter.onViewCreated()

        verify { view.requestPermissions(permissions.toTypedArray()) }
    }

    

    @Test
    fun `presenter constants should be properly defined`() {
        
        presenter.start()
        
        verify { speakTextUseCase.execute(match { it.contains("nombre") }, "auth_ask_name") }
    }
}
