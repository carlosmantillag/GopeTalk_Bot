package com.example.gopetalk_bot.data.repositories

import android.util.Log
import com.example.gopetalk_bot.data.datasources.remote.WebSocketDataSource
import com.example.gopetalk_bot.domain.repositories.WebSocketRepository

class WebSocketRepositoryImpl(
    private val webSocketDataSource: WebSocketDataSource
) : WebSocketRepository {
    
    override fun connect(url: String, authToken: String?, channel: String?, listener: WebSocketRepository.MicrophoneControlListener) {
        Log.d(TAG, "Repository: Connecting to WebSocket - url=$url, authToken=${authToken?.take(20)}..., channel=$channel")
        webSocketDataSource.connect(url, authToken, channel, object : WebSocketDataSource.MicrophoneControlListener {
            override fun onMicrophoneStart() {
                Log.d(TAG, "Repository: Microphone START signal propagated")
                listener.onMicrophoneStart()
            }
            
            override fun onMicrophoneStop() {
                Log.d(TAG, "Repository: Microphone STOP signal propagated")
                listener.onMicrophoneStop()
            }
            
            override fun onConnectionEstablished() {
                Log.d(TAG, "Repository: Connection established propagated")
                listener.onConnectionEstablished()
            }
            
            override fun onConnectionClosed() {
                Log.d(TAG, "Repository: Connection closed propagated")
                listener.onConnectionClosed()
            }
            
            override fun onError(error: String) {
                Log.e(TAG, "Repository: Error propagated - $error")
                listener.onError(error)
            }
        })
    }
    
    override fun disconnect() {
        Log.d(TAG, "Repository: Disconnecting WebSocket")
        webSocketDataSource.disconnect()
    }
    
    override fun updateChannel(authToken: String?, channel: String?) {
        Log.d(TAG, "Repository: Updating channel - authToken=${authToken?.take(20)}..., channel=$channel")
        webSocketDataSource.updateChannel(authToken, channel)
    }
    
    override fun isConnected(): Boolean {
        val connected = webSocketDataSource.isConnected()
        Log.d(TAG, "Repository: isConnected=$connected")
        return connected
    }
    
    companion object {
        private const val TAG = "WebSocketRepositoryImpl"
    }
}
