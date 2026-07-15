package com.example.vantaread.data.repository

import android.util.Log
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
    private companion object {
        const val TAG = "AuthRepository"
    }

    val auth = firebaseServices.auth
    val isConfigured: Boolean = firebaseServices.isAuthConfigured
    val configInfo = firebaseServices.configInfo

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth?.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            Log.d(TAG, "Auth state changed: user=${user?.uid}")
            _currentUser.value = user
        }
    }

    suspend fun signIn(email: String, pass: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "signIn with email: $email")
            val result = requireAuth().signInWithEmailAndPassword(email, pass).await()
            val user = result.user ?: throw Exception("User is null after sign in")
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "signIn error", e)
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, pass: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "signUp with email: $email")
            val result = requireAuth().createUserWithEmailAndPassword(email, pass).await()
            val user = result.user ?: throw Exception("User is null after sign up")
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "signUp error", e)
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            Log.d(TAG, "sendPasswordReset for email: $email")
            requireAuth().sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "sendPasswordReset error", e)
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "signInWithCredential")
            val result = requireAuth().signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("User is null after sign in with credential")
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "signInWithCredential error", e)
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    fun signOut() {
        Log.d(TAG, "signOut")
        auth?.signOut()
        _currentUser.value = null
    }

    fun requireAuth() = requireNotNull(auth) {
        "Account features are unavailable because Firebase is not configured in this build."
    }
}
