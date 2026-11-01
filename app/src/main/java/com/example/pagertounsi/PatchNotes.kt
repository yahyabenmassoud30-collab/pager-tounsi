package com.example.pagertounsi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import kotlinx.coroutines.flow.first



data class PatchNote(
    val version: String,
    val date: String,
    val changes: List<String>
)

@Composable
fun PatchNotesDialog(
    onDismiss: () -> Unit
) {
    // Define your patch notes here - you can edit these whenever you want
    val patchNotes = listOf(
        PatchNote(
            version = "V1.2.0",
            date = "March 14, 2026",
            changes = listOf(

                "•added video sending",
                "•added a video player"
            )
        ),
        PatchNote(
            version = "V1.1.0",
            date = "March 14, 2026",
            changes = listOf(

                "•voice messages are now out",
                "•group members and group members status are out"
            )
        ),
        PatchNote(
            version = "V1.0",
            date = "March 13, 2026",
            changes = listOf(

                "•enhanced notification managing",
                "•added a last seen protocol for notification managing"
            )
        ),
        PatchNote(
            version = "v0.9.9.8",
            date = "March 13, 2026",
            changes = listOf(

                "• added patch notes :)"
            )
        ),
        PatchNote(
            version = "v0.9.9",
            date = "March 13, 2026",
            changes = listOf(

                "• Fixed message duplication bug",
                "• Improved app stability",
                "• Better error handling",
                "• UI/UX enhancements"
            )
        ),
        PatchNote(
            version = "v0.9.0",
            date = "March 13, 2026",
            changes = listOf(
                "• Push notifications added",
                "• Background message receiving even when app is closed",
                "added group list and saved joined group history",
                "• Added leave group functionality",
                "• Performance improvements"
            )
        ),
        PatchNote(
            version = "v0.7",
            date = "March 12, 2026",
            changes = listOf(
                "• added photo uploading",
                "• added icon from gallery near messages sending input",
                "• enhanced real time chat with socket.io",
            )
        )



        // Add more patch notes here as you update the app
    )

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📋 What's New",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Patch Notes List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(patchNotes) { note ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            // Version and date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    note.version,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    note.date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Changes list
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                note.changes.forEach { change ->
                                    Text(
                                        change,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            // Divider between versions
                            if (note != patchNotes.last()) {
                                Divider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Got it!")
                }
            }
        }
    }
}

// DataStore keys
object PatchNotesPrefs {
    val LAST_SEEN_VERSION = booleanPreferencesKey("last_seen_version")
    val CURRENT_VERSION = "v1.2.0" // Update this when you release new versions
}