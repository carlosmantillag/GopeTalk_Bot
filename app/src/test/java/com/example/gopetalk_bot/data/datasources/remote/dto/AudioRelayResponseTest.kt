package com.example.gopetalk_bot.data.datasources.remote.dto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AudioRelayResponseTest {

    @Test
    fun `should create AudioRelayResponse with all fields`() {
        val response = AudioRelayResponse(
            status = "success",
            channel = "general",
            recipients = listOf(1, 2, 3),
            audioBase64 = "SGVsbG8gV29ybGQ=",
            duration = 5.5,
            sampleRate = 16000,
            format = "wav"
        )

        assertThat(response.status).isEqualTo("success")
        assertThat(response.channel).isEqualTo("general")
        assertThat(response.recipients).containsExactly(1, 2, 3)
        assertThat(response.audioBase64).isEqualTo("SGVsbG8gV29ybGQ=")
        assertThat(response.duration).isEqualTo(5.5)
        assertThat(response.sampleRate).isEqualTo(16000)
        assertThat(response.format).isEqualTo("wav")
    }

    @Test
    fun `should handle empty recipients list`() {
        val response = AudioRelayResponse(
            status = "success",
            channel = "general",
            recipients = emptyList(),
            audioBase64 = "data",
            duration = 1.0,
            sampleRate = 16000,
            format = "wav"
        )

        assertThat(response.recipients).isEmpty()
    }

    @Test
    fun `should handle single recipient`() {
        val response = AudioRelayResponse(
            status = "success",
            channel = "general",
            recipients = listOf(42),
            audioBase64 = "data",
            duration = 1.0,
            sampleRate = 16000,
            format = "wav"
        )

        assertThat(response.recipients).hasSize(1)
        assertThat(response.recipients).contains(42)
    }

    @Test
    fun `should handle multiple recipients`() {
        val recipients = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val response = AudioRelayResponse(
            status = "success",
            channel = "general",
            recipients = recipients,
            audioBase64 = "data",
            duration = 1.0,
            sampleRate = 16000,
            format = "wav"
        )

        assertThat(response.recipients).hasSize(10)
        assertThat(response.recipients).containsExactlyElementsIn(recipients)
    }

    @Test
    fun `should handle different audio formats`() {
        val formats = listOf("wav", "mp3", "ogg", "flac")
        
        formats.forEach { format ->
            val response = AudioRelayResponse(
                status = "success",
                channel = "general",
                recipients = listOf(1),
                audioBase64 = "data",
                duration = 1.0,
                sampleRate = 16000,
                format = format
            )
            
            assertThat(response.format).isEqualTo(format)
        }
    }

    @Test
    fun `should handle different sample rates`() {
        val sampleRates = listOf(8000, 16000, 22050, 44100, 48000)
        
        sampleRates.forEach { rate ->
            val response = AudioRelayResponse(
                status = "success",
                channel = "general",
                recipients = listOf(1),
                audioBase64 = "data",
                duration = 1.0,
                sampleRate = rate,
                format = "wav"
            )
            
            assertThat(response.sampleRate).isEqualTo(rate)
        }
    }

    @Test
    fun `should handle zero duration`() {
        val response = AudioRelayResponse(
            status = "success",
            channel = "general",
            recipients = listOf(1),
            audioBase64 = "data",
            duration = 0.0,
            sampleRate = 16000,
            format = "wav"
        )

        assertThat(response.duration).isEqualTo(0.0)
    }

    @Test
    fun `should handle long duration`() {
        val response = AudioRelayResponse(
            status = "success",
            channel = "general",
            recipients = listOf(1),
            audioBase64 = "data",
            duration = 3600.5, 
            sampleRate = 16000,
            format = "wav"
        )

        assertThat(response.duration).isEqualTo(3600.5)
    }

    @Test
    fun `should handle error status`() {
        val response = AudioRelayResponse(
            status = "error",
            channel = "general",
            recipients = emptyList(),
            audioBase64 = "",
            duration = 0.0,
            sampleRate = 16000,
            format = "wav"
        )

        assertThat(response.status).isEqualTo("error")
    }

    @Test
    fun `should handle empty audioBase64`() {
        val response = AudioRelayResponse(
            status = "success",
            channel = "general",
            recipients = listOf(1),
            audioBase64 = "",
            duration = 0.0,
            sampleRate = 16000,
            format = "wav"
        )

        assertThat(response.audioBase64).isEmpty()
    }

    @Test
    fun `should handle large audioBase64`() {
        val largeBase64 = "A".repeat(10000)
        val response = AudioRelayResponse(
            status = "success",
            channel = "general",
            recipients = listOf(1),
            audioBase64 = largeBase64,
            duration = 10.0,
            sampleRate = 16000,
            format = "wav"
        )

        assertThat(response.audioBase64).hasLength(10000)
    }

    @Test
    fun `should support data class copy`() {
        val original = AudioRelayResponse(
            status = "success",
            channel = "general",
            recipients = listOf(1),
            audioBase64 = "data",
            duration = 1.0,
            sampleRate = 16000,
            format = "wav"
        )
        val copied = original.copy(channel = "random")

        assertThat(copied.channel).isEqualTo("random")
        assertThat(copied.status).isEqualTo("success")
        assertThat(original.channel).isEqualTo("general")
    }

    @Test
    fun `should support data class equality`() {
        val response1 = AudioRelayResponse(
            status = "success",
            channel = "general",
            recipients = listOf(1, 2),
            audioBase64 = "data",
            duration = 1.0,
            sampleRate = 16000,
            format = "wav"
        )
        val response2 = AudioRelayResponse(
            status = "success",
            channel = "general",
            recipients = listOf(1, 2),
            audioBase64 = "data",
            duration = 1.0,
            sampleRate = 16000,
            format = "wav"
        )
        val response3 = response1.copy(status = "error")

        assertThat(response1).isEqualTo(response2)
        assertThat(response1).isNotEqualTo(response3)
    }
}
