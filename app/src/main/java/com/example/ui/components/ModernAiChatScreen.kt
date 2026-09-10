package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.JarvisChatMessage
import com.example.ui.theme.CyberBackground
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
import com.example.voice.VoiceState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAiChatScreen(
    messages: List<JarvisChatMessage>,
    isThinking: Boolean,
    voiceState: VoiceState,
    isTtsEnabled: Boolean,
    onSendMessage: (String) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onSpeak: (String) -> Unit,
    onExecuteTask: (messageId: String, command: String) -> Unit,
    onClearChat: () -> Unit,
    onToggleTts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDevConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    // Auto-scroll on new message
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isListening = voiceState is VoiceState.Listening

    // Infinite breathing animation for voice listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .imePadding()
    ) {
        // Minimalist ChatGPT Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberDark)
                .border(1.dp, CyberBorder.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title & Online Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CyberSurface)
                        .border(1.dp, NeonGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡",
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Codex AI",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isThinking) TerminalYellow else NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isThinking) "Working..." else "Ready",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Top Actions: New Chat, TTS, Menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("action_new_chat")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onToggleTts,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("action_toggle_tts")
                ) {
                    Icon(
                        imageVector = if (isTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Toggle Voice Readout",
                        tint = if (isTtsEnabled) CyberCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("action_overflow_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(CyberSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings & API Key", color = TextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                onOpenSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Developer Diagnostics", color = CyberCyan, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                onOpenDevConsole()
                            }
                        )
                    }
                }
            }
        }

        // Conversation Stream
        MessageList(
            messages = messages,
            isThinking = isThinking,
            listState = listState,
            onSendMessage = onSendMessage,
            onSpeak = onSpeak,
            onCopy = { clipboardManager.setText(AnnotatedString(it)) },
            onExecuteTask = onExecuteTask,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        // Voice Listening Indicator Banner
        if (isListening) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalRed.copy(alpha = 0.15f))
                    .border(1.dp, TerminalRed.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(TerminalRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Listening... speak in Hindi or English",
                        fontSize = 12.sp,
                        color = TerminalRed,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onStopVoice,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop listening",
                        tint = TerminalRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Bottom Input Container (ChatGPT Pill Style)
        InputField(
            inputText = inputText,
            onInputTextChange = { inputText = it },
            isListening = isListening,
            isThinking = isThinking,
            onSendMessage = {
                onSendMessage(it)
                inputText = ""
            },
            onStartVoice = onStartVoice,
            onStopVoice = onStopVoice
        )
    }
}

/**
 * MessageList displays the chronological thread of conversation messages,
 * suggestion prompts when empty, and progress indicators during reasoning.
 */
@Composable
fun MessageList(
    messages: List<JarvisChatMessage>,
    isThinking: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSendMessage: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit,
    onExecuteTask: (messageId: String, command: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (messages.isEmpty() || (messages.size == 1 && !messages[0].isUser)) {
            // Welcoming ChatGPT-Style Greeting & Starter Prompts
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceVariant)
                        .border(1.5.dp, CyberCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🤖", fontSize = 26.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "How can I help you today?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Ask questions, write scripts, or execute background tasks effortlessly.",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Minimal Suggestion Pills
                val suggestions = listOf(
                    "⚡ Check device health & RAM" to "Check device health, RAM, and storage",
                    "📁 List workspace files" to "List all files in current workspace",
                    "🐞 Scan for system errors" to "Scan device logcat for errors and diagnose them",
                    "🌐 Ping network connectivity" to "ping google.com -c 2"
                )

                suggestions.forEach { (label, prompt) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurface)
                            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                            .clickable { onSendMessage(prompt) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageItem(
                        message = msg,
                        onSpeak = { onSpeak(msg.message) },
                        onCopy = { onCopy(it) },
                        onExecuteTask = { onExecuteTask(msg.id, it) }
                    )
                }

                if (isThinking) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CyberCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "AI is thinking...",
                                fontSize = 12.sp,
                                color = CyberCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * InputField provides text and voice prompt inputs that delegate intent
 * to the AIOrchestrator, with active loading/disabled states while tasks run.
 */
@Composable
fun InputField(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isListening: Boolean,
    isThinking: Boolean,
    onSendMessage: (String) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberDark)
            .border(1.dp, CyberBorder.copy(alpha = 0.5f))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Voice Input Button
        IconButton(
            onClick = {
                if (isListening) onStopVoice() else onStartVoice()
            },
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isListening) TerminalRed.copy(alpha = 0.2f) else CyberSurfaceVariant)
                .border(
                    1.dp,
                    if (isListening) TerminalRed else CyberBorder,
                    CircleShape
                )
                .testTag("action_voice_input")
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Input",
                tint = if (isListening) TerminalRed else CyberCyan,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Main Message Input
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputTextChange,
            modifier = Modifier
                .weight(1f)
                .testTag("chat_message_input"),
            placeholder = {
                Text(
                    if (isThinking) "AI is busy processing..." else "Message Codex AI...",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            },
            maxLines = 4,
            enabled = !isThinking,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                color = if (isThinking) TextMuted else TextPrimary
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = CyberBorder,
                cursorColor = CyberCyan
            ),
            shape = RoundedCornerShape(22.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Send Button
        IconButton(
            onClick = {
                if (inputText.isNotBlank()) {
                    onSendMessage(inputText.trim())
                }
            },
            enabled = inputText.isNotBlank() && !isThinking,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (inputText.isNotBlank() && !isThinking) CyberCyan else CyberSurfaceVariant)
                .testTag("action_send_message")
        ) {
            if (isThinking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = CyberCyan,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank() && !isThinking) Color.Black else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: JarvisChatMessage,
    onSpeak: () -> Unit,
    onCopy: (String) -> Unit,
    onExecuteTask: (String) -> Unit
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    if (message.isUser) {
        // User Message (Right Aligned, Elegant Tint)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                    .background(CyberSurfaceVariant)
                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = message.message,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedTime,
                    fontSize = 9.sp,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    } else {
        // AI Assistant Message (Left Aligned, Clear Distinction)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            // Assistant Avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CyberSurfaceVariant)
                    .border(1.dp, NeonGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🤖",
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp))
                    .padding(14.dp)
            ) {
                // Header: Agent Name & Task Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Codex AI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )

                    if (!message.taskStatus.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        message.isRunning -> TerminalYellow.copy(alpha = 0.15f)
                                        message.isError -> TerminalRed.copy(alpha = 0.15f)
                                        else -> NeonGreen.copy(alpha = 0.15f)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = message.taskStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    message.isRunning -> TerminalYellow
                                    message.isError -> TerminalRed
                                    else -> NeonGreen
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Message Text
                SelectionContainer {
                    Text(
                        text = message.message,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }

                // If message has generated code block
                if (!message.codeSnippet.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF070B14))
                            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (message.codeLanguage ?: "Code").uppercase(),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { onCopy(message.codeSnippet) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        SelectionContainer {
                            Text(
                                text = message.codeSnippet,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // If task generated command output (Internal Execution Result)
                if (!message.commandOutput.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF05080E))
                            .border(1.dp, if (message.isError) TerminalRed.copy(alpha = 0.5f) else NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (message.isError) "OUTPUT (FAILED)" else "TASK OUTPUT",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (message.isError) TerminalRed else NeonGreen
                                )
                                if (message.executionDurationMs > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${message.executionDurationMs}ms",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onCopy(message.commandOutput) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy output",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        SelectionContainer {
                            Text(
                                text = message.commandOutput,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (message.isError) TerminalRed else TextPrimary
                            )
                        }
                    }
                }

                // If there's a runnable command that hasn't been executed yet
                if (!message.runCommand.isNullOrBlank() && message.commandOutput.isNullOrBlank() && !message.isRunning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonGreen.copy(alpha = 0.15f))
                            .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable { onExecuteTask(message.runCommand) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Execute task",
                            tint = NeonGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Execute Task: ${message.runCommand}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NeonGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Actions: Speak & Copy & Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read aloud",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        IconButton(
                            onClick = { onCopy(message.message) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                tint = TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = formattedTime,
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
