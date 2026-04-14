package com.smarthome.voiceapp.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class TPLinkProtocol {
    companion object {
        private const val PORT = 9999
        private val KEY = "lski*8J0gO8lWF@".toByteArray()
        private const val TAG = "TPLinkProtocol"
    }
    
    private val gson = Gson()
    
    private fun encrypt(data: String): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(KEY, "AES"))
        return cipher.doFinal(data.toByteArray())
    }
    
    private fun decrypt(data: ByteArray): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(KEY, "AES"))
        return String(cipher.doFinal(data))
    }
    
    suspend fun sendCommand(ip: String, state: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val cmd = """{"system":{"set_relay_state":{"state":$state}}}"""
            val encrypted = encrypt(cmd)
            val socket = DatagramSocket()
            socket.soTimeout = 2000
            val packet = DatagramPacket(encrypted, encrypted.size, InetAddress.getByName(ip), PORT)
            socket.send(packet)
            socket.close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "sendCommand failed: ${e.message}")
            false
        }
    }
    
    suspend fun getDeviceInfo(ip: String): DiscoveredDevice? = withContext(Dispatchers.IO) {
        try {
            val cmd = """{"system":{"get_sysinfo":{}}}"""
            val encrypted = encrypt(cmd)
            val socket = DatagramSocket()
            socket.soTimeout = 2000
            val packet = DatagramPacket(encrypted, encrypted.size, InetAddress.getByName(ip), PORT)
            socket.send(packet)
            val response = ByteArray(4096)
            val responsePacket = DatagramPacket(response, response.size)
            socket.receive(responsePacket)
            socket.close()
            
            val dataLength = responsePacket.length
            val json = decrypt(response.copyOf(dataLength))
            val info = gson.fromJson(json, SysInfo::class.java)
            
            DiscoveredDevice(
                deviceId = info.deviceId ?: ip,
                alias = info.alias ?: "Device $ip",
                ipAddress = ip,
                relayState = info.relayState ?: 0,
                model = info.model ?: "",
                mac = info.mac ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "getDeviceInfo failed for $ip: ${e.message}")
            null
        }
    }
    
    suspend fun scanNetwork(baseIP: String): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<DiscoveredDevice>()
        
        val parts = baseIP.split(".")
        if (parts.size != 4) {
            Log.e(TAG, "Invalid IP format: $baseIP")
            return@withContext devices
        }
        
        val base = "${parts[0]}.${parts[1]}.${parts[2]}."
        
        val socket = try {
            DatagramSocket(0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create socket: ${e.message}")
            return@withContext devices
        }
        
        try {
            socket.soTimeout = 5000
            socket.broadcast = true
            
            val cmd = """{"system":{"get_sysinfo":{}}}"""
            val encrypted = encrypt(cmd)
            
            Log.d(TAG, "Scanning network: $base*")
            
            for (i in 1..254) {
                val ip = "$base$i"
                try {
                    val packet = DatagramPacket(encrypted.copyOf(), encrypted.size, InetAddress.getByName(ip), PORT)
                    socket.send(packet)
                } catch (e: Exception) {
                    // Skip individual send errors
                }
            }
            
            Log.d(TAG, "Waiting for responses...")
            val endTime = System.currentTimeMillis() + 5000
            
            while (System.currentTimeMillis() < endTime) {
                try {
                    val response = ByteArray(4096)
                    val packet = DatagramPacket(response, response.size)
                    socket.receive(packet)
                    
                    val dataLength = packet.length
                    val json = decrypt(response.copyOf(dataLength))
                    val info = gson.fromJson(json, SysInfo::class.java)
                    
                    val deviceIP = packet.address.hostAddress ?: continue
                    
                    Log.d(TAG, "Found device: ${info.alias} at $deviceIP (MAC: ${info.mac})")
                    
                    val device = DiscoveredDevice(
                        deviceId = info.deviceId ?: deviceIP,
                        alias = info.alias ?: "Device $deviceIP",
                        ipAddress = deviceIP,
                        relayState = info.relayState ?: 0,
                        model = info.model ?: "",
                        mac = info.mac ?: ""
                    )
                    
                    if (device.ipAddress.isNotEmpty() && !devices.any { it.ipAddress == device.ipAddress }) {
                        devices.add(device)
                    }
                } catch (e: Exception) {
                    // Continue waiting for more responses
                }
            }
            
            Log.d(TAG, "Scan complete. Found ${devices.size} devices")
            
        } finally {
            socket.close()
        }
        
        devices
    }
}

data class DiscoveredDevice(
    val deviceId: String,
    val alias: String,
    val ipAddress: String,
    val relayState: Int,
    val model: String,
    val mac: String = ""
)

data class SysInfo(
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("alias") val alias: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("relay_state") val relayState: Int?,
    @SerializedName("mac") val mac: String?
)
