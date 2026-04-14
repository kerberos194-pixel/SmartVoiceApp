package com.smarthome.voiceapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smarthome.voiceapp.databinding.ItemDeviceBinding
import com.smarthome.voiceapp.domain.model.Device

class DeviceAdapter(
    private val onDeviceClick: (Device) -> Unit
) : ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceViewHolder(
        private val binding: ItemDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onDeviceClick(getItem(pos))
                }
            }
        }

        fun bind(device: Device) {
            binding.tvDeviceName.text = device.name
            binding.tvDeviceType.text = device.type.name
            binding.tvStatus.text = if (device.isOn) "ON" else "OFF"
            
            val color = if (device.isOn) 0xFF4CAF50.toInt() else 0xFF9E9E9E.toInt()
            binding.tvStatus.setTextColor(color)
            binding.indicator.setBackgroundColor(color)
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }
    }
}
