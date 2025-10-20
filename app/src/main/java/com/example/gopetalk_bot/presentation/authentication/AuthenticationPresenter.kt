package com.example.gopetalk_bot.presentation.authentication

import android.os.Handler
import android.os.Looper
import com.example.gopetalk_bot.data.datasources.local.SpeechRecognizerDataSource
import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.data.datasources.remote.dto.AuthenticationResponse
import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.usecases.SendAuthenticationUseCase
import com.example.gopetalk_bot.domain.usecases.SetTtsListenerUseCase
import com.example.gopetalk_bot.domain.usecases.ShutdownTtsUseCase
import com.example.gopetalk_bot.domain.usecases.SpeakTextUseCase
import com.google.gson.Gson

class AuthenticationPresenter(
    private val view: AuthenticationContract.View,
    private val speechRecognizerDataSource: SpeechRecognizerDataSource,
    private val sendAuthenticationUseCase: SendAuthenticationUseCase,
    private val speakTextUseCase: SpeakTextUseCase,
    private val setTtsListenerUseCase: SetTtsListenerUseCase,
    private val shutdownTtsUseCase: ShutdownTtsUseCase,
    private val userPreferences: UserPreferences
) : AuthenticationContract.Presenter {

    private enum class AuthState {
        WAITING_FOR_NAME,
        WAITING_FOR_PIN,
        WAITING_FOR_PIN_CONFIRMATION,
        AUTHENTICATED
    }
    
    private var authState: AuthState = AuthState.WAITING_FOR_NAME
    private var userName: String? = null
    private var userPin: Int? = null

    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private var isTtsSpeaking = false
    private val gson = Gson()

    override fun start() {
        view.logInfo("Authentication Presenter started.")
        
        setTtsListenerUseCase.execute(
            onStart = {
                view.logInfo("TTS started. Stopping speech recognition.")
                isTtsSpeaking = true
                // Stop listening while TTS is speaking to avoid self-listening
                speechRecognizerDataSource.stopListening()
            },
            onDone = {
                view.logInfo("TTS finished. Starting speech recognition.")
                isTtsSpeaking = false
                // Start listening after TTS finishes with a delay
                mainThreadHandler.postDelayed({
                    if (!isTtsSpeaking) {
                        startListening()
                    }
                }, 1000)
            },
            onError = {
                view.logError("TTS error.", null)
                isTtsSpeaking = false
            }
        )
        
        // Start authentication flow
        startAuthenticationFlow()
    }
    
    private fun startAuthenticationFlow() {
        authState = AuthState.WAITING_FOR_NAME
        view.logInfo("Starting authentication flow")
        speakTextUseCase.execute("Bienvenido Usuario, ¿cuál es tu nombre?", "auth_ask_name")
    }
    
    private fun startListening() {
        if (isTtsSpeaking) {
            view.logInfo("TTS is speaking, skipping speech recognition")
            return
        }
        
        view.logInfo("Starting speech recognition for state: $authState")
        speechRecognizerDataSource.startListening(
            onResult = { recognizedText ->
                view.logInfo("Speech recognized: $recognizedText")
                // Stop listening immediately after getting result
                speechRecognizerDataSource.stopListening()
                handleAuthenticationResponse(recognizedText)
            },
            onError = { error ->
                view.logError("Speech recognition error: $error", null)
                // Only retry if it's not a "no match" or "speech timeout" error
                if (!error.contains("No match") && !error.contains("No speech")) {
                    speakTextUseCase.execute("No pude entender, por favor repite", "auth_error")
                } else {
                    // Retry listening
                    mainThreadHandler.postDelayed({
                        if (!isTtsSpeaking) {
                            startListening()
                        }
                    }, 500)
                }
            }
        )
    }
    
    private fun handleAuthenticationResponse(transcription: String) {
        when (authState) {
            AuthState.WAITING_FOR_NAME -> {
                if (transcription.isNotBlank()) {
                    userName = transcription
                    view.logInfo("User name captured: $userName")
                    speakTextUseCase.execute("Bienvenido $userName", "auth_welcome_name")
                    
                    // Wait a bit before asking for PIN
                    mainThreadHandler.postDelayed({
                        authState = AuthState.WAITING_FOR_PIN
                        speakTextUseCase.execute(
                            "Dame un PIN de 4 dígitos por favor para poder entrar, no lo olvides",
                            "auth_ask_pin"
                        )
                    }, 2000)
                }
            }
            
            AuthState.WAITING_FOR_PIN -> {
                if (transcription.isNotBlank()) {
                    // Convert words to numbers and validate
                    val pinString = convertWordsToNumbers(transcription)
                    view.logInfo("PIN captured (original): $transcription")
                    view.logInfo("PIN captured (converted): $pinString")
                    
                    // Validate that PIN is numeric and has 4 digits
                    val pinInt = pinString.toIntOrNull()
                    if (pinInt != null && pinString.length == 4) {
                        userPin = pinInt
                        authState = AuthState.WAITING_FOR_PIN_CONFIRMATION
                        speakTextUseCase.execute(
                            "El PIN es $userPin, ¿me confirmas?",
                            "auth_confirm_pin"
                        )
                    } else {
                        view.logError("Invalid PIN format: $pinString", null)
                        speakTextUseCase.execute(
                            "El PIN debe ser de 4 dígitos numéricos. Por favor, repite el PIN",
                            "auth_invalid_pin"
                        )
                    }
                }
            }
            
            AuthState.WAITING_FOR_PIN_CONFIRMATION -> {
                val response = transcription.lowercase()
                view.logInfo("Confirmation response: $response")
                
                if (response.contains("sí") || response.contains("si") || response.contains("yes") || 
                    response.contains("correcto") || response.contains("afirmativo")) {
                    // User confirmed, send authentication
                    sendAuthentication()
                } else if (response.contains("no")) {
                    // User rejected, ask for PIN again
                    view.logInfo("User rejected PIN, asking again")
                    authState = AuthState.WAITING_FOR_PIN
                    speakTextUseCase.execute(
                        "Dame un PIN de 4 dígitos por favor para poder entrar, no lo olvides",
                        "auth_ask_pin_retry"
                    )
                } else {
                    // Unclear response, ask again
                    speakTextUseCase.execute(
                        "No entendí, por favor di sí o no",
                        "auth_unclear"
                    )
                }
            }
            
            AuthState.AUTHENTICATED -> {
                // Already authenticated, this shouldn't happen
                view.logInfo("Already authenticated, ignoring")
            }
        }
    }
    
    private fun sendAuthentication() {
        val name = userName ?: return
        val pin = userPin ?: return
        
        view.logInfo("Sending authentication: name=$name, pin=$pin")
        
        sendAuthenticationUseCase.execute(name, pin) { response ->
            mainThreadHandler.post {
                when (response) {
                    is ApiResponse.Success -> {
                        view.logInfo("Authentication successful: ${response.body}")
                        
                        try {
                            // Parse the response to extract message and token
                            val authResponse = gson.fromJson(response.body, AuthenticationResponse::class.java)
                            
                            // Save the token
                            userPreferences.authToken = authResponse.token
                            userPreferences.username = name
                            view.logInfo("Token saved: ${authResponse.token}")
                            
                            authState = AuthState.AUTHENTICATED
                            
                            // Speak only the message from the backend
                            speakTextUseCase.execute(
                                authResponse.message,
                                "auth_success"
                            )
                            
                            // Navigate to main activity after successful authentication
                            mainThreadHandler.postDelayed({
                                view.navigateToMainActivity()
                            }, 2000)
                        } catch (e: Exception) {
                            view.logError("Error parsing auth response: ${e.message}", e)
                            view.showAuthenticationError("Error procesando respuesta")
                            speakTextUseCase.execute(
                                "Error en la autenticación, por favor intenta de nuevo",
                                "auth_failed"
                            )
                            mainThreadHandler.postDelayed({
                                startAuthenticationFlow()
                            }, 2000)
                        }
                    }
                    is ApiResponse.Error -> {
                        view.logError("Authentication failed: ${response.message}", response.exception)
                        view.showAuthenticationError("Error en la autenticación")
                        speakTextUseCase.execute(
                            "Error en la autenticación, por favor intenta de nuevo",
                            "auth_failed"
                        )
                        // Restart authentication flow
                        mainThreadHandler.postDelayed({
                            startAuthenticationFlow()
                        }, 2000)
                    }
                }
            }
        }
    }

    override fun stop() {
        speechRecognizerDataSource.stopListening()
        speechRecognizerDataSource.release()
        shutdownTtsUseCase.execute()
        view.logInfo("Authentication Presenter stopped.")
    }
    
    /**
     * Converts Spanish number words to digits.
     * Handles numbers from 0-9 and removes spaces.
     */
    private fun convertWordsToNumbers(text: String): String {
        val numberMap = mapOf(
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
        
        var result = text.lowercase().replace(" ", "")
        
        // Replace each word with its corresponding digit
        numberMap.forEach { (word, digit) ->
            result = result.replace(word, digit)
        }
        
        // Keep only digits
        result = result.filter { it.isDigit() }
        
        return result
    }
}
