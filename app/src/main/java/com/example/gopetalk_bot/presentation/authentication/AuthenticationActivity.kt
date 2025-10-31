package com.example.gopetalk_bot.presentation.authentication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.gopetalk_bot.data.datasources.local.UserPreferences
import com.example.gopetalk_bot.di.ServiceLocator
import com.example.gopetalk_bot.presentation.main.MainActivity
import com.example.gopetalk_bot.ui.theme.GopeTalk_BotTheme

class AuthenticationActivity : ComponentActivity(), AuthenticationContract.View {

    private var presenter: AuthenticationContract.Presenter? = null

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        presenter?.onPermissionsResult(permissions.entries.all { it.value })
    }

    override val context: Context
        get() = this

    companion object {
        private const val TAG = "AuthenticationActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userPreferences = UserPreferences(this)
        
        
        if (userPreferences.hasActiveSession()) {
            logInfo("Active session found, navigating to MainActivity")
            navigateToMainActivity()
            finish()
            return
        }

        presenter = ServiceLocator.provideAuthenticationPresenter(this)

        presenter?.onViewCreated()

        setContent {
            GopeTalk_BotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuthenticationScreen()
                }
            }
        }
    }

    override fun logInfo(message: String) {
        Log.i(TAG, message)
    }

    override fun logError(message: String, t: Throwable?) {
        if (t != null) {
            Log.e(TAG, message, t)
        } else {
            Log.e(TAG, message)
        }
    }

    override fun navigateToMainActivity() {
        logInfo("Authentication successful, navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun showAuthenticationError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun requestPermissions(permissions: Array<String>) {
        requestMultiplePermissionsLauncher.launch(permissions)
    }

    override fun showPermissionsRequiredError() {
        Toast.makeText(
            this,
            "Se requieren permisos de micrófono para continuar",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.stop()
    }
}
