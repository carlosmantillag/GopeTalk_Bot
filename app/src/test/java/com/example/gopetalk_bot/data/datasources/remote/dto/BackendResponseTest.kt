package com.example.gopetalk_bot.data.datasources.remote.dto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackendResponseTest {

    @Test
    fun `should create BackendResponse with default values`() {
        val response = BackendResponse()

        assertThat(response.status).isEmpty()
        assertThat(response.intent).isEmpty()
        assertThat(response.message).isEmpty()
        assertThat(response.data).isNull()
        assertThat(response.text).isEmpty()
        assertThat(response.action).isEmpty()
        assertThat(response.channels).isEmpty()
        assertThat(response.users).isEmpty()
        assertThat(response.audioFile).isNull()
        assertThat(response.isAudioResponse).isFalse()
        assertThat(response.channel).isNull()
    }

    @Test
    fun `should create BackendResponse with all fields`() {
        val response = BackendResponse(
            status = "ok",
            intent = "request_channel_connect",
            message = "Conectado al canal 1",
            data = BackendResponseData(channel = "canal-1", channelLabel = "1"),
            text = "Hello",
            action = "list_channels",
            channels = listOf("general", "random"),
            users = listOf("user1", "user2"),
            audioFile = "audio.wav",
            isAudioResponse = true,
            channel = "general"
        )

        assertThat(response.status).isEqualTo("ok")
        assertThat(response.intent).isEqualTo("request_channel_connect")
        assertThat(response.message).isEqualTo("Conectado al canal 1")
        assertThat(response.data?.channel).isEqualTo("canal-1")
        assertThat(response.data?.channelLabel).isEqualTo("1")
        assertThat(response.text).isEqualTo("Hello")
        assertThat(response.action).isEqualTo("list_channels")
        assertThat(response.channels).containsExactly("general", "random")
        assertThat(response.users).containsExactly("user1", "user2")
        assertThat(response.audioFile).isEqualTo("audio.wav")
        assertThat(response.isAudioResponse).isTrue()
        assertThat(response.channel).isEqualTo("general")
    }

    @Test
    fun `should handle list_channels action`() {
        val response = BackendResponse(
            action = "list_channels",
            channels = listOf("channel1", "channel2", "channel3")
        )

        assertThat(response.action).isEqualTo("list_channels")
        assertThat(response.channels).hasSize(3)
    }

    @Test
    fun `should handle list_users action`() {
        val response = BackendResponse(
            action = "list_users",
            users = listOf("Alice", "Bob", "Charlie")
        )

        assertThat(response.action).isEqualTo("list_users")
        assertThat(response.users).hasSize(3)
    }

    @Test
    fun `should handle logout action`() {
        val response = BackendResponse(
            action = "logout",
            message = "",
            text = "Cerrando sesión"
        )

        assertThat(response.action).isEqualTo("logout")
        assertThat(response.text).isEqualTo("Cerrando sesión")
    }

    @Test
    fun `should handle audio response`() {
        val response = BackendResponse(
            isAudioResponse = true,
            audioFile = "response.wav"
        )

        assertThat(response.isAudioResponse).isTrue()
        assertThat(response.audioFile).isEqualTo("response.wav")
    }

    @Test
    fun `should support data class copy`() {
        val original = BackendResponse(message = "Original", intent = "test_intent")
        val copied = original.copy(message = "Modified")

        assertThat(copied.message).isEqualTo("Modified")
        assertThat(copied.intent).isEqualTo("test_intent")
        assertThat(original.message).isEqualTo("Original")
    }

    @Test
    fun `should support data class equality`() {
        val response1 = BackendResponse(message = "Hello", intent = "greet")
        val response2 = BackendResponse(message = "Hello", intent = "greet")
        val response3 = BackendResponse(message = "Goodbye", intent = "greet")

        assertThat(response1).isEqualTo(response2)
        assertThat(response1).isNotEqualTo(response3)
    }

    @Test
    fun `should handle empty lists`() {
        val response = BackendResponse(
            channels = emptyList(),
            users = emptyList()
        )

        assertThat(response.channels).isEmpty()
        assertThat(response.users).isEmpty()
    }

    @Test
    fun `should handle null audioFile`() {
        val response = BackendResponse(audioFile = null)

        assertThat(response.audioFile).isNull()
    }

    @Test
    fun `should handle null channel`() {
        val response = BackendResponse(data = BackendResponseData(channel = null))

        assertThat(response.data?.channel).isNull()
        assertThat(response.channel).isNull()
    }

    @Test
    fun `should handle special characters in text`() {
        val text = "¡Hola! ¿Cómo estás? Ñoño"
        val response = BackendResponse(text = text)

        assertThat(response.text).isEqualTo(text)
    }

    @Test
    fun `should handle large channel list`() {
        val channels = (1..100).map { "channel$it" }
        val response = BackendResponse(channels = channels)

        assertThat(response.channels).hasSize(100)
    }

    @Test
    fun `should handle large user list`() {
        val users = (1..50).map { "user$it" }
        val response = BackendResponse(users = users)

        assertThat(response.users).hasSize(50)
    }

    @Test
    fun `should expose channel data within nested data object`() {
        val response = BackendResponse(
            data = BackendResponseData(channel = "canal-3", channelLabel = "3")
        )

        assertThat(response.data?.channel).isEqualTo("canal-3")
        assertThat(response.data?.channelLabel).isEqualTo("3")
    }
}
