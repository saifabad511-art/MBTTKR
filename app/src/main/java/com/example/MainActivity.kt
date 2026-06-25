package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MubtakirApp
import com.example.ui.MubtakirViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.util.NotificationHelper
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) {
      // Permission granted! Trigger a goals reminder notification
      NotificationHelper.sendDailyGoalsNotification(this)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Create the notification channel
    NotificationHelper.createNotificationChannel(this)

    // Check and request permission for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      // For older versions, trigger notification directly when opening
      NotificationHelper.sendDailyGoalsNotification(this)
    }

    setContent {
      val viewModel: MubtakirViewModel = viewModel()
      val isDarkModeOpt by viewModel.isDarkMode.collectAsState()
      val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
      val useDarkTheme = isDarkModeOpt ?: systemInDark

      MyApplicationTheme(darkTheme = useDarkTheme) {
        MubtakirApp(viewModel = viewModel)
      }
    }
  }
}
