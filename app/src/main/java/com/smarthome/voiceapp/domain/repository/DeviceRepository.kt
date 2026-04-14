package com.smarthome.voiceapp.domain.repository

import com.smarthome.voiceapp.domain.model.CommandResult
import com.smarthome.voiceapp.domain.model.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun getDevices(): Flow<List<Device>>
    suspend fun turnOn(deviceId: String): CommandResult
    suspend fun turnOff(deviceId: String): CommandResult
    suspend fun discoverDevices(): List<Device>
}
