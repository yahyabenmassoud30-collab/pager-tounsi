package com.example.pagertounsi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun VoiceMessageBubble(
    voiceUrl: String,
    duration: Int,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var localFilePath by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    val voiceRecorder = remember { VoiceRecorder(context) }

    // Download voice file
    LaunchedEffect(voiceUrl) {
        isLoading = true
        hasError = false

        withContext(Dispatchers.IO) {
            try {
                val fileName = "voice_${voiceUrl.substringAfterLast('/')}"
                val file = File(context.cacheDir, fileName)

                if (file.exists()) {
                    localFilePath = file.absolutePath
                } else {
                    val url = URL(voiceUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.connect()

                    if (connection.responseCode == 200) {
                        val inputStream = connection.inputStream
                        val outputStream = FileOutputStream(file)

                        val buffer = ByteArray(4096)
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }

                        outputStream.close()
                        inputStream.close()

                        if (file.exists() && file.length() > 0) {
                            localFilePath = file.absolutePath
                        } else {
                            hasError = true
                        }
                    } else {
                        hasError = true
                    }
                    connection.disconnect()
                }
            } catch (e: Exception) {
                hasError = true
            }
        }

        isLoading = false
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(min = 160.dp, max = 240.dp),
            color = if (isCurrentUser)
                MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (hasError || localFilePath == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "⚠️",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Play/Pause Button
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    voiceRecorder.stopPlaying()
                                    isPlaying = false
                                    progress = 0f
                                } else {
                                    localFilePath?.let { path ->
                                        voiceRecorder.playAudio(
                                            filePath = path,
                                            onComplete = {
                                                isPlaying = false
                                                progress = 0f
                                            }
                                        )
                                        isPlaying = true

                                        scope.launch {
                                            for (i in 0..duration) {
                                                if (!isPlaying) break
                                                progress = i / duration.toFloat()
                                                delay(1000)
                                            }
                                            progress = 0f
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = if (isCurrentUser)
                                    MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp),
                            color = if (isCurrentUser)
                                MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary,
                            trackColor = if (isCurrentUser)
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Duration
                        Text(
                            "${duration}s",
                            fontSize = 12.sp,
                            color = if (isCurrentUser)
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}