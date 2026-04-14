package com.smarthome.voiceapp.service

import android.content.Context
import com.smarthome.voiceapp.domain.model.CommandResult
import com.smarthome.voiceapp.domain.usecase.ProcessVoiceCommandUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val useCase: ProcessVoiceCommandUseCase
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    
    interface Listener {
        fun onResult(result: String)
    }
    
    var listener: Listener? = null
    
    fun process(text: String) {
        scope.launch {
            val result = useCase(text)
            val msg = when (result) {
                is CommandResult.Success -> result.message
                is CommandResult.Error -> "Error: ${result.message}"
            }
            listener?.onResult(msg)
        }
    }
}
