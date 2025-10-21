package com.example.gopetalk_bot.data.datasources.remote

import com.example.gopetalk_bot.BuildConfig
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.example.gopetalk_bot.data.datasources.remote.dto.AudioRelayResponse
import com.example.gopetalk_bot.data.datasources.remote.dto.AuthenticationResponse
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException

class RemoteDataSource {
    private val handler = Handler(Looper.getMainLooper())
    private val gson = Gson()
    private val baseUrl = "http://${BuildConfig.BACKEND_HOST}/"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
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
            handler.post { 
                callback.onFailure(IOException("Audio file does not exist: ${audioFile.path}")) 
            }
            return
        }

        val multipartBody = createMultipartBody(audioFile) ?: run {
            handler.post { callback.onFailure(IOException("Failed to read audio file")) }
            return
        }

        Log.d(TAG, "Sending audio to backend via Retrofit")
        Log.d(TAG, "URL: ${baseUrl}audio/ingest")
        Log.d(TAG, "Auth-Token: ${authToken?.take(20)}...")
        Log.d(TAG, "File: ${audioFile.name} (${audioFile.length()} bytes)")

        apiService.sendAudioCommand( authToken, multipartBody).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.d(TAG, "Response received: ${response.code()}")
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val contentType = responseBody.contentType()
                        Log.d(TAG, "Content-Type: $contentType")
                        val bodyString = responseBody.string()
                        
                        // Check if response is JSON with audioBase64
                        if (contentType?.toString()?.contains("application/json") == true && bodyString.contains("audioBase64")) {
                            try {
                                val audioRelay = gson.fromJson(bodyString, AudioRelayResponse::class.java)
                                Log.d(TAG, "Audio relay response: channel=${audioRelay.channel}, recipients=${audioRelay.recipients.size}")
                                
                                // Decode base64 to WAV file
                                val audioBytes = Base64.decode(audioRelay.audioBase64, Base64.DEFAULT)
                                val tempFile = File.createTempFile("received_audio_", ".wav")
                                tempFile.writeBytes(audioBytes)
                                
                                Log.d(TAG, "Audio file decoded and saved: ${tempFile.path}")
                                handler.post { callback.onSuccess(response.code(), bodyString, tempFile) }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error decoding audio relay response", e)
                                handler.post { callback.onFailure(IOException("Error decoding audio: ${e.message}", e)) }
                            }
                        } else {
                            // Text response (commands, etc.)
                            Log.d(TAG, "Text response: $bodyString")
                            handler.post { callback.onSuccess(response.code(), bodyString, null) }
                        }
                    } ?: run {
                        handler.post { callback.onSuccess(response.code(), "", null) }
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Backend error ${response.code()}: $errorBody")
                    handler.post { 
                        callback.onFailure(IOException("Backend error ${response.code()}: $errorBody")) 
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Retrofit error", t)
                handler.post { 
                    callback.onFailure(IOException("Failed to send audio: ${t.message}", t)) 
                }
            }
        })
    }

    private fun createMultipartBody(audioFile: File): MultipartBody.Part? = try {
        val requestBody = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
        MultipartBody.Part.createFormData("file", audioFile.name, requestBody)
    } catch (e: Exception) {
        Log.e(TAG, "Error creating multipart body", e)
        null
    }
    
    fun downloadAudioFile(fileUrl: String, outputFile: File, callback: AudioDownloadCallback) {
        Log.d(TAG, "Downloading audio file from: $fileUrl")
        
        val fullUrl = if (fileUrl.startsWith("http")) {
            fileUrl
        } else {
            "$baseUrl$fileUrl"
        }
        
        apiService.downloadAudioFile(fullUrl).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        try {
                            val inputStream = body.byteStream()
                            val outputStream = outputFile.outputStream()
                            
                            inputStream.use { input ->
                                outputStream.use { output ->
                                    input.copyTo(output)
                                }
                            }
                            
                            Log.d(TAG, "Audio file downloaded successfully: ${outputFile.path}")
                            handler.post { callback.onSuccess(outputFile) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving audio file", e)
                            handler.post { callback.onFailure(IOException("Error saving audio file: ${e.message}", e)) }
                        }
                    } ?: run {
                        handler.post { callback.onFailure(IOException("Empty response body")) }
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Download error ${response.code()}: $errorBody")
                    handler.post { callback.onFailure(IOException("Download error ${response.code()}: $errorBody")) }
                }
            }
            
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Download failed", t)
                handler.post { callback.onFailure(IOException("Failed to download audio: ${t.message}", t)) }
            }
        })
    }

    fun sendAuthentication(nombre: String, pin: Int, callback: AuthCallback) {
        val authRequest = com.example.gopetalk_bot.data.datasources.remote.dto.AuthenticationRequest(nombre, pin)

        Log.d(TAG, "Sending authentication to backend")
        Log.d(TAG, "URL: ${baseUrl}auth")
        Log.d(TAG, "Request JSON: ${gson.toJson(authRequest)}")
        Log.d(TAG, "Nombre: $nombre, PIN: $pin")

        apiService.authenticate(authRequest).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.d(TAG, "Authentication response received: ${response.code()}")
                if (response.isSuccessful) {
                    val bodyString = response.body()?.string() ?: ""
                    Log.d(TAG, "Authentication successful: $bodyString")

                    try {
                        val authResponse = gson.fromJson(bodyString, AuthenticationResponse::class.java)
                        Log.d(TAG, "Parsed auth response - Message: ${authResponse.message}, Token: ${authResponse.token}")
                        handler.post {
                            callback.onSuccess(response.code(), authResponse.message, authResponse.token)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing authentication response", e)
                        handler.post {
                            callback.onFailure(IOException("Error parsing response: ${e.message}", e))
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Authentication error ${response.code()}: $errorBody")
                    handler.post {
                        callback.onFailure(IOException("Authentication error ${response.code()}: $errorBody"))
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Authentication request failed", t)
                handler.post {
                    callback.onFailure(IOException("Failed to authenticate: ${t.message}", t))
                }
            }
        })
    }

    fun pollAudio(authToken: String?, callback: AudioPollCallback) {
        Log.d(TAG, "Polling audio with authToken: ${authToken?.take(20)}...")

        apiService.pollAudio(authToken).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                when (response.code()) {
                    200 -> {
                        // Audio received
                        response.body()?.let { body ->
                            try {
                                val audioBytes = body.bytes()
                                val tempFile = File.createTempFile("polled_audio_", ".wav")
                                tempFile.writeBytes(audioBytes)

                                val fromUserId = response.headers()["X-Audio-From"] ?: "unknown"
                                val channel = response.headers()["X-Channel"] ?: "unknown"

                                Log.d(TAG, "Audio received via polling from user $fromUserId in channel $channel (${audioBytes.size} bytes)")
                                handler.post { callback.onAudioReceived(tempFile, fromUserId, channel) }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing polled audio", e)
                                handler.post { callback.onFailure(IOException("Error processing audio: ${e.message}", e)) }
                            }
                        } ?: run {
                            handler.post { callback.onFailure(IOException("Empty response body")) }
                        }
                    }
                    204 -> {
                        // No audio pending
                        Log.d(TAG, "No audio pending")
                        handler.post { callback.onNoAudio() }
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e(TAG, "Polling error ${response.code()}: $errorBody")
                        handler.post { callback.onFailure(IOException("Polling error ${response.code()}: $errorBody")) }
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Polling failed", t)
                handler.post { callback.onFailure(IOException("Failed to poll audio: ${t.message}", t)) }
            }
        })
    }

    companion object {
        private const val TAG = "RemoteDataSource"
    }
}
