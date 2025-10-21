package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.entities.ApiResponse
import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.entities.AudioFormat
import com.example.gopetalk_bot.domain.repositories.ApiRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class SendAudioCommandUseCaseTest {

    private lateinit var apiRepository: ApiRepository
    private lateinit var useCase: SendAudioCommandUseCase
    private lateinit var mockFile: File

    @Before
    fun setup() {
        apiRepository = mockk(relaxed = true)
        useCase = SendAudioCommandUseCase(apiRepository)
        mockFile = mockk {
            every { exists() } returns true
            every { length() } returns 1024L
        }
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should call repository sendAudioCommand with correct parameters`() {
        val audioData = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        val callback: (ApiResponse) -> Unit = mockk(relaxed = true)

        useCase.execute(audioData, callback)

        verify { apiRepository.sendAudioCommand(audioData, callback) }
    }

    @Test
    fun `execute should invoke callback with success response`() {
        val audioData = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        val successResponse = ApiResponse.Success(200, "OK", null)
        val callback: (ApiResponse) -> Unit = mockk(relaxed = true)

        every { 
            apiRepository.sendAudioCommand(any(), any()) 
        } answers {
            val cb = secondArg<(ApiResponse) -> Unit>()
            cb(successResponse)
        }

        useCase.execute(audioData, callback)

        verify { callback(successResponse) }
    }

    @Test
    fun `execute should invoke callback with error response`() {
        val audioData = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        val errorResponse = ApiResponse.Error("Network error", null)
        val callback: (ApiResponse) -> Unit = mockk(relaxed = true)

        every { 
            apiRepository.sendAudioCommand(any(), any()) 
        } answers {
            val cb = secondArg<(ApiResponse) -> Unit>()
            cb(errorResponse)
        }

        useCase.execute(audioData, callback)

        verify { callback(errorResponse) }
    }

    @Test
    fun `execute should handle different sample rates`() {
        val audioData1 = AudioData(mockFile, 16000, 1, AudioFormat.PCM_16BIT)
        val audioData2 = AudioData(mockFile, 44100, 2, AudioFormat.PCM_16BIT)
        val callback: (ApiResponse) -> Unit = mockk(relaxed = true)

        useCase.execute(audioData1, callback)
        useCase.execute(audioData2, callback)

        verify(exactly = 2) { apiRepository.sendAudioCommand(any(), callback) }
    }
}
