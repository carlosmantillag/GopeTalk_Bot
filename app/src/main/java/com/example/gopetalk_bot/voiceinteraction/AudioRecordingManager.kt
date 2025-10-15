package com.example.gopetalk_bot.voiceinteraction

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.example.gopetalk_bot.main.AudioRmsMonitor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread
import kotlin.math.log10
import kotlin.math.sqrt

class AudioRecordingManager(
    private val context: Context,
    private val onRecordingStopped: (File) -> Unit,
    private val logInfo: (String) -> Unit,
    private val logError: (String, Throwable?) -> Unit
) {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var audioFile: File? = null
    @Volatile
    private var isMonitoring = false
    @Volatile
    private var isRecording = false

    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    // Silence detection parameters
    private val silenceThresholdDb = 70 // Quieter than a quiet library
    private val silenceTimeoutMs = 2000L // 2 seconds of silence to stop
    private var lastSoundTime = 0L
    private val handler = Handler(Looper.getMainLooper())

    fun isRecording(): Boolean = isRecording

    fun startMonitoring() {
        if (isMonitoring) return
        try {
            audioRecord = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                logError("AudioRecord could not be initialized.", null)
                return
            }

            isMonitoring = true
            audioRecord?.startRecording()
            logInfo("Started audio monitoring.")

            recordingThread = thread {
                monitorAudio()
            }
        } catch (e: SecurityException) {
            logError("Audio recording permission not granted.", e)
        }
    }

    private fun monitorAudio() {
        val data = ByteArray(bufferSize)
        var fos: FileOutputStream? = null
        var totalBytesRead = 0

        while (isMonitoring) {
            val read = audioRecord?.read(data, 0, bufferSize) ?: 0
            if (read > 0) {
                val rmsDb = calculateRmsDb(data, read)
                logInfo("Current audio level (dB): $rmsDb")
                AudioRmsMonitor.updateRmsDb(rmsDb)

                if (rmsDb > silenceThresholdDb) {
                    lastSoundTime = System.currentTimeMillis()
                    if (!isRecording) {
                        isRecording = true
                        logInfo("Sound detected, starting recording.")
                        audioFile = File(context.cacheDir, "command.wav")
                        fos = FileOutputStream(audioFile)
                        // Write a placeholder for the WAV header
                        fos.write(ByteArray(44))
                    }
                }

                if (isRecording) {
                    fos?.write(data, 0, read)
                    totalBytesRead += read

                    if (System.currentTimeMillis() - lastSoundTime > silenceTimeoutMs) {
                        logInfo("Silence detected, stopping recording.")
                        stopRecordingAndFinalize(fos, totalBytesRead)
                        totalBytesRead = 0 // Reset for next recording
                    }
                }
            }
        }
        // If monitoring stops while recording, finalize the file.
        if (isRecording) {
            stopRecordingAndFinalize(fos, totalBytesRead)
        }
    }

    private fun stopRecordingAndFinalize(fos: FileOutputStream?, totalBytesRead: Int) {
        isRecording = false
        try {
            fos?.close()
            audioFile?.let { file ->
                if (totalBytesRead > 0) {
                    writeWavHeader(file, totalBytesRead)
                    handler.post { onRecordingStopped(file) }
                } else {
                    file.delete() // Delete empty file
                }
            }
        } catch (e: IOException) {
            logError("Error closing file stream.", e)
        }
    }


    private fun calculateRmsDb(data: ByteArray, readSize: Int): Float {
        val shortData = ShortArray(readSize / 2)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortData)

        val sumOfSquares = shortData.sumOf { it.toDouble() * it.toDouble() }
        val rms = sqrt(sumOfSquares / shortData.size)

        // Clamp RMS to avoid log10(0)
        val clampedRms = if (rms > 0) rms else 1.0
        // Basic dB calculation, you might need a reference value for more accuracy
        val db = 20 * log10(clampedRms)
        return db.toFloat()
    }


    fun stopMonitoring() {
        if (!isMonitoring) return

        isMonitoring = false
        try {
            recordingThread?.join(500) // Wait a bit for the thread to finish
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingThread = null
            logInfo("Stopped audio monitoring.")
        } catch (e: Exception) {
            logError("Error stopping AudioRecord.", e)
        }
    }

    @Throws(IOException::class)
    private fun writeWavHeader(file: File, totalAudioLen: Int) {
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = (sampleRate * 16 * channels / 8).toLong()

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte(); header[5] = (totalDataLen shr 8 and 0xff).toByte(); header[6] = (totalDataLen shr 16 and 0xff).toByte(); header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = channels.toByte(); header[23] = 0
        ByteBuffer.wrap(header, 24, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate)
        ByteBuffer.wrap(header, 28, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(byteRate.toInt())
        header[32] = (2 * 16 / 8).toByte(); header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte(); header[41] = (totalAudioLen shr 8 and 0xff).toByte(); header[42] = (totalAudioLen shr 16 and 0xff).toByte(); header[43] = (totalAudioLen shr 24 and 0xff).toByte()

        RandomAccessFile(file, "rw").use {
            it.seek(0)
            it.write(header)
        }
    }

    fun release() {
        if (isMonitoring) {
            stopMonitoring()
        }
    }
}