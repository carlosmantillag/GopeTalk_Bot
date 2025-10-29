package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread
import kotlin.math.log10
import kotlin.math.sqrt

interface AudioBufferProvider {
    fun getMinBufferSize(sampleRate: Int, channelConfig: Int, audioFormat: Int): Int
}

class AndroidAudioBufferProvider : AudioBufferProvider {
    override fun getMinBufferSize(sampleRate: Int, channelConfig: Int, audioFormat: Int): Int {
        return AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    }
}

class AudioDataSource(
    private val context: Context,
    private val bufferProvider: AudioBufferProvider = AndroidAudioBufferProvider(),
    private val adaptiveThresholdManager: AdaptiveNoiseThresholdManager = AdaptiveNoiseThresholdManager(),
    private val voiceActivityDetector: VoiceActivityDetector = VoiceActivityDetector()
) {

    private companion object {
        const val TAG = "AudioDataSource"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val SILENCE_TIMEOUT_MS = 3000L
        const val UNCONFIRMED_VOICE_TIMEOUT_MS = 1500L
        const val WAV_HEADER_SIZE = 44
        const val AUDIO_FILENAME = "command.wav"
        const val THREAD_JOIN_TIMEOUT = 500L
        const val MIN_RMS = 1.0
        const val RMS_DB_MULTIPLIER = 20
        const val BITS_PER_SAMPLE = 16
        const val CHANNELS = 1
        const val STATUS_LOG_INTERVAL = 50
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var audioFile: File? = null
    
    @Volatile private var isMonitoring = false
    @Volatile private var isRecording = false
    @Volatile private var isPaused = false
    @Volatile private var lastSoundTime = 0L
    @Volatile private var voiceConfirmed = false
    private var sampleCount = 0

    private val bufferSize by lazy { 
        bufferProvider.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    }

    data class AudioLevelData(val rmsDb: Float)
    data class RecordedAudioData(val file: File)

    fun startMonitoring(
        onAudioLevel: (AudioLevelData) -> Unit,
        onRecordingStopped: (RecordedAudioData) -> Unit,
        onError: (String, Throwable?) -> Unit
    ) {
        if (isMonitoring) return
        
        try {
            initializeAudioRecord(onError) ?: return
            startRecordingThread(onAudioLevel, onRecordingStopped, onError)
        } catch (e: SecurityException) {
            onError("Audio recording permission not granted.", e)
        }
    }

    @RequiresPermission("android.permission.RECORD_AUDIO")
    private fun initializeAudioRecord(onError: (String, Throwable?) -> Unit): AudioRecord? {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )
        
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            onError("AudioRecord could not be initialized.", null)
            return null
        }
        
        return audioRecord
    }

    private fun startRecordingThread(
        onAudioLevel: (AudioLevelData) -> Unit,
        onRecordingStopped: (RecordedAudioData) -> Unit,
        onError: (String, Throwable?) -> Unit
    ) {
        isMonitoring = true
        audioRecord?.startRecording()
        Log.d(TAG, "Started audio monitoring.")

        recordingThread = thread {
            monitorAudio(onAudioLevel, onRecordingStopped, onError)
        }
    }

    private fun monitorAudio(
        onAudioLevel: (AudioLevelData) -> Unit,
        onRecordingStopped: (RecordedAudioData) -> Unit,
        onError: (String, Throwable?) -> Unit
    ) {
        val data = ByteArray(bufferSize)
        var fos: FileOutputStream? = null
        var totalBytesRead = 0

        while (isMonitoring) {
            val read = audioRecord?.read(data, 0, bufferSize) ?: 0
            if (read > 0) {
                val rmsDb = calculateRmsDb(data, read)
                onAudioLevel(AudioLevelData(rmsDb))

                if (!isPaused) {
                    adaptiveThresholdManager.processAudioLevel(rmsDb, isRecording)
                    
                    sampleCount++
                    if (sampleCount % STATUS_LOG_INTERVAL == 0) {
                        logAdaptiveStatus(rmsDb)
                    }
                    
                    val result = processAudioData(data, read, rmsDb, fos, totalBytesRead, onRecordingStopped, onError)
                    fos = result.first
                    totalBytesRead = result.second
                }
            }
        }
        
        if (isRecording) {
            stopRecordingAndFinalize(fos, totalBytesRead, onRecordingStopped, onError)
        }
    }

    private fun processAudioData(
        data: ByteArray,
        read: Int,
        rmsDb: Float,
        fos: FileOutputStream?,
        totalBytesRead: Int,
        onRecordingStopped: (RecordedAudioData) -> Unit,
        onError: (String, Throwable?) -> Unit
    ): Pair<FileOutputStream?, Int> {
        var outputStream = fos
        var bytesRead = totalBytesRead

        val shortData = ShortArray(read / 2)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortData)
        
        val isAboveThreshold = isSoundDetected(rmsDb)
        val isVoice = voiceActivityDetector.isVoiceDetected(rmsDb, isAboveThreshold, shortData)
        
        if (isAboveThreshold && !isRecording) {
            lastSoundTime = System.currentTimeMillis()
            outputStream = startNewRecording()
            voiceConfirmed = false
            Log.d(TAG, "🎤 Sonido detectado, iniciando grabación temporal...")
        }
        
        if (isVoice && !voiceConfirmed) {
            voiceConfirmed = true
            Log.d(TAG, "✓ VOZ CONFIRMADA - Grabación validada")
        }
        
        if (isVoice || (isAboveThreshold && voiceConfirmed)) {
            lastSoundTime = System.currentTimeMillis()
        }

        if (isRecording) {
            outputStream?.write(data, 0, read)
            bytesRead += read

            if (isSilenceDetected()) {
                Log.d(TAG, "Silencio detectado, finalizando grabación...")
                if (voiceConfirmed) {
                    Log.d(TAG, "✓ Enviando audio al backend")
                    stopRecordingAndFinalize(outputStream, bytesRead, onRecordingStopped, onError)
                } else {
                    Log.d(TAG, "✗ Descartando audio (no se confirmó voz)")
                    discardRecording(outputStream)
                }
                return Pair(null, 0)
            }
        }

        return Pair(outputStream, bytesRead)
    }

    private fun isSoundDetected(rmsDb: Float): Boolean {
        val threshold = adaptiveThresholdManager.getCurrentThreshold()
        return rmsDb > threshold
    }

    private fun isSilenceDetected(): Boolean {
        val elapsed = System.currentTimeMillis() - lastSoundTime
        val timeout = if (voiceConfirmed) SILENCE_TIMEOUT_MS else UNCONFIRMED_VOICE_TIMEOUT_MS
        
        return elapsed > timeout
    }

    private fun startNewRecording(): FileOutputStream {
        isRecording = true
        val threshold = adaptiveThresholdManager.getCurrentThreshold()
        val ambient = adaptiveThresholdManager.getAmbientNoiseLevel()
        val environment = adaptiveThresholdManager.getEnvironmentType()
        Log.d(TAG, "  - Ambiente: $environment (${String.format("%.1f", ambient)} dB)")
        Log.d(TAG, "  - Umbral usado: ${String.format("%.1f", threshold)} dB")
        audioFile = File(context.cacheDir, AUDIO_FILENAME)
        val fos = FileOutputStream(audioFile)
        fos.write(ByteArray(WAV_HEADER_SIZE))
        return fos
    }

    private fun stopRecordingAndFinalize(
        fos: FileOutputStream?,
        totalBytesRead: Int,
        onRecordingStopped: (RecordedAudioData) -> Unit,
        onError: (String, Throwable?) -> Unit
    ) {
        isRecording = false
        voiceActivityDetector.reset()
        try {
            fos?.close()
            audioFile?.let { file ->
                if (totalBytesRead > 0) {
                    writeWavHeader(file, totalBytesRead)
                    onRecordingStopped(RecordedAudioData(file))
                } else {
                    file.delete()
                }
            }
        } catch (e: IOException) {
            onError("Error closing file stream.", e)
        }
    }
    
    private fun discardRecording(fos: FileOutputStream?) {
        isRecording = false
        voiceConfirmed = false
        voiceActivityDetector.reset()
        try {
            fos?.close()
            audioFile?.delete()
            audioFile = null
        } catch (e: IOException) {
            Log.e(TAG, "Error descartando grabación", e)
        }
    }

    private fun calculateRmsDb(data: ByteArray, readSize: Int): Float {
        val shortData = ShortArray(readSize / 2)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortData)

        val sumOfSquares = shortData.sumOf { it.toDouble() * it.toDouble() }
        val rms = sqrt(sumOfSquares / shortData.size)
        val clampedRms = if (rms > 0) rms else MIN_RMS
        
        return (RMS_DB_MULTIPLIER * log10(clampedRms)).toFloat()
    }

    @Throws(IOException::class)
    private fun writeWavHeader(file: File, totalAudioLen: Int) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = (SAMPLE_RATE * BITS_PER_SAMPLE * CHANNELS / 8).toLong()
        val header = createWavHeader(totalDataLen, totalAudioLen, byteRate)

        RandomAccessFile(file, "rw").use {
            it.seek(0)
            it.write(header)
        }
    }

    private fun createWavHeader(totalDataLen: Int, totalAudioLen: Int, byteRate: Long): ByteArray {
        val header = ByteArray(WAV_HEADER_SIZE)
        
        writeRiffHeader(header, totalDataLen)
        writeFmtChunk(header, byteRate)
        writeDataChunk(header, totalAudioLen)
        
        return header
    }

    private fun writeRiffHeader(header: ByteArray, totalDataLen: Int) {
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
    }

    private fun writeFmtChunk(header: ByteArray, byteRate: Long) {
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = CHANNELS.toByte(); header[23] = 0
        ByteBuffer.wrap(header, 24, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(SAMPLE_RATE)
        ByteBuffer.wrap(header, 28, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(byteRate.toInt())
        header[32] = (2 * BITS_PER_SAMPLE / 8).toByte(); header[33] = 0
        header[34] = BITS_PER_SAMPLE.toByte(); header[35] = 0
    }

    private fun writeDataChunk(header: ByteArray, totalAudioLen: Int) {
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()
    }

    fun pauseRecording() {
        isPaused = true
    }

    fun resumeRecording() {
        isPaused = false
    }

    fun stopMonitoring() {
        if (!isMonitoring) return

        isMonitoring = false
        try {
            recordingThread?.join(THREAD_JOIN_TIMEOUT)
            cleanupAudioRecord()
        } catch (e: Exception) {
            // Error stopping AudioRecord
        }
    }

    private fun cleanupAudioRecord() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordingThread = null
    }

    fun release() {
        if (isMonitoring) stopMonitoring()
    }

    private fun logAdaptiveStatus(currentRmsDb: Float) {
        if (adaptiveThresholdManager.isCalibrated()) {
            val threshold = adaptiveThresholdManager.getCurrentThreshold()
            val ambient = adaptiveThresholdManager.getAmbientNoiseLevel()
            val environment = adaptiveThresholdManager.getEnvironmentType()
            Log.v(TAG, "Audio: ${String.format("%.1f", currentRmsDb)} dB | " +
                      "Ambiente: $environment (${String.format("%.1f", ambient)} dB) | " +
                      "Umbral: ${String.format("%.1f", threshold)} dB")
        }
    }

    fun getAdaptiveStatus(): String {
        return adaptiveThresholdManager.getStatusInfo()
    }

    fun resetAdaptiveSystem() {
        adaptiveThresholdManager.reset()
        Log.i(TAG, "Sistema adaptativo reiniciado")
    }

    fun forceRecalibration() {
        adaptiveThresholdManager.forceRecalibration()
        Log.i(TAG, "Recalibración forzada")
    }
}