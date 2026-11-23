package com.blockphonespam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blockphonespam.service.KeepAliveForegroundService
import com.blockphonespam.ui.MainScreen
import com.blockphonespam.ui.MainViewModel
import com.blockphonespam.ui.theme.BlockPhoneSpamTheme
import com.blockphonespam.util.OemAutoStartHelper
import com.blockphonespam.util.PermissionHelper
import com.blockphonespam.worker.KeepAliveWorker

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as BlockPhoneSpamApp).repository)
    }

    private val callScreeningRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemState(this)
    }

    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemState(this)
    }

    private val runtimePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        startProtectionServices()
        viewModel.refreshSystemState(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        startProtectionServices()
        requestInitialPermissionsIfNeeded()

        setContent {
            BlockPhoneSpamTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                MainScreen(
                    state = state,
                    onTogglePrefix = viewModel::onTogglePrefix,
                    onNewPrefixChange = viewModel::onNewPrefixInputChange,
                    onAddPrefix = viewModel::onAddPrefix,
                    onDeletePrefix = viewModel::onDeletePrefix,
                    onClearBlockedCalls = viewModel::onClearBlockedCalls,
                    onRequestCallScreeningRole = ::requestCallScreeningRole,
                    onRequestBatteryOptimization = ::requestBatteryOptimizationExemption,
                    onRequestRuntimePermissions = ::requestRuntimePermissions,
                    onOpenAutoStartSettings = { OemAutoStartHelper.openAutoStartSettings(this) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startProtectionServices()
        viewModel.refreshSystemState(this)
    }

    private fun startProtectionServices() {
        KeepAliveForegroundService.start(this)
        KeepAliveWorker.schedule(this)
    }

    private fun requestInitialPermissionsIfNeeded() {
        val state = PermissionHelper.checkAll(this)
        if (!state.hasPhonePermissions || !state.hasNotificationPermission) {
            requestRuntimePermissions()
        } else if (!state.hasCallScreeningRole) {
            requestCallScreeningRole()
        }
    }

    private fun requestCallScreeningRole() {
        val intent = PermissionHelper.createCallScreeningRoleIntent(this)
        if (intent != null) {
            callScreeningRoleLauncher.launch(intent)
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (!PermissionHelper.isIgnoringBatteryOptimizations(this)) {
            try {
                batteryOptimizationLauncher.launch(
                    PermissionHelper.createBatteryOptimizationIntent(this)
                )
            } catch (_: Exception) {
                startActivity(PermissionHelper.createAppDetailsSettingsIntent(this))
            }
        }
    }

    private fun requestRuntimePermissions() {
        runtimePermissionsLauncher.launch(PermissionHelper.requiredRuntimePermissions())
    }
}
