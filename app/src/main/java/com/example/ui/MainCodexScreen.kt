package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.DeveloperDiagnosticsDialog
import com.example.ui.components.ModernAiChatScreen
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.CyberDark
import com.example.viewmodel.CodexViewModel

@Composable
fun MainCodexScreen(
    viewModel: CodexViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val jarvisMessages by viewModel.jarvisMessages.collectAsStateWithLifecycle()
    val isThinking by viewModel.isJarvisThinking.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
    val isAutoExecute by viewModel.isAutoExecuteOnVoice.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val targetEnv by viewModel.selectedTargetEnv.collectAsStateWithLifecycle()
    val voiceLanguage by viewModel.voiceLanguage.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
    val showDevConsole by viewModel.showDevConsole.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    val workspaceFiles by viewModel.workspaceFiles.collectAsStateWithLifecycle()

    // Permission launcher for microphone audio
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceListening()
        } else {
            viewModel.showSnackbar("Microphone permission required for voice input.")
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main ChatGPT-style Single Conversation Interface
            ModernAiChatScreen(
                messages = jarvisMessages,
                isThinking = isThinking,
                voiceState = voiceState,
                isTtsEnabled = isTtsEnabled,
                isTermuxInstalled = viewModel.isTermuxInstalled,
                isKaliInstalled = viewModel.isKaliInstalled,
                onSendMessage = { text -> viewModel.sendJarvisMessage(text) },
                onStartVoice = {
                    val hasAudioPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasAudioPermission) {
                        viewModel.startVoiceListening()
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopVoice = { viewModel.stopVoiceListening() },
                onSpeak = { text -> viewModel.speakJarvisText(text) },
                onExecuteTask = { messageId, cmd ->
                    viewModel.executeTaskCommandInBackground(messageId, cmd)
                },
                onClearChat = { viewModel.clearChatMessages() },
                onToggleTts = { viewModel.toggleTts() },
                onOpenSettings = { viewModel.setShowSettingsDialog(true) },
                onOpenDevConsole = { viewModel.setShowDevConsole(true) },
                onDismissVoiceError = { viewModel.resetVoiceState() }
            )

            // Settings Dialog (API Key, Language, Target Env)
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
                        viewModel.showSnackbar("Settings saved.")
                    },
                    onDismiss = { viewModel.setShowSettingsDialog(false) }
                )
            }

            // Developer Diagnostics Modal (Keeps complexity internal, but accessible for debugging)
            if (showDevConsole) {
                DeveloperDiagnosticsDialog(
                    logs = terminalLogs,
                    isTermuxInstalled = viewModel.isTermuxInstalled,
                    isKaliInstalled = viewModel.isKaliInstalled,
                    workspaceFilesCount = workspaceFiles.size,
                    onDismiss = { viewModel.setShowDevConsole(false) },
                    onClearLogs = { viewModel.clearTerminal() }
                )
            }
        }
    }
}
