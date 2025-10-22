package com.example.gopetalk_bot.data.repositories

import android.util.Log
import com.example.gopetalk_bot.data.datasources.remote.WebSocketDataSource
import com.example.gopetalk_bot.domain.repositories.WebSocketRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Tests adicionales para aumentar branch coverage en WebSocketRepositoryImpl
 */
class WebSocketRepositoryImplEdgeCasesTest {

    private lateinit var webSocketDataSource: WebSocketDataSource
    private lateinit var repository: WebSocketRepositoryImpl

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0

        webSocketDataSource = mockk(relaxed = true)
        repository = WebSocketRepositoryImpl(webSocketDataSource)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        clearAllMocks()
    }

    // ==================== Connect Edge Cases ====================

    @Test
    fun `connect with null authToken should work`() {
        val listener = mockk<WebSocketRepository.MicrophoneControlListener>(relaxed = true)

        repository.connect("ws://test.com", null, "channel", listener)

        verify {
            webSocketDataSource.connect(
                url = "ws://test.com",
                authToken = null,
                channel = "channel",
                listener = any()
            )
        }
    }

    @Test
    fun `connect with null channel should work`() {
        val listener = mockk<WebSocketRepository.MicrophoneControlListener>(relaxed = true)

        repository.connect("ws://test.com", "token", null, listener)

        verify {
            webSocketDataSource.connect(
                url = "ws://test.com",
                authToken = "token",
                channel = null,
                listener = any()
            )
        }
    }

    @Test
    fun `connect with both null authToken and channel should work`() {
        val listener = mockk<WebSocketRepository.MicrophoneControlListener>(relaxed = true)

        repository.connect("ws://test.com", null, null, listener)

        verify {
            webSocketDataSource.connect(
                url = "ws://test.com",
                authToken = null,
                channel = null,
                listener = any()
            )
        }
    }

    @Test
    fun `connect with empty url should pass to datasource`() {
        val listener = mockk<WebSocketRepository.MicrophoneControlListener>(relaxed = true)

        repository.connect("", "token", "channel", listener)

        verify {
            webSocketDataSource.connect(
                url = "",
                authToken = "token",
                channel = "channel",
                listener = any()
            )
        }
    }

    @Test
    fun `connect with very long authToken should work`() {
        val listener = mockk<WebSocketRepository.MicrophoneControlListener>(relaxed = true)
        val longToken = "a".repeat(1000)

        repository.connect("ws://test.com", longToken, "channel", listener)

        verify {
            webSocketDataSource.connect(
                url = "ws://test.com",
                authToken = longToken,
                channel = "channel",
                listener = any()
            )
        }
    }

    // ==================== Listener Propagation Edge Cases ====================

    @Test
    fun `all listener events should be propagated correctly`() {
        val callbackSlot = slot<WebSocketDataSource.MicrophoneControlListener>()
        var micStartCalled = false
        var micStopCalled = false
        var connectionEstablishedCalled = false
        var connectionClosedCalled = false
        var errorCalled = false
        var errorMessage = ""

        every {
            webSocketDataSource.connect(any(), any(), any(), capture(callbackSlot))
        } just Runs

        val listener = object : WebSocketRepository.MicrophoneControlListener {
            override fun onMicrophoneStart() { micStartCalled = true }
            override fun onMicrophoneStop() { micStopCalled = true }
            override fun onConnectionEstablished() { connectionEstablishedCalled = true }
            override fun onConnectionClosed() { connectionClosedCalled = true }
            override fun onError(error: String) {
                errorCalled = true
                errorMessage = error
            }
        }

        repository.connect("ws://test.com", "token", "channel", listener)

        // Trigger all events
        callbackSlot.captured.onMicrophoneStart()
        callbackSlot.captured.onMicrophoneStop()
        callbackSlot.captured.onConnectionEstablished()
        callbackSlot.captured.onConnectionClosed()
        callbackSlot.captured.onError("Test error")

        assertThat(micStartCalled).isTrue()
        assertThat(micStopCalled).isTrue()
        assertThat(connectionEstablishedCalled).isTrue()
        assertThat(connectionClosedCalled).isTrue()
        assertThat(errorCalled).isTrue()
        assertThat(errorMessage).isEqualTo("Test error")
    }

    @Test
    fun `onError should propagate empty error message`() {
        val callbackSlot = slot<WebSocketDataSource.MicrophoneControlListener>()
        var errorMessage = "not-called"

        every {
            webSocketDataSource.connect(any(), any(), any(), capture(callbackSlot))
        } just Runs

        val listener = object : WebSocketRepository.MicrophoneControlListener {
            override fun onMicrophoneStart() {}
            override fun onMicrophoneStop() {}
            override fun onConnectionEstablished() {}
            override fun onConnectionClosed() {}
            override fun onError(error: String) { errorMessage = error }
        }

        repository.connect("ws://test.com", "token", "channel", listener)
        callbackSlot.captured.onError("")

        assertThat(errorMessage).isEmpty()
    }

    @Test
    fun `onError should propagate very long error message`() {
        val callbackSlot = slot<WebSocketDataSource.MicrophoneControlListener>()
        val longError = "Error: " + "x".repeat(5000)
        var errorMessage = ""

        every {
            webSocketDataSource.connect(any(), any(), any(), capture(callbackSlot))
        } just Runs

        val listener = object : WebSocketRepository.MicrophoneControlListener {
            override fun onMicrophoneStart() {}
            override fun onMicrophoneStop() {}
            override fun onConnectionEstablished() {}
            override fun onConnectionClosed() {}
            override fun onError(error: String) { errorMessage = error }
        }

        repository.connect("ws://test.com", "token", "channel", listener)
        callbackSlot.captured.onError(longError)

        assertThat(errorMessage).isEqualTo(longError)
    }

    // ==================== UpdateChannel Edge Cases ====================

    @Test
    fun `updateChannel with null authToken should work`() {
        repository.updateChannel(null, "new-channel")

        verify {
            webSocketDataSource.updateChannel(null, "new-channel")
        }
    }

    @Test
    fun `updateChannel with null channel should work`() {
        repository.updateChannel("token", null)

        verify {
            webSocketDataSource.updateChannel("token", null)
        }
    }

    @Test
    fun `updateChannel with both null should work`() {
        repository.updateChannel(null, null)

        verify {
            webSocketDataSource.updateChannel(null, null)
        }
    }

    @Test
    fun `updateChannel with empty strings should work`() {
        repository.updateChannel("", "")

        verify {
            webSocketDataSource.updateChannel("", "")
        }
    }

    @Test
    fun `updateChannel multiple times should work`() {
        repository.updateChannel("token1", "channel1")
        repository.updateChannel("token2", "channel2")
        repository.updateChannel("token3", "channel3")

        verify(exactly = 1) { webSocketDataSource.updateChannel("token1", "channel1") }
        verify(exactly = 1) { webSocketDataSource.updateChannel("token2", "channel2") }
        verify(exactly = 1) { webSocketDataSource.updateChannel("token3", "channel3") }
    }

    // ==================== Disconnect Edge Cases ====================

    @Test
    fun `disconnect should call datasource`() {
        repository.disconnect()

        verify { webSocketDataSource.disconnect() }
    }

    @Test
    fun `multiple disconnect calls should work`() {
        repository.disconnect()
        repository.disconnect()
        repository.disconnect()

        verify(exactly = 3) { webSocketDataSource.disconnect() }
    }

    @Test
    fun `disconnect without connect should work`() {
        // Should not throw exception
        repository.disconnect()

        verify { webSocketDataSource.disconnect() }
    }

    // ==================== IsConnected Edge Cases ====================

    @Test
    fun `isConnected should return true when connected`() {
        every { webSocketDataSource.isConnected() } returns true

        val result = repository.isConnected()

        assertThat(result).isTrue()
    }

    @Test
    fun `isConnected should return false when not connected`() {
        every { webSocketDataSource.isConnected() } returns false

        val result = repository.isConnected()

        assertThat(result).isFalse()
    }

    @Test
    fun `isConnected should be called multiple times`() {
        every { webSocketDataSource.isConnected() } returns true

        repository.isConnected()
        repository.isConnected()
        repository.isConnected()

        verify(exactly = 3) { webSocketDataSource.isConnected() }
    }

    // ==================== Complex Scenarios ====================

    @Test
    fun `connect then disconnect then connect again should work`() {
        val listener = mockk<WebSocketRepository.MicrophoneControlListener>(relaxed = true)

        repository.connect("ws://test1.com", "token1", "channel1", listener)
        repository.disconnect()
        repository.connect("ws://test2.com", "token2", "channel2", listener)

        verify(exactly = 1) {
            webSocketDataSource.connect("ws://test1.com", "token1", "channel1", any())
        }
        verify(exactly = 1) { webSocketDataSource.disconnect() }
        verify(exactly = 1) {
            webSocketDataSource.connect("ws://test2.com", "token2", "channel2", any())
        }
    }

    @Test
    fun `updateChannel after connect should work`() {
        val listener = mockk<WebSocketRepository.MicrophoneControlListener>(relaxed = true)

        repository.connect("ws://test.com", "token", "channel1", listener)
        repository.updateChannel("token", "channel2")

        verify { webSocketDataSource.connect("ws://test.com", "token", "channel1", any()) }
        verify { webSocketDataSource.updateChannel("token", "channel2") }
    }

    @Test
    fun `isConnected before and after connect should work`() {
        every { webSocketDataSource.isConnected() } returns false andThen true
        val listener = mockk<WebSocketRepository.MicrophoneControlListener>(relaxed = true)

        val beforeConnect = repository.isConnected()
        repository.connect("ws://test.com", "token", "channel", listener)
        val afterConnect = repository.isConnected()

        assertThat(beforeConnect).isFalse()
        assertThat(afterConnect).isTrue()
    }

    @Test
    fun `listener events can be triggered multiple times`() {
        val callbackSlot = slot<WebSocketDataSource.MicrophoneControlListener>()
        var micStartCount = 0
        var micStopCount = 0

        every {
            webSocketDataSource.connect(any(), any(), any(), capture(callbackSlot))
        } just Runs

        val listener = object : WebSocketRepository.MicrophoneControlListener {
            override fun onMicrophoneStart() { micStartCount++ }
            override fun onMicrophoneStop() { micStopCount++ }
            override fun onConnectionEstablished() {}
            override fun onConnectionClosed() {}
            override fun onError(error: String) {}
        }

        repository.connect("ws://test.com", "token", "channel", listener)

        // Trigger events multiple times
        callbackSlot.captured.onMicrophoneStart()
        callbackSlot.captured.onMicrophoneStart()
        callbackSlot.captured.onMicrophoneStop()
        callbackSlot.captured.onMicrophoneStart()
        callbackSlot.captured.onMicrophoneStop()
        callbackSlot.captured.onMicrophoneStop()

        assertThat(micStartCount).isEqualTo(3)
        assertThat(micStopCount).isEqualTo(3)
    }
}
