package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_recordings")
data class AudioRecording(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val filePath: String,                 // Raw / original file path
    val processedFilePath: String? = null, // Processed file path on device
    val durationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val appliedEffect: String? = null,     // denoise, vocal_sep, mastering, transcription
    val transcription: String? = null,    // Speech-to-text response (if any)
    val status: String = "original",       // original, uploading, processing, success, failed
    val errorMessage: String? = null
)
