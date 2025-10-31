package com.example.gopetalk_bot.data.datasources.local

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat

class AndroidPermissionChecker : PermissionChecker {
    override fun checkPermission(context: Context, permission: String): Int {
        return ContextCompat.checkSelfPermission(context, permission)
    }

    override fun getSdkVersion(): Int {
        return Build.VERSION.SDK_INT
    }
}
