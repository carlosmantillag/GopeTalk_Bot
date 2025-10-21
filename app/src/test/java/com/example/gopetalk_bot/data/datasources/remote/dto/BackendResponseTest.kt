package com.example.gopetalk_bot.data.datasources.remote.dto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackendResponseTest {

    @Test
    fun `should create BackendResponse with default values`() {
        val response = BackendResponse()

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
            text = "Hello",
            action = "list_channels",
            channels = listOf("general", "random"),
            users = listOf("user1", "user2"),
            audioFile = "audio.wav",
            isAudioResponse = true,
            channel = "general"
        )

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
        val original = BackendResponse(text = "Original", action = "test")
        val copied = original.copy(text = "Modified")

        assertThat(copied.text).isEqualTo("Modified")
        assertThat(copied.action).isEqualTo("test")
        assertThat(original.text).isEqualTo("Original")
    }

    @Test
    fun `should support data class equality`() {
        val response1 = BackendResponse(text = "Hello", action = "greet")
        val response2 = BackendResponse(text = "Hello", action = "greet")
        val response3 = BackendResponse(text = "Goodbye", action = "greet")

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
        val response = BackendResponse(channel = null)

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
}
