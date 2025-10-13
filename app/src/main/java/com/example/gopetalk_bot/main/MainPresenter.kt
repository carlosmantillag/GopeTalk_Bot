package com.example.gopetalk_bot.main

import android.Manifest
import android.os.Build
import com.example.gopetalk_bot.main.MainContract

class MainPresenter(private val view: MainContract.View) : MainContract.Presenter {

    val permissionsToRequest by lazy {
        mutableListOf(
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    override fun onViewCreated() {
        if (view.areAllPermissionsGranted()) {
            view.startVoiceService()
        } else {
            view.requestPermissions()
        }
    }

    override fun onPermissionsResult(allGranted: Boolean) {
        if (allGranted) {
            view.startVoiceService()
        } else {
            view.showPermissionsRequiredError()
        }
    }
}