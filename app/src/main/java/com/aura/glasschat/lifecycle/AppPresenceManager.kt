package com.aura.glasschat.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aura.glasschat.data.repository.AuthRepository
import com.aura.glasschat.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppPresenceManager(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        val uid = authRepository.currentUserId
        if (uid.isNotEmpty()) {
            scope.launch {
                userRepository.updatePresence(uid, isOnline = true)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        val uid = authRepository.currentUserId
        if (uid.isNotEmpty()) {
            scope.launch {
                userRepository.updatePresence(uid, isOnline = false)
            }
        }
    }
}
