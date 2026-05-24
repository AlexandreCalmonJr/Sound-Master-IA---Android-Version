package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundMasterUi(viewModel: SoundMasterViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val apiStatus by viewModel.apiStatus.collectAsStateWithLifecycle()
    val processingStatus by viewModel.processingStatus.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Logo",
                            tint = NeonPurple,
                            modifier = Modifier.size(28.dp).testTag("app_logo")
                        )
                        Column {
                            Text(
                                text = "DIAGNÓSTICO ACÚSTICO",
                                color = GlowCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "ChurchSound IA",
                                color = BrightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (apiStatus) {
                                    "ONLINE" -> NeonMint.copy(alpha = 0.15f)
                                    "OFFLINE" -> GlowingError.copy(alpha = 0.15f)
                                    else -> GlowCyan.copy(alpha = 0.15f)
                                }
                            )
                            .clickable { viewModel.checkApiConnection() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("api_status_connector")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (apiStatus) {
                                            "ONLINE" -> NeonMint
                                            "OFFLINE" -> GlowingError
                                            else -> GlowCyan
                                        }
                                    )
                            )
                            Text(
                                text = when (apiStatus) {
                                    "ONLINE" -> "Mesa Online"
                                    "OFFLINE" -> "Mesa Offline"
                                    else -> "Autodetect"
                                },
                                color = when (apiStatus) {
                                    "ONLINE" -> NeonMint
                                    "OFFLINE" -> GlowingError
                                    else -> GlowCyan
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpaceBlack,
                    titleContentColor = BrightWhite
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SpaceBlack,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.HOME,
                    onClick = { viewModel.setScreen(AppScreen.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                    label = { Text("Início") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ContentBlue,
                        selectedTextColor = GlowCyan,
                        indicatorColor = ContainerBlue,
                        unselectedIconColor = SoftGrey.copy(alpha = 0.6f),
                        unselectedTextColor = SoftGrey.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_home_tab")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.MEASURE,
                    onClick = { viewModel.setScreen(AppScreen.MEASURE) },
                    icon = { Icon(Icons.Default.CompassCalibration, contentDescription = "Medição") },
                    label = { Text("Medição") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ContentBlue,
                        selectedTextColor = GlowCyan,
                        indicatorColor = ContainerBlue,
                        unselectedIconColor = SoftGrey.copy(alpha = 0.6f),
                        unselectedTextColor = SoftGrey.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_measure_tab")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.CONSOLE,
                    onClick = { viewModel.setScreen(AppScreen.CONSOLE) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Mixer") },
                    label = { Text("Mixer") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ContentBlue,
                        selectedTextColor = GlowCyan,
                        indicatorColor = ContainerBlue,
                        unselectedIconColor = SoftGrey.copy(alpha = 0.6f),
                        unselectedTextColor = SoftGrey.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_console_tab")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.CHAT_AI,
                    onClick = { viewModel.setScreen(AppScreen.CHAT_AI) },
                    icon = { Icon(Icons.Default.Forum, contentDescription = "Chat IA") },
                    label = { Text("Chat IA") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ContentBlue,
                        selectedTextColor = GlowCyan,
                        indicatorColor = ContainerBlue,
                        unselectedIconColor = SoftGrey.copy(alpha = 0.6f),
                        unselectedTextColor = SoftGrey.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_library_tab")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.SETTINGS,
                    onClick = { viewModel.setScreen(AppScreen.SETTINGS) },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Logs") },
                    label = { Text("Logs") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ContentBlue,
                        selectedTextColor = GlowCyan,
                        indicatorColor = ContainerBlue,
                        unselectedIconColor = SoftGrey.copy(alpha = 0.6f),
                        unselectedTextColor = SoftGrey.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("nav_settings_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBlack)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    AppScreen.HOME -> HomeScreen(viewModel)
                    AppScreen.MEASURE -> MeasureScreen(viewModel)
                    AppScreen.GENERATOR -> GeneratorScreen(viewModel)
                    AppScreen.CONSOLE -> ConsoleScreen(viewModel)
                    AppScreen.CHAT_AI -> ChatAiScreen(viewModel)
                    AppScreen.SETTINGS -> SettingsScreen(viewModel)
                }
            }

            AnimatedVisibility(
                visible = processingStatus != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                    border = BorderStroke(1.dp, NeonPurple),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("processing_status_alert")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NeonPurple,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = processingStatus ?: "",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: SoundMasterViewModel) {
    val currentSplDb by viewModel.currentSplDb.collectAsStateWithLifecycle()
    val currentEstimatedRt60 by viewModel.currentEstimatedRt60.collectAsStateWithLifecycle()
    val mixerConnected by viewModel.mixerConnected.collectAsStateWithLifecycle()
    val mixerIp by viewModel.mixerIpAddress.collectAsStateWithLifecycle()
    val mappedPointsList by viewModel.recordings.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Bem-vindo Header Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("home_welcome_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "GERENCIADOR ACÚSTICO",
                        color = GlowCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ChurchSound IA",
                        color = BrightWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                    Text(
                        text = "Calibração física, espectro e diagnósticos inteligentes",
                        color = SoftGrey,
                        fontSize = 12.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Live quick info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceBlack.copy(alpha = 0.5f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (mixerConnected) NeonMint else GlowingError)
                            )
                            Text(
                                text = "MESA: ${if (mixerConnected) "CONECTADA" else "DESCONECTADA"}",
                                color = if (mixerConnected) NeonMint else SoftGrey,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Live SPL",
                                tint = GlowCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f dB SPL", currentSplDb),
                                color = GlowCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Menu de Funções
        item {
            Text(
                text = "FUNÇÕES DO APLICATIVO",
                color = SoftGrey,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        // Card 1: Medir & Analisar Acústica (Acoustic Mapping)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setScreen(AppScreen.MEASURE) }
                    .testTag("menu_measure_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NeonPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = "Medição",
                            tint = NeonPurple,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Medição e Análise",
                            color = BrightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Escaneamento acústico, picos de frequência e decaimento RT60.",
                            color = SoftGrey,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Ir",
                        tint = NeonPurple.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Card 2: Gerador de Sinas de Áudio
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, GlowCyan.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setScreen(AppScreen.GENERATOR) }
                    .testTag("menu_generator_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlowCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsInputAntenna,
                            contentDescription = "Gerador",
                            tint = GlowCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gerador de Sinais",
                            color = BrightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Conecte o som e emita Ruído Rosa, Branco e varreduras físicas.",
                            color = SoftGrey,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Ir",
                        tint = GlowCyan.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Card 3: Console Mixer Digital
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, ContentBlue.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setScreen(AppScreen.CONSOLE) }
                    .testTag("menu_mixer_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ContentBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Mixer",
                            tint = ContentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ajustar Mesa Digital",
                            color = BrightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Mixagem, faders auxiliares de retorno e presets inteligentes de voz.",
                            color = SoftGrey,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Ir",
                        tint = ContentBlue.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Card 4: Assistente de Diagnósticos IA
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, GlowCyan.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setScreen(AppScreen.CHAT_AI) }
                    .testTag("menu_chat_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlowCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = "Assistente",
                            tint = GlowCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Assistente de Diagnóstico IA",
                            color = BrightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Fale com o especialista virtual sobre sibilância, lama e acústica.",
                            color = SoftGrey,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Ir",
                        tint = GlowCyan.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Card 5: Conectividade & Logs
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, NeonMint.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setScreen(AppScreen.SETTINGS) }
                    .testTag("menu_settings_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NeonMint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Sistemas",
                            tint = NeonMint,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Conexões e Logs",
                            color = BrightWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Endereço de rede, status do túnel remoto e logs de telemetria.",
                            color = SoftGrey,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Ir",
                        tint = NeonMint.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // DIRECT DIRECTIVE QUICK ACTIONS
        item {
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SpaceBlack.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "AÇÕES DIRETAS DE AJUSTE / DIAGNÓSTICO:",
                        color = SoftGrey,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )

                    val quickTools = listOf(
                        Triple("Calcular RT60", Icons.Default.CompassCalibration, "Calcular decaimento RT60 do templo em tempo real"),
                        Triple("Bloquear Microfonia", Icons.Default.Security, "Como configurar o Supressor de Microfonia AFS2?"),
                        Triple("Equalizar Anti-Lama", Icons.Default.FilterAlt, "Como tratar o embolamento grave (lama) das vozes?"),
                        Triple("Alinhamento Igreja", Icons.Default.AutoGraph, "Como alinhar a resposta espectral da igreja no equalizador?")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickTools) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .clickable { 
                                        viewModel.sendChatMessage(item.third) 
                                        viewModel.setScreen(AppScreen.CHAT_AI)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = item.second,
                                        contentDescription = item.first,
                                        tint = NeonPurple,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = item.first,
                                        color = BrightWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // HISTÓRICO DE MAPEAMENTO DE COBERTURA DO TEMPLO
        if (mappedPointsList.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PONTOS ACÚSTICOS MAPEADOS NO TEMPLO:",
                    color = SoftGrey,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            items(mappedPointsList) { pt ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                    border = BorderStroke(1.dp, CardLine.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("home_mapped_point_${pt.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "Ponto",
                                    tint = NeonPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = pt.title,
                                    color = BrightWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonMint.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "RT60: ${pt.processedFilePath ?: "---"}",
                                        color = NeonMint,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonPurple.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${pt.appliedEffect ?: "---"}",
                                        color = NeonPurple,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pres. Sonora: ${pt.filePath} dB SPL. ${pt.transcription ?: ""}",
                            color = SoftGrey,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.sendChatMessage("Analise o ponto acústico da área '${pt.title}' que possui uma pressão de ${pt.filePath} dB SPL, decaimento RT60 de ${pt.processedFilePath} e frequência de pico em ${pt.appliedEffect}. O que fazer para calibrar?")
                                    viewModel.setScreen(AppScreen.CHAT_AI)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SpaceBlack.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = "Consultar",
                                    tint = GlowCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CONSULTAR COMENTÁRIO IA", color = GlowCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { viewModel.deleteRecording(pt.id) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GlowingError.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Deletar",
                                    tint = GlowingError,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MeasureScreen(viewModel: SoundMasterViewModel) {
    val activeMappingArea by viewModel.activeMappingArea.collectAsStateWithLifecycle()
    val isAcousticMapping by viewModel.isAcousticMapping.collectAsStateWithLifecycle()
    val mappingProgress by viewModel.mappingProgress.collectAsStateWithLifecycle()
    val currentSplDb by viewModel.currentSplDb.collectAsStateWithLifecycle()
    val currentRtaSpec by viewModel.currentRtaSpec.collectAsStateWithLifecycle()
    val currentPeakHz by viewModel.currentPeakHz.collectAsStateWithLifecycle()
    val currentEstimatedRt60 by viewModel.currentEstimatedRt60.collectAsStateWithLifecycle()

    val calibrationFile by viewModel.calibrationFile.collectAsStateWithLifecycle()
    val calibrationStatus by viewModel.calibrationStatus.collectAsStateWithLifecycle()
    val splOffset by viewModel.splOffset.collectAsStateWithLifecycle()

    val edtVal by viewModel.rt60Edt.collectAsStateWithLifecycle()
    val t20Val by viewModel.rt60T20.collectAsStateWithLifecycle()
    val t30Val by viewModel.rt60T30.collectAsStateWithLifecycle()
    val c50Val by viewModel.rt60C50.collectAsStateWithLifecycle()
    val c80Val by viewModel.rt60C80.collectAsStateWithLifecycle()
    val d50Val by viewModel.rt60D50.collectAsStateWithLifecycle()
    val stiVal by viewModel.rt60Sti.collectAsStateWithLifecycle()
    val stiCategory by viewModel.rt60StiCategory.collectAsStateWithLifecycle()

    val calcLength by viewModel.calcLength.collectAsStateWithLifecycle()
    val calcWidth by viewModel.calcWidth.collectAsStateWithLifecycle()
    val calcHeight by viewModel.calcHeight.collectAsStateWithLifecycle()
    val calcAbsorption by viewModel.calcAbsorption.collectAsStateWithLifecycle()
    val calcDelayDist by viewModel.calcDelayDist.collectAsStateWithLifecycle()
    val calcVolume by viewModel.calcVolume.collectAsStateWithLifecycle()
    val calcRt60 by viewModel.calcRt60.collectAsStateWithLifecycle()
    val calcDelayMs by viewModel.calcDelayMs.collectAsStateWithLifecycle()
    val calcShowResults by viewModel.calcShowResults.collectAsStateWithLifecycle()

    val activeMicSource by viewModel.activeMicSource.collectAsStateWithLifecycle()
    val highResRtaSpec by viewModel.highResRtaSpec.collectAsStateWithLifecycle()
    val tfMagnitude by viewModel.tfMagnitude.collectAsStateWithLifecycle()
    val tfPhase by viewModel.tfPhase.collectAsStateWithLifecycle()
    val feedbackDetected by viewModel.feedbackDetected.collectAsStateWithLifecycle()
    val feedbackFreq by viewModel.feedbackFreq.collectAsStateWithLifecycle()
    val autoCutEnabled by viewModel.autoCutEnabled.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Voltar
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setScreen(AppScreen.HOME) }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = GlowCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voltar para o Início", color = GlowCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // MICROPHONE SELECTOR
        item {
            var expanded by remember { mutableStateOf(false) }
            val micLabel = when (activeMicSource) {
                "CAMCORDER" -> "Microfone Camcorder"
                "VOICE_RECOGNITION" -> "Reconhecimento de Voz"
                "UNPROCESSED" -> "Unprocessed (Sem Processamento)"
                else -> "Microfone Padrão (MIC)"
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SpaceBlack.copy(alpha = 0.4f))
                    .border(1.dp, CardLine.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microfone",
                        tint = GlowCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ENTRADA:",
                        color = SoftGrey,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box {
                        Text(
                            text = micLabel,
                            color = GlowCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { expanded = true }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(DarkSlate)
                        ) {
                            val options = listOf(
                                Pair("MIC", "Microfone Padrão (MIC)"),
                                Pair("CAMCORDER", "Microfone Camcorder"),
                                Pair("VOICE_RECOGNITION", "Reconhecimento de Voz"),
                                Pair("UNPROCESSED", "Unprocessed (Sem Processamento)")
                            )
                            options.forEach { (valStr, textStr) ->
                                DropdownMenuItem(
                                    text = { Text(textStr, color = BrightWhite, fontSize = 12.sp) },
                                    onClick = {
                                        viewModel.setActiveMicSource(valStr)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Text(
                    text = "FFT: 1024",
                    color = SoftGrey,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // CARD MEDIR & ANALISAR
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("acoustic_mapping_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompassCalibration,
                                contentDescription = "Mapeador",
                                tint = NeonPurple
                            )
                        }
                        Column {
                            Text(
                                text = "Medir & Analisar Acústica",
                                color = BrightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Mapeamento espectral do templo em tempo real",
                                color = SoftGrey,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SELEÇÃO DA ÁREA DO TEMPLO
                    Text(
                        text = "ÁREA DISPOSITIVO / COBERTURA:",
                        color = SoftGrey,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val churchAreas = listOf(
                        "Púlpito / Altar",
                        "Nave - Frente",
                        "Nave - Fundo",
                        "Galeria Lateral E",
                        "Galeria Lateral D",
                        "Mezanino (Alta)"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(churchAreas) { area ->
                            val isSel = activeMappingArea == area
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) NeonPurple else SpaceBlack.copy(alpha = 0.4f))
                                    .border(1.dp, if (isSel) NeonPurple else CardLine.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (!isAcousticMapping) {
                                            viewModel.setActiveMappingArea(area)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = area,
                                    color = if (isSel) SpaceBlack else SoftGrey,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SPL METER & RTA FREQ SPECTRUM LAYOUT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SpaceBlack.copy(alpha = 0.4f))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // DB SPL METER
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(100.dp)
                        ) {
                            Text(
                                text = "NÍVEL SPL",
                                color = SoftGrey,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", currentSplDb),
                                color = if (currentSplDb > 85f) GlowingError else GlowCyan,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "dB SPL (dBA)",
                                color = SoftGrey,
                                fontSize = 9.sp
                            )
                        }

                        // RT60 & PEAK MINI HIGHLIGHTS
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RT60 estimado:", color = SoftGrey, fontSize = 11.sp)
                                Text(
                                    text = String.format(Locale.US, "%.2fs", currentEstimatedRt60),
                                    color = NeonMint,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pico Ressonante:", color = SoftGrey, fontSize = 11.sp)
                                Text(
                                    text = "${currentPeakHz}Hz",
                                    color = NeonPurple,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // FFT SPECTRUM VISUALIZER BARS
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SpaceBlack)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val bandsString = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
                            currentRtaSpec.forEachIndexed { idx, value ->
                                val scaledHeight = (value / 100f).coerceIn(0.1f, 1.0f)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .width(12.dp)
                                            .fillMaxHeight(scaledHeight)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        NeonPurple,
                                                        GlowCyan.copy(alpha = 0.7f),
                                                        NeonMint.copy(alpha = 0.4f)
                                                    )
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = bandsString.getOrElse(idx) { "" },
                                        color = SoftGrey,
                                        fontSize = 8.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // BUTTON SCAN TRIGGER
                    Button(
                        onClick = { viewModel.startAcousticMapping() },
                        enabled = mappingProgress == 0f,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (mappingProgress > 0f) NeonPurple.copy(alpha = 0.3f) else NeonPurple
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (mappingProgress > 0f) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = BrightWhite,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                               )
                                Text(
                                    text = "SALVANDO COORDENADA...",
                                    color = BrightWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Mapear",
                                    tint = SpaceBlack,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "SALVAR / MAPEAR COORDENADA ATIVA",
                                    color = SpaceBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }


        // CARD ANALISADOR DE ÁUDIO PRO (SMAART MODE)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("audio_analyzer_pro_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlowCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "Smaart Mode",
                                tint = GlowCyan
                            )
                        }
                        Column {
                            Text(
                                text = "Analisador de Áudio Pro",
                                color = BrightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Espectro em tempo real & Função de Transferência",
                                color = SoftGrey,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. RTA Graph (Real Time Analyzer)
                    Text(
                        text = "RTA (REAL TIME ANALYZER) - ALTA RESOLUÇÃO",
                        color = SoftGrey,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceBlack.copy(alpha = 0.6f))
                            .border(1.dp, CardLine.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp)) {
                            // Draw grid lines
                            val gridCols = 5
                            for (c in 1 until gridCols) {
                                val gx = c * size.width / gridCols
                                drawLine(
                                    color = CardLine.copy(alpha = 0.2f),
                                    start = Offset(gx, 0f),
                                    end = Offset(gx, size.height),
                                    strokeWidth = 1f
                                )
                            }
                            val gridRows = 4
                            for (r in 1 until gridRows) {
                                val gy = r * size.height / gridRows
                                drawLine(
                                    color = CardLine.copy(alpha = 0.2f),
                                    start = Offset(0f, gy),
                                    end = Offset(size.width, gy),
                                    strokeWidth = 1f
                                )
                            }

                            // Draw RTA Curve
                            if (highResRtaSpec.isNotEmpty()) {
                                val rtaPath = androidx.compose.ui.graphics.Path()
                                rtaPath.moveTo(0f, size.height)
                                for (i in highResRtaSpec.indices) {
                                    val px = i * size.width / (highResRtaSpec.size - 1)
                                    val dbVal = highResRtaSpec[i]
                                    val py = size.height - (dbVal - 10f) / 90f * size.height
                                    rtaPath.lineTo(px, py)
                                }
                                rtaPath.lineTo(size.width, size.height)
                                rtaPath.close()
                                drawPath(
                                    path = rtaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            NeonPurple.copy(alpha = 0.7f),
                                            NeonPurple.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                
                                // Draw RTA Stroke Line
                                val strokePath = androidx.compose.ui.graphics.Path()
                                for (i in highResRtaSpec.indices) {
                                    val px = i * size.width / (highResRtaSpec.size - 1)
                                    val dbVal = highResRtaSpec[i]
                                    val py = size.height - (dbVal - 10f) / 90f * size.height
                                    if (i == 0) strokePath.moveTo(px, py) else strokePath.lineTo(px, py)
                                }
                                drawPath(
                                    path = strokePath,
                                    color = NeonPurple,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("20Hz", color = SoftGrey, fontSize = 8.sp)
                        Text("200Hz", color = SoftGrey, fontSize = 8.sp)
                        Text("2kHz", color = SoftGrey, fontSize = 8.sp)
                        Text("20kHz", color = SoftGrey, fontSize = 8.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Magnitude Graph (Transfer Function)
                    Text(
                        text = "MAGNITUDE (TRANSFER FUNCTION)",
                        color = SoftGrey,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceBlack.copy(alpha = 0.6f))
                            .border(1.dp, CardLine.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp)) {
                            // Draw grid lines
                            val gridCols = 5
                            for (c in 1 until gridCols) {
                                val gx = c * size.width / gridCols
                                drawLine(
                                    color = CardLine.copy(alpha = 0.2f),
                                    start = Offset(gx, 0f),
                                    end = Offset(gx, size.height),
                                    strokeWidth = 1f
                                )
                            }
                            val gridRows = 4
                            for (r in 1 until gridRows) {
                                val gy = r * size.height / gridRows
                                drawLine(
                                    color = CardLine.copy(alpha = 0.2f),
                                    start = Offset(0f, gy),
                                    end = Offset(size.width, gy),
                                    strokeWidth = 1f
                                )
                            }

                            // Draw Magnitude Trace
                            if (tfMagnitude.isNotEmpty()) {
                                val magPath = androidx.compose.ui.graphics.Path()
                                for (i in tfMagnitude.indices) {
                                    val px = i * size.width / (tfMagnitude.size - 1)
                                    val dbVal = tfMagnitude[i]
                                    val py = (18f - dbVal) / 36f * size.height
                                    if (i == 0) magPath.moveTo(px, py) else magPath.lineTo(px, py)
                                }
                                drawPath(
                                    path = magPath,
                                    color = GlowCyan,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }
                        
                        // Scale label
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(SpaceBlack.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("dB / Hz", color = GlowCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("20Hz", color = SoftGrey, fontSize = 8.sp)
                        Text("200Hz", color = SoftGrey, fontSize = 8.sp)
                        Text("2kHz", color = SoftGrey, fontSize = 8.sp)
                        Text("20kHz", color = SoftGrey, fontSize = 8.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Phase Graph
                    Text(
                        text = "RESPOSTA DE FASE",
                        color = SoftGrey,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceBlack.copy(alpha = 0.6f))
                            .border(1.dp, CardLine.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp)) {
                            // Draw grid lines
                            val gridCols = 5
                            for (c in 1 until gridCols) {
                                val gx = c * size.width / gridCols
                                drawLine(
                                    color = CardLine.copy(alpha = 0.2f),
                                    start = Offset(gx, 0f),
                                    end = Offset(gx, size.height),
                                    strokeWidth = 1f
                                )
                            }
                            val gridRows = 4
                            for (r in 1 until gridRows) {
                                val gy = r * size.height / gridRows
                                drawLine(
                                    color = CardLine.copy(alpha = 0.2f),
                                    start = Offset(0f, gy),
                                    end = Offset(size.width, gy),
                                    strokeWidth = 1f
                                )
                            }

                            // Draw Phase Trace
                            if (tfPhase.isNotEmpty()) {
                                val phasePath = androidx.compose.ui.graphics.Path()
                                for (i in tfPhase.indices) {
                                    val px = i * size.width / (tfPhase.size - 1)
                                    val phaseVal = tfPhase[i]
                                    val py = (180f - phaseVal) / 360f * size.height
                                    if (i == 0) phasePath.moveTo(px, py) else phasePath.lineTo(px, py)
                                }
                                drawPath(
                                    path = phasePath,
                                    color = Color(0xFFFFB300), // Amber color
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
                                )
                            }
                        }
                        
                        // Scale label
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(SpaceBlack.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Deg / Hz", color = Color(0xFFFFB300), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("20Hz", color = SoftGrey, fontSize = 8.sp)
                        Text("200Hz", color = SoftGrey, fontSize = 8.sp)
                        Text("2kHz", color = SoftGrey, fontSize = 8.sp)
                        Text("20kHz", color = SoftGrey, fontSize = 8.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Feedback Detector Layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "DETECTOR DE FEEDBACK",
                            color = BrightWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Auto-Cut Switch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AUTO-CUT",
                                color = SoftGrey,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = autoCutEnabled,
                                onCheckedChange = { viewModel.toggleAutoCut(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GlowingError,
                                    checkedTrackColor = GlowingError.copy(alpha = 0.4f),
                                    uncheckedThumbColor = SoftGrey,
                                    uncheckedTrackColor = SpaceBlack
                                ),
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Feedback status banner
                    if (feedbackDetected) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlowingError.copy(alpha = 0.15f))
                                .border(1.dp, GlowingError.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "🚨 REALIMENTAÇÃO DETECTADA EM %.0f Hz!", feedbackFreq),
                                color = GlowingError,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = { viewModel.triggerFeedbackCut() },
                                colors = ButtonDefaults.buttonColors(containerColor = GlowingError),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Text("CORTAR FREQUÊNCIA NA MESA", color = SpaceBlack, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonMint.copy(alpha = 0.1f))
                                .border(1.dp, NeonMint.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✓ SEM PICOS DE REALIMENTAÇÃO PERIGOSOS",
                                color = NeonMint,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // CALIBRATION SECTOR IN MEASUREMENT FOR CLEAN WORKFLOW
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("calibration_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonMint.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsVoice,
                                contentDescription = "Calibração",
                                tint = NeonMint
                            )
                        }
                        Column {
                            Text(
                                text = "Ajuste & Calibração Mic",
                                color = BrightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Calibração física de microfone e SPL",
                                color = SoftGrey,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "🎙 Arquivo de Calibração",
                        color = BrightWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.selectCalibrationFile("Dayton_EMM6_Cal.txt") },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonMint),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Carregar arquivo", color = SpaceBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Text(
                            text = calibrationFile ?: "Nenhum arquivo (.cal)",
                            color = SoftGrey,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (calibrationFile != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SpaceBlack.copy(alpha = 0.4f))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(calibrationStatus, color = NeonMint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { viewModel.resetMicrophoneCalib() },
                                colors = ButtonDefaults.buttonColors(containerColor = GlowingError.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, GlowingError),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("LIMPAR", color = GlowingError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "📢 Calibração de SPL a 94dB",
                        color = BrightWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { viewModel.calibrateSpl() },
                            colors = ButtonDefaults.buttonColors(containerColor = SpaceBlack.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, NeonMint),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Calibrar a 94dB (1kHz)", color = BrightWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "Offset: ${if (splOffset > 0f) "+$splOffset" else "$splOffset"} dB",
                            color = NeonMint,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // CARD SCHROEDER E PARÂMETROS ACÚSTICOS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("schroeder_parameters_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlowCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Schroeder",
                                tint = GlowCyan
                            )
                        }
                        Column {
                            Text(
                                text = "Schroeder & Parâmetros",
                                color = BrightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Inteligibilidade (STI) e tempos de decaimento",
                                color = SoftGrey,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Grid 2x4 for EDT, T20, T30, RT60 and C50, C80, D50, STI
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // EDT
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(SpaceBlack.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .border(1.dp, CardLine.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("EDT", color = SoftGrey, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (edtVal > 0f) String.format(Locale.US, "%.2fs", edtVal) else "--",
                                    color = GlowCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            // T20
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(SpaceBlack.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .border(1.dp, CardLine.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("T20", color = SoftGrey, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (t20Val > 0f) String.format(Locale.US, "%.2fs", t20Val) else "--",
                                    color = NeonPurple,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            // T30
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(SpaceBlack.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .border(1.dp, CardLine.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("T30", color = SoftGrey, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (t30Val > 0f) String.format(Locale.US, "%.2fs", t30Val) else "--",
                                    color = GlowingError,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            // RT60 Final
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(SpaceBlack.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .border(1.dp, CardLine.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("RT60", color = SoftGrey, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%.2fs", currentEstimatedRt60),
                                    color = BrightWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // C50
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(SpaceBlack.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .border(1.dp, CardLine.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("C50", color = SoftGrey, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (c50Val != 0f) String.format(Locale.US, "%.1f dB", c50Val) else "--",
                                    color = GlowCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            // C80
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(SpaceBlack.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .border(1.dp, CardLine.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("C80", color = SoftGrey, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (c80Val != 0f) String.format(Locale.US, "%.1f dB", c80Val) else "--",
                                    color = GlowCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            // D50
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(SpaceBlack.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .border(1.dp, CardLine.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("D50", color = SoftGrey, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (d50Val > 0f) String.format(Locale.US, "%.0f%%", d50Val) else "--",
                                    color = NeonMint,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            // STI
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(SpaceBlack.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .border(1.dp, CardLine.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("STI", color = SoftGrey, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (stiVal > 0f) String.format(Locale.US, "%.2f", stiVal) else "--",
                                    color = GlowingError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                if (stiVal > 0f) {
                                    Text(
                                        text = stiCategory,
                                        color = if (stiCategory == "Excelente" || stiCategory == "Bom") NeonMint else GlowingError,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // CARD CALCULADORA FÍSICA (SABINE/EYRING)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("acoustic_calculator_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Calculadora",
                                tint = NeonPurple
                            )
                        }
                        Column {
                            Text(
                                text = "Calculadora Acústica",
                                color = BrightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Estimativa de RT60 por Sabine & Eyring",
                                color = SoftGrey,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Sliders: Comprimento, Largura, Altura
                    Text("Dimensões da Sala (metros):", color = SoftGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Length Slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Comprimento: ${calcLength.toInt()}m", color = BrightWhite, fontSize = 11.sp, modifier = Modifier.width(110.dp))
                        Slider(
                            value = calcLength,
                            onValueChange = { viewModel.setCalcLength(it) },
                            valueRange = 5f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonPurple,
                                activeTrackColor = NeonPurple,
                                inactiveTrackColor = SpaceBlack
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Width Slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Largura: ${calcWidth.toInt()}m", color = BrightWhite, fontSize = 11.sp, modifier = Modifier.width(110.dp))
                        Slider(
                            value = calcWidth,
                            onValueChange = { viewModel.setCalcWidth(it) },
                            valueRange = 5f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonPurple,
                                activeTrackColor = NeonPurple,
                                inactiveTrackColor = SpaceBlack
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Height Slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Altura: ${calcHeight.toInt()}m", color = BrightWhite, fontSize = 11.sp, modifier = Modifier.width(110.dp))
                        Slider(
                            value = calcHeight,
                            onValueChange = { viewModel.setCalcHeight(it) },
                            valueRange = 2f..25f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonPurple,
                                activeTrackColor = NeonPurple,
                                inactiveTrackColor = SpaceBlack
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Delay Distance Slider
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dist. PA-Aux: ${calcDelayDist.toInt()}m", color = BrightWhite, fontSize = 11.sp, modifier = Modifier.width(110.dp))
                        Slider(
                            value = calcDelayDist,
                            onValueChange = { viewModel.setCalcDelayDist(it) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonPurple,
                                activeTrackColor = NeonPurple,
                                inactiveTrackColor = SpaceBlack
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Absorption selection buttons
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Absorção de Superfície Estimada (α):", color = SoftGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    val absorptionOptions = listOf(
                        0.05f to "Baixa (0.05)",
                        0.15f to "Média (0.15)",
                        0.30f to "Alta (0.30)"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        absorptionOptions.forEach { (coeff, label) ->
                            val isSel = calcAbsorption == coeff
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) NeonPurple else SpaceBlack.copy(alpha = 0.4f))
                                    .border(1.dp, if (isSel) NeonPurple else CardLine.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setCalcAbsorption(coeff) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) SpaceBlack else SoftGrey,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.calculateAcoustics() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Text("CALCULAR ACÚSTICA", color = SpaceBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        if (calcShowResults) {
                            Button(
                                onClick = { viewModel.clearAcousticCalc() },
                                colors = ButtonDefaults.buttonColors(containerColor = SpaceBlack.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, GlowingError),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text("LIMPAR", color = GlowingError, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    // Display results if active
                    if (calcShowResults) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SpaceBlack.copy(alpha = 0.5f))
                                .border(1.dp, CardLine.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Volume Total:", color = SoftGrey, fontSize = 11.sp)
                                Text(String.format(Locale.US, "%.0f m³", calcVolume), color = GlowCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RT60 Estimado (Eyring):", color = SoftGrey, fontSize = 11.sp)
                                Text(
                                    String.format(Locale.US, "%.2fs", calcRt60),
                                    color = if (calcRt60 > 1.6f) GlowingError else if (calcRt60 >= 1.4f) NeonMint else GlowCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (calcDelayMs > 0f) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Atraso Auxiliares (Delay):", color = SoftGrey, fontSize = 11.sp)
                                    Text(String.format(Locale.US, "%.1f ms", calcDelayMs), color = GlowCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Dica: Para igrejas, busque um RT60 entre 1.4s e 1.6s. Valores muito altos (reverb longo) embolam a inteligibilidade da pregação.",
                                color = SoftGrey,
                                fontSize = 10.sp,
                                style = androidx.compose.ui.text.TextStyle(lineHeight = 13.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratorScreen(viewModel: SoundMasterViewModel) {
    val signalActive by viewModel.signalGeneratorActive.collectAsStateWithLifecycle()
    val signalType by viewModel.signalType.collectAsStateWithLifecycle()
    val signalLevel by viewModel.signalGeneratorLevel.collectAsStateWithLifecycle()
    val sweepLogarithmic by viewModel.sweepLogarithmic.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Voltar
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setScreen(AppScreen.HOME) }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = GlowCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voltar para o Início", color = GlowCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // GERADOR CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("signal_generator_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsInputAntenna,
                                contentDescription = "Gerador",
                                tint = NeonPurple
                            )
                        }
                        Column {
                            Text(
                                text = "Gerador de Sinais",
                                color = BrightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Alinhamento e calibração do sistema de PA de templo",
                                color = SoftGrey,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF231D15)),
                        border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Dica",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Dica: Conecte a saída de fone de ouvido do celular a um canal mono ou estéreo na mesa para jogar o som no PA do templo.",
                                color = Color(0xFFFCD34D),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Sinais de Teste Disponíveis:",
                        color = BrightWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Toque em qualquer sinal para ligar, desligar ou trocar a emissão.",
                        color = SoftGrey,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val signals = listOf(
                        Triple("pink", "Ruído Rosa", Icons.Default.VolumeUp),
                        Triple("white", "Ruído Branco", Icons.Default.Waves),
                        Triple("mls", "Sequência MLS", Icons.Default.BarChart),
                        Triple("chirp", "Varredura Chirp", Icons.Default.TrendingUp),
                        Triple("dual", "Modo Dual-Tone", Icons.Default.GraphicEq),
                        Triple("measure_pink", "Rosa Filtrado Vocais", Icons.Default.LinearScale)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in 0..2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0..1) {
                                    val index = row * 2 + col
                                    if (index < signals.size) {
                                        val sig = signals[index]
                                        val isSel = signalType == sig.first && signalActive
                                        val borderCol = if (isSel) NeonPurple else CardLine.copy(alpha = 0.2f)
                                        val bgGrad = if (isSel) {
                                            Brush.verticalGradient(listOf(Color(0xFF2E1A47), Color(0xFF1F1235)))
                                        } else {
                                            Brush.verticalGradient(listOf(SpaceBlack.copy(alpha = 0.4f), SpaceBlack.copy(alpha = 0.2f)))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(bgGrad)
                                                .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    if (signalType == sig.first && signalActive) {
                                                        viewModel.toggleSignalGenerator()
                                                    } else {
                                                        viewModel.setSignalType(sig.first)
                                                        if (!signalActive) viewModel.toggleSignalGenerator()
                                                    }
                                                }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = sig.third,
                                                    contentDescription = sig.second,
                                                    tint = if (isSel) NeonPurple else SoftGrey.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = sig.second,
                                                    color = if (isSel) BrightWhite else SoftGrey,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ATENUAÇÃO DE NÍVEL",
                            color = SoftGrey,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$signalLevel dBFS",
                            color = NeonPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Slider(
                        value = signalLevel.toFloat(),
                        onValueChange = { viewModel.setSignalGeneratorLevel(it.toInt()) },
                        valueRange = -60f..0f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = NeonPurple,
                            inactiveTrackColor = CardLine.copy(alpha = 0.3f),
                            thumbColor = NeonPurple
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("-60dB (Seguro)", color = SoftGrey.copy(alpha = 0.5f), fontSize = 10.sp)
                        Text("0dB (Máximo)", color = SoftGrey.copy(alpha = 0.5f), fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SpaceBlack.copy(alpha = 0.3f))
                            .clickable { viewModel.toggleSweepLogarithmic() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = sweepLogarithmic,
                            onCheckedChange = { viewModel.toggleSweepLogarithmic() },
                            colors = CheckboxDefaults.colors(checkedColor = NeonPurple)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fazer Varredura Logarítmica (20Hz - 20kHz)",
                            color = BrightWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Global Stop Button
                    Button(
                        onClick = { viewModel.toggleSignalGenerator() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (signalActive) GlowingError else NeonPurple
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            text = if (signalActive) "PARAR EMISSÃO DE SINAL" else "INICIAR EMISSÃO DE SINAL SELECIONADO",
                            color = if (signalActive) BrightWhite else SpaceBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: SoundMasterViewModel) {
    val signalActive by viewModel.signalGeneratorActive.collectAsStateWithLifecycle()
    val signalType by viewModel.signalType.collectAsStateWithLifecycle()
    val signalLevel by viewModel.signalGeneratorLevel.collectAsStateWithLifecycle()
    val sweepLogarithmic by viewModel.sweepLogarithmic.collectAsStateWithLifecycle()

    val calibrationFile by viewModel.calibrationFile.collectAsStateWithLifecycle()
    val calibrationStatus by viewModel.calibrationStatus.collectAsStateWithLifecycle()
    val splOffset by viewModel.splOffset.collectAsStateWithLifecycle()

    val mixerIp by viewModel.mixerIpAddress.collectAsStateWithLifecycle()
    val mixerConnected by viewModel.mixerConnected.collectAsStateWithLifecycle()
    val mixerModel by viewModel.mixerModel.collectAsStateWithLifecycle()
    val mixerFirmware by viewModel.mixerFirmware.collectAsStateWithLifecycle()

    val tunnelActive by viewModel.tunnelActive.collectAsStateWithLifecycle()
    val tunnelLink by viewModel.tunnelLink.collectAsStateWithLifecycle()

    val activeMappingArea by viewModel.activeMappingArea.collectAsStateWithLifecycle()
    val isAcousticMapping by viewModel.isAcousticMapping.collectAsStateWithLifecycle()
    val mappingProgress by viewModel.mappingProgress.collectAsStateWithLifecycle()
    val currentSplDb by viewModel.currentSplDb.collectAsStateWithLifecycle()
    val currentRtaSpec by viewModel.currentRtaSpec.collectAsStateWithLifecycle()
    val currentPeakHz by viewModel.currentPeakHz.collectAsStateWithLifecycle()
    val currentEstimatedRt60 by viewModel.currentEstimatedRt60.collectAsStateWithLifecycle()
    val mappedPointsList by viewModel.recordings.collectAsStateWithLifecycle()

    var tempIp by remember { mutableStateOf(mixerIp) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // MEDIR & ANALISAR (MAPEAMENTO ACÚSTICO DO TEMPLO - IMAGE 2 DIRECTIVES)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("acoustic_mapping_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompassCalibration,
                                contentDescription = "Mapeador",
                                tint = NeonPurple
                            )
                        }
                        Column {
                            Text(
                                text = "Medir & Analisar Acústica",
                                color = BrightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Mapeamento espectral do templo em tempo real",
                                color = SoftGrey,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SELEÇÃO DA ÁREA DO TEMPLO
                    Text(
                        text = "ÁREA DISPOSITIVO / COBERTURA:",
                        color = SoftGrey,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val churchAreas = listOf(
                        "Púlpito / Altar",
                        "Nave - Frente",
                        "Nave - Fundo",
                        "Galeria Lateral E",
                        "Galeria Lateral D",
                        "Mezanino (Alta)"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(churchAreas) { area ->
                            val isSel = activeMappingArea == area
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) NeonPurple else SpaceBlack.copy(alpha = 0.4f))
                                    .border(1.dp, if (isSel) NeonPurple else CardLine.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (!isAcousticMapping) {
                                            viewModel.setActiveMappingArea(area)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = area,
                                    color = if (isSel) SpaceBlack else SoftGrey,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SPL METER & RTA FREQ SPECTRUM LAYOUT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SpaceBlack.copy(alpha = 0.4f))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // DB SPL METER
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(100.dp)
                        ) {
                            Text(
                                text = "NÍVEL SPL",
                                color = SoftGrey,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", currentSplDb),
                                color = if (currentSplDb > 85f) GlowingError else GlowCyan,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "dB SPL (dBA)",
                                color = SoftGrey,
                                fontSize = 9.sp
                            )
                        }

                        // RT60 & PEAK MINI HIGHLIGHTS
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RT60 estimado:", color = SoftGrey, fontSize = 11.sp)
                                Text(
                                    text = String.format(Locale.US, "%.2fs", currentEstimatedRt60),
                                    color = NeonMint,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pico Ressonante:", color = SoftGrey, fontSize = 11.sp)
                                Text(
                                    text = "${currentPeakHz}Hz",
                                    color = NeonPurple,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // FFT SPECTRUM VISUALIZER BARS (DRAWN AS COLUMNS)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SpaceBlack)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val bandsString = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
                            currentRtaSpec.forEachIndexed { idx, value ->
                                val scaledHeight = (value / 100f).coerceIn(0.1f, 1.0f)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .width(12.dp)
                                            .fillMaxHeight(scaledHeight)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        NeonPurple,
                                                        GlowCyan.copy(alpha = 0.7f),
                                                        NeonMint.copy(alpha = 0.4f)
                                                    )
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = bandsString.getOrElse(idx) { "" },
                                        color = SoftGrey,
                                        fontSize = 8.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // BUTTON SCAN TRIGGER
                    Button(
                        onClick = { viewModel.startAcousticMapping() },
                        enabled = !isAcousticMapping,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAcousticMapping) NeonPurple.copy(alpha = 0.3f) else NeonPurple
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (isAcousticMapping) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = BrightWhite,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "ESCANEANDO AMBIENTE (${(mappingProgress * 100).toInt()}%)",
                                    color = BrightWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Mapear",
                                    tint = SpaceBlack,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "SALVAR / MAPEAR COORDENADA ATIVA",
                                    color = SpaceBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // HISTÓRICO DE MAPEAMENTO DE COBERTURA DO TEMPLO
                    if (mappedPointsList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "PONTOS ACÚSTICOS MAPEADOS NO TEMPLO:",
                            color = SoftGrey,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            mappedPointsList.forEach { pt ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SpaceBlack.copy(alpha = 0.6f)),
                                    border = BorderStroke(1.dp, CardLine.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Place,
                                                    contentDescription = "Ponto",
                                                    tint = NeonPurple,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = pt.title,
                                                    color = BrightWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(NeonMint.copy(alpha = 0.12f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "RT60: ${pt.processedFilePath ?: "---"}",
                                                        color = NeonMint,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(NeonPurple.copy(alpha = 0.12f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "${pt.appliedEffect ?: "---"}",
                                                        color = NeonPurple,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Pres. Sonora: ${pt.filePath} dB SPL. ${pt.transcription ?: ""}",
                                            color = SoftGrey,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.sendChatMessage("Analise o ponto acústico da área '${pt.title}' que possui uma pressão de ${pt.filePath} dB SPL, decaimento RT60 de ${pt.processedFilePath} e frequência de pico em ${pt.appliedEffect}. O que fazer para calibrar?")
                                                    viewModel.setScreen(AppScreen.CHAT_AI)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DarkSlate),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f).height(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Forum,
                                                    contentDescription = "Consultar",
                                                    tint = GlowCyan,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("CONSULTAR COMENTÁRIO IA", color = GlowCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteRecording(pt.id) },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(GlowingError.copy(alpha = 0.15f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Deletar",
                                                    tint = GlowingError,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // GERADOR DE SINAIS SECTION
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("signal_generator_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsInputAntenna,
                                contentDescription = "Gerador",
                                tint = NeonPurple
                            )
                        }
                        Column {
                            Text(
                                text = "Gerador de Sinais",
                                color = BrightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Alinhamento e calibração do sistema de PA",
                                color = SoftGrey,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tip / Dica
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF231D15)),
                        border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Dica",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Dica: Use a saída de fone ligada à mesa para jogar o som no PA e calibrar o sistema.",
                                color = Color(0xFFFCD34D),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Sinais de Teste",
                        color = BrightWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Selecione o tipo de sinal para emitir pelo sistema de áudio.",
                        color = SoftGrey,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Signal Grid 2x3
                    val signals = listOf(
                        Triple("pink", "Rosa", Icons.Default.VolumeUp),
                        Triple("white", "Branco", Icons.Default.Waves),
                        Triple("mls", "MLS", Icons.Default.BarChart),
                        Triple("chirp", "Chirp", Icons.Default.TrendingUp),
                        Triple("dual", "Dual-Tone", Icons.Default.GraphicEq),
                        Triple("measure_pink", "Medir Rosa", Icons.Default.LinearScale)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in 0..2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0..1) {
                                    val index = row * 2 + col
                                    if (index < signals.size) {
                                        val sig = signals[index]
                                        val isSel = signalType == sig.first && signalActive
                                        val borderCol = if (isSel) NeonPurple else CardLine.copy(alpha = 0.2f)
                                        val bgGrad = if (isSel) {
                                            Brush.verticalGradient(listOf(Color(0xFF2E1A47), Color(0xFF1F1235)))
                                        } else {
                                            Brush.verticalGradient(listOf(SpaceBlack.copy(alpha = 0.4f), SpaceBlack.copy(alpha = 0.2f)))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(bgGrad)
                                                .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    if (signalType == sig.first && signalActive) {
                                                        viewModel.toggleSignalGenerator()
                                                    } else {
                                                        viewModel.setSignalType(sig.first)
                                                        if (!signalActive) viewModel.toggleSignalGenerator()
                                                    }
                                                }
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = sig.third,
                                                    contentDescription = sig.second,
                                                    tint = if (isSel) NeonPurple else SoftGrey.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = sig.second,
                                                    color = if (isSel) BrightWhite else SoftGrey,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // LEVEL SLIDER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NÍVEL DE SAÍDA",
                            color = SoftGrey,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$signalLevel dB",
                            color = NeonPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Slider(
                        value = signalLevel.toFloat(),
                        onValueChange = { viewModel.setSignalGeneratorLevel(it.toInt()) },
                        valueRange = -60f..0f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = NeonPurple,
                            inactiveTrackColor = CardLine.copy(alpha = 0.3f),
                            thumbColor = NeonPurple
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("-60dB", color = SoftGrey.copy(alpha = 0.5f), fontSize = 10.sp)
                        Text("0dB", color = SoftGrey.copy(alpha = 0.5f), fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sweep Logarithmic checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SpaceBlack.copy(alpha = 0.3f))
                            .clickable { viewModel.toggleSweepLogarithmic() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = sweepLogarithmic,
                            onCheckedChange = { viewModel.toggleSweepLogarithmic() },
                            colors = CheckboxDefaults.colors(checkedColor = NeonPurple)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sweep Logarítmico (20Hz - 20kHz)",
                            color = BrightWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // CALIBRAÇÃO SECTION
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("calibration_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonMint.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsVoice,
                                contentDescription = "Calibração",
                                tint = NeonMint
                            )
                        }
                        Column {
                            Text(
                                text = "Calibração",
                                color = BrightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Calibração de microfone e nível SPL",
                                color = SoftGrey,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tip / Dica High Res (32k)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14221F)),
                        border = BorderStroke(1.dp, NeonMint.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Dica",
                                tint = NeonMint,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Dica: Use o modo High-Res (32k) para detectar ressonâncias graves com precisão de 1.3Hz.",
                                color = NeonMint,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "🎙 Calibração de Microfone",
                        color = BrightWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Carregue o arquivo de calibração (.txt ou .cal) fornecido pelo fabricante (ex: Dayton EMM-6, Sonarworks).",
                        color = SoftGrey,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.selectCalibrationFile("Dayton_EMM6_Cal.txt") },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonMint),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Escolher arquivo", color = SpaceBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Text(
                            text = calibrationFile ?: "Nenhum arquivo escolhido",
                            color = SoftGrey,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceBlack.copy(alpha = 0.4f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Status:", color = SoftGrey, fontSize = 10.sp)
                            Text(
                                text = calibrationStatus,
                                color = if (calibrationFile != null) NeonMint else Color(0xFFFFB300),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (calibrationFile != null) {
                            Button(
                                onClick = { viewModel.resetMicrophoneCalib() },
                                colors = ButtonDefaults.buttonColors(containerColor = GlowingError.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, GlowingError),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("RESETAR MIC", color = GlowingError, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "📢 Calibração de Nível (SPL)",
                        color = BrightWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Insira um calibrador acústico de 94dB (1kHz) no microfone e clique em calibrar para parear a leitura com o SPL real.",
                        color = SoftGrey,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { viewModel.calibrateSpl() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSlate),
                            border = BorderStroke(1.dp, NeonMint),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Calibrar a 94dB", color = BrightWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Text(
                            text = "Offset Global: ${if (splOffset > 0f) "+$splOffset" else "$splOffset"} dB",
                            color = NeonMint,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // SISTEMAS & CONECTIVIDADE SECTION
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("connectivity_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlowCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Conectividade",
                                tint = GlowCyan
                            )
                        }
                        Column {
                            Text(
                                text = "Sistemas & Conectividade",
                                color = BrightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Gestão de IP, rede e infraestrutura do servidor",
                                color = SoftGrey,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "MESA DE SOM (UI24R)",
                        color = SoftGrey,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tempIp,
                            onValueChange = { tempIp = it },
                            placeholder = { Text("ex: 10.10.1.1", color = SoftGrey.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GlowCyan,
                                unfocusedBorderColor = CardLine.copy(alpha = 0.3f),
                                focusedLabelColor = GlowCyan,
                                focusedTextColor = BrightWhite,
                                unfocusedTextColor = BrightWhite
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(56.dp)
                        )

                        Button(
                            onClick = {
                                if (mixerConnected) viewModel.disconnectMixer()
                                else viewModel.connectMixer(tempIp)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlowCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text(
                                text = if (mixerConnected) "DESCONECTAR" else "CONECTAR",
                                color = SpaceBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Detail items
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceBlack.copy(alpha = 0.4f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Modelo Detectado:", color = SoftGrey, fontSize = 9.sp)
                            Text(
                                text = mixerModel,
                                color = if (mixerConnected) GlowCyan else SoftGrey,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Versão Firmware:", color = SoftGrey, fontSize = 9.sp)
                            Text(
                                text = mixerFirmware,
                                color = SoftGrey,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Column {
                                Text("CANAIS", color = SoftGrey, fontSize = 8.sp, textAlign = TextAlign.Center)
                                Text(if (mixerConnected) "24" else "-", color = BrightWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                            }
                            Column {
                                Text("AUX", color = SoftGrey, fontSize = 8.sp, textAlign = TextAlign.Center)
                                Text(if (mixerConnected) "10" else "-", color = BrightWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                            }
                            Column {
                                Text("FX", color = SoftGrey, fontSize = 8.sp, textAlign = TextAlign.Center)
                                Text(if (mixerConnected) "-" else "-", color = SoftGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "STATUS DO SERVIDOR & TÚNEL",
                        color = SoftGrey,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Tunnel Details Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SpaceBlack.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, CardLine.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Acesso Externo (Túnel)", color = BrightWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (tunnelActive) "Espelho ativo público" else "Desativado",
                                        color = if (tunnelActive) GlowCyan else SoftGrey,
                                        fontSize = 10.sp
                                    )
                                }

                                Switch(
                                    checked = tunnelActive,
                                    onCheckedChange = { viewModel.toggleTunnel() },
                                    colors = SwitchDefaults.colors(checkedThumbColor = GlowCyan)
                                )
                            }

                            if (tunnelActive && tunnelLink != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                SelectionContainer {
                                    Text(
                                        text = tunnelLink ?: "",
                                        color = GlowCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.toggleTunnel() },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (tunnelActive) GlowingError.copy(alpha = 0.15f) else GlowCyan),
                                    border = if (tunnelActive) BorderStroke(1.dp, GlowingError) else null,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (tunnelActive) "DESATIVAR TÚNEL" else "ATIVAR TÚNEL",
                                        color = if (tunnelActive) GlowingError else SpaceBlack,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }

                                Button(
                                    onClick = { /* Simple prompt feedback */ },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSlate),
                                    border = BorderStroke(1.dp, CardLine.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = tunnelActive,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("COPIAR LINK", color = if (tunnelActive) BrightWhite else SoftGrey.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConsoleScreen(viewModel: SoundMasterViewModel) {
    val channelFaders by viewModel.channelFaders.collectAsStateWithLifecycle()
    val channelMutes by viewModel.channelMutes.collectAsStateWithLifecycle()
    val targetChannel by viewModel.targetChannel.collectAsStateWithLifecycle()

    val auxMixes by viewModel.auxMixes.collectAsStateWithLifecycle()

    var activeMixTarget by remember { mutableStateOf("PA") } // "PA" or "AUX"
    var activeAuxNum by remember { mutableStateOf(1) } // 1..10

    var selectedVoicePresetCh by remember { mutableStateOf(targetChannel) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // HEADER SELECTION SELECT TARGET
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ALVO DO MIX (FADERS):",
                        color = SoftGrey,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { activeMixTarget = "PA" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (activeMixTarget == "PA") GlowCyan else SpaceBlack.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("PA / MASTER MIX", color = if (activeMixTarget == "PA") SpaceBlack else BrightWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { activeMixTarget = "AUX" },
                            colors = ButtonDefaults.buttonColors(containerColor = if (activeMixTarget == "AUX") NeonMint else SpaceBlack.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ENVIO AUXILIARES", color = if (activeMixTarget == "AUX") SpaceBlack else BrightWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    if (activeMixTarget == "AUX") {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(10) { idx ->
                                val auxId = idx + 1
                                val name = auxMixes[auxId]?.name ?: "AUX $auxId"
                                val isSel = activeAuxNum == auxId
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) NeonMint else SpaceBlack.copy(alpha = 0.5f))
                                        .border(1.dp, if (isSel) NeonMint else CardLine.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .clickable { activeAuxNum = auxId }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${auxId}: $name",
                                        color = if (isSel) SpaceBlack else SoftGrey,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // MIX DE ENTRADA (24 CHANNELS)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("input_mix_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GlowCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LinearScale,
                                contentDescription = "Faders",
                                tint = GlowCyan
                            )
                        }
                        Column {
                            Text(
                                text = "Mix de Entrada",
                                color = BrightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Controle dos 24 canais e processamento IA",
                                color = SoftGrey,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Horizontal faders array list
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    ) {
                        items((1..24).toList()) { chId ->
                            val faderVal = channelFaders[chId] ?: 0.70f
                            val isMuted = channelMutes[chId] ?: false
                            val isSelected = targetChannel == chId

                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) SpaceBlack else SpaceBlack.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, if (isSelected) GlowCyan else CardLine.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .width(76.dp)
                                    .fillMaxHeight()
                                    .clickable { viewModel.setTargetChannel(chId) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "CH ${String.format("%02d", chId)}",
                                        color = if (isSelected) GlowCyan else SoftGrey,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Vertical slider mock/custom
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxHeight()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .weight(1f)
                                                    .background(CardLine.copy(alpha = 0.3f))
                                            )
                                        }

                                        Slider(
                                            value = faderVal,
                                            onValueChange = { viewModel.setChannelFader(chId, it) },
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = if (isMuted) SoftGrey else GlowCyan,
                                                thumbColor = if (isMuted) SoftGrey else GlowCyan
                                            ),
                                            valueRange = 0f..1f,
                                            modifier = Modifier
                                                .height(140.dp)
                                                .scale(0.8f) // scale slider to look vertical inside cards
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "CANAL $chId",
                                            color = BrightWhite,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = { viewModel.toggleChannelMute(chId) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isMuted) GlowingError else DarkSlate
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(24.dp)
                                        ) {
                                            Text(
                                                text = "MUTE",
                                                color = if (isMuted) SpaceBlack else GlowingError,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // MIX DE MONITORES (AUX 1-10)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("monitor_mix_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonMint.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsInputComponent,
                                contentDescription = "Monitores",
                                tint = NeonMint
                            )
                        }
                        Column {
                            Text(
                                text = "Mix de Monitores (AUX)",
                                color = BrightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Controle de palco e envios auxiliares (1-10)",
                                color = SoftGrey,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tip card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14221F)),
                        border = BorderStroke(1.dp, NeonMint.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Dica Aux",
                                tint = NeonMint,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Dica: Monitore os auxiliares via AES67 na página Saúde de Cabos para garantir que o músico ouça um som sem ruídos.",
                                color = NeonMint,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Horizontal list of Monitor lines
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    ) {
                        items(auxMixes.entries.toList()) { entry ->
                            val auxId = entry.key
                            val config = entry.value

                            Card(
                                colors = CardDefaults.cardColors(containerColor = SpaceBlack.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .width(170.dp)
                                    .fillMaxHeight()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = config.name,
                                            color = NeonMint,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NeonMint.copy(alpha = 0.15f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("POST-FADER", color = NeonMint, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Column {
                                        Text("NÍVEL ENVIO", color = SoftGrey, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        Slider(
                                            value = config.level,
                                            onValueChange = { viewModel.setAuxLevel(auxId, it) },
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = NeonMint,
                                                thumbColor = NeonMint
                                            ),
                                            valueRange = 0f..1f,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("DELAY", color = SoftGrey, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            Text("${config.delayMs}ms", color = NeonMint, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Slider(
                                            value = config.delayMs.toFloat(),
                                            onValueChange = { viewModel.setAuxDelay(auxId, it.toInt()) },
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = NeonMint,
                                                thumbColor = NeonMint
                                            ),
                                            valueRange = 0f..500f,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.toggleAuxMute(auxId) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (config.mute) GlowingError else SpaceBlack
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(32.dp)
                                    ) {
                                        Text("MUTE AUXILIAR", color = if (config.mute) SpaceBlack else GlowingError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // PRESETS DE VOZ IA (IMAGE 6)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate),
                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().testTag("ia_presets_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Presets",
                                tint = NeonPurple
                            )
                        }
                        Column {
                            Text(
                                text = "Presets de Voz IA",
                                color = BrightWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Modelos de processamento baseados em timbres reais",
                                color = SoftGrey,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("APLICAR AO CANAL:", color = SoftGrey, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                        // Dropdown selection input
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SpaceBlack)
                                .clickable { /* simple mock rotation */ }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Canal ${String.format("%02d", selectedVoicePresetCh)}", color = BrightWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "dropdown", tint = GlowCyan, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4 IA Model preset cards
                    val presets = listOf(
                        Triple("baritone", "Voz Masculina (Barítono)", "Otimizado para clareza em frequências graves. Aplica corte em 120Hz e brilho em 3kHz."),
                        Triple("soprano", "Voz Feminina (Soprano)", "Controle de sibilância (De-Esser) e \"ar\" (High Shelf) acima de 12kHz."),
                        Triple("speech", "Pregador / Fala", "Compressão dinâmica agressiva para manter a inteligibilidade mesmo em gritos ou sussurros."),
                        Triple("clean", "Smart Clean (IA)", "Faxina inteligente: remove ruído de fundo, sibilância e embola os médios para uma voz cristalina.")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        presets.forEach { pr ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SpaceBlack.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, CardLine.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = pr.second,
                                            color = BrightWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when (pr.first) {
                                                        "baritone" -> NeonPurple.copy(alpha = 0.15f)
                                                        "soprano" -> GlowCyan.copy(alpha = 0.15f)
                                                        "speech" -> NeonMint.copy(alpha = 0.15f)
                                                        else -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                    }
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = when (pr.first) {
                                                    "baritone" -> "HPF 120Hz"
                                                    "soprano" -> "HPF 150Hz"
                                                    "speech" -> "IA ACTIVE"
                                                    else -> "TOTAL CLEAN"
                                                },
                                                color = when (pr.first) {
                                                    "baritone" -> NeonPurple
                                                    "soprano" -> GlowCyan
                                                    "speech" -> NeonMint
                                                    else -> Color(0xFF10B981)
                                                },
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = pr.third,
                                        color = SoftGrey,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = { viewModel.applyVoicePreset(pr.first, selectedVoicePresetCh) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (pr.first == "clean") Color(0xFF10B981) else Color(0xFFE11D48)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (pr.first == "clean") "LIMPEZA INTELIGENTE" else "APLICAR AO CANAL",
                                            color = SpaceBlack,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatAiScreen(viewModel: SoundMasterViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var userText by remember { mutableStateOf("") }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HEADER
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSlate),
            border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NeonPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = "Chat",
                        tint = NeonPurple
                    )
                }
                Column {
                    Text(
                        text = "Assistente IA Acústica",
                        color = BrightWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Análise espectral e calibração de ambiente em tempo real",
                        color = SoftGrey,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ACOUSTICS TOOL STRIP (IMAGE 2 ALIGNMENT)
        Card(
            colors = CardDefaults.cardColors(containerColor = SpaceBlack.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, CardLine.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "AÇÕES DIRETAS DE AJUSTE / DIAGNÓSTICO:",
                    color = SoftGrey,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )

                val quickTools = listOf(
                    Triple("Calcular RT60", Icons.Default.CompassCalibration, "Calcular decaimento RT60 do templo em tempo real"),
                    Triple("Bloquear Microfonia", Icons.Default.Security, "Como configurar o Supressor de Microfonia AFS2?"),
                    Triple("Equalizar Anti-Lama", Icons.Default.FilterAlt, "Como tratar o embolamento grave (lama) das vozes?"),
                    Triple("Alinhamento Igreja", Icons.Default.AutoGraph, "Como alinhar a resposta espectral da igreja no equalizador?")
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickTools) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSlate),
                            border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .clickable { viewModel.sendChatMessage(item.third) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = item.second,
                                    contentDescription = item.first,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = item.first,
                                    color = BrightWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // BUBBLES SCROLL AREA
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(messages) { msg ->
                    val isUser = msg.sender == "USER"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) Color(0xFF1E293B) else DarkSlate
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isUser) GlowCyan.copy(alpha = 0.4f) else CardLine.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = msg.text,
                                    color = if (isUser) GlowCyan else BrightWhite,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )

                                if (msg.commandDesc != null && msg.command != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { viewModel.applyAiSuggestedCommand(msg.commandDesc) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = msg.commandDesc,
                                            color = SpaceBlack,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SUGGESTIONS LIST ROW
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "SUGESTÕES DE DIAGNÓSTICO ACÚSTICO:",
                color = SoftGrey,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val sugList = listOf(
                    "Como resolver microfonia?",
                    "Tratar sobra de graves (lama)",
                    "Otimizar inteligibilidade pregação",
                    "Como tratar agudos ásperos vivos?"
                )
                items(sugList) { sug ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSlate)
                            .border(1.dp, CardLine.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .clickable { viewModel.sendChatMessage(sug) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(sug, color = GlowCyan, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // TEXT INPUT BAR WITH ACCENT
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = userText,
                    onValueChange = { userText = it },
                    placeholder = { Text("Digitar pergunta acústica...", color = SoftGrey.copy(alpha = 0.5f), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = CardLine.copy(alpha = 0.3f),
                        focusedTextColor = BrightWhite,
                        unfocusedTextColor = BrightWhite
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (userText.trim().isNotEmpty()) {
                                viewModel.sendChatMessage(userText)
                                userText = ""
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    modifier = Modifier.weight(1f).height(56.dp)
                )

                IconButton(
                    onClick = {
                        if (userText.trim().isNotEmpty()) {
                            viewModel.sendChatMessage(userText)
                            userText = ""
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonPurple)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = SpaceBlack,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: SoundMasterViewModel) {
    val consoleLogs by viewModel.consoleLogs.collectAsStateWithLifecycle()
    val apiUrl by viewModel.apiUrl.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var textUrl by remember { mutableStateOf(apiUrl) }

    LaunchedEffect(consoleLogs.size) {
        if (consoleLogs.isNotEmpty()) {
            listState.animateScrollToItem(consoleLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP TITLE LOG CONSOLE
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSlate),
            border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Terminal",
                    tint = GlowCyan,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Terminal Técnico",
                        color = BrightWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Logs de telemetria de som em tempo real",
                        color = SoftGrey,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // LOG PRINT BOX BODY
        Card(
            colors = CardDefaults.cardColors(containerColor = SpaceBlack),
            border = BorderStroke(1.dp, CardLine.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(consoleLogs) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("Erro") || log.contains("PANIC") || log.contains("MUTE ALL")) GlowingError else GlowCyan,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // BACKEND API ENDPOINT EDIT
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSlate),
            border = BorderStroke(1.dp, CardLine.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "CONFIGURAÇÕES DE COMUNICADOR SERVER IA",
                    color = SoftGrey,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textUrl,
                        onValueChange = { textUrl = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlowCyan,
                            unfocusedBorderColor = CardLine.copy(alpha = 0.2f),
                            focusedTextColor = BrightWhite,
                            unfocusedTextColor = BrightWhite
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = { viewModel.setApiUrl(textUrl) },
                        colors = ButtonDefaults.buttonColors(containerColor = GlowCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SALVAR", color = SpaceBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
