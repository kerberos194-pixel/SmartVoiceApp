package com.smarthome.voiceapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smarthome.voiceapp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvStatus.text = "Smart Voice Ready"
        
        binding.btnDiscover.setOnClickListener {
            Toast.makeText(this, "Discovering devices...", Toast.LENGTH_SHORT).show()
        }
    }
}
