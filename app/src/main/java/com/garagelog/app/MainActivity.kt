package com.garagelog.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.garagelog.app.ui.GarageRootScreen
import com.garagelog.app.ui.GarageViewModel
import com.garagelog.app.widget.GarageWidgetProvider

class MainActivity : ComponentActivity() {

    private var openMileageSignal by mutableIntStateOf(0)
    private var widgetVehicleId by mutableStateOf<Long?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        handleWidgetIntent(intent)

        setContent {
            MaterialTheme {
                val vm: GarageViewModel = viewModel()
                GarageRootScreen(
                    viewModel = vm,
                    openMileageSignal = openMileageSignal,
                    widgetVehicleId = widgetVehicleId
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(GarageWidgetProvider.EXTRA_OPEN_MILEAGE, false) == true) {
            val vId = intent.getLongExtra(GarageWidgetProvider.EXTRA_VEHICLE_ID, -1L)
            widgetVehicleId = if (vId > 0) vId else null
            openMileageSignal++
            intent.removeExtra(GarageWidgetProvider.EXTRA_OPEN_MILEAGE)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
