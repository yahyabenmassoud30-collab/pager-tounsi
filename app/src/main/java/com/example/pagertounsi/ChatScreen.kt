package com.example.pagertounsi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import io.socket.emitter.Emitter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import com.example.pagertounsi.VoiceMessageBubble
import com.example.pagertounsi.VideoMessageBubble





// ==================== HELPER FUNCTIONS ====================
fun isDuplicateMessage(existing: ChatMessageData, newMessage: ChatMessageData): Boolean {
    // If it's a media message, check by URL
    if (newMessage.photoUrl != null && existing.photoUrl == newMessage.photoUrl) return true
    if (newMessage.videoUrl != null && existing.videoUrl == newMessage.videoUrl) return true
    if (newMessage.voiceUrl != null && existing.voiceUrl == newMessage.voiceUrl) return true

    // For text messages, check content and approximate time
    if (newMessage.message.isNotBlank() &&
        existing.message == newMessage.message &&
        existing.username == newMessage.username &&
        Math.abs(existing.timestamp - newMessage.timestamp) < 5000) { // Within 5 seconds
        return true
    }

    return false
}
fun parseDate(dateString: String): Long {
    return try {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "EEE MMM dd HH:mm:ss zzz yyyy"
        )

        for (format in formats) {
            try {
                val parser = SimpleDateFormat(format, Locale.getDefault())
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(dateString)
                if (date != null) {
                    return date.time
                }
            } catch (e: Exception) {
                // Try next format
            }
        }
        System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var fileName: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = it.getString(nameIndex)
            }
        }
    }
    return fileName
}

fun loadOldMessages(
    context: Context,
    groupCode: String,
    messages: MutableList<ChatMessageData>,
    listState: LazyListState,
    scope: CoroutineScope
) {
    ApiClient.api.getMessages(groupCode).enqueue(object : Callback<List<MessageResponse>> {
        override fun onResponse(call: Call<List<MessageResponse>>, response: Response<List<MessageResponse>>) {
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isNotEmpty()) {
                    messages.clear()
                    body.forEach { msg ->
                        val safeUsername = if (msg.username.isNullOrEmpty()) "Unknown" else msg.username
                        val safeMessage = msg.message ?: ""
                        val timestamp = parseDate(msg.createdAt)
                        messages.add(
                            ChatMessageData(
                                username = safeUsername,
                                message = safeMessage,
                                photoUrl = msg.photoUrl,
                                videoUrl = msg.videoUrl,
                                voiceUrl = msg.voiceUrl,
                                duration = msg.duration,
                                timestamp = timestamp
                            )
                        )
                    }
                    messages.sortBy { it.timestamp }
                    scope.launch {
                        if (messages.isNotEmpty()) {
                            listState.scrollToItem(messages.size - 1)
                        }
                    }
                }
            }
        }
        override fun onFailure(call: Call<List<MessageResponse>>, t: Throwable) {
            Toast.makeText(context, "Failed to load messages: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    })
}

fun uploadPhotoFile(
    context: Context,
    file: File,
    groupCode: String,
    username: String,
    onComplete: () -> Unit
) {
    Log.d("PhotoUpload", "=== Starting photo upload ===")
    Log.d("PhotoUpload", "File path: ${file.absolutePath}")
    Log.d("PhotoUpload", "File size: ${file.length()} bytes")

    val mimeType = when (file.extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        else -> "image/jpeg"
    }
    Log.d("PhotoUpload", "MIME type: $mimeType")

    val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
    val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)

    ApiClient.api.uploadPhoto(body).enqueue(object : Callback<PhotoUploadResponse> {
        override fun onResponse(call: Call<PhotoUploadResponse>, response: Response<PhotoUploadResponse>) {
            Log.d("PhotoUpload", "Response code: ${response.code()}")
            onComplete()
            if (response.isSuccessful && response.body()?.status == "ok") {
                val photoUrl = response.body()?.photoUrl
                Log.d("PhotoUpload", "✅ Success! Photo URL: $photoUrl")
                val socket = SocketManager.getSocket()
                socket?.emit("sendMessage", groupCode, "", username, photoUrl, null, null, null)
                Toast.makeText(context, "Photo uploaded!", Toast.LENGTH_SHORT).show()
                file.delete()
            } else {
                Log.e("PhotoUpload", "❌ Upload failed")
                Toast.makeText(context, "Failed to upload photo", Toast.LENGTH_SHORT).show()
                file.delete()
            }
        }
        override fun onFailure(call: Call<PhotoUploadResponse>, t: Throwable) {
            Log.e("PhotoUpload", "❌ Network error", t)
            onComplete()
            Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            file.delete()
        }
    })
}

fun uploadPhoto(
    context: Context,
    uri: Uri,
    groupCode: String,
    username: String,
    onComplete: () -> Unit
) {
    try {
        val contentResolver = context.contentResolver
        val fileName = getFileNameFromUri(context, uri)
        val inputStream = contentResolver.openInputStream(uri) ?: return
        val extension = fileName?.substringAfterLast('.', "jpg") ?: "jpg"
        val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.$extension")
        file.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        uploadPhotoFile(context, file, groupCode, username, onComplete)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to process photo: ${e.message}", Toast.LENGTH_SHORT).show()
        onComplete()
    }
}

fun uploadVideoFile(
    context: Context,
    file: File,
    groupCode: String,
    username: String,
    onComplete: () -> Unit
) {
    Log.d("VideoUpload", "=== Starting video upload ===")
    Log.d("VideoUpload", "File path: ${file.absolutePath}")
    Log.d("VideoUpload", "File size: ${file.length()} bytes")
    Log.d("VideoUpload", "File extension: ${file.extension}")

    val mimeType = when (file.extension.lowercase()) {
        "mp4" -> "video/mp4"
        "3gp" -> "video/3gpp"
        "mov" -> "video/quicktime"
        "avi" -> "video/x-msvideo"
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        else -> "video/mp4"
    }
    Log.d("VideoUpload", "Detected MIME type: $mimeType")

    val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
    val body = MultipartBody.Part.createFormData("video", file.name, requestFile)

    ApiClient.api.uploadVideo(body).enqueue(object : Callback<VideoUploadResponse> {
        override fun onResponse(call: Call<VideoUploadResponse>, response: Response<VideoUploadResponse>) {
            Log.d("VideoUpload", "Response received. Code: ${response.code()}")
            Log.d("VideoUpload", "Response message: ${response.message()}")

            onComplete()

            if (response.isSuccessful) {
                val body = response.body()
                Log.d("VideoUpload", "Response body: $body")

                if (body?.status == "ok") {
                    val videoUrl = body.videoUrl
                    Log.d("VideoUpload", "✅ Upload successful! Video URL: $videoUrl")

                    val socket = SocketManager.getSocket()
                    Log.d("VideoUpload", "Emitting message with video URL")
                    socket?.emit("sendMessage", groupCode, "", username, null, videoUrl, null, null)

                    Toast.makeText(context, "Video uploaded!", Toast.LENGTH_SHORT).show()
                    file.delete()
                } else {
                    Log.e("VideoUpload", "❌ Upload failed. Status: ${body?.status}")
                    Log.e("VideoUpload", "Error message: ${body?.message}")
                    Toast.makeText(context, "Failed to upload video: ${body?.message}", Toast.LENGTH_SHORT).show()
                    file.delete()
                }
            } else {
                Log.e("VideoUpload", "❌ HTTP error: ${response.code()}")
                try {
                    val errorBody = response.errorBody()?.string()
                    Log.e("VideoUpload", "Error body: $errorBody")
                } catch (e: Exception) {
                    Log.e("VideoUpload", "Could not read error body")
                }
                Toast.makeText(context, "Server error: ${response.code()}", Toast.LENGTH_SHORT).show()
                file.delete()
            }
        }

        override fun onFailure(call: Call<VideoUploadResponse>, t: Throwable) {
            Log.e("VideoUpload", "❌ Network failure", t)
            onComplete()
            Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            file.delete()
        }
    })
}

fun uploadVideo(
    context: Context,
    uri: Uri,
    groupCode: String,
    username: String,
    onComplete: () -> Unit
) {
    try {
        val contentResolver = context.contentResolver
        val fileName = getFileNameFromUri(context, uri)
        val inputStream = contentResolver.openInputStream(uri) ?: return
        val extension = fileName?.substringAfterLast('.', "mp4") ?: "mp4"
        val file = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.$extension")
        file.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        uploadVideoFile(context, file, groupCode, username, onComplete)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to process video: ${e.message}", Toast.LENGTH_SHORT).show()
        onComplete()
    }
}

fun uploadVoiceFile(
    context: Context,
    filePath: String,
    groupCode: String,
    username: String,
    duration: Int,
    onComplete: () -> Unit
) {
    val file = File(filePath)
    val requestFile = file.asRequestBody("audio/3gpp".toMediaTypeOrNull())
    val body = MultipartBody.Part.createFormData("audio", file.name, requestFile)
    ApiClient.api.uploadVoice(body).enqueue(object : Callback<VoiceUploadResponse> {
        override fun onResponse(call: Call<VoiceUploadResponse>, response: Response<VoiceUploadResponse>) {
            onComplete()
            if (response.isSuccessful && response.body()?.status == "ok") {
                val voiceUrl = response.body()?.voiceUrl
                val socket = SocketManager.getSocket()
                socket?.emit("sendMessage", groupCode, "", username, null, null, voiceUrl, duration)
                Toast.makeText(context, "Voice message sent!", Toast.LENGTH_SHORT).show()
                file.delete()
            } else {
                Toast.makeText(context, "Failed to upload voice", Toast.LENGTH_SHORT).show()
                file.delete()
            }
        }
        override fun onFailure(call: Call<VoiceUploadResponse>, t: Throwable) {
            onComplete()
            Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            file.delete()
        }
    })
}

// ==================== COMPOSABLE FUNCTIONS ====================






@Composable
fun MessageBubble(message: ChatMessageData, currentUsername: String) {
    val isCurrentUser = message.username == currentUsername

    val timeString = remember(message.timestamp) {
        val date = Date(message.timestamp)
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            tonalElevation = if (isCurrentUser) 0.dp else 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isCurrentUser) {
                    Text(message.username, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }

                // Video message
                if (!message.videoUrl.isNullOrEmpty()) {
                    VideoMessageBubble(
                        videoUrl = message.videoUrl,
                        isCurrentUser = isCurrentUser,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Voice message
                if (!message.voiceUrl.isNullOrEmpty()) {
                    VoiceMessageBubble(
                        voiceUrl = message.voiceUrl,
                        duration = message.duration ?: 0,
                        isCurrentUser = isCurrentUser,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Photo message
                if (!message.photoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(message.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Photo message",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Text message
                if (message.message.isNotBlank()) {
                    Text(
                        message.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                } else if (!message.photoUrl.isNullOrEmpty() && message.message.isBlank()) {
                    Text(
                        "📷 Photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrentUser)
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun AsyncImage(
    model: Any,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val painter = rememberAsyncImagePainter(model = model, contentScale = contentScale)
    Image(painter = painter, contentDescription = contentDescription, modifier = modifier)
}

// ==================== MAIN CHAT SCREEN COMPOSABLE ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, groupCode: String) {

    Log.d("ChatScreen", "=== ChatScreen started for group: $groupCode ===")

    val messages = remember { mutableStateListOf<ChatMessageData>() }
    var text by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var isVideoUploading by remember { mutableStateOf(false) }
    var hasLoadedMessages = remember { mutableStateOf(false) }

    // Voice recording states
    var isRecording by remember { mutableStateOf(false) }
    var recordingPath by remember { mutableStateOf<String?>(null) }
    var recordingDuration by remember { mutableStateOf(0) }
    var showVoiceInterface by remember { mutableStateOf(false) }

    // Members dialog state
    var showMembersDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Initialize VoiceRecorder
    val voiceRecorder = remember { VoiceRecorder(context) }

    // Define startVoiceRecording function
    fun startVoiceRecording() {
        recordingPath = voiceRecorder.startRecording()
        if (recordingPath != null) {
            isRecording = true
        }
    }

    // Timer for recording duration
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            Log.d("ChatScreen", "Gallery launcher triggered with URI: $uri")

            val mimeType = context.contentResolver.getType(uri)
            Log.d("ChatScreen", "MIME type: $mimeType")

            when {
                mimeType?.startsWith("image/") == true -> {
                    isUploading = true
                    try {
                        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    } catch (e: Exception) {
                        Log.e("ChatScreen", "Error granting URI permission: ${e.message}")
                    }

                    uploadPhoto(
                        context = context,
                        uri = uri,
                        groupCode = groupCode,
                        username = username,
                        onComplete = { isUploading = false }
                    )
                }
                mimeType?.startsWith("video/") == true -> {
                    isVideoUploading = true
                    try {
                        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    } catch (e: Exception) {
                        Log.e("ChatScreen", "Error granting URI permission: ${e.message}")
                    }

                    uploadVideo(
                        context = context,
                        uri = uri,
                        groupCode = groupCode,
                        username = username,
                        onComplete = { isVideoUploading = false }
                    )
                }
                else -> {
                    Toast.makeText(context, "Unsupported file type", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val dataStoreKey = stringPreferencesKey("username")
            val preferences = context.dataStore.data.first()
            username = preferences[dataStoreKey] ?: "Unknown"

            if (username == "Unknown") {
                errorMessage = "No username found. Please login again."
                isLoading = false
                return@LaunchedEffect
            }

            if (!SocketManager.isConnected()) {
                SocketManager.connect()
                delay(2000)
            }

            val socket = SocketManager.getSocket()

            if (socket != null && socket.connected()) {
                socket.emit("userOnline", username)
                socket.emit("joinGroup", groupCode, username)

                ApiClient.api.joinGroup(GroupMemberRequest(groupCode, username))
                    .enqueue(object : Callback<ApiResponse> {
                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                            if (!hasLoadedMessages.value) {
                                loadOldMessages(context, groupCode, messages, listState, scope)
                                hasLoadedMessages.value = true
                            }
                        }
                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                            Toast.makeText(context, "Failed to join group", Toast.LENGTH_SHORT).show()
                        }
                    })

                isLoading = false
                Toast.makeText(context, "Joined group $groupCode", Toast.LENGTH_SHORT).show()
            } else {
                errorMessage = "Cannot connect to server"
                isLoading = false
            }
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        val socket = SocketManager.getSocket()
        Log.d("ChatScreen", "📡 Setting up message listener for group: $groupCode")

        val messageListener = Emitter.Listener { args ->
            try {
                if (args.isNotEmpty()) {
                    val data = args[0]
                    Log.d("ChatScreen", "📩 RAW message received: $data")
                    Log.d("ChatScreen", "   Data type: ${data?.javaClass?.simpleName}")

                    val chatMessage = when (data) {
                        is JSONObject -> {
                            Log.d("ChatScreen", "   Parsing JSON message")

                            // Extract all possible fields
                            val msgUsername = data.optString("username", "Unknown")
                            val msgText = data.optString("message", "")
                            val msgPhotoUrl = data.optString("photoUrl", null).takeIf { it != "null" && it.isNotEmpty() }
                            val msgVideoUrl = data.optString("videoUrl", null).takeIf { it != "null" && it.isNotEmpty() }
                            val msgVoiceUrl = data.optString("voiceUrl", null).takeIf { it != "null" && it.isNotEmpty() }
                            val msgDuration = if (data.has("duration")) data.optInt("duration") else null
                            val msgTimestamp = data.optLong("timestamp", System.currentTimeMillis())

                            // Log what we found
                            Log.d("ChatScreen", "   Parsed fields:")
                            Log.d("ChatScreen", "      username: $msgUsername")
                            Log.d("ChatScreen", "      text: ${msgText.take(30)}${if (msgText.length > 30) "..." else ""}")
                            if (msgPhotoUrl != null) Log.d("ChatScreen", "      photo: $msgPhotoUrl")
                            if (msgVideoUrl != null) Log.d("ChatScreen", "      video: $msgVideoUrl")
                            if (msgVoiceUrl != null) Log.d("ChatScreen", "      voice: $msgVoiceUrl")
                            if (msgDuration != null) Log.d("ChatScreen", "      duration: $msgDuration")
                            Log.d("ChatScreen", "      timestamp: $msgTimestamp")

                            ChatMessageData(
                                username = if (msgUsername.isNullOrEmpty()) "Unknown" else msgUsername,
                                message = msgText ?: "",
                                photoUrl = msgPhotoUrl,
                                videoUrl = msgVideoUrl,
                                voiceUrl = msgVoiceUrl,
                                duration = msgDuration,
                                timestamp = msgTimestamp
                            )
                        }
                        is String -> {
                            Log.d("ChatScreen", "   Parsing String message: $data")
                            val parts = data.split(":", limit = 2)
                            val msgUsername = if (parts.size > 1) parts[0].trim() else "Unknown"
                            val msgText = if (parts.size > 1) parts[1].trim() else data

                            Log.d("ChatScreen", "   Parsed - user: $msgUsername, text: ${msgText.take(30)}")

                            ChatMessageData(
                                username = if (msgUsername.isNullOrEmpty()) "Unknown" else msgUsername,
                                message = msgText ?: "",
                                photoUrl = null,
                                videoUrl = null,
                                voiceUrl = null,
                                duration = null,
                                timestamp = System.currentTimeMillis()
                            )
                        }
                        else -> {
                            Log.e("ChatScreen", "❌ Unknown message type: ${data?.javaClass?.simpleName}")
                            ChatMessageData("System", "Unknown message format")
                        }
                    }

                    // Check for duplicates using more sophisticated logic
                    val isDuplicate = messages.any { existing ->
                        // For media messages, check URLs
                        if (chatMessage.photoUrl != null && existing.photoUrl == chatMessage.photoUrl) return@any true
                        if (chatMessage.videoUrl != null && existing.videoUrl == chatMessage.videoUrl) return@any true
                        if (chatMessage.voiceUrl != null && existing.voiceUrl == chatMessage.voiceUrl) return@any true

                        // For text messages, check content and timestamp within a small window
                        if (chatMessage.message.isNotBlank() &&
                            existing.message == chatMessage.message &&
                            existing.username == chatMessage.username &&
                            Math.abs(existing.timestamp - chatMessage.timestamp) < 2000) { // Within 2 seconds
                            return@any true
                        }

                        false
                    }

                    if (!isDuplicate) {
                        Log.d("ChatScreen", "✅ Adding new message to list")
                        messages.add(chatMessage)
                        messages.sortBy { it.timestamp }

                        Log.d("ChatScreen", "   Current message count: ${messages.size}")

                        scope.launch {
                            listState.animateScrollToItem(messages.size - 1)
                        }

                        // Determine message type for logging
                        val messageType = when {
                            chatMessage.videoUrl != null -> "🎥 video"
                            chatMessage.photoUrl != null -> "📷 photo"
                            chatMessage.voiceUrl != null -> "🎤 voice"
                            else -> "💬 text"
                        }
                        Log.d("ChatScreen", "   Message type: $messageType")

                        // Only show notification for messages from other users
                        if (chatMessage.username != username) {
                            Log.d("ChatScreen", "   Message from other user - would show notification")
                            // Notification logic here if needed
                        }
                    } else {
                        Log.d("ChatScreen", "⏭️ Duplicate message ignored")
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatScreen", "❌ Error processing message: ${e.message}")
                e.printStackTrace()
            }
        }

        // Error listener for socket issues
        val errorListener = Emitter.Listener { args ->
            Log.e("ChatScreen", "⚠️ Socket error: ${args.contentToString()}")
        }

        // Connect listener
        val connectListener = Emitter.Listener {
            Log.d("ChatScreen", "✅ Socket connected")
            // Re-join group if needed
            socket?.emit("joinGroup", groupCode, username)
        }

        // Disconnect listener
        val disconnectListener = Emitter.Listener {
            Log.d("ChatScreen", "❌ Socket disconnected")
        }

        // Attach all listeners
        socket?.on("newMessage", messageListener)
        socket?.on("connect_error", errorListener)
        socket?.on("connect", connectListener)
        socket?.on("disconnect", disconnectListener)

        Log.d("ChatScreen", "📡 All listeners attached")

        onDispose {
            Log.d("ChatScreen", "🧹 Cleaning up listeners for group: $groupCode")

            // Update last seen when leaving
            socket?.emit("updateLastSeen", groupCode, username)
            Log.d("ChatScreen", "   Last seen updated for group: $groupCode")

            // Remove all listeners
            socket?.off("newMessage", messageListener)
            socket?.off("connect_error", errorListener)
            socket?.off("connect", connectListener)
            socket?.off("disconnect", disconnectListener)

            // Release voice recorder
            voiceRecorder.release()
            Log.d("ChatScreen", "   Voice recorder released")

            Log.d("ChatScreen", "🧹 Cleanup complete")
        }
    }

    if (errorMessage != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                Button(onClick = { navController.popBackStack() }) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Group: $groupCode")
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "?",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←", fontSize = 24.sp)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showMembersDialog = true }
                    ) {
                        Text("👥", fontSize = 20.sp)
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                android.app.AlertDialog.Builder(context)
                                    .setTitle("Leave Group")
                                    .setMessage("Are you sure you want to leave this group? You will stop receiving messages from this group.")
                                    .setPositiveButton("Leave") { _, _ ->
                                        ApiClient.api.leaveGroup(GroupMemberRequest(groupCode, username))
                                            .enqueue(object : Callback<ApiResponse> {
                                                override fun onResponse(
                                                    call: Call<ApiResponse>,
                                                    response: Response<ApiResponse>
                                                ) {
                                                    if (response.body()?.status == "ok") {
                                                        Toast.makeText(context, "Left group $groupCode", Toast.LENGTH_SHORT).show()
                                                        val socket = SocketManager.getSocket()
                                                        socket?.emit("leaveGroup", groupCode, username)
                                                        navController.popBackStack()
                                                    } else {
                                                        Toast.makeText(context, "Failed to leave group", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                                                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            })
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                            }
                        }
                    ) {
                        Text("🚪", fontSize = 24.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(msg, username)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Column {
                    if (isUploading || isVideoUploading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isVideoUploading)
                                MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary
                        )
                    }

                    if (showVoiceInterface) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    showVoiceInterface = false
                                    if (isRecording) {
                                        voiceRecorder.stopRecording()
                                        isRecording = false
                                    }
                                }
                            ) {
                                Text("Cancel")
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isRecording) Color.Red else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isRecording) "Recording... $recordingDuration s" else "Ready to record"
                                )
                            }
                            if (!isRecording) {
                                Button(
                                    onClick = { startVoiceRecording() }
                                ) {
                                    Text("Record")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val path = voiceRecorder.stopRecording()
                                        isRecording = false
                                        if (path != null) {
                                            val duration = voiceRecorder.getAudioDuration(path)
                                            uploadVoiceFile(context, path, groupCode, username, duration) {
                                                showVoiceInterface = false
                                            }
                                        }
                                    }
                                ) {
                                    Text("Send ($recordingDuration s)")
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎤",
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clickable(enabled = !isUploading && !isVideoUploading) {
                                        showVoiceInterface = true
                                    }
                                    .padding(8.dp)
                                    .align(Alignment.CenterVertically)
                            )

                            Text(
                                text = "📷🎥",
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clickable(enabled = !isUploading && !isVideoUploading) {
                                        galleryLauncher.launch("*/*")
                                    }
                                    .padding(8.dp)
                                    .align(Alignment.CenterVertically)
                            )

                            TextField(
                                value = text,
                                onValueChange = { text = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Type a message...") },
                                shape = MaterialTheme.shapes.large,
                                enabled = !isUploading && !isVideoUploading && !showVoiceInterface,
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )

                            Button(
                                onClick = {
                                    if (text.isNotBlank()) {
                                        val socket = SocketManager.getSocket()
                                        if (socket != null && socket.connected()) {
                                            socket.emit("sendMessage", groupCode, text, username, null, null, null, null)
                                            text = ""
                                        } else {
                                            Toast.makeText(context, "Not connected to server", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp),
                                shape = MaterialTheme.shapes.large,
                                enabled = !isUploading && !isVideoUploading && !showVoiceInterface
                            ) {
                                Text("Send")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMembersDialog) {
        GroupMembersDialog(
            groupCode = groupCode,
            onDismiss = { showMembersDialog = false }
        )
    }
}