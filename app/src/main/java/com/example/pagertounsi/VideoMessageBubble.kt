package com.example.pagertounsi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.widget.VideoView
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color

@Composable
fun VideoMessageBubble(
    videoUrl: String,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var showVideoPlayer by remember { mutableStateOf(false) }
    var videoFinished by remember { mutableStateOf(false) }
    var videoWidth by remember { mutableStateOf(0) }
    var videoHeight by remember { mutableStateOf(0) }
    val videoViewRef = remember { mutableStateOf<VideoView?>(null) }

    // Calculate aspect ratio for dynamic sizing
    val aspectRatio = if (videoHeight > 0) videoWidth.toFloat() / videoHeight else 16f/9f

    // Max height for videos (to keep consistency with other bubbles)
    val maxVideoHeight = 300.dp

    // Calculate dynamic height based on aspect ratio
    val dynamicHeight = if (showVideoPlayer) {
        (maxVideoHeight).coerceAtMost(300.dp)  // Cap at 300dp
    } else {
        0.dp
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        // Thumbnail/Preview - matches other message bubbles
        Surface(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 280.dp)
                .clickable {
                    showVideoPlayer = !showVideoPlayer
                    videoFinished = false
                },
            color = if (isCurrentUser)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (videoFinished) Icons.Default.Replay else Icons.Default.PlayArrow,
                    contentDescription = if (videoFinished) "Replay Video" else "Play Video",
                    tint = if (isCurrentUser)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        if (videoFinished) "📹 Video (Replay)" else "📹 Video",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCurrentUser)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (videoFinished) "Tap to watch again" else "Tap to play",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentUser)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Video Player - dynamic sizing when open
        if (showVideoPlayer) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                // Video container with dynamic height
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dynamicHeight),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black
                ) {
                    AndroidView(
                        factory = { ctx ->
                            FrameLayout(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                VideoView(ctx).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )

                                    setVideoURI(Uri.parse(videoUrl))

                                    // Get video dimensions when prepared
                                    setOnPreparedListener { mp ->
                                        videoWidth = mp.videoWidth
                                        videoHeight = mp.videoHeight
                                        start()
                                        isPlaying = true
                                        videoFinished = false
                                    }

                                    val mediaController = MediaController(ctx)
                                    mediaController.setAnchorView(this)
                                    setMediaController(mediaController)

                                    setOnCompletionListener {
                                        isPlaying = false
                                        videoFinished = true
                                    }

                                    videoViewRef.value = this
                                    addView(this)
                                }
                            }
                        },
                        update = { }
                    )
                }

                // Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = {
                            videoViewRef.value?.stopPlayback()
                            showVideoPlayer = false
                        }
                    ) {
                        Text("Close")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            videoViewRef.value?.seekTo(0)
                            videoViewRef.value?.start()
                            videoFinished = false
                            isPlaying = true
                        },
                        enabled = videoFinished
                    ) {
                        Text("Replay")
                    }
                }
            }
        }
    }
}