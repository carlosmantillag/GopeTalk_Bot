package com.example.gopetalk_bot.data.repositories

import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.data.datasources.remote.RemoteDataSource
import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.AudioData
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.IOException

class ApiRepositoryImplTest {

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
    fun `sendAudioCommand should call remote data source with correct parameters`() {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, com.example.gopetalk_bot.domain.entities.AudioFormat.PCM_16BIT)
        val callback: (ApiResponse) -> Unit = mockk(relaxed = true)

        repository.sendAudioCommand(audioData, callback)

        verify { 
            remoteDataSource.sendAudioCommand(
                audioFile = mockFile,
                authToken = "test-token",
                callback = any()
            )
        }
    }

    @Test
    fun `sendAudioCommand should return success response on success`() {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, com.example.gopetalk_bot.domain.entities.AudioFormat.PCM_16BIT)
        var capturedResponse: ApiResponse? = null
        var capturedCallback: RemoteDataSource.ApiCallback? = null

        every { remoteDataSource.sendAudioCommand(any(), any(), any()) } answers {
            capturedCallback = thirdArg()
        }

        repository.sendAudioCommand(audioData) { response ->
            capturedResponse = response
        }

        capturedCallback?.onSuccess(200, "Success", null)

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Success::class.java)
        val successResponse = capturedResponse as ApiResponse.Success
        assertThat(successResponse.statusCode).isEqualTo(200)
        assertThat(successResponse.body).isEqualTo("Success")
    }

    @Test
    fun `sendAudioCommand should return error response on failure`() {
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, com.example.gopetalk_bot.domain.entities.AudioFormat.PCM_16BIT)
        var capturedResponse: ApiResponse? = null
        var capturedCallback: RemoteDataSource.ApiCallback? = null

        every { remoteDataSource.sendAudioCommand(any(), any(), any()) } answers {
            capturedCallback = thirdArg()
        }

        repository.sendAudioCommand(audioData) { response ->
            capturedResponse = response
        }

        val ioException = IOException("Network error")
        capturedCallback?.onFailure(ioException)

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Error::class.java)
        val errorResponse = capturedResponse as ApiResponse.Error
        assertThat(errorResponse.message).isEqualTo("Network error")
        assertThat(errorResponse.exception).isEqualTo(ioException)
    }

    @Test
    fun `sendAuthentication should call remote data source with correct parameters`() {
        val callback: (ApiResponse) -> Unit = mockk(relaxed = true)

        repository.sendAuthentication("TestUser", 1234, callback)

        verify {
            remoteDataSource.sendAuthentication(
                nombre = "TestUser",
                pin = 1234,
                callback = any()
            )
        }
    }

    @Test
    fun `sendAuthentication should return success response with JSON body`() {
        var capturedResponse: ApiResponse? = null
        var capturedCallback: RemoteDataSource.AuthCallback? = null

        every { remoteDataSource.sendAuthentication(any(), any(), any()) } answers {
            capturedCallback = thirdArg()
        }

        repository.sendAuthentication("TestUser", 1234) { response ->
            capturedResponse = response
        }

        capturedCallback?.onSuccess(200, "Welcome", "auth-token-123")

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Success::class.java)
        val successResponse = capturedResponse as ApiResponse.Success
        assertThat(successResponse.statusCode).isEqualTo(200)
        assertThat(successResponse.body).contains("Welcome")
        assertThat(successResponse.body).contains("auth-token-123")
    }

    @Test
    fun `sendAuthentication should return error response on failure`() {
        var capturedResponse: ApiResponse? = null
        var capturedCallback: RemoteDataSource.AuthCallback? = null

        every { remoteDataSource.sendAuthentication(any(), any(), any()) } answers {
            capturedCallback = thirdArg()
        }

        repository.sendAuthentication("TestUser", 1234) { response ->
            capturedResponse = response
        }

        val ioException = IOException("Auth failed")
        capturedCallback?.onFailure(ioException)

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Error::class.java)
        val errorResponse = capturedResponse as ApiResponse.Error
        assertThat(errorResponse.message).isEqualTo("Auth failed")
    }

    @Test
    fun `pollAudio should call remote data source with auth token`() {
        val onAudioReceived: (File, String, String) -> Unit = mockk(relaxed = true)
        val onNoAudio: () -> Unit = mockk(relaxed = true)
        val onError: (String) -> Unit = mockk(relaxed = true)

        repository.pollAudio(onAudioReceived, onNoAudio, onError)

        verify {
            remoteDataSource.pollAudio(
                authToken = "test-token",
                callback = any()
            )
        }
    }

    @Test
    fun `pollAudio should invoke onAudioReceived when audio is available`() {
        val mockFile = mockk<File>(relaxed = true)
        var capturedCallback: RemoteDataSource.AudioPollCallback? = null
        var receivedFile: File? = null
        var receivedUserId: String? = null
        var receivedChannel: String? = null

        every { remoteDataSource.pollAudio(any(), any()) } answers {
            capturedCallback = secondArg()
        }

        repository.pollAudio(
            onAudioReceived = { file, userId, channel ->
                receivedFile = file
                receivedUserId = userId
                receivedChannel = channel
            },
            onNoAudio = {},
            onError = {}
        )

        capturedCallback?.onAudioReceived(mockFile, "user123", "general")

        assertThat(receivedFile).isEqualTo(mockFile)
        assertThat(receivedUserId).isEqualTo("user123")
        assertThat(receivedChannel).isEqualTo("general")
    }

    @Test
    fun `pollAudio should invoke onNoAudio when no audio available`() {
        var capturedCallback: RemoteDataSource.AudioPollCallback? = null
        var noAudioCalled = false

        every { remoteDataSource.pollAudio(any(), any()) } answers {
            capturedCallback = secondArg()
        }

        repository.pollAudio(
            onAudioReceived = { _, _, _ -> },
            onNoAudio = { noAudioCalled = true },
            onError = {}
        )

        capturedCallback?.onNoAudio()

        assertThat(noAudioCalled).isTrue()
    }

    @Test
    fun `pollAudio should invoke onError on failure`() {
        var capturedCallback: RemoteDataSource.AudioPollCallback? = null
        var errorMessage: String? = null

        every { remoteDataSource.pollAudio(any(), any()) } answers {
            capturedCallback = secondArg()
        }

        repository.pollAudio(
            onAudioReceived = { _, _, _ -> },
            onNoAudio = {},
            onError = { errorMessage = it }
        )

        capturedCallback?.onFailure(IOException("Polling failed"))

        assertThat(errorMessage).isEqualTo("Polling failed")
    }

    @Test
    fun `sendAudioCommand with audio file response should return file`() {
        val mockFile = mockk<File>(relaxed = true)
        val mockResponseFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, com.example.gopetalk_bot.domain.entities.AudioFormat.PCM_16BIT)
        var capturedResponse: ApiResponse? = null
        var capturedCallback: RemoteDataSource.ApiCallback? = null

        every { remoteDataSource.sendAudioCommand(any(), any(), any()) } answers {
            capturedCallback = thirdArg()
        }

        repository.sendAudioCommand(audioData) { response ->
            capturedResponse = response
        }

        capturedCallback?.onSuccess(200, "OK", mockResponseFile)

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Success::class.java)
        val successResponse = capturedResponse as ApiResponse.Success
        assertThat(successResponse.audioFile).isEqualTo(mockResponseFile)
    }

    @Test
    fun `sendAuthentication with different status codes should work`() {
        var capturedResponse: ApiResponse? = null
        var capturedCallback: RemoteDataSource.AuthCallback? = null

        every { remoteDataSource.sendAuthentication(any(), any(), any()) } answers {
            capturedCallback = thirdArg()
        }

        repository.sendAuthentication("User", 5678) { response ->
            capturedResponse = response
        }

        capturedCallback?.onSuccess(201, "Created", "new-token")

        assertThat(capturedResponse).isInstanceOf(ApiResponse.Success::class.java)
        val successResponse = capturedResponse as ApiResponse.Success
        assertThat(successResponse.statusCode).isEqualTo(201)
    }

    @Test
    fun `pollAudio with different channels should work`() {
        val mockFile = mockk<File>(relaxed = true)
        var capturedCallback: RemoteDataSource.AudioPollCallback? = null
        var receivedChannel: String? = null

        every { remoteDataSource.pollAudio(any(), any()) } answers {
            capturedCallback = secondArg()
        }

        repository.pollAudio(
            onAudioReceived = { _, _, channel -> receivedChannel = channel },
            onNoAudio = {},
            onError = {}
        )

        capturedCallback?.onAudioReceived(mockFile, "user456", "private-channel")

        assertThat(receivedChannel).isEqualTo("private-channel")
    }

    @Test
    fun `sendAudioCommand with empty auth token should still work`() {
        every { userPreferences.authToken } returns ""
        val mockFile = mockk<File>(relaxed = true)
        val audioData = AudioData(mockFile, 16000, 1, com.example.gopetalk_bot.domain.entities.AudioFormat.PCM_16BIT)

        repository.sendAudioCommand(audioData) {}

        verify { 
            remoteDataSource.sendAudioCommand(
                audioFile = mockFile,
                authToken = "",
                callback = any()
            )
        }
    }
}
