package com.example.gopetalk_bot.domain.repositories

interface WebSocketRepository {
    fun connect(url: String, authToken: String?, channel: String?, listener: MicrophoneControlListener)
    fun disconnect()
    fun updateChannel(authToken: String?, channel: String?)
    fun isConnected(): Boolean
    
    interface MicrophoneControlListener {
        fun onMicrophoneStart()
        fun onMicrophoneStop()
        fun onConnectionEstablished()
        fun onConnectionClosed()
        fun onError(error: String)
    }
}
