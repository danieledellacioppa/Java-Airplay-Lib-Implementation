package com.cjx.airplayjavademo.tools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object LogRepository {

    var isConnectionActive by mutableStateOf(false)

    private val _logs = mutableStateListOf<String>()
    val logs: List<String> get() = _logs // Espone i log solo in lettura

    fun updateConnectionStatus(isActive: Boolean) {
        isConnectionActive = isActive
        addLog("Connection status updated: $isActive")
    }

    fun addLog(message: String) {
        _logs.add(message)
    }

    fun clearLogs() {
        _logs.clear()
    }
}