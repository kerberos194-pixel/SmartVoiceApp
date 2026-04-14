package com.smarthome.voiceapp.domain.usecase

import com.smarthome.voiceapp.domain.model.*
import com.smarthome.voiceapp.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ProcessVoiceCommandUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(text: String): CommandResult {
        val cmd = parseCommand(text.lowercase())
        val devices = deviceRepository.getDevices().first()
        
        if (cmd.action == CommandAction.UNKNOWN) {
            return CommandResult.Error("No understood")
        }
        
        val device = devices.firstOrNull() ?: return CommandResult.Error("No devices found")
        
        return when (cmd.action) {
            CommandAction.TURN_ON -> deviceRepository.turnOn(device.id)
            CommandAction.TURN_OFF -> deviceRepository.turnOff(device.id)
            CommandAction.STATUS -> CommandResult.Success("${device.name} is ${if (device.isOn) "on" else "off"}")
            else -> CommandResult.Error("Not supported")
        }
    }
    
    private fun parseCommand(text: String): VoiceCommand {
        val action = when {
            text.contains("enciende") || text.contains("turn on") -> CommandAction.TURN_ON
            text.contains("apaga") || text.contains("turn off") -> CommandAction.TURN_OFF
            text.contains("estado") || text.contains("status") -> CommandAction.STATUS
            else -> CommandAction.UNKNOWN
        }
        
        val deviceType = when {
            text.contains("luz") || text.contains("light") -> "light"
            text.contains("enchufe") || text.contains("plug") -> "plug"
            else -> null
        }
        
        return VoiceCommand(action, deviceType, null, null)
    }
}
