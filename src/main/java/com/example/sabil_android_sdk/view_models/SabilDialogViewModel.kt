package com.example.sabil_android_sdk.view_models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.sabil_android_sdk.models.SabilDeviceUsage

class SabilDialogViewModel : ViewModel() {
    val deviceId: MutableLiveData<String> by lazy {
        MutableLiveData()
    }
    val deviceUsages: MutableLiveData<List<SabilDeviceUsage>> by lazy {
        MutableLiveData()
    }


}