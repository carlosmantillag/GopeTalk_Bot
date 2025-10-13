package com.example.gopetalk_bot.network

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException

class ApiService {

    private val client = OkHttpClient()

    // TODO: Replace with your actual endpoint URLs
    private val sendCommandUrl = "YOUR_COMMAND_ENDPOINT_URL"
    private val channelListUrl = "YOUR_CHANNELS_ENDPOINT_URL"
    private val userListUrl = "YOUR_USERS_ENDPOINT_URL"
    private val connectToChannelUrl = "YOUR_CONNECT_ENDPOINT_URL"

    interface ApiCallback {
        fun onSuccess(response: String)
        fun onFailure(e: IOException)
    }

    fun sendAudioCommand(audioFile: File, callback: ApiCallback) {
        if (sendCommandUrl.startsWith("YOUR_")) {
            callback.onFailure(IOException("Command endpoint URL is not set."))
            return
        }

        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("audio", audioFile.name, audioFile.asRequestBody("audio/wav".toMediaTypeOrNull()))
                .build()

            val request = Request.Builder()
                .url(sendCommandUrl)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    callback.onFailure(e)
                }

                override fun onResponse(call: okhttp3.Call, response: Response) {
                    response.use { res ->
                        if (!res.isSuccessful) {
                            callback.onFailure(IOException("Unexpected code $res"))
                            return
                        }
                        val body = res.body?.string()
                        if (body != null) {
                            callback.onSuccess(body)
                        } else {
                            callback.onFailure(IOException("Empty response body."))
                        }
                    }
                }
            })
        } catch (e: Exception) {
            callback.onFailure(IOException("Failed to create audio command request.", e))
        }
    }

    fun getChannelList(callback: ApiCallback) {
        // TODO: Implement channel list fetching logic
        callback.onFailure(IOException("getChannelList not implemented."))
    }

    fun getUserList(channelId: String, callback: ApiCallback) {
        // TODO: Implement user list fetching logic
        callback.onFailure(IOException("getUserList not implemented."))
    }

    fun connectToChannel(channelId: String, callback: ApiCallback) {
        // TODO: Implement connect to channel logic
        callback.onFailure(IOException("connectToChannel not implemented."))
    }
}
