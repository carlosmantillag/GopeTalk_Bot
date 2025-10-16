package com.example.gopetalk_bot.presentation.main

/**
 * Contract for Main screen (MVP pattern)
 */
interface MainContract {
    interface View {
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
