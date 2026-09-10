package com.example.ui.components

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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CommandResultEntity
import com.example.executor.ExecutionState
import com.example.executor.LogType
import com.example.executor.TerminalLog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UI screen with a dedicated text field for entering shell commands,
 * an execute action button that triggers the ProcessBuilder execution service,
 * and a real-time output terminal viewer with Room execution history tabs.
 */
@Composable
fun ShellCommandExecutorScreen(
    logs: List<TerminalLog>,
    commandHistory: List<CommandResultEntity>,
    executionState: ExecutionState,
    isExecuting: Boolean,
    onExecuteCommand: (String) -> Unit,
    onKillProcess: () -> Unit,
    onClearLogs: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commandInput by remember { mutableStateOf("") }
    var selectedViewMode by remember { mutableIntStateOf(0) } // 0: Live Terminal Output, 1: Room History Log
    val terminalListState = rememberLazyListState()
    val historyListState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    var copiedFeedback by remember { mutableStateOf<String?>(null) }

    // Clear copy feedback badge after a short delay
    LaunchedEffect(copiedFeedback) {
        if (copiedFeedback != null) {
            kotlinx.coroutines.delay(2000)
            copiedFeedback = null
        }
    }

    // Auto-scroll to bottom when new logs arrive in real-time
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            terminalListState.animateScrollToItem(logs.size - 1)
        }
    }

    val sampleCommands = listOf(
        "uname -a",
        "whoami",
        "pwd",
        "ls -la",
        "date",
        "cat /proc/version",
        "df -h"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(14.dp)
            .testTag("shell_command_executor_screen")
    ) {
        // 1. Header with Execution Service Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isExecuting || executionState is ExecutionState.Running -> NeonGreen
                                    else -> CyberCyan
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ProcessBuilder Shell Service",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isExecuting || executionState is ExecutionState.Running) "Executing command in background..." else "Ready for input (Room persistent)",
                            fontSize = 10.sp,
                            color = if (isExecuting) NeonGreen else TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExecuting || executionState is ExecutionState.Running) {
                        IconButton(
                            onClick = onKillProcess,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("kill_command_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Kill Process", tint = TerminalRed)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (selectedViewMode == 0) onClearLogs() else onClearHistory()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("clear_output_button")
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = if (selectedViewMode == 0) "Clear Output" else "Clear History",
                                tint = TextMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Command Input Section with Execute Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "COMMAND INPUT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shell_command_text_field"),
                        placeholder = {
                            Text(
                                "Enter shell command (e.g., uname -a, ls -la)...",
                                fontSize = 12.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CyberCyan,
                            focusedContainerColor = CyberSurface,
                            unfocusedContainerColor = CyberSurface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Execute Button (Triggers execution service)
                    Button(
                        onClick = {
                            val cmd = commandInput.trim()
                            if (cmd.isNotBlank()) {
                                onExecuteCommand(cmd)
                            }
                        },
                        enabled = commandInput.isNotBlank() && !isExecuting && executionState !is ExecutionState.Running,
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("trigger_execution_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = CyberBlack,
                            disabledContainerColor = CyberSurfaceVariant,
                            disabledContentColor = TextMuted
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        if (isExecuting || executionState is ExecutionState.Running) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CyberBlack,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Execute",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Run",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Command Presets
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sampleCommands) { cmd ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CyberSurface,
                            modifier = Modifier
                                .border(0.5.dp, CyberBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    commandInput = cmd
                                }
                        ) {
                            Text(
                                text = cmd,
                                fontSize = 11.sp,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. View Switcher (Real-time Console Output vs Room Database History)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CyberSurface)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selectedViewMode == 0) CyberCyan.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { selectedViewMode = 0 }
                    .testTag("tab_realtime_output")
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Real-Time Output (${logs.size})",
                    fontSize = 11.sp,
                    fontWeight = if (selectedViewMode == 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedViewMode == 0) CyberCyan else TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selectedViewMode == 1) NeonGreen.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { selectedViewMode = 1 }
                    .testTag("tab_room_db_log")
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Room DB Log (${commandHistory.size})",
                    fontSize = 11.sp,
                    fontWeight = if (selectedViewMode == 1) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedViewMode == 1) NeonGreen else TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Output Display Area
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                .testTag("realtime_output_container"),
            colors = CardDefaults.cardColors(containerColor = CyberDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            // Header bar next to terminal output display with Copy and status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (selectedViewMode == 0) "TERMINAL OUTPUT" else "EXECUTION RESULTS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    if (copiedFeedback != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonGreen.copy(alpha = 0.2f),
                            modifier = Modifier.border(0.5.dp, NeonGreen, RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = copiedFeedback ?: "",
                                fontSize = 9.sp,
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Copy Output Button next to terminal display
                FilledTonalButton(
                    onClick = {
                        val textToCopy = if (selectedViewMode == 0) {
                            logs.joinToString("\n") { it.text }
                        } else {
                            commandHistory.joinToString("\n---\n") { item ->
                                "Command: ${item.command}\nStatus: ${item.status} (exit ${item.exitCode})\nDuration: ${item.durationMs}ms\nStdout:\n${item.stdout}\nStderr:\n${item.stderr}"
                            }
                        }
                        if (textToCopy.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            copiedFeedback = "Copied to clipboard!"
                        } else {
                            copiedFeedback = "Nothing to copy"
                        }
                    },
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("copy_terminal_output_button"),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CyberCyan.copy(alpha = 0.15f),
                        contentColor = CyberCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Output",
                        modifier = Modifier.size(14.dp)
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

            if (selectedViewMode == 0) {
                // Real-time Console Stream
                SelectionContainer {
                    LazyColumn(
                        state = terminalListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (logs.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No command executed yet.",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Enter a command above and tap 'Run' to view real-time output.",
                                        color = TextMuted.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
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
            } else {
                // Room Database Output History
                LazyColumn(
                    state = historyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (commandHistory.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No saved command outputs in Room database.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        items(commandHistory) { item ->
                            HistoryItemCard(item = item, onReExecute = { onExecuteCommand(it) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: CommandResultEntity,
    onReExecute: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, CyberBorder, RoundedCornerShape(8.dp))
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (item.isSuccess) NeonGreen else TerminalRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.command,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.durationMs}ms | $formattedTime",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            val details = "Command: ${item.command}\nStatus: ${item.status}\nExit code: ${item.exitCode}\nStdout: ${item.stdout}\nStderr: ${item.stderr}"
                            clipboardManager.setText(AnnotatedString(details))
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("copy_history_item_button")
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Result",
                            tint = CyberCyan,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onReExecute(item.command) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Re-run",
                            tint = CyberCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = CyberBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(6.dp))

                SelectionContainer {
                    Column {
                        if (item.stdout.isNotBlank()) {
                            Text(
                                text = "STDOUT:",
                                fontSize = 10.sp,
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = item.stdout,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (item.stderr.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "STDERR (exit code ${item.exitCode}):",
                                fontSize = 10.sp,
                                color = TerminalRed,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = item.stderr,
                                fontSize = 11.sp,
                                color = TerminalRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
