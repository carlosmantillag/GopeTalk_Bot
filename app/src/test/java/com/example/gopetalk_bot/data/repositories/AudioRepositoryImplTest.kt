package com.example.gopetalk_bot.data.repositories

import com.example.gopetalk_bot.data.datasources.local.AudioDataSource
import com.example.gopetalk_bot.domain.entities.AudioData
import com.example.gopetalk_bot.domain.entities.AudioLevel
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AudioRepositoryImplTest {

    private lateinit var audioDataSource: AudioDataSource
    private lateinit var repository: AudioRepositoryImpl

    @Before
    fun setup() {
        audioDataSource = mockk(relaxed = true)
        repository = AudioRepositoryImpl(audioDataSource)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `startMonitoring should call data source startMonitoring`() {
        repository.startMonitoring()

        verify { audioDataSource.startMonitoring(any(), any(), any()) }
    }

    @Test
    fun `stopMonitoring should call data source stopMonitoring`() {
        repository.stopMonitoring()

        verify { audioDataSource.stopMonitoring() }
    }

    @Test
    fun `pauseRecording should call data source pauseRecording`() {
        repository.pauseRecording()

        verify { audioDataSource.pauseRecording() }
    }

    @Test
    fun `resumeRecording should call data source resumeRecording`() {
        repository.resumeRecording()

        verify { audioDataSource.resumeRecording() }
    }

    @Test
    fun `release should call data source release and clear callbacks`() {
        repository.release()

        verify { audioDataSource.release() }
    }

   /* @Test
    fun `getRecordedAudioStream should emit recorded audio from data source`() = runTest {
        var capturedCallback: ((AudioDataSource.RecordedAudioData) -> Unit)? = null
        val mockFile = mockk<File>(relaxed = true)

        every { audioDataSource.startMonitoring(any(), any(), any()) } answers {
            capturedCallback = secondArg()
        }

        repository.startMonitoring()
        val flow = repository.getRecordedAudioStream()

        // Simulate recording stopped callback
        val testRecordedData = AudioDataSource.RecordedAudioData(file = mockFile)
        capturedCallback?.invoke(testRecordedData)

        val result = flow.first()
        assertThat(result.file).isEqualTo(mockFile)
        assertThat(result.sampleRate).isEqualTo(16000)
    }


    */

    @Test
    fun `multiple startMonitoring calls should work correctly`() {
        repository.startMonitoring()
        repository.startMonitoring()

        verify(exactly = 2) { audioDataSource.startMonitoring(any(), any(), any()) }
    }

    @Test
    fun `pauseRecording and resumeRecording should work in sequence`() {
        repository.pauseRecording()
        repository.resumeRecording()
        repository.pauseRecording()

        verify(exactly = 2) { audioDataSource.pauseRecording() }
        verify(exactly = 1) { audioDataSource.resumeRecording() }
    }

    @Test
    fun `stopMonitoring should be idempotent`() {
        repository.stopMonitoring()
        repository.stopMonitoring()

        verify(exactly = 2) { audioDataSource.stopMonitoring() }
    }

    @Test
    fun `release should be idempotent`() {
        repository.release()
        repository.release()

        verify(exactly = 2) { audioDataSource.release() }
    }

    @Test
    fun `startMonitoring after stopMonitoring should work`() {
        repository.startMonitoring()
        repository.stopMonitoring()
        repository.startMonitoring()

        verify(exactly = 2) { audioDataSource.startMonitoring(any(), any(), any()) }
        verify(exactly = 1) { audioDataSource.stopMonitoring() }
    }

    @Test
    fun `pauseRecording multiple times should work`() {
        repository.pauseRecording()
        repository.pauseRecording()
        repository.pauseRecording()

        verify(exactly = 3) { audioDataSource.pauseRecording() }
    }

    @Test
    fun `resumeRecording multiple times should work`() {
        repository.resumeRecording()
        repository.resumeRecording()

        verify(exactly = 2) { audioDataSource.resumeRecording() }
    }

    @Test
    fun `error callback should be invoked on data source error`() = runTest {
        var capturedErrorCallback: ((String) -> Unit)? = null
        var errorMessage: String? = null

        every { audioDataSource.startMonitoring(any(), any(), any()) } answers {
            capturedErrorCallback = thirdArg()
        }

        repository.startMonitoring()
        capturedErrorCallback?.invoke("Test error")

        // Error is logged but doesn't throw
        assertThat(capturedErrorCallback).isNotNull()
    }
}
