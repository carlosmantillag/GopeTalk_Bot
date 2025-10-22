package com.example.gopetalk_bot.domain.entities

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.io.IOException

class ApiResponseTest {

    @Test
    fun `Success response should contain correct data`() {
        val audioFile = File("test.mp3")
        val response = ApiResponse.Success(200, "Success message", audioFile)

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body).isEqualTo("Success message")
        assertThat(response.audioFile).isEqualTo(audioFile)
    }

    @Test
    fun `Success response without audio file should work`() {
        val response = ApiResponse.Success(200, "Success", null)

        assertThat(response.statusCode).isEqualTo(200)
        assertThat(response.body).isEqualTo("Success")
        assertThat(response.audioFile).isNull()
    }

    @Test
    fun `Error response should contain message and exception`() {
        val exception = IOException("Network error")
        val response = ApiResponse.Error("Failed to connect", 500, exception)

        assertThat(response.message).isEqualTo("Failed to connect")
        assertThat(response.statusCode).isEqualTo(500)
        assertThat(response.exception).isEqualTo(exception)
    }

    @Test
    fun `Error response without exception should work`() {
        val response = ApiResponse.Error("Unknown error", null, null)

        assertThat(response.message).isEqualTo("Unknown error")
        assertThat(response.statusCode).isNull()
        assertThat(response.exception).isNull()
    }

    @Test
    fun `Success responses with same data should be equal`() {
        val response1 = ApiResponse.Success(200, "OK", null)
        val response2 = ApiResponse.Success(200, "OK", null)

        assertThat(response1).isEqualTo(response2)
    }

    @Test
    fun `Error responses with same data should be equal`() {
        val exception = IOException("Error")
        val response1 = ApiResponse.Error("Failed", 500, exception)
        val response2 = ApiResponse.Error("Failed", 500, exception)

        assertThat(response1).isEqualTo(response2)
    }

    @Test
    fun `Success and Error responses should not be equal`() {
        val success = ApiResponse.Success(200, "OK", null)
        val error = ApiResponse.Error("Failed", null, null)

        assertThat(success).isNotEqualTo(error)
    }

    @Test
    fun `Success response with different status codes should not be equal`() {
        val response1 = ApiResponse.Success(200, "OK", null)
        val response2 = ApiResponse.Success(201, "OK", null)

        assertThat(response1).isNotEqualTo(response2)
    }

    @Test
    fun `Success response toString should contain data`() {
        val response = ApiResponse.Success(200, "Success", null)
        val string = response.toString()

        assertThat(string).contains("200")
        assertThat(string).contains("Success")
    }

    @Test
    fun `Error response toString should contain message`() {
        val response = ApiResponse.Error("Network error", null, null)
        val string = response.toString()

        assertThat(string).contains("Network error")
    }

    @Test
    fun `Error response with status code should work`() {
        val response = ApiResponse.Error("Server error", 500, null)

        assertThat(response.message).isEqualTo("Server error")
        assertThat(response.statusCode).isEqualTo(500)
        assertThat(response.exception).isNull()
    }

    @Test
    fun `Error response with all parameters should work`() {
        val exception = IOException("Connection failed")
        val response = ApiResponse.Error("Network error", 503, exception)

        assertThat(response.message).isEqualTo("Network error")
        assertThat(response.statusCode).isEqualTo(503)
        assertThat(response.exception).isEqualTo(exception)
    }

    @Test
    fun `Success response copy should create new instance`() {
        val original = ApiResponse.Success(200, "OK", null)
        val copied = original.copy(statusCode = 201)

        assertThat(original.statusCode).isEqualTo(200)
        assertThat(copied.statusCode).isEqualTo(201)
    }

    @Test
    fun `Error response copy should create new instance`() {
        val original = ApiResponse.Error("Error 1", 500, null)
        val copied = original.copy(message = "Error 2")

        assertThat(original.message).isEqualTo("Error 1")
        assertThat(copied.message).isEqualTo("Error 2")
    }

    @Test
    fun `Success response hashCode should be consistent`() {
        val response1 = ApiResponse.Success(200, "OK", null)
        val response2 = ApiResponse.Success(200, "OK", null)

        assertThat(response1.hashCode()).isEqualTo(response2.hashCode())
    }

    @Test
    fun `Error response hashCode should be consistent`() {
        val exception = IOException("Error")
        val response1 = ApiResponse.Error("Failed", 500, exception)
        val response2 = ApiResponse.Error("Failed", 500, exception)

        assertThat(response1.hashCode()).isEqualTo(response2.hashCode())
    }

    @Test
    fun `Success response with empty body should work`() {
        val response = ApiResponse.Success(204, "", null)

        assertThat(response.statusCode).isEqualTo(204)
        assertThat(response.body).isEmpty()
    }

    @Test
    fun `Error response with empty message should work`() {
        val response = ApiResponse.Error("", null, null)

        assertThat(response.message).isEmpty()
    }

    @Test
    fun `Success response with very long body should work`() {
        val longBody = "a".repeat(10000)
        val response = ApiResponse.Success(200, longBody, null)

        assertThat(response.body).hasLength(10000)
    }
}
