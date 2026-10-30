package com.metroplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.metroplayer.ui.MetroRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val app = application as MetroApp

        setContent {
            val ctx = LocalContext.current
            val audioPerm = if (Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            fun has(p: String) =
                ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

            var granted by remember { mutableStateOf(has(audioPerm)) }

            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { granted = has(audioPerm) }

            val request: () -> Unit = {
                val missing = mutableListOf<String>()
                if (!has(audioPerm)) missing.add(audioPerm)
                if (Build.VERSION.SDK_INT >= 33 && !has(Manifest.permission.POST_NOTIFICATIONS)) {
                    missing.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (missing.isNotEmpty()) launcher.launch(missing.toTypedArray())
            }

            LaunchedEffect(Unit) { request() }

            MetroRoot(app, granted, request)
        }
    }
}
