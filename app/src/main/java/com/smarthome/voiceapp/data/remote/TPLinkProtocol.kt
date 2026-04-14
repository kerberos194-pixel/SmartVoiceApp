package com.smarthome.voiceapp.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import com.google.gson.Gson

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
            false
        }
    }
}
