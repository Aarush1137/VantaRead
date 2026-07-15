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
import com.example.vantaread.data.repository.FirebaseConfigInfo
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCodeSent: Boolean = false,
    val verificationId: String? = null,
    val isFirebaseConfigured: Boolean = false,
    val configInfo: FirebaseConfigInfo? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudSyncRepository: CloudSyncRepository,
) : ViewModel() {

    val currentUser = authRepository.currentUser
    val isFirebaseConfigured = authRepository.isConfigured

    private val _uiState = MutableStateFlow(AuthUiState(
        isFirebaseConfigured = authRepository.isConfigured,
        configInfo = authRepository.configInfo
    ))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var phoneVerificationTimeoutJob: Job? = null

    fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(isLoading = false, error = message)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun submitEmailPassword(email: String, password: String, isSignUp: Boolean) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Email and password cannot be empty.")
            return
        }
        
        when {
            !EMAIL_PATTERN.matches(normalizedEmail) -> {
                _uiState.value = _uiState.value.copy(error = "Enter a valid email address.")
                return
            }
            password.length < MINIMUM_PASSWORD_LENGTH -> {
                _uiState.value = _uiState.value.copy(error = "Password must contain at least $MINIMUM_PASSWORD_LENGTH characters.")
                return
            }
        }

        if (isSignUp) signUp(normalizedEmail, password) else signIn(normalizedEmail, password)
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Sign in started for $email")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                if (!isFirebaseConfigured) {
                    throw Exception("Firebase is not configured. Add google-services.json to enable sign-in.")
                }
                
                val result = withTimeout(AUTH_TIMEOUT_MS) {
                    authRepository.signIn(email, pass)
                }
                if (result.isFailure) {
                    val message = result.exceptionOrNull()?.message ?: "Sign in failed"
                    Log.w("AuthViewModel", "Sign in failed: $message")
                    _uiState.value = _uiState.value.copy(error = message)
                } else {
                    Log.d("AuthViewModel", "Sign in successful")
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("AuthViewModel", "Sign in timed out after ${AUTH_TIMEOUT_MS}ms")
                _uiState.value = _uiState.value.copy(error = "Sign in timed out. This often happens if the Firebase configuration is missing or invalid.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign in exception", e)
                _uiState.value = _uiState.value.copy(error = e.message ?: "An unexpected error occurred.")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Sign up started for $email")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                if (!isFirebaseConfigured) {
                    throw Exception("Firebase is not configured. Add google-services.json to enable sign-up.")
                }

                val result = withTimeout(AUTH_TIMEOUT_MS) {
                    authRepository.signUp(email, pass)
                }
                if (result.isFailure) {
                    val message = result.exceptionOrNull()?.message ?: "Sign up failed"
                    Log.w("AuthViewModel", "Sign up failed: $message")
                    _uiState.value = _uiState.value.copy(error = message)
                } else {
                    Log.d("AuthViewModel", "Sign up successful")
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("AuthViewModel", "Sign up timed out after ${AUTH_TIMEOUT_MS}ms")
                _uiState.value = _uiState.value.copy(error = "Sign up timed out. This often happens if the Firebase configuration is missing or invalid.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign up exception", e)
                _uiState.value = _uiState.value.copy(error = e.message ?: "An unexpected error occurred.")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun signInWithGoogleAccount(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Google sign in started")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                if (!isFirebaseConfigured) {
                    throw Exception("Firebase is not configured. Add google-services.json.")
                }

                withTimeout(AUTH_TIMEOUT_MS) {
                    val idToken = account?.idToken
                    if (idToken.isNullOrBlank()) {
                        Log.e("AuthViewModel", "Google sign in failed: Missing ID token")
                        _uiState.value = _uiState.value.copy(error = "Google sign-in is missing an ID token. Check the local web client ID.")
                    } else {
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        val result = authRepository.signInWithCredential(credential)
                        if (result.isFailure) {
                            val message = result.exceptionOrNull()?.message ?: "Google sign-in failed"
                            Log.w("AuthViewModel", "Google sign in failed: $message")
                            _uiState.value = _uiState.value.copy(error = message)
                        } else {
                            Log.d("AuthViewModel", "Google sign in successful")
                        }
                    }
                }
            } catch (e: ApiException) {
                Log.e("AuthViewModel", "Google sign in ApiException", e)
                _uiState.value = _uiState.value.copy(error = "Google sign-in failed (Code: ${e.statusCode}). Check Firebase Console configuration.")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("AuthViewModel", "Google sign in timed out after ${AUTH_TIMEOUT_MS}ms")
                _uiState.value = _uiState.value.copy(error = "Google sign-in timed out. Check your connection or Firebase config.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign in exception", e)
                _uiState.value = _uiState.value.copy(error = e.message ?: "Google sign-in failed")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun verifyPhoneNumber(phoneNumber: String, activity: Activity) {
        val normalizedPhone = phoneNumber.trim()
        if (!PHONE_PATTERN.matches(normalizedPhone)) {
            _uiState.update { it.copy(error = "Enter a phone number with country code, for example +919876543210.") }
            return
        }

        Log.d("AuthViewModel", "Phone verification started for $normalizedPhone")
        _uiState.update { it.copy(isLoading = true, error = null) }
        phoneVerificationTimeoutJob?.cancel()

        val auth = authRepository.auth
        if (auth == null) {
            _uiState.update { it.copy(isLoading = false, error = "Phone sign-in is unavailable because Firebase is not configured.") }
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
                        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Verification failed")
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        Log.d("AuthViewModel", "Phone verification code sent")
                        phoneVerificationTimeoutJob?.cancel()
                        _uiState.value = _uiState.value.copy(isLoading = false, isCodeSent = true, verificationId = verificationId, error = "Verification code sent.")
                    }
                })
                .build()
        }.onSuccess { options ->
            PhoneAuthProvider.verifyPhoneNumber(options)
            phoneVerificationTimeoutJob = viewModelScope.launch {
                delay(PHONE_VERIFICATION_TIMEOUT_MS)
                if (_uiState.value.isLoading && !_uiState.value.isCodeSent) {
                    Log.w("AuthViewModel", "Phone verification timed out")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Phone verification timed out. This often means Firebase SMS services are not configured correctly.")
                }
            }
        }
            .onFailure { exception ->
                Log.e("AuthViewModel", "Could not start phone verification", exception)
                phoneVerificationTimeoutJob?.cancel()
                _uiState.value = _uiState.value.copy(isLoading = false, error = exception.message ?: "Could not start phone verification.")
            }
    }

    fun verifyCode(code: String) {
        val vid = _uiState.value.verificationId
        if ((vid == null) || code.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Request a verification code first.")
            return
        }
        val credential = PhoneAuthProvider.getCredential(vid, code)
        signInWithPhoneAuthCredential(credential)
    }

    fun cancelPhoneVerification() {
        phoneVerificationTimeoutJob?.cancel()
        _uiState.value = _uiState.value.copy(isCodeSent = false, verificationId = null, isLoading = false)
    }

    fun resetPassword(email: String) {
        val normalizedEmail = email.trim()
        if (!EMAIL_PATTERN.matches(normalizedEmail)) {
            _uiState.value = _uiState.value.copy(error = "Enter your email address first.")
            return
        }

        viewModelScope.launch {
            Log.d("AuthViewModel", "Password reset requested for $normalizedEmail")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                if (!isFirebaseConfigured) {
                    throw Exception("Firebase is not configured.")
                }

                val result = withTimeout(AUTH_TIMEOUT_MS) {
                    authRepository.sendPasswordReset(normalizedEmail)
                }
                val msg = if (result.isSuccess) {
                    Log.d("AuthViewModel", "Password reset link sent")
                    "If an account exists for this email, a reset link has been sent."
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Could not request a password reset."
                    Log.w("AuthViewModel", "Password reset failed: $errorMsg")
                    errorMsg
                }
                _uiState.value = _uiState.value.copy(error = msg)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("AuthViewModel", "Password reset timed out")
                _uiState.value = _uiState.value.copy(error = "Password reset request timed out.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Password reset exception", e)
                _uiState.value = _uiState.value.copy(error = e.message ?: "Could not request a password reset.")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Signing in with phone credential")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = withTimeout(AUTH_TIMEOUT_MS) {
                    authRepository.signInWithCredential(credential)
                }
                if (result.isFailure) {
                    val message = result.exceptionOrNull()?.message ?: "Phone sign-in failed"
                    Log.w("AuthViewModel", "Phone sign in failed: $message")
                    _uiState.value = _uiState.value.copy(error = message)
                } else {
                    Log.d("AuthViewModel", "Phone sign in successful")
                    _uiState.value = _uiState.value.copy(isCodeSent = false)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e("AuthViewModel", "Phone sign in timed out")
                _uiState.value = _uiState.value.copy(error = "Phone sign-in timed out. Check your connection.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Phone sign in exception", e)
                _uiState.value = _uiState.value.copy(error = e.message ?: "Phone sign-in failed")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
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
