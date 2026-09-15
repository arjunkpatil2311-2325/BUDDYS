package com.aura.glasschat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.security.AppLockManager
import com.aura.glasschat.ui.components.UpdateDialog
import com.aura.glasschat.ui.navigation.AppNavHost
import com.aura.glasschat.ui.navigation.Screen
import com.aura.glasschat.ui.screens.PrivacyLockScreen
import com.aura.glasschat.ui.theme.GlassChatTheme
import com.aura.glasschat.ui.viewmodel.UpdateUiState
import com.aura.glasschat.ui.viewmodel.UpdateViewModel

class MainActivity : ComponentActivity() {

    private val authRepository = AuthRepository()
    private lateinit var appLockManager: AppLockManager
    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize App Security & Apply FLAG_SECURE for screenshot & recents privacy (Release only)
        appLockManager = AppLockManager.getInstance(this)
        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isDebuggable) {
            appLockManager.applySecureWindow(this)
        }

        val startDestination = if (authRepository.isUserLoggedIn) {
            Screen.Home.route
        } else {
            Screen.Auth.route
        }

        setContent {
            GlassChatTheme {
                val isAppLocked by appLockManager.isAppLocked.collectAsState()
                val updateUiState by updateViewModel.uiState.collectAsState()
                val navController = rememberNavController()

                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(
                        navController = navController,
                        startDestination = startDestination
                    )

                    if (isAppLocked && authRepository.isUserLoggedIn) {
                        PrivacyLockScreen(
                            onUnlocked = {
                                appLockManager.unlockApp()
                            }
                        )
                    }

                    // Online In-App Update Dialog
                    UpdateDialog(
                        state = updateUiState,
                        onUpdateClick = {
                            when (val state = updateUiState) {
                                is UpdateUiState.UpdateAvailable -> {
                                    updateViewModel.startDownload(state.manifest, state.isMandatory)
                                }
                                is UpdateUiState.Error -> {
                                    state.manifest?.let {
                                        updateViewModel.startDownload(it, state.isMandatory)
                                    }
                                }
                                else -> {}
                            }
                        },
                        onInstallClick = {
                            if (updateUiState is UpdateUiState.ReadyToInstall) {
                                updateViewModel.installApk((updateUiState as UpdateUiState.ReadyToInstall).apkFile)
                            }
                        },
                        onDismissClick = {
                            updateViewModel.dismiss()
                        }
                    )
                }
            }
        }
    }
}

