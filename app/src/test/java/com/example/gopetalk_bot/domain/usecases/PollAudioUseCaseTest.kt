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

    @Test
    fun `execute should handle multiple consecutive calls`() {
        val onAudioReceived: (File, String, String) -> Unit = mockk(relaxed = true)
        val onNoAudio: () -> Unit = mockk(relaxed = true)
        val onError: (String) -> Unit = mockk(relaxed = true)

        useCase.execute(onAudioReceived, onNoAudio, onError)
        useCase.execute(onAudioReceived, onNoAudio, onError)

        verify(exactly = 2) { apiRepository.pollAudio(onAudioReceived, onNoAudio, onError) }
    }

    @Test
    fun `execute should handle different callback instances`() {
        val onAudioReceived1: (File, String, String) -> Unit = mockk(relaxed = true)
        val onNoAudio1: () -> Unit = mockk(relaxed = true)
        val onError1: (String) -> Unit = mockk(relaxed = true)

        val onAudioReceived2: (File, String, String) -> Unit = mockk(relaxed = true)
        val onNoAudio2: () -> Unit = mockk(relaxed = true)
        val onError2: (String) -> Unit = mockk(relaxed = true)

        useCase.execute(onAudioReceived1, onNoAudio1, onError1)
        useCase.execute(onAudioReceived2, onNoAudio2, onError2)

        verify { apiRepository.pollAudio(onAudioReceived1, onNoAudio1, onError1) }
        verify { apiRepository.pollAudio(onAudioReceived2, onNoAudio2, onError2) }
    }

    @Test
    fun `execute should work with lambda callbacks`() {
        var file: File? = null
        var userId: String? = null
        var channel: String? = null

        useCase.execute(
            onAudioReceived = { f, u, c -> 
                file = f
                userId = u
                channel = c
            },
            onNoAudio = {},
            onError = {}
        )

        verify { apiRepository.pollAudio(any(), any(), any()) }
    }

    @Test
    fun `execute should handle empty lambda callbacks`() {
        useCase.execute(
            onAudioReceived = { _, _, _ -> },
            onNoAudio = {},
            onError = {}
        )

        verify { apiRepository.pollAudio(any(), any(), any()) }
    }
}
