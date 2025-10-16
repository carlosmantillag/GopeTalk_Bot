package com.example.gopetalk_bot.domain.repositories

import com.example.gopetalk_bot.domain.entities.PermissionStatus

/**
 * Repository interface for permission operations
 */
interface PermissionRepository {
    /**
     * Get required permissions for the app
     */
    fun getRequiredPermissions(): List<String>
    
    /**
     * Check if all required permissions are granted
     */
    fun areAllPermissionsGranted(): Boolean
    
    /**
     * Get current permission status
     */
    fun getPermissionStatus(): PermissionStatus
}
