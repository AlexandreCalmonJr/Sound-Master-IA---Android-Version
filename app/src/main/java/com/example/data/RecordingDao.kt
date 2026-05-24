package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM audio_recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<AudioRecording>>

    @Query("SELECT * FROM audio_recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: Int): AudioRecording?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: AudioRecording): Long

    @Update
    suspend fun updateRecording(recording: AudioRecording)

    @Delete
    suspend fun deleteRecording(recording: AudioRecording)

    @Query("DELETE FROM audio_recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Int)
}
