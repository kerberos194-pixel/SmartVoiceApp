package com.smarthome.voiceapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smarthome.voiceapp.databinding.ActivityMainBinding
import com.smarthome.voiceapp.domain.model.Device
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.smarthome.voiceapp.domain.repository.DeviceRepository

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    @Inject
    lateinit var deviceRepository: DeviceRepository
    
    private lateinit var deviceAdapter: DeviceAdapter
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            Toast.makeText(this, "Micrófono listo", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupClickListeners()
        observeDevices()
        checkPermissions()
    }
    
    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter { device ->
            toggleDevice(device)
        }
        binding.recyclerDevices.layoutManager = LinearLayoutManager(this)
        binding.recyclerDevices.adapter = deviceAdapter
    }
    
    private fun setupClickListeners() {
        binding.btnDiscover.setOnClickListener {
            discoverDevices()
        }
        
        binding.btnTest.setOnClickListener {
            binding.tvResult.text = "Comando: encender luz"
            Toast.makeText(this, "Voice funciona!", Toast.LENGTH_SHORT).show()
        }
        
        binding.fabMic.setOnClickListener {
            binding.tvResult.text = "Di: 'enciende la luz'"
            Toast.makeText(this, "Di un comando de voz", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeDevices() {
        lifecycleScope.launch {
            deviceRepository.getDevices().collect { devices ->
                updateUI(devices)
            }
        }
    }
    
    private fun updateUI(devices: List<Device>) {
        deviceAdapter.submitList(devices)
        
        if (devices.isEmpty()) {
            binding.tvEmpty.visibility = android.view.View.VISIBLE
            binding.recyclerDevices.visibility = android.view.View.GONE
        } else {
            binding.tvEmpty.visibility = android.view.View.GONE
            binding.recyclerDevices.visibility = android.view.View.VISIBLE
            binding.tvResult.text = "${devices.size} dispositivos encontrados"
        }
    }
    
    private fun discoverDevices() {
        binding.tvResult.text = "Buscando dispositivos..."
        
        lifecycleScope.launch {
            try {
                val devices = deviceRepository.discoverDevices()
                if (devices.isEmpty()) {
                    binding.tvResult.text = "No se encontraron dispositivos"
                    Toast.makeText(this@MainActivity, "Verifica que los dispositivos estén en la misma red", Toast.LENGTH_LONG).show()
                } else {
                    binding.tvResult.text = "${devices.size} dispositivos encontrados"
                    Toast.makeText(this@MainActivity, "Descubrimiento exitoso!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.tvResult.text = "Error: ${e.message}"
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun toggleDevice(device: Device) {
        lifecycleScope.launch {
            val result = if (device.isOn) {
                deviceRepository.turnOff(device.id)
            } else {
                deviceRepository.turnOn(device.id)
            }
            
            when (result) {
                is com.smarthome.voiceapp.domain.model.CommandResult.Success -> {
                    binding.tvResult.text = result.message
                    Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
                }
                is com.smarthome.voiceapp.domain.model.CommandResult.Error -> {
                    binding.tvResult.text = "Error: ${result.message}"
                    Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
