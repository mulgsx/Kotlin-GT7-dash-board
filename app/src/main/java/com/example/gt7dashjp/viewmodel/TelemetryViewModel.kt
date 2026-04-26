package com.example.gt7dashjp.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gt7dashjp.data.GT7UdpClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DEFAULT_IP = "192.168.0.0"

enum class StatusType { IDLE, CONNECTING, RECEIVING, ERROR }

data class TelemetryUiState(
    val isListening: Boolean = false,
    val rpm: Float = 0f,
    val packetCount: Int = 0,
    val status: String = "IDLE: Press 'Start Receiving' to connect",
    val statusType: StatusType = StatusType.IDLE,
    val currentIp: String = DEFAULT_IP,
    val ipFieldText: String = DEFAULT_IP
)

class TelemetryViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val DEFAULT_IP = "192.168.0.0"
        private const val PREFS_NAME = "gt7_prefs"
        private const val PREFS_KEY_IP = "gt7_ps_ip"
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val savedIp = prefs.getString(PREFS_KEY_IP, DEFAULT_IP) ?: DEFAULT_IP

    private val _uiState = MutableStateFlow(
        TelemetryUiState(currentIp = savedIp, ipFieldText = savedIp)
    )
    val uiState: StateFlow<TelemetryUiState> = _uiState.asStateFlow()

    private var client: GT7UdpClient? = null
    private var pollingJob: Job? = null

    @Volatile private var latestPacketCount: Int = 0
    @Volatile private var latestRpm: Float = 0f

    fun onIpChanged(ip: String) {
        _uiState.value = _uiState.value.copy(ipFieldText = ip)
    }

    fun isValidIp(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all { part -> part.toIntOrNull()?.let { it in 0..255 } ?: false }
    }

    fun startListening() {
        val ip = _uiState.value.ipFieldText
        prefs.edit().putString(PREFS_KEY_IP, ip).apply()

        client?.stopCommunication()
        latestPacketCount = 0
        latestRpm = 0f

        _uiState.value = _uiState.value.copy(
            isListening = true,
            statusType = StatusType.CONNECTING,
            status = "CONNECTING: Connecting to $ip...",
            currentIp = ip,
            packetCount = 0,
            rpm = 0f
        )

        client = GT7UdpClient(
            onConnected = { fromIp ->
                _uiState.value = _uiState.value.copy(
                    statusType = StatusType.RECEIVING,
                    status = "RECEIVING: Receiving data from $fromIp"
                )
            },
            onPacketReceived = { count, rpm ->
                latestPacketCount = count
                latestRpm = rpm
            },
            onError = { message ->
                pollingJob?.cancel()
                _uiState.value = _uiState.value.copy(
                    isListening = false,
                    statusType = StatusType.ERROR,
                    status = "ERROR: $message"
                )
            },
            onStatus = { /* minor status updates during heartbeat / timeout */ }
        )
        client!!.start(ip)

        pollingJob = viewModelScope.launch {
            while (true) {
                delay(200)
                _uiState.value = _uiState.value.copy(
                    packetCount = latestPacketCount,
                    rpm = latestRpm
                )
            }
        }
    }

    fun stopListening() {
        pollingJob?.cancel()
        pollingJob = null
        client?.stopCommunication()
        client = null
        _uiState.value = _uiState.value.copy(
            isListening = false,
            statusType = StatusType.IDLE,
            status = "IDLE: Press 'Start Receiving' to connect",
            packetCount = 0,
            rpm = 0f
        )
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        client?.stopCommunication()
    }
}
