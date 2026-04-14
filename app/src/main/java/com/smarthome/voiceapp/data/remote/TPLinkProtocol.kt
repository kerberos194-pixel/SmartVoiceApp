package com.smarthome.voiceapp.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
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
        private const val DISCOVERY_PORT = 9999
        private val KEY = "lski*8J0gO8lWF@".toByteArray()
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
            socket.soTimeout = 3000
            val packet = DatagramPacket(encrypted, encrypted.size, InetAddress.getByName(ip), PORT)
            socket.send(packet)
            val response = ByteArray(4096)
            socket.receive(DatagramPacket(response, response.size))
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun discoverDevices(): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<DiscoveredDevice>()
        
        try {
            val socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 5000
            
            val broadcastAddress = InetAddress.getByName("255.255.255.255")
            val discoveryCmd = """{"system":{"get_sysinfo":{}}}"""
            val encrypted = encrypt(discoveryCmd)
            
            val sendPacket = DatagramPacket(encrypted, encrypted.size, broadcastAddress, DISCOVERY_PORT)
            socket.send(sendPacket)
            
            val seenIPs = mutableSetOf<String>()
            val endTime = System.currentTimeMillis() + 5000
            
            while (System.currentTimeMillis() < endTime) {
                try {
                    val buffer = ByteArray(4096)
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(receivePacket)
                    
                    val ip = receivePacket.address.hostAddress
                    if (ip != null && !seenIPs.contains(ip)) {
                        seenIPs.add(ip)
                        
                        val data = receivePacket.data.copyOf(receivePacket.length)
                        val json = decrypt(data)
                        val info = gson.fromJson(json, SysInfo::class.java)
                        
                        devices.add(DiscoveredDevice(
                            deviceId = info.deviceId ?: ip,
                            alias = info.alias ?: "Unknown",
                            ipAddress = ip,
                            relayState = info.relayState ?: 0,
                            model = info.model ?: ""
                        ))
                    }
                } catch (e: Exception) {
                    // Continue listening
                }
            }
            
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        devices
    }
}

data class DiscoveredDevice(
    val deviceId: String,
    val alias: String,
    val ipAddress: String,
    val relayState: Int,
    val model: String
)

data class SysInfo(
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("alias") val alias: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("relay_state") val relayState: Int?,
    @SerializedName("mac") val mac: String?
)
