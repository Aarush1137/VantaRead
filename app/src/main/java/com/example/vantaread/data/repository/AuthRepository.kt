package com.example.vantaread.data.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseServices: FirebaseServices
) {
    val auth = firebaseServices.auth
    val isConfigured: Boolean = firebaseServices.isAuthConfigured

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth?.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    suspend fun signIn(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val result = requireAuth().signInWithEmailAndPassword(email, pass).await()
            val user = result.user ?: throw Exception("User is null")
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val result = requireAuth().createUserWithEmailAndPassword(email, pass).await()
            val user = result.user ?: throw Exception("User is null")
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            requireAuth().sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser> {
        return try {
            val result = requireAuth().signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("User is null")
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    fun signOut() {
        auth?.signOut()
        _currentUser.value = null
    }

    fun requireAuth() = requireNotNull(auth) {
        "Account features are unavailable because Firebase is not configured in this build."
    }
}
