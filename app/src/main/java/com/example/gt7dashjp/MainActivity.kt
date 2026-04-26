package com.example.gt7dashjp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.example.gt7dashjp.ui.DashboardScreen
import com.example.gt7dashjp.ui.theme.Gt7dashJPTheme
import com.example.gt7dashjp.viewmodel.TelemetryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide status bar and navigation bar (full screen)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val viewModel = ViewModelProvider(this)[TelemetryViewModel::class.java]
        setContent {
            Gt7dashJPTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}

