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
        
        val ip = device.ipAddress ?: return CommandResult.Error("No IP")
        val success = tplinkProtocol.sendCommand(ip, 1)
        
        return if (success) {
            CommandResult.Success("${device.name} turned on")
        } else {
            CommandResult.Error("Failed to turn on")
        }
    }
    
    override suspend fun turnOff(deviceId: String): CommandResult {
        val device = database.deviceDao().getById(deviceId) 
            ?: return CommandResult.Error("Device not found")
        
        val ip = device.ipAddress ?: return CommandResult.Error("No IP")
        val success = tplinkProtocol.sendCommand(ip, 0)
        
        return if (success) {
            CommandResult.Success("${device.name} turned off")
        } else {
            CommandResult.Error("Failed to turn off")
        }
    }
    
    override suspend fun discoverDevices(): List<Device> {
        return emptyList()
    }
    
    private fun DeviceEntity.toDomain() = Device(
        id = id, name = name, type = DeviceType.valueOf(type),
        room = room, isOn = isOn, brightness = brightness, ipAddress = ipAddress
    )
}
