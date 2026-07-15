package com.example.vantaread.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.repository.AuthRepository
import com.example.vantaread.data.repository.CloudSyncRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudSyncRepository: CloudSyncRepository
) : ViewModel() {

    val currentUser = authRepository.currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isCodeSent = MutableStateFlow(false)
    val isCodeSent: StateFlow<Boolean> = _isCodeSent.asStateFlow()

    private var verificationId: String? = null

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.signIn(email, pass)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Sign in failed"
            }
            _isLoading.value = false
        }
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authRepository.signUp(email, pass)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Sign up failed"
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun syncBookmarks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                cloudSyncRepository.syncBookmarksToCloud()
                _error.value = "Bookmarks synced successfully!"
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to sync bookmarks"
            }
            _isLoading.value = false
        }
    }

    fun syncBookmarksFromCloud() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                cloudSyncRepository.syncBookmarksFromCloud()
                _error.value = "Bookmarks restored successfully!"
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to restore bookmarks"
            }
            _isLoading.value = false
        }
    }

    fun verifyPhoneNumber(phoneNumber: String, activity: Activity) {
        _isLoading.value = true
        _error.value = null
        
        val options = PhoneAuthOptions.newBuilder(authRepository.auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    _isLoading.value = false
                    _error.value = e.message ?: "Verification failed"
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@AuthViewModel.verificationId = verificationId
                    _isLoading.value = false
                    _isCodeSent.value = true
                }
            })
            .build()
            
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyCode(code: String) {
        val vid = verificationId ?: return
        val credential = PhoneAuthProvider.getCredential(vid, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                authRepository.auth.signInWithCredential(credential).await()
                // Note: AuthRepository listens to auth state changes, so it will update currentUser automatically
                _isCodeSent.value = false
            } catch (e: Exception) {
                _error.value = e.message ?: "Phone sign-in failed"
            }
            _isLoading.value = false
        }
    }
}
