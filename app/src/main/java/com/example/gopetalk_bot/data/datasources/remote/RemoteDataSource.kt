package com.example.gopetalk_bot.data.datasources.remote

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException

/**
 * Remote data source for API calls
 */
class RemoteDataSource(
    private val backendHost: String = "159.223.150.185",
    private val backendPort: Int = 8086
) {
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private val baseUrl = "http://$backendHost:$backendPort/"
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val apiService: ApiService by lazy { 
        retrofit.create(ApiService::class.java) 
    }

    interface ApiCallback {
        fun onSuccess(statusCode: Int)
        fun onFailure(e: IOException)
    }

    fun sendAudioCommand(audioFile: File, userId: String, callback: ApiCallback) {
        if (!audioFile.exists()) {
            handler.post { 
                callback.onFailure(IOException("Audio file does not exist: ${audioFile.path}")) 
            }
            return
        }

        val requestBody = createAudioRequestBody(audioFile) ?: run {
            handler.post { callback.onFailure(IOException("Failed to read audio file")) }
            return
        }

        Log.d(TAG, "Sending audio to backend via Retrofit: $baseUrl (${audioFile.length()} bytes)")

        apiService.sendAudioCommand(userId, requestBody).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    handler.post { callback.onSuccess(response.code()) }
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

    private fun createAudioRequestBody(audioFile: File): RequestBody? = try {
        audioFile.asRequestBody("audio/wav".toMediaType())
    } catch (e: Exception) {
        Log.e(TAG, "Error creating request body", e)
        null
    }

    companion object {
        private const val TAG = "RemoteDataSource"
    }
}
