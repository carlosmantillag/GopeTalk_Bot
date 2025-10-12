
package com.example.gopetalk_bot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.Locale

class VoiceInteractionService : Service() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private var webSocket: WebSocket? = null
    private val client by lazy { OkHttpClient() }

    private var hotwordDetected = false

    companion object {
        private const val TAG = "VoiceInteractionService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "VoiceInteractionChannel"
        private const val HOTWORD = "gopebot"
        // --- CAMBIA ESTA URL POR LA DE TU WEBSOCKET ---
        private const val WEBSOCKET_URL = "ws://159.223.150.185/ws" // Ejemplo
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        setupNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        setupTextToSpeech()
        setupSpeechRecognizer()
        connectWebSocket()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        startListening()
        return START_STICKY
    }

    private fun setupTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.forLanguageTag("es-ES")
                Log.i(TAG, "TextToSpeech initialized successfully.")
            } else {
                Log.e(TAG, "Failed to initialize TextToSpeech.")
            }
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition is not available on this device.")
            stopSelf() // Detener el servicio si no hay reconocimiento de voz
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val text = matches[0].lowercase(Locale.getDefault())
                    Log.i(TAG, "onResults: $text")
                    handleSpeechResult(text)
                }
                // Reiniciar la escucha
                startListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                 val partialText = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0) ?: ""
                 Log.d(TAG, "onPartialResults: $partialText")
                 if (!hotwordDetected && partialText.lowercase(Locale.getDefault()).contains(HOTWORD)) {
                     hotwordDetected = true
                     Log.i(TAG, "Hotword detected!")
                     // Podrías dar una señal auditiva o visual aquí si quisieras
                 }
            }

            override fun onError(error: Int) {
                // No reiniciar la escucha si el error es 'ERROR_CLIENT',
                // ya que es el resultado de llamar a speechRecognizer.cancel()
                if (error == SpeechRecognizer.ERROR_CLIENT) {
                    Log.d(TAG, "onError: ERROR_CLIENT - expected after cancel()")
                    return // No hacer nada, startListening() se encargará de reiniciar
                }
                
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Recognizer error $error"
                }
                Log.e(TAG, "onError: $errorMessage, restarting listening.")
                
                // Reiniciar la escucha en otros errores
                startListening()
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
            }
            // Ignorar otros métodos por brevedad
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun handleSpeechResult(text: String) {
        if (text.contains(HOTWORD)) {
            val command = text.substringAfter(HOTWORD).trim()
            if (command.isNotEmpty()) {
                Log.i(TAG, "Command sent to WebSocket: $command")
                webSocket?.send(command)
            }
        }
    }


    private fun startListening() {
        // Cancelar cualquier escucha anterior para evitar el error "recognizer busy"
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.cancel()
        }
        
        // Iniciar la escucha en el hilo principal
        Handler(Looper.getMainLooper()).post {
            if (::speechRecognizer.isInitialized) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                try {
                    speechRecognizer.startListening(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting listening: ${e.message}")
                }
            }
        }
    }

    private fun connectWebSocket() {
        val apiKey = BuildConfig.WEBSOCKET_API_KEY
        
        // Crear la request con o sin autenticación según la configuración
        val requestBuilder = Request.Builder().url(WEBSOCKET_URL)
        
        // Solo agregar el header de Authorization si la clave API no es el placeholder
        if (apiKey != "159.223.150.185" && apiKey.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }
        
        val request = requestBuilder.build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connection opened.")
                speak("Conectado al servidor de Gope.")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.i(TAG, "WebSocket onMessage: $text")
                speak(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket connection failure.", t)
                
                // Log adicional para errores de autenticación
                if (t.message?.contains("401") == true || t.message?.contains("Unauthorized") == true) {
                    Log.e(TAG, "Error de autenticación. Verifica la clave API en build.gradle.kts")
                    speak("Error de conexión: Verifica la configuración de la API")
                } else {
                    Log.e(TAG, "Error de conexión: ${t.message}")
                    speak("Error de conexión al servidor")
                }
                
                // Intentar reconectar después de 5 segundos
                Handler(Looper.getMainLooper()).postDelayed({
                    connectWebSocket()
                }, 5000)
            }
        })
    }

    private fun speak(text: String) {
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Interaction Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GopeTalk Bot Activo")
            .setContentText("Escuchando por el hotword '$HOTWORD'...")
            .setSmallIcon(R.mipmap.ic_launcher) // Reemplaza con tu propio ícono
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        speechRecognizer.destroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
        webSocket?.close(1000, "Service destroyed")
        client.dispatcher.executorService.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // No binding
    }
}
