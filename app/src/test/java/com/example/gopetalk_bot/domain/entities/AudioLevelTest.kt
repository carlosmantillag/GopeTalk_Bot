package com.example.gopetalk_bot.domain.entities

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AudioLevelTest {

    @Test
    fun `AudioLevel should contain correct rmsDb value`() {
        val audioLevel = AudioLevel(50.0f)

        assertThat(audioLevel.rmsDb).isEqualTo(50.0f)
    }

    @Test
    fun `AudioLevel with zero should work`() {
        val audioLevel = AudioLevel(0.0f)

        assertThat(audioLevel.rmsDb).isEqualTo(0.0f)
    }

    @Test
    fun `AudioLevel with negative value should work`() {
        val audioLevel = AudioLevel(-10.0f)

        assertThat(audioLevel.rmsDb).isEqualTo(-10.0f)
    }

    @Test
    fun `AudioLevel with high value should work`() {
        val audioLevel = AudioLevel(100.0f)

        assertThat(audioLevel.rmsDb).isEqualTo(100.0f)
    }

    @Test
    fun `AudioLevel with same rmsDb should be equal`() {
        val level1 = AudioLevel(50.0f)
        val level2 = AudioLevel(50.0f)

        assertThat(level1).isEqualTo(level2)
    }

    @Test
    fun `AudioLevel with different rmsDb should not be equal`() {
        val level1 = AudioLevel(50.0f)
        val level2 = AudioLevel(60.0f)

        assertThat(level1).isNotEqualTo(level2)
    }

    @Test
    fun `AudioLevel copy should create new instance`() {
        val original = AudioLevel(50.0f)
        val copied = original.copy(rmsDb = 60.0f)

        assertThat(copied.rmsDb).isEqualTo(60.0f)
        assertThat(original.rmsDb).isEqualTo(50.0f)
    }

    @Test
    fun `AudioLevel toString should contain rmsDb`() {
        val audioLevel = AudioLevel(50.0f)
        val string = audioLevel.toString()

        assertThat(string).contains("50")
    }

    @Test
    fun `AudioLevel hashCode should be consistent`() {
        val level1 = AudioLevel(50.0f)
        val level2 = AudioLevel(50.0f)

        assertThat(level1.hashCode()).isEqualTo(level2.hashCode())
    }

    @Test
    fun `AudioLevel should have timestamp`() {
        val before = System.currentTimeMillis()
        val audioLevel = AudioLevel(50.0f)
        val after = System.currentTimeMillis()

        assertThat(audioLevel.timestamp).isAtLeast(before)
        assertThat(audioLevel.timestamp).isAtMost(after)
    }

    @Test
    fun `AudioLevel with custom timestamp should work`() {
        val customTimestamp = 1234567890L
        val audioLevel = AudioLevel(50.0f, customTimestamp)

        assertThat(audioLevel.timestamp).isEqualTo(customTimestamp)
    }

    @Test
    fun `AudioLevel copy with timestamp should work`() {
        val original = AudioLevel(50.0f, 1000L)
        val copied = original.copy(timestamp = 2000L)

        assertThat(original.timestamp).isEqualTo(1000L)
        assertThat(copied.timestamp).isEqualTo(2000L)
    }

    @Test
    fun `AudioLevel with different timestamps should not be equal`() {
        val level1 = AudioLevel(50.0f, 1000L)
        val level2 = AudioLevel(50.0f, 2000L)

        assertThat(level1).isNotEqualTo(level2)
    }

    @Test
    fun `AudioLevel toString should contain timestamp`() {
        val audioLevel = AudioLevel(50.0f, 1234567890L)
        val string = audioLevel.toString()

        assertThat(string).contains("1234567890")
    }
}
