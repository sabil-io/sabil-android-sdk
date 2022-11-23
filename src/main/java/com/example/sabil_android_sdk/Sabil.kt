package com.example.sabil_android_sdk

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.sabil_android_sdk.models.*
import com.example.sabil_android_sdk.ui.SabilDialog
import com.example.sabil_android_sdk.view_models.SabilDialogViewModel
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
    private const val baseUrl = "https://api.sabil.io"
    private const val ANDROID_VENDOR_ID_KEY = "sabil_vendor_id"
    private const val DEVICE_ID_KEY = "sabil_device_id"
    lateinit var clientId: String
    lateinit var userId: String
    var secret: String? = null
    var appearanceConfig: SabilAppearanceConfig? = null
    var onLogoutCurrentDevice: (() -> Unit?)? = null
    var onLogoutOtherDevice: (() -> Unit?)? = null
    var onLimitExceeded: (() -> Unit?)? = null
    private val viewModel =
        ViewModelProvider {
            ViewModelStore()
        }[SabilDialogViewModel::class.java]
    var dialog: SabilDialog? = null
    private lateinit var sharedPreferences: SharedPreferences

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
        sharedPreferences =
            context.getSharedPreferences("sabil_shared_preference", Context.MODE_PRIVATE)
        val stored = sharedPreferences.getString(DEVICE_ID_KEY, null)
        if (stored is String) {
            viewModel.deviceId.value = stored
        }

        this.clientId = clientId
        this.secret = secret
        if (userId != null) {
            this.userId = userId
        }
        if (appearanceConfig != null) {
            this.appearanceConfig = appearanceConfig
        }
        viewModel.limitConfig.value = limitConfig
        this.onLogoutCurrentDevice = onLogoutCurrentDevice
        this.onLogoutOtherDevice = onLogoutOtherDevice
        this.onLimitExceeded = onLimitExceeded
    }

    private fun getIdentifierForVendor(): String {
        val storedId = sharedPreferences.getString(ANDROID_VENDOR_ID_KEY, null)
        if (storedId is String) {
            return storedId
        }
        val newId = UUID.randomUUID().toString()
        with(sharedPreferences.edit()) {
            putString(ANDROID_VENDOR_ID_KEY, newId)
            commit()
        }
        return newId
    }

    fun attach(fragmentManager: FragmentManager, metadata: Map<String, String>? = null) {
        if (!this::userId.isInitialized) {
            Log.d("SabilSDK", "You must initialize the userId before calling attach")
            return
        }
        httpRequest<SabilAccessState, SabilAttachData>(
            "POST",
            "$baseUrl/v2/access",
            SabilAttachData(
                viewModel.deviceId.value,
                SabilSignals(getIdentifierForVendor()),
                userId,
                SabilDeviceInfo(
                    SabilOS("Android", android.os.Build.VERSION.RELEASE),
                    SabilDeviceDetails(
                        android.os.Build.MANUFACTURER,
                        "mobile",
                        android.os.Build.MODEL
                    )
                ),
                metadata,
                null
            )
        ) { state ->
            if (state !is SabilAccessState) {
                return@httpRequest
            }
            viewModel.defaultDeviceLimit.value = state.default_device_limit
            viewModel.deviceId.value = state.device_id
            if (!state.success || state.attached_devices <= (viewModel.limitConfig.value?.overallLimit
                    ?: state.default_device_limit)
            ) {
                return@httpRequest
            }
            onLimitExceeded?.invoke()
            val showDialog =
                appearanceConfig?.showBlockingDialog ?: state.block_over_usage
            if (!showDialog) {
                return@httpRequest
            }
            dialog = SabilDialog(viewModel) {
                for (device in it) {
                    detach(device)
                }
            }
            dialog?.isCancelable = false
            dialog?.show(fragmentManager, SabilDialog.TAG)
            getUserAttachedDevices {
                viewModel.devices.value = it.toMutableList()
            }
        }
    }


    fun detach(device: SabilDevice?) {
        val idToDetach = device?.id ?: viewModel.deviceId.value
        if (idToDetach !is String) {
            Log.d("SabilSDK", "Device id to detach not found ")
            return
        }
        if (!this::userId.isInitialized) {
            Log.d("SabilSDK", "You must initialize the userId before calling detach")
            return
        }
        viewModel.detachLoading.value = true
        httpRequest<SabilAccessState, SabilDetachData>(
            "POST",
            "$baseUrl/v2/access/detach",
            SabilDetachData(idToDetach, userId)
        ) { state ->
            if (state !is SabilAccessState || !state.success) {
                return@httpRequest
            }
            if (device == null || device.id == viewModel.deviceId.value) {
                onLogoutCurrentDevice?.invoke()
            }
            viewModel.devices.value?.removeAll { it.id == device?.id }
            viewModel.devices.postValue(viewModel.devices.value)
            viewModel.detachLoading.value = false
            viewModel.defaultDeviceLimit.value = state.default_device_limit
            if ((viewModel.devices.value?.size
                    ?: 0) <= (viewModel.limitConfig.value?.overallLimit
                    ?: state.default_device_limit)
            ) {
                dialog?.dismiss()
                dialog = null
            }
        }
    }

    fun identify(metadata: Map<String, String>? = null) {

    }

    fun getUserAttachedDevices(onComplete: (List<SabilDevice>) -> Unit) {
        if (!this::userId.isInitialized) {
            Log.d("SabilSDK", "You must initialize the userId before calling attach")
            return
        }
        httpRequest<List<SabilDevice>, Unit>(
            "GET",
            "$baseUrl/v2/access/user/$userId/attached_devices",
            null
        ) {
            onComplete(it ?: listOf())
        }
    }

    private inline fun <reified T, reified S> httpRequest(
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
                Log.d("SabilSDK", "Error http request ${e.message}")
            }

        }
    }
}