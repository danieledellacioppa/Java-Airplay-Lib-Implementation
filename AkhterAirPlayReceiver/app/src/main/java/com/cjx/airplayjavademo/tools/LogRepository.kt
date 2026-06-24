package com.cjx.airplayjavademo.tools

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class MessageType(val type: String, val color: Color)

data class LogEntry(
    val time: String, // Ora in cui il log è stato aggiunto
    val tag: String,  // Tag del log
    val message: String, // Messaggio del log
    val type: MessageType = MessageType("default", Color.Black) // Tipo di messaggio (INFO, WARNING, ERROR)
)


object LogRepository {

    private const val LOG_DIRECTORY_NAME = "airplay_session_logs"
    private const val TAG = "LogRepository"
    private val fileLock = Any()
    private val fileTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val lineTimestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private var currentLogFile: File? = null

    // Lista di log osservabile dai Composable
    private val logMessages: SnapshotStateList<LogEntry> = mutableStateListOf()

    var isConnectionActive by mutableStateOf(false)

    fun setConnection(active: Boolean) {
        isConnectionActive = active
    }

    fun initialize(context: Context) {
        synchronized(fileLock) {
            if (currentLogFile != null) {
                return
            }

            val directory = File(context.applicationContext.filesDir, LOG_DIRECTORY_NAME)
            if (!directory.exists() && !directory.mkdirs()) {
                Log.e(TAG, "Failed to create log directory: ${directory.absolutePath}")
                return
            }

            val fileName = "airplay_session_${LocalDateTime.now().format(fileTimestampFormatter)}.txt"
            currentLogFile = File(directory, fileName)
        }
        addLog(TAG, "Persistent session log file: ${currentLogPathForAdb()}", 'I')
    }

    fun currentLogPathForAdb(): String {
        val file = synchronized(fileLock) { currentLogFile }
        return file?.let { "files/$LOG_DIRECTORY_NAME/${it.name}" }
            ?: "files/$LOG_DIRECTORY_NAME/<not-initialized>"
    }

    // Aggiunge un log con orario
    fun addLog(tag: String, message: String, type: Char = 'I') {
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val normalizedType = type.uppercaseChar()
        val color = when (normalizedType) {
            'D' -> Color.LightGray
            'I' -> Color.White
            'W' -> Color.Yellow
            'E' -> Color.Red
            else -> Color.Black
        }
        logMessages.add(LogEntry(currentTime, tag, message, MessageType(normalizedType.toString(), color)))
        appendToSessionFile(normalizedType, tag, message)

        when (normalizedType) {
            'D' -> Log.d(tag, message)
            'I' -> Log.i(tag, message)
            'W' -> Log.w(tag, message)
            'E' -> Log.e(tag, message)
            else -> Log.d(tag, message)
        }
    }

    // Restituisce la lista di log
    fun getLogs(): List<LogEntry> {
        return logMessages
    }

    private fun appendToSessionFile(level: Char, tag: String, message: String) {
        val file = synchronized(fileLock) { currentLogFile } ?: return
        val timestamp = LocalDateTime.now().format(lineTimestampFormatter)
        val sanitizedTag = sanitizeForLine(tag)
        val sanitizedMessage = sanitizeForLine(message)
        val line = "$timestamp\t$level\t$sanitizedTag\t$sanitizedMessage\n"

        synchronized(fileLock) {
            try {
                FileWriter(file, true).use { writer ->
                    writer.append(line)
                    writer.flush()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to append session log: ${file.absolutePath}", e)
            }
        }
    }

    private fun sanitizeForLine(value: String): String {
        return value
            .replace("\r", "\\r")
            .replace("\n", "\\n")
    }
}
