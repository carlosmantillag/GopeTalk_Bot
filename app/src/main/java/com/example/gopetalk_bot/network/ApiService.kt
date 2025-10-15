package com.example.gopetalk_bot.network

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import kotlin.random.Random

class ApiService {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())

    private val sendCommandUrl = "YOUR_COMMAND_ENDPOINT_URL"
    private val channelListUrl = "YOUR_CHANNELS_ENDPOINT_URL"
    private val userListUrl = "YOUR_USERS_ENDPOINT_URL"
    private val connectToChannelUrl = "YOUR_CONNECT_ENDPOINT_URL"

    interface ApiCallback {
        fun onSuccess(response: BackendResponse)
        fun onFailure(e: IOException)
    }

    fun sendAudioCommand(audioFile: File, callback: ApiCallback) {
        // Simulate a network delay
        handler.postDelayed({
            // Simulate different responses based on some logic
            val simulatedResponse = when (Random.nextInt(3)) {
                0 -> BackendResponse(
                    text = "la lista de canales es: canal 1, canal 2",
                    action = "list_channels",
                    channels = listOf("canal 1", "canal 2")
                )
                1 -> BackendResponse(
                    text = "la lista de usuarios es: usuario 1, usuario 2",
                    action = "list_users",
                    users = listOf("usuario 1", "usuario 2")
                )
                else -> BackendResponse(
                    text = "Conectando al canal general",
                    action = "connect_to_channel"
                )
            }
            callback.onSuccess(simulatedResponse)
        }, 1000) // 1-second delay

        /* Original network code - commented out for simulation
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
                        if (body == null) {
                            callback.onFailure(IOException("Empty response body."))
                            return
                        }
                        try {
                            val backendResponse = gson.fromJson(body, BackendResponse::class.java)
                            callback.onSuccess(backendResponse)
                        } catch (e: JsonSyntaxException) {
                            callback.onFailure(IOException("Failed to parse JSON response.", e))
                        }
                    }
                }
            })
        } catch (e: Exception) {
            callback.onFailure(IOException("Failed to create audio command request.", e))
        }
        */
    }

    // TODO: Update other API methods to use a similar pattern with specific data classes
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
