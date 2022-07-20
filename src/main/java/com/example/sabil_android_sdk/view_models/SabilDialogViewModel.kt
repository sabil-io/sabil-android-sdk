package com.example.sabil_android_sdk.view_models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sabil_android_sdk.models.SabilDeviceUsage
import com.example.sabil_android_sdk.models.SabilLimitConfig

class SabilDialogViewModel : ViewModel() {
    val deviceId: MutableLiveData<String> by lazy {
        MutableLiveData()
    }
    val deviceUsages: MutableLiveData<MutableList<SabilDeviceUsage>> by lazy {
        MutableLiveData()
    }

    val detachLoading: MutableLiveData<Boolean> by lazy {
        MutableLiveData()
    }

    val fetchLoading: MutableLiveData<Boolean> by lazy {
        MutableLiveData()
    }

    val limitConfig: MutableLiveData<SabilLimitConfig> by lazy {
        MutableLiveData()
    }


}