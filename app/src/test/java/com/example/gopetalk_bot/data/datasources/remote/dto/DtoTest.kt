package com.example.gopetalk_bot.data.datasources.remote.dto

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test

class DtoTest {

    private val gson = Gson()

    // ==================== AuthenticationRequest Tests ====================

    @Test
    fun `AuthenticationRequest should be created with correct values`() {
        val request = AuthenticationRequest("Carlos", 1234)
        
        assertThat(request.nombre).isEqualTo("Carlos")
        assertThat(request.pin).isEqualTo(1234)
    }

    @Test
    fun `AuthenticationRequest should serialize to JSON correctly`() {
        val request = AuthenticationRequest("Carlos", 1234)
        val json = gson.toJson(request)
        
        assertThat(json).contains("\"nombre\":\"Carlos\"")
        assertThat(json).contains("\"pin\":1234")
    }

    @Test
    fun `AuthenticationRequest should deserialize from JSON correctly`() {
        val json = """{"nombre":"Carlos","pin":1234}"""
        val request = gson.fromJson(json, AuthenticationRequest::class.java)
        
        assertThat(request.nombre).isEqualTo("Carlos")
        assertThat(request.pin).isEqualTo(1234)
    }

    @Test
    fun `AuthenticationRequest should support equality`() {
        val request1 = AuthenticationRequest("Carlos", 1234)
        val request2 = AuthenticationRequest("Carlos", 1234)
        val request3 = AuthenticationRequest("Juan", 5678)
        
        assertThat(request1).isEqualTo(request2)
        assertThat(request1).isNotEqualTo(request3)
    }

    @Test
    fun `AuthenticationRequest should support copy`() {
        val request1 = AuthenticationRequest("Carlos", 1234)
        val request2 = request1.copy(pin = 5678)
        
        assertThat(request2.nombre).isEqualTo("Carlos")
        assertThat(request2.pin).isEqualTo(5678)
    }

    @Test
    fun `AuthenticationRequest should handle different PIN lengths`() {
        val request1 = AuthenticationRequest("User", 1)
        val request2 = AuthenticationRequest("User", 9999)
        val request3 = AuthenticationRequest("User", 123456)
        
        assertThat(request1.pin).isEqualTo(1)
        assertThat(request2.pin).isEqualTo(9999)
        assertThat(request3.pin).isEqualTo(123456)
    }

    // ==================== AuthenticationResponse Tests ====================

    @Test
    fun `AuthenticationResponse should be created with correct values`() {
        val response = AuthenticationResponse("Welcome", "token-123")
        
        assertThat(response.message).isEqualTo("Welcome")
        assertThat(response.token).isEqualTo("token-123")
    }

    @Test
    fun `AuthenticationResponse should serialize to JSON correctly`() {
        val response = AuthenticationResponse("Welcome", "token-123")
        val json = gson.toJson(response)
        
        assertThat(json).contains("\"message\":\"Welcome\"")
        assertThat(json).contains("\"token\":\"token-123\"")
    }

    @Test
    fun `AuthenticationResponse should deserialize from JSON correctly`() {
        val json = """{"message":"Welcome","token":"token-123"}"""
        val response = gson.fromJson(json, AuthenticationResponse::class.java)
        
        assertThat(response.message).isEqualTo("Welcome")
        assertThat(response.token).isEqualTo("token-123")
    }

    @Test
    fun `AuthenticationResponse should support equality`() {
        val response1 = AuthenticationResponse("Welcome", "token-123")
        val response2 = AuthenticationResponse("Welcome", "token-123")
        val response3 = AuthenticationResponse("Error", "token-456")
        
        assertThat(response1).isEqualTo(response2)
        assertThat(response1).isNotEqualTo(response3)
    }

    @Test
    fun `AuthenticationResponse should support copy`() {
        val response1 = AuthenticationResponse("Welcome", "token-123")
        val response2 = response1.copy(message = "Success")
        
        assertThat(response2.message).isEqualTo("Success")
        assertThat(response2.token).isEqualTo("token-123")
    }

    // ==================== BackendResponse Tests ====================

    @Test
    fun `BackendResponse should be created with default values`() {
        val response = BackendResponse()
        
        assertThat(response.status).isEmpty()
        assertThat(response.intent).isEmpty()
        assertThat(response.message).isEmpty()
        assertThat(response.data).isNull()
        assertThat(response.text).isEmpty()
        assertThat(response.action).isEmpty()
        assertThat(response.channels).isEmpty()
        assertThat(response.users).isEmpty()
        assertThat(response.audioFile).isNull()
        assertThat(response.isAudioResponse).isFalse()
        assertThat(response.channel).isNull()
    }

    @Test
    fun `BackendResponse should be created with custom values`() {
        val response = BackendResponse(
            status = "ok",
            intent = "request_channel_connect",
            message = "Conectado al canal 1",
            data = BackendResponseData(channel = "canal-1", channelLabel = "1"),
            text = "Hello",
            action = "message",
            channels = listOf("general", "tech"),
            users = listOf("user1", "user2"),
            audioFile = "audio.wav",
            isAudioResponse = true,
            channel = "general"
        )
        
        assertThat(response.status).isEqualTo("ok")
        assertThat(response.intent).isEqualTo("request_channel_connect")
        assertThat(response.message).isEqualTo("Conectado al canal 1")
        assertThat(response.data?.channel).isEqualTo("canal-1")
        assertThat(response.data?.channelLabel).isEqualTo("1")
        assertThat(response.text).isEqualTo("Hello")
        assertThat(response.action).isEqualTo("message")
        assertThat(response.channels).containsExactly("general", "tech")
        assertThat(response.users).containsExactly("user1", "user2")
        assertThat(response.audioFile).isEqualTo("audio.wav")
        assertThat(response.isAudioResponse).isTrue()
        assertThat(response.channel).isEqualTo("general")
    }

    @Test
    fun `BackendResponse should deserialize from JSON correctly`() {
        val json = """
            {
                "status": "ok",
                "intent": "request_channel_connect",
                "message": "Conectado al canal 1",
                "data": {"channel":"canal-1","channel_label":"1"},
                "text": "Hello World",
                "action": "list_channels",
                "channels": ["general", "tech", "music"],
                "users": ["alice", "bob"],
                "audio_file": "response.wav",
                "is_audio_response": true,
                "channel": "general"
            }
        """.trimIndent()
        
        val response = gson.fromJson(json, BackendResponse::class.java)
        
        assertThat(response.status).isEqualTo("ok")
        assertThat(response.intent).isEqualTo("request_channel_connect")
        assertThat(response.message).isEqualTo("Conectado al canal 1")
        assertThat(response.data?.channel).isEqualTo("canal-1")
        assertThat(response.data?.channelLabel).isEqualTo("1")
        assertThat(response.text).isEqualTo("Hello World")
        assertThat(response.action).isEqualTo("list_channels")
        assertThat(response.channels).containsExactly("general", "tech", "music")
        assertThat(response.users).containsExactly("alice", "bob")
        assertThat(response.audioFile).isEqualTo("response.wav")
        assertThat(response.isAudioResponse).isTrue()
        assertThat(response.channel).isEqualTo("general")
    }

    @Test
    fun `BackendResponse should handle partial JSON`() {
        val json = """{"message": "Hola", "data":{"channel":"general"}}"""
        val response = gson.fromJson(json, BackendResponse::class.java)
        
        assertThat(response.message).isEqualTo("Hola")
        assertThat(response.data?.channel).isEqualTo("general")
        assertThat(response.action).isEmpty()
        assertThat(response.text).isEmpty()
    }

    @Test
    fun `BackendResponse should serialize to JSON correctly`() {
        val response = BackendResponse(
            status = "ok",
            intent = "notify",
            message = "Todo listo",
            data = BackendResponseData(channel = "general", channelLabel = "1")
        )
        val json = gson.toJson(response)
        
        assertThat(json).contains("\"status\":\"ok\"")
        assertThat(json).contains("\"intent\":\"notify\"")
        assertThat(json).contains("\"message\":\"Todo listo\"")
        assertThat(json).contains("\"data\":{\"channel\":\"general\",\"channel_label\":\"1\"}")
    }

    @Test
    fun `BackendResponse should support equality`() {
        val response1 = BackendResponse(message = "Hello", intent = "message")
        val response2 = BackendResponse(message = "Hello", intent = "message")
        val response3 = BackendResponse(message = "Goodbye", intent = "logout")
        
        assertThat(response1).isEqualTo(response2)
        assertThat(response1).isNotEqualTo(response3)
    }

    @Test
    fun `BackendResponse should support copy`() {
        val response1 = BackendResponse(message = "Hello", intent = "message")
        val response2 = response1.copy(message = "Goodbye")
        
        assertThat(response2.message).isEqualTo("Goodbye")
        assertThat(response2.intent).isEqualTo("message")
    }

    @Test
    fun `BackendResponse should handle list_channels action`() {
        val response = BackendResponse(
            action = "list_channels",
            channels = listOf("general", "tech", "music", "random")
        )
        
        assertThat(response.action).isEqualTo("list_channels")
        assertThat(response.channels).hasSize(4)
        assertThat(response.channels).contains("general")
    }

    @Test
    fun `BackendResponse should handle list_users action`() {
        val response = BackendResponse(
            action = "list_users",
            users = listOf("alice", "bob", "charlie")
        )
        
        assertThat(response.action).isEqualTo("list_users")
        assertThat(response.users).hasSize(3)
        assertThat(response.users).contains("alice")
    }

    @Test
    fun `BackendResponse should handle logout action`() {
        val response = BackendResponse(
            action = "logout",
            text = "Logged out successfully"
        )
        
        assertThat(response.action).isEqualTo("logout")
        assertThat(response.text).isEqualTo("Logged out successfully")
    }

    // ==================== AudioRelayResponse Tests ====================

    @Test
    fun `AudioRelayResponse should be created with correct values`() {
        val response = AudioRelayResponse(
            status = "success",
            channel = "general",
            recipients = listOf(1, 2, 3),
            audioBase64 = "SGVsbG8gV29ybGQ=",
            duration = 5.5,
            sampleRate = 16000,
            format = "wav"
        )
        
        assertThat(response.status).isEqualTo("success")
        assertThat(response.channel).isEqualTo("general")
        assertThat(response.recipients).containsExactly(1, 2, 3)
        assertThat(response.audioBase64).isEqualTo("SGVsbG8gV29ybGQ=")
        assertThat(response.duration).isEqualTo(5.5)
        assertThat(response.sampleRate).isEqualTo(16000)
        assertThat(response.format).isEqualTo("wav")
    }

    @Test
    fun `AudioRelayResponse should serialize to JSON correctly`() {
        val response = AudioRelayResponse(
            status = "success",
            channel = "general",
            recipients = listOf(1, 2),
            audioBase64 = "base64data",
            duration = 3.0,
            sampleRate = 16000,
            format = "wav"
        )
        val json = gson.toJson(response)
        
        assertThat(json).contains("\"status\":\"success\"")
        assertThat(json).contains("\"channel\":\"general\"")
        assertThat(json).contains("\"recipients\":[1,2]")
        assertThat(json).contains("\"audioBase64\":\"base64data\"")
    }

    @Test
    fun `AudioRelayResponse should deserialize from JSON correctly`() {
        val json = """
            {
                "status": "success",
                "channel": "tech",
                "recipients": [10, 20, 30],
                "audioBase64": "YXVkaW9kYXRh",
                "duration": 4.2,
                "sampleRate": 44100,
                "format": "mp3"
            }
        """.trimIndent()
        
        val response = gson.fromJson(json, AudioRelayResponse::class.java)
        
        assertThat(response.status).isEqualTo("success")
        assertThat(response.channel).isEqualTo("tech")
        assertThat(response.recipients).containsExactly(10, 20, 30)
        assertThat(response.audioBase64).isEqualTo("YXVkaW9kYXRh")
        assertThat(response.duration).isEqualTo(4.2)
        assertThat(response.sampleRate).isEqualTo(44100)
        assertThat(response.format).isEqualTo("mp3")
    }

    @Test
    fun `AudioRelayResponse should support equality`() {
        val response1 = AudioRelayResponse(
            "success", "general", listOf(1), "data", 1.0, 16000, "wav"
        )
        val response2 = AudioRelayResponse(
            "success", "general", listOf(1), "data", 1.0, 16000, "wav"
        )
        val response3 = AudioRelayResponse(
            "error", "tech", listOf(2), "other", 2.0, 44100, "mp3"
        )
        
        assertThat(response1).isEqualTo(response2)
        assertThat(response1).isNotEqualTo(response3)
    }

    @Test
    fun `AudioRelayResponse should support copy`() {
        val response1 = AudioRelayResponse(
            "success", "general", listOf(1), "data", 1.0, 16000, "wav"
        )
        val response2 = response1.copy(channel = "tech")
        
        assertThat(response2.channel).isEqualTo("tech")
        assertThat(response2.status).isEqualTo("success")
    }

    @Test
    fun `AudioRelayResponse should handle empty recipients list`() {
        val response = AudioRelayResponse(
            "success", "general", emptyList(), "data", 1.0, 16000, "wav"
        )
        
        assertThat(response.recipients).isEmpty()
    }

    @Test
    fun `AudioRelayResponse should handle multiple recipients`() {
        val recipients = (1..100).toList()
        val response = AudioRelayResponse(
            "success", "general", recipients, "data", 1.0, 16000, "wav"
        )
        
        assertThat(response.recipients).hasSize(100)
        assertThat(response.recipients).contains(50)
    }

    @Test
    fun `AudioRelayResponse should handle different sample rates`() {
        val response1 = AudioRelayResponse(
            "success", "general", listOf(1), "data", 1.0, 8000, "wav"
        )
        val response2 = AudioRelayResponse(
            "success", "general", listOf(1), "data", 1.0, 16000, "wav"
        )
        val response3 = AudioRelayResponse(
            "success", "general", listOf(1), "data", 1.0, 44100, "wav"
        )
        
        assertThat(response1.sampleRate).isEqualTo(8000)
        assertThat(response2.sampleRate).isEqualTo(16000)
        assertThat(response3.sampleRate).isEqualTo(44100)
    }

    @Test
    fun `AudioRelayResponse should handle different audio formats`() {
        val formats = listOf("wav", "mp3", "ogg", "flac", "aac")
        
        formats.forEach { format ->
            val response = AudioRelayResponse(
                "success", "general", listOf(1), "data", 1.0, 16000, format
            )
            assertThat(response.format).isEqualTo(format)
        }
    }

    // ==================== Integration Tests ====================

    @Test
    fun `All DTOs should work together in authentication flow`() {
        // Create request
        val request = AuthenticationRequest("Carlos", 1234)
        val requestJson = gson.toJson(request)
        
        // Simulate server response
        val responseJson = """{"message":"Welcome Carlos","token":"abc123"}"""
        val response = gson.fromJson(responseJson, AuthenticationResponse::class.java)
        
        assertThat(request.nombre).isEqualTo("Carlos")
        assertThat(response.message).contains("Carlos")
        assertThat(response.token).isNotEmpty()
    }

    @Test
    fun `BackendResponse should handle complex nested data`() {
        val json = """
            {
                "text": "Available channels",
                "action": "list_channels",
                "channels": ["general", "tech", "music", "random", "gaming"],
                "users": ["alice", "bob", "charlie", "david"],
                "channel": "general",
                "is_audio_response": false
            }
        """.trimIndent()
        
        val response = gson.fromJson(json, BackendResponse::class.java)
        
        assertThat(response.channels).hasSize(5)
        assertThat(response.users).hasSize(4)
        assertThat(response.isAudioResponse).isFalse()
    }
}
