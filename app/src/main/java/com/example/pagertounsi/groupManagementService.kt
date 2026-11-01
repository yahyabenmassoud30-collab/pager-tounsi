package com.example.pagertounsi

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import io.socket.emitter.Emitter
import kotlinx.coroutines.*
import org.json.JSONObject

class GroupManagementService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private lateinit var username: String
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "chat_messages"
        private const val CHANNEL_NAME = "Chat Messages"
        private const val WAKE_LOCK_TAG = "ChatApp:GroupService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("GroupService", "Service created")

        acquireWakeLock()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            wakeLock?.acquire(10 * 60 * 1000L)
            Log.d("GroupService", "Wake lock acquired")
        } catch (e: Exception) {
            Log.e("GroupService", "Error acquiring wake lock: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val usernameExtra = intent?.getStringExtra("username")
        if (usernameExtra.isNullOrEmpty()) {
            Log.d("GroupService", "No username provided, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        username = usernameExtra
        Log.d("GroupService", "Service started for user: $username")

        if (!isRunning) {
            isRunning = true
            startListeningForMessages()
        }

        return START_STICKY
    }

    private fun startListeningForMessages() {
        serviceScope.launch {
            try {
                Log.d("GroupService", "Starting to listen for messages")

                var retryCount = 0
                val maxRetries = 5

                while (retryCount < maxRetries && !SocketManager.isConnected()) {
                    Log.d("GroupService", "Socket not connected, connecting... (attempt ${retryCount + 1})")
                    SocketManager.connect()
                    delay(3000)
                    retryCount++
                }

                val socket = SocketManager.getSocket()

                if (socket == null || !socket.connected()) {
                    Log.d("GroupService", "Socket not connected after retries")
                    scheduleRetry()
                    return@launch
                }

                Log.d("GroupService", "Socket connected, attaching listener")

                socket.off("newMessage")
                socket.on("newMessage", Emitter.Listener { args ->
                    try {
                        if (args.isNotEmpty()) {
                            val data = args[0]

                            var msgUsername = "Unknown"
                            var message = ""
                            var photoUrl: String? = null

                            when (data) {
                                is JSONObject -> {
                                    msgUsername = data.optString("username", "Unknown")
                                    message = data.optString("message", "")
                                    photoUrl = data.optString("photoUrl", null)
                                }
                                is String -> {
                                    val parts = data.split(":", limit = 2)
                                    msgUsername = if (parts.size > 1) parts[0].trim() else "Unknown"
                                    message = if (parts.size > 1) parts[1].trim() else data
                                }
                            }

                            if (::username.isInitialized && msgUsername != username) {
                                val content = if (!photoUrl.isNullOrEmpty() && photoUrl != "null")
                                    "📷 Photo"
                                else
                                    message

                                showNotification("New message from $msgUsername", content)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("GroupService", "Error processing message: ${e.message}")
                    }
                })

                socket.on("disconnect") {
                    Log.d("GroupService", "Socket disconnected, will retry")
                    scheduleRetry()
                }

            } catch (e: Exception) {
                Log.e("GroupService", "Error in startListeningForMessages: ${e.message}")
                scheduleRetry()
            }
        }
    }

    private fun scheduleRetry() {
        serviceScope.launch {
            delay(10000)
            if (isRunning) {
                Log.d("GroupService", "Retrying connection...")
                startListeningForMessages()
            }
        }
    }

    private fun showNotification(title: String, content: String) {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)

            Log.d("GroupService", "Notification shown: $title")
        } catch (e: Exception) {
            Log.e("GroupService", "Error showing notification: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for new chat messages"
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                Log.d("GroupService", "Notification channel created")
            } catch (e: Exception) {
                Log.e("GroupService", "Error creating channel: ${e.message}")
            }
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("Chat App")
            .setContentText("Listening for messages...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(Notification.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d("GroupService", "Wake lock released")
            }
        } catch (e: Exception) {
            Log.e("GroupService", "Error releasing wake lock: ${e.message}")
        }

        Log.d("GroupService", "Service destroyed")

        if (::username.isInitialized) {
            val restartIntent = Intent(this, GroupManagementService::class.java).apply {
                putExtra("username", username)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                startService(restartIntent)
            }
            Log.d("GroupService", "Service restarted")
        }
    }
}