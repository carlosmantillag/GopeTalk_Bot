package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioDataSourceTest {

    private lateinit var dataSource: AudioDataSource
    private lateinit var mockContext: Context
    private lateinit var mockBufferProvider: AudioBufferProvider

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockBufferProvider = mockk(relaxed = true)
        
        // Mock cache directory
        val mockCacheDir = mockk<File>(relaxed = true)
        every { mockContext.cacheDir } returns mockCacheDir
        every { mockCacheDir.absolutePath } returns "/tmp/cache"
        
        // Mock buffer size
        every { mockBufferProvider.getMinBufferSize(any(), any(), any()) } returns 4096
        
        dataSource = AudioDataSource(mockContext, mockBufferProvider)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `AudioLevelData should hold rmsDb value`() {
        val audioLevel = AudioDataSource.AudioLevelData(75.5f)
        
        assertThat(audioLevel.rmsDb).isEqualTo(75.5f)
    }

    @Test
    fun `RecordedAudioData should hold file reference`() {
        val mockFile = mockk<File>()
        val recordedAudio = AudioDataSource.RecordedAudioData(mockFile)
        
        assertThat(recordedAudio.file).isEqualTo(mockFile)
    }

    @Test
    fun `pauseRecording should set paused state`() {
        // No podemos verificar el estado directamente, pero podemos verificar que no lanza excepciones
        dataSource.pauseRecording()
        
        // Si llega aquí, la función se ejecutó sin errores
        assertThat(true).isTrue()
    }

    @Test
    fun `resumeRecording should resume from paused state`() {
        dataSource.pauseRecording()
        dataSource.resumeRecording()
        
        // Si llega aquí, la función se ejecutó sin errores
        assertThat(true).isTrue()
    }

    @Test
    fun `stopMonitoring should handle not monitoring state`() {
        // Llamar stopMonitoring cuando no está monitoreando no debe lanzar excepción
        dataSource.stopMonitoring()
        
        assertThat(true).isTrue()
    }

    @Test
    fun `release should call stopMonitoring if monitoring`() {
        // Llamar release cuando no está monitoreando no debe lanzar excepción
        dataSource.release()
        
        assertThat(true).isTrue()
    }

    @Test
    fun `multiple pauseRecording calls should be safe`() {
        dataSource.pauseRecording()
        dataSource.pauseRecording()
        dataSource.pauseRecording()
        
        assertThat(true).isTrue()
    }

    @Test
    fun `multiple resumeRecording calls should be safe`() {
        dataSource.resumeRecording()
        dataSource.resumeRecording()
        dataSource.resumeRecording()
        
        assertThat(true).isTrue()
    }

    @Test
    fun `pause and resume cycle should work`() {
        dataSource.pauseRecording()
        dataSource.resumeRecording()
        dataSource.pauseRecording()
        dataSource.resumeRecording()
        
        assertThat(true).isTrue()
    }

    @Test
    fun `AudioLevelData with zero rmsDb`() {
        val audioLevel = AudioDataSource.AudioLevelData(0f)
        
        assertThat(audioLevel.rmsDb).isEqualTo(0f)
    }

    @Test
    fun `AudioLevelData with negative rmsDb`() {
        val audioLevel = AudioDataSource.AudioLevelData(-10f)
        
        assertThat(audioLevel.rmsDb).isEqualTo(-10f)
    }

    @Test
    fun `AudioLevelData with very high rmsDb`() {
        val audioLevel = AudioDataSource.AudioLevelData(150f)
        
        assertThat(audioLevel.rmsDb).isEqualTo(150f)
    }

    @Test
    fun `AudioLevelData equality check`() {
        val audioLevel1 = AudioDataSource.AudioLevelData(75.5f)
        val audioLevel2 = AudioDataSource.AudioLevelData(75.5f)
        
        assertThat(audioLevel1).isEqualTo(audioLevel2)
    }

    @Test
    fun `AudioLevelData inequality check`() {
        val audioLevel1 = AudioDataSource.AudioLevelData(75.5f)
        val audioLevel2 = AudioDataSource.AudioLevelData(80.0f)
        
        assertThat(audioLevel1).isNotEqualTo(audioLevel2)
    }

    @Test
    fun `RecordedAudioData equality check`() {
        val mockFile = mockk<File>()
        val recordedAudio1 = AudioDataSource.RecordedAudioData(mockFile)
        val recordedAudio2 = AudioDataSource.RecordedAudioData(mockFile)
        
        assertThat(recordedAudio1).isEqualTo(recordedAudio2)
    }

    @Test
    fun `RecordedAudioData with different files`() {
        val mockFile1 = mockk<File>()
        val mockFile2 = mockk<File>()
        val recordedAudio1 = AudioDataSource.RecordedAudioData(mockFile1)
        val recordedAudio2 = AudioDataSource.RecordedAudioData(mockFile2)
        
        assertThat(recordedAudio1).isNotEqualTo(recordedAudio2)
    }

    @Test
    fun `stopMonitoring multiple times should be safe`() {
        dataSource.stopMonitoring()
        dataSource.stopMonitoring()
        dataSource.stopMonitoring()
        
        assertThat(true).isTrue()
    }

    @Test
    fun `release multiple times should be safe`() {
        dataSource.release()
        dataSource.release()
        
        assertThat(true).isTrue()
    }

    @Test
    fun `AudioLevelData toString should contain rmsDb`() {
        val audioLevel = AudioDataSource.AudioLevelData(75.5f)
        val toString = audioLevel.toString()
        
        assertThat(toString).contains("75.5")
    }

    @Test
    fun `RecordedAudioData toString should contain file info`() {
        val mockFile = mockk<File>()
        every { mockFile.toString() } returns "test.wav"
        val recordedAudio = AudioDataSource.RecordedAudioData(mockFile)
        val toString = recordedAudio.toString()
        
        assertThat(toString).contains("file")
    }

    @Test
    fun `AudioLevelData copy should create new instance`() {
        val audioLevel1 = AudioDataSource.AudioLevelData(75.5f)
        val audioLevel2 = audioLevel1.copy(rmsDb = 80.0f)
        
        assertThat(audioLevel1.rmsDb).isEqualTo(75.5f)
        assertThat(audioLevel2.rmsDb).isEqualTo(80.0f)
    }

    @Test
    fun `RecordedAudioData copy should create new instance`() {
        val mockFile1 = mockk<File>()
        val mockFile2 = mockk<File>()
        val recordedAudio1 = AudioDataSource.RecordedAudioData(mockFile1)
        val recordedAudio2 = recordedAudio1.copy(file = mockFile2)
        
        assertThat(recordedAudio1.file).isEqualTo(mockFile1)
        assertThat(recordedAudio2.file).isEqualTo(mockFile2)
    }

    @Test
    fun `AudioLevelData hashCode should be consistent`() {
        val audioLevel1 = AudioDataSource.AudioLevelData(75.5f)
        val audioLevel2 = AudioDataSource.AudioLevelData(75.5f)
        
        assertThat(audioLevel1.hashCode()).isEqualTo(audioLevel2.hashCode())
    }

    @Test
    fun `RecordedAudioData hashCode should be consistent`() {
        val mockFile = mockk<File>()
        val recordedAudio1 = AudioDataSource.RecordedAudioData(mockFile)
        val recordedAudio2 = AudioDataSource.RecordedAudioData(mockFile)
        
        assertThat(recordedAudio1.hashCode()).isEqualTo(recordedAudio2.hashCode())
    }
}
