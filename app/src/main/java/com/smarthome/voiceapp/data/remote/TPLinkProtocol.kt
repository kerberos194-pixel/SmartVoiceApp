package com.smarthome.voiceapp.data.remote

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
            socket.receive(DatagramPacket(response, response.size))
            socket.close()
            
            val json = decrypt(response.copyOf(response.size))
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
            null
        }
    }
    
    suspend fun scanNetwork(baseIP: String): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<DiscoveredDevice>()
        
        val parts = baseIP.split(".")
        if (parts.size != 4) return@withContext devices
        
        val base = "${parts[0]}.${parts[1]}.${parts[2]}."
        
        val socket = try {
            DatagramSocket(PORT)
        } catch (e: Exception) {
            return@withContext devices
        }
        socket.broadcast = true
        socket.soTimeout = 3000
        
        for (i in 1..254) {
            val ip = "$base$i"
            try {
                val cmd = """{"system":{"get_sysinfo":{}}}"""
                val encrypted = encrypt(cmd)
                val packet = DatagramPacket(encrypted, encrypted.size, InetAddress.getByName(ip), PORT)
                socket.send(packet)
            } catch (e: Exception) {
                // Skip
            }
        }
        
        val endTime = System.currentTimeMillis() + 3000
        while (System.currentTimeMillis() < endTime) {
            try {
                val response = ByteArray(4096)
                val packet = DatagramPacket(response, response.size)
                socket.receive(packet)
                
                val json = decrypt(response.copyOf(packet.length))
                val info = gson.fromJson(json, SysInfo::class.java)
                
                val device = DiscoveredDevice(
                    deviceId = info.deviceId ?: packet.address.hostAddress ?: "unknown",
                    alias = info.alias ?: "Device ${packet.address.hostAddress}",
                    ipAddress = packet.address.hostAddress ?: "",
                    relayState = info.relayState ?: 0,
                    model = info.model ?: "",
                    mac = info.mac ?: ""
                )
                
                if (device.ipAddress.isNotEmpty()) {
                    devices.add(device)
                }
            } catch (e: Exception) {
                // Timeout or parse error
            }
        }
        
        socket.close()
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
