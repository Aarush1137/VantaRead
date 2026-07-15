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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.app.Activity
import com.example.vantaread.R
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
    var passwordVisible by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }
    var usePhoneAuth by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isCodeSent by viewModel.isCodeSent.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    val context = LocalContext.current
    val googleWebClientId = stringResource(R.string.google_web_client_id)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (currentUser != null) {
                Text("Signed in", style = MaterialTheme.typography.headlineSmall)
                Text(
                    currentUser?.displayName ?: currentUser?.email ?: currentUser?.phoneNumber ?: "VantaRead account",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.syncBookmarks() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Sync Bookmarks to Cloud")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.syncBookmarksFromCloud() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Restore Bookmarks from Cloud")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign Out")
                }
            } else {
                Text(
                    if (usePhoneAuth) "Phone Sign In" else if (isSignUp) "Create Account" else "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        if (googleWebClientId == "YOUR_WEB_CLIENT_ID") {
                            viewModel.showMessage("Google sign-in needs google_web_client_id in app/src/main/res/values/firebase_auth.xml.")
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
                    enabled = !isLoading
                ) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue with Google")
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                if (usePhoneAuth) {
                    if (isCodeSent) {
                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = { verificationCode = it },
                            label = { Text("Verification Code") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (error != null) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        Button(
                            onClick = { viewModel.verifyCode(verificationCode) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && verificationCode.isNotBlank()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Verify Code")
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number (e.g. +91...)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (error != null) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        Button(
                            onClick = { 
                                val activity = context as? Activity
                                if (activity != null) {
                                    viewModel.verifyPhoneNumber(phoneNumber, activity)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && phoneNumber.isNotBlank()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Send Verification Code")
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    Button(
                        onClick = {
                            if (isSignUp) viewModel.signUp(email, password) else viewModel.signIn(email, password)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(if (isSignUp) "Sign Up" else "Sign In")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(onClick = { isSignUp = !isSignUp }) {
                        Text(if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(onClick = { usePhoneAuth = !usePhoneAuth }) {
                    Text(if (usePhoneAuth) "Use Email/Password instead" else "Sign in with Phone Number")
                }

                TextButton(onClick = onContinueAsGuest) {
                    Text("Continue without account")
                }
            }
        }
    }
}
