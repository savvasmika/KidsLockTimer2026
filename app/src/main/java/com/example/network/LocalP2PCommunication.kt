package com.example.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

sealed class P2PMessage {
    data class PairRequest(
        val parentDeviceId: String,
        val parentName: String,
        val confirmationCode: String,
        val parentPublicKey: String = "",
        val nonce: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val signature: String = "",
        val returnIp: String,
        val returnPort: Int
    ) : P2PMessage()

    data class PairResponse(
        val childDeviceId: String,
        val childName: String,
        val confirmationCode: String,
        val childPublicKey: String = "",
        val authToken: String,
        val nonce: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val signature: String = "",
        val accepted: Boolean
    ) : P2PMessage()

    data class UnlockRequestMsg(
        val requestId: String,
        val childDeviceId: String,
        val childName: String,
        val requestedMinutes: Int,
        val nonce: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val signature: String = ""
    ) : P2PMessage()

    data class UnlockCommandMsg(
        val targetDeviceId: String,
        val authToken: String,
        val isLocked: Boolean,
        val grantedMinutes: Int,
        val nonce: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val signature: String = ""
    ) : P2PMessage()

    data class ThemeChangeMsg(
        val targetDeviceId: String,
        val authToken: String,
        val themeId: String,
        val nonce: String = "",
        val timestamp: Long = System.currentTimeMillis()
    ) : P2PMessage()

    data class HeartbeatMsg(
        val childDeviceId: String,
        val isLocked: Boolean,
        val batteryPercent: Int,
        val activeThemeId: String
    ) : P2PMessage()
}

class LocalP2PCommunication {

    companion object {
        private const val TAG = "KidLock_P2P"
        const val SERVER_PORT = 8899
    }

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _incomingMessages = MutableSharedFlow<P2PMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<P2PMessage> = _incomingMessages.asSharedFlow()

    fun startServer(port: Int = SERVER_PORT) {
        if (isRunning) return
        isRunning = true

        scope.launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                Log.d(TAG, "P2P Server started on port $port")

                while (isRunning && serverSocket?.isClosed == false) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        handleClientConnection(client)
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.w(TAG, "Socket accept error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not start P2P server on port $port", e)
            } finally {
                stopServer()
            }
        }
    }

    private fun handleClientConnection(client: Socket) {
        scope.launch {
            try {
                client.use { socket ->
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val writer = PrintWriter(socket.getOutputStream(), true)

                    val line = reader.readLine()
                    if (line != null) {
                        Log.d(TAG, "Received message: $line")
                        val json = JSONObject(line)
                        val type = json.optString("type")

                        when (type) {
                            "PAIR_REQUEST" -> {
                                val msg = P2PMessage.PairRequest(
                                    parentDeviceId = json.getString("parentDeviceId"),
                                    parentName = json.optString("parentName", "Parent Device"),
                                    confirmationCode = json.getString("code"),
                                    parentPublicKey = json.optString("publicKey", ""),
                                    nonce = json.optString("nonce", ""),
                                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                                    signature = json.optString("signature", ""),
                                    returnIp = socket.inetAddress.hostAddress ?: "",
                                    returnPort = json.optInt("returnPort", SERVER_PORT)
                                )
                                _incomingMessages.emit(msg)
                                writer.println(JSONObject().put("status", "RECEIVED").toString())
                            }

                            "PAIR_RESPONSE" -> {
                                val msg = P2PMessage.PairResponse(
                                    childDeviceId = json.getString("childDeviceId"),
                                    childName = json.getString("childName"),
                                    confirmationCode = json.getString("code"),
                                    childPublicKey = json.optString("publicKey", ""),
                                    authToken = json.getString("authToken"),
                                    nonce = json.optString("nonce", ""),
                                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                                    signature = json.optString("signature", ""),
                                    accepted = json.getBoolean("accepted")
                                )
                                _incomingMessages.emit(msg)
                                writer.println(JSONObject().put("status", "OK").toString())
                            }

                            "UNLOCK_REQUEST" -> {
                                val msg = P2PMessage.UnlockRequestMsg(
                                    requestId = json.getString("requestId"),
                                    childDeviceId = json.getString("childDeviceId"),
                                    childName = json.getString("childName"),
                                    requestedMinutes = json.optInt("minutes", 30),
                                    nonce = json.optString("nonce", ""),
                                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                                    signature = json.optString("signature", "")
                                )
                                _incomingMessages.emit(msg)
                                writer.println(JSONObject().put("status", "RECEIVED").toString())
                            }

                            "UNLOCK_COMMAND" -> {
                                val msg = P2PMessage.UnlockCommandMsg(
                                    targetDeviceId = json.getString("targetDeviceId"),
                                    authToken = json.getString("authToken"),
                                    isLocked = json.getBoolean("isLocked"),
                                    grantedMinutes = json.optInt("grantedMinutes", 0),
                                    nonce = json.optString("nonce", ""),
                                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                                    signature = json.optString("signature", "")
                                )
                                _incomingMessages.emit(msg)
                                writer.println(JSONObject().put("status", "SUCCESS").toString())
                            }

                            "THEME_CHANGE" -> {
                                val msg = P2PMessage.ThemeChangeMsg(
                                    targetDeviceId = json.getString("targetDeviceId"),
                                    authToken = json.getString("authToken"),
                                    themeId = json.getString("themeId"),
                                    nonce = json.optString("nonce", ""),
                                    timestamp = json.optLong("timestamp", System.currentTimeMillis())
                                )
                                _incomingMessages.emit(msg)
                                writer.println(JSONObject().put("status", "SUCCESS").toString())
                            }

                            "HEARTBEAT" -> {
                                val msg = P2PMessage.HeartbeatMsg(
                                    childDeviceId = json.getString("childDeviceId"),
                                    isLocked = json.getBoolean("isLocked"),
                                    batteryPercent = json.optInt("battery", 100),
                                    activeThemeId = json.optString("theme", "space")
                                )
                                _incomingMessages.emit(msg)
                                writer.println(JSONObject().put("status", "ACK").toString())
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client connection", e)
            }
        }
    }

    suspend fun sendMessage(ip: String, port: Int, jsonPayload: JSONObject): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket(ip, port).use { socket ->
                socket.soTimeout = 4000
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                writer.println(jsonPayload.toString())
                val response = reader.readLine()
                Log.d(TAG, "Sent to $ip:$port, response: $response")
                return@withContext response != null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send message to $ip:$port: ${e.message}")
            return@withContext false
        }
    }

    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing server socket", e)
        }
        serverSocket = null
    }
}
