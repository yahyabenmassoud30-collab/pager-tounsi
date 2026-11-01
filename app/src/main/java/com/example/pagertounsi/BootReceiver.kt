package com.example.pagertounsi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

// This should be declared ONLY ONCE in your entire project
// Make sure it's not in any other file


class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device boot completed, restarting service")

            CoroutineScope(Dispatchers.IO).launch {
                delay(5000)

                val username = getSavedUsername(context)

                if (username != null) {
                    Log.d("BootReceiver", "Restarting service for user: $username")

                    val serviceIntent = Intent(context, GroupManagementService::class.java).apply {
                        putExtra("username", username)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }

    private suspend fun getSavedUsername(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val dataStoreKey = stringPreferencesKey("username")
                val preferences = context.dataStore.data.first()
                preferences[dataStoreKey]
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error getting username: ${e.message}")
                null
            }
        }
    }
}