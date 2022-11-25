package com.example.sabil_android_sdk

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
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
    private const val BASE_URL = "https://api.sabil.io"
    private const val ANDROID_VENDOR_ID_KEY = "sabil_vendor_id"
    private const val DEVICE_ID_KEY = "sabil_device_id"
    private const val DEVICE_IDENTIFIER_KEY = "sabil_device_identifier"
    private lateinit var clientId: String

    /**
     * The id of the current user. Ensure it is set on configure or as soon as it becomes available.
     */
    var userId: String? = null

    /**
     * If using signed requests, pass the (signed) secret here.
     */
    var secret: String? = null
    var appearanceConfig: SabilAppearanceConfig? = null
    var onLogoutCurrentDevice: (() -> Unit?)? = null
    var onLogoutOtherDevice: (() -> Unit?)? = null
    var onLimitExceeded: (() -> Unit?)? = null
    private val viewModel =
        ViewModelProvider {
            ViewModelStore()
        }[SabilDialogViewModel::class.java]
    private var dialog: SabilDialog? = null
    private lateinit var sharedPreferences: SharedPreferences

    /**
     * The device id for this device generated by Sabil. This is different from the identity.
     */
    var deviceId: String?
        get() = viewModel.deviceId.value
        set(value) {
            viewModel.deviceId.value = value
        }

    /**
     * The device identity for this device generated by Sabil. Any devices with the same identity
     * can safely be assumed to be a single physical device.z
     */
    var deviceIdentity: String?
        get() {
            if (!this::sharedPreferences.isInitialized) {
                return null
            }
            return sharedPreferences.getString(DEVICE_IDENTIFIER_KEY, null)
        }
        set(value) {
            if (!this::sharedPreferences.isInitialized) {
                return
            }
            with(sharedPreferences.edit()) {
                putString(DEVICE_IDENTIFIER_KEY, value)
                commit()
            }
        }

    /**
     * Call this method on your app launch to configure the SDK.
     * @param context a context used for showing the dialog and storing needed properties.
     * @param clientId your projects API client ID.
     * @param userId the id of the current user if available. If the id is not available, make sure
     * to set the property `userId` as soon as it becomes available.
     * @param appearanceConfig options to configure the appearance of the dialog.
     * @param limitConfig the configuration object for the limit. By default the SDK uses the values
     * set on your project dashboard. This value will override the defaults. Only set this if you
     * want to override the defaults set on the dashboard.
     * @param onLogoutCurrentDevice a callback that will be triggered if the user chooses to log out the current device.
     * @param onLogoutOtherDevice a callback that will be triggered if the user elects to logout a
     * devices other than this one.
     * @param onLimitExceeded a callback that will be triggered if the user exceeds the configured limit.
     */
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
            deviceId = stored
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

    /**
     * Attaches a device to a user. You must have set the clientId and the user id before colling this.
     * Otherwise, it will do nothing.
     * @param fragmentManager a FragmentManager object used to present the dialog if limit is
     * exceeded and the blocking dialog is enabled. Pass null if not neeeded.
     * @param metadata a key-value map to store any data you want. Sabil will pass this along with
     * any webhook or other server-to-server calls.
     * @param onComplete a callback to be triggered once the attach finishes.
     */
    fun attach(
        fragmentManager: FragmentManager? = null,
        metadata: Map<String, String>? = null,
        onComplete: (() -> Unit)? = null
    ) {
        val user = userId
        if (user !is String) {
            Log.d("SabilSDK", "You must initialize the userId before calling attach")
            return
        }
        httpRequest<SabilAccessState, SabilAttachData>(
            "POST",
            "$BASE_URL/v2/access",
            SabilAttachData(
                viewModel.deviceId.value,
                SabilSignals(getIdentifierForVendor()),
                user,
                deviceInfo(),
                metadata,
                null
            )
        ) { state ->
            onComplete?.invoke()
            if (state !is SabilAccessState) {
                return@httpRequest
            }
            viewModel.defaultDeviceLimit.value = state.default_device_limit
            deviceId = state.device_id
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
            if (fragmentManager !is FragmentManager) {
                return@httpRequest
            }
            dialog?.isCancelable = false
            dialog?.show(fragmentManager, SabilDialog.TAG)
            getUserAttachedDevices {
                viewModel.devices.value = it.toMutableList()
            }
        }
    }

    private fun deviceInfo() = SabilDeviceInfo(
        SabilOS("Android", Build.VERSION.RELEASE),
        SabilDeviceDetails(
            Build.MANUFACTURER,
            "mobile",
            Build.MODEL
        )
    )


    /**
     * Call this to detach a user device.
     * @param device The device to detach. If null, will detach current device.
     */
    fun detach(device: SabilDevice?) {
        val idToDetach = device?.id ?: viewModel.deviceId.value
        if (idToDetach !is String) {
            Log.d("SabilSDK", "Device id to detach not found ")
            return
        }
        val user = userId
        if (user !is String) {
            Log.d("SabilSDK", "You must initialize the userId before calling detach")
            return
        }
        viewModel.detachLoading.value = true
        httpRequest<SabilAccessState, SabilDetachData>(
            "POST",
            "$BASE_URL/v2/access/detach",
            SabilDetachData(idToDetach, user)
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

    /**
     * Use this method to identify this device. Once identified any subsequent attach requests will
     * know that this device is the identity. Usually this is only needed if
     * 1. you don't have the user id AND
     * 2. the user can perform actions before logging in.
     * @param metadata a key-value map to store any data you want. Sabil will pass this along with
     * any webhook or other server-to-server calls.
     * @param onComplete a callback that will be triggered once the identification is completed.
     */
    fun identify(
        metadata: Map<String, String>? = null,
        onComplete: ((SabilIdentifyResponse?) -> Unit)? = null
    ) {
        if (!this::clientId.isInitialized) {
            Log.d("SabilSDK", "You must initialize the clientId before calling attach")
            return
        }
        httpRequest<SabilIdentifyResponse, SabilIdentifyData>(
            "POST", "$BASE_URL/v2/identity", SabilIdentifyData(
                deviceId,
                SabilSignals(getIdentifierForVendor()),
                deviceInfo(),
                metadata,
                deviceIdentity
            )
        ) {
            onComplete?.invoke(it)
        }
    }

    fun getUserAttachedDevices(onComplete: (List<SabilDevice>) -> Unit) {
        val user = userId
        if (user !is String) {
            Log.d("SabilSDK", "You must initialize the userId before calling attach")
            return
        }
        httpRequest<List<SabilDevice>, Unit>(
            "GET",
            "$BASE_URL/v2/access/user/$userId/attached_devices",
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