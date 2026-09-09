package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.executor.ExecutionState
import com.example.ui.components.*
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CodexTab
import com.example.viewmodel.CodexViewModel
import kotlinx.coroutines.launch

@Composable
fun MainCodexScreen(
    viewModel: CodexViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val promptInput by viewModel.promptInput.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val currentScript by viewModel.currentScript.collectAsStateWithLifecycle()
    val isAutoExecute by viewModel.isAutoExecuteOnVoice.collectAsStateWithLifecycle()
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
    val voiceLanguage by viewModel.voiceLanguage.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val executionState by viewModel.executionState.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    val terminalInput by viewModel.terminalInput.collectAsStateWithLifecycle()
    val savedScripts by viewModel.savedScripts.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val targetEnv by viewModel.selectedTargetEnv.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    // SilentVenge Agent States
    val currentAgentTask by viewModel.currentAgentTask.collectAsStateWithLifecycle()
    val agentTaskState by viewModel.agentTaskState.collectAsStateWithLifecycle()
    val pendingSecurityAction by viewModel.pendingSecurityAction.collectAsStateWithLifecycle()
    val currentSteps by viewModel.currentSteps.collectAsStateWithLifecycle()
    val modelConfig by viewModel.modelConfig.collectAsStateWithLifecycle()
    val tokenTelemetry by viewModel.tokenTelemetry.collectAsStateWithLifecycle()
    val bridgeLogs by viewModel.bridgeLogs.collectAsStateWithLifecycle()
    val activeShellProcess by viewModel.activeShellProcess.collectAsStateWithLifecycle()
    val pipelineQueue by viewModel.pipelineQueue.collectAsStateWithLifecycle()
    val serverDaemons by viewModel.serverDaemons.collectAsStateWithLifecycle()
    val workspaceFiles by viewModel.workspaceFiles.collectAsStateWithLifecycle()
    val projectInfo by viewModel.projectInfo.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val isTestingConnection by viewModel.isTestingConnection.collectAsStateWithLifecycle()

    // JARVIS Multi-Agent & Chat States
    val jarvisMessages by viewModel.jarvisMessages.collectAsStateWithLifecycle()
    val selectedJarvisAgent by viewModel.selectedJarvisAgent.collectAsStateWithLifecycle()
    val isJarvisThinking by viewModel.isJarvisThinking.collectAsStateWithLifecycle()

    // Bug Hunter States
    val bugDiagnosis by viewModel.bugDiagnosis.collectAsStateWithLifecycle()
    val isDiagnosingBug by viewModel.isDiagnosingBug.collectAsStateWithLifecycle()
    val logcatCaptured by viewModel.logcatCaptured.collectAsStateWithLifecycle()
    val isCapturingLogcat by viewModel.isCapturingLogcat.collectAsStateWithLifecycle()

    // File Inspector States
    val fileInspectionResult by viewModel.fileInspectionResult.collectAsStateWithLifecycle()
    val isInspectingFile by viewModel.isInspectingFile.collectAsStateWithLifecycle()

    // Device Audit States
    val deviceAuditReport by viewModel.deviceAuditReport.collectAsStateWithLifecycle()

    // Permission launcher for microphone
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceListening()
        } else {
            viewModel.showSnackbar("Microphone permission denied. Voice input is disabled.")
        }
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = CyberDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopBarHeader(
                targetEnv = targetEnv,
                isTermuxDetected = viewModel.isTermuxInstalled,
                isAutoExecute = isAutoExecute,
                isTtsEnabled = isTtsEnabled,
                onToggleAutoExecute = { viewModel.toggleAutoExecuteOnVoice() },
                onToggleTts = { viewModel.toggleTts() },
                onOpenSettings = { viewModel.setShowSettingsDialog(true) }
            )
        },
        bottomBar = {
            ScrollableTabRow(
                selectedTabIndex = activeTab.ordinal,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("codex_bottom_navigation"),
                containerColor = CyberDark,
                contentColor = TextPrimary,
                edgePadding = 6.dp,
                indicator = { tabPositions ->
                    if (activeTab.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                            color = when (activeTab) {
                                CodexTab.AGENT -> NeonGreen
                                CodexTab.PIPELINE -> CyberCyan
                                CodexTab.TERMINAL -> CyberCyan
                                CodexTab.TOOLS -> NeonGreen
                                CodexTab.ROUTER -> CyberCyan
                                CodexTab.ARCH -> NeonGreen
                                CodexTab.HISTORY -> CyberCyan
                            },
                            height = 2.5.dp
                        )
                    }
                },
                divider = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CyberBorder)
                    )
                }
            ) {
                CodexTab.entries.forEach { tab ->
                    val isSelected = activeTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.setTab(tab) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.icon, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) {
                                        when (tab) {
                                            CodexTab.AGENT -> NeonGreen
                                            CodexTab.PIPELINE -> CyberCyan
                                            CodexTab.TERMINAL -> CyberCyan
                                            CodexTab.TOOLS -> NeonGreen
                                            CodexTab.ROUTER -> CyberCyan
                                            CodexTab.ARCH -> NeonGreen
                                            CodexTab.HISTORY -> CyberCyan
                                        }
                                    } else TextMuted
                                )
                                if (tab == CodexTab.PIPELINE && (activeShellProcess != null || pipelineQueue.isNotEmpty())) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (activeShellProcess != null) NeonGreen else TerminalYellow)
                                    )
                                }
                                if (tab == CodexTab.TERMINAL && executionState is ExecutionState.Running) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(NeonGreen)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                CodexTab.AGENT -> {
                    SilentVengeAgentScreen(
                        currentTask = currentAgentTask,
                        taskState = agentTaskState,
                        pendingSecurityAction = pendingSecurityAction,
                        currentSteps = currentSteps,
                        modelConfig = modelConfig,
                        tokenTelemetry = tokenTelemetry,
                        onRunGoal = { viewModel.runAgentGoal(it) },
                        onConfirmSecurity = { viewModel.confirmSecurityAction(it) },
                        onStopTask = { viewModel.stopAgentTask() },
                        onNavigateToTerminal = { viewModel.setTab(CodexTab.TERMINAL) }
                    )
                }
                CodexTab.PIPELINE -> {
                    PipelineDashboardScreen(
                        currentTask = currentAgentTask,
                        taskState = agentTaskState,
                        activeProcess = activeShellProcess,
                        logs = if (bridgeLogs.isNotEmpty()) bridgeLogs else terminalLogs,
                        taskQueue = pipelineQueue,
                        currentSteps = currentSteps,
                        onEnqueueGoal = { viewModel.enqueueAgentGoal(it) },
                        onRemoveQueuedTask = { viewModel.removeQueuedTask(it) },
                        onClearQueue = { viewModel.clearTaskQueue() },
                        onKillActiveProcess = { viewModel.killTerminalProcess() },
                        onClearLogs = { viewModel.clearTerminalLogs() },
                        onNavigateToTerminal = { viewModel.setTab(CodexTab.TERMINAL) }
                    )
                }
                CodexTab.TERMINAL -> {
                    SilentVengeTerminalScreen(
                        logs = if (bridgeLogs.isNotEmpty()) bridgeLogs else terminalLogs,
                        executionState = executionState,
                        isTermuxInstalled = viewModel.isTermuxInstalled,
                        isKaliInstalled = viewModel.isKaliInstalled,
                        onExecuteCommand = { viewModel.executeTerminalCommand(it) },
                        onDispatchToTermux = { viewModel.dispatchToTermux(it) },
                        onKillProcess = { viewModel.killTerminalProcess() },
                        onClearLogs = { viewModel.clearTerminalLogs() }
                    )
                }
                CodexTab.TOOLS -> {
                    SilentVengeToolsScreen(
                        files = workspaceFiles,
                        projectInfo = projectInfo,
                        serverDaemons = serverDaemons,
                        onRefreshFiles = { viewModel.refreshWorkspaceFiles() },
                        onCreateFile = { name, content -> viewModel.createWorkspaceFile(name, content) },
                        onDeleteFile = { viewModel.deleteWorkspaceFile(it) },
                        onSearchCode = { viewModel.searchCodeInWorkspace(it) },
                        searchResults = searchResults,
                        onStartServer = { name, cmd, port -> viewModel.startServerDaemon(name, cmd, port) },
                        onStopServer = { viewModel.stopServerDaemon(it) },
                        onNotify = { viewModel.showSnackbar(it) }
                    )
                }
                CodexTab.ROUTER -> {
                    SilentVengeModelRouterScreen(
                        config = modelConfig,
                        telemetry = tokenTelemetry,
                        onUpdateConfig = { viewModel.updateModelRouteConfig(it) },
                        onTestConnection = { provider, endpoint -> viewModel.testModelConnection(provider, endpoint) },
                        connectionStatus = connectionStatus,
                        isTestingConnection = isTestingConnection
                    )
                }
                CodexTab.ARCH -> {
                    SilentVengeArchitectureScreen()
                }
                CodexTab.HISTORY -> {
                    HistoryScreen(
                        scripts = savedScripts,
                        onSelectScript = { viewModel.loadSavedScript(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDeleteScript = { viewModel.deleteSavedScript(it) }
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            initialApiKey = customApiKey,
            targetEnv = targetEnv,
            isAutoExecute = isAutoExecute,
            isTtsEnabled = isTtsEnabled,
            voiceLanguage = voiceLanguage,
            onVoiceLanguageChange = { viewModel.setVoiceLanguage(it) },
            onSave = { apiKey, env, autoExec, tts ->
                viewModel.setCustomApiKey(apiKey)
                viewModel.setTargetEnv(env)
                if (autoExec != isAutoExecute) viewModel.toggleAutoExecuteOnVoice()
                if (tts != isTtsEnabled) viewModel.toggleTts()
                viewModel.setShowSettingsDialog(false)
                viewModel.showSnackbar("Settings updated successfully.")
            },
            onDismiss = { viewModel.setShowSettingsDialog(false) }
        )
    }
}

@Composable
fun TopBarHeader(
    targetEnv: String,
    isTermuxDetected: Boolean,
    isAutoExecute: Boolean,
    isTtsEnabled: Boolean,
    onToggleAutoExecute: () -> Unit,
    onToggleTts: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberDark)
            .border(1.dp, CyberBorder)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo & Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyberSurface)
                    .border(1.dp, NeonGreen, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = ">_",
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "SILENTVENGE",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isTermuxDetected) NeonGreen else CyberCyan)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isTermuxDetected) "Termux Bridge Ready" else "POSIX Sandbox",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }
            }
        }

        // Action Toggles
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Auto Execute toggle chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isAutoExecute) NeonGreen.copy(alpha = 0.2f) else CyberSurface)
                    .border(
                        1.dp,
                        if (isAutoExecute) NeonGreen else CyberBorder,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onToggleAutoExecute() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = if (isAutoExecute) NeonGreen else TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAutoExecute) "AUTO-RUN" else "MANUAL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isAutoExecute) NeonGreen else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // TTS Readout Toggle
            IconButton(
                onClick = onToggleTts,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Toggle Voice Readout",
                    tint = if (isTtsEnabled) CyberCyan else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Settings Button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
