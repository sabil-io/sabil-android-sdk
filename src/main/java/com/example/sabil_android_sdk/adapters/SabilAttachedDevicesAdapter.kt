package com.example.sabil_android_sdk.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sabil_android_sdk.R
import com.example.sabil_android_sdk.models.SabilDevice

class SabilAttachedDeviceViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val titleTextView: TextView? = view.findViewById(R.id.device_usage_title_text_view)
    val lastUpdatedTextView: TextView? = view.findViewById(R.id.last_updated_text_view)
    val deviceTypeImageView: ImageView? = view.findViewById(R.id.device_type_image_view)
    val selectedRadioButton: RadioButton? = view.findViewById(R.id.selected_radio_button)
}

class SabilAttachedDevicesAdapter(
    var deviceId: String,
    var devices: List<SabilDevice>,
    var selected: MutableSet<SabilDevice>
) :
    RecyclerView.Adapter<SabilAttachedDeviceViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SabilAttachedDeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sabil_device_usage_view_holder, parent, false)
        return SabilAttachedDeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: SabilAttachedDeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.titleTextView?.text =
            if (device.id == deviceId) "Current device" else device.info?.os?.name
        if (device.id == deviceId) {
            holder.titleTextView?.setTextColor(Color.parseColor("#D0496F"))
        }
        holder.lastUpdatedTextView?.text = device.updatedAt?.toString()
        val image = when (device.info?.device?.type) {
            "mobile" -> R.drawable.ic_baseline_smartphone_24
            "tablet" -> R.drawable.ic_baseline_tablet_mac_24
            else -> R.drawable.ic_baseline_computer_24
        }
        holder.deviceTypeImageView?.setImageResource(image)
        holder.view.setOnClickListener {
            if (selected.contains(device)) {
                selected.remove(device)
            } else {
                selected.add(device)
            }
            val color =
                if (selected.contains(device)) "#E4E4E6" else "#ffffff"
            it.setBackgroundColor(Color.parseColor(color))
            holder.selectedRadioButton?.isChecked = selected.contains(device)
        }
    }

    override fun getItemCount(): Int {
        return devices.size
    }
}