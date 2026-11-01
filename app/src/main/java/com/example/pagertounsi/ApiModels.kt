package com.example.pagertounsi

// Request Models
data class SignupRequest(
    val email: String,
    val username: String,
    val password: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class EmailRequest(
    val email: String
)

data class VerifyRequest(
    val email: String,
    val code: String
)

data class GroupMemberRequest(
    val groupCode: String,
    val username: String
)

data class UsernameRequest(
    val username: String
)

// New request for getting group members
data class GroupCodeRequest(
    val groupCode: String
)

// FCM Request Models
data class FcmTokenRequest(
    val username: String,
    val token: String
)

data class FcmMessageRequest(
    val groupCode: String,
    val username: String,
    val message: String,
    val photoUrl: String? = null,
    val targetUsername: String? = null
)

// Response Models
data class ApiResponse(
    val status: String,
    val message: String? = null
)

data class SignupResponse(
    val status: String,
    val email: String? = null,
    val message: String? = null
)

data class LoginResponse(
    val status: String,
    val email: String? = null,
    val message: String? = null
)

data class GroupResponse(
    val status: String,
    val code: String? = null
)

data class GroupsResponse(
    val status: String,
    val groups: List<String> = emptyList()
)

// New response models for group members
data class GroupMemberInfo(
    val username: String,
    val joinedAt: String,
    val isOnline: Boolean
)

data class GroupMembersResponse(
    val status: String,
    val members: List<GroupMemberInfo> = emptyList(),
    val count: Int = 0,
    val message: String? = null
)

data class MessageResponse(
    val groupCode: String,
    val username: String,
    val message: String,
    val videoUrl: String? = null,
    val photoUrl: String? = null,
    val voiceUrl: String? = null,
    val duration: Int? = null,
    val createdAt: String
)

data class PhotoUploadResponse(
    val status: String,
    val photoUrl: String? = null,
    val filename: String? = null,
    val message: String? = null
)

data class VoiceUploadResponse(
    val status: String,
    val voiceUrl: String? = null,
    val filename: String? = null,
    val message: String? = null
)

// Message model for chat
data class ChatMessageData(
    val username: String,
    val message: String = "",
    val photoUrl: String? = null,
    val voiceUrl: String? = null,
    val videoUrl: String? = null,
    val duration: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class VideoUploadResponse(
    val status: String,
    val videoUrl: String? = null,
    val filename: String? = null,
    val message: String? = null
)