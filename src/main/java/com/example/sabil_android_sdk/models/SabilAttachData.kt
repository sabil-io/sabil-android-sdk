package com.example.sabil_android_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class SabilSignals(val androidVendorIdentifier: String?)

@Serializable
data class SabilAttachData(
    val device_id: String?,
    val signals: SabilSignals,
    val user: String,
    val device_info: SabilDeviceInfo,
    val metadata: Map<String, String>?,
    val identity: String?
)
