package com.example.sabil_android_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class SabilIdentifyData(
    val device_id: String?,
    val signals: SabilSignals,
    val device_info: SabilDeviceInfo,
    val metadata: Map<String, String>?,
    val identity_id: String?
)
