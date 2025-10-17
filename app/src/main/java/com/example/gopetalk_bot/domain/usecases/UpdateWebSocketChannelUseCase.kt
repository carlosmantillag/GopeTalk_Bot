package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.WebSocketRepository

class UpdateWebSocketChannelUseCase(
    private val webSocketRepository: WebSocketRepository
) {
    fun execute(userId: String, channel: String?) {
        webSocketRepository.updateChannel(userId, channel)
    }
}
