package com.example.pagertounsi

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// Define the data class here (only once!)
data class SimpleVersionInfo(
    val latestVersion: String,
    val latestVersionCode: Int,
    val updateUrl: String,
    val releaseNotes: List<String>
)

class VersionChecker(private val context: Context) {

    companion object {
        private const val VERSION_URL = "http://pagertn.viewdns.net:3000/app-version.json"
        private const val TAG = "VersionChecker"
    }

    suspend fun checkForNewVersion(): SimpleVersionInfo? = withContext(Dispatchers.IO) {
        return@withContext try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(VERSION_URL)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: return@withContext null)
                val androidJson = json.getJSONObject("android")

                // Parse release notes array
                val releaseNotesArray = androidJson.getJSONArray("release_notes")
                val releaseNotes = List(releaseNotesArray.length()) { index ->
                    releaseNotesArray.getString(index)
                }

                SimpleVersionInfo(
                    latestVersion = androidJson.getString("latest_version"),
                    latestVersionCode = androidJson.getInt("latest_version_code"),
                    updateUrl = androidJson.getString("update_url"),
                    releaseNotes = releaseNotes
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking version: ${e.message}")
            null
        }
    }

    // For backward compatibility with your old code
    suspend fun checkVersion(): SimpleVersionInfo? = checkForNewVersion()

    fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            1
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }
}