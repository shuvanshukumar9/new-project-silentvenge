package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.executor.ExecutionState
import com.example.executor.LogType
import com.example.executor.TerminalLog
import com.example.ui.theme.*

@Composable
fun SilentVengeTerminalScreen(
    logs: List<TerminalLog>,
    executionState: ExecutionState,
    isTermuxInstalled: Boolean,
    isKaliInstalled: Boolean,
    onExecuteCommand: (String) -> Unit,
    onDispatchToTermux: (String) -> Unit,
    onKillProcess: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cmdInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    var copiedFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(copiedFeedback) {
        if (copiedFeedback) {
            kotlinx.coroutines.delay(2000)
            copiedFeedback = false
        }
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val quickCommands = listOf(
        "ls -la",
        "python3 --version",
        "uname -a",
        "ps -ef",
        "python3 -m unittest",
        "df -h"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(12.dp)
            .testTag("terminal_screen_root")
    ) {
        // 1. Termux & Kali Bridge Status Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Termux status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isTermuxInstalled) NeonGreen else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTermuxInstalled) "Termux Bridge Ready" else "Termux Uninstalled",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isTermuxInstalled) NeonGreen else TextMuted
                    )
                }

                // Kali status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isKaliInstalled) CyberCyan else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isKaliInstalled) "Kali NetHunter Active" else "Kali Off",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isKaliInstalled) CyberCyan else TextMuted
                    )
                }

                // Kill switch
                if (executionState is ExecutionState.Running) {
                    IconButton(
                        onClick = onKillProcess,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("kill_process_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Kill Process", tint = TerminalRed)
                    }
                } else {
                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Terminal", tint = TextMuted)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Commands Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(quickCommands) { cmd ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyberSurface,
                    modifier = Modifier.border(0.5.dp, CyberBorder, RoundedCornerShape(6.dp))
                ) {
                    TextButton(
                        onClick = {
                            cmdInput = cmd
                            onExecuteCommand(cmd)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(cmd, fontSize = 11.sp, color = CyberCyan, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Terminal Log Window
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDark),
            shape = RoundedCornerShape(10.dp)
        ) {
            // Header bar next to terminal output display with Copy and status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CONSOLE OUTPUT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    if (copiedFeedback) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COPIED!",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                FilledTonalButton(
                    onClick = {
                        val text = logs.joinToString("\n") { it.text }
                        if (text.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(text))
                            copiedFeedback = true
                        }
                    },
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("copy_terminal_output_button"),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CyberCyan.copy(alpha = 0.15f),
                        contentColor = CyberCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Output",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            HorizontalDivider(color = CyberBorder, thickness = 0.5.dp)

            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                text = "SilentVenge Controlled Terminal Bridge Ready.\nEnter commands below or dispatch to Termux.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        items(logs) { log ->
                            val color = when (log.type) {
                                LogType.COMMAND -> CyberCyan
                                LogType.STDOUT -> TextPrimary
                                LogType.STDERR -> TerminalRed
                                LogType.SUCCESS -> NeonGreen
                                LogType.ERROR -> TerminalRed
                                LogType.SYSTEM -> TextMuted
                            }
                            Text(
                                text = log.text,
                                color = color,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Command Input & Dispatch Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = cmdInput,
                onValueChange = { cmdInput = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_command_input"),
                placeholder = { Text("$ command (e.g. pytest)", fontSize = 11.sp, color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyberCyan
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Run in Local Sandbox
            IconButton(
                onClick = {
                    if (cmdInput.isNotBlank()) {
                        onExecuteCommand(cmdInput)
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberCyan)
                    .testTag("terminal_run_local_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Run Local", tint = CyberBlack)
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Dispatch to Termux RUN_COMMAND
            IconButton(
                onClick = {
                    if (cmdInput.isNotBlank()) {
                        onDispatchToTermux(cmdInput)
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberSurfaceVariant)
                    .testTag("terminal_dispatch_termux_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = "Dispatch to Termux", tint = NeonGreen)
            }
        }
    }
}
