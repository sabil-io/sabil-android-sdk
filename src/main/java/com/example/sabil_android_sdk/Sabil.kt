package com.example.sabil_android_sdk

import android.provider.Settings
import android.util.Log
import com.example.sabil_android_sdk.models.SabilAppearanceConfig
import com.example.sabil_android_sdk.models.SabilAttachData
import com.example.sabil_android_sdk.models.SabilAttachResponse
import com.example.sabil_android_sdk.models.SabilLimitConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Sabil {
    val baseUrl = "http://10.0.2.2:8007/"
    lateinit var clientId: String
    lateinit var userId: String
    var secret: String? = null
    var appearanceConfig: SabilAppearanceConfig = SabilAppearanceConfig(true)
    var onLogoutCurrentDevice: (() -> Unit)? = null
    var onLogoutOtherDevice: (() -> Unit)? = null
    var onLimitExceeded: (() -> Unit)? = null
    var limitConfig = SabilLimitConfig(1, 2)

    fun configure(
        clientId: String,
        secret: String? = null,
        userId: String? = null,
        appearanceConfig: SabilAppearanceConfig? = null,
        onLogoutCurrentDevice: (() -> Unit)? = null,
        onLogoutOtherDevice: (() -> Unit)? = null,
        onLimitExceeded: (() -> Unit)? = null
    ) {
        this.clientId = clientId
        this.secret = secret
        if (userId != null) {
            this.userId = userId
        }
        if (appearanceConfig != null) {
            this.appearanceConfig = appearanceConfig
        }
        this.onLogoutCurrentDevice = onLogoutCurrentDevice
        this.onLogoutOtherDevice = onLogoutOtherDevice
        this.onLimitExceeded = onLimitExceeded
    }

    fun attach(metadata: Map<String, Any>? = null) {
        if (!this::userId.isInitialized) {
            Log.d("Sabil SDK", "You must initialize the userId before calling attach")
            return
        }
        httpRequest<SabilAttachResponse, SabilAttachData>(
            "POST",
            "$baseUrl/usage/attach",
            SabilAttachData(getDeviceId(), userId)
        ) {
            if (it !is SabilAttachResponse) {
                return@httpRequest
            }
            if (!appearanceConfig.showBlockingDialog) {
                return@httpRequest
            }
            if (it.success && it.attached_devices > limitConfig.overallLimit) {
                //TODO: show blocking dialog
                onLimitExceeded?.invoke()
            }
        }
    }

    inline fun <reified T, reified S> httpRequest(
        method: String,
        urlString: String,
        body: S? = null,
        noinline onComplete: ((T?) -> Unit)? = null
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val client = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json()
                }
            }
            val response = client.request {
                this.method = HttpMethod(method)
                url(urlString)
                basicAuth(clientId, secret ?: "")
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                if (body != null) {
                    setBody(body)
                }
            }
            val data = response.body() as? T
            if (data != null) {
                onComplete?.invoke(data)
            }
        }
    }

    fun getDeviceId(): String {
        return Settings.Secure.ANDROID_ID
    }
}