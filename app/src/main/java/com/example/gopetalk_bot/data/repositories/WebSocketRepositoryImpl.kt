package com.example.gopetalk_bot.data.repositories

import com.example.gopetalk_bot.data.datasources.remote.WebSocketDataSource
import com.example.gopetalk_bot.domain.repositories.WebSocketRepository

class WebSocketRepositoryImpl(
    private val webSocketDataSource: WebSocketDataSource
) : WebSocketRepository {
    
    override fun connect(url: String, userId: String, channel: String?, listener: WebSocketRepository.MicrophoneControlListener) {
        webSocketDataSource.connect(url, userId, channel, object : WebSocketDataSource.MicrophoneControlListener {
            override fun onMicrophoneStart() {
                listener.onMicrophoneStart()
            }
            
            override fun onMicrophoneStop() {
                listener.onMicrophoneStop()
            }
            
            override fun onConnectionEstablished() {
                listener.onConnectionEstablished()
            }
            
            override fun onConnectionClosed() {
                listener.onConnectionClosed()
            }
            
            override fun onError(error: String) {
                listener.onError(error)
            }
        })
    }
    
    override fun disconnect() {
        webSocketDataSource.disconnect()
    }
    
    override fun updateChannel(userId: String, channel: String?) {
        webSocketDataSource.updateChannel(userId, channel)
    }
    
    override fun isConnected(): Boolean {
        return webSocketDataSource.isConnected()
    }
}
