package com.example.gopetalk_bot.main

interface MainContract {
    interface View {
        fun areAllPermissionsGranted(): Boolean
        fun requestPermissions()
        fun showPermissionsRequiredError()
        fun startVoiceService()
    }

    interface Presenter {
        fun onViewCreated()
        fun onPermissionsResult(allGranted: Boolean)
    }
}