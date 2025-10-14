package com.example.gopetalk_bot.main

interface MainContract {
    interface View {
        fun requestPermissions(permissions: Array<String>)
        fun showPermissionsRequiredError()
        fun startVoiceService()
    }

    interface Presenter {
        fun onViewCreated()
        fun onPermissionsResult(allGranted: Boolean)
    }
}