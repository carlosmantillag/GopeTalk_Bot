package com.example.gopetalk_bot.data.datasources.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

interface PermissionChecker {
    fun checkPermission(context: Context, permission: String): Int
    fun getSdkVersion(): Int
}

class AndroidPermissionChecker : PermissionChecker {
    override fun checkPermission(context: Context, permission: String): Int {
        return ContextCompat.checkSelfPermission(context, permission)
    }
    
    override fun getSdkVersion(): Int {
        return Build.VERSION.SDK_INT
    }
}

class PermissionDataSource(
    private val context: Context,
    private val permissionChecker: PermissionChecker = AndroidPermissionChecker()
) {
    fun getRequiredPermissions(): List<String> {
        return mutableListOf(
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (permissionChecker.getSdkVersion() >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun areAllPermissionsGranted(): Boolean {
        return getRequiredPermissions().all {
            permissionChecker.checkPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
