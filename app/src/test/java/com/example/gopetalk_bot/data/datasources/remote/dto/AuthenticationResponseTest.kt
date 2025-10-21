package com.example.gopetalk_bot.data.datasources.remote.dto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AuthenticationResponseTest {

    @Test
    fun `should create AuthenticationResponse with all fields`() {
        val message = "Authentication successful"
        val token = "abc123token"

        val response = AuthenticationResponse(message, token)

        assertThat(response.message).isEqualTo(message)
        assertThat(response.token).isEqualTo(token)
    }

    @Test
    fun `should handle empty message`() {
        val response = AuthenticationResponse("", "token123")

        assertThat(response.message).isEmpty()
        assertThat(response.token).isEqualTo("token123")
    }

    @Test
    fun `should handle empty token`() {
        val response = AuthenticationResponse("Success", "")

        assertThat(response.message).isEqualTo("Success")
        assertThat(response.token).isEmpty()
    }

    @Test
    fun `should support data class copy`() {
        val original = AuthenticationResponse("Original", "token1")
        val copied = original.copy(message = "Modified")

        assertThat(copied.message).isEqualTo("Modified")
        assertThat(copied.token).isEqualTo("token1")
        assertThat(original.message).isEqualTo("Original")
    }

    @Test
    fun `should support data class equality`() {
        val response1 = AuthenticationResponse("Message", "token")
        val response2 = AuthenticationResponse("Message", "token")
        val response3 = AuthenticationResponse("Different", "token")

        assertThat(response1).isEqualTo(response2)
        assertThat(response1).isNotEqualTo(response3)
    }

    @Test
    fun `should support data class toString`() {
        val response = AuthenticationResponse("Test", "token123")
        val string = response.toString()

        assertThat(string).contains("Test")
        assertThat(string).contains("token123")
    }

    @Test
    fun `should handle special characters in message`() {
        val message = "¡Bienvenido! Autenticación exitosa: 100%"
        val response = AuthenticationResponse(message, "token")

        assertThat(response.message).isEqualTo(message)
    }

    @Test
    fun `should handle long token`() {
        val longToken = "a".repeat(500)
        val response = AuthenticationResponse("Message", longToken)

        assertThat(response.token).hasLength(500)
    }
}
