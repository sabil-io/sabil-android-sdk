package com.example.sabil_android_sdk.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SabilDeviceUsage(
    val device_id: String,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val status: String,
    val device_info: SabilDeviceInfo?
)
