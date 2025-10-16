package com.example.gopetalk_bot.domain.entities

/**
 * Entity representing permission status
 */
data class PermissionStatus(
    val permissions: List<String>,
    val allGranted: Boolean
)
