package com.example.gopetalk_bot.data.datasources.remote

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit API service interface
 */
interface ApiService {
    @POST("audio/ingest")
    fun sendAudioCommand(
        @Header("X-User-ID") userId: String,
        @Body audioFile: RequestBody
    ): Call<ResponseBody>
}
