package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ui.KoeNaWinMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.KoeNaWinViewModel
import com.example.widget.KoeNaWinWidgetProvider

class MainActivity : ComponentActivity() {

    // Notification permission request code
    private val permissionRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ask for Notification Permissions on API 33+ (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    permissionRequestCode
                )
            }
        }

        setContent {
            MyApplicationTheme {
                val viewModel: KoeNaWinViewModel = viewModel()
                
                // Live listener to database updates from widget clicking
                DisposableEffect(Unit) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            // Triggers flow updating indirectly or directly through standard broadcast
                            viewModel.triggerWidgetAndAlarmUpdate()
                        }
                    }
                    val filter = IntentFilter(KoeNaWinWidgetProvider.ACTION_DATA_CHANGED)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                    } else {
                        registerReceiver(receiver, filter)
                    }
                    
                    onDispose {
                        try {
                            unregisterReceiver(receiver)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // Initial Widget & Alarm health sync
                LaunchedEffectUnit {
                    viewModel.triggerWidgetAndAlarmUpdate()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    KoeNaWinMainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Reusable micro-helper to compile Jetpack Compose LaunchedEffect(Unit) reliably across environments
@androidx.compose.runtime.Composable
fun LaunchedEffectUnit(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit, block = block)
}
