package com.example.gopetalk_bot.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class MainPresenter(private val view: MainContract.View, private val context: Context) : MainContract.Presenter {

    private val permissionsToRequest by lazy {
        mutableListOf(
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    private fun areAllPermissionsGranted(): Boolean {
        return permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onViewCreated() {
        if (areAllPermissionsGranted()) {
            view.startVoiceService()
        } else {
            view.requestPermissions(permissionsToRequest)
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