package com.smarthome.voiceapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val room: String?,
    val isOn: Boolean,
    val brightness: Int?,
    val ipAddress: String?
)
