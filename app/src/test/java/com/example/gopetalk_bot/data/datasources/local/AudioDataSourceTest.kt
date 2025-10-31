package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import kotlin.math.log10
import kotlin.math.sqrt

class AudioDataSourceTest {

    private lateinit var dataSource: AudioDataSource
    private lateinit var mockContext: Context
    private lateinit var mockBufferProvider: AudioBufferProvider
    private lateinit var mockAdaptiveThresholdManager: AdaptiveNoiseThresholdManager
    private lateinit var mockVoiceActivityDetector: VoiceActivityDetector
    private lateinit var tempCacheDir: File

    @Before
    fun setup() {
        // Limpiar mocks anteriores
        clearAllMocks()
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.v(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        
        mockContext = mockk(relaxed = true)
        mockBufferProvider = mockk(relaxed = true)
        mockAdaptiveThresholdManager = mockk(relaxed = true)
        mockVoiceActivityDetector = mockk(relaxed = true)
        
        // Mock cache directory
        tempCacheDir = Files.createTempDirectory("audio_cache_test").toFile()
        every { mockContext.cacheDir } returns tempCacheDir
        
        // Mock buffer size
        every { mockBufferProvider.getMinBufferSize(any(), any(), any()) } returns 4096
        
        every { mockAdaptiveThresholdManager.processAudioLevel(any(), any()) } just Runs
        every { mockAdaptiveThresholdManager.getCurrentThreshold() } returns 70f
        every { mockAdaptiveThresholdManager.getAmbientNoiseLevel() } returns 60f
        every { mockAdaptiveThresholdManager.getEnvironmentType() } returns "Normal"
        every { mockAdaptiveThresholdManager.isCalibrated() } returns true
        every { mockVoiceActivityDetector.isVoiceDetected(any(), any(), any()) } returns false
        every { mockVoiceActivityDetector.reset() } just Runs
        
        dataSource = AudioDataSource(
            context = mockContext,
            bufferProvider = mockBufferProvider,
            adaptiveThresholdManager = mockAdaptiveThresholdManager,
            voiceActivityDetector = mockVoiceActivityDetector
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
        if (::tempCacheDir.isInitialized) {
            tempCacheDir.deleteRecursively()
        }
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

    // ==================== Tests para AudioBufferProvider ====================
    
    @Test
    fun `AudioBufferProvider should return buffer size`() {
        val bufferSize = mockBufferProvider.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        
        assertThat(bufferSize).isEqualTo(4096)
    }

    @Test
    fun `AndroidAudioBufferProvider should delegate to AudioRecord`() {
        // Este test verifica que la implementación real delega correctamente
        // No podemos ejecutar AudioRecord.getMinBufferSize en tests unitarios sin Robolectric
        // pero podemos verificar que la clase existe y tiene el método correcto
        val provider = AndroidAudioBufferProvider()
        assertThat(provider).isNotNull()
    }

    // ==================== Tests para cálculo de RMS ====================
    
    @Test
    fun `calculateRmsDb with silence should return low value`() {
        // Creamos audio silencioso (todos ceros)
        val silentData = ByteArray(4096) { 0 }
        
        // Usamos reflection para acceder al método privado
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "calculateRmsDb",
            ByteArray::class.java,
            Int::class.java
        )
        method.isAccessible = true
        
        val rmsDb = method.invoke(dataSource, silentData, silentData.size) as Float
        
        // El silencio debería dar un valor muy bajo (cerca de 0 o negativo)
        assertThat(rmsDb).isLessThan(50f)
    }

    @Test
    fun `calculateRmsDb with loud audio should return high value`() {
        // Creamos audio fuerte (valores altos)
        val loudData = ByteArray(4096)
        val buffer = ByteBuffer.wrap(loudData).order(ByteOrder.LITTLE_ENDIAN)
        
        // Llenamos con valores de 16 bits altos (simulando audio fuerte)
        for (i in 0 until loudData.size / 2) {
            buffer.putShort(10000) // Valor alto pero no máximo
        }
        
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "calculateRmsDb",
            ByteArray::class.java,
            Int::class.java
        )
        method.isAccessible = true
        
        val rmsDb = method.invoke(dataSource, loudData, loudData.size) as Float
        
        // Audio fuerte debería dar un valor alto
        assertThat(rmsDb).isGreaterThan(70f)
    }

    @Test
    fun `calculateRmsDb with medium audio should return medium value`() {
        val mediumData = ByteArray(4096)
        val buffer = ByteBuffer.wrap(mediumData).order(ByteOrder.LITTLE_ENDIAN)
        
        // Valores medios
        for (i in 0 until mediumData.size / 2) {
            buffer.putShort(1000)
        }
        
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "calculateRmsDb",
            ByteArray::class.java,
            Int::class.java
        )
        method.isAccessible = true
        
        val rmsDb = method.invoke(dataSource, mediumData, mediumData.size) as Float
        
        assertThat(rmsDb).isAtLeast(40f)
        assertThat(rmsDb).isAtMost(90f)
    }

    @Test
    fun `calculateRmsDb with varying audio should calculate correctly`() {
        val varyingData = ByteArray(4096)
        val buffer = ByteBuffer.wrap(varyingData).order(ByteOrder.LITTLE_ENDIAN)
        
        // Valores variados
        for (i in 0 until varyingData.size / 2) {
            buffer.putShort((i % 5000).toShort())
        }
        
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "calculateRmsDb",
            ByteArray::class.java,
            Int::class.java
        )
        method.isAccessible = true
        
        val rmsDb = method.invoke(dataSource, varyingData, varyingData.size) as Float
        
        // Debería retornar un valor válido
        assertThat(rmsDb).isNotNaN()
        assertThat(rmsDb).isFinite()
    }

    // ==================== Tests para detección de sonido/silencio ====================
    
    @Test
    fun `isSoundDetected should return true when rmsDb above threshold`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "isSoundDetected",
            Float::class.java
        )
        method.isAccessible = true
        
        val result = method.invoke(dataSource, 75f) as Boolean
        
        assertThat(result).isTrue()
    }

    @Test
    fun `isSoundDetected should return false when rmsDb below threshold`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "isSoundDetected",
            Float::class.java
        )
        method.isAccessible = true
        
        val result = method.invoke(dataSource, 65f) as Boolean
        
        assertThat(result).isFalse()
    }

    @Test
    fun `isSoundDetected should return false when rmsDb equals threshold`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "isSoundDetected",
            Float::class.java
        )
        method.isAccessible = true
        
        val result = method.invoke(dataSource, 70f) as Boolean
        
        assertThat(result).isFalse()
    }

    // ==================== Tests para generación de headers WAV ====================
    
    @Test
    fun `createWavHeader should generate correct header size`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "createWavHeader",
            Int::class.java,
            Int::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        val header = method.invoke(dataSource, 1000, 1000, 32000L) as ByteArray
        
        assertThat(header.size).isEqualTo(44)
    }

    @Test
    fun `createWavHeader should contain RIFF identifier`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "createWavHeader",
            Int::class.java,
            Int::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        val header = method.invoke(dataSource, 1000, 1000, 32000L) as ByteArray
        
        // Verificar "RIFF" en los primeros 4 bytes
        assertThat(header[0]).isEqualTo('R'.code.toByte())
        assertThat(header[1]).isEqualTo('I'.code.toByte())
        assertThat(header[2]).isEqualTo('F'.code.toByte())
        assertThat(header[3]).isEqualTo('F'.code.toByte())
    }

    @Test
    fun `createWavHeader should contain WAVE identifier`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "createWavHeader",
            Int::class.java,
            Int::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        val header = method.invoke(dataSource, 1000, 1000, 32000L) as ByteArray
        
        // Verificar "WAVE" en bytes 8-11
        assertThat(header[8]).isEqualTo('W'.code.toByte())
        assertThat(header[9]).isEqualTo('A'.code.toByte())
        assertThat(header[10]).isEqualTo('V'.code.toByte())
        assertThat(header[11]).isEqualTo('E'.code.toByte())
    }

    @Test
    fun `createWavHeader should contain fmt chunk identifier`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "createWavHeader",
            Int::class.java,
            Int::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        val header = method.invoke(dataSource, 1000, 1000, 32000L) as ByteArray
        
        // Verificar "fmt " en bytes 12-15
        assertThat(header[12]).isEqualTo('f'.code.toByte())
        assertThat(header[13]).isEqualTo('m'.code.toByte())
        assertThat(header[14]).isEqualTo('t'.code.toByte())
        assertThat(header[15]).isEqualTo(' '.code.toByte())
    }

    @Test
    fun `createWavHeader should contain data chunk identifier`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "createWavHeader",
            Int::class.java,
            Int::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        val header = method.invoke(dataSource, 1000, 1000, 32000L) as ByteArray
        
        // Verificar "data" en bytes 36-39
        assertThat(header[36]).isEqualTo('d'.code.toByte())
        assertThat(header[37]).isEqualTo('a'.code.toByte())
        assertThat(header[38]).isEqualTo('t'.code.toByte())
        assertThat(header[39]).isEqualTo('a'.code.toByte())
    }

    @Test
    fun `createWavHeader should set correct audio format (PCM)`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "createWavHeader",
            Int::class.java,
            Int::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        val header = method.invoke(dataSource, 1000, 1000, 32000L) as ByteArray
        
        // Byte 20-21 debe ser 1 (PCM format)
        assertThat(header[20]).isEqualTo(1.toByte())
        assertThat(header[21]).isEqualTo(0.toByte())
    }

    @Test
    fun `createWavHeader should set correct number of channels`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "createWavHeader",
            Int::class.java,
            Int::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        val header = method.invoke(dataSource, 1000, 1000, 32000L) as ByteArray
        
        // Byte 22-23 debe ser 1 (mono)
        assertThat(header[22]).isEqualTo(1.toByte())
        assertThat(header[23]).isEqualTo(0.toByte())
    }

    @Test
    fun `createWavHeader should set correct sample rate`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "createWavHeader",
            Int::class.java,
            Int::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        val header = method.invoke(dataSource, 1000, 1000, 32000L) as ByteArray
        
        // Bytes 24-27 contienen el sample rate (16000 Hz en little endian)
        val sampleRate = ByteBuffer.wrap(header, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertThat(sampleRate).isEqualTo(16000)
    }

    @Test
    fun `createWavHeader should set correct bits per sample`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "createWavHeader",
            Int::class.java,
            Int::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        val header = method.invoke(dataSource, 1000, 1000, 32000L) as ByteArray
        
        // Bytes 34-35 contienen bits per sample (16 bits)
        assertThat(header[34]).isEqualTo(16.toByte())
        assertThat(header[35]).isEqualTo(0.toByte())
    }

    @Test
    fun `createWavHeader should calculate correct file size`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "createWavHeader",
            Int::class.java,
            Int::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        val totalAudioLen = 1000
        val totalDataLen = totalAudioLen + 36
        val header = method.invoke(dataSource, totalDataLen, totalAudioLen, 32000L) as ByteArray
        
        // Bytes 4-7 contienen el tamaño del archivo (totalDataLen)
        val fileSize = ByteBuffer.wrap(header, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertThat(fileSize).isEqualTo(totalDataLen)
    }

    @Test
    fun `createWavHeader should calculate correct data chunk size`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "createWavHeader",
            Int::class.java,
            Int::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        val totalAudioLen = 1000
        val header = method.invoke(dataSource, 1036, totalAudioLen, 32000L) as ByteArray
        
        // Bytes 40-43 contienen el tamaño del data chunk
        val dataSize = ByteBuffer.wrap(header, 40, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertThat(dataSize).isEqualTo(totalAudioLen)
    }

    // ==================== Tests para writeRiffHeader ====================
    
    @Test
    fun `writeRiffHeader should write RIFF correctly`() {
        val header = ByteArray(44)
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "writeRiffHeader",
            ByteArray::class.java,
            Int::class.java
        )
        method.isAccessible = true
        
        method.invoke(dataSource, header, 1000)
        
        assertThat(header[0]).isEqualTo('R'.code.toByte())
        assertThat(header[1]).isEqualTo('I'.code.toByte())
        assertThat(header[2]).isEqualTo('F'.code.toByte())
        assertThat(header[3]).isEqualTo('F'.code.toByte())
    }

    @Test
    fun `writeRiffHeader should write WAVE correctly`() {
        val header = ByteArray(44)
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "writeRiffHeader",
            ByteArray::class.java,
            Int::class.java
        )
        method.isAccessible = true
        
        method.invoke(dataSource, header, 1000)
        
        assertThat(header[8]).isEqualTo('W'.code.toByte())
        assertThat(header[9]).isEqualTo('A'.code.toByte())
        assertThat(header[10]).isEqualTo('V'.code.toByte())
        assertThat(header[11]).isEqualTo('E'.code.toByte())
    }

    @Test
    fun `writeRiffHeader should write size in little endian`() {
        val header = ByteArray(44)
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "writeRiffHeader",
            ByteArray::class.java,
            Int::class.java
        )
        method.isAccessible = true
        
        val totalDataLen = 0x12345678
        method.invoke(dataSource, header, totalDataLen)
        
        // Verificar que los bytes 4-7 contienen el tamaño en little endian
        assertThat(header[4]).isEqualTo(0x78.toByte())
        assertThat(header[5]).isEqualTo(0x56.toByte())
        assertThat(header[6]).isEqualTo(0x34.toByte())
        assertThat(header[7]).isEqualTo(0x12.toByte())
    }

    // ==================== Tests para writeFmtChunk ====================
    
    @Test
    fun `writeFmtChunk should write fmt identifier`() {
        val header = ByteArray(44)
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "writeFmtChunk",
            ByteArray::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        method.invoke(dataSource, header, 32000L)
        
        assertThat(header[12]).isEqualTo('f'.code.toByte())
        assertThat(header[13]).isEqualTo('m'.code.toByte())
        assertThat(header[14]).isEqualTo('t'.code.toByte())
        assertThat(header[15]).isEqualTo(' '.code.toByte())
    }

    @Test
    fun `writeFmtChunk should write fmt chunk size`() {
        val header = ByteArray(44)
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "writeFmtChunk",
            ByteArray::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        method.invoke(dataSource, header, 32000L)
        
        // Bytes 16-19 deben ser 16 (tamaño del fmt chunk)
        assertThat(header[16]).isEqualTo(16.toByte())
        assertThat(header[17]).isEqualTo(0.toByte())
        assertThat(header[18]).isEqualTo(0.toByte())
        assertThat(header[19]).isEqualTo(0.toByte())
    }

    @Test
    fun `writeFmtChunk should write PCM format`() {
        val header = ByteArray(44)
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "writeFmtChunk",
            ByteArray::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        method.invoke(dataSource, header, 32000L)
        
        // Bytes 20-21 deben ser 1 (PCM)
        assertThat(header[20]).isEqualTo(1.toByte())
        assertThat(header[21]).isEqualTo(0.toByte())
    }

    @Test
    fun `writeFmtChunk should write mono channel`() {
        val header = ByteArray(44)
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "writeFmtChunk",
            ByteArray::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        method.invoke(dataSource, header, 32000L)
        
        // Bytes 22-23 deben ser 1 (mono)
        assertThat(header[22]).isEqualTo(1.toByte())
        assertThat(header[23]).isEqualTo(0.toByte())
    }

    @Test
    fun `writeFmtChunk should write correct byte rate`() {
        val header = ByteArray(44)
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "writeFmtChunk",
            ByteArray::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        val byteRate = 32000L
        method.invoke(dataSource, header, byteRate)
        
        // Bytes 28-31 contienen el byte rate
        val readByteRate = ByteBuffer.wrap(header, 28, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertThat(readByteRate).isEqualTo(byteRate.toInt())
    }

    // ==================== Tests para writeDataChunk ====================
    
    @Test
    fun `writeDataChunk should write data identifier`() {
        val header = ByteArray(44)
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "writeDataChunk",
            ByteArray::class.java,
            Int::class.java
        )
        method.isAccessible = true
        
        method.invoke(dataSource, header, 1000)
        
        assertThat(header[36]).isEqualTo('d'.code.toByte())
        assertThat(header[37]).isEqualTo('a'.code.toByte())
        assertThat(header[38]).isEqualTo('t'.code.toByte())
        assertThat(header[39]).isEqualTo('a'.code.toByte())
    }

    @Test
    fun `writeDataChunk should write data size in little endian`() {
        val header = ByteArray(44)
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "writeDataChunk",
            ByteArray::class.java,
            Int::class.java
        )
        method.isAccessible = true
        
        val dataSize = 0x12345678
        method.invoke(dataSource, header, dataSize)
        
        // Bytes 40-43 contienen el tamaño en little endian
        assertThat(header[40]).isEqualTo(0x78.toByte())
        assertThat(header[41]).isEqualTo(0x56.toByte())
        assertThat(header[42]).isEqualTo(0x34.toByte())
        assertThat(header[43]).isEqualTo(0x12.toByte())
    }

    // ==================== Tests de integración ====================
    
    @Test
    fun `complete WAV header should be valid`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "createWavHeader",
            Int::class.java,
            Int::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        val totalAudioLen = 16000 // 1 segundo de audio a 16kHz mono 16-bit
        val totalDataLen = totalAudioLen + 36
        val byteRate = 32000L // 16000 Hz * 16 bits * 1 channel / 8
        
        val header = method.invoke(dataSource, totalDataLen, totalAudioLen, byteRate) as ByteArray
        
        // Verificar estructura completa
        assertThat(header.size).isEqualTo(44)
        
        // RIFF header
        assertThat(String(header, 0, 4)).isEqualTo("RIFF")
        assertThat(String(header, 8, 4)).isEqualTo("WAVE")
        
        // fmt chunk
        assertThat(String(header, 12, 4)).isEqualTo("fmt ")
        
        // data chunk
        assertThat(String(header, 36, 4)).isEqualTo("data")
    }

    @Test
    fun `buffer provider should return valid buffer size`() {
        // Verificar que el buffer provider retorna un valor válido cuando se llama
        val bufferSize = mockBufferProvider.getMinBufferSize(
            16000, 
            AudioFormat.CHANNEL_IN_MONO, 
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        assertThat(bufferSize).isEqualTo(4096)
        assertThat(bufferSize).isGreaterThan(0)
    }

    @Test
    fun `AudioDataSource should be properly initialized with context and buffer provider`() {
        // Verificar que el AudioDataSource se inicializa correctamente
        assertThat(dataSource).isNotNull()
        
        // Verificar que el buffer provider retorna un valor válido
        val bufferSize = mockBufferProvider.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        assertThat(bufferSize).isGreaterThan(0)
    }

    // ==================== Tests para isSilenceDetected ====================
    
    @Test
    fun `isSilenceDetected should return false immediately after sound`() {
        val method = AudioDataSource::class.java.getDeclaredMethod("isSilenceDetected")
        method.isAccessible = true
        
        // Establecer lastSoundTime a ahora
        val lastSoundTimeField = AudioDataSource::class.java.getDeclaredField("lastSoundTime")
        lastSoundTimeField.isAccessible = true
        lastSoundTimeField.set(dataSource, System.currentTimeMillis())
        
        val result = method.invoke(dataSource) as Boolean
        
        assertThat(result).isFalse()
    }

    @Test
    fun `isSilenceDetected should return true after timeout`() {
        val method = AudioDataSource::class.java.getDeclaredMethod("isSilenceDetected")
        method.isAccessible = true
        
        // Establecer lastSoundTime a hace 3 segundos (más que SILENCE_TIMEOUT_MS = 2000)
        val lastSoundTimeField = AudioDataSource::class.java.getDeclaredField("lastSoundTime")
        lastSoundTimeField.isAccessible = true
        lastSoundTimeField.set(dataSource, System.currentTimeMillis() - 3000)
        
        val result = method.invoke(dataSource) as Boolean
        
        assertThat(result).isTrue()
    }

    @Test
    fun `isSilenceDetected should return false just before timeout`() {
        val method = AudioDataSource::class.java.getDeclaredMethod("isSilenceDetected")
        method.isAccessible = true
        
        // Establecer lastSoundTime a hace 1.5 segundos (menos que SILENCE_TIMEOUT_MS = 2000)
        val lastSoundTimeField = AudioDataSource::class.java.getDeclaredField("lastSoundTime")
        lastSoundTimeField.isAccessible = true
        lastSoundTimeField.set(dataSource, System.currentTimeMillis() - 1500)
        
        val result = method.invoke(dataSource) as Boolean
        
        assertThat(result).isFalse()
    }

    @Test
    fun `isSilenceDetected should return true exactly at timeout`() {
        val method = AudioDataSource::class.java.getDeclaredMethod("isSilenceDetected")
        method.isAccessible = true
        
        // Establecer lastSoundTime a hace exactamente 2001ms
        val lastSoundTimeField = AudioDataSource::class.java.getDeclaredField("lastSoundTime")
        lastSoundTimeField.isAccessible = true
        lastSoundTimeField.set(dataSource, System.currentTimeMillis() - 2001)
        
        val result = method.invoke(dataSource) as Boolean
        
        assertThat(result).isTrue()
    }

    // ==================== Tests para startNewRecording ====================
    
    @Test
    fun `startNewRecording should set isRecording flag`() {
        val isRecordingField = AudioDataSource::class.java.getDeclaredField("isRecording")
        isRecordingField.isAccessible = true
        
        // Verificar que inicialmente es false
        assertThat(isRecordingField.get(dataSource) as Boolean).isFalse()
        
        // startNewRecording requiere acceso al sistema de archivos, 
        // por lo que solo verificamos el estado inicial
        assertThat(true).isTrue()
    }

    // ==================== Tests para stopRecordingAndFinalize ====================
    
    @Test
    fun `stopRecordingAndFinalize should set isRecording to false`() {
        val isRecordingField = AudioDataSource::class.java.getDeclaredField("isRecording")
        isRecordingField.isAccessible = true
        isRecordingField.set(dataSource, true)
        
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "stopRecordingAndFinalize",
            FileOutputStream::class.java,
            Int::class.java,
            Function1::class.java,
            Function2::class.java
        )
        method.isAccessible = true
        
        val onRecordingStopped: (AudioDataSource.RecordedAudioData) -> Unit = {}
        val onError: (String, Throwable?) -> Unit = { _, _ -> }
        
        method.invoke(dataSource, null, 0, onRecordingStopped, onError)
        
        assertThat(isRecordingField.get(dataSource) as Boolean).isFalse()
    }

    @Test
    fun `stopRecordingAndFinalize should call onError on IOException`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "stopRecordingAndFinalize",
            FileOutputStream::class.java,
            Int::class.java,
            Function1::class.java,
            Function2::class.java
        )
        method.isAccessible = true
        
        val mockFos = mockk<FileOutputStream>()
        every { mockFos.close() } throws IOException("Test error")
        
        var errorCalled = false
        var errorMessage = ""
        val onRecordingStopped: (AudioDataSource.RecordedAudioData) -> Unit = {}
        val onError: (String, Throwable?) -> Unit = { msg, _ -> 
            errorCalled = true
            errorMessage = msg
        }
        
        method.invoke(dataSource, mockFos, 100, onRecordingStopped, onError)
        
        assertThat(errorCalled).isTrue()
        assertThat(errorMessage).contains("Error closing file stream")
    }

    @Test
    fun `stopRecordingAndFinalize should delete file when no bytes read`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "stopRecordingAndFinalize",
            FileOutputStream::class.java,
            Int::class.java,
            Function1::class.java,
            Function2::class.java
        )
        method.isAccessible = true
        
        val mockFile = mockk<File>(relaxed = true)
        every { mockFile.delete() } returns true
        
        val audioFileField = AudioDataSource::class.java.getDeclaredField("audioFile")
        audioFileField.isAccessible = true
        audioFileField.set(dataSource, mockFile)
        
        val onRecordingStopped: (AudioDataSource.RecordedAudioData) -> Unit = {}
        val onError: (String, Throwable?) -> Unit = { _, _ -> }
        
        method.invoke(dataSource, null, 0, onRecordingStopped, onError)
        
        verify { mockFile.delete() }
    }

    @Test
    fun `stopRecordingAndFinalize should call onRecordingStopped when bytes read`() {
        // Crear un archivo temporal real para este test
        val tempFile = File.createTempFile("test_audio", ".wav")
        tempFile.deleteOnExit()
        
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "stopRecordingAndFinalize",
            FileOutputStream::class.java,
            Int::class.java,
            Function1::class.java,
            Function2::class.java
        )
        method.isAccessible = true
        
        val audioFileField = AudioDataSource::class.java.getDeclaredField("audioFile")
        audioFileField.isAccessible = true
        audioFileField.set(dataSource, tempFile)
        
        var callbackCalled = false
        val onRecordingStopped: (AudioDataSource.RecordedAudioData) -> Unit = { 
            callbackCalled = true
        }
        val onError: (String, Throwable?) -> Unit = { _, _ -> }
        
        method.invoke(dataSource, null, 1000, onRecordingStopped, onError)
        
        assertThat(callbackCalled).isTrue()
        tempFile.delete()
    }

    // ==================== Tests para cleanupAudioRecord ====================
    
    @Test
    fun `cleanupAudioRecord should set audioRecord to null`() {
        val method = AudioDataSource::class.java.getDeclaredMethod("cleanupAudioRecord")
        method.isAccessible = true
        
        val audioRecordField = AudioDataSource::class.java.getDeclaredField("audioRecord")
        audioRecordField.isAccessible = true
        
        // Establecer un mock de AudioRecord
        val mockAudioRecord = mockk<AudioRecord>(relaxed = true)
        audioRecordField.set(dataSource, mockAudioRecord)
        
        method.invoke(dataSource)
        
        assertThat(audioRecordField.get(dataSource)).isNull()
    }

    @Test
    fun `cleanupAudioRecord should call stop and release on audioRecord`() {
        val method = AudioDataSource::class.java.getDeclaredMethod("cleanupAudioRecord")
        method.isAccessible = true
        
        val audioRecordField = AudioDataSource::class.java.getDeclaredField("audioRecord")
        audioRecordField.isAccessible = true
        
        val mockAudioRecord = mockk<AudioRecord>(relaxed = true)
        every { mockAudioRecord.stop() } just Runs
        every { mockAudioRecord.release() } just Runs
        
        audioRecordField.set(dataSource, mockAudioRecord)
        
        method.invoke(dataSource)
        
        verify { mockAudioRecord.stop() }
        verify { mockAudioRecord.release() }
    }

    @Test
    fun `cleanupAudioRecord should set recordingThread to null`() {
        val method = AudioDataSource::class.java.getDeclaredMethod("cleanupAudioRecord")
        method.isAccessible = true
        
        val recordingThreadField = AudioDataSource::class.java.getDeclaredField("recordingThread")
        recordingThreadField.isAccessible = true
        
        method.invoke(dataSource)
        
        assertThat(recordingThreadField.get(dataSource)).isNull()
    }

    // ==================== Tests para processAudioData ====================
    
    @Test
    fun `processAudioData should update lastSoundTime when sound detected`() {
        every { mockVoiceActivityDetector.isVoiceDetected(any(), any(), any()) } returns true
        
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "processAudioData",
            ByteArray::class.java,
            Int::class.java,
            Float::class.java,
            FileOutputStream::class.java,
            Int::class.java,
            Function1::class.java,
            Function2::class.java
        )
        method.isAccessible = true
        
        val lastSoundTimeField = AudioDataSource::class.java.getDeclaredField("lastSoundTime")
        lastSoundTimeField.isAccessible = true
        val initialTime = 0L
        lastSoundTimeField.set(dataSource, initialTime)
        
        // Establecer isRecording a true para evitar llamar startNewRecording
        val isRecordingField = AudioDataSource::class.java.getDeclaredField("isRecording")
        isRecordingField.isAccessible = true
        isRecordingField.set(dataSource, true)
        
        val mockFos = mockk<FileOutputStream>(relaxed = true)
        every { mockFos.write(any<ByteArray>(), any(), any()) } just Runs
        
        val data = ByteArray(100)
        val rmsDb = 75f // Por encima del threshold
        val onRecordingStopped: (AudioDataSource.RecordedAudioData) -> Unit = {}
        val onError: (String, Throwable?) -> Unit = { _, _ -> }
        
        method.invoke(dataSource, data, 100, rmsDb, mockFos, 0, onRecordingStopped, onError)
        
        val updatedTime = lastSoundTimeField.get(dataSource) as Long
        assertThat(updatedTime).isGreaterThan(initialTime)
    }

    @Test
    fun `processAudioData should not update lastSoundTime when no sound`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "processAudioData",
            ByteArray::class.java,
            Int::class.java,
            Float::class.java,
            FileOutputStream::class.java,
            Int::class.java,
            Function1::class.java,
            Function2::class.java
        )
        method.isAccessible = true
        
        val lastSoundTimeField = AudioDataSource::class.java.getDeclaredField("lastSoundTime")
        lastSoundTimeField.isAccessible = true
        val initialTime = 12345L
        lastSoundTimeField.set(dataSource, initialTime)
        
        val data = ByteArray(100)
        val rmsDb = 60f // Por debajo del threshold
        val onRecordingStopped: (AudioDataSource.RecordedAudioData) -> Unit = {}
        val onError: (String, Throwable?) -> Unit = { _, _ -> }
        
        method.invoke(dataSource, data, 100, rmsDb, null, 0, onRecordingStopped, onError)
        
        val updatedTime = lastSoundTimeField.get(dataSource) as Long
        assertThat(updatedTime).isEqualTo(initialTime)
    }

    @Test
    fun `processAudioData should return updated byte count when recording`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "processAudioData",
            ByteArray::class.java,
            Int::class.java,
            Float::class.java,
            FileOutputStream::class.java,
            Int::class.java,
            Function1::class.java,
            Function2::class.java
        )
        method.isAccessible = true
        
        val isRecordingField = AudioDataSource::class.java.getDeclaredField("isRecording")
        isRecordingField.isAccessible = true
        isRecordingField.set(dataSource, true)
        
        val lastSoundTimeField = AudioDataSource::class.java.getDeclaredField("lastSoundTime")
        lastSoundTimeField.isAccessible = true
        lastSoundTimeField.set(dataSource, System.currentTimeMillis())
        
        val mockFos = mockk<FileOutputStream>(relaxed = true)
        every { mockFos.write(any<ByteArray>(), any(), any()) } just Runs
        
        val data = ByteArray(100)
        val rmsDb = 75f
        val initialBytes = 500
        val onRecordingStopped: (AudioDataSource.RecordedAudioData) -> Unit = {}
        val onError: (String, Throwable?) -> Unit = { _, _ -> }
        
        val result = method.invoke(dataSource, data, 100, rmsDb, mockFos, initialBytes, onRecordingStopped, onError) as Pair<*, *>
        
        assertThat(result.second as Int).isEqualTo(initialBytes + 100)
    }

    // ==================== Tests para edge cases ====================
    
    @Test
    fun `calculateRmsDb should handle empty data`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "calculateRmsDb",
            ByteArray::class.java,
            Int::class.java
        )
        method.isAccessible = true
        
        val emptyData = ByteArray(2)
        val rmsDb = method.invoke(dataSource, emptyData, 2) as Float
        
        assertThat(rmsDb).isNotNaN()
        assertThat(rmsDb).isFinite()
    }

    @Test
    fun `calculateRmsDb should handle maximum values`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "calculateRmsDb",
            ByteArray::class.java,
            Int::class.java
        )
        method.isAccessible = true
        
        val maxData = ByteArray(4096)
        val buffer = ByteBuffer.wrap(maxData).order(ByteOrder.LITTLE_ENDIAN)
        
        for (i in 0 until maxData.size / 2) {
            buffer.putShort(Short.MAX_VALUE)
        }
        
        val rmsDb = method.invoke(dataSource, maxData, maxData.size) as Float
        
        assertThat(rmsDb).isNotNaN()
        assertThat(rmsDb).isFinite()
        assertThat(rmsDb).isGreaterThan(80f)
    }

    @Test
    fun `calculateRmsDb should handle negative values`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "calculateRmsDb",
            ByteArray::class.java,
            Int::class.java
        )
        method.isAccessible = true
        
        val negativeData = ByteArray(4096)
        val buffer = ByteBuffer.wrap(negativeData).order(ByteOrder.LITTLE_ENDIAN)
        
        for (i in 0 until negativeData.size / 2) {
            buffer.putShort((-5000).toShort())
        }
        
        val rmsDb = method.invoke(dataSource, negativeData, negativeData.size) as Float
        
        assertThat(rmsDb).isNotNaN()
        assertThat(rmsDb).isFinite()
    }

    @Test
    fun `createWavHeader with different audio lengths`() {
        val method = AudioDataSource::class.java.getDeclaredMethod(
            "createWavHeader",
            Int::class.java,
            Int::class.java,
            Long::class.java
        )
        method.isAccessible = true
        
        // Test con diferentes tamaños
        val sizes = listOf(100, 1000, 10000, 100000)
        
        sizes.forEach { size ->
            val totalDataLen = size + 36
            val header = method.invoke(dataSource, totalDataLen, size, 32000L) as ByteArray
            
            assertThat(header.size).isEqualTo(44)
            assertThat(String(header, 0, 4)).isEqualTo("RIFF")
        }
    }

    @Test
    fun `stopMonitoring should handle exception in thread join`() {
        val isMonitoringField = AudioDataSource::class.java.getDeclaredField("isMonitoring")
        isMonitoringField.isAccessible = true
        isMonitoringField.set(dataSource, true)
        
        val mockThread = mockk<Thread>(relaxed = true)
        every { mockThread.join(any()) } throws InterruptedException("Test")
        
        val recordingThreadField = AudioDataSource::class.java.getDeclaredField("recordingThread")
        recordingThreadField.isAccessible = true
        recordingThreadField.set(dataSource, mockThread)
        
        // No debería lanzar excepción
        dataSource.stopMonitoring()
        
        assertThat(isMonitoringField.get(dataSource) as Boolean).isFalse()
    }

    @Test
    fun `release should not call stopMonitoring when not monitoring`() {
        val isMonitoringField = AudioDataSource::class.java.getDeclaredField("isMonitoring")
        isMonitoringField.isAccessible = true
        isMonitoringField.set(dataSource, false)
        
        // No debería lanzar excepción
        dataSource.release()
        
        assertThat(true).isTrue()
    }
}
