package com.smarthome.voiceapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.voiceapp.domain.repository.DeviceRepository
import com.smarthome.voiceapp.service.VoiceHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiState(
    val isListening: Boolean = false,
    val lastResult: String = ""
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    val voiceHandler: VoiceHandler
) : ViewModel() {
    
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state
    
    fun setListening(listening: Boolean) {
        _state.value = _state.value.copy(isListening = listening)
    }
    
    fun setResult(result: String) {
        _state.value = _state.value.copy(lastResult = result)
    }
    
    fun discoverDevices() {
        viewModelScope.launch {
            deviceRepository.discoverDevices()
        }
    }
}
