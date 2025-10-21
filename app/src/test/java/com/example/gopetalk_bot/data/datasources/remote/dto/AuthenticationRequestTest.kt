package com.example.gopetalk_bot.data.datasources.remote.dto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AuthenticationRequestTest {

    @Test
    fun `should create AuthenticationRequest with all fields`() {
        val request = AuthenticationRequest("Carlos", 1234)

        assertThat(request.nombre).isEqualTo("Carlos")
        assertThat(request.pin).isEqualTo(1234)
    }

    @Test
    fun `should handle empty nombre`() {
        val request = AuthenticationRequest("", 1234)

        assertThat(request.nombre).isEmpty()
        assertThat(request.pin).isEqualTo(1234)
    }

    @Test
    fun `should handle 4-digit PIN`() {
        val request = AuthenticationRequest("User", 9999)

        assertThat(request.pin).isEqualTo(9999)
    }

    @Test
    fun `should handle minimum PIN`() {
        val request = AuthenticationRequest("User", 0)

        assertThat(request.pin).isEqualTo(0)
    }

    @Test
    fun `should handle maximum 4-digit PIN`() {
        val request = AuthenticationRequest("User", 9999)

        assertThat(request.pin).isEqualTo(9999)
    }

    @Test
    fun `should handle nombre with spaces`() {
        val request = AuthenticationRequest("Carlos Mantilla", 1234)

        assertThat(request.nombre).isEqualTo("Carlos Mantilla")
    }

    @Test
    fun `should handle nombre with special characters`() {
        val request = AuthenticationRequest("José María", 1234)

        assertThat(request.nombre).isEqualTo("José María")
    }

    @Test
    fun `should handle long nombre`() {
        val longName = "A".repeat(100)
        val request = AuthenticationRequest(longName, 1234)

        assertThat(request.nombre).hasLength(100)
    }

    @Test
    fun `should support data class copy`() {
        val original = AuthenticationRequest("Original", 1234)
        val copied = original.copy(nombre = "Modified")

        assertThat(copied.nombre).isEqualTo("Modified")
        assertThat(copied.pin).isEqualTo(1234)
        assertThat(original.nombre).isEqualTo("Original")
    }

    @Test
    fun `should support data class equality`() {
        val request1 = AuthenticationRequest("User", 1234)
        val request2 = AuthenticationRequest("User", 1234)
        val request3 = AuthenticationRequest("User", 5678)

        assertThat(request1).isEqualTo(request2)
        assertThat(request1).isNotEqualTo(request3)
    }

    @Test
    fun `should support data class toString`() {
        val request = AuthenticationRequest("TestUser", 1234)
        val string = request.toString()

        assertThat(string).contains("TestUser")
        assertThat(string).contains("1234")
    }

    @Test
    fun `should handle different PINs`() {
        val pins = listOf(0, 1111, 2222, 5555, 9999)
        
        pins.forEach { pin ->
            val request = AuthenticationRequest("User", pin)
            assertThat(request.pin).isEqualTo(pin)
        }
    }

    @Test
    fun `should handle nombre with numbers`() {
        val request = AuthenticationRequest("User123", 1234)

        assertThat(request.nombre).isEqualTo("User123")
    }

    @Test
    fun `should handle single character nombre`() {
        val request = AuthenticationRequest("A", 1234)

        assertThat(request.nombre).hasLength(1)
        assertThat(request.nombre).isEqualTo("A")
    }
}
