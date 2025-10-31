package com.example.gopetalk_bot.data.repositories

import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.data.datasources.remote.RemoteDataSource
import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.entities.AudioFormat
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException


class ApiRepositoryImplEdgeCasesTest {

    private lateinit var remoteDataSource: RemoteDataSource
    private lateinit var userPreferences: UserPreferences
    private lateinit var repository: ApiRepositoryImpl

    @Before
    fun setup() {
        remoteDataSource = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        every { userPreferences.authToken } returns "test-token"
        repository = ApiRepositoryImpl(remoteDataSource, userPreferences)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    

    @Test
    fun `sendAudioCommand should handle null auth token`() {
        every { userPreferences.authToken } returns null
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        val callback: (ApiResponse) -> Unit = mockk(relaxed = true)

        repository.sendAudioCommand(audioData, callback)

        verify {
            remoteDataSource.sendAudioCommand(
                audioFile = mockFile,
                authToken = null,
                callback = any()
            )
        }
    }

    @Test
    fun `sendAudioCommand success callback should include audio file when provided`() {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        val responseAudioFile = mockk<File>(relaxed = true)
        var capturedResponse: ApiResponse? = null
        val callbackSlot = slot<RemoteDataSource.ApiCallback>()

        every {
            remoteDataSource.sendAudioCommand(any(), any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onSuccess(200, "Success", responseAudioFile)
        }

        repository.sendAudioCommand(audioData) { response ->
            capturedResponse = response
        }

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Success::class.java)
        val successResponse = capturedResponse as ApiResponse.Success
        assertThat(successResponse.audioFile).isEqualTo(responseAudioFile)
    }

    @Test
    fun `sendAudioCommand success callback should handle null audio file`() {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        var capturedResponse: ApiResponse? = null
        val callbackSlot = slot<RemoteDataSource.ApiCallback>()

        every {
            remoteDataSource.sendAudioCommand(any(), any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onSuccess(200, "Success", null)
        }

        repository.sendAudioCommand(audioData) { response ->
            capturedResponse = response
        }

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Success::class.java)
        val successResponse = capturedResponse as ApiResponse.Success
        assertThat(successResponse.audioFile).isNull()
    }

    @Test
    fun `sendAudioCommand error callback should handle IOException with message`() {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        var capturedResponse: ApiResponse? = null
        val callbackSlot = slot<RemoteDataSource.ApiCallback>()

        every {
            remoteDataSource.sendAudioCommand(any(), any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onFailure(IOException("Network error"))
        }

        repository.sendAudioCommand(audioData) { response ->
            capturedResponse = response
        }

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Error::class.java)
        val errorResponse = capturedResponse as ApiResponse.Error
        assertThat(errorResponse.message).isEqualTo("Network error")
    }

    @Test
    fun `sendAudioCommand error callback should handle IOException without message`() {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        var capturedResponse: ApiResponse? = null
        val callbackSlot = slot<RemoteDataSource.ApiCallback>()

        every {
            remoteDataSource.sendAudioCommand(any(), any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onFailure(IOException())
        }

        repository.sendAudioCommand(audioData) { response ->
            capturedResponse = response
        }

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Error::class.java)
        val errorResponse = capturedResponse as ApiResponse.Error
        assertThat(errorResponse.message).isEqualTo("Unknown error")
    }

    

    @Test
    fun `sendAuthentication should format response body correctly`() {
        var capturedResponse: ApiResponse? = null
        val callbackSlot = slot<RemoteDataSource.AuthCallback>()

        every {
            remoteDataSource.sendAuthentication(any(), any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onSuccess(200, "Welcome", "token123")
        }

        repository.sendAuthentication("Carlos", 1234) { response ->
            capturedResponse = response
        }

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Success::class.java)
        val successResponse = capturedResponse as ApiResponse.Success
        assertThat(successResponse.body).contains("\"message\":\"Welcome\"")
        assertThat(successResponse.body).contains("\"token\":\"token123\"")
    }

    @Test
    fun `sendAuthentication should handle special characters in message`() {
        var capturedResponse: ApiResponse? = null
        val callbackSlot = slot<RemoteDataSource.AuthCallback>()

        every {
            remoteDataSource.sendAuthentication(any(), any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onSuccess(200, "Welcome \"user\"", "token")
        }

        repository.sendAuthentication("User", 5678) { response ->
            capturedResponse = response
        }

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Success::class.java)
        val successResponse = capturedResponse as ApiResponse.Success
        assertThat(successResponse.body).contains("Welcome")
    }

    @Test
    fun `sendAuthentication error should extract status code from AuthenticationException`() {
        var capturedResponse: ApiResponse? = null
        val authException = RemoteDataSource.AuthenticationException("Auth failed", 401)
        val callbackSlot = slot<RemoteDataSource.AuthCallback>()

        every {
            remoteDataSource.sendAuthentication(any(), any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onFailure(authException)
        }

        repository.sendAuthentication("User", 1111) { response ->
            capturedResponse = response
        }

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Error::class.java)
        val errorResponse = capturedResponse as ApiResponse.Error
        assertThat(errorResponse.statusCode).isEqualTo(401)
        assertThat(errorResponse.message).isEqualTo("Auth failed")
    }

    @Test
    fun `sendAuthentication error should handle regular IOException`() {
        var capturedResponse: ApiResponse? = null
        val callbackSlot = slot<RemoteDataSource.AuthCallback>()

        every {
            remoteDataSource.sendAuthentication(any(), any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onFailure(IOException("Connection timeout"))
        }

        repository.sendAuthentication("User", 9999) { response ->
            capturedResponse = response
        }

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Error::class.java)
        val errorResponse = capturedResponse as ApiResponse.Error
        assertThat(errorResponse.statusCode).isNull()
        assertThat(errorResponse.message).isEqualTo("Connection timeout")
    }

    @Test
    fun `sendAuthentication error should handle IOException without message`() {
        var capturedResponse: ApiResponse? = null
        val callbackSlot = slot<RemoteDataSource.AuthCallback>()

        every {
            remoteDataSource.sendAuthentication(any(), any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onFailure(IOException())
        }

        repository.sendAuthentication("User", 0) { response ->
            capturedResponse = response
        }

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Error::class.java)
        val errorResponse = capturedResponse as ApiResponse.Error
        assertThat(errorResponse.message).isEqualTo("Unknown error")
    }

    

    @Test
    fun `pollAudio should pass null auth token when not set`() {
        every { userPreferences.authToken } returns null

        repository.pollAudio(
            onAudioReceived = { _, _, _ -> },
            onNoAudio = { },
            onError = { }
        )

        verify {
            remoteDataSource.pollAudio(
                authToken = null,
                callback = any()
            )
        }
    }

    @Test
    fun `pollAudio onAudioReceived should pass all parameters`() {
        val mockFile = mockk<File>(relaxed = true)
        var receivedFile: File? = null
        var receivedUserId: String? = null
        var receivedChannel: String? = null
        val callbackSlot = slot<RemoteDataSource.AudioPollCallback>()

        every {
            remoteDataSource.pollAudio(any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onAudioReceived(mockFile, "user123", "channel456")
        }

        repository.pollAudio(
            onAudioReceived = { file, userId, channel ->
                receivedFile = file
                receivedUserId = userId
                receivedChannel = channel
            },
            onNoAudio = { },
            onError = { }
        )

        assertThat(receivedFile).isEqualTo(mockFile)
        assertThat(receivedUserId).isEqualTo("user123")
        assertThat(receivedChannel).isEqualTo("channel456")
    }

    @Test
    fun `pollAudio onNoAudio should be called correctly`() {
        var noAudioCalled = false
        val callbackSlot = slot<RemoteDataSource.AudioPollCallback>()

        every {
            remoteDataSource.pollAudio(any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onNoAudio()
        }

        repository.pollAudio(
            onAudioReceived = { _, _, _ -> },
            onNoAudio = { noAudioCalled = true },
            onError = { }
        )

        assertThat(noAudioCalled).isTrue()
    }

    @Test
    fun `pollAudio onError should handle IOException with message`() {
        var errorMessage: String? = null
        val callbackSlot = slot<RemoteDataSource.AudioPollCallback>()

        every {
            remoteDataSource.pollAudio(any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onFailure(IOException("Poll failed"))
        }

        repository.pollAudio(
            onAudioReceived = { _, _, _ -> },
            onNoAudio = { },
            onError = { errorMessage = it }
        )

        assertThat(errorMessage).isEqualTo("Poll failed")
    }

    @Test
    fun `pollAudio onError should handle IOException without message`() {
        var errorMessage: String? = null
        val callbackSlot = slot<RemoteDataSource.AudioPollCallback>()

        every {
            remoteDataSource.pollAudio(any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onFailure(IOException())
        }

        repository.pollAudio(
            onAudioReceived = { _, _, _ -> },
            onNoAudio = { },
            onError = { errorMessage = it }
        )

        assertThat(errorMessage).isEqualTo("Unknown error")
    }

    @Test
    fun `pollAudio should handle empty channel string`() {
        val mockFile = mockk<File>(relaxed = true)
        var receivedChannel: String? = null
        val callbackSlot = slot<RemoteDataSource.AudioPollCallback>()

        every {
            remoteDataSource.pollAudio(any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onAudioReceived(mockFile, "user", "")
        }

        repository.pollAudio(
            onAudioReceived = { _, _, channel ->
                receivedChannel = channel
            },
            onNoAudio = { },
            onError = { }
        )

        assertThat(receivedChannel).isEmpty()
    }

    @Test
    fun `pollAudio should handle empty userId string`() {
        val mockFile = mockk<File>(relaxed = true)
        var receivedUserId: String? = null
        val callbackSlot = slot<RemoteDataSource.AudioPollCallback>()

        every {
            remoteDataSource.pollAudio(any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured.onAudioReceived(mockFile, "", "channel")
        }

        repository.pollAudio(
            onAudioReceived = { _, userId, _ ->
                receivedUserId = userId
            },
            onNoAudio = { },
            onError = { }
        )

        assertThat(receivedUserId).isEmpty()
    }
}
