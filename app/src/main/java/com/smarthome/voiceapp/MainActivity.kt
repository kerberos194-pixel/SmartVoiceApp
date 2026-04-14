package com.smarthome.voiceapp

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.smarthome.voiceapp.databinding.ActivityMainBinding
import com.smarthome.voiceapp.service.VoiceHandler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), VoiceHandler.Listener {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    @Inject
    lateinit var voiceHandler: VoiceHandler
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        voiceHandler.listener = this
        
        binding.fabMic.setOnClickListener { checkPermissionAndListen() }
        binding.btnDiscover.setOnClickListener { viewModel.discoverDevices() }
        binding.btnTest.setOnClickListener { voiceHandler.process("enciende la luz") }
        
        viewModel.state.observe(this) { state ->
            binding.tvStatus.text = if (state.isListening) "Listening..." else "Tap to speak"
            binding.tvResult.text = state.lastResult
        }
    }
    
    private fun checkPermissionAndListen() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) 
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1)
        }
    }
    
    private fun startListening() {
        viewModel.setListening(true)
        val recognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this)
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
        }
        
        recognizer.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onResults(results: android.os.Bundle?) {
                results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                    voiceHandler.process(it)
                }
                viewModel.setListening(false)
                recognizer.destroy()
            }
            override fun onError(error: Int) { viewModel.setListening(false); recognizer.destroy() }
            override fun onReadyForSpeech(p: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        recognizer.startListening(intent)
    }
    
    override fun onResult(result: String) {
        runOnUiThread { binding.tvResult.text = result }
    }
}
