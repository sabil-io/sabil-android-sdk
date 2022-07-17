package com.example.sabil_android_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class SabilAttachResponse(val success: Boolean, val attached_devices: Int)
