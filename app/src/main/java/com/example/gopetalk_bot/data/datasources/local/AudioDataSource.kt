package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
 * Local data source for audio recording and monitoring
 */
class AudioDataSource(
    private val context: Context
) {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var audioFile: File? = null
    
    @Volatile
    private var isMonitoring = false
    
    @Volatile
    private var isRecording = false
    
    @Volatile
    private var isPaused = false

    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    // Audio detection thresholds
    private val silenceThresholdDb = 70f
    private val silenceTimeoutMs = 2000L
    private var lastSoundTime = 0L

    data class AudioLevelData(val rmsDb: Float)
    data class RecordedAudioData(val file: File)

    fun startMonitoring(
        onAudioLevel: (AudioLevelData) -> Unit,
        onRecordingStopped: (RecordedAudioData) -> Unit,
        onError: (String, Throwable?) -> Unit
    ) {
        if (isMonitoring) return
        
        try {
            audioRecord = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("AudioRecord could not be initialized.", null)
                return
            }

            isMonitoring = true
            audioRecord?.startRecording()
            Log.d(TAG, "Started audio monitoring.")

            recordingThread = thread {
                monitorAudio(onAudioLevel, onRecordingStopped, onError)
            }
        } catch (e: SecurityException) {
            onError("Audio recording permission not granted.", e)
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

                // Skip processing if paused (e.g., during TTS playback)
                if (isPaused) {
                    continue
                }

                if (rmsDb > silenceThresholdDb) {
                    lastSoundTime = System.currentTimeMillis()
                    if (!isRecording) {
                        isRecording = true
                        Log.d(TAG, "Sound detected, starting recording.")
                        audioFile = File(context.cacheDir, "command.wav")
                        fos = FileOutputStream(audioFile)
                        fos.write(ByteArray(44)) // WAV header placeholder
                    }
                }

                if (isRecording) {
                    fos?.write(data, 0, read)
                    totalBytesRead += read

                    if (System.currentTimeMillis() - lastSoundTime > silenceTimeoutMs) {
                        Log.d(TAG, "Silence detected, stopping recording.")
                        stopRecordingAndFinalize(fos, totalBytesRead, onRecordingStopped, onError)
                        totalBytesRead = 0
                    }
                }
            }
        }
        
        if (isRecording) {
            stopRecordingAndFinalize(fos, totalBytesRead, onRecordingStopped, onError)
        }
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

        val clampedRms = if (rms > 0) rms else 1.0
        val db = 20 * log10(clampedRms)
        return db.toFloat()
    }

    @Throws(IOException::class)
    private fun writeWavHeader(file: File, totalAudioLen: Int) {
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = (sampleRate * 16 * channels / 8).toLong()

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = channels.toByte(); header[23] = 0
        ByteBuffer.wrap(header, 24, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate)
        ByteBuffer.wrap(header, 28, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(byteRate.toInt())
        header[32] = (2 * 16 / 8).toByte(); header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()

        RandomAccessFile(file, "rw").use {
            it.seek(0)
            it.write(header)
        }
    }

    fun pauseRecording() {
        isPaused = true
        Log.d(TAG, "Audio recording paused.")
    }

    fun resumeRecording() {
        isPaused = false
        Log.d(TAG, "Audio recording resumed.")
    }

    fun stopMonitoring() {
        if (!isMonitoring) return

        isMonitoring = false
        try {
            recordingThread?.join(500)
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingThread = null
            Log.d(TAG, "Stopped audio monitoring.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord.", e)
        }
    }

    fun release() {
        if (isMonitoring) {
            stopMonitoring()
        }
    }

    companion object {
        private const val TAG = "AudioDataSource"
    }
}
