package com.example.sabil_android_sdk

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.FragmentManager
import com.example.sabil_android_sdk.models.*
import com.example.sabil_android_sdk.ui.SabilDialog
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
import kotlinx.serialization.json.Json
import java.util.*


object Sabil {
    const val baseUrl = "https://83b5-74-199-74-188.ngrok.io"
    lateinit var clientId: String
    lateinit var userId: String
    lateinit var deviceId: String
    var secret: String? = null
    var appearanceConfig: SabilAppearanceConfig = SabilAppearanceConfig(true)
    var limitConfig = SabilLimitConfig(1, 2)
    var onLogoutCurrentDevice: (() -> Unit?)? = null
    var onLogoutOtherDevice: (() -> Unit?)? = null
    var onLimitExceeded: (() -> Unit?)? = null

    fun configure(
        context: Context,
        clientId: String,
        secret: String? = null,
        userId: String? = null,
        appearanceConfig: SabilAppearanceConfig? = null,
        limitConfig: SabilLimitConfig? = null,
        onLogoutCurrentDevice: (() -> Unit?)? = null,
        onLogoutOtherDevice: (() -> Unit?)? = null,
        onLimitExceeded: (() -> Unit?)? = null
    ) {
        val sharedPreferences =
            context.getSharedPreferences("sabil_shared_preference", Context.MODE_PRIVATE)
        if (sharedPreferences.contains("sabil_device_id")) {
            val stored = sharedPreferences.getString("sabil_device_id", "-1")
            if (stored is String && stored != "-1") {
                deviceId = stored
            } else {
                generateDeviceId(sharedPreferences)
            }
        } else {
            generateDeviceId(sharedPreferences)
        }

        this.clientId = clientId
        this.secret = secret
        if (userId != null) {
            this.userId = userId
        }
        if (appearanceConfig != null) {
            this.appearanceConfig = appearanceConfig
        }
        if (limitConfig != null) {
            this.limitConfig = limitConfig
        }
        this.onLogoutCurrentDevice = onLogoutCurrentDevice
        this.onLogoutOtherDevice = onLogoutOtherDevice
        this.onLimitExceeded = onLimitExceeded
    }

    private fun generateDeviceId(sharedPreferences: SharedPreferences) {
        deviceId = UUID.randomUUID().toString()
        with(sharedPreferences.edit()) {
            putString("sabil_device_id", deviceId)
            commit()
        }
    }

    fun attach(fragmentManager: FragmentManager) {
        if (!this::userId.isInitialized) {
            Log.d("SabilSDK", "You must initialize the userId before calling attach")
            return
        }
        httpRequest<SabilAttachResponse, SabilAttachData>(
            "POST",
            "$baseUrl/usage/attach",
            SabilAttachData(
                deviceId,
                userId,
                SabilDeviceInfo(
                    SabilOS("Android", android.os.Build.VERSION.RELEASE),
                    SabilDevice(android.os.Build.MANUFACTURER, "mobile", android.os.Build.MODEL)
                )
            )
        ) {
            if (it !is SabilAttachResponse) {
                return@httpRequest
            }
            if (!it.success || it.attached_devices <= limitConfig.overallLimit) {
                return@httpRequest
            }
            onLimitExceeded?.invoke()
            if (!appearanceConfig.showBlockingDialog) {
                return@httpRequest
            }
            val dialog = SabilDialog()
            dialog.show(fragmentManager, SabilDialog.TAG)
            getUserAttachedDevices {
                Log.d("SabilSDK", "attached devices: $it")
                dialog.viewModel.deviceUsages.value = it
            }
        }
    }


    fun detach(usage: SabilDeviceUsage?) {
        if (!this::userId.isInitialized) {
            Log.d("SabilSDK", "You must initialize the userId before calling attach")
            return
        }
        httpRequest<SabilAttachResponse, SabilDetachData>(
            "POST",
            "$baseUrl/usage/detach",
            SabilDetachData(usage?.device_id ?: deviceId, userId)
        ) {
            //TODO: hide dialog if needed
            //TODO: update attached devices list
        }
    }

    fun getUserAttachedDevices(onComplete: (List<SabilDeviceUsage>) -> Unit) {
        if (!this::userId.isInitialized) {
            Log.d("SabilSDK", "You must initialize the userId before calling attach")
            return
        }
        httpRequest<List<SabilDeviceUsage>, Unit>(
            "GET",
            "$baseUrl/usage/$userId/attached_devices",
            null
        ) {
            onComplete(it ?: listOf())
        }
    }

    inline fun <reified T, reified S> httpRequest(
        reqMethod: String,
        urlString: String,
        body: S? = null,
        noinline onComplete: ((T?) -> Unit)
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val client = HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json(Json {
                            ignoreUnknownKeys = true

                        })
                    }
                }
                val response = client.request(urlString) {
                    method = HttpMethod.parse(reqMethod)
                    headers {
                        append(HttpHeaders.Authorization, "Basic $clientId:${secret ?: ""}")
                    }
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    if (body != null) {
                        setBody(body)
                    }
                }
                val data = response.body() as? T
                if (data != null) {
                    Handler(Looper.getMainLooper()).post {
                        onComplete(data)
                    }
                }
            } catch (e: Exception) {
                Log.d("SabilSDK", "Error attaching ${e.message}")
            } finally {
                Log.d("SabilSDK", "Attach complete")
            }

        }
    }
}