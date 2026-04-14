package com.smarthome.voiceapp

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.voiceapp.domain.repository.DeviceRepository
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
    private val deviceRepository: DeviceRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state
    
    fun observe(owner: LifecycleOwner, onChange: (UiState) -> Unit) {
        state.observe(owner) { onChange(it) }
    }
    
    fun setListening(listening: Boolean) {
        _state.value = _state.value.copy(isListening = listening)
    }
    
    fun discoverDevices() {
        viewModelScope.launch {
            deviceRepository.discoverDevices()
        }
    }
    
    private fun <T> StateFlow<T>.observe(owner: LifecycleOwner, observer: (T) -> Unit) {
        androidx.lifecycle.lifecycleScope.launchWhenStarted {
            collect { observer(it) }
        }
    }
}
