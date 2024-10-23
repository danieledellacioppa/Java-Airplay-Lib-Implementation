package com.cjx.airplayjavademo.tools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

object LogRepository {
    // Lista di log osservabile dai Composable
    private val logMessages: SnapshotStateList<String> = mutableStateListOf()

    var isConnectionActive by mutableStateOf(false)

    fun setConnection(active: Boolean) {
        isConnectionActive = active
    }

    fun addLog(message: String) {
        logMessages.add(message)
    }

    fun getLogs(): List<String> {
        return logMessages
    }
}