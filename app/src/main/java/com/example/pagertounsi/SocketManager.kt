package com.example.pagertounsi

import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException
import android.util.Log

object SocketManager {

    private var socket: Socket? = null
    private const val TAG = "SocketManager"

    fun getSocket(): Socket? {
        return socket
    }

    fun connect() {
        try {
            if (socket == null || !socket!!.connected()) {
                Log.d(TAG, "Connecting to socket...")
                val options = IO.Options()
                options.reconnection = true
                options.reconnectionAttempts = 5
                options.reconnectionDelay = 1000

                socket = IO.socket("http://pagertn.viewdns.net:3000", options)

                socket?.on("connect") { args ->
                    Log.d(TAG, "Socket connected successfully")
                }

                socket?.on("connect_error") { args ->
                    Log.d(TAG, "Socket connection error: ${args?.contentToString()}")
                }

                socket?.on("disconnect") { args ->
                    Log.d(TAG, "Socket disconnected")
                }

                socket?.connect()
            }
        } catch (e: URISyntaxException) {
            Log.e(TAG, "URI Syntax Error: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}")
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            socket?.disconnect()
            socket?.off()
            socket = null
            Log.d(TAG, "Socket disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
    }

    fun isConnected(): Boolean {
        return socket?.connected() ?: false
    }
}