package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.api.SoundMasterApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class SoundMasterRepository(
    private val recordingDao: RecordingDao,
    private val context: Context
) {
    val allRecordings: Flow<List<AudioRecording>> = recordingDao.getAllRecordings()

    suspend fun getRecordingById(id: Int): AudioRecording? = withContext(Dispatchers.IO) {
        recordingDao.getRecordingById(id)
    }

    suspend fun insertRecording(recording: AudioRecording): Long = withContext(Dispatchers.IO) {
        recordingDao.insertRecording(recording)
    }

    suspend fun updateRecording(recording: AudioRecording) = withContext(Dispatchers.IO) {
        recordingDao.updateRecording(recording)
    }

    suspend fun deleteRecording(recording: AudioRecording) = withContext(Dispatchers.IO) {
        // Delete local files
        try {
            val originalFile = File(recording.filePath)
            if (originalFile.exists()) originalFile.delete()
        } catch (e: Exception) {
            Log.e("SoundMasterRepository", "Error deleting original file", e)
        }

        recording.processedFilePath?.let { path ->
            try {
                val processedFile = File(path)
                if (processedFile.exists()) processedFile.delete()
            } catch (e: Exception) {
                Log.e("SoundMasterRepository", "Error deleting processed file", e)
            }
        }

        recordingDao.deleteRecording(recording)
    }

    suspend fun deleteRecordingById(id: Int) = withContext(Dispatchers.IO) {
        val recording = recordingDao.getRecordingById(id)
        if (recording != null) {
            deleteRecording(recording)
        }
    }

    suspend fun testApiConnection(baseUrl: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val api = SoundMasterApiClient.getClient(baseUrl)
            
            // Try status
            val statusResponse = try {
                api.checkStatus()
            } catch (e: Exception) {
                null
            }

            if (statusResponse != null && statusResponse.isSuccessful) {
                val body = statusResponse.body()
                return@withContext Pair(true, body?.message ?: body?.status ?: "Online v${body?.version ?: "1.0"}")
            }

            // Try health
            val healthResponse = try {
                api.checkHealth()
            } catch (e: Exception) {
                null
            }

            if (healthResponse != null && healthResponse.isSuccessful) {
                val body = healthResponse.body()
                return@withContext Pair(true, body?.message ?: body?.status ?: "Online")
            }

            // Simple root ping
            val rootResponse = try {
                api.checkRoot()
            } catch (e: Exception) {
                null
            }

            if (rootResponse != null && rootResponse.isSuccessful) {
                return@withContext Pair(true, "API respondendo (Root)")
            }

            Pair(false, "Servidor conectado, mas respondeu com erro (${statusResponse?.code() ?: healthResponse?.code() ?: "Sem corpo"})")
        } catch (e: Exception) {
            Log.e("SoundMasterRepository", "Error checking connection", e)
            Pair(false, "Erro de conexão: ${e.localizedMessage ?: "Servidor inacessível"}")
        }
    }

    suspend fun processAudioOnServer(
        recordingId: Int,
        baseUrl: String,
        effect: String,
        intensity: Float = 1.0f
    ): Boolean = withContext(Dispatchers.IO) {
        val recording = recordingDao.getRecordingById(recordingId) ?: return@withContext false
        
        // Update state to uploading
        recordingDao.updateRecording(recording.copy(status = "uploading", appliedEffect = effect))
        
        try {
            val originalFile = File(recording.filePath)
            if (!originalFile.exists()) {
                recordingDao.updateRecording(recording.copy(status = "failed", errorMessage = "Arquivo original não encontrado"))
                return@withContext false
            }

            val api = SoundMasterApiClient.getClient(baseUrl)
            
            val requestFile = originalFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", originalFile.name, requestFile)
            
            if (effect == "transcription" || effect == "speech_to_text") {
                recordingDao.updateRecording(recording.copy(status = "processing"))
                val response = api.transcribeAudio(filePart)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    recordingDao.updateRecording(
                        recording.copy(
                            status = "success",
                            transcription = body.text,
                            appliedEffect = effect
                        )
                    )
                    return@withContext true
                } else {
                    val code = response.code()
                    val errBody = response.errorBody()?.string() ?: ""
                    recordingDao.updateRecording(
                        recording.copy(
                            status = "failed",
                            errorMessage = "Erro na transcrição ($code): $errBody"
                        )
                    )
                    return@withContext false
                }
            } else {
                // Audio Effects (Denoise, Vocal Sep, Mastering)
                recordingDao.updateRecording(recording.copy(status = "processing"))

                val effectBody = effect.toRequestBody("text/plain".toMediaTypeOrNull())
                val intensityBody = intensity.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                
                // Try processAudio first, fallback to enhanceAudio
                var response = try {
                    api.processAudio(filePart, effectBody, intensityBody)
                } catch (e: Exception) {
                    null
                }

                if (response == null || !response.isSuccessful) {
                    response = try {
                        api.enhanceAudio(filePart, effectBody)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (response != null && response.isSuccessful && response.body() != null) {
                    // Save processed response bytes to local output WAV file
                    val outputFileName = "${originalFile.nameWithoutExtension}_processed_${effect}.wav"
                    val outputFile = File(context.filesDir, outputFileName)
                    
                    if (outputFile.exists()) outputFile.delete()
                    
                    val responseBody = response.body()!!
                    val inputStream = responseBody.byteStream()
                    val outputStream = FileOutputStream(outputFile)
                    
                    val buffer = ByteArray(4096)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()

                    recordingDao.updateRecording(
                        recording.copy(
                            status = "success",
                            processedFilePath = outputFile.absolutePath,
                            appliedEffect = effect
                        )
                    )
                    return@withContext true
                } else {
                    val code = response?.code() ?: -1
                    val errBody = response?.errorBody()?.string() ?: "Sem corpo de erro do servidor"
                    recordingDao.updateRecording(
                        recording.copy(
                            status = "failed",
                            errorMessage = "Erro no processamento de efeitos ($code): $errBody"
                        )
                    )
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e("SoundMasterRepository", "Exceptions during audio processing", e)
            recordingDao.updateRecording(
                recording.copy(
                    status = "failed",
                    errorMessage = "Exception: ${e.localizedMessage ?: "Erro de rede"}"
                )
            )
            return@withContext false
        }
    }
}
