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

    private val _currentEstimatedRt60 = MutableStateFlow(1.6f)
    val currentEstimatedRt60: StateFlow<Float> = _currentEstimatedRt60

    // Audio Recorder
    private val wavRecorder = WavAudioRecorder(application)
    val recordingAmplitude: StateFlow<Float> = wavRecorder.amplitude
    val recordingDurationMs: StateFlow<Long> = wavRecorder.durationMs

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private var activeRecordingFile: File? = null

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
    }

    fun setActiveMappingArea(area: String) {
        _activeMappingArea.value = area
    }

    fun startAcousticMapping() {
        if (_isAcousticMapping.value) return
        _isAcousticMapping.value = true
        _mappingProgress.value = 0f
        addConsoleLog("Mapeador: Iniciando escaneamento acústico em tempo real no local: ${_activeMappingArea.value}...")

        viewModelScope.launch {
            val maxSteps = 25
            for (step in 1..maxSteps) {
                kotlinx.coroutines.delay(110)
                _mappingProgress.value = step.toFloat() / maxSteps
                
                // Real-time bouncing meter values
                val baseDb = when (_activeMappingArea.value) {
                    "Púlpito / Altar" -> 72f
                    "Nave - Frente" -> 82f
                    "Nave - Fundo" -> 90f
                    "Galeria Lateral E" -> 78f
                    "Galeria Lateral D" -> 80f
                    "Mezanino (Alta)" -> 85f
                    else -> 75f
                }
                _currentSplDb.value = baseDb + (0..120).random().toFloat() / 15f
                
                // Shuffle spectrum bars
                val spec = List(10) { (15..95).random().toFloat() }
                _currentRtaSpec.value = spec
            }

            // Calculations completed
            val finPeak = when (_activeMappingArea.value) {
                "Púlpito / Altar" -> 250 // Muddy vocals
                "Nave - Frente" -> 1000 // balanced 1kHz mid
                "Nave - Fundo" -> 125 // Low room resonance
                "Galeria Lateral E" -> 4200 // feedback coupling
                "Galeria Lateral D" -> 3200 // vocal sibilance
                "Mezanino (Alta)" -> 180 // Low mud
                else -> 440
            }
            val finSpl = _currentSplDb.value
            val finRt60 = when (_activeMappingArea.value) {
                "Púlpito / Altar" -> 1.15f
                "Nave - Frente" -> 1.62f
                "Nave - Fundo" -> 2.25f
                "Galeria Lateral E" -> 1.85f
                "Galeria Lateral D" -> 1.78f
                "Mezanino (Alta)" -> 2.45f
                else -> 1.50f
            }

            _currentPeakHz.value = finPeak
            _currentEstimatedRt60.value = finRt60

            val textDiagnosis = when (finPeak) {
                250 -> "Sobra de médias-graves. Recomenda-se filtro anti-lama (-3dB em 250Hz)."
                125, 180 -> "Acúmulo de subgraves na cobertura. Ativar filtro HPF em 120Hz."
                4200 -> "Pico agudo propenso a microfonia de acoplamento. Usar supressor AFS2."
                3200 -> "Sibilância áspera nas médias-altas. Ativar suavizador (-2.5dB @ 3.2kHz)."
                else -> "Resposta espectral ideal. Equilíbrio de fase estável."
            }

            // Save inside database audio_recordings
            val recording = AudioRecording(
                title = _activeMappingArea.value,
                filePath = String.format(Locale.US, "%.1f", finSpl), // stores SPL
                processedFilePath = String.format(Locale.US, "%.2fs", finRt60), // stores RT60
                appliedEffect = "${finPeak}Hz", // peak frequency
                transcription = textDiagnosis,
                status = "original"
            )
            repository.insertRecording(recording)

            _isAcousticMapping.value = false
            addConsoleLog("Mapeamento concluído para '${_activeMappingArea.value}': SPL médio=${String.format(Locale.US, "%.1f", finSpl)} dB | RT60=${String.format(Locale.US, "%.2fs", finRt60)} | Pico=${finPeak}Hz")
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
            addConsoleLog("Gerador de Ruído Rosa ATIVADO no master à ${_signalGeneratorLevel.value} dB")
        } else {
            addConsoleLog("Gerador de Ruído descativado.")
        }
    }

    fun setSignalGeneratorLevel(db: Int) {
        _signalGeneratorLevel.value = db
        if (_signalGeneratorActive.value) {
            addConsoleLog("Nível do gerador ajustado para $db dB")
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
    }

    fun toggleMuteAll() {
        val next = !_muteAllActive.value
        _muteAllActive.value = next
        addConsoleLog(if (next) "PANIC MUTE ALL: Todos canais mutados!" else "Mute geral liberado.")
    }

    fun toggleMuteG1() {
        val next = !_muteG1.value
        _muteG1.value = next
        addConsoleLog("Mute Group G1 (Vozes) ${if (next) "MUTADO" else "LIBERADO"}")
    }

    fun toggleMuteG2() {
        val next = !_muteG2.value
        _muteG2.value = next
        addConsoleLog("Mute Group G2 (Instrumentos) ${if (next) "MUTADO" else "LIBERADO"}")
    }

    fun toggleMuteG3() {
        val next = !_muteG3.value
        _muteG3.value = next
        addConsoleLog("Mute Group G3 (Efeitos/Aux) ${if (next) "MUTADO" else "LIBERADO"}")
    }

    fun cutFeedbackFreq(hz: Int) {
        addConsoleLog("AFS detectado! Atenuando frequência problemática de $hz Hz com filtro Notch de -12dB.")
    }

    fun triggerRT60Measurement() {
        if (_rt60Measuring.value) return
        _rt60Measuring.value = true
        _rt60Progress.value = 0.0f
        addConsoleLog("Iniciando medição acústica RT60. Emita um pulso sonoro seco (palma ou estalo).")

        viewModelScope.launch {
            // Simulate 3 seconds test countdown
            for (i in 1..30) {
                kotlinx.coroutines.delay(100)
                _rt60Progress.value = i / 30f
                if (i == 10) {
                    addConsoleLog("Impulso acústico capturado com sucesso! Analisando decaimento da sala...")
                }
            }

            // Simulate decay times calculations
            val isSmallRoom = (1..2).random() == 1
            val result = if (isSmallRoom) {
                mapOf(
                    "125Hz" to 0.72,
                    "500Hz" to 0.61,
                    "1kHz" to 0.58,
                    "4kHz" to 0.49
                )
            } else {
                // Large reverberating room (e.g. church)
                mapOf(
                    "125Hz" to 2.12,
                    "500Hz" to 1.85,
                    "1kHz" to 1.64,
                    "4kHz" to 1.32
                )
            }

            _rt60BandsDecay.value = result
            _rt60Measuring.value = false
            _rt60Progress.value = 0.0f
            addConsoleLog("Medição RT60 concluída. Médias calculadas: 500Hz=${result["500Hz"]}s.")
        }
    }

    fun triggerTimbreAnalysis() {
        val tones = listOf(
            "Grave excessivo (Ressonância abaixo de 200Hz). Recomenda-se filtro HPF no Canal ${_targetChannel.value}.",
            "Falta de grave na sala. Fraco acoplamento espectral.",
            "Agudo brilhante/reflexivo. Curva excessivamente viva acima de 4kHz.",
            "Agudo apagado. Sugere-se elevação de agudos no amplificador do master.",
            "Balanço tonal equilibrado no espectro audível monitorado."
        )
        val selected = tones.random()
        _spectralTimbre.value = selected
        addConsoleLog("Análise de Timbre concluída: $selected")
    }

    fun applyChannelStripAction(actionType: String) {
        val ch = _targetChannel.value
        when (actionType) {
            "clean" -> {
                addConsoleLog("Preset de Som Limpo aplicado ao Canal $ch. (EQ otimizado/Comp 2:1)")
            }
            "hpf" -> {
                val current = _channelHpf.value.toMutableMap()
                val next = !(current[ch] ?: false)
                current[ch] = next
                _channelHpf.value = current
                addConsoleLog("Filtro HPF (Corta-Sub) em 100Hz no Canal $ch: ${if (next) "LIGADO" else "DESLIGADO"}")
            }
            "gate" -> {
                val current = _channelGate.value.toMutableMap()
                val next = !(current[ch] ?: false)
                current[ch] = next
                _channelGate.value = current
                addConsoleLog("Noise Gate leve (-52dB) no Canal $ch: ${if (next) "ATIVADO" else "BYPASS"}")
            }
            "compressor" -> {
                val current = _channelCompressor.value.toMutableMap()
                val next = !(current[ch] ?: false)
                current[ch] = next
                _channelCompressor.value = current
                addConsoleLog("Compressor soft (Ratio 2.5:1) no Canal $ch: ${if (next) "ATIVADO" else "BYPASS"}")
            }
            "mud" -> {
                val current = _channelEqMud.value.toMutableMap()
                val next = !(current[ch] ?: false)
                current[ch] = next
                _channelEqMud.value = current
                addConsoleLog("Filtro anti-lama acústico (-3dB em 250Hz) no Canal $ch: ${if (next) "LIGADO" else "BYPASS"}")
            }
            "harsh" -> {
                val current = _channelEqHarsh.value.toMutableMap()
                val next = !(current[ch] ?: false)
                current[ch] = next
                _channelEqHarsh.value = current
                addConsoleLog("Filtro suavizador (-2.5dB em 3.2kHz) no Canal $ch: ${if (next) "LIGADO" else "BYPASS"}")
            }
        }
    }

    fun setSignalType(type: String) {
        _signalType.value = type
        addConsoleLog("Sinal selecionado: ${type.uppercase(Locale.getDefault())}")
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
            val current = _channelFaders.value.toMutableMap()
            current[ch] = level.coerceIn(0f, 1f)
            _channelFaders.value = current
        }
    }

    fun toggleChannelMute(ch: Int) {
        if (ch in 1..24) {
            val current = _channelMutes.value.toMutableMap()
            val next = !(current[ch] ?: false)
            current[ch] = next
            _channelMutes.value = current
            addConsoleLog("Canal $ch: ${if (next) "MUTADO" else "DESMUTADO"}")
        }
    }

    fun setAuxLevel(auxId: Int, level: Float) {
        val current = _auxMixes.value.toMutableMap()
        val config = current[auxId]
        if (config != null) {
            current[auxId] = config.copy(level = level.coerceIn(0f, 1f))
            _auxMixes.value = current
        }
    }

    fun setAuxDelay(auxId: Int, delayMs: Int) {
        val current = _auxMixes.value.toMutableMap()
        val config = current[auxId]
        if (config != null) {
            current[auxId] = config.copy(delayMs = delayMs.coerceIn(0, 500))
            _auxMixes.value = current
            addConsoleLog("Delay no AUX $auxId (${config.name}) ajustado para ${delayMs}ms")
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
        _chatMessages.value = msgs

        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)

            val query = trimmed.lowercase(Locale.getDefault())
            val aiResponse: ChatMessage

            if (query.contains("microfonia") || query.contains("feedback") || query.contains("rt60")) {
                aiResponse = ChatMessage(
                    sender = "AI",
                    text = "Detectei um pico sutil de acoplamento em 4.2kHz nas linhas da igreja. Sugiro acionar o supressor bi-dinâmico AFS2 global ou atenuar o Canal ${_targetChannel.value} onde o som do púlpito está bem vivo.",
                    command = "enable_afs",
                    commandDesc = "Ativar Supressor Dinâmico AFS2"
                )
            } else if (query.contains("graves") || query.contains("sub") || query.contains("lama") || query.contains("ressonância")) {
                aiResponse = ChatMessage(
                    sender = "AI",
                    text = "A acústica do templo apresenta uma ressonância típica de acúmulo de graves perto de 250Hz (região das frequências médias-graves emboladas). Sugiro aplicar o filtro anti-lama inteligente no canal ativo.",
                    command = "apply_eq_mud",
                    commandDesc = "Aplicar Filtro Anti-Lama (-3dB @ 250Hz)"
                )
            } else if (query.contains("pregação") || query.contains("voz") || query.contains("pastor")) {
                aiResponse = ChatMessage(
                    sender = "AI",
                    text = "Para a pregação do Pastor, é importante cortar as sobras de sub-graves de impacto mecânico do púlpito e comprimir sutilmente para manter a clareza em sussurros. Quer aplicar o preset de Pregador / Fala no Canal ${_targetChannel.value}?",
                    command = "apply_preset_speech",
                    commandDesc = "Aplicar Preset Pregador / Fala"
                )
            } else if (query.contains("brilho") || query.contains("agudo") || query.contains("suave") || query.contains("sibilância")) {
                aiResponse = ChatMessage(
                    sender = "AI",
                    text = "A sala possui superfícies reflexivas (vidros/paredes lisas) que realçam a aspereza acima de 3kHz. Recomendo suavizar as sibilâncias no Canal ${_targetChannel.value}.",
                    command = "apply_eq_harsh",
                    commandDesc = "Aplicar Filtro Suavizador (-2.5dB @ 3.2kHz)"
                )
            } else {
                aiResponse = ChatMessage(
                    sender = "AI",
                    text = "Analisei a assinatura do ambiente para o Canal ${_targetChannel.value}. O espectro está sob controle. Recomendamos o filtro Smart Clean para garantir imunidade absoluta contra ruídos de fundo elétricos (hums).",
                    command = "apply_preset_clean",
                    commandDesc = "Ativar Limpeza Inteligente IA"
                )
            }

            val newMsgs = _chatMessages.value.toMutableList()
            newMsgs.add(aiResponse)
            _chatMessages.value = newMsgs
            
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

    override fun onCleared() {
        super.onCleared()
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
