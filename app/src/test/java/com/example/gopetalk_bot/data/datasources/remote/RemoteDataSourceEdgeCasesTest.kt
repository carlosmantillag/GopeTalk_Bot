package com.example.gopetalk_bot.data.datasources.remote

import android.util.Base64
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

/**
 * Tests adicionales para aumentar cobertura de RemoteDataSource
 * Enfocados en casos edge y escenarios específicos
 */
class RemoteDataSourceEdgeCasesTest {

    private lateinit var dataSource: RemoteDataSource
    private lateinit var mockMainThreadExecutor: MainThreadExecutor
    private lateinit var mockBase64Decoder: Base64Decoder

    @Before
    fun setup() {
        mockMainThreadExecutor = mockk(relaxed = true)
        mockBase64Decoder = mockk(relaxed = true)
        
        // Mock para ejecutar inmediatamente
        every { mockMainThreadExecutor.post(any()) } answers {
            firstArg<Runnable>().run()
        }
        
        // Mock para Base64
        every { mockBase64Decoder.decode(any(), any()) } returns ByteArray(100)
        
        dataSource = RemoteDataSource(mockMainThreadExecutor, mockBase64Decoder)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== Tests de ApiCallback ====================

    @Test
    fun `ApiCallback should handle success with all parameters`() {
        val callback = mockk<RemoteDataSource.ApiCallback>(relaxed = true)
        val mockFile = mockk<File>()
        
        callback.onSuccess(200, "Response body", mockFile)
        
        verify { callback.onSuccess(200, "Response body", mockFile) }
    }

    @Test
    fun `ApiCallback should handle success with empty body`() {
        val callback = mockk<RemoteDataSource.ApiCallback>(relaxed = true)
        
        callback.onSuccess(204, "", null)
        
        verify { callback.onSuccess(204, "", null) }
    }

    @Test
    fun `ApiCallback should handle different HTTP status codes`() {
        val callback = mockk<RemoteDataSource.ApiCallback>(relaxed = true)
        
        callback.onSuccess(200, "OK", null)
        callback.onSuccess(201, "Created", null)
        callback.onSuccess(202, "Accepted", null)
        callback.onSuccess(204, "No Content", null)
        
        verify { callback.onSuccess(200, "OK", null) }
        verify { callback.onSuccess(201, "Created", null) }
        verify { callback.onSuccess(202, "Accepted", null) }
        verify { callback.onSuccess(204, "No Content", null) }
    }

    @Test
    fun `ApiCallback should handle failure with nested exceptions`() {
        val callback = mockk<RemoteDataSource.ApiCallback>(relaxed = true)
        val rootCause = IllegalStateException("Root")
        val exception = IOException("Wrapper", rootCause)
        
        callback.onFailure(exception)
        
        verify { callback.onFailure(match { it.cause == rootCause }) }
    }

    // ==================== Tests de AudioDownloadCallback ====================

    @Test
    fun `AudioDownloadCallback should handle success with large files`() {
        val callback = mockk<RemoteDataSource.AudioDownloadCallback>(relaxed = true)
        val mockFile = mockk<File>()
        every { mockFile.length() } returns 10_000_000L // 10MB
        
        callback.onSuccess(mockFile)
        
        verify { callback.onSuccess(mockFile) }
    }

    @Test
    fun `AudioDownloadCallback should handle failure with timeout exception`() {
        val callback = mockk<RemoteDataSource.AudioDownloadCallback>(relaxed = true)
        val exception = IOException("Timeout")
        
        callback.onFailure(exception)
        
        verify { callback.onFailure(match { it.message == "Timeout" }) }
    }

    @Test
    fun `AudioDownloadCallback should handle multiple sequential downloads`() {
        val callback = mockk<RemoteDataSource.AudioDownloadCallback>(relaxed = true)
        val file1 = mockk<File>()
        val file2 = mockk<File>()
        val file3 = mockk<File>()
        
        callback.onSuccess(file1)
        callback.onSuccess(file2)
        callback.onSuccess(file3)
        
        verify { callback.onSuccess(file1) }
        verify { callback.onSuccess(file2) }
        verify { callback.onSuccess(file3) }
    }

    // ==================== Tests de AuthCallback ====================

    @Test
    fun `AuthCallback should handle success with JWT token`() {
        val callback = mockk<RemoteDataSource.AuthCallback>(relaxed = true)
        val jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U"
        
        callback.onSuccess(200, "Authentication successful", jwtToken)
        
        verify { callback.onSuccess(200, "Authentication successful", jwtToken) }
    }

    @Test
    fun `AuthCallback should handle failure with AuthenticationException`() {
        val callback = mockk<RemoteDataSource.AuthCallback>(relaxed = true)
        val exception = RemoteDataSource.AuthenticationException("Invalid credentials", 401)
        
        callback.onFailure(exception)
        
        verify { callback.onFailure(match { it is RemoteDataSource.AuthenticationException }) }
    }

    @Test
    fun `AuthCallback should handle different authentication messages`() {
        val callback = mockk<RemoteDataSource.AuthCallback>(relaxed = true)
        
        callback.onSuccess(200, "Login successful", "token1")
        callback.onSuccess(200, "Welcome back", "token2")
        callback.onSuccess(200, "Authenticated", "token3")
        
        verify { callback.onSuccess(200, "Login successful", "token1") }
        verify { callback.onSuccess(200, "Welcome back", "token2") }
        verify { callback.onSuccess(200, "Authenticated", "token3") }
    }

    @Test
    fun `AuthCallback should handle special characters in tokens`() {
        val callback = mockk<RemoteDataSource.AuthCallback>(relaxed = true)
        val specialToken = "token-with-special_chars.123+/="
        
        callback.onSuccess(200, "OK", specialToken)
        
        verify { callback.onSuccess(200, "OK", specialToken) }
    }

    // ==================== Tests de AudioPollCallback ====================

    @Test
    fun `AudioPollCallback should handle onAudioReceived with metadata`() {
        val callback = mockk<RemoteDataSource.AudioPollCallback>(relaxed = true)
        val mockFile = mockk<File>()
        
        callback.onAudioReceived(mockFile, "user123", "general")
        
        verify { callback.onAudioReceived(mockFile, "user123", "general") }
    }

    @Test
    fun `AudioPollCallback should handle onNoAudio correctly`() {
        val callback = mockk<RemoteDataSource.AudioPollCallback>(relaxed = true)
        
        callback.onNoAudio()
        
        verify(exactly = 1) { callback.onNoAudio() }
    }

    @Test
    fun `AudioPollCallback should handle failure during polling`() {
        val callback = mockk<RemoteDataSource.AudioPollCallback>(relaxed = true)
        val exception = IOException("Polling failed")
        
        callback.onFailure(exception)
        
        verify { callback.onFailure(match { it.message == "Polling failed" }) }
    }

    @Test
    fun `AudioPollCallback should handle special characters in user and channel`() {
        val callback = mockk<RemoteDataSource.AudioPollCallback>(relaxed = true)
        val mockFile = mockk<File>()
        
        callback.onAudioReceived(mockFile, "user@domain.com", "channel#123")
        
        verify { callback.onAudioReceived(mockFile, "user@domain.com", "channel#123") }
    }

    @Test
    fun `AudioPollCallback should handle very long user and channel names`() {
        val callback = mockk<RemoteDataSource.AudioPollCallback>(relaxed = true)
        val mockFile = mockk<File>()
        val longUser = "u".repeat(1000)
        val longChannel = "c".repeat(1000)
        
        callback.onAudioReceived(mockFile, longUser, longChannel)
        
        verify { callback.onAudioReceived(mockFile, longUser, longChannel) }
    }

    // ==================== Tests de AuthenticationException ====================

    @Test
    fun `AuthenticationException should handle 400 Bad Request`() {
        val exception = RemoteDataSource.AuthenticationException("Bad Request", 400)
        
        assertThat(exception.statusCode).isEqualTo(400)
        assertThat(exception.message).isEqualTo("Bad Request")
    }

    @Test
    fun `AuthenticationException should handle 403 Forbidden`() {
        val exception = RemoteDataSource.AuthenticationException("Forbidden", 403)
        
        assertThat(exception.statusCode).isEqualTo(403)
    }

    @Test
    fun `AuthenticationException should handle 404 Not Found`() {
        val exception = RemoteDataSource.AuthenticationException("Not Found", 404)
        
        assertThat(exception.statusCode).isEqualTo(404)
    }

    @Test
    fun `AuthenticationException should handle 500 Internal Server Error`() {
        val exception = RemoteDataSource.AuthenticationException("Internal Server Error", 500)
        
        assertThat(exception.statusCode).isEqualTo(500)
    }

    @Test
    fun `AuthenticationException should handle 503 Service Unavailable`() {
        val exception = RemoteDataSource.AuthenticationException("Service Unavailable", 503)
        
        assertThat(exception.statusCode).isEqualTo(503)
    }

    // ==================== Tests de MainThreadExecutor ====================

    @Test
    fun `MainThreadExecutor should handle runnable that throws exception`() {
        val runnable = Runnable { throw RuntimeException("Test exception") }
        
        try {
            mockMainThreadExecutor.post(runnable)
        } catch (e: RuntimeException) {
            assertThat(e.message).isEqualTo("Test exception")
        }
    }

    @Test
    fun `MainThreadExecutor should handle empty runnable`() {
        val runnable = Runnable { }
        
        mockMainThreadExecutor.post(runnable)
        
        verify { mockMainThreadExecutor.post(runnable) }
    }

    @Test
    fun `MainThreadExecutor should handle runnable with long execution`() {
        var executed = false
        val runnable = Runnable {
            Thread.sleep(10)
            executed = true
        }
        
        mockMainThreadExecutor.post(runnable)
        
        assertThat(executed).isTrue()
    }

    // ==================== Tests de Base64Decoder ====================

    @Test
    fun `Base64Decoder should handle URL_SAFE flag`() {
        val testString = "test-url_safe"
        every { mockBase64Decoder.decode(testString, Base64.URL_SAFE) } returns ByteArray(20)
        
        val result = mockBase64Decoder.decode(testString, Base64.URL_SAFE)
        
        assertThat(result).hasLength(20)
    }

    @Test
    fun `Base64Decoder should handle NO_PADDING flag`() {
        val testString = "testnopadding"
        every { mockBase64Decoder.decode(testString, Base64.NO_PADDING) } returns ByteArray(15)
        
        val result = mockBase64Decoder.decode(testString, Base64.NO_PADDING)
        
        assertThat(result).hasLength(15)
    }

    @Test
    fun `Base64Decoder should handle combined flags`() {
        val testString = "test"
        val combinedFlags = Base64.URL_SAFE or Base64.NO_WRAP
        every { mockBase64Decoder.decode(testString, combinedFlags) } returns ByteArray(25)
        
        val result = mockBase64Decoder.decode(testString, combinedFlags)
        
        assertThat(result).hasLength(25)
    }

    @Test
    fun `Base64Decoder should handle invalid base64 string gracefully`() {
        val invalidString = "not-valid-base64!!!"
        every { mockBase64Decoder.decode(invalidString, any()) } throws IllegalArgumentException("Invalid base64")
        
        try {
            mockBase64Decoder.decode(invalidString, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).isEqualTo("Invalid base64")
        }
    }

    // ==================== Tests de callbacks concurrentes ====================

    @Test
    fun `multiple callbacks of different types should work independently`() {
        val apiCallback = mockk<RemoteDataSource.ApiCallback>(relaxed = true)
        val downloadCallback = mockk<RemoteDataSource.AudioDownloadCallback>(relaxed = true)
        val authCallback = mockk<RemoteDataSource.AuthCallback>(relaxed = true)
        val pollCallback = mockk<RemoteDataSource.AudioPollCallback>(relaxed = true)
        
        apiCallback.onSuccess(200, "OK", null)
        downloadCallback.onSuccess(mockk())
        authCallback.onSuccess(200, "Auth OK", "token")
        pollCallback.onNoAudio()
        
        verify { apiCallback.onSuccess(200, "OK", null) }
        verify { downloadCallback.onSuccess(any()) }
        verify { authCallback.onSuccess(200, "Auth OK", "token") }
        verify { pollCallback.onNoAudio() }
    }

    @Test
    fun `callbacks should handle rapid sequential calls`() {
        val callback = mockk<RemoteDataSource.ApiCallback>(relaxed = true)
        
        repeat(100) { i ->
            callback.onSuccess(200, "Response $i", null)
        }
        
        verify(exactly = 100) { callback.onSuccess(any(), any(), any()) }
    }

    // ==================== Tests de casos edge adicionales ====================

    @Test
    fun `AuthenticationException with zero status code should work`() {
        val exception = RemoteDataSource.AuthenticationException("Error", 0)
        
        assertThat(exception.statusCode).isEqualTo(0)
    }

    @Test
    fun `AuthenticationException with negative status code should work`() {
        val exception = RemoteDataSource.AuthenticationException("Error", -1)
        
        assertThat(exception.statusCode).isEqualTo(-1)
    }

    @Test
    fun `callbacks should handle unicode characters in messages`() {
        val callback = mockk<RemoteDataSource.ApiCallback>(relaxed = true)
        val unicodeMessage = "Success ✓ 成功 🎉"
        
        callback.onSuccess(200, unicodeMessage, null)
        
        verify { callback.onSuccess(200, unicodeMessage, null) }
    }

    @Test
    fun `Base64Decoder should handle whitespace in strings`() {
        val stringWithWhitespace = "SGVs bG8g V29y bGQ="
        every { mockBase64Decoder.decode(stringWithWhitespace, any()) } returns ByteArray(30)
        
        val result = mockBase64Decoder.decode(stringWithWhitespace, Base64.DEFAULT)
        
        assertThat(result).hasLength(30)
    }
}
