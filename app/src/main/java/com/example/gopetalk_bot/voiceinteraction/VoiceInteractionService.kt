package com.example.gopetalk_bot.voiceinteraction

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gopetalk_bot.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.concurrent.thread

class VoiceInteractionService : Service(), VoiceInteractionContract.View {

    private lateinit var presenter: VoiceInteractionContract.Presenter
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech

    // State Management
    private var isHotwordDetected = false
    private var isRecording = false

    // Audio Recording
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var audioFile: File? = null
    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    companion object {
        private const val TAG = "VoiceInteractionService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "VoiceInteractionChannel"
        private const val HOTWORD = "gopebot"
    }

    override fun onCreate() {
        super.onCreate()
        presenter = VoiceInteractionPresenter(this)
        setupNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        setupTextToSpeech()
        setupSpeechRecognizer()
        presenter.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startListeningForHotword()
        return START_STICKY
    }

    private fun setupTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.forLanguageTag("es-ES")
            } else {
                logError("Failed to initialize TextToSpeech.")
            }
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            logError("Speech recognition is not available.")
            stopSelf()
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    if (!isHotwordDetected && text.lowercase(Locale.getDefault()).contains(HOTWORD)) {
                        isHotwordDetected = true
                        presenter.onHotwordDetected()
                    }
                }

                override fun onEndOfSpeech() {
                    if (isHotwordDetected) {
                        presenter.onSpeechEnded()
                    }
                }

                override fun onResults(results: Bundle?) {
                    // The final result is ignored, we just restart listening for the hotword.
                    isHotwordDetected = false
                    startListeningForHotword()
                }

                override fun onError(error: Int) {
                    if (error != SpeechRecognizer.ERROR_CLIENT && error != SpeechRecognizer.ERROR_NO_MATCH) {
                        logError("SpeechRecognizer error: $error")
                    }
                    isHotwordDetected = false
                    startListeningForHotword()
                }
            })
        }
    }

    override fun startListeningForHotword() {
        if (isRecording) return // Don't interrupt command recording
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            logError("Error starting listening: ${e.message}")
        }
    }

    override fun startCommandRecording() {
        if (isRecording) return
        try {
            audioFile = File(cacheDir, "command.wav")
            audioRecord = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                logError("AudioRecord could not be initialized.")
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
        val fos = try {
            FileOutputStream(file)
        } catch (e: IOException) {
            logError("Could not create audio file output stream.", e)
            return
        }

        fos.use { outputStream ->
            // Write a placeholder for the WAV header
            outputStream.write(ByteArray(44))

            var totalBytesRead = 0
            while (isRecording) {
                val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                if (read > 0) {
                    try {
                        outputStream.write(data, 0, read)
                        totalBytesRead += read
                    } catch (e: IOException) {
                        logError("Error writing audio data to file.", e)
                        break
                    }
                }
            }

            try {
                // Now that we know the size, write the real WAV header
                writeWavHeader(FileOutputStream(file, false), totalBytesRead)
            } catch (e: IOException) {
                logError("Error writing WAV header.", e)
            }
        }
    }

    override fun stopCommandRecording(): File? {
        if (!isRecording) return null

        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingThread?.join()
            logInfo("Stopped recording command audio.")
        } catch (e: Exception) {
            logError("Error stopping AudioRecord.", e)
        }
        return audioFile
    }

    @Throws(IOException::class)
    private fun writeWavHeader(fos: FileOutputStream, totalAudioLen: Int) {
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

        fos.use { it.write(header, 0, 44) }
    }

    override fun speak(text: String) {
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun logInfo(message: String) {
        Log.i(TAG, message)
    }

    override fun logError(message: String, t: Throwable?) {
        if (t != null) {
            Log.e(TAG, message, t)
        } else {
            Log.e(TAG, message)
        }
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Voice Interaction Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("GopeTalk Bot Activo")
        .setContentText("Escuchando por el hotword '$HOTWORD'...")
        .setSmallIcon(R.mipmap.ic_launcher)
        .build()

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        recordingThread?.interrupt()
        audioRecord?.release()
        presenter.stop()
        speechRecognizer.destroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
