package com.example.pagertounsi

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(nav: NavController) {

    Log.d("GroupScreen", "========== GroupScreen Started ==========")

    var groupCode by remember { mutableStateOf("") }
    var myGroups by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var username by remember { mutableStateOf("") }

    // Update dialog state
    var showUpdateDialog by remember { mutableStateOf(false) }
    var versionInfo by remember { mutableStateOf<SimpleVersionInfo?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val versionChecker = remember { VersionChecker(context) }
    val updateDownloader = remember { UpdateDownloader(context) }

    // Define loadUserGroups function
    fun loadUserGroups(username: String) {
        Log.d("GroupScreen", "Loading groups for user: $username")
        isLoading = true
        ApiClient.api.getUserGroups(UsernameRequest(username))
            .enqueue(object : Callback<GroupsResponse> {
                override fun onResponse(
                    call: Call<GroupsResponse>,
                    response: Response<GroupsResponse>
                ) {
                    isLoading = false
                    if (response.isSuccessful) {
                        val groups = response.body()?.groups ?: emptyList()
                        Log.d("GroupScreen", "✅ Loaded ${groups.size} groups")
                        myGroups = groups
                        errorMessage = null
                    } else {
                        Log.e("GroupScreen", "❌ Failed to load groups: ${response.code()}")
                        errorMessage = "Failed to load groups"
                    }
                }

                override fun onFailure(call: Call<GroupsResponse>, t: Throwable) {
                    isLoading = false
                    Log.e("GroupScreen", "❌ Network error loading groups: ${t.message}")
                    errorMessage = "Network error: ${t.message}"
                }
            })
    }

    // Define leaveGroup function
    fun leaveGroup(groupCode: String) {
        Log.d("GroupScreen", "Leaving group: $groupCode")
        ApiClient.api.leaveGroup(GroupMemberRequest(groupCode, username))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == "ok") {
                        Log.d("GroupScreen", "✅ Successfully left group: $groupCode")
                        Toast.makeText(context, "Left group $groupCode", Toast.LENGTH_SHORT).show()
                        // Refresh the list
                        loadUserGroups(username)
                    } else {
                        Log.e("GroupScreen", "❌ Failed to leave group: $groupCode")
                        Toast.makeText(context, "Failed to leave group", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e("GroupScreen", "❌ Network error leaving group: ${t.message}")
                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Define checkForUpdates function with detailed logging
    suspend fun checkForUpdates() {
        Log.d("GroupScreen", "🔍 === Checking for updates ===")
        Log.d("GroupScreen", "   Using VersionChecker: ${versionChecker}")

        val info = withContext(Dispatchers.IO) {
            Log.d("GroupScreen", "   Fetching version info from server...")
            versionChecker.checkForNewVersion()
        }

        if (info != null) {
            Log.d("GroupScreen", "✅ SUCCESS - Got version info from server:")
            Log.d("GroupScreen", "   📱 Latest version: ${info.latestVersion}")
            Log.d("GroupScreen", "   🔢 Latest version code: ${info.latestVersionCode}")
            Log.d("GroupScreen", "   🔗 Update URL: ${info.updateUrl}")
            Log.d("GroupScreen", "   📝 Release notes: ${info.releaseNotes}")

            val currentCode = versionChecker.getCurrentVersionCode()
            Log.d("GroupScreen", "   📱 Current app version code: $currentCode")

            if (currentCode < info.latestVersionCode) {
                Log.d("GroupScreen", "🎉🎉🎉 NEW VERSION AVAILABLE! Showing update dialog.")
                Log.d("GroupScreen", "   Current: $currentCode < Latest: ${info.latestVersionCode}")
                versionInfo = info
                showUpdateDialog = true
            } else {
                Log.d("GroupScreen", "✅ App is up to date (current: $currentCode, latest: ${info.latestVersionCode})")
            }
        } else {
            Log.e("GroupScreen", "❌❌❌ FAILED to get version info from server!")
            Log.e("GroupScreen", "   Check: Server running? JSON file exists? URL accessible?")
            Log.e("GroupScreen", "   URL: http://pagertn.viewdns.net:3000/app-version.json")
        }
        Log.d("GroupScreen", "🔍 === Update check complete ===\n")
    }

    // Load username and user's groups
    LaunchedEffect(Unit) {
        Log.d("GroupScreen", "🚀 LaunchedEffect started")
        try {
            // Get username from DataStore
            Log.d("GroupScreen", "   Loading username from DataStore...")
            val dataStoreKey = stringPreferencesKey("username")
            val preferences = context.dataStore.data.first()
            username = preferences[dataStoreKey] ?: ""

            Log.d("GroupScreen", "   Username: '${username}'")

            if (username.isNotEmpty()) {
                Log.d("GroupScreen", "   Checking for updates...")
                checkForUpdates()

                Log.d("GroupScreen", "   Loading user groups...")
                loadUserGroups(username)
            } else {
                Log.e("GroupScreen", "❌ User not logged in")
                errorMessage = "User not logged in"
                isLoading = false
            }
        } catch (e: Exception) {
            Log.e("GroupScreen", "❌ Error in LaunchedEffect: ${e.message}")
            e.printStackTrace()
            errorMessage = "Error loading user data: ${e.message}"
            isLoading = false
        }
        Log.d("GroupScreen", "🏁 LaunchedEffect completed\n")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Groups") },
                actions = {
                    IconButton(
                        onClick = {
                            Log.d("GroupScreen", "🔄 Refresh button clicked")
                            if (username.isNotEmpty()) {
                                scope.launch {
                                    Log.d("GroupScreen", "   Manual refresh - checking updates")
                                    checkForUpdates()
                                    Log.d("GroupScreen", "   Manual refresh - reloading groups")
                                    loadUserGroups(username)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Join Group Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Join New Group",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = groupCode,
                        onValueChange = { groupCode = it },
                        label = { Text("Enter Group Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (groupCode.isNotBlank()) {
                                Log.d("GroupScreen", "Joining group: $groupCode")
                                nav.navigate("chat/$groupCode")
                            } else {
                                Toast.makeText(context, "Please enter a group code", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Join Group")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // My Groups Section
            Text(
                "My Groups",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else if (myGroups.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "📭",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "You haven't joined any groups yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(myGroups) { group ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Group Info
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "Group: $group",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Tap to open",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Action Buttons
                                Row {
                                    // Open Chat Button
                                    Button(
                                        onClick = {
                                            Log.d("GroupScreen", "Opening group: $group")
                                            nav.navigate("chat/$group")
                                        },
                                        modifier = Modifier.padding(end = 8.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Open")
                                    }

                                    // Leave Group Button
                                    OutlinedButton(
                                        onClick = {
                                            leaveGroup(group)
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ExitToApp,
                                            contentDescription = "Leave",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Leave")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Update Dialog
    if (showUpdateDialog && versionInfo != null) {
        Log.d("GroupScreen", "📢 Showing update dialog for version ${versionInfo!!.latestVersion}")
        SimpleUpdateDialog(
            versionInfo = versionInfo!!,
            onDownload = {
                Log.d("GroupScreen", "⬇️ User clicked Download for version ${versionInfo!!.latestVersion}")
                updateDownloader.downloadApk(versionInfo!!.updateUrl)
                showUpdateDialog = false
            },
            onDismiss = {
                Log.d("GroupScreen", "❌ User dismissed update dialog")
                showUpdateDialog = false
            }
        )
    }
}