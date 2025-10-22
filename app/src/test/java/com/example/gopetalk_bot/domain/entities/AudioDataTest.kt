package com.example.gopetalk_bot.domain.entities

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class AudioDataTest {

    @Test
    fun `AudioData should contain correct properties`() {
        val file = File("audio.wav")
        val audioData = AudioData(
            file = file,
            sampleRate = 16000,
            channels = 1,
            format = AudioFormat.PCM_16BIT
        )

        assertThat(audioData.file).isEqualTo(file)
        assertThat(audioData.sampleRate).isEqualTo(16000)
        assertThat(audioData.channels).isEqualTo(1)
        assertThat(audioData.format).isEqualTo(AudioFormat.PCM_16BIT)
    }

    @Test
    fun `AudioData with different sample rates should work`() {
        val file = File("audio.wav")
        val audioData44k = AudioData(file, 44100, 2, AudioFormat.PCM_16BIT)
        val audioData16k = AudioData(file, 16000, 1, AudioFormat.PCM_16BIT)

        assertThat(audioData44k.sampleRate).isEqualTo(44100)
        assertThat(audioData16k.sampleRate).isEqualTo(16000)
    }

    @Test
    fun `AudioData with mono and stereo channels should work`() {
        val file = File("audio.wav")
        val mono = AudioData(file, 16000, 1, AudioFormat.PCM_16BIT)
        val stereo = AudioData(file, 16000, 2, AudioFormat.PCM_16BIT)

        assertThat(mono.channels).isEqualTo(1)
        assertThat(stereo.channels).isEqualTo(2)
    }

    @Test
    fun `AudioData with same properties should be equal`() {
        val file = File("audio.wav")
        val audioData1 = AudioData(file, 16000, 1, AudioFormat.PCM_16BIT)
        val audioData2 = AudioData(file, 16000, 1, AudioFormat.PCM_16BIT)

        assertThat(audioData1).isEqualTo(audioData2)
    }

    @Test
    fun `AudioData with different files should not be equal`() {
        val file1 = File("audio1.wav")
        val file2 = File("audio2.wav")
        val audioData1 = AudioData(file1, 16000, 1, AudioFormat.PCM_16BIT)
        val audioData2 = AudioData(file2, 16000, 1, AudioFormat.PCM_16BIT)

        assertThat(audioData1).isNotEqualTo(audioData2)
    }

    @Test
    fun `AudioData copy should create new instance with modified properties`() {
        val file = File("audio.wav")
        val original = AudioData(file, 16000, 1, AudioFormat.PCM_16BIT)
        val copied = original.copy(sampleRate = 44100)

        assertThat(copied.sampleRate).isEqualTo(44100)
        assertThat(copied.file).isEqualTo(file)
        assertThat(copied.channels).isEqualTo(1)
    }

    @Test
    fun `AudioData toString should contain properties`() {
        val file = File("audio.wav")
        val audioData = AudioData(file, 16000, 1, AudioFormat.PCM_16BIT)
        val string = audioData.toString()

        assertThat(string).contains("16000")
        assertThat(string).contains("1")
    }

    @Test
    fun `AudioData hashCode should be consistent`() {
        val file = File("audio.wav")
        val audioData1 = AudioData(file, 16000, 1, AudioFormat.PCM_16BIT)
        val audioData2 = AudioData(file, 16000, 1, AudioFormat.PCM_16BIT)

        assertThat(audioData1.hashCode()).isEqualTo(audioData2.hashCode())
    }

    @Test
    fun `AudioData copy with different file should work`() {
        val file1 = File("audio1.wav")
        val file2 = File("audio2.wav")
        val original = AudioData(file1, 16000, 1, AudioFormat.PCM_16BIT)
        val copied = original.copy(file = file2)

        assertThat(original.file).isEqualTo(file1)
        assertThat(copied.file).isEqualTo(file2)
    }

    @Test
    fun `AudioData copy with different channels should work`() {
        val file = File("audio.wav")
        val original = AudioData(file, 16000, 1, AudioFormat.PCM_16BIT)
        val copied = original.copy(channels = 2)

        assertThat(original.channels).isEqualTo(1)
        assertThat(copied.channels).isEqualTo(2)
    }

    @Test
    fun `AudioData with different sample rates should not be equal`() {
        val file = File("audio.wav")
        val audioData1 = AudioData(file, 16000, 1, AudioFormat.PCM_16BIT)
        val audioData2 = AudioData(file, 44100, 1, AudioFormat.PCM_16BIT)

        assertThat(audioData1).isNotEqualTo(audioData2)
    }

    @Test
    fun `AudioData with different channels should not be equal`() {
        val file = File("audio.wav")
        val audioData1 = AudioData(file, 16000, 1, AudioFormat.PCM_16BIT)
        val audioData2 = AudioData(file, 16000, 2, AudioFormat.PCM_16BIT)

        assertThat(audioData1).isNotEqualTo(audioData2)
    }

    @Test
    fun `AudioData with high sample rate should work`() {
        val file = File("audio.wav")
        val audioData = AudioData(file, 192000, 2, AudioFormat.PCM_16BIT)

        assertThat(audioData.sampleRate).isEqualTo(192000)
    }

    @Test
    fun `AudioData with multiple channels should work`() {
        val file = File("audio.wav")
        val audioData = AudioData(file, 16000, 8, AudioFormat.PCM_16BIT)

        assertThat(audioData.channels).isEqualTo(8)
    }
}
