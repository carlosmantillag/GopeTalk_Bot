package com.example.gopetalk_bot.data.datasources.remote

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketDataSource {
    private var webSocket: WebSocket? = null
    private var listener: MicrophoneControlListener? = null
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MINUTES) // No timeout for WebSocket
        .build()

    interface MicrophoneControlListener {
        fun onMicrophoneStart()
        fun onMicrophoneStop()
        fun onConnectionEstablished()
        fun onConnectionClosed()
        fun onError(error: String)
    }

    fun connect(url: String, authToken: String?, channel: String?, listener: MicrophoneControlListener) {
        this.listener = listener
        
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connection opened")
                
                // Send handshake
                val handshake = JSONObject().apply {
                    if (!authToken.isNullOrBlank()) {
                        put("authToken", authToken)
                    }
                    if (!channel.isNullOrBlank()) {
                        put("channel", channel)
                    }
                }
                val handshakeStr = handshake.toString()
                Log.d(TAG, "Sending handshake: $handshakeStr")
                webSocket.send(handshakeStr)
                
                listener.onConnectionEstablished()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message received: $text")
                
                try {
                    // Try to parse as JSON first
                    if (text.trim().startsWith("{")) {
                        val json = JSONObject(text)
                        
                        // Check for action field
                        if (json.has("action")) {
                            val action = json.getString("action").uppercase()
                            Log.d(TAG, "JSON action received: $action")
                            
                            when (action) {
                                "START" -> {
                                    Log.d(TAG, "Microphone START signal received (JSON)")
                                    listener.onMicrophoneStart()
                                    return
                                }
                                "STOP" -> {
                                    Log.d(TAG, "Microphone STOP signal received (JSON)")
                                    listener.onMicrophoneStop()
                                    return
                                }
                            }
                        }
                        
                        // Check for type field (alternative format)
                        if (json.has("type")) {
                            val type = json.getString("type").uppercase()
                            Log.d(TAG, "JSON type received: $type")
                            
                            when (type) {
                                "START" -> {
                                    Log.d(TAG, "Microphone START signal received (JSON type)")
                                    listener.onMicrophoneStart()
                                    return
                                }
                                "STOP" -> {
                                    Log.d(TAG, "Microphone STOP signal received (JSON type)")
                                    listener.onMicrophoneStop()
                                    return
                                }
                            }
                        }
                        
                        Log.d(TAG, "JSON message without recognized action/type: $text")
                    } else {
                        // Handle simple text messages (START/STOP)
                        when (text.trim().uppercase()) {
                            "START" -> {
                                Log.d(TAG, "Microphone START signal received (plain text)")
                                listener.onMicrophoneStart()
                                return
                            }
                            "STOP" -> {
                                Log.d(TAG, "Microphone STOP signal received (plain text)")
                                listener.onMicrophoneStop()
                                return
                            }
                        }
                    }
                    
                    Log.w(TAG, "Unknown WebSocket message: $text")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing WebSocket message: $text", e)
                    
                    // Fallback to simple text parsing
                    when (text.trim().uppercase()) {
                        "START" -> {
                            Log.d(TAG, "Microphone START signal received (fallback)")
                            listener.onMicrophoneStart()
                        }
                        "STOP" -> {
                            Log.d(TAG, "Microphone STOP signal received (fallback)")
                            listener.onMicrophoneStop()
                        }
                        else -> {
                            Log.w(TAG, "Unknown WebSocket message after fallback: $text")
                        }
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                listener.onConnectionClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error", t)
                listener.onError(t.message ?: "Unknown WebSocket error")
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
    }
    
    fun updateChannel(authToken: String?, channel: String?) {
        webSocket?.let { ws ->
            val update = JSONObject().apply {
                if (!authToken.isNullOrBlank()) {
                    put("authToken", authToken)
                }
                if (!channel.isNullOrBlank()) {
                    put("channel", channel)
                }
            }
            val updateStr = update.toString()
            Log.d(TAG, "Updating channel: $updateStr")
            ws.send(updateStr)
        } ?: Log.w(TAG, "Cannot update channel: WebSocket not connected")
    }

    fun isConnected(): Boolean {
        return webSocket != null
    }

    companion object {
        private const val TAG = "WebSocketDataSource"
    }
}
