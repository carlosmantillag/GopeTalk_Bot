package com.example.gopetalk_bot.data.datasources.remote

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.io.IOException

class RemoteDataSource(
    private val backendHost: String = "159.223.150.185",
) {
    private val handler = Handler(Looper.getMainLooper())
    private val baseUrl = "http://$backendHost/"
    
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
        fun onSuccess(statusCode: Int, body: String)
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

        apiService.sendAudioCommand(userId, multipartBody).enqueue(object : Callback<String> {
            override fun onResponse(call: Call<String>, response: Response<String>) {
                Log.d(TAG, "Response received: ${response.code()}")
                if (response.isSuccessful) {
                    val body = response.body() ?: ""
                    Log.d(TAG, "Success body: $body")
                    handler.post { callback.onSuccess(response.code(), body) }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Backend error ${response.code()}: $errorBody")
                    handler.post { 
                        callback.onFailure(IOException("Backend error ${response.code()}: $errorBody")) 
                    }
                }
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
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

    companion object {
        private const val TAG = "RemoteDataSource"
    }
}
