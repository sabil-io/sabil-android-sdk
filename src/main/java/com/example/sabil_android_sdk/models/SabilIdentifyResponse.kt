package com.example.sabil_android_sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class SabilIdentifyResponse(val identity: String, val confidence: Float)
