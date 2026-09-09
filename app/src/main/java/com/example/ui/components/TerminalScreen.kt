package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.executor.ExecutionState
import com.example.executor.LogType
import com.example.executor.TerminalLog
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalText
import com.example.ui.theme.TerminalYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TerminalScreen(
    logs: List<TerminalLog>,
    executionState: ExecutionState,
    terminalInput: String,
    onTerminalInputChange: (String) -> Unit,
    onSendCommand: (String) -> Unit,
    onKillProcess: () -> Unit,
    onClearLogs: () -> Unit,
    onRerunScript: () -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val quickCommands = listOf(
        "uname -a",
        "df -h",
        "whoami",
        "pwd",
        "ls -la",
        "cat /proc/cpuinfo | grep 'model name' | head -n 1",
        "echo ${'$'}PATH"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBg)
    ) {
        // Terminal Status Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberDark)
                .border(width = 1.dp, color = CyberBorder)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (executionState) {
                    is ExecutionState.Running -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = NeonGreen
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RUNNING...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NeonGreen
                        )
                    }
                    is ExecutionState.Completed -> {
                        val isSuccess = executionState.exitCode == 0
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isSuccess) NeonGreen else TerminalRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EXIT ${executionState.exitCode} (${executionState.durationMs}ms)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSuccess) NeonGreen else TerminalRed
                        )
                    }
                    is ExecutionState.Failed -> {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TerminalRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FAILED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TerminalRed
                        )
                    }
                    ExecutionState.Idle -> {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TextMuted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "IDLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                }
            }

            // Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (executionState is ExecutionState.Running) {
                    IconButton(
                        onClick = onKillProcess,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("kill_process_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Kill Process",
                            tint = TerminalRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onRerunScript,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("rerun_script_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Re-run Script",
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val text = logs.joinToString("\n") { it.text }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Logs", text))
                        onNotify("Console logs copied to clipboard!")
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Console",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear Logs",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Terminal Console Log Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Terminal console ready.\nRun a script or type a shell command below.",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("terminal_logs_list"),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        TerminalLogItem(log)
                    }
                }
            }
        }

        // Quick Shell Command Suggestions
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberDark)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickCommands) { cmd ->
                Text(
                    text = cmd,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberSurfaceVariant)
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                        .clickable {
                            onTerminalInputChange(cmd)
                            onSendCommand(cmd)
                        }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        // Direct Command Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberDark)
                .border(1.dp, CyberBorder)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "android:~$ ",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NeonGreen,
                fontSize = 12.sp
            )

            OutlinedTextField(
                value = terminalInput,
                onValueChange = onTerminalInputChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_command_input"),
                placeholder = {
                    Text(
                        "type shell command...",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    cursorColor = CyberCyan
                ),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendCommand(terminalInput) })
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = { onSendCommand(terminalInput) },
                enabled = terminalInput.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (terminalInput.isNotBlank()) NeonGreen else CyberSurface)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Command",
                    tint = if (terminalInput.isNotBlank()) Color.Black else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TerminalLogItem(log: TerminalLog) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(log.timestamp))

    val textColor = when (log.type) {
        LogType.SYSTEM -> CyberCyan
        LogType.COMMAND -> NeonGreen
        LogType.STDOUT -> TerminalText
        LogType.STDERR -> TerminalRed
        LogType.SUCCESS -> NeonGreen
        LogType.ERROR -> TerminalRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = formattedTime,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = TextMuted,
            modifier = Modifier.width(52.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = log.text,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = textColor,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
