package com.example.sabil_android_sdk.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sabil_android_sdk.R
import com.example.sabil_android_sdk.adapters.SabilAttachedDevicesAdapter
import com.example.sabil_android_sdk.databinding.SabilDialogViewBinding
import com.example.sabil_android_sdk.view_models.SabilDialogViewModel


class SabilDialog : DialogFragment() {
    companion object {
        const val TAG = "SabilDialog"
    }

    lateinit var viewModel: SabilDialogViewModel
    private lateinit var adapter: SabilAttachedDevicesAdapter

//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        return AlertDialog.Builder(requireContext())
//            .setMessage("Hello world")
//            .setCancelable(false)
//            .setPositiveButton("Oky") { _, _ ->
//
//            }
//            .create()
//    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!this::viewModel.isInitialized) {
            viewModel = ViewModelProvider(this).get(SabilDialogViewModel::class.java)
        }
        if (!this::adapter.isInitialized) {
            adapter = SabilAttachedDevicesAdapter(
                viewModel.deviceUsages.value ?: listOf(),
                viewModel.deviceId.value ?: ""
            )
        }

        viewModel.deviceId.observe(this) {
            adapter.deviceId = it
            adapter.notifyDataSetChanged()
        }
        viewModel.deviceUsages.observe(this) {
            adapter.deviceUsages = it
            adapter.notifyDataSetChanged()
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
        super.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding: SabilDialogViewBinding =
            DataBindingUtil.inflate(inflater, R.layout.sabil_dialog_view, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        return binding.root
    }
}