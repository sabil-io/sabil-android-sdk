package com.example.sabil_android_sdk.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sabil_android_sdk.R
import com.example.sabil_android_sdk.adapters.SabilAttachedDevicesAdapter
import com.example.sabil_android_sdk.databinding.SabilDialogViewBinding
import com.example.sabil_android_sdk.models.SabilDeviceUsage
import com.example.sabil_android_sdk.view_models.SabilDialogViewModel


class SabilDialog(
    private val viewModel: SabilDialogViewModel,
    private val onLogout: (MutableSet<SabilDeviceUsage>) -> Unit
) :
    DialogFragment() {
    companion object {
        const val TAG = "SabilDialog"
    }

    private lateinit var adapter: SabilAttachedDevicesAdapter

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!this::adapter.isInitialized) {
            adapter = SabilAttachedDevicesAdapter(
                viewModel.deviceId.value ?: "",
                viewModel.deviceUsages.value ?: listOf(),
                mutableSetOf()
            )
        }

        viewModel.deviceId.observe(this) {
            adapter.deviceId = it
            adapter.notifyDataSetChanged()
        }
        viewModel.deviceUsages.observe(this) {
            adapter.deviceUsages = it
            adapter.notifyDataSetChanged()
            updateLogoutSubtitle()
        }
    }

    override fun onResume() {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.attached_devices_rv)
        val unwrappedContext = context
        if (unwrappedContext is Context) {
            recyclerView?.layoutManager = LinearLayoutManager(unwrappedContext)
        } else {
            Log.d("SabilSDK", "Context is null")
        }
        recyclerView?.adapter = adapter
        updateLogoutSubtitle()
        view?.findViewById<Button>(R.id.logout_button)?.setOnClickListener {
            onLogout(adapter.selected)
        }
        super.onResume()
    }

    private fun updateLogoutSubtitle() {
        val overage =
            (viewModel.deviceUsages.value?.size ?: 0) - (viewModel.limitConfig.value?.overallLimit
                ?: 0)
        view?.findViewById<TextView>(R.id.logout_subtitle)?.text = resources.getQuantityString(
            R.plurals.logout_to_proceed, overage, overage
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding: SabilDialogViewBinding =
            DataBindingUtil.inflate(inflater, R.layout.sabil_dialog_view, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }
}