package com.example.pagertounsi

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response



@Composable
fun LoginScreen(nav: NavController) {

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            "Welcome Back",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errorMessage.isNotEmpty()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errorMessage.isNotEmpty()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    errorMessage = "Please fill all fields"
                    return@Button
                }

                isLoading = true
                errorMessage = ""

                ApiClient.api.login(LoginRequest(username, password))
                    .enqueue(object : Callback<LoginResponse> {

                        override fun onResponse(
                            call: Call<LoginResponse>,
                            response: Response<LoginResponse>
                        ) {
                            isLoading = false

                            val body = response.body()
                            if (body == null) {
                                errorMessage = "Empty response from server"
                                return
                            }

                            when (body.status) {
                                "ok" -> {
                                    scope.launch {
                                        try {
                                            context.dataStore.edit { preferences ->
                                                preferences[stringPreferencesKey("username")] = username
                                            }

                                            Log.d("Login", "Getting FCM token...")

                                            // Get and send FCM token
                                            FirebaseMessaging.getInstance().token
                                                .addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        val token = task.result
                                                        Log.d("Login", "FCM Token: $token")

                                                        ApiClient.api.updateFcmToken(FcmTokenRequest(username, token))
                                                            .enqueue(object : Callback<ApiResponse> {
                                                                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                                                                    Log.d("Login", "Token sent to server: ${response.body()?.status}")
                                                                }
                                                                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                                                                    Log.e("Login", "Failed to send token: ${t.message}")
                                                                }
                                                            })
                                                    } else {
                                                        Log.e("Login", "Failed to get FCM token", task.exception)
                                                    }
                                                }

                                            // Send online status
                                            SocketManager.getSocket()?.emit("userOnline", username)

                                            // Start foreground service as backup
                                            val serviceIntent = Intent(context, GroupManagementService::class.java).apply {
                                                putExtra("username", username)
                                            }
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                context.startForegroundService(serviceIntent)
                                            } else {
                                                context.startService(serviceIntent)
                                            }

                                            nav.navigate("groups") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Error: ${e.message}"
                                            Log.e("Login", "Error: ${e.message}")
                                        }
                                    }
                                }
                                "not_verified" -> {
                                    body.email?.let { email ->
                                        nav.navigate("verify/$email")
                                    } ?: run {
                                        errorMessage = "Email not found"
                                    }
                                }
                                "error" -> {
                                    errorMessage = body.message ?: "Login failed"
                                }
                                else -> {
                                    errorMessage = "Unexpected response"
                                }
                            }
                        }

                        override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                            isLoading = false
                            errorMessage = "Network error: ${t.message}"
                            Log.e("Login", "Network error: ${t.message}")
                        }
                    })
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotBlank() && password.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = { nav.navigate("signup") }
        ) {
            Text("Don't have an account? Sign up")
        }
    }
}