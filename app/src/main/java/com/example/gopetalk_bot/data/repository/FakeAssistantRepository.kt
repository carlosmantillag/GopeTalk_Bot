package com.example.gopetalk_bot.data.repository

import kotlinx.coroutines.delay

// Define the data structure for the response, matching the agreed API contract.
data class AssistantResponse(
    val response_text: String
)

/**
 * This class simulates interaction with the assistant's backend.
 * It returns predefined responses to allow for frontend development
 * while the real backend is not yet available.
 */
class FakeAssistantRepository {

    /**
     * Simulates sending a text query to the backend and receiving a response.
     *
     * @param query The text transcribed from the user's voice.
     * @return An [AssistantResponse] object containing the text to be synthesized.
     */
    suspend fun getResponse(query: String): AssistantResponse {
        // Simulate network latency
        delay(1500)

        val responseText = when {
            query.contains("hola", ignoreCase = true) -> "¡Hola! ¿En qué puedo ayudarte hoy?"
            query.contains("adiós", ignoreCase = true) -> "¡Hasta luego! Que tengas un buen día."
            else -> "Lo siento, no he entendido eso. ¿Podrías repetirlo?"
        }

        return AssistantResponse(response_text = responseText)
    }
}
