package com.smarthome.voiceapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smarthome.voiceapp.data.repository.DeviceRepositoryImpl
import com.smarthome.voiceapp.databinding.ActivityMainBinding
import com.smarthome.voiceapp.domain.model.CommandResult
import com.smarthome.voiceapp.domain.model.Device
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    @Inject
    lateinit var deviceRepository: DeviceRepositoryImpl
    
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
            showScanOrAddDialog()
        }
        
        binding.btnTest.setOnClickListener {
            binding.tvResult.text = "Voice: Di 'enciende la luz'"
            Toast.makeText(this, "Voice listo!", Toast.LENGTH_SHORT).show()
        }
        
        binding.fabMic.setOnClickListener {
            binding.tvResult.text = "Di: 'enciende la luz'"
            Toast.makeText(this, "Comando de voz", Toast.LENGTH_SHORT).show()
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
            binding.tvResult.text = "${devices.size} dispositivo(s)"
        }
    }
    
    private fun showAddDeviceDialog() {
        val editText = EditText(this).apply {
            hint = "Ej: 192.168.1.100"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Añadir dispositivo TP-Link")
            .setMessage("Ingresa la dirección IP del dispositivo.\n\nPara saber la IP:\n1. Abre la app Kasa\n2. Ve al dispositivo\n3. Busca 'Device Info'")
            .setView(editText)
            .setPositiveButton("Añadir") { _, _ ->
                val ip = editText.text.toString().trim()
                if (ip.isNotEmpty()) {
                    addDeviceByIP(ip)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showScanOrAddDialog() {
        val options = arrayOf("Escanear red (automático)", "Añadir por IP manual")
        
        AlertDialog.Builder(this)
            .setTitle("Buscar dispositivos")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showScanDialog()
                    1 -> showAddDeviceDialog()
                }
            }
            .show()
    }
    
    private fun showScanDialog() {
        val editText = EditText(this).apply {
            hint = "Ej: 192.168.1.1"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Escanear red")
            .setMessage("Ingresa cualquier IP de tu red local.\nEj: 192.168.1.1\n\nEl escaneo detectará todos los dispositivos TP-Link.")
            .setView(editText)
            .setPositiveButton("Escanear") { _, _ ->
                val ip = editText.text.toString().trim()
                if (ip.isNotEmpty()) {
                    scanNetwork(ip)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun scanNetwork(baseIP: String) {
        binding.tvResult.text = "Escaneando red..."
        
        lifecycleScope.launch {
            val result = deviceRepository.scanAndAddDevices(baseIP)
            
            when (result) {
                is CommandResult.Success -> {
                    binding.tvResult.text = result.message
                    Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
                }
                is CommandResult.Error -> {
                    binding.tvResult.text = "Error: ${result.message}"
                    Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun addDeviceByIP(ip: String) {
        binding.tvResult.text = "Conectando a $ip..."
        
        lifecycleScope.launch {
            val result = deviceRepository.addDeviceByIP(ip)
            
            when (result) {
                is CommandResult.Success -> {
                    binding.tvResult.text = result.message
                    Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
                }
                is CommandResult.Error -> {
                    binding.tvResult.text = "Error: ${result.message}"
                    Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                }
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
                is CommandResult.Success -> {
                    binding.tvResult.text = result.message
                }
                is CommandResult.Error -> {
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
