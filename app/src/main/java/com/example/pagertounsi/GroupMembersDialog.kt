package com.example.pagertounsi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupMembersDialog(
    groupCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var members by remember { mutableStateOf<List<GroupMemberInfo>>(emptyList()) }
    var memberCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Define loadMembers function FIRST before using it
    fun loadMembers() {
        isLoading = true
        ApiClient.api.getGroupMembers(GroupCodeRequest(groupCode))
            .enqueue(object : retrofit2.Callback<GroupMembersResponse> {
                override fun onResponse(
                    call: retrofit2.Call<GroupMembersResponse>,
                    response: retrofit2.Response<GroupMembersResponse>
                ) {
                    isLoading = false
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.status == "ok") {
                            members = body.members
                            memberCount = body.count
                        } else {
                            errorMessage = body?.message ?: "Failed to load members"
                        }
                    } else {
                        errorMessage = "Error: ${response.code()}"
                    }
                }

                override fun onFailure(call: retrofit2.Call<GroupMembersResponse>, t: Throwable) {
                    isLoading = false
                    errorMessage = "Network error: ${t.message}"
                }
            })
    }

    // Load members when dialog opens - NOW loadMembers is defined
    LaunchedEffect(Unit) {
        loadMembers()
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = MaterialTheme.shapes.large
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
                    Column {
                        Text(
                            "Group Members",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Group: $groupCode • $memberCount members",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(members) { member ->
                            MemberItem(member)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Online count summary
                if (!isLoading && members.isNotEmpty()) {
                    val onlineCount = members.count { it.isOnline }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "🟢 Online",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "$onlineCount/${members.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemberItem(member: GroupMemberInfo) {
    val joinedDate = remember(member.joinedAt) {
        try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val date = format.parse(member.joinedAt)
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            "Unknown date"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (member.isOnline)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Online indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (member.isOnline) Color.Green else Color.Gray,
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Username
                Text(
                    member.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (member.isOnline)
                        FontWeight.Bold
                    else FontWeight.Normal
                )
            }

            // Joined date
            Text(
                "Joined $joinedDate",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}