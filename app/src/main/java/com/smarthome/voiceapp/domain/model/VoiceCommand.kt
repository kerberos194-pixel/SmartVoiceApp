package com.smarthome.voiceapp.domain.model

sealed class CommandResult {
    data class Success(val message: String) : CommandResult()
    data class Error(val message: String) : CommandResult()
}

data class VoiceCommand(
    val action: CommandAction,
    val targetDevice: String?,
    val targetRoom: String?,
    val value: Int?
)

enum class CommandAction { TURN_ON, TURN_OFF, SET_BRIGHTNESS, STATUS, UNKNOWN }
