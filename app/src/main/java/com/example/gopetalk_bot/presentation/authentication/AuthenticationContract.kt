package com.example.gopetalk_bot.presentation.authentication

import android.content.Context

interface AuthenticationContract {
    interface View {
        val context: Context
        fun logInfo(message: String)
        fun logError(message: String, t: Throwable? = null)
        fun navigateToMainActivity()
        fun showAuthenticationError(message: String)
        fun requestPermissions(permissions: Array<String>)
        fun showPermissionsRequiredError()
    }

    interface Presenter {
        fun start()
        fun stop()
        fun onViewCreated()
        fun onPermissionsResult(allGranted: Boolean)
    }
}
