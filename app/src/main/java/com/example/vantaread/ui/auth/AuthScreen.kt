package com.example.vantaread.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onNavigateBack: () -> Unit,
    onAuthenticated: () -> Unit = onNavigateBack,
    onContinueAsGuest: () -> Unit = onNavigateBack,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(value = false) }
    var isSignUp by remember { mutableStateOf(value = false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Email, 1: Phone

    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    val context = LocalContext.current
    val googleWebClientId = remember(context) {
        context.resources
            .getIdentifier("default_web_client_id", "string", context.packageName)
            .takeIf { it != 0 }
            ?.let { context.getString(it) }
            ?.takeIf { it.isNotBlank() }
    }
    
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            viewModel.signInWithGoogleAccount(task.getResult(ApiException::class.java))
        } catch (e: ApiException) {
            viewModel.showMessage(e.message ?: "Google sign-in was cancelled or failed.")
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onAuthenticated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Account") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (currentUser != null) {
                // Signed in state (already handled by LaunchedEffect, but good for safety)
                Text("Signed in", style = MaterialTheme.typography.headlineSmall)
                Text(
                    currentUser?.displayName ?: currentUser?.email ?: currentUser?.phoneNumber ?: "VantaRead account",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign Out")
                }
            } else {
                Text(
                    text = if (isSignUp) "Create Account" else "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isSignUp) "Sign up to sync your bookmarks across devices" else "Sign in to access your cloud-synced library",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (!uiState.isFirebaseConfigured) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Firebase Not Configured",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Account features require a local app/google-services.json file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            
                            TextButton(
                                onClick = { expanded = !expanded },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    if (expanded) "Hide Diagnosis" else "Show Diagnosis",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            
                            if (expanded) {
                                uiState.configInfo?.let { info ->
                                    DiagnosisRow("google-services plugin", info.hasGoogleServicesJson)
                                    DiagnosisRow("App ID resource", info.hasGoogleAppId)
                                    DiagnosisRow("API Key resource", info.hasApiKey)
                                    DiagnosisRow("Google Sign-In ready", info.hasWebClientId)
                                    DiagnosisRow("Firestore ready", info.hasProjectId)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Package: ${info.packageName}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    info.appIdPreview?.let {
                                        Text(
                                            "App ID: $it",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Note: If you just added the file, click 'Sync Project with Gradle Files' and 'Rebuild Project'.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; viewModel.clearMessage() },
                        text = { Text("Email") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; viewModel.clearMessage() },
                        text = { Text("Phone") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                uiState.error?.let { message ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                if (selectedTab == 0) {
                    // Email Auth
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = null)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.submitEmailPassword(email, password, isSignUp) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading && uiState.isFirebaseConfigured
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(if (isSignUp) "Create Account" else "Sign In")
                        }
                    }
                } else {
                    // Phone Auth
                    if (uiState.isCodeSent) {
                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = { verificationCode = it },
                            label = { Text("6-digit Verification Code") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.verifyCode(verificationCode) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading && verificationCode.length == 6
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Verify & Continue")
                            }
                        }
                        TextButton(onClick = { viewModel.cancelPhoneVerification() }) {
                            Text("Use a different number")
                        }
                    } else {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number (with country code)") },
                            placeholder = { Text("+91 9876543210") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) viewModel.verifyPhoneNumber(phoneNumber, activity)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading && uiState.isFirebaseConfigured
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Send Verification Code")
                            }
                        }
                    }
                }

                if (selectedTab == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { isSignUp = !isSignUp; viewModel.clearMessage() }) {
                            Text(if (isSignUp) "Already have an account?" else "Create new account")
                        }
                        if (!isSignUp) {
                            TextButton(onClick = { viewModel.resetPassword(email) }) {
                                Text("Forgot password?")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                // Third-party Auth
                HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp))
                
                OutlinedButton(
                    onClick = {
                        if (googleWebClientId == null) {
                            viewModel.showMessage("Google sign-in client ID not found in resources.")
                        } else {
                            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(googleWebClientId)
                                .requestEmail()
                                .build()
                            val client = GoogleSignIn.getClient(context, options)
                            googleLauncher.launch(client.signInIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && uiState.isFirebaseConfigured
                ) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue with Google")
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = {
                    Log.d("AuthScreen", "Continue as Guest clicked")
                    onContinueAsGuest()
                }) {
                    Text("Continue without account (Guest Mode)")
                }
            }
        }
    }
}

@Composable
private fun DiagnosisRow(label: String, success: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            if (success) "FOUND" else "MISSING",
            style = MaterialTheme.typography.labelSmall,
            color = if (success) Color(0xFF4CAF50) else Color(0xFFF44336),
            fontWeight = FontWeight.Bold
        )
    }
}
