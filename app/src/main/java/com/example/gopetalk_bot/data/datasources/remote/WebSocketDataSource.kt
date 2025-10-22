package com.example.gopetalk_bot.data.datasources.remote

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MINUTES)
        .build()
) {
    
    private companion object {
        const val TAG = "WebSocketDataSource"
        const val CONNECT_TIMEOUT = 10L
        const val CLOSE_CODE_NORMAL = 1000
        const val CLOSE_REASON = "Client disconnecting"
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val JSON_KEY_ACTION = "action"
        const val JSON_KEY_TYPE = "type"
        const val JSON_KEY_AUTH_TOKEN = "authToken"
        const val JSON_KEY_CHANNEL = "channel"
    }

    private var webSocket: WebSocket? = null
    private var listener: MicrophoneControlListener? = null

    interface MicrophoneControlListener {
        fun onMicrophoneStart()
        fun onMicrophoneStop()
        fun onConnectionEstablished()
        fun onConnectionClosed()
        fun onError(error: String)
    }

    fun connect(url: String, authToken: String?, channel: String?, listener: MicrophoneControlListener) {
        this.listener = listener
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, createWebSocketListener(authToken, channel, listener))
    }

    private fun createWebSocketListener(
        authToken: String?,
        channel: String?,
        listener: MicrophoneControlListener
    ) = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connection opened")
            sendHandshake(webSocket, authToken, channel)
            listener.onConnectionEstablished()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleMessage(text, listener)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(CLOSE_CODE_NORMAL, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            listener.onConnectionClosed()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket error", t)
            listener.onError(t.message ?: "Unknown WebSocket error")
        }
    }

    private fun sendHandshake(webSocket: WebSocket, authToken: String?, channel: String?) {
        val handshake = createHandshakeJson(authToken, channel)
        webSocket.send(handshake.toString())
    }

    private fun createHandshakeJson(authToken: String?, channel: String?) = JSONObject().apply {
        if (!authToken.isNullOrBlank()) put(JSON_KEY_AUTH_TOKEN, authToken)
        if (!channel.isNullOrBlank()) put(JSON_KEY_CHANNEL, channel)
    }

    private fun handleMessage(text: String, listener: MicrophoneControlListener) {
        try {
            if (text.trim().startsWith("{")) {
                handleJsonMessage(text, listener)
            } else {
                handlePlainTextMessage(text, listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
            handlePlainTextMessage(text, listener)
        }
    }

    private fun handleJsonMessage(text: String, listener: MicrophoneControlListener) {
        val json = JSONObject(text)
        val command = json.optString(JSON_KEY_ACTION).ifEmpty { 
            json.optString(JSON_KEY_TYPE) 
        }.uppercase()
        
        handleCommand(command, listener)
    }

    private fun handlePlainTextMessage(text: String, listener: MicrophoneControlListener) {
        handleCommand(text.trim().uppercase(), listener)
    }

    private fun handleCommand(command: String, listener: MicrophoneControlListener) {
        when (command) {
            ACTION_START -> listener.onMicrophoneStart()
            ACTION_STOP -> listener.onMicrophoneStop()
            else -> Log.w(TAG, "Unknown command: $command")
        }
    }

    fun disconnect() {
        webSocket?.close(CLOSE_CODE_NORMAL, CLOSE_REASON)
        webSocket = null
    }
    
    fun updateChannel(authToken: String?, channel: String?) {
        webSocket?.let { ws ->
            val update = createHandshakeJson(authToken, channel)
            ws.send(update.toString())
        } ?: Log.w(TAG, "Cannot update channel: WebSocket not connected")
    }

    fun isConnected(): Boolean = webSocket != null
}
