package com.example.gopetalk_bot.presentation.main

import android.content.Context

interface MainContract {
    interface View {
        val context: Context
        fun requestPermissions(permissions: Array<String>)
        fun showPermissionsRequiredError()
        fun startVoiceService()
        fun speakWelcomeMessage(username: String)
    }

    interface Presenter {
        fun onViewCreated()
        fun onPermissionsResult(allGranted: Boolean)
    }
}
