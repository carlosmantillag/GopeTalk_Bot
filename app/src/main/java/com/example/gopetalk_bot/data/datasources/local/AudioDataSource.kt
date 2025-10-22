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

/**
 * Interface para obtener el buffer size - permite testing sin Robolectric
 */
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
    private val bufferProvider: AudioBufferProvider = AndroidAudioBufferProvider()
) {

    private companion object {
        const val TAG = "AudioDataSource"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val SILENCE_THRESHOLD_DB = 70f
        const val SILENCE_TIMEOUT_MS = 2000L
        const val WAV_HEADER_SIZE = 44
        const val AUDIO_FILENAME = "command.wav"
        const val THREAD_JOIN_TIMEOUT = 500L
        const val MIN_RMS = 1.0
        const val RMS_DB_MULTIPLIER = 20
        const val BITS_PER_SAMPLE = 16
        const val CHANNELS = 1
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var audioFile: File? = null
    
    @Volatile private var isMonitoring = false
    @Volatile private var isRecording = false
    @Volatile private var isPaused = false
    @Volatile private var lastSoundTime = 0L

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

        if (isSoundDetected(rmsDb)) {
            lastSoundTime = System.currentTimeMillis()
            if (!isRecording) {
                outputStream = startNewRecording()
            }
        }

        if (isRecording) {
            outputStream?.write(data, 0, read)
            bytesRead += read

            if (isSilenceDetected()) {
                Log.d(TAG, "Silence detected, stopping recording.")
                stopRecordingAndFinalize(outputStream, bytesRead, onRecordingStopped, onError)
                return Pair(null, 0)
            }
        }

        return Pair(outputStream, bytesRead)
    }

    private fun isSoundDetected(rmsDb: Float): Boolean = rmsDb > SILENCE_THRESHOLD_DB

    private fun isSilenceDetected(): Boolean {
        return System.currentTimeMillis() - lastSoundTime > SILENCE_TIMEOUT_MS
    }

    private fun startNewRecording(): FileOutputStream {
        isRecording = true
        Log.d(TAG, "Sound detected, starting recording.")
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
}
