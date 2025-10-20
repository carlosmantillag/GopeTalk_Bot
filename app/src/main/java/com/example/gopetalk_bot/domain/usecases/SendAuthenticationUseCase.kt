package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.repositories.ApiRepository

class SendAuthenticationUseCase(
    private val apiRepository: ApiRepository
) {
    fun execute(nombre: String, pin: Int, callback: (ApiResponse) -> Unit) {
        apiRepository.sendAuthentication(nombre, pin, callback)
    }
}