package com.aura.glasschat.security

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide Lifecycle and Security Controller.
 * Ensures the app locks on background timeout and handles secure window flags.
 */
class AppLockManager private constructor(private val context: Context) : DefaultLifecycleObserver {

    val pinManager: PrivacyPinManager = PrivacyPinManager(context)

    private val _isAppLocked = MutableStateFlow(pinManager.hasPin())
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private var lastBackgroundTimestamp: Long = 0L

    fun initialize() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        // Check initial state
        if (pinManager.hasPin()) {
            _isAppLocked.value = true
        } else {
            _isAppLocked.value = false
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (!pinManager.hasPin()) {
            _isAppLocked.value = false
            return
        }

        if (lastBackgroundTimestamp > 0L) {
            val backgroundDuration = System.currentTimeMillis() - lastBackgroundTimestamp
            val timeoutThreshold = pinManager.getLockTimeout().durationMs
            if (backgroundDuration >= timeoutThreshold) {
                _isAppLocked.value = true
            }
        } else {
            // Cold start with existing PIN
            _isAppLocked.value = true
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun unlockApp() {
        _isAppLocked.value = false
        lastBackgroundTimestamp = 0L
    }

    fun lockAppManually() {
        if (pinManager.hasPin()) {
            _isAppLocked.value = true
        }
    }

    /**
     * Secures the Window against screenshots, screen recordings,
     * and recents preview exposure.
     */
    fun applySecureWindow(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    fun clearSecureWindow(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    companion object {
        @Volatile
        private var instance: AppLockManager? = null

        fun getInstance(context: Context): AppLockManager {
            return instance ?: synchronized(this) {
                instance ?: AppLockManager(context.applicationContext).also {
                    instance = it
                    it.initialize()
                }
            }
        }
    }
}
