package com.example.gopetalk_bot.domain.usecases

import com.example.gopetalk_bot.domain.repositories.ApiRepository
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class PollAudioUseCaseTest {

    private lateinit var apiRepository: ApiRepository
    private lateinit var useCase: PollAudioUseCase

    @Before
    fun setup() {
        apiRepository = mockk(relaxed = true)
        useCase = PollAudioUseCase(apiRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `execute should call pollAudio on repository with callbacks`() {
        val onAudioReceived: (File, String, String) -> Unit = mockk(relaxed = true)
        val onNoAudio: () -> Unit = mockk(relaxed = true)
        val onError: (String) -> Unit = mockk(relaxed = true)

        useCase.execute(onAudioReceived, onNoAudio, onError)

        verify { 
            apiRepository.pollAudio(
                onAudioReceived = onAudioReceived,
                onNoAudio = onNoAudio,
                onError = onError
            )
        }
    }

    @Test
    fun `execute should pass callbacks correctly to repository`() {
        var audioReceivedCalled = false
        var noAudioCalled = false
        var errorCalled = false

        useCase.execute(
            onAudioReceived = { _, _, _ -> audioReceivedCalled = true },
            onNoAudio = { noAudioCalled = true },
            onError = { errorCalled = true }
        )

        verify { apiRepository.pollAudio(any(), any(), any()) }
    }
}
