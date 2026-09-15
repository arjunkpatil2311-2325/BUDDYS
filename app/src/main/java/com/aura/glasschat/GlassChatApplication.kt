package com.aura.glasschat

import android.app.Application
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.UserRepository
import com.aura.glasschat.lifecycle.AppPresenceManager

class GlassChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize lifecycle presence tracking
        val authRepository = AuthRepository()
        val userRepository = UserRepository()
        val presenceManager = AppPresenceManager(authRepository, userRepository)
        presenceManager.initialize()
    }
}
