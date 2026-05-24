package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class WavAudioRecorder(private val context: Context) {

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var bufferSize = 0

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private var currentRecordFile: File? = null
    private var startTime = 0L

    init {
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    }

    @SuppressLint("MissingPermission")
    fun startRecording(outputFile: File) {
        if (isRecording) return

        currentRecordFile = outputFile
        
        // Prepare temp raw PCM file
        val tempRawFile = File(context.cacheDir, "temp_audio.pcm")
        if (tempRawFile.exists()) tempRawFile.delete()

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("WavAudioRecorder", "Unable to initialize AudioRecord")
            return
        }

        audioRecord?.startRecording()
        isRecording = true
        startTime = System.currentTimeMillis()

        recordingThread = Thread({
            writeAudioDataToFile(tempRawFile)
            copyWaveFile(tempRawFile, outputFile)
            tempRawFile.delete()
        }, "WavRecorderThread")

        recordingThread?.start()

        // Start duration tracker
        CoroutineScope(Dispatchers.Default).launch {
            while (isRecording) {
                _durationMs.value = System.currentTimeMillis() - startTime
                kotlinx.coroutines.delay(100)
            }
        }
    }

    fun stopRecording(): Long {
        if (!isRecording) return 0L
        isRecording = false

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            recordingThread?.join()
        } catch (e: Exception) {
            Log.e("WavAudioRecorder", "Error stopping recording", e)
        }

        val totalDuration = System.currentTimeMillis() - startTime
        _durationMs.value = 0L
        _amplitude.value = 0f
        return totalDuration
    }

    private fun writeAudioDataToFile(tempRawFile: File) {
        val data = ByteArray(bufferSize)
        var os: FileOutputStream? = null
        try {
            os = FileOutputStream(tempRawFile)
        } catch (e: Exception) {
            Log.e("WavAudioRecorder", "Temp file raw output open error", e)
        }

        if (os != null) {
            while (isRecording) {
                val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                if (AudioRecord.ERROR_INVALID_OPERATION != read && AudioRecord.ERROR_BAD_VALUE != read && read > 0) {
                    try {
                        os.write(data, 0, read)
                        
                        // Calculate maximum amplitude for UI visualizer support
                        var maxAmp = 0
                        for (i in 0 until read step 2) {
                            if (i + 1 < read) {
                                val shortVal = ((data[i + 1].toInt() shl 8) or (data[i].toInt() and 0xff))
                                val absVal = Math.abs(shortVal)
                                if (absVal > maxAmp) {
                                    maxAmp = absVal
                                }
                            }
                        }
                        // Normalize to 0..1f float
                        _amplitude.value = (maxAmp.toFloat() / 32768f).coerceIn(0f, 1f)
                    } catch (e: IOException) {
                        Log.e("WavAudioRecorder", "IOException writing raw PCM data", e)
                    }
                }
            }
            try {
                os.close()
            } catch (e: IOException) {
                Log.e("WavAudioRecorder", "IOException closing temp file", e)
            }
        }
    }

    private fun copyWaveFile(tempRawFile: File, outputFile: File) {
        var inStream: FileInputStream? = null
        var outStream: FileOutputStream? = null
        var totalAudioLen = 0L
        var totalDataLen: Long
        val longSampleRate = sampleRate.toLong()
        val channels = 1
        val byteRate = 16 * sampleRate * channels / 8

        val buffer = ByteArray(bufferSize)

        try {
            inStream = FileInputStream(tempRawFile)
            outStream = FileOutputStream(outputFile)
            totalAudioLen = inStream.channel.size()
            totalDataLen = totalAudioLen + 36

            // Write 44 bytes WAV header
            writeWavHeader(outStream, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate.toLong())

            while (inStream.read(buffer) != -1) {
                outStream.write(buffer)
            }
        } catch (e: Exception) {
            Log.e("WavAudioRecorder", "Error copying and formatting WAV", e)
        } finally {
            try {
                inStream?.close()
                outStream?.close()
            } catch (e: Exception) {
                Log.e("WavAudioRecorder", "Closing streams fail", e)
            }
        }
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        totalAudioLen: Long,
        totalDataLen: Long,
        longSampleRate: Long,
        channels: Int,
        byteRate: Long
    ) {
        val header = ByteArray(44)
        header[0] = 'R'.toByte() // RIFF
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.toByte() // WAVE
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        header[12] = 'f'.toByte() // 'fmt ' chunk
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = ((longSampleRate shr 8) and 0xff).toByte()
        header[26] = ((longSampleRate shr 16) and 0xff).toByte()
        header[27] = ((longSampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.toByte() // 'data' chunk
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
        out.write(header, 0, 44)
    }
}
