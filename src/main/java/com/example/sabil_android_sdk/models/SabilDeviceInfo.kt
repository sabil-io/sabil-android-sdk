package com.example.sabil_android_sdk.models
import kotlinx.serialization.Serializable

@Serializable
data class SabilDeviceInfo(val os: SabilOS?, val device: SabilDeviceDetails?)
