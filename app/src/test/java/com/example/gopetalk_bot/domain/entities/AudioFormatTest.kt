package com.example.gopetalk_bot.domain.entities

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AudioFormatTest {

    @Test
    fun `AudioFormat should have PCM_16BIT value`() {
        val format = AudioFormat.PCM_16BIT

        assertThat(format).isNotNull()
        assertThat(format.name).isEqualTo("PCM_16BIT")
    }

    @Test
    fun `AudioFormat values should contain PCM_16BIT`() {
        val values = AudioFormat.values()

        assertThat(values.toList()).contains(AudioFormat.PCM_16BIT)
    }

    @Test
    fun `AudioFormat valueOf should return correct enum`() {
        val format = AudioFormat.valueOf("PCM_16BIT")

        assertThat(format).isEqualTo(AudioFormat.PCM_16BIT)
    }

    @Test
    fun `AudioFormat should be comparable`() {
        val format1 = AudioFormat.PCM_16BIT
        val format2 = AudioFormat.PCM_16BIT

        assertThat(format1).isEqualTo(format2)
    }

    @Test
    fun `AudioFormat toString should return name`() {
        val format = AudioFormat.PCM_16BIT

        assertThat(format.toString()).isEqualTo("PCM_16BIT")
    }

    @Test
    fun `AudioFormat ordinal should be consistent`() {
        val format = AudioFormat.PCM_16BIT

        assertThat(format.ordinal).isEqualTo(0)
    }

    @Test
    fun `AudioFormat values array should have correct size`() {
        val values = AudioFormat.values()

        assertThat(values).hasLength(1)
    }
}
