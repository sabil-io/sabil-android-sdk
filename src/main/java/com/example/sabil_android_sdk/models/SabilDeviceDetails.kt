package com.example.sabil_android_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class SabilDeviceDetails(val vendor: String?, val type: String?, val model: String?)
