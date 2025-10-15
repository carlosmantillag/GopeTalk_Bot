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
    private var isRecording = false

    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    fun isRecording(): Boolean = isRecording

    fun startCommandRecording() {
        if (isRecording) return
        try {
            audioFile = File(context.cacheDir, "command.wav")
            audioRecord = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                logError("AudioRecord could not be initialized.", null)
                return
            }

            isRecording = true
            audioRecord?.startRecording()
            logInfo("Started recording command audio.")

            recordingThread = thread {
                writeAudioDataToFile()
            }


        } catch (e: SecurityException) {
            logError("Audio recording permission not granted.", e)
        }
    }

    private fun writeAudioDataToFile() {
        val data = ByteArray(bufferSize)
        val file = audioFile ?: return
        try {
            FileOutputStream(file).use { fos ->
                // Write a placeholder for the WAV header
                fos.write(ByteArray(44))

                var totalBytesRead = 0
                while (isRecording) {
                    val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                    if (read > 0) {
                        calculateRms(data, read)
                        try {
                            fos.write(data, 0, read)
                            totalBytesRead += read
                        } catch (e: IOException) {
                            logError("Error writing audio data to file.", e)
                            break
                        }
                    }
                }
                // Now that we know the size, write the real WAV header
                writeWavHeader(file, totalBytesRead)
            }
        } catch (e: IOException) {
            logError("Could not create audio file output stream.", e)
        }
    }

    private fun calculateRms(data: ByteArray, readSize: Int) {
        val shortData = ShortArray(readSize / 2)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortData)

        val sumOfSquares = shortData.sumOf { it.toDouble() * it.toDouble() }
        val rms = sqrt(sumOfSquares / shortData.size)
        AudioRmsMonitor.updateRmsDb(rms.toFloat())
    }


    fun stopCommandRecording(): File? {
        if (!isRecording) return null

        isRecording = false
        try {
            recordingThread?.join(500) // Wait a bit for the thread to finish
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingThread = null
            logInfo("Stopped recording command audio.")
        } catch (e: Exception) {
            logError("Error stopping AudioRecord.", e)
        }
        return audioFile
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
        if (isRecording) {
            stopCommandRecording()
        }
    }
}