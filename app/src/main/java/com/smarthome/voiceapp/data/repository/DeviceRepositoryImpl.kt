package com.smarthome.voiceapp.data.repository

import com.smarthome.voiceapp.data.local.AppDatabase
import com.smarthome.voiceapp.data.local.DeviceEntity
import com.smarthome.voiceapp.data.remote.TPLinkProtocol
import com.smarthome.voiceapp.domain.model.*
import com.smarthome.voiceapp.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DeviceRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val tplinkProtocol: TPLinkProtocol
) : DeviceRepository {
    
    override fun getDevices(): Flow<List<Device>> {
        return database.deviceDao().getAll().map { list ->
            list.map { it.toDomain() }
        }
    }
    
    override suspend fun turnOn(deviceId: String): CommandResult {
        val device = database.deviceDao().getById(deviceId) 
            ?: return CommandResult.Error("Device not found")
        
        val ip = device.ipAddress ?: return CommandResult.Error("No IP address")
        val success = tplinkProtocol.sendCommand(ip, 1)
        
        return if (success) {
            database.deviceDao().insert(device.copy(isOn = true))
            CommandResult.Success("${device.name} turned on")
        } else {
            CommandResult.Error("Failed - check device is online")
        }
    }
    
    override suspend fun turnOff(deviceId: String): CommandResult {
        val device = database.deviceDao().getById(deviceId) 
            ?: return CommandResult.Error("Device not found")
        
        val ip = device.ipAddress ?: return CommandResult.Error("No IP address")
        val success = tplinkProtocol.sendCommand(ip, 0)
        
        return if (success) {
            database.deviceDao().insert(device.copy(isOn = false))
            CommandResult.Success("${device.name} turned off")
        } else {
            CommandResult.Error("Failed - check device is online")
        }
    }
    
    override suspend fun discoverDevices(): List<Device> {
        return emptyList()
    }
    
    suspend fun addDeviceByIP(ip: String): CommandResult {
        val device = tplinkProtocol.getDeviceInfo(ip)
        
        if (device == null) {
            return CommandResult.Error("No device found at $ip")
        }
        
        val entity = DeviceEntity(
            id = device.deviceId,
            name = device.alias,
            type = determineType(device.model),
            room = null,
            isOn = device.relayState == 1,
            brightness = null,
            ipAddress = device.ipAddress
        )
        
        database.deviceDao().insert(entity)
        
        return CommandResult.Success("Device '${device.alias}' added successfully")
    }
    
    private fun determineType(model: String): String {
        return when {
            model.contains("LB", ignoreCase = true) -> "LIGHT"
            model.contains("LB100", ignoreCase = true) -> "LIGHT"
            model.contains("HS110", ignoreCase = true) -> "PLUG"
            model.contains("HS100", ignoreCase = true) -> "SWITCH"
            model.contains("KB", ignoreCase = true) -> "LIGHT"
            model.contains("KL", ignoreCase = true) -> "LIGHT"
            else -> "LIGHT"
        }
    }
    
    private fun DeviceEntity.toDomain() = Device(
        id = id,
        name = name,
        type = try { DeviceType.valueOf(type) } catch (e: Exception) { DeviceType.UNKNOWN },
        room = room,
        isOn = isOn,
        brightness = brightness,
        ipAddress = ipAddress
    )
}
