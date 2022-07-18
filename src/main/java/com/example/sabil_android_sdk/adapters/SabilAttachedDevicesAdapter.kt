package com.example.sabil_android_sdk.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sabil_android_sdk.R
import com.example.sabil_android_sdk.models.SabilDeviceUsage

class SabilAttachedDeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val titleTextView: TextView = view.findViewById(R.id.device_usage_title_text_view)
    val lastUpdatedTextView: TextView = view.findViewById(R.id.last_updated_text_view)
    val deviceTypeImageView: ImageView = view.findViewById(R.id.device_type_image_view)
}

class SabilAttachedDevicesAdapter(
    var deviceUsages: List<SabilDeviceUsage>,
    var deviceId: String
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
        val usage = deviceUsages[position]
        holder.titleTextView.text =
            if (usage.device_id == deviceId) "Current device" else usage.device_info?.os?.name
//        if (usage.device_id == deviceId) {
//            holder.titleTextView.setTextColor(Color.RED)
//        } else {
//            holder.titleTextView.setTextColor(Color.BLACK)
//        }
        holder.lastUpdatedTextView.text = usage.updatedAt.toString()
    }

    override fun getItemCount(): Int {
        return deviceUsages.size
    }
}