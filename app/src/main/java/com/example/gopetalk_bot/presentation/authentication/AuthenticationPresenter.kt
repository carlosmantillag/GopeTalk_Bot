package com.example.gopetalk_bot.presentation.authentication

import android.os.Handler
import android.os.Looper
import com.example.gopetalk_bot.data.datasources.local.SpeechRecognizerDataSource
import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.data.datasources.remote.dto.AuthenticationResponse
import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.usecases.*
import com.google.gson.Gson

class AuthenticationPresenter(
    private val view: AuthenticationContract.View,
    private val speechRecognizerDataSource: SpeechRecognizerDataSource,
    private val sendAuthenticationUseCase: SendAuthenticationUseCase,
    private val speakTextUseCase: SpeakTextUseCase,
    private val setTtsListenerUseCase: SetTtsListenerUseCase,
    private val shutdownTtsUseCase: ShutdownTtsUseCase,
    private val userPreferences: UserPreferences,
    private val checkPermissionsUseCase: CheckPermissionsUseCase,
    private val mainThreadHandler: Handler = Handler(Looper.getMainLooper())
) : AuthenticationContract.Presenter {

    private companion object {
        const val DELAY_AFTER_TTS = 50L
        const val DELAY_BEFORE_PIN = 50L
        const val DELAY_RETRY_LISTENING = 50L
        const val DELAY_NAVIGATION = 1000L
        const val PIN_LENGTH = 4
        
        const val MSG_WELCOME = "Bienvenido Usuario, ¿cuál es tu nombre?"
        const val MSG_WELCOME_NAME = "Bienvenido"
        const val MSG_ASK_PIN = "Dame un PIN de 4 dígitos por favor para poder entrar, no lo olvides"
        const val MSG_CONFIRM_PIN = "El PIN es %s, ¿me confirmas?"
        const val MSG_INVALID_PIN = "El PIN debe ser de 4 dígitos numéricos. Por favor, repite el PIN"
        const val MSG_UNCLEAR = "No entendí, por favor di sí o no"
        const val MSG_RETRY = "No pude entender, por favor repite"
        const val MSG_AUTH_ERROR = "Error en la autenticación, por favor intenta de nuevo"
        const val MSG_INVALID_CREDENTIALS = "Usuario ya registrado, PIN incorrecto"
        
        const val HTTP_UNAUTHORIZED = 401
        private val NUMBER_WORD_MAP = mapOf(
            "cero" to "0",
            "uno" to "1",
            "dos" to "2",
            "tres" to "3",
            "cuatro" to "4",
            "cinco" to "5",
            "seis" to "6",
            "siete" to "7",
            "ocho" to "8",
            "nueve" to "9"
        )
        private val PIN_CONTIGUOUS_REGEX = Regex("\\b\\d{$PIN_LENGTH}\\b")
    }

    private enum class AuthState {
        WAITING_FOR_NAME,
        WAITING_FOR_PIN,
        WAITING_FOR_PIN_CONFIRMATION,
        AUTHENTICATED
    }
    
    private var authState: AuthState = AuthState.WAITING_FOR_NAME
    private var userName: String? = null
    private var userPin: Int? = null
    private var isTtsSpeaking = false
    private val gson = Gson()

    override fun onViewCreated() {
        val permissionStatus = checkPermissionsUseCase.execute()
        
        if (permissionStatus.allGranted) {
            start()
        } else {
            view.requestPermissions(permissionStatus.permissions.toTypedArray())
        }
    }

    override fun onPermissionsResult(allGranted: Boolean) {
        if (allGranted) {
            start()
        } else {
            view.showPermissionsRequiredError()
        }
    }

    override fun start() {
        view.logInfo("Authentication Presenter started.")
        setupTtsListeners()
        startAuthenticationFlow()
    }

    private fun setupTtsListeners() {
        setTtsListenerUseCase.execute(
            onStart = { handleTtsStart() },
            onDone = { handleTtsDone() },
            onError = { handleTtsError() }
        )
    }

    private fun handleTtsStart() {
        isTtsSpeaking = true
        speechRecognizerDataSource.stopListening()
    }

    private fun handleTtsDone() {
        isTtsSpeaking = false
        mainThreadHandler.postDelayed({
            if (!isTtsSpeaking) startListening()
        }, DELAY_AFTER_TTS)
    }

    private fun handleTtsError() {
        view.logError("TTS error.", null)
        isTtsSpeaking = false
    }
    
    private fun startAuthenticationFlow() {
        authState = AuthState.WAITING_FOR_NAME
        view.logInfo("Starting authentication flow")
        speak(MSG_WELCOME, "auth_ask_name")
    }

    private fun speak(text: String, utteranceId: String) {
        speakTextUseCase.execute(text, utteranceId)
    }
    
    private fun startListening() {
        if (isTtsSpeaking) return
        
        view.logInfo("Starting speech recognition for state: $authState")
        speechRecognizerDataSource.startListening(
            onResult = { handleSpeechResult(it) },
            onError = { handleSpeechError(it) }
        )
    }

    private fun handleSpeechResult(recognizedText: String) {
        view.logInfo("Speech recognized: $recognizedText")
        speechRecognizerDataSource.stopListening()
        handleAuthenticationResponse(recognizedText)
    }

    private fun handleSpeechError(error: String) {
        view.logError("Speech recognition error: $error", null)
        if (shouldRetryListening(error)) {
            retryListening()
        } else {
            speak(MSG_RETRY, "auth_error")
        }
    }

    private fun shouldRetryListening(error: String): Boolean {
        return error.contains("No match") || error.contains("No speech")
    }

    private fun retryListening() {
        mainThreadHandler.postDelayed({
            if (!isTtsSpeaking) startListening()
        }, DELAY_RETRY_LISTENING)
    }
    
    private fun handleAuthenticationResponse(transcription: String) {
        when (authState) {
            AuthState.WAITING_FOR_NAME -> handleNameInput(transcription)
            AuthState.WAITING_FOR_PIN -> handlePinInput(transcription)
            AuthState.WAITING_FOR_PIN_CONFIRMATION -> handlePinConfirmation(transcription)
            AuthState.AUTHENTICATED -> view.logInfo("Already authenticated, ignoring")
        }
    }

    private fun handleNameInput(name: String) {
        if (name.isBlank()) return
        
        userName = name
        view.logInfo("User name captured: $userName")
        speak("$MSG_WELCOME_NAME $userName", "auth_welcome_name")
        
        mainThreadHandler.postDelayed({
            authState = AuthState.WAITING_FOR_PIN
            speak(MSG_ASK_PIN, "auth_ask_pin")
        }, DELAY_BEFORE_PIN)
    }

    private fun handlePinInput(transcription: String) {
        if (transcription.isBlank()) return
        
        val rawPin = convertWordsToNumbers(transcription)
        val candidatePin = if (isValidPin(rawPin)) {
            rawPin
        } else {
            extractPinDigits(transcription)
        }

        view.logInfo(
            "PIN captured: $transcription -> raw=$rawPin candidate=${candidatePin ?: "none"}"
        )
        
        if (candidatePin != null && isValidPin(candidatePin)) {
            userPin = candidatePin.toInt()
            val pinWithSpaces = candidatePin.toCharArray().joinToString(" ")
            authState = AuthState.WAITING_FOR_PIN_CONFIRMATION
            speak(String.format(MSG_CONFIRM_PIN, pinWithSpaces), "auth_confirm_pin")
        } else {
            view.logError("Invalid PIN format: $rawPin", null)
            speak(MSG_INVALID_PIN, "auth_invalid_pin")
        }
    }

    private fun isValidPin(pin: String): Boolean {
        return pin.toIntOrNull() != null && pin.length == PIN_LENGTH
    }

    private fun handlePinConfirmation(response: String) {
        val lowerResponse = response.lowercase()
        view.logInfo("Confirmation response: $lowerResponse")
        
        when {
            isConfirmation(lowerResponse) -> sendAuthentication()
            isRejection(lowerResponse) -> retryPinInput()
            else -> speak(MSG_UNCLEAR, "auth_unclear")
        }
    }

    private fun isConfirmation(response: String): Boolean {
        return response.contains("sí") || response.contains("si") || 
               response.contains("yes") || response.contains("correcto") || 
               response.contains("afirmativo")
    }

    private fun isRejection(response: String): Boolean {
        return response.contains("no")
    }

    private fun retryPinInput() {
        view.logInfo("User rejected PIN, asking again")
        authState = AuthState.WAITING_FOR_PIN
        speak(MSG_ASK_PIN, "auth_ask_pin_retry")
    }
    
    private fun sendAuthentication() {
        val name = userName ?: return
        val pin = userPin ?: return
        
        view.logInfo("Sending authentication: name=$name, pin=$pin")
        
        sendAuthenticationUseCase.execute(name, pin) { response ->
            mainThreadHandler.post {
                when (response) {
                    is ApiResponse.Success -> handleAuthSuccess(response, name)
                    is ApiResponse.Error -> handleAuthError(response)
                }
            }
        }
    }

    private fun handleAuthSuccess(response: ApiResponse.Success, name: String) {
        try {
            val authResponse = gson.fromJson(response.body, AuthenticationResponse::class.java)
            saveUserCredentials(name, authResponse.token)
            authState = AuthState.AUTHENTICATED
            speak(authResponse.message, "auth_success")
            navigateToMainDelayed()
        } catch (e: Exception) {
            view.logError("Error parsing auth response: ${e.message}", e)
            showAuthErrorAndRetry()
        }
    }

    private fun saveUserCredentials(name: String, token: String) {
        userPreferences.authToken = token
        userPreferences.username = name
        view.logInfo("Token saved")
    }

    private fun navigateToMainDelayed() {
        mainThreadHandler.postDelayed({
            view.navigateToMainActivity()
        }, DELAY_NAVIGATION)
    }

    private fun handleAuthError(response: ApiResponse.Error) {
        view.logError("Authentication failed: ${response.message}", response.exception)
        
        if (response.statusCode == HTTP_UNAUTHORIZED) {
            showInvalidCredentialsError()
        } else {
            showAuthErrorAndRetry()
        }
    }

    private fun showInvalidCredentialsError() {
        view.showAuthenticationError(MSG_INVALID_CREDENTIALS)
        speak(MSG_INVALID_CREDENTIALS, "auth_invalid_credentials")
        mainThreadHandler.postDelayed({
            retryPinInput()
        }, DELAY_NAVIGATION)
    }

    private fun showAuthErrorAndRetry() {
        view.showAuthenticationError("Error en la autenticación")
        speak(MSG_AUTH_ERROR, "auth_failed")
        mainThreadHandler.postDelayed({
            startAuthenticationFlow()
        }, DELAY_NAVIGATION)
    }

    override fun stop() {
        speechRecognizerDataSource.stopListening()
        speechRecognizerDataSource.release()
        shutdownTtsUseCase.execute()
        view.logInfo("Authentication Presenter stopped.")
    }

    private fun extractPinDigits(transcription: String): String? {
        val normalized = normalizeTranscriptionForPin(transcription)
        if (normalized.isEmpty()) return null

        PIN_CONTIGUOUS_REGEX.findAll(normalized).lastOrNull()?.value?.let { return it }

        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        var current = StringBuilder()
        var lastCandidate: String? = null
        var hasPartialDigits = false
        var candidateCount = 0

        for (token in tokens) {
            if (!token.all { it.isDigit() }) {
                if (current.isNotEmpty()) {
                    hasPartialDigits = true
                    current = StringBuilder()
                }
                continue
            }

            if (token.length >= PIN_LENGTH) {
                if (current.isNotEmpty()) {
                    hasPartialDigits = true
                    current = StringBuilder()
                }

                var index = 0
                while (index + PIN_LENGTH <= token.length) {
                    lastCandidate = token.substring(index, index + PIN_LENGTH)
                    candidateCount++
                    index += PIN_LENGTH
                }

                if (index != token.length) {
                    hasPartialDigits = true
                    current.append(token.substring(index))
                }
            } else {
                current.append(token)
                when {
                    current.length == PIN_LENGTH -> {
                        lastCandidate = current.toString()
                        candidateCount++
                        current = StringBuilder()
                    }
                    current.length > PIN_LENGTH -> {
                        hasPartialDigits = true
                        current = StringBuilder()
                    }
                }
            }
        }

        if (current.isNotEmpty()) {
            hasPartialDigits = true
        }

        if (candidateCount == 0) return null

        return if (candidateCount > 1 || !hasPartialDigits) lastCandidate else null
    }

    private fun normalizeTranscriptionForPin(text: String): String {
        var normalized = text.lowercase()
        NUMBER_WORD_MAP.forEach { (word, digit) ->
            normalized = normalized.replace(Regex("\\b${Regex.escape(word)}\\b"), digit)
        }
        normalized = normalized.replace(Regex("[^0-9\\s]"), " ")
        return normalized.replace(Regex("\\s+"), " ").trim()
    }
    
    private fun convertWordsToNumbers(text: String): String {
        val normalized = normalizeTranscriptionForPin(text)
        if (normalized.isEmpty()) return ""
        return normalized.filter { it.isDigit() }
    }
}
