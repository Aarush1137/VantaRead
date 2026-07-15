package com.example.vantaread.ui.auth

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vantaread.data.repository.AuthRepository
import com.example.vantaread.data.repository.CloudSyncRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudSyncRepository: CloudSyncRepository,
) : ViewModel() {

    val currentUser = authRepository.currentUser
    val isFirebaseConfigured = authRepository.isConfigured

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isCodeSent = MutableStateFlow(false)
    val isCodeSent: StateFlow<Boolean> = _isCodeSent.asStateFlow()

    private var verificationId: String? = null
    private var phoneVerificationTimeoutJob: Job? = null

    fun showMessage(message: String) {
        _isLoading.value = false
        _error.value = message
    }

    fun clearMessage() {
        _error.value = null
    }

    fun submitEmailPassword(email: String, password: String, isSignUp: Boolean) {
        val normalizedEmail = email.trim()
        when {
            !EMAIL_PATTERN.matches(normalizedEmail) -> {
                _error.value = "Enter a valid email address."
                return
            }
            password.length < MINIMUM_PASSWORD_LENGTH -> {
                _error.value = "Password must contain at least $MINIMUM_PASSWORD_LENGTH characters."
                return
            }
        }

        if (isSignUp) signUp(normalizedEmail, password) else signIn(normalizedEmail, password)
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Sign in started for $email")
            _isLoading.value = true
            _error.value = null
            try {
                val result = withTimeout(AUTH_TIMEOUT_MS) {
                    authRepository.signIn(email, pass)
                }
                if (result.isFailure) {
                    Log.w("AuthViewModel", "Sign in failed: ${result.exceptionOrNull()?.message}")
                    _error.value = result.exceptionOrNull()?.message ?: "Sign in failed"
                } else {
                    Log.d("AuthViewModel", "Sign in successful")
                }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("AuthViewModel", "Sign in timed out after ${AUTH_TIMEOUT_MS}ms")
                _error.value = "Sign in timed out. This may be due to a poor internet connection or a temporary issue with our authentication servers. Please try again in a moment."
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign in exception", e)
                _error.value = e.message ?: "Sign in failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Sign up started for $email")
            _isLoading.value = true
            _error.value = null
            try {
                val result = withTimeout(AUTH_TIMEOUT_MS) {
                    authRepository.signUp(email, pass)
                }
                if (result.isFailure) {
                    Log.w("AuthViewModel", "Sign up failed: ${result.exceptionOrNull()?.message}")
                    _error.value = result.exceptionOrNull()?.message ?: "Sign up failed"
                } else {
                    Log.d("AuthViewModel", "Sign up successful")
                }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("AuthViewModel", "Sign up timed out after ${AUTH_TIMEOUT_MS}ms")
                _error.value = "Sign up timed out. This may be due to a poor internet connection or a temporary issue with our authentication servers. Please try again in a moment."
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign up exception", e)
                _error.value = e.message ?: "Sign up failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun signInWithGoogleAccount(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Google sign in started")
            _isLoading.value = true
            _error.value = null
            try {
                withTimeout(AUTH_TIMEOUT_MS) {
                    val idToken = account?.idToken
                    if (idToken.isNullOrBlank()) {
                        Log.e("AuthViewModel", "Google sign in failed: Missing ID token")
                        _error.value = "Google sign-in is missing an ID token. Check the local web client ID."
                    } else {
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        val result = authRepository.signInWithCredential(credential)
                        if (result.isFailure) {
                            Log.w("AuthViewModel", "Google sign in failed: ${result.exceptionOrNull()?.message}")
                            _error.value = result.exceptionOrNull()?.message ?: "Google sign-in failed"
                        } else {
                            Log.d("AuthViewModel", "Google sign in successful")
                        }
                    }
                }
            } catch (e: ApiException) {
                Log.e("AuthViewModel", "Google sign in ApiException", e)
                _error.value = e.message ?: "Google sign-in failed"
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("AuthViewModel", "Google sign in timed out after ${AUTH_TIMEOUT_MS}ms")
                _error.value = "Google sign-in timed out. This may be due to a poor internet connection or a temporary issue with our authentication servers. Please try again in a moment."
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign in exception", e)
                _error.value = e.message ?: "Google sign-in failed"
            } finally {
                _isLoading.value = false
            }
        }
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
        val normalizedPhone = phoneNumber.trim()
        if (!PHONE_PATTERN.matches(normalizedPhone)) {
            _error.value = "Enter a phone number with country code, for example +919876543210."
            return
        }

        Log.d("AuthViewModel", "Phone verification started for $normalizedPhone")
        _isLoading.value = true
        _error.value = null
        phoneVerificationTimeoutJob?.cancel()

        val auth = authRepository.auth
        if (auth == null) {
            _isLoading.value = false
            _error.value = "Phone sign-in is unavailable because Firebase is not configured in this build."
            return
        }

        runCatching {
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(normalizedPhone)
                .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        Log.d("AuthViewModel", "Phone verification completed automatically")
                        phoneVerificationTimeoutJob?.cancel()
                        signInWithPhoneAuthCredential(credential)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.e("AuthViewModel", "Phone verification failed", e)
                        phoneVerificationTimeoutJob?.cancel()
                        _isLoading.value = false
                        _error.value = e.message ?: "Verification failed"
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        Log.d("AuthViewModel", "Phone verification code sent")
                        phoneVerificationTimeoutJob?.cancel()
                        this@AuthViewModel.verificationId = verificationId
                        _isLoading.value = false
                        _isCodeSent.value = true
                        _error.value = "Verification code sent."
                    }
                })
                .build()
        }.onSuccess { options ->
            PhoneAuthProvider.verifyPhoneNumber(options)
            phoneVerificationTimeoutJob = viewModelScope.launch {
                delay(PHONE_VERIFICATION_TIMEOUT_MS)
                if (_isLoading.value && !_isCodeSent.value) {
                    Log.w("AuthViewModel", "Phone verification timed out")
                    _isLoading.value = false
                    _error.value = "Phone verification timed out. This could be due to network issues or your SMS quota. Please try again later."
                }
            }
        }
            .onFailure {
                Log.e("AuthViewModel", "Could not start phone verification", it)
                phoneVerificationTimeoutJob?.cancel()
                _isLoading.value = false
                _error.value = it.message ?: "Could not start phone verification."
            }
    }

    fun verifyCode(code: String) {
        val vid = verificationId
        if ((vid == null) || code.isBlank()) {
            _error.value = "Request a verification code first."
            return
        }
        val credential = PhoneAuthProvider.getCredential(vid, code)
        signInWithPhoneAuthCredential(credential)
    }

    fun cancelPhoneVerification() {
        phoneVerificationTimeoutJob?.cancel()
        verificationId = null
        _isCodeSent.value = false
        _isLoading.value = false
    }

    fun resetPassword(email: String) {
        val normalizedEmail = email.trim()
        if (!EMAIL_PATTERN.matches(normalizedEmail)) {
            _error.value = "Enter your email address first."
            return
        }

        viewModelScope.launch {
            Log.d("AuthViewModel", "Password reset requested for $normalizedEmail")
            _isLoading.value = true
            _error.value = null
            try {
                val result = withTimeout(AUTH_TIMEOUT_MS) {
                    authRepository.sendPasswordReset(normalizedEmail)
                }
                _error.value = if (result.isSuccess) {
                    Log.d("AuthViewModel", "Password reset link sent")
                    "If an account exists for this email, a reset link has been sent."
                } else {
                    Log.w("AuthViewModel", "Password reset failed: ${result.exceptionOrNull()?.message}")
                    result.exceptionOrNull()?.message ?: "Could not request a password reset."
                }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("AuthViewModel", "Password reset timed out")
                _error.value = "Password reset request timed out. Please check your connection and try again."
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Password reset exception", e)
                _error.value = e.message ?: "Could not request a password reset."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Signing in with phone credential")
            _isLoading.value = true
            _error.value = null
            try {
                val result = withTimeout(AUTH_TIMEOUT_MS) {
                    authRepository.signInWithCredential(credential)
                }
                if (result.isFailure) {
                    Log.w("AuthViewModel", "Phone sign in failed: ${result.exceptionOrNull()?.message}")
                    _error.value = result.exceptionOrNull()?.message ?: "Phone sign-in failed"
                } else {
                    Log.d("AuthViewModel", "Phone sign in successful")
                }
                // Note: AuthRepository listens to auth state changes, so it will update currentUser automatically
                _isCodeSent.value = false
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("AuthViewModel", "Phone sign in timed out")
                _error.value = "Phone sign-in timed out. Please check your connection and try again."
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Phone sign in exception", e)
                _error.value = e.message ?: "Phone sign-in failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        val PHONE_PATTERN = Regex("^\\+[1-9]\\d{7,14}$")
        const val MINIMUM_PASSWORD_LENGTH = 6
        const val AUTH_TIMEOUT_MS = 30_000L
        const val PHONE_VERIFICATION_TIMEOUT_MS = 70_000L
    }
}
