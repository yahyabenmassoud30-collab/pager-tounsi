package com.example.pagertounsi

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("/signup")
    fun signup(@Body request: SignupRequest): Call<SignupResponse>

    @POST("/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("/sendVerification")
    fun sendVerification(@Body request: EmailRequest): Call<ApiResponse>

    @POST("/verifyCode")
    fun verifyCode(@Body request: VerifyRequest): Call<ApiResponse>

    @POST("/resendVerification")
    fun resendVerification(@Body request: EmailRequest): Call<ApiResponse>

    @POST("/checkVerification")
    fun checkVerification(@Body request: EmailRequest): Call<ApiResponse>

    @POST("/createGroup")
    fun createGroup(): Call<GroupResponse>

    @GET("/messages/{groupCode}")
    fun getMessages(@Path("groupCode") groupCode: String): Call<List<MessageResponse>>

    @POST("/joinGroup")
    fun joinGroup(@Body request: GroupMemberRequest): Call<ApiResponse>

    @POST("/leaveGroup")
    fun leaveGroup(@Body request: GroupMemberRequest): Call<ApiResponse>

    @POST("/getUserGroups")
    fun getUserGroups(@Body request: UsernameRequest): Call<GroupsResponse>

    // New endpoint for getting group members
    @POST("/getGroupMembers")
    fun getGroupMembers(@Body request: GroupCodeRequest): Call<GroupMembersResponse>

    @Multipart
    @POST("/uploadPhoto")
    fun uploadPhoto(@Part photo: MultipartBody.Part): Call<PhotoUploadResponse>

    @Multipart
    @POST("/uploadVoice")
    fun uploadVoice(@Part audio: MultipartBody.Part): Call<VoiceUploadResponse>

    // FCM Endpoints
    @POST("/updateFcmToken")
    fun updateFcmToken(@Body request: FcmTokenRequest): Call<ApiResponse>

    @POST("/sendFcmMessage")
    fun sendFcmMessage(@Body request: FcmMessageRequest): Call<ApiResponse>

    @Multipart
    @POST("/uploadVideo")
    fun uploadVideo(@Part video: MultipartBody.Part): Call<VideoUploadResponse>
}