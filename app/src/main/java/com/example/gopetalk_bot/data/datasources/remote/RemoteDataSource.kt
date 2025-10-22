package com.example.gopetalk_bot.data.datasources.remote

import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.example.gopetalk_bot.BuildConfig
import com.example.gopetalk_bot.data.datasources.remote.dto.*
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Interface para ejecutar código en el hilo principal - permite testing
 */
interface MainThreadExecutor {
    fun post(runnable: Runnable)
}

class AndroidMainThreadExecutor : MainThreadExecutor {
    private val handler = Handler(Looper.getMainLooper())
    override fun post(runnable: Runnable) {
        handler.post(runnable)
    }
}

/**
 * Interface para operaciones con Base64 - permite testing
 */
interface Base64Decoder {
    fun decode(str: String, flags: Int): ByteArray
}

class AndroidBase64Decoder : Base64Decoder {
    override fun decode(str: String, flags: Int): ByteArray {
        return Base64.decode(str, flags)
    }
}

class RemoteDataSource(
    private val mainThreadExecutor: MainThreadExecutor = AndroidMainThreadExecutor(),
    private val base64Decoder: Base64Decoder = AndroidBase64Decoder()
) {
    
    private companion object {
        const val TAG = "RemoteDataSource"
        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 60L
        const val WRITE_TIMEOUT = 60L
        const val HTTP_OK = 200
        const val HTTP_NO_CONTENT = 204
        const val AUDIO_MIME_TYPE = "audio/wav"
        const val TEMP_AUDIO_PREFIX = "received_audio_"
        const val TEMP_POLLED_AUDIO_PREFIX = "polled_audio_"
        const val AUDIO_EXTENSION = ".wav"
        const val HEADER_AUDIO_FROM = "X-Audio-From"
        const val HEADER_CHANNEL = "X-Channel"
        const val UNKNOWN = "unknown"
    }

    private val gson = Gson()
    private val baseUrl = "http://${BuildConfig.BACKEND_HOST}/"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(createLoggingInterceptor())
        .build()
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val apiService: ApiService by lazy { 
        retrofit.create(ApiService::class.java) 
    }

    private fun createLoggingInterceptor() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    interface ApiCallback {
        fun onSuccess(statusCode: Int, body: String, audioFile: File?)
        fun onFailure(e: IOException)
    }
    
    interface AudioDownloadCallback {
        fun onSuccess(audioFile: File)
        fun onFailure(e: IOException)
    }

    interface AuthCallback {
        fun onSuccess(statusCode: Int, message: String, token: String)
        fun onFailure(e: IOException)
    }

    interface AudioPollCallback {
        fun onAudioReceived(audioFile: File, fromUserId: String, channel: String)
        fun onNoAudio()
        fun onFailure(e: IOException)
    }

    fun sendAudioCommand(audioFile: File, authToken: String?, callback: ApiCallback) {
        if (!audioFile.exists()) {
            postFailure(callback, "Audio file does not exist: ${audioFile.path}")
            return
        }

        val multipartBody = createMultipartBody(audioFile) ?: run {
            postFailure(callback, "Failed to read audio file")
            return
        }

        logAudioRequest(audioFile, authToken)
        apiService.sendAudioCommand(authToken, multipartBody).enqueue(createAudioCallback(callback))
    }

    private fun logAudioRequest(audioFile: File, authToken: String?) {
        Log.d(TAG, "Sending audio: ${audioFile.name} (${audioFile.length()} bytes)")
        Log.d(TAG, "Auth-Token: ${authToken?.take(20)}...")
    }

    private fun createAudioCallback(callback: ApiCallback) = object : retrofit2.Callback<ResponseBody> {
        override fun onResponse(call: retrofit2.Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
            if (response.isSuccessful) {
                handleSuccessfulAudioResponse(response, callback)
            } else {
                handleErrorResponse(response, callback)
            }
        }

        override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
            Log.e(TAG, "Retrofit error", t)
            postFailure(callback, "Failed to send audio: ${t.message}", t)
        }
    }

    private fun handleSuccessfulAudioResponse(response: retrofit2.Response<ResponseBody>, callback: ApiCallback) {
        response.body()?.let { responseBody ->
            val bodyString = responseBody.string()
            
            if (isAudioRelayResponse(responseBody.contentType(), bodyString)) {
                handleAudioRelayResponse(bodyString, response.code(), callback)
            } else {
                mainThreadExecutor.post { callback.onSuccess(response.code(), bodyString, null) }
            }
        } ?: mainThreadExecutor.post { callback.onSuccess(response.code(), "", null) }
    }

    private fun isAudioRelayResponse(contentType: MediaType?, body: String): Boolean {
        return contentType?.toString()?.contains("application/json") == true && body.contains("audioBase64")
    }

    private fun handleAudioRelayResponse(bodyString: String, statusCode: Int, callback: ApiCallback) {
        try {
            val audioRelay = gson.fromJson(bodyString, AudioRelayResponse::class.java)
            val audioBytes = base64Decoder.decode(audioRelay.audioBase64, Base64.DEFAULT)
            val tempFile = File.createTempFile(TEMP_AUDIO_PREFIX, AUDIO_EXTENSION)
            tempFile.writeBytes(audioBytes)
            
            mainThreadExecutor.post { callback.onSuccess(statusCode, bodyString, tempFile) }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding audio relay response", e)
            postFailure(callback, "Error decoding audio: ${e.message}", e)
        }
    }

    private fun createMultipartBody(audioFile: File): MultipartBody.Part? = try {
        val requestBody = audioFile.asRequestBody(AUDIO_MIME_TYPE.toMediaTypeOrNull())
        MultipartBody.Part.createFormData("file", audioFile.name, requestBody)
    } catch (e: Exception) {
        Log.e(TAG, "Error creating multipart body", e)
        null
    }

    private fun postFailure(callback: ApiCallback, message: String, cause: Throwable? = null) {
        mainThreadExecutor.post { callback.onFailure(IOException(message, cause)) }
    }

    private fun handleErrorResponse(response: retrofit2.Response<ResponseBody>, callback: ApiCallback) {
        val errorBody = response.errorBody()?.string() ?: UNKNOWN
        Log.e(TAG, "Backend error ${response.code()}: $errorBody")
        postFailure(callback, "Backend error ${response.code()}: $errorBody")
    }
    
    fun downloadAudioFile(fileUrl: String, outputFile: File, callback: AudioDownloadCallback) {
        val fullUrl = buildFullUrl(fileUrl)
        Log.d(TAG, "Downloading audio from: $fullUrl")
        
        apiService.downloadAudioFile(fullUrl).enqueue(createDownloadCallback(outputFile, callback))
    }

    private fun buildFullUrl(fileUrl: String): String {
        return if (fileUrl.startsWith("http")) fileUrl else "$baseUrl$fileUrl"
    }

    private fun createDownloadCallback(outputFile: File, callback: AudioDownloadCallback) = 
        object : retrofit2.Callback<ResponseBody> {
            override fun onResponse(call: retrofit2.Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { saveAudioFile(it, outputFile, callback) }
                        ?: postDownloadFailure(callback, "Empty response body")
                } else {
                    val errorBody = response.errorBody()?.string() ?: UNKNOWN
                    Log.e(TAG, "Download error ${response.code()}: $errorBody")
                    postDownloadFailure(callback, "Download error ${response.code()}: $errorBody")
                }
            }
            
            override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Download failed", t)
                postDownloadFailure(callback, "Failed to download audio: ${t.message}", t)
            }
        }

    private fun saveAudioFile(body: ResponseBody, outputFile: File, callback: AudioDownloadCallback) {
        try {
            body.byteStream().use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            mainThreadExecutor.post { callback.onSuccess(outputFile) }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving audio file", e)
            postDownloadFailure(callback, "Error saving audio file: ${e.message}", e)
        }
    }

    private fun postDownloadFailure(callback: AudioDownloadCallback, message: String, cause: Throwable? = null) {
        mainThreadExecutor.post { callback.onFailure(IOException(message, cause)) }
    }

    fun sendAuthentication(nombre: String, pin: Int, callback: AuthCallback) {
        val authRequest = AuthenticationRequest(nombre, pin)
        Log.d(TAG, "Authenticating: $nombre")

        apiService.authenticate(authRequest).enqueue(createAuthCallback(callback))
    }

    private fun createAuthCallback(callback: AuthCallback) = object : retrofit2.Callback<ResponseBody> {
        override fun onResponse(call: retrofit2.Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
            if (response.isSuccessful) {
                handleAuthSuccess(response, callback)
            } else {
                handleAuthError(response, callback)
            }
        }

        override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
            Log.e(TAG, "Authentication request failed", t)
            postAuthFailure(callback, "Failed to authenticate: ${t.message}", null, t)
        }
    }

    private fun handleAuthSuccess(response: retrofit2.Response<ResponseBody>, callback: AuthCallback) {
        val bodyString = response.body()?.string() ?: ""
        try {
            val authResponse = gson.fromJson(bodyString, AuthenticationResponse::class.java)
            mainThreadExecutor.post {
                callback.onSuccess(response.code(), authResponse.message, authResponse.token)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing authentication response", e)
            postAuthFailure(callback, "Error parsing response: ${e.message}", null, e)
        }
    }

    private fun handleAuthError(response: retrofit2.Response<ResponseBody>, callback: AuthCallback) {
        val errorBody = response.errorBody()?.string() ?: UNKNOWN
        val statusCode = response.code()
        Log.e(TAG, "Authentication error $statusCode: $errorBody")
        postAuthFailure(callback, "Authentication error $statusCode: $errorBody", statusCode)
    }

    private fun postAuthFailure(callback: AuthCallback, message: String, statusCode: Int? = null, cause: Throwable? = null) {
        val exception = if (statusCode != null) {
            AuthenticationException(message, statusCode, cause)
        } else {
            IOException(message, cause)
        }
        mainThreadExecutor.post { callback.onFailure(exception) }
    }
    
    class AuthenticationException(message: String, val statusCode: Int, cause: Throwable? = null) : IOException(message, cause)

    fun pollAudio(authToken: String?, callback: AudioPollCallback) {
        apiService.pollAudio(authToken).enqueue(createPollCallback(callback))
    }

    private fun createPollCallback(callback: AudioPollCallback) = object : retrofit2.Callback<ResponseBody> {
        override fun onResponse(call: retrofit2.Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
            when (response.code()) {
                HTTP_OK -> handlePolledAudio(response, callback)
                HTTP_NO_CONTENT -> mainThreadExecutor.post { callback.onNoAudio() }
                else -> handlePollError(response, callback)
            }
        }

        override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
            Log.e(TAG, "Polling failed", t)
            postPollFailure(callback, "Failed to poll audio: ${t.message}", t)
        }
    }

    private fun handlePolledAudio(response: retrofit2.Response<ResponseBody>, callback: AudioPollCallback) {
        response.body()?.let { body ->
            try {
                val audioBytes = body.bytes()
                val tempFile = File.createTempFile(TEMP_POLLED_AUDIO_PREFIX, AUDIO_EXTENSION)
                tempFile.writeBytes(audioBytes)

                val fromUserId = response.headers()[HEADER_AUDIO_FROM] ?: UNKNOWN
                val channel = response.headers()[HEADER_CHANNEL] ?: UNKNOWN

                mainThreadExecutor.post { callback.onAudioReceived(tempFile, fromUserId, channel) }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing polled audio", e)
                postPollFailure(callback, "Error processing audio: ${e.message}", e)
            }
        } ?: postPollFailure(callback, "Empty response body")
    }

    private fun handlePollError(response: retrofit2.Response<ResponseBody>, callback: AudioPollCallback) {
        val errorBody = response.errorBody()?.string() ?: UNKNOWN
        Log.e(TAG, "Polling error ${response.code()}: $errorBody")
        postPollFailure(callback, "Polling error ${response.code()}: $errorBody")
    }

    private fun postPollFailure(callback: AudioPollCallback, message: String, cause: Throwable? = null) {
        mainThreadExecutor.post { callback.onFailure(IOException(message, cause)) }
    }
}
