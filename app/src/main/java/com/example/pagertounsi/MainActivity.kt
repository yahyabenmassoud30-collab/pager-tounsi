package com.example.pagertounsi

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class MainActivity : ComponentActivity() {

    // Remove these old state variables
    // private lateinit var versionChecker: VersionChecker
    // private lateinit var updateDownloader: UpdateDownloader
    // private var showUpdateDialog by mutableStateOf(false)
    // private var versionInfo by mutableStateOf<VersionInfo?>(null)
    // private var isForceUpdate by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Remove the old update checker initialization
        // versionChecker = VersionChecker(this)
        // updateDownloader = UpdateDownloader(this)

        NotificationService.createNotificationChannel(this)
        SocketManager.connect()

        val groupCode = intent.getStringExtra("groupCode")

        // Remove the old update check
        // lifecycleScope.launch {
        //     checkForUpdates()
        // }

        setContent {
            val navController = rememberNavController()
            val dataStore = this.dataStore

            var showPatchNotes by remember { mutableStateOf(true) }

            // Check if user is already logged in and send online status
            LaunchedEffect(Unit) {
                try {
                    val dataStoreKey = stringPreferencesKey("username")
                    val preferences = dataStore.data.first()
                    val savedUsername = preferences[dataStoreKey]

                    if (savedUsername != null && savedUsername.isNotEmpty()) {
                        // Send online status when app opens
                        SocketManager.getSocket()?.on("connect") {
                            SocketManager.getSocket()?.emit("userOnline", savedUsername)
                            Log.d("MainActivity", "User online: $savedUsername")
                        }

                        try {
                            val serviceIntent = Intent(this@MainActivity, GroupManagementService::class.java).apply {
                                putExtra("username", savedUsername)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(serviceIntent)
                            } else {
                                startService(serviceIntent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        navController.navigate("groups") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            LaunchedEffect(groupCode) {
                if (groupCode != null && groupCode.isNotEmpty()) {
                    navController.navigate("chat/$groupCode")
                }
            }

            // Remove the old update dialog
            // if (showUpdateDialog && versionInfo != null) {
            //     ForceUpdateDialog(...)
            // }

            // Show patch notes dialog
            if (showPatchNotes) {
                PatchNotesDialog(
                    onDismiss = {
                        showPatchNotes = false
                    }
                )
            }

            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(navController)
                }
                composable("signup") {
                    SignupScreen(navController)
                }
                composable("groups") {
                    GroupScreen(navController)
                }
                composable("chat/{groupCode}") { backStackEntry ->
                    val code = backStackEntry.arguments?.getString("groupCode") ?: ""
                    ChatScreen(navController, code)
                }
                composable(
                    "verify/{email}",
                    arguments = listOf(navArgument("email") { type = NavType.StringType })
                ) { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email") ?: ""
                    VerificationScreen(navController, email)
                }
            }
        }
    }

    // Remove the old checkForUpdates function
    // private suspend fun checkForUpdates() { ... }

    override fun onDestroy() {
        super.onDestroy()
        try {
            SocketManager.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        // Remove the old update check on resume
        // lifecycleScope.launch {
        //     checkForUpdates()
        // }
    }
}