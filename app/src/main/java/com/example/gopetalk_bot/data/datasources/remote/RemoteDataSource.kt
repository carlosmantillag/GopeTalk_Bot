package com.example.gopetalk_bot.data.datasources.remote

import com.example.gopetalk_bot.BuildConfig
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.example.gopetalk_bot.data.datasources.remote.dto.AudioRelayResponse
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

    fun sendAudioCommand(audioFile: File, userId: String, callback: ApiCallback) {
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
        Log.d(TAG, "User-ID: $userId")
        Log.d(TAG, "File: ${audioFile.name} (${audioFile.length()} bytes)")

        apiService.sendAudioCommand(userId, multipartBody).enqueue(object : Callback<ResponseBody> {
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

    companion object {
        private const val TAG = "RemoteDataSource"
    }
}
