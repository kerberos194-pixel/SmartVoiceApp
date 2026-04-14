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
            socket.soTimeout = 3000
            val packet = DatagramPacket(encrypted, encrypted.size, InetAddress.getByName(ip), PORT)
            socket.send(packet)
            val response = ByteArray(4096)
            socket.receive(DatagramPacket(response, response.size))
            socket.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun pingDevice(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cmd = """{"system":{"get_sysinfo":{}}}"""
            val encrypted = encrypt(cmd)
            val socket = DatagramSocket()
            socket.soTimeout = 2000
            socket.broadcast = true
            val packet = DatagramPacket(encrypted, encrypted.size, InetAddress.getByName(ip), PORT)
            socket.send(packet)
            val response = ByteArray(4096)
            socket.receive(DatagramPacket(response, response.size))
            socket.close()
            val json = decrypt(response.copyOf(response.size))
            json.contains("deviceId")
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
                model = info.model ?: ""
            )
        } catch (e: Exception) {
            null
        }
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
