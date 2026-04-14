package com.smarthome.voiceapp.domain.model

data class Device(
    val id: String,
    val name: String,
    val type: DeviceType,
    val room: String?,
    val isOn: Boolean,
    val brightness: Int? = null,
    val ipAddress: String? = null
)

enum class DeviceType { LIGHT, SWITCH, PLUG, THERMOSTAT, UNKNOWN }
