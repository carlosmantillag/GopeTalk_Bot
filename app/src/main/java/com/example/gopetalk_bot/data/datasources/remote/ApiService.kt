package com.example.gopetalk_bot.data.datasources.remote

import com.example.gopetalk_bot.data.datasources.remote.dto.AuthenticationRequest
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("audio/ingest")
    fun sendAudioCommand(
        @Header("X-User-ID") userId: String,
        @Header("X-Auth-Token") authToken: String?,
        @Part file: MultipartBody.Part
    ): Call<okhttp3.ResponseBody>
    
    @retrofit2.http.GET
    fun downloadAudioFile(
        @retrofit2.http.Url fileUrl: String
    ): Call<okhttp3.ResponseBody>
    
    @POST("auth")
    fun authenticate(
        @Body authRequest: AuthenticationRequest
    ): Call<okhttp3.ResponseBody>
}
