package com.hz_apps.autowificonnector.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wifi_configurations")
data class WifiConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val ssid: String,
    val identity: String,
    val password: String,
    val isEditAllowed: Boolean,
    var order: Int = 0
)
