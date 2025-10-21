package com.example.gopetalk_bot.presentation.authentication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gopetalk_bot.data.datasources.local.TextToSpeechDataSource
import com.example.gopetalk_bot.data.datasources.local.PermissionDataSource
import com.example.gopetalk_bot.data.datasources.remote.RemoteDataSource
import com.example.gopetalk_bot.data.repositories.ApiRepositoryImpl
import com.example.gopetalk_bot.data.repositories.PermissionRepositoryImpl
import com.example.gopetalk_bot.data.repositories.TextToSpeechRepositoryImpl
import com.example.gopetalk_bot.domain.usecases.CheckPermissionsUseCase
import com.example.gopetalk_bot.domain.usecases.SendAuthenticationUseCase
import com.example.gopetalk_bot.domain.usecases.SetTtsListenerUseCase
import com.example.gopetalk_bot.domain.usecases.ShutdownTtsUseCase
import com.example.gopetalk_bot.domain.usecases.SpeakTextUseCase
import com.example.gopetalk_bot.presentation.main.MainActivity
import com.example.gopetalk_bot.ui.theme.GopeTalk_BotTheme
import kotlin.random.Random

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

        val userPreferences = com.example.gopetalk_bot.data.datasources.local.UserPreferences(this)
        
        // Verificar si ya hay una sesión activa
        if (userPreferences.hasActiveSession()) {
            logInfo("Active session found, navigating to MainActivity")
            navigateToMainActivity()
            finish()
            return
        }
        
        val speechRecognizerDataSource = com.example.gopetalk_bot.data.datasources.local.SpeechRecognizerDataSource(this)
        val permissionDataSource = PermissionDataSource(this)
        val permissionRepository = PermissionRepositoryImpl(permissionDataSource)
        val checkPermissionsUseCase = CheckPermissionsUseCase(permissionRepository)
        
        val remoteDataSource = RemoteDataSource()
        val apiRepository = ApiRepositoryImpl(remoteDataSource, userPreferences)

        val ttsDataSource = TextToSpeechDataSource(this) { error ->
            logError("TTS Error: $error")
        }
        val ttsRepository = TextToSpeechRepositoryImpl(ttsDataSource)

        val sendAuthenticationUseCase = SendAuthenticationUseCase(apiRepository)
        val speakTextUseCase = SpeakTextUseCase(ttsRepository)
        val setTtsListenerUseCase = SetTtsListenerUseCase(ttsRepository)
        val shutdownTtsUseCase = ShutdownTtsUseCase(ttsRepository)

        presenter = AuthenticationPresenter(
            view = this,
            speechRecognizerDataSource = speechRecognizerDataSource,
            sendAuthenticationUseCase = sendAuthenticationUseCase,
            speakTextUseCase = speakTextUseCase,
            setTtsListenerUseCase = setTtsListenerUseCase,
            shutdownTtsUseCase = shutdownTtsUseCase,
            userPreferences = userPreferences,
            checkPermissionsUseCase = checkPermissionsUseCase
        )

        presenter?.onViewCreated()

        setContent {
            GopeTalk_BotTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF2E0C43),
                                        Color(0xFF0E4377)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        repeat(50) { index ->
                            val infiniteTransition =
                                rememberInfiniteTransition(label = "particle_animation_$index")

                            val initialX =
                                remember { (Random.nextFloat() - 0.5f) * 400 }
                            val duration = remember { Random.nextInt(4000, 10000) }

                            val animatedAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.1f,
                                targetValue = 0.7f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        durationMillis = duration / 2,
                                        easing = LinearEasing
                                    ),
                                    repeatMode = RepeatMode.Reverse
                                ), label = "particle_alpha_$index"
                            )

                            val animatedY by infiniteTransition.animateFloat(
                                initialValue = 400f,
                                targetValue = -400f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(duration, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ), label = "particle_y_$index"
                            )

                            Box(
                                modifier = Modifier
                                    .offset(x = initialX.dp, y = animatedY.dp)
                                    .size(2.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = animatedAlpha))
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(CircleShape)
                                    .background(color = Color.Black)
                            )
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Text(
                                text = "Autenticación",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                            
                            Text(
                                text = "Escuchando...",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
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
