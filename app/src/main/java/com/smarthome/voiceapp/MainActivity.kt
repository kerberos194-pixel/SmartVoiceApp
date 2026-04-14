package com.smarthome.voiceapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.smarthome.voiceapp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
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
        
        binding.tvStatus.text = "Smart Voice Ready"
        binding.tvResult.text = "Presiona el botón para empezar"
        
        binding.btnDiscover.setOnClickListener {
            Toast.makeText(this, "Buscando dispositivos...", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnTest.setOnClickListener {
            binding.tvResult.text = "Comando de prueba procesado"
            Toast.makeText(this, "Voice funciona!", Toast.LENGTH_SHORT).show()
        }
        
        binding.fabMic.setOnClickListener {
            checkPermissions()
        }
        
        checkPermissions()
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            Toast.makeText(this, "Todos los permisos concedidos", Toast.LENGTH_SHORT).show()
        }
    }
}
