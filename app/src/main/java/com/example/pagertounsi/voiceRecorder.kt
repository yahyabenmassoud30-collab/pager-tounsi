package com.example.pagertounsi

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

class VoiceRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentFilePath: String? = null
    private var isPlayingState = false
    private var onPlaybackComplete: (() -> Unit)? = null

    // In your VoiceRecorder class, add this function
    fun requestMicrophonePermission(activity: Activity) {
        if (!hasMicrophonePermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            }
        }
    }

    fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecording(): String? {
        try {
            val fileName = "voice_${System.currentTimeMillis()}.3gp"
            val file = File(context.cacheDir, fileName)
            currentFilePath = file.absolutePath

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                prepare()
                start()
                Log.d("VoiceRecorder", "Recording started: ${file.absolutePath}")
            }
            return currentFilePath
        } catch (e: IOException) {
            Log.e("VoiceRecorder", "Failed to start recording: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    fun stopRecording(): String? {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Log.d("VoiceRecorder", "Recording stopped: $currentFilePath")
            return currentFilePath
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Failed to stop recording: ${e.message}")
            return null
        }
    }

    fun getAudioDuration(filePath: String): Int {
        return try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
            }
            val duration = player.duration / 1000 // Convert to seconds
            player.release()
            duration
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Failed to get duration: ${e.message}")
            0
        }
    }

    // SIMPLE VERSION - No effects
    fun playAudio(filePath: String, onComplete: () -> Unit = {}) {
        try {
            onPlaybackComplete = onComplete
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                setOnCompletionListener {
                    isPlayingState = false
                    release()
                    mediaPlayer = null
                    onPlaybackComplete?.invoke()
                }
            }
            mediaPlayer = player
            isPlayingState = true
            Log.d("VoiceRecorder", "Playing audio: $filePath")
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Failed to play audio: ${e.message}")
            e.printStackTrace()
            onComplete()
        }
    }

    fun stopPlaying() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                    release()
                }
            }
            mediaPlayer = null
            isPlayingState = false
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Failed to stop playing: ${e.message}")
        }
    }

    fun isCurrentlyPlaying(): Boolean = isPlayingState

    fun release() {
        stopRecording()
        stopPlaying()
    }
}