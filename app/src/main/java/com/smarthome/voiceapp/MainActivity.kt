package com.smarthome.voiceapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.smarthome.voiceapp.databinding.ActivityMainBinding
import com.smarthome.voiceapp.service.VoiceHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), VoiceHandler.Listener {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel.voiceHandler.listener = this
        
        binding.fabMic.setOnClickListener { checkPermissionAndListen() }
        binding.btnDiscover.setOnClickListener { viewModel.discoverDevices() }
        binding.btnTest.setOnClickListener { viewModel.voiceHandler.process("enciende la luz") }
        
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                binding.tvStatus.text = if (state.isListening) "Listening..." else "Tap to speak"
                binding.tvResult.text = state.lastResult
            }
        }
    }
    
    private fun checkPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    
    private fun startListening() {
        viewModel.setListening(true)
        val recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                    viewModel.voiceHandler.process(it)
                }
                viewModel.setListening(false)
                recognizer.destroy()
            }
            override fun onError(error: Int) { 
                viewModel.setListening(false)
                recognizer.destroy()
            }
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(intent)
    }
    
    override fun onResult(result: String) {
        runOnUiThread { binding.tvResult.text = result }
    }
}
