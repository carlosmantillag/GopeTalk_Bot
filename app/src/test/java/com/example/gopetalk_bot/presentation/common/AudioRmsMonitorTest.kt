package com.example.gopetalk_bot.presentation.common

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioRmsMonitorTest {

    @Before
    fun setup() {
        AudioRmsMonitor.reset()
    }

    @Test
    fun `initial rmsDb should be zero`() = runTest {
        val initialValue = AudioRmsMonitor.rmsDbFlow.first()
        
        assertThat(initialValue).isEqualTo(0.0f)
    }

    @Test
    fun `updateRmsDb should update the flow value`() = runTest {
        AudioRmsMonitor.updateRmsDb(50.0f)
        
        val value = AudioRmsMonitor.rmsDbFlow.first()
        assertThat(value).isEqualTo(50.0f)
    }

    @Test
    fun `updateRmsDb with negative value should work`() = runTest {
        AudioRmsMonitor.updateRmsDb(-10.0f)
        
        val value = AudioRmsMonitor.rmsDbFlow.first()
        assertThat(value).isEqualTo(-10.0f)
    }

    @Test
    fun `updateRmsDb with zero should work`() = runTest {
        AudioRmsMonitor.updateRmsDb(0.0f)
        
        val value = AudioRmsMonitor.rmsDbFlow.first()
        assertThat(value).isEqualTo(0.0f)
    }

    @Test
    fun `updateRmsDb with high value should work`() = runTest {
        AudioRmsMonitor.updateRmsDb(100.0f)
        
        val value = AudioRmsMonitor.rmsDbFlow.first()
        assertThat(value).isEqualTo(100.0f)
    }

    @Test
    fun `multiple updates should use latest value`() = runTest {
        AudioRmsMonitor.updateRmsDb(10.0f)
        AudioRmsMonitor.updateRmsDb(20.0f)
        AudioRmsMonitor.updateRmsDb(30.0f)
        
        val value = AudioRmsMonitor.rmsDbFlow.first()
        assertThat(value).isEqualTo(30.0f)
    }

    @Test
    fun `updateRmsDb with decimal values should work`() = runTest {
        AudioRmsMonitor.updateRmsDb(45.67f)
        
        val value = AudioRmsMonitor.rmsDbFlow.first()
        assertThat(value).isWithin(0.01f).of(45.67f)
    }
}
