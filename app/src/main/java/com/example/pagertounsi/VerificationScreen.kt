package com.example.pagertounsi

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun VerificationScreen(nav: NavController, email: String) {

    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000)
            timeLeft--
        } else {
            canResend = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            "Verify Your Email",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "We've sent a verification code to:",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            email,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = code,
            onValueChange = {
                if (it.length <= 6) code = it
            },
            label = { Text("Enter 6-digit code") },
            placeholder = { Text("000000") },
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            supportingText = if (isError) {
                { Text(message) }
            } else null
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isLoading = true
                isError = false
                ApiClient.api.verifyCode(VerifyRequest(email, code))
                    .enqueue(object : Callback<ApiResponse> {
                        override fun onResponse(
                            call: Call<ApiResponse>,
                            response: Response<ApiResponse>
                        ) {
                            isLoading = false
                            when (response.body()?.status) {
                                "ok" -> {
                                    nav.navigate("login") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                                "expired" -> {
                                    isError = true
                                    message = "Code expired. Request a new one."
                                }
                                else -> {
                                    isError = true
                                    message = response.body()?.message ?: "Invalid code"
                                }
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                            isLoading = false
                            isError = true
                            message = "Network error. Please try again."
                        }
                    })
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = code.length == 6 && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Verify")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isResending = true
                ApiClient.api.resendVerification(EmailRequest(email))
                    .enqueue(object : Callback<ApiResponse> {
                        override fun onResponse(
                            call: Call<ApiResponse>,
                            response: Response<ApiResponse>
                        ) {
                            isResending = false
                            if (response.body()?.status == "sent") {
                                message = "New code sent!"
                                isError = false
                                timeLeft = 60
                                canResend = false
                            } else {
                                isError = true
                                message = response.body()?.message ?: "Failed to resend"
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                            isResending = false
                            isError = true
                            message = "Network error"
                        }
                    })
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canResend && !isResending
        ) {
            if (isResending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (canResend) "Resend Code" else "Resend in $timeLeft seconds")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { nav.navigate("login") }
        ) {
            Text("Back to Login")
        }
    }
}