package com.example.pagertounsi

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class UpdateDownloader(private val context: Context) {

    fun downloadApk(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d("UpdateDownloader", "Opened browser for download: $url")
        } catch (e: Exception) {
            Log.e("UpdateDownloader", "Failed to open browser", e)
        }
    }
}