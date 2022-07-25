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
    const val baseUrl = "http://10.0.2.2:8007"
    lateinit var clientId: String
    lateinit var userId: String
    var secret: String? = null
    var appearanceConfig: SabilAppearanceConfig? = null
    var onLogoutCurrentDevice: (() -> Unit?)? = null
    var onLogoutOtherDevice: (() -> Unit?)? = null
    var onLimitExceeded: (() -> Unit?)? = null
    val viewModel =
        ViewModelProvider {
            ViewModelStore()
        }[SabilDialogViewModel::class.java]
    var dialog: SabilDialog? = null

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
                viewModel.deviceId.value = stored
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
        viewModel.limitConfig.value = limitConfig
        this.onLogoutCurrentDevice = onLogoutCurrentDevice
        this.onLogoutOtherDevice = onLogoutOtherDevice
        this.onLimitExceeded = onLimitExceeded
    }

    private fun generateDeviceId(sharedPreferences: SharedPreferences) {
        viewModel.deviceId.value = UUID.randomUUID().toString()
        with(sharedPreferences.edit()) {
            putString("sabil_device_id", viewModel.deviceId.value)
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
                viewModel.deviceId.value ?: "",
                userId,
                SabilDeviceInfo(
                    SabilOS("Android", android.os.Build.VERSION.RELEASE),
                    SabilDevice(android.os.Build.MANUFACTURER, "mobile", android.os.Build.MODEL)
                )
            )
        ) { attachResponse ->
            if (attachResponse !is SabilAttachResponse) {
                return@httpRequest
            }
            viewModel.defaultDeviceLimit.value = attachResponse.default_device_limit
            if (!attachResponse.success || attachResponse.attached_devices <= (viewModel.limitConfig.value?.overallLimit
                    ?: attachResponse.default_device_limit)
            ) {
                return@httpRequest
            }
            onLimitExceeded?.invoke()
            val showDialog =
                appearanceConfig?.showBlockingDialog ?: attachResponse.block_over_usage
            if (!showDialog) {
                return@httpRequest
            }
            dialog = SabilDialog(viewModel) {
                for (usage in it) {
                    detach(usage)
                }
            }
            dialog?.isCancelable = false
            dialog?.show(fragmentManager, SabilDialog.TAG)
            getUserAttachedDevices {
                viewModel.deviceUsages.value = it.toMutableList()
            }
        }
    }


    fun detach(usage: SabilDeviceUsage?) {
        viewModel.detachLoading.value = true
        if (!this::userId.isInitialized) {
            Log.d("SabilSDK", "You must initialize the userId before calling attach")
            viewModel.detachLoading.value = false
            return
        }
        httpRequest<SabilAttachResponse, SabilDetachData>(
            "POST",
            "$baseUrl/usage/detach",
            SabilDetachData(usage?.device_id ?: viewModel.deviceId.value ?: "", userId)
        ) { attachResponse ->
            if (attachResponse !is SabilAttachResponse || !attachResponse.success) {
                return@httpRequest
            }
            if (usage == null || usage.device_id == viewModel.deviceId.value) {
                onLogoutCurrentDevice?.invoke()
            }
            viewModel.deviceUsages.value?.removeAll { it.device_id == usage?.device_id }
            viewModel.deviceUsages.postValue(viewModel.deviceUsages.value)
            viewModel.detachLoading.value = true
            viewModel.defaultDeviceLimit.value = attachResponse.default_device_limit
            if ((viewModel.deviceUsages.value?.size
                    ?: 0) <= (viewModel.limitConfig.value?.overallLimit
                    ?: attachResponse.default_device_limit)
            ) {
                dialog?.dismiss()
                dialog = null
            }
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
                Log.d("SabilSDK", "Error http request ${e.message}")
            }

        }
    }
}