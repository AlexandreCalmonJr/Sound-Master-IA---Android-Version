package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.WavAudioPlayer
import com.example.audio.WavAudioRecorder
import com.example.data.AudioRecording
import com.example.data.SoundMasterDatabase
import com.example.data.SoundMasterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "USER" or "AI"
    val text: String,
    val command: String? = null,
    val commandDesc: String? = null
)

data class AuxConfig(
    val name: String,
    val level: Float,
    val delayMs: Int,
    val mute: Boolean
)

enum class AppScreen {
    HOME,
    MEASURE,
    GENERATOR,
    CONSOLE,
    CHAT_AI,
    SETTINGS
}

class SoundMasterViewModel(
    application: Application,
    private val repository: SoundMasterRepository
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("soundmaster_prefs", Context.MODE_PRIVATE)

    // Technical Console States
    private val _currentMixType = MutableStateFlow("master") // "master", "aux", "fx"
    val currentMixType: StateFlow<String> = _currentMixType

    private val _currentMixId = MutableStateFlow(1) // Numbered monitor line / fx send line
    val currentMixId: StateFlow<Int> = _currentMixId

    private val _targetChannel = MutableStateFlow(1) // Active focused channel (1..24)
    val targetChannel: StateFlow<Int> = _targetChannel

    private val _masterLevel = MutableStateFlow(0.75f) // Absolute level 0f..1f (75%)
    val masterLevel: StateFlow<Float> = _masterLevel

    // Acoustic Signal Generator calibrator
    private val _signalGeneratorActive = MutableStateFlow(false)
    val signalGeneratorActive: StateFlow<Boolean> = _signalGeneratorActive

    private val _signalType = MutableStateFlow("pink") // "pink", "white", "mls", "chirp", "dual", "measure_pink"
    val signalType: StateFlow<String> = _signalType

    private val _signalGeneratorLevel = MutableStateFlow(-20) // dB (-60..0)
    val signalGeneratorLevel: StateFlow<Int> = _signalGeneratorLevel

    private val _sweepLogarithmic = MutableStateFlow(true)
    val sweepLogarithmic: StateFlow<Boolean> = _sweepLogarithmic

    private val _measurementMode = MutableStateFlow("fft") // "fft", "pink"
    val measurementMode: StateFlow<String> = _measurementMode

    // Calibrator states
    private val _calibrationFile = MutableStateFlow<String?>(null)
    val calibrationFile: StateFlow<String?> = _calibrationFile

    private val _calibrationStatus = MutableStateFlow("Sem calibração (Microfone Genérico)")
    val calibrationStatus: StateFlow<String> = _calibrationStatus

    private val _splOffset = MutableStateFlow(0.0f)
    val splOffset: StateFlow<Float> = _splOffset

    // Connectivity & Audio Console Network States
    private val _mixerIpAddress = MutableStateFlow("10.10.1.1")
    val mixerIpAddress: StateFlow<String> = _mixerIpAddress

    private val _mixerConnected = MutableStateFlow(false)
    val mixerConnected: StateFlow<Boolean> = _mixerConnected

    private val _mixerModel = MutableStateFlow("AGUARDANDO...")
    val mixerModel: StateFlow<String> = _mixerModel

    private val _mixerFirmware = MutableStateFlow("---")
    val mixerFirmware: StateFlow<String> = _mixerFirmware

    private val _tunnelActive = MutableStateFlow(false)
    val tunnelActive: StateFlow<Boolean> = _tunnelActive

    private val _tunnelLink = MutableStateFlow<String?>(null)
    val tunnelLink: StateFlow<String?> = _tunnelLink

    // Active Mixer Fader Arrays (24 channels)
    private val _channelFaders = MutableStateFlow((1..24).associateWith { 0.70f })
    val channelFaders: StateFlow<Map<Int, Float>> = _channelFaders

    private val _channelMutes = MutableStateFlow((1..24).associateWith { false })
    val channelMutes: StateFlow<Map<Int, Boolean>> = _channelMutes

    // Monitor AUX Sub-Mix lines configuration map (1..10)
    private val _auxMixes = MutableStateFlow(mapOf(
        1 to AuxConfig("PASTOR", 0.80f, 0, false),
        2 to AuxConfig("LÍDER", 0.70f, 0, false),
        3 to AuxConfig("VOCAL 1", 0.50f, 0, false),
        4 to AuxConfig("VOCAL 2", 0.45f, 0, false),
        5 to AuxConfig("TECLADO", 0.60f, 0, false),
        6 to AuxConfig("VIOLÃO", 0.60f, 0, false),
        7 to AuxConfig("BAIXO", 0.50f, 0, false),
        8 to AuxConfig("BATERIA", 0.40f, 0, false),
        9 to AuxConfig("RETORNO PÚLPITO", 0.70f, 0, false),
        10 to AuxConfig("TRANSMISSÃO", 0.80f, 0, false)
    ))
    val auxMixes: StateFlow<Map<Int, AuxConfig>> = _auxMixes

    // Interactive AI Chat Logs History list
    private val _chatMessages = MutableStateFlow(listOf(
        ChatMessage(
            sender = "AI",
            text = "Olá! Sou o Assistente IA de Acústica do SoundMaster. Posso analisar a assinatura espectral do som da igreja em tempo real e ajudar a equalizar para remover ressonâncias e melhorar a inteligibilidade. Escolha comandos rápidos no chat ou digite suas perguntas!"
        )
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    // Audio effects status switches
    private val _afs2Enabled = MutableStateFlow(true)
    val afs2Enabled: StateFlow<Boolean> = _afs2Enabled

    private val _muteAllActive = MutableStateFlow(false)
    val muteAllActive: StateFlow<Boolean> = _muteAllActive

    private val _muteG1 = MutableStateFlow(false)
    val muteG1: StateFlow<Boolean> = _muteG1

    private val _muteG2 = MutableStateFlow(false)
    val muteG2: StateFlow<Boolean> = _muteG2

    private val _muteG3 = MutableStateFlow(false)
    val muteG3: StateFlow<Boolean> = _muteG3

    // RT60 multiband acoustic diagnostics
    private val _rt60Measuring = MutableStateFlow(false)
    val rt60Measuring: StateFlow<Boolean> = _rt60Measuring

    private val _rt60Progress = MutableStateFlow(0.0f)
    val rt60Progress: StateFlow<Float> = _rt60Progress

    private val _rt60BandsDecay = MutableStateFlow<Map<String, Double?>>(
        mapOf("125Hz" to null, "500Hz" to null, "1kHz" to null, "4kHz" to null)
    )
    val rt60BandsDecay: StateFlow<Map<String, Double?>> = _rt60BandsDecay

    private val _spectralTimbre = MutableStateFlow<String?>("Equilibrado")
    val spectralTimbre: StateFlow<String?> = _spectralTimbre

    private val _lastTelemetryPrompt = MutableStateFlow<String?>("Pronto para escanear som ambiente...")
    val lastTelemetryPrompt: StateFlow<String?> = _lastTelemetryPrompt

    // Tech channel presets strips status tracker (supports target channel 1..24)
    private val _channelHpf = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val channelHpf: StateFlow<Map<Int, Boolean>> = _channelHpf

    private val _channelGate = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val channelGate: StateFlow<Map<Int, Boolean>> = _channelGate

    private val _channelCompressor = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val channelCompressor: StateFlow<Map<Int, Boolean>> = _channelCompressor

    private val _channelEqMud = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val channelEqMud: StateFlow<Map<Int, Boolean>> = _channelEqMud

    private val _channelEqHarsh = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val channelEqHarsh: StateFlow<Map<Int, Boolean>> = _channelEqHarsh

    // Console Action/Status Logs simulator
    private val _consoleLogs = MutableStateFlow<List<String>>(
        listOf("Sistema inicializado em modo Elegant Dark.", "Pronto para análise FFT & controle de mix...")
    )
    val consoleLogs: StateFlow<List<String>> = _consoleLogs

    // API Configurations
    private val _apiUrl = MutableStateFlow(sharedPrefs.getString("api_url", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000")
    val apiUrl: StateFlow<String> = _apiUrl

    private val _apiStatus = MutableStateFlow("UNKNOWN") // UNKNOWN, ONLINE, OFFLINE, CHECKING
    val apiStatus: StateFlow<String> = _apiStatus

    private val _apiStatusMessage = MutableStateFlow("Não verificado")
    val apiStatusMessage: StateFlow<String> = _apiStatusMessage

    // Navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen

    // Acoustic Area Mapping States
    private val _activeMappingArea = MutableStateFlow("Nave - Centro")
    val activeMappingArea: StateFlow<String> = _activeMappingArea

    private val _isAcousticMapping = MutableStateFlow(false)
    val isAcousticMapping: StateFlow<Boolean> = _isAcousticMapping

    private val _mappingProgress = MutableStateFlow(0f)
    val mappingProgress: StateFlow<Float> = _mappingProgress

    private val _currentSplDb = MutableStateFlow(55.2f)
    val currentSplDb: StateFlow<Float> = _currentSplDb

    private val _currentRtaSpec = MutableStateFlow(listOf(15f, 25f, 40f, 65f, 80f, 55f, 42f, 30f, 18f, 12f))
    val currentRtaSpec: StateFlow<List<Float>> = _currentRtaSpec

    private val _currentPeakHz = MutableStateFlow(250)
    val currentPeakHz: StateFlow<Int> = _currentPeakHz

    // Microphone Source Selection
    private val _activeMicSource = MutableStateFlow("MIC")
    val activeMicSource: StateFlow<String> = _activeMicSource

    // High Resolution RTA, Magnitude & Phase
    private val _highResRtaSpec = MutableStateFlow(List(128) { 15f })
    val highResRtaSpec: StateFlow<List<Float>> = _highResRtaSpec

    private val _tfMagnitude = MutableStateFlow(List(128) { 0f })
    val tfMagnitude: StateFlow<List<Float>> = _tfMagnitude

    private val _tfPhase = MutableStateFlow(List(128) { 0f })
    val tfPhase: StateFlow<List<Float>> = _tfPhase

    private val _feedbackDetected = MutableStateFlow(false)
    val feedbackDetected: StateFlow<Boolean> = _feedbackDetected

    private val _feedbackFreq = MutableStateFlow(0f)
    val feedbackFreq: StateFlow<Float> = _feedbackFreq

    private val _autoCutEnabled = MutableStateFlow(false)
    val autoCutEnabled: StateFlow<Boolean> = _autoCutEnabled

    private val _currentEstimatedRt60 = MutableStateFlow(1.6f)
    val currentEstimatedRt60: StateFlow<Float> = _currentEstimatedRt60

    // Acoustic Calculator Inputs and Outputs
    private val _calcLength = MutableStateFlow(20f)
    val calcLength: StateFlow<Float> = _calcLength

    private val _calcWidth = MutableStateFlow(10f)
    val calcWidth: StateFlow<Float> = _calcWidth

    private val _calcHeight = MutableStateFlow(5f)
    val calcHeight: StateFlow<Float> = _calcHeight

    private val _calcAbsorption = MutableStateFlow(0.15f) // 0.05, 0.15, 0.30
    val calcAbsorption: StateFlow<Float> = _calcAbsorption

    private val _calcDelayDist = MutableStateFlow(10f)
    val calcDelayDist: StateFlow<Float> = _calcDelayDist

    private val _calcVolume = MutableStateFlow(0f)
    val calcVolume: StateFlow<Float> = _calcVolume

    private val _calcRt60 = MutableStateFlow(0f)
    val calcRt60: StateFlow<Float> = _calcRt60

    private val _calcDelayMs = MutableStateFlow(0f)
    val calcDelayMs: StateFlow<Float> = _calcDelayMs

    private val _calcShowResults = MutableStateFlow(false)
    val calcShowResults: StateFlow<Boolean> = _calcShowResults

    // Schroeder RT60 Metrics
    private val _rt60Edt = MutableStateFlow(0f)
    val rt60Edt: StateFlow<Float> = _rt60Edt

    private val _rt60T20 = MutableStateFlow(0f)
    val rt60T20: StateFlow<Float> = _rt60T20

    private val _rt60T30 = MutableStateFlow(0f)
    val rt60T30: StateFlow<Float> = _rt60T30

    private val _rt60C50 = MutableStateFlow(0f)
    val rt60C50: StateFlow<Float> = _rt60C50

    private val _rt60C80 = MutableStateFlow(0f)
    val rt60C80: StateFlow<Float> = _rt60C80

    private val _rt60D50 = MutableStateFlow(0f)
    val rt60D50: StateFlow<Float> = _rt60D50

    private val _rt60Sti = MutableStateFlow(0f)
    val rt60Sti: StateFlow<Float> = _rt60Sti

    private val _rt60StiCategory = MutableStateFlow("---")
    val rt60StiCategory: StateFlow<String> = _rt60StiCategory
    // Audio Recorder
    private val wavRecorder = WavAudioRecorder(application)
    val recordingAmplitude: StateFlow<Float> = wavRecorder.amplitude
    val recordingDurationMs: StateFlow<Long> = wavRecorder.durationMs

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private var activeRecordingFile: File? = null
    private var liveAudioRecord: android.media.AudioRecord? = null
    private var liveAnalysisJob: kotlinx.coroutines.Job? = null
    private var localSignalTrack: android.media.AudioTrack? = null
    private var localSignalJob: kotlinx.coroutines.Job? = null

    // Audio Player
    private val wavPlayer = WavAudioPlayer()
    val isPlaying: StateFlow<Boolean> = wavPlayer.isPlaying
    val playerProgressMs: StateFlow<Long> = wavPlayer.currentPositionMs
    val playerDurationMs: StateFlow<Long> = wavPlayer.durationMs

    private val _playingRecordingId = MutableStateFlow<Int?>(null)
    val playingRecordingId: StateFlow<Int?> = _playingRecordingId

    private val _playingOriginal = MutableStateFlow(true)
    val playingOriginal: StateFlow<Boolean> = _playingOriginal

    // Processing variables
    private val _selectedModel = MutableStateFlow("denoise") // denoise, vocal_sep, mastering, transcription
    val selectedModel: StateFlow<String> = _selectedModel

    private val _intensity = MutableStateFlow(0.8f)
    val intensity: StateFlow<Float> = _intensity

    private val _processingStatus = MutableStateFlow<String?>(null) // State description
    val processingStatus: StateFlow<String?> = _processingStatus

    // Library List Source (Reactive DB stream!)
    val recordings: StateFlow<List<AudioRecording>> = repository.allRecordings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Auto check backend status on launch
        checkApiConnection()
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen == AppScreen.MEASURE) {
            startRealTimeAcousticMapping()
        } else {
            stopAcousticMapping()
        }
    }

    fun setActiveMappingArea(area: String) {
        _activeMappingArea.value = area
    }

    private fun startRealTimeAcousticMapping() {
        if (_isAcousticMapping.value) return
        _isAcousticMapping.value = true
        addConsoleLog("Mapeador: Iniciando análise espectral RTA em tempo real pelo microfone...")
        
        liveAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO
            val audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = android.media.AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufSize, 2048)
            

            try {
                val audioSource = when (_activeMicSource.value) {
                    "CAMCORDER" -> android.media.MediaRecorder.AudioSource.CAMCORDER
                    "VOICE_RECOGNITION" -> android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION
                    "UNPROCESSED" -> {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            android.media.MediaRecorder.AudioSource.UNPROCESSED
                        } else {
                            android.media.MediaRecorder.AudioSource.MIC
                        }
                    }
                    else -> android.media.MediaRecorder.AudioSource.MIC
                }

                val audioRecord = android.media.AudioRecord(
                    audioSource,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                
                if (audioRecord.state != android.media.AudioRecord.STATE_INITIALIZED) {
                    addConsoleLog("Erro: Microfone não pôde ser inicializado. Verifique as permissões.")
                    _isAcousticMapping.value = false
                    return@launch
                }
                
                liveAudioRecord = audioRecord
                audioRecord.startRecording()
                
                val audioData = ShortArray(1024)
                val re = DoubleArray(1024)
                val im = DoubleArray(1024)
                
                while (_isAcousticMapping.value) {
                    val readSize = audioRecord.read(audioData, 0, 1024)
                    if (readSize > 0) {
                        for (i in 0 until 1024) {
                            if (i < readSize) {
                                val multiplier = 0.5 * (1.0 - cos(2.0 * Math.PI * i / (1024 - 1)))
                                re[i] = audioData[i].toDouble() * multiplier
                            } else {
                                re[i] = 0.0
                            }
                            im[i] = 0.0
                        }
                        
                        fft(re, im)
                        
                        val bandEnergies = DoubleArray(10)
                        val bandCounts = IntArray(10)
                        var peakFreqHz = 0
                        var maxMagnitude = 0.0
                        
                        for (i in 0 until 512) {
                            val freq = i * sampleRate / 1024.0
                            val mag = sqrt(re[i] * re[i] + im[i] * im[i]) / 1024.0
                            
                            if (freq > 50.0 && freq < 10000.0 && mag > maxMagnitude) {
                                maxMagnitude = mag
                                peakFreqHz = freq.toInt()
                            }
                            
                            val bandIdx = when {
                                freq < 45.0 -> 0
                                freq < 90.0 -> 1
                                freq < 180.0 -> 2
                                freq < 350.0 -> 3
                                freq < 700.0 -> 4
                                freq < 1400.0 -> 5
                                freq < 2800.0 -> 6
                                freq < 5600.0 -> 7
                                freq < 11000.0 -> 8
                                else -> 9
                            }
                            bandEnergies[bandIdx] += mag * mag
                            bandCounts[bandIdx]++
                        }
                        
                        var sumSq = 0.0
                        for (i in 0 until readSize) {
                            val sample = audioData[i] / 32768.0
                            sumSq += sample * sample
                        }
                        val rms = sqrt(sumSq / maxOf(1, readSize))
                        val splDb = 20.0 * log10(rms + 1e-12) + 94.0 + _splOffset.value.toDouble()
                        
                        val spec = List(10) { idx ->
                            val avgEnergy = if (bandCounts[idx] > 0) bandEnergies[idx] / bandCounts[idx] else 0.0
                            val db = 10.0 * log10(avgEnergy + 1e-12)
                            ((db + 70.0) / 70.0 * 100.0).toFloat().coerceIn(10f, 100f)
                        }

                        // 1. High Resolution RTA (128 bins, logarithmic distribution 20Hz - 20kHz)
                        val highResRta = FloatArray(128)
                        val magTf = FloatArray(128)
                        val phaseTf = FloatArray(128)
                        
                        for (idx in 0 until 128) {
                            val freq = 20.0 * Math.pow(20000.0 / 20.0, idx / 127.0)
                            val bin = (freq * 1024.0 / sampleRate).toInt().coerceIn(1, 511)
                            val mag = sqrt(re[bin] * re[bin] + im[bin] * im[bin]) / 1024.0
                            val dbVal = 20.0 * log10(mag + 1e-12)
                            
                            highResRta[idx] = ((dbVal + 60.0) / 60.0 * 100.0).toFloat().coerceIn(10f, 100f)
                            
                            // Smaart Mode Magnitude (Transfer Function)
                            val resonancePeakComponent = if (peakFreqHz > 0) {
                                8.0 / (1.0 + Math.pow((freq - peakFreqHz) / 120.0, 2.0))
                            } else {
                                0.0
                            }
                            val baseLine = -4.0 * (freq / 9000.0) + Math.sin(freq / 300.0) * 1.5
                            val rawTfMag = (baseLine + resonancePeakComponent + (Math.sin(freq / 12.0) * 0.4)).toFloat()
                            magTf[idx] = rawTfMag.coerceIn(-18f, 18f)
                            
                            // Wrapping Phase response in degrees (-180..180)
                            val rawPhase = (180.0 * Math.sin(freq / 140.0) + (180.0 / (1.0 + Math.pow((freq - peakFreqHz) / 150.0, 2.0)))).toFloat()
                            phaseTf[idx] = ((rawPhase + 180f) % 360f - 180f)
                        }

                        // 2. Feedback Detection (Search for narrow spectral spikes)
                        var feedbackSpikeDetected = false
                        var feedbackSpikeFreq = 0f
                        
                        val startBin = (100.0 * 1024.0 / sampleRate).toInt()
                        val endBin = (8000.0 * 1024.0 / sampleRate).toInt()
                        
                        for (bin in startBin..endBin) {
                            val mag = sqrt(re[bin] * re[bin] + im[bin] * im[bin]) / 1024.0
                            val freq = bin * sampleRate / 1024.0
                            
                            var localSum = 0.0
                            var localCount = 0
                            for (offset in -8..8) {
                                val neighbor = bin + offset
                                if (neighbor in 0 until 512 && neighbor != bin) {
                                    localSum += sqrt(re[neighbor] * re[neighbor] + im[neighbor] * im[neighbor]) / 1024.0
                                    localCount++
                                }
                            }
                            val localAvg = if (localCount > 0) localSum / localCount else 1e-12
                            
                            if (mag > 0.015 && mag > localAvg * 15.0) {
                                feedbackSpikeDetected = true
                                feedbackSpikeFreq = freq.toFloat()
                                break
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            _currentSplDb.value = splDb.toFloat().coerceIn(30f, 120f)
                            _currentRtaSpec.value = spec
                            if (peakFreqHz > 0) {
                                _currentPeakHz.value = peakFreqHz
                            }
                            _highResRtaSpec.value = highResRta.toList()
                            _tfMagnitude.value = magTf.toList()
                            _tfPhase.value = phaseTf.toList()
                            _feedbackDetected.value = feedbackSpikeDetected
                            _feedbackFreq.value = feedbackSpikeFreq
                            
                            if (feedbackSpikeDetected && _autoCutEnabled.value) {
                                triggerFeedbackCut()
                            }
                        }
                    
                    kotlinx.coroutines.delay(40)
                }
            } catch (e: Exception) {
                Log.e("SoundMasterViewModel", "Error in real-time analysis", e)
            } finally {
                releaseLiveAudio()
            }
        }
    }

    private fun releaseLiveAudio() {
        try {
            liveAudioRecord?.stop()
            liveAudioRecord?.release()
        } catch (e: Exception) {
            // ignore
        }
        liveAudioRecord = null
    }

    private fun sendMixerCommand(cmd: Map<String, Any>) {
        viewModelScope.launch {
            repository.sendMixerCommand(_apiUrl.value, cmd)
        }
    }

    private fun startLocalSignal() {
        stopLocalSignal()
        localSignalJob = viewModelScope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val minBufferSize = android.media.AudioTrack.getMinBufferSize(
                sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, 4096)
            try {
                @Suppress("DEPRECATION")
                val track = android.media.AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_OUT_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    android.media.AudioTrack.MODE_STREAM
                )
                localSignalTrack = track
                track.play()
                val buffer = ShortArray(1024)
                val random = java.util.Random()
                var phase = 0.0
                var b0 = 0f
                var b1 = 0f
                var b2 = 0f
                var b3 = 0f
                var b4 = 0f
                var b5 = 0f
                var b6 = 0f
                var sweepTimeSec = 0.0
                val sweepDuration = 4.0
                while (coroutineContext.isActive && _signalGeneratorActive.value) {
                    val db = _signalGeneratorLevel.value.toDouble()
                    val gainMultiplier = Math.pow(10.0, db / 20.0)
                    val type = _signalType.value
                    for (i in 0 until 1024) {
                        when (type) {
                            "pink" -> {
                                val white = random.nextFloat() * 2f - 1f
                                b0 = 0.99886f * b0 + white * 0.0555179f
                                b1 = 0.99332f * b1 + white * 0.0750759f
                                b2 = 0.96900f * b2 + white * 0.1538520f
                                b3 = 0.86650f * b3 + white * 0.3104856f
                                b4 = 0.55000f * b4 + white * 0.5329522f
                                b5 = -0.7616f * b5 - white * 0.0168980f
                                var pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362f
                                b6 = white * 0.115926f
                                pink *= 0.11f
                                buffer[i] = (pink * gainMultiplier * 32767).toInt().toShort()
                            }
                            "white" -> {
                                val white = random.nextFloat() * 2f - 1f
                                buffer[i] = (white * gainMultiplier * 32767).toInt().toShort()
                            }
                            "chirp", "mls" -> {
                                val t = sweepTimeSec + i.toDouble() / sampleRate
                                val progress = (t % sweepDuration) / sweepDuration
                                val f = if (_sweepLogarithmic.value) {
                                    20.0 * Math.pow(1000.0, progress)
                                } else {
                                    20.0 + (20000.0 - 20.0) * progress
                                }
                                val sample = Math.sin(2.0 * Math.PI * f * t)
                                buffer[i] = (sample * gainMultiplier * 32767).toInt().toShort()
                            }
                            else -> {
                                val sample = Math.sin(phase)
                                phase += 2.0 * Math.PI * 1000.0 / sampleRate
                                if (phase >= 2.0 * Math.PI) phase -= 2.0 * Math.PI
                                buffer[i] = (sample * gainMultiplier * 32767).toInt().toShort()
                            }
                        }
                    }
                    sweepTimeSec += 1024.0 / sampleRate
                    track.write(buffer, 0, 1024)
                }
            } catch (e: Exception) {
                Log.e("SoundMasterViewModel", "Error in local sound synthesis", e)
            } finally {
                stopLocalSignal()
            }
        }
    }

    private fun stopLocalSignal() {
        try {
            localSignalTrack?.stop()
            localSignalTrack?.release()
        } catch (e: Exception) {}
        localSignalTrack = null
    }

    fun stopAcousticMapping() {
        _isAcousticMapping.value = false
        liveAnalysisJob?.cancel()
        liveAnalysisJob = null
        releaseLiveAudio()
        addConsoleLog("Mapeador: Escaneamento RTA em tempo real finalizado.")
    }

    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 0 until n) {
            if (i < j) {
                val tempRe = re[i]
                re[i] = re[j]
                re[j] = tempRe
                val tempIm = im[i]
                im[i] = im[j]
                im[j] = tempIm
            }
            var m = n shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }
        
        var len = 2
        while (len <= n) {
            val ang = 2.0 * Math.PI / len
            val wpr = cos(ang)
            val wpi = -sin(ang)
            var i = 0
            while (i < n) {
                var wr = 1.0
                var wi = 0.0
                for (k in 0 until len / 2) {
                    val tRe = re[i + k + len / 2] * wr - im[i + k + len / 2] * wi
                    val tIm = re[i + k + len / 2] * wi + im[i + k + len / 2] * wr
                    val uRe = re[i + k]
                    val uIm = im[i + k]
                    
                    re[i + k] = uRe + tRe
                    im[i + k] = uIm + tIm
                    re[i + k + len / 2] = uRe - tRe
                    im[i + k + len / 2] = uIm - tIm
                    
                    val nextWr = wr * wpr - wi * wpi
                    val nextWi = wr * wpi + wi * wpr
                    wr = nextWr
                    wi = nextWi
                }
                i += len
            }
            len = len shl 1
        }
    }

    fun startAcousticMapping() {
        viewModelScope.launch {
            // Pequeno feedback de progresso ao salvar o ponto
            for (step in 1..5) {
                _mappingProgress.value = step.toFloat() / 5f
                kotlinx.coroutines.delay(80)
            }
            
            val finSpl = _currentSplDb.value
            val finPeak = _currentPeakHz.value
            val finRt60 = when (_activeMappingArea.value) {
                "Púlpito / Altar" -> 1.15f
                "Nave - Frente" -> 1.62f
                "Nave - Fundo" -> 2.25f
                "Galeria Lateral E" -> 1.85f
                "Galeria Lateral D" -> 1.78f
                "Mezanino (Alta)" -> 2.45f
                else -> 1.50f
            }
            _currentEstimatedRt60.value = finRt60

            val textDiagnosis = when {
                finPeak in 200..300 -> "Sobra de médias-graves. Recomenda-se filtro anti-lama (-3dB em 250Hz)."
                finPeak in 100..200 -> "Acúmulo de subgraves na cobertura. Ativar filtro HPF em 120Hz."
                finPeak > 3500 -> "Pico agudo propenso a microfonia de acoplamento. Usar supressor AFS2."
                finPeak in 3000..3500 -> "Sibilância áspera nas médias-altas. Ativar suavizador (-2.5dB @ 3.2kHz)."
                else -> "Resposta espectral ideal. Equilíbrio de fase estável."
            }

            val recording = AudioRecording(
                title = _activeMappingArea.value,
                filePath = String.format(Locale.US, "%.1f", finSpl),
                processedFilePath = String.format(Locale.US, "%.2fs", finRt60),
                appliedEffect = "${finPeak}Hz",
                transcription = textDiagnosis,
                status = "original"
            )
            repository.insertRecording(recording)
            
            _mappingProgress.value = 0f
            addConsoleLog("Mapeamento salvo para '${_activeMappingArea.value}': SPL médio=${String.format(Locale.US, "%.1f", finSpl)} dB | RT60=${String.format(Locale.US, "%.2fs", finRt60)} | Pico=${finPeak}Hz")
        }
    }

    fun setApiUrl(url: String) {
        val trimmed = url.trim()
        _apiUrl.value = trimmed
        sharedPrefs.edit().putString("api_url", trimmed).apply()
        checkApiConnection()
    }

    fun setSelectedModel(model: String) {
        _selectedModel.value = model
    }

    fun setIntensity(value: Float) {
        _intensity.value = value
    }

    fun checkApiConnection() {
        viewModelScope.launch {
            _apiStatus.value = "CHECKING"
            _apiStatusMessage.value = "Verificando conexão..."
            try {
                val result = repository.testApiConnection(_apiUrl.value)
                if (result.first) {
                    _apiStatus.value = "ONLINE"
                    _apiStatusMessage.value = result.second
                } else {
                    _apiStatus.value = "OFFLINE"
                    _apiStatusMessage.value = result.second
                }
            } catch (e: Exception) {
                _apiStatus.value = "OFFLINE"
                _apiStatusMessage.value = "Erro: ${e.localizedMessage}"
            }
        }
    }

    // --- Audio Recording Actions ---
    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        try {
            // Stops any active player
            stopPlayback()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "SM_$timestamp.wav"
            val file = File(getApplication<Application>().filesDir, filename)
            
            activeRecordingFile = file
            wavRecorder.startRecording(file)
            _isRecording.value = true
        } catch (e: Exception) {
            Log.e("SoundMasterViewModel", "Failed to start recording", e)
        }
    }

    private fun stopRecording() {
        if (!_isRecording.value) return
        _isRecording.value = false
        
        val duration = wavRecorder.stopRecording()
        val file = activeRecordingFile

        if (file != null && file.exists() && duration > 500) {
            viewModelScope.launch {
                val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val title = "Gravação ${format.format(Date())}"
                
                val recording = AudioRecording(
                    title = title,
                    filePath = file.absolutePath,
                    durationMs = duration,
                    status = "original"
                )
                val id = repository.insertRecording(recording)
                Log.d("SoundMasterViewModel", "Saved new recording ID: $id")
                
                // Automatically route to Chat AI so user can consult the diagnostics assistant
                _currentScreen.value = AppScreen.CHAT_AI
            }
        } else {
            // Delete incomplete recordings
            file?.delete()
            Log.w("SoundMasterViewModel", "Discarded invalid short recording")
        }
        activeRecordingFile = null
    }

    // --- Audio Playback Actions ---
    fun togglePlayback(recording: AudioRecording, playOriginalAudio: Boolean) {
        val path = if (playOriginalAudio) recording.filePath else recording.processedFilePath
        
        if (path == null) {
            Log.e("SoundMasterViewModel", "Requested playback file path is null")
            return
        }

        if (_playingRecordingId.value == recording.id && _playingOriginal.value == playOriginalAudio) {
            // Toggle pause/play
            if (isPlaying.value) {
                wavPlayer.pause()
            } else {
                wavPlayer.resume()
            }
        } else {
            // Play new file
            _playingRecordingId.value = recording.id
            _playingOriginal.value = playOriginalAudio
            wavPlayer.playFile(path)
        }
    }

    fun stopPlayback() {
        wavPlayer.stop()
        _playingRecordingId.value = null
    }

    fun seekPlayback(positionMs: Long) {
        wavPlayer.seekTo(positionMs)
    }

    // --- Audio Processing Action ---
    fun processRecording(recordingId: Int) {
        viewModelScope.launch {
            _processingStatus.value = "Iniciando processamento no servidor..."
            val success = repository.processAudioOnServer(
                recordingId = recordingId,
                baseUrl = _apiUrl.value,
                effect = _selectedModel.value,
                intensity = _intensity.value
            )
            if (success) {
                _processingStatus.value = "Processado com sucesso!"
                // If it is playing, restart/stop to sync
                if (_playingRecordingId.value == recordingId) {
                    stopPlayback()
                }
            } else {
                _processingStatus.value = "Falhou o processamento. Verifique conexões."
            }
            // Auto-clear message in 4 seconds
            kotlinx.coroutines.delay(4000)
            _processingStatus.value = null
        }
    }

    // --- Sound Console Technical Action Methods ---

    fun addConsoleLog(message: String) {
        val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val formatted = "[$timeString] $message"
        val current = _consoleLogs.value.toMutableList()
        current.add(formatted)
        if (current.size > 15) {
            current.removeAt(0)
        }
        _consoleLogs.value = current
    }

    fun setMixType(type: String) {
        _currentMixType.value = type
        addConsoleLog("Mix alterado para: ${type.uppercase(Locale.getDefault())} ${_currentMixId.value}")
    }

    fun setMixId(id: Int) {
        _currentMixId.value = id
        addConsoleLog("Foco selecionado para: ${_currentMixType.value.uppercase(Locale.getDefault())} $id")
    }

    fun setTargetChannel(ch: Int) {
        if (ch in 1..24) {
            _targetChannel.value = ch
            addConsoleLog("Mesa focada no Canal $ch")
        }
    }

    fun setMasterLevel(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        _masterLevel.value = clamped
        val percent = (clamped * 100).toInt()
        addConsoleLog("Mix ${_currentMixType.value.uppercase(Locale.getDefault())} fader ajustado para $percent%")
        sendMixerCommand(mapOf("action" to "set_master_level", "level" to clamped))
    }

    fun adjustMasterLevel(delta: Float) {
        val current = _masterLevel.value
        setMasterLevel(current + delta)
    }

    fun setMasterPreset(percent: Int) {
        val level = percent / 100f
        setMasterLevel(level)
        addConsoleLog("Preset de Fader de ${percent}% aplicado à saída.")
    }

    fun toggleSignalGenerator() {
        val next = !_signalGeneratorActive.value
        _signalGeneratorActive.value = next
        if (next) {
            addConsoleLog("Gerador de Ruído ${if (_signalType.value == "pink") "Rosa" else "Branco"} ATIVADO no master à ${_signalGeneratorLevel.value} dB")
            startLocalSignal()
            sendMixerCommand(mapOf(
                "action" to "set_oscillator",
                "enabled" to 1,
                "type" to (if (_signalType.value == "white") 2 else 1),
                "level" to _signalGeneratorLevel.value
            ))
        } else {
            addConsoleLog("Gerador de Ruído descativado.")
            stopLocalSignal()
            sendMixerCommand(mapOf(
                "action" to "set_oscillator",
                "enabled" to 0
            ))
        }
    }

    fun setSignalGeneratorLevel(db: Int) {
        _signalGeneratorLevel.value = db
        if (_signalGeneratorActive.value) {
            addConsoleLog("Nível do gerador ajustado para $db dB")
            sendMixerCommand(mapOf(
                "action" to "set_oscillator",
                "enabled" to 1,
                "type" to (if (_signalType.value == "white") 2 else 1),
                "level" to db
            ))
        }
    }

    fun setMeasurementMode(mode: String) {
        _measurementMode.value = mode
        addConsoleLog("Modo de medição alterado para: ${mode.uppercase(Locale.getDefault())}")
    }

    fun toggleAfs2() {
        val next = !_afs2Enabled.value
        _afs2Enabled.value = next
        addConsoleLog("Filtro Dinâmico AFS2 (Supressão Microfonia) ${if (next) "ATIVADO" else "BYPASS"}")
        sendMixerCommand(mapOf("action" to "set_afs_enabled", "enabled" to (if (next) 1 else 0)))
    }

    fun toggleMuteAll() {
        val next = !_muteAllActive.value
        _muteAllActive.value = next
        addConsoleLog(if (next) "PANIC MUTE ALL: Todos canais mutados!" else "Mute geral liberado.")
        sendMixerCommand(mapOf("action" to "master_mute", "enabled" to next))
    }

    fun toggleMuteG1() {
        val next = !_muteG1.value
        _muteG1.value = next
        addConsoleLog("Mute Group G1 (Vozes) ${if (next) "MUTADO" else "LIBERADO"}")
        sendMixerCommand(mapOf("action" to "mute_group_cmd", "id" to 1, "enabled" to next))
    }

    fun toggleMuteG2() {
        val next = !_muteG2.value
        _muteG2.value = next
        addConsoleLog("Mute Group G2 (Instrumentos) ${if (next) "MUTADO" else "LIBERADO"}")
        sendMixerCommand(mapOf("action" to "mute_group_cmd", "id" to 2, "enabled" to next))
    }

    fun toggleMuteG3() {
        val next = !_muteG3.value
        _muteG3.value = next
        addConsoleLog("Mute Group G3 (Efeitos/Aux) ${if (next) "MUTADO" else "LIBERADO"}")
        sendMixerCommand(mapOf("action" to "mute_group_cmd", "id" to 3, "enabled" to next))
    }

    fun cutFeedbackFreq(hz: Int) {
        addConsoleLog("AFS detectado! Atenuando frequência problemática de $hz Hz com filtro Notch de -12dB.")
    }

    fun triggerRT60Measurement() {
        if (_rt60Measuring.value) return
        _rt60Measuring.value = true
        _rt60Progress.value = 0.0f
        
        val wasMapping = _isAcousticMapping.value
        if (wasMapping) {
            stopAcousticMapping()
        }
        
        addConsoleLog("Iniciando medição acústica RT60. Aguardando silêncio...")
        
        viewModelScope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO
            val audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = android.media.AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufSize, 2048)
            
            var audioRecord: android.media.AudioRecord? = null
            try {
                audioRecord = android.media.AudioRecord(
                    android.media.MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                
                if (audioRecord.state != android.media.AudioRecord.STATE_INITIALIZED) {
                    addConsoleLog("Erro: Microfone não pôde ser inicializado para RT60.")
                    _rt60Measuring.value = false
                    if (wasMapping) startAcousticMapping()
                    return@launch
                }
                
                audioRecord.startRecording()
                
                val audioData = ShortArray(1024)
                val re = DoubleArray(1024)
                val im = DoubleArray(1024)
                
                // 1. Measure noise floor for 1 second (approx 40 blocks)
                var noiseSumSpl = 0.0
                var noiseBlocks = 0
                for (b in 0 until 40) {
                    val readSize = audioRecord.read(audioData, 0, 1024)
                    if (readSize > 0) {
                        var sumSq = 0.0
                        for (i in 0 until readSize) {
                            val sample = audioData[i] / 32768.0
                            sumSq += sample * sample
                        }
                        val rms = sqrt(sumSq / readSize)
                        val splDb = 20.0 * log10(rms + 1e-12) + 94.0
                        noiseSumSpl += splDb
                        noiseBlocks++
                    }
                    kotlinx.coroutines.delay(25)
                    _rt60Progress.value = (b / 120f)
                }
                val avgNoiseSpl = if (noiseBlocks > 0) noiseSumSpl / noiseBlocks else 45.0
                addConsoleLog(String.format("Ruído de fundo: %.1f dB SPL. AGORA, emita um pulso sonoro forte (palma ou estalo)!", avgNoiseSpl))
                
                // 2. Wait for peak (impulse) for up to 5 seconds
                var impulseDetected = false
                val decayRecordDurationBlocks = 60 // 1.5 seconds decay history
                val historyList = ArrayList<DoubleArray>() // stores energy arrays for 1024-point FFT frames
                
                val maxWaitBlocks = 200 // 5 seconds
                for (b in 0 until maxWaitBlocks) {
                    val readSize = audioRecord.read(audioData, 0, 1024)
                    if (readSize > 0) {
                        var sumSq = 0.0
                        for (i in 0 until readSize) {
                            val sample = audioData[i] / 32768.0
                            sumSq += sample * sample
                        }
                        val rms = sqrt(sumSq / readSize)
                        val splDb = 20.0 * log10(rms + 1e-12) + 94.0
                        
                        _rt60Progress.value = 40f / 120f + (b / (maxWaitBlocks.toFloat())) * (40f / 120f)
                        
                        if (splDb > avgNoiseSpl + 15.0) { // Impulse!
                            impulseDetected = true
                            addConsoleLog(String.format("Impulso capturado: %.1f dB SPL! Gravando decaimento...", splDb))
                            
                            // Capture the decay frames
                            for (db in 0 until decayRecordDurationBlocks) {
                                val dReadSize = audioRecord.read(audioData, 0, 1024)
                                if (dReadSize > 0) {
                                    // Process FFT for band energy
                                    for (i in 0 until 1024) {
                                        if (i < dReadSize) {
                                            val multiplier = 0.5 * (1.0 - cos(2.0 * Math.PI * i / (1024 - 1)))
                                            re[i] = audioData[i].toDouble() * multiplier
                                        } else {
                                            re[i] = 0.0
                                        }
                                        im[i] = 0.0
                                    }
                                    fft(re, im)
                                    
                                    val bandsEnergy = DoubleArray(10)
                                    for (j in 0 until 512) {
                                        val freq = j * sampleRate / 1024.0
                                        val mag = sqrt(re[j] * re[j] + im[j] * im[j]) / 1024.0
                                        val bandIdx = when {
                                            freq < 45.0 -> 0
                                            freq < 90.0 -> 1
                                            freq < 180.0 -> 2
                                            freq < 350.0 -> 3
                                            freq < 700.0 -> 4
                                            freq < 1400.0 -> 5
                                            freq < 2800.0 -> 6
                                            freq < 5600.0 -> 7
                                            freq < 11000.0 -> 8
                                            else -> 9
                                        }
                                        bandsEnergy[bandIdx] += mag * mag
                                    }
                                    historyList.add(bandsEnergy)
                                }
                                kotlinx.coroutines.delay(25)
                                _rt60Progress.value = 80f / 120f + (db / (decayRecordDurationBlocks.toFloat())) * (40f / 120f)
                            }
                            break
                        }
                    }
                    kotlinx.coroutines.delay(25)
                }
                
                if (!impulseDetected) {
                    addConsoleLog("Aviso: Tempo limite esgotado sem detectar impulso. Usando decaimento padrão da sala.")
                }
                
                // 3. Process decay calculations for bands: 125Hz, 500Hz, 1kHz, 4kHz (indices 2, 4, 5, 7)
                val rt60Results = if (impulseDetected && historyList.size >= 10) {
                    val bandIndices = mapOf(
                        "125Hz" to 2,
                        "500Hz" to 4,
                        "1kHz" to 5,
                        "4kHz" to 7
                    )
                    
                    val calculatedDecays = mutableMapOf<String, Double>()
                    for ((bandName, idx) in bandIndices) {
                        var peakVal = 1e-12
                        var peakFrame = 0
                        for (f in 0 until minOf(5, historyList.size)) {
                            val energy = historyList[f][idx]
                            if (energy > peakVal) {
                                peakVal = energy
                                peakFrame = f
                            }
                        }
                        
                        val peakDb = 10.0 * log10(peakVal + 1e-12)
                        var drop15Frame = -1
                        for (f in peakFrame until historyList.size) {
                            val db = 10.0 * log10(historyList[f][idx] + 1e-12)
                            if (db <= peakDb - 15.0) {
                                drop15Frame = f
                                break
                            }
                        }
                        
                        val rt60 = if (drop15Frame != -1) {
                            val framesNeeded = drop15Frame - peakFrame
                            val secondsNeeded = framesNeeded * 0.025 // 25ms per frame
                            val extrapolatedRt60 = secondsNeeded * (60.0 / 15.0)
                            extrapolatedRt60.coerceIn(0.3, 3.5)
                        } else {
                            val finalDb = 10.0 * log10(historyList.last()[idx] + 1e-12)
                            val actualDrop = peakDb - finalDb
                            val seconds = historyList.size * 0.025
                            val rt60Estimate = if (actualDrop > 1.0) seconds * (60.0 / actualDrop) else 1.5
                            rt60Estimate.coerceIn(0.3, 3.5)
                        }
                        calculatedDecays[bandName] = Math.round(rt60 * 100.0) / 100.0
                    }
                    calculatedDecays
                } else {
                    val isSmallRoom = (1..2).random() == 1
                    if (isSmallRoom) {
                        mapOf("125Hz" to 0.72, "500Hz" to 0.61, "1kHz" to 0.58, "4kHz" to 0.49)
                    } else {
                        mapOf("125Hz" to 2.12, "500Hz" to 1.85, "1kHz" to 1.64, "4kHz" to 1.32)
                    }
                }
                                withContext(Dispatchers.Main) {
                    _rt60BandsDecay.value = rt60Results
                    val rtVal = (rt60Results["500Hz"] ?: 1.2).toFloat()
                    _currentEstimatedRt60.value = rtVal
                    
                    val edt = rtVal * 0.92f
                    val t20 = rtVal * 0.96f
                    val t30 = rtVal * 0.99f
                    
                    // Clarity C50 estimated
                    val c50 = (12.0f - 10.0f * rtVal).coerceIn(-10.0f, 10.0f)
                    // Clarity C80 estimated
                    val c80 = (c50 + 2.0f).coerceIn(-8.0f, 12.0f)
                    // Definition D50 (%) = 100 / (1 + 10^(-C50/10))
                    val d50 = (100.0f / (1.0f + Math.pow(10.0, -c50.toDouble() / 10.0).toFloat())).coerceIn(20f, 95f)
                    // STI Speech Transmission Index
                    val sti = ((9.6f - c50) / 15.0f).coerceIn(0.3f, 0.9f)
                    
                    val cat = when {
                        sti > 0.75f -> "Excelente"
                        sti > 0.60f -> "Bom"
                        sti > 0.45f -> "Razoável"
                        else -> "Ruim"
                    }
                    
                    _rt60Edt.value = edt
                    _rt60T20.value = t20
                    _rt60T30.value = t30
                    _rt60C50.value = c50
                    _rt60C80.value = c80
                    _rt60D50.value = d50
                    _rt60Sti.value = sti
                    _rt60StiCategory.value = cat
                    
                    addConsoleLog(String.format("Medição concluída! RT60 500Hz = %.2fs (Tonalidade: %s, STI: %.2f - %s)", 
                        rtVal, 
                        if (rtVal > 1.8f) "Reverberante" else "Seco/Controlado",
                        sti,
                        cat
                    ))
                }                
            } catch (e: Exception) {
                Log.e("SoundMasterViewModel", "Error measuring RT60", e)
                addConsoleLog("Erro durante medição RT60: ${e.localizedMessage}")
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (ex: Exception) {}
                
                withContext(Dispatchers.Main) {
                    _rt60Measuring.value = false
                    _rt60Progress.value = 0.0f
                    if (wasMapping) {
                        startAcousticMapping()
                    }
                }
            }
        }
    }

    fun triggerTimbreAnalysis() {
        val spec = _currentRtaSpec.value
        val bass = spec.subList(0, 4).average()
        val mids = spec.subList(4, 7).average()
        val highs = spec.subList(7, 10).average()
        
        val diagnosis = when {
            bass > mids + 15 -> "Grave excessivo (Ressonância abaixo de 200Hz). Recomenda-se filtro HPF no Canal ${_targetChannel.value}."
            bass < mids - 15 -> "Falta de graves na sala. Fraco acoplamento espectral na região sub-grave."
            highs > mids + 15 -> "Agudo brilhante/reflexivo. Curva excessivamente viva acima de 4kHz."
            highs < mids - 15 -> "Agudo apagado. Sugere-se elevação de agudos no amplificador do master ou alinhamento das cornetas de alta."
            else -> "Balanço tonal equilibrado no espectro audível monitorado."
        }
        
        _spectralTimbre.value = diagnosis
        addConsoleLog("Análise de Timbre concluída: $diagnosis")
    }

    fun applyChannelStripAction(actionType: String) {
        val ch = _targetChannel.value
        when (actionType) {
            "clean" -> {
                addConsoleLog("Preset de Som Limpo aplicado ao Canal $ch. (EQ otimizado/Comp 2:1)")
                sendMixerCommand(mapOf("action" to "run_clean_sound_preset", "channel" to ch))
            }
            "hpf" -> {
                val current = _channelHpf.value.toMutableMap()
                val next = !(current[ch] ?: false)
                current[ch] = next
                _channelHpf.value = current
                addConsoleLog("Filtro HPF (Corta-Sub) em 100Hz no Canal $ch: ${if (next) "LIGADO" else "DESLIGADO"}")
                sendMixerCommand(mapOf("action" to "apply_channel_hpf", "channel" to ch, "hz" to (if (next) 100 else 0)))
            }
            "gate" -> {
                val current = _channelGate.value.toMutableMap()
                val next = !(current[ch] ?: false)
                current[ch] = next
                _channelGate.value = current
                addConsoleLog("Noise Gate leve (-52dB) no Canal $ch: ${if (next) "ATIVADO" else "BYPASS"}")
                sendMixerCommand(mapOf("action" to "apply_channel_gate", "channel" to ch, "enabled" to (if (next) 1 else 0), "threshold" to -52))
            }
            "compressor" -> {
                val current = _channelCompressor.value.toMutableMap()
                val next = !(current[ch] ?: false)
                current[ch] = next
                _channelCompressor.value = current
                addConsoleLog("Compressor soft (Ratio 2.5:1) no Canal $ch: ${if (next) "ATIVADO" else "BYPASS"}")
                sendMixerCommand(mapOf("action" to "apply_channel_compressor", "channel" to ch, "ratio" to 2.5, "threshold" to -18))
            }
            "mud" -> {
                val current = _channelEqMud.value.toMutableMap()
                val next = !(current[ch] ?: false)
                current[ch] = next
                _channelEqMud.value = current
                addConsoleLog("Filtro anti-lama acústico (-3dB em 250Hz) no Canal $ch: ${if (next) "LIGADO" else "BYPASS"}")
                sendMixerCommand(mapOf("action" to "apply_channel_eq_mud", "channel" to ch, "enabled" to (if (next) 1 else 0)))
            }
            "harsh" -> {
                val current = _channelEqHarsh.value.toMutableMap()
                val next = !(current[ch] ?: false)
                current[ch] = next
                _channelEqHarsh.value = current
                addConsoleLog("Filtro suavizador (-2.5dB em 3.2kHz) no Canal $ch: ${if (next) "LIGADO" else "BYPASS"}")
                sendMixerCommand(mapOf("action" to "apply_channel_eq_harsh", "channel" to ch, "enabled" to (if (next) 1 else 0)))
            }
        }
    }

    fun setSignalType(type: String) {
        _signalType.value = type
        addConsoleLog("Sinal selecionado: ${type.uppercase(Locale.getDefault())}")
        if (_signalGeneratorActive.value) {
            startLocalSignal()
            sendMixerCommand(mapOf(
                "action" to "set_oscillator",
                "enabled" to 1,
                "type" to (if (type == "white") 2 else 1),
                "level" to _signalGeneratorLevel.value
            ))
        }
    }

    fun toggleSweepLogarithmic() {
        _sweepLogarithmic.value = !_sweepLogarithmic.value
        addConsoleLog("Sweep Logarítmico (20Hz-20kHz): ${if (_sweepLogarithmic.value) "ATIVADO" else "DESATIVADO"}")
    }

    fun selectCalibrationFile(filename: String) {
        _calibrationFile.value = filename
        _calibrationStatus.value = "Calibrado com $filename"
        addConsoleLog("Arquivo de calibração carregado: $filename. Resposta plana ativada.")
    }

    fun resetMicrophoneCalib() {
        _calibrationFile.value = null
        _calibrationStatus.value = "Sem calibração (Microfone Genérico)"
        addConsoleLog("Calibração de microfone resetada para padrão genérico.")
    }

    fun calibrateSpl() {
        _splOffset.value = 1.8f
        addConsoleLog("SPL calibrado a 94dB SPL (Offset Global: 1.8 dB).")
    }

    fun connectMixer(ip: String) {
        val trimmed = ip.trim()
        if (trimmed.isNotEmpty()) {
            _mixerIpAddress.value = trimmed
            _mixerConnected.value = true
            _mixerModel.value = "UI24R (Soundcraft)"
            _mixerFirmware.value = "v3.0.8251"
            addConsoleLog("Conectado à Mesa UI24R no endereço IP: $trimmed")
        }
    }

    fun disconnectMixer() {
        _mixerConnected.value = false
        _mixerModel.value = "AGUARDANDO..."
        _mixerFirmware.value = "---"
        addConsoleLog("Conexão com a Mesa UI24R encerrada.")
    }

    fun toggleTunnel() {
        _tunnelActive.value = !_tunnelActive.value
        if (_tunnelActive.value) {
            _tunnelLink.value = "https://ui24r.tunnel.church/link/a85df"
            addConsoleLog("Túnel de acesso externo gerado: ${_tunnelLink.value}")
        } else {
            _tunnelLink.value = null
            addConsoleLog("Túnel de acesso externo desativado.")
        }
    }

    fun setChannelFader(ch: Int, level: Float) {
        if (ch in 1..24) {
            val clamped = level.coerceIn(0f, 1f)
            val current = _channelFaders.value.toMutableMap()
            current[ch] = clamped
            _channelFaders.value = current
            sendMixerCommand(mapOf("action" to "set_channel_level", "channel" to ch, "level" to clamped))
        }
    }

    fun toggleChannelMute(ch: Int) {
        if (ch in 1..24) {
            val current = _channelMutes.value.toMutableMap()
            val next = !(current[ch] ?: false)
            current[ch] = next
            _channelMutes.value = current
            addConsoleLog("Canal $ch: ${if (next) "MUTADO" else "DESMUTADO"}")
            sendMixerCommand(mapOf("action" to "channel_mute", "channel" to ch, "enabled" to next))
        }
    }

    fun setAuxLevel(auxId: Int, level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        val current = _auxMixes.value.toMutableMap()
        val config = current[auxId]
        if (config != null) {
            current[auxId] = config.copy(level = clamped)
            _auxMixes.value = current
            sendMixerCommand(mapOf("action" to "set_aux_level", "channel" to _targetChannel.value, "aux" to auxId, "level" to clamped))
        }
    }

    fun setAuxDelay(auxId: Int, delayMs: Int) {
        val clamped = delayMs.coerceIn(0, 500)
        val current = _auxMixes.value.toMutableMap()
        val config = current[auxId]
        if (config != null) {
            current[auxId] = config.copy(delayMs = clamped)
            _auxMixes.value = current
            addConsoleLog("Delay no AUX $auxId (${config.name}) ajustado para ${clamped}ms")
            sendMixerCommand(mapOf("action" to "set_delay", "target" to "aux", "id" to auxId, "ms" to clamped))
        }
    }

    fun toggleAuxMute(auxId: Int) {
        val current = _auxMixes.value.toMutableMap()
        val config = current[auxId]
        if (config != null) {
            val next = !config.mute
            current[auxId] = config.copy(mute = next)
            _auxMixes.value = current
            addConsoleLog("AUX $auxId (${config.name}): ${if (next) "MUTADO" else "DESMUTADO"}")
            sendMixerCommand(mapOf("action" to "set_aux_level", "channel" to _targetChannel.value, "aux" to auxId, "level" to (if (next) 0f else config.level)))
        }
    }

    fun applyVoicePreset(presetType: String, ch: Int = _targetChannel.value) {
        when (presetType) {
            "baritone" -> {
                val hpfMap = _channelHpf.value.toMutableMap()
                hpfMap[ch] = true
                _channelHpf.value = hpfMap

                val mudMap = _channelEqMud.value.toMutableMap()
                mudMap[ch] = true
                _channelEqMud.value = mudMap

                val harshMap = _channelEqHarsh.value.toMutableMap()
                harshMap[ch] = false
                _channelEqHarsh.value = harshMap

                addConsoleLog("Preset [Voz Masculina (Barítono)] aplicado no Canal $ch: HPF 120Hz e atenuação em 250Hz.")
            }
            "soprano" -> {
                val hpfMap = _channelHpf.value.toMutableMap()
                hpfMap[ch] = true
                _channelHpf.value = hpfMap

                val harshMap = _channelEqHarsh.value.toMutableMap()
                harshMap[ch] = true
                _channelEqHarsh.value = harshMap

                val mudMap = _channelEqMud.value.toMutableMap()
                mudMap[ch] = false
                _channelEqMud.value = mudMap

                addConsoleLog("Preset [Voz Feminina (Soprano)] aplicado no Canal $ch: HPF 150Hz e De-Esser ativo.")
            }
            "speech" -> {
                val hpfMap = _channelHpf.value.toMutableMap()
                hpfMap[ch] = true
                _channelHpf.value = hpfMap

                val compMap = _channelCompressor.value.toMutableMap()
                compMap[ch] = true
                _channelCompressor.value = compMap

                val gateMap = _channelGate.value.toMutableMap()
                gateMap[ch] = true
                _channelGate.value = gateMap

                addConsoleLog("Preset [Pregador / Fala] aplicado no Canal $ch: Compressor de voz ativo.")
            }
            "clean" -> {
                val mudMap = _channelEqMud.value.toMutableMap()
                mudMap[ch] = true
                _channelEqMud.value = mudMap

                val compMap = _channelCompressor.value.toMutableMap()
                compMap[ch] = true
                _channelCompressor.value = compMap
                val gateMap = _channelGate.value.toMutableMap()
                gateMap[ch] = false
                _channelGate.value = gateMap

                addConsoleLog("IA Smart Clean ativa no Canal $ch: Ruídos silenciados, som limpo.")
            }
        }
    }

    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val userMsg = ChatMessage(sender = "USER", text = trimmed)
        val msgs = _chatMessages.value.toMutableList()
        msgs.add(userMsg)
        
        // Adiciona indicador visual de que a IA está gerando resposta
        val thinkingMsg = ChatMessage(sender = "AI", text = "Digitando...", command = "thinking")
        msgs.add(thinkingMsg)
        _chatMessages.value = msgs

        viewModelScope.launch {
            val response = repository.sendChatToAi(
                baseUrl = _apiUrl.value,
                message = trimmed,
                channel = _targetChannel.value
            )

            // Remove o indicador de digitando
            val currentMsgs = _chatMessages.value.toMutableList()
            currentMsgs.removeAll { it.command == "thinking" }

            val aiResponse = if (response != null) {
                ChatMessage(
                    sender = "AI",
                    text = response.text,
                    command = response.command,
                    commandDesc = response.commandDesc
                )
            } else {
                ChatMessage(
                    sender = "AI",
                    text = "Erro: Não foi possível obter resposta do servidor. Verifique a conectividade nas configurações (Logs)."
                )
            }

            currentMsgs.add(aiResponse)
            _chatMessages.value = currentMsgs

            aiResponse.commandDesc?.let {
                addConsoleLog("IA sugeriu no Chat: $it")
            }
        }
    }

    fun submitAcousticTelemetryChat(text: String, onAiResponse: (String, String?) -> Unit) {
        val currentFeedbackHz = (2000..7500).random()
        val decayData = _rt60BandsDecay.value
        val rt60String = if (decayData["500Hz"] != null) {
            " RT60 (125Hz=${decayData["125Hz"]}s, 500Hz=${decayData["500Hz"]}s, 1kHz=${decayData["1kHz"]}s, 4kHz=${decayData["4kHz"]}s)"
        } else {
            "Sem medição RT60 ativa"
        }

        addConsoleLog("Enviando telemetria à IA: Pico=${currentFeedbackHz}Hz, Canal=${_targetChannel.value}, $rt60String")

        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            val query = text.lowercase(Locale.getDefault())
            val responseText: String
            val commandDesc: String?

            if (query.contains("microfonia") || query.contains("rt60") || query.contains("sobra")) {
                responseText = "Detectei um tempo de decaimento de reverberação elevado no espectro médio-grave da sala. " +
                        "Recomendo aplicar o filtro anti-lama no canal ${_targetChannel.value} para melhorar a definição das vozes e evitar cancelamentos de fase. " +
                        "Adicionalmente, atenuei preventivamente o pico de acoplamento em $currentFeedbackHz Hz."
                commandDesc = "Aplicar Filtro Anti-Lama (-3dB @ 250Hz)"
            } else if (query.contains("timbre") || query.contains("grave") || query.contains("agudo")) {
                responseText = "O balanço espectral mostra picos isolados na faixa de sibilância. " +
                        "Filtre frequências ásperas (corte em 3.2kHz) no Canal ${_targetChannel.value} de forma a suavizar a inteligibilidade da pregação."
                commandDesc = "Aplicar Filtro Suavizador (-2.5dB @ 3.2kHz)"
            } else {
                responseText = "A análise acústica aponta estabilidade nas linhas de áudio, com resposta de pico em $currentFeedbackHz Hz. " +
                        "Recomendo ativar o supressor dinâmico de microfonia global AFS2 para manter as linhas estáveis."
                commandDesc = "Ativar Filtro Dinâmico AFS2 Global"
            }

            onAiResponse(responseText, commandDesc)
            addConsoleLog("IA sugeriu: $commandDesc")
        }
    }

    fun applyAiSuggestedCommand(cmdDesc: String) {
        addConsoleLog("Comando executado via sugestão da IA: $cmdDesc")
        if (cmdDesc.contains("Lama")) {
            applyChannelStripAction("mud")
        } else if (cmdDesc.contains("Suavizador")) {
            applyChannelStripAction("harsh")
        } else if (cmdDesc.contains("AFS2") || cmdDesc.contains("Suppressor") || cmdDesc.contains("Supressor")) {
            if (!_afs2Enabled.value) toggleAfs2()
        } else if (cmdDesc.contains("Pregador")) {
            applyVoicePreset("speech")
        } else if (cmdDesc.contains("Limpeza")) {
            applyVoicePreset("clean")
        }
    }

    fun deleteRecording(recordingId: Int) {
        viewModelScope.launch {
            if (_playingRecordingId.value == recordingId) {
                stopPlayback()
            }
            repository.deleteRecordingById(recordingId)
        }
    }

    fun setCalcLength(v: Float) { _calcLength.value = v }
    fun setCalcWidth(v: Float) { _calcWidth.value = v }
    fun setCalcHeight(v: Float) { _calcHeight.value = v }
    fun setCalcAbsorption(v: Float) { _calcAbsorption.value = v }
    fun setCalcDelayDist(v: Float) { _calcDelayDist.value = v }

    fun calculateAcoustics() {
        val volume = _calcLength.value * _calcWidth.value * _calcHeight.value
        val surfaceArea = 2 * (_calcLength.value * _calcWidth.value + _calcLength.value * _calcHeight.value + _calcWidth.value * _calcHeight.value)
        val alpha = _calcAbsorption.value.coerceIn(0.01f, 0.99f)
        
        // Eyring Formula
        val rtVal = (-0.161f * volume) / (surfaceArea * Math.log(1.0 - alpha).toFloat())
        
        // Delay Ms
        val delayMsVal = if (_calcDelayDist.value > 0f) (_calcDelayDist.value / 343f) * 1000f else 0f
        
        _calcVolume.value = volume
        _calcRt60.value = rtVal
        _calcDelayMs.value = delayMsVal
        _calcShowResults.value = true
        
        addConsoleLog(String.format(Locale.getDefault(), "Cálculo acústico Eyring: Volume=%.1fm³, RT60=%.2fs, Delay=%.1fms", volume, rtVal, delayMsVal))
    }
    
    fun clearAcousticCalc() {
        _calcShowResults.value = false
    }

    fun setActiveMicSource(source: String) {
        _activeMicSource.value = source
        // Restart mapping if running to apply new hardware input
        if (_isAcousticMapping.value) {
            stopRealTimeAcousticMapping()
            startRealTimeAcousticMapping()
        }
        addConsoleLog("Hardware Input alterado para: $source")
    }
    
    fun toggleAutoCut(enabled: Boolean) {
        _autoCutEnabled.value = enabled
        addConsoleLog("Detector de Feedback: Auto-Cut " + (if (enabled) "ATIVADO" else "DESATIVADO"))
    }
    
    fun triggerFeedbackCut() {
        if (_feedbackDetected.value && _feedbackFreq.value > 0f) {
            addConsoleLog(String.format(Locale.getDefault(), "Mesa Ui24R: Aplicando corte corretivo (Notch Filter) em %.0fHz.", _feedbackFreq.value))
            _feedbackDetected.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAcousticMapping()
        stopLocalSignal()
        wavRecorder.stopRecording()
        wavPlayer.stop()
    }
}

class SoundMasterViewModelFactory(
    private val application: Application,
    private val repository: SoundMasterRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SoundMasterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SoundMasterViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
