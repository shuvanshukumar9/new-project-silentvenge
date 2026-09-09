package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.model.AgentTask
import com.example.agent.model.TaskState
import com.example.agent.model.TaskStep
import com.example.executor.ActiveShellProcess
import com.example.executor.ExecutionBackend
import com.example.executor.LogType
import com.example.executor.TerminalLog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pipeline Dashboard Screen visualizing the complete automation pipeline state:
 * - Active Shell Processes (PID, runtime duration, command, backend, kill action)
 * - Live Log Stream & recent logs with severity filters
 * - Pending Task Queue items with queue controls (enqueue, cancel, clear)
 * - Live State Machine Pipeline progress tracking
 */
@Composable
fun PipelineDashboardScreen(
    currentTask: AgentTask?,
    taskState: TaskState,
    activeProcess: ActiveShellProcess?,
    logs: List<TerminalLog>,
    taskQueue: List<AgentTask>,
    currentSteps: List<TaskStep>,
    onEnqueueGoal: (String) -> Unit,
    onRemoveQueuedTask: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onKillActiveProcess: () -> Unit,
    onClearLogs: () -> Unit,
    onNavigateToTerminal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var newGoalInput by remember { mutableStateOf("") }
    var selectedLogFilter by remember { mutableStateOf("ALL") }
    var showQuickEnqueue by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, selectedLogFilter) {
        when (selectedLogFilter) {
            "STDOUT" -> logs.filter { it.type == LogType.STDOUT }
            "STDERR" -> logs.filter { it.type == LogType.STDERR || it.type == LogType.ERROR }
            "SYSTEM" -> logs.filter { it.type == LogType.SYSTEM || it.type == LogType.COMMAND || it.type == LogType.SUCCESS }
            else -> logs
        }
    }

    val quickPipelineGoals = listOf(
        "Build standalone Python HTTP server with unit tests",
        "Scan workspace, check syntax and run tests",
        "Build Android APK and verify compilation",
        "Backup project workspace to tar.gz with hash check"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(14.dp)
            .testTag("pipeline_dashboard_root"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ===================================================================
        // 1. PIPELINE OVERVIEW HUD
        // ===================================================================
        item {
            PipelineHudCard(
                taskState = taskState,
                hasActiveProcess = activeProcess != null,
                queueCount = taskQueue.size,
                currentTask = currentTask
            )
        }

        // ===================================================================
        // 2. ACTIVE SHELL PROCESS CARD
        // ===================================================================
        item {
            ActiveShellProcessCard(
                process = activeProcess,
                onKillProcess = onKillActiveProcess,
                onViewInTerminal = onNavigateToTerminal
            )
        }

        // ===================================================================
        // 3. TASK QUEUE MANAGEMENT
        // ===================================================================
        item {
            PipelineQueueCard(
                queue = taskQueue,
                currentTask = currentTask,
                taskState = taskState,
                newGoalInput = newGoalInput,
                onGoalInputChange = { newGoalInput = it },
                onEnqueue = {
                    if (newGoalInput.isNotBlank()) {
                        onEnqueueGoal(newGoalInput)
                        newGoalInput = ""
                    }
                },
                onRemoveTask = onRemoveQueuedTask,
                onClearQueue = onClearQueue,
                showQuickEnqueue = showQuickEnqueue,
                onToggleQuickEnqueue = { showQuickEnqueue = !showQuickEnqueue },
                quickGoals = quickPipelineGoals
            )
        }

        // ===================================================================
        // 4. PIPELINE STAGES & CURRENT EXECUTION PROGRESS
        // ===================================================================
        if (currentTask != null) {
            item {
                PipelineExecutionFlowCard(
                    currentTask = currentTask,
                    taskState = taskState,
                    currentSteps = currentSteps
                )
            }
        }

        // ===================================================================
        // 5. RECENT LOGS & LIVE STREAM
        // ===================================================================
        item {
            PipelineLogsCard(
                logs = filteredLogs,
                totalLogCount = logs.size,
                selectedFilter = selectedLogFilter,
                onFilterSelected = { selectedLogFilter = it },
                onClearLogs = onClearLogs,
                onOpenTerminal = onNavigateToTerminal
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ===========================================================================
// SUB-COMPONENTS
// ===========================================================================

@Composable
fun PipelineHudCard(
    taskState: TaskState,
    hasActiveProcess: Boolean,
    queueCount: Int,
    currentTask: AgentTask?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hud_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (hasActiveProcess) NeonGreen.copy(alpha = pulseAlpha) else CyberBorder,
                RoundedCornerShape(12.dp)
            )
            .testTag("pipeline_hud_card"),
        colors = CardDefaults.cardColors(containerColor = CyberDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                                    hasActiveProcess -> NeonGreen
                                    taskState != TaskState.IDLE && taskState != TaskState.COMPLETED && taskState != TaskState.FAILED -> CyberCyan
                                    taskState == TaskState.FAILED -> TerminalRed
                                    else -> NeonGreenDim
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AUTOMATION PIPELINE ENGINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when {
                        hasActiveProcess -> NeonGreen.copy(alpha = 0.2f)
                        taskState == TaskState.QUEUED -> TerminalYellow.copy(alpha = 0.2f)
                        taskState == TaskState.IDLE -> CyberSurface
                        else -> CyberCyan.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = if (hasActiveProcess) "ACTIVE SHELL" else taskState.label.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            hasActiveProcess -> NeonGreen
                            taskState == TaskState.QUEUED -> TerminalYellow
                            taskState == TaskState.IDLE -> TextMuted
                            else -> CyberCyan
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Metrix Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Pipeline State
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = CyberSurface
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("STAGE", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(taskState.icon, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = taskState.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Metric 2: Active Shell
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = CyberSurface
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("PROCESS", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hasActiveProcess) "RUNNING" else "IDLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasActiveProcess) NeonGreen else TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Metric 3: Queue Count
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = CyberSurface
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("QUEUED", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$queueCount item(s)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (queueCount > 0) TerminalYellow else TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (currentTask != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyberSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Goal: ", fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(
                            text = currentTask.goal,
                            fontSize = 11.sp,
                            color = CyberCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveShellProcessCard(
    process: ActiveShellProcess?,
    onKillProcess: () -> Unit,
    onViewInTerminal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (process != null) NeonGreen.copy(alpha = 0.8f) else CyberBorder,
                RoundedCornerShape(12.dp)
            )
            .testTag("active_shell_process_card"),
        colors = CardDefaults.cardColors(containerColor = CyberDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = if (process != null) NeonGreen else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ACTIVE SHELL PROCESS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (process != null) NeonGreen else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (process != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NeonGreen.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PID ${process.pid}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (process == null) {
                // Empty state for process
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "No shell process running",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Commands triggered by pipeline steps or terminal will appear here live.",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                // Active process details
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Backend: ${process.backend.name}",
                                fontSize = 10.sp,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            val elapsedSec = ((System.currentTimeMillis() - process.startedAt) / 1000).coerceAtLeast(0)
                            Text(
                                text = "Elapsed: ${elapsedSec}s",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$ ${process.command}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "cwd: ${process.workingDir}",
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onViewInTerminal,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("view_terminal_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Output", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onKillProcess,
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("kill_process_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terminate (SIGKILL)", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PipelineQueueCard(
    queue: List<AgentTask>,
    currentTask: AgentTask?,
    taskState: TaskState,
    newGoalInput: String,
    onGoalInputChange: (String) -> Unit,
    onEnqueue: () -> Unit,
    onRemoveTask: (Int) -> Unit,
    onClearQueue: () -> Unit,
    showQuickEnqueue: Boolean,
    onToggleQuickEnqueue: () -> Unit,
    quickGoals: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .testTag("pipeline_queue_card"),
        colors = CardDefaults.cardColors(containerColor = CyberDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FormatListNumbered,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TASK QUEUE (${queue.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (queue.isNotEmpty()) {
                    TextButton(
                        onClick = onClearQueue,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("clear_queue_button")
                    ) {
                        Text("Clear All", fontSize = 10.sp, color = TerminalRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Queue Enqueue Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newGoalInput,
                    onValueChange = onGoalInputChange,
                    placeholder = { Text("Enqueue task: e.g. 'Run pytest suite'", fontSize = 11.sp, color = TextMuted) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pipeline_queue_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CyberCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onEnqueue,
                    enabled = newGoalInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("pipeline_enqueue_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = CyberBlack, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Queue", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberBlack)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Enqueue Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showQuickEnqueue) "▲ Hide templates" else "▼ Quick presets",
                    fontSize = 10.sp,
                    color = CyberCyanDim,
                    modifier = Modifier
                        .clickable { onToggleQuickEnqueue() }
                        .padding(vertical = 4.dp)
                )
            }

            AnimatedVisibility(visible = showQuickEnqueue) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickGoals.forEach { q ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CyberSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGoalInputChange(q) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(q, fontSize = 10.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Queue List or Empty State
            if (queue.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyberSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏳", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Queue is empty",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Add tasks to be processed automatically one after another.",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    queue.forEachIndexed { index, item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyberSurface,
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = TerminalYellow.copy(alpha = 0.2f),
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TerminalYellow,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = item.goal,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Status: QUEUED",
                                            fontSize = 9.sp,
                                            color = TerminalYellow,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onRemoveTask(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Cancel",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
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

@Composable
fun PipelineExecutionFlowCard(
    currentTask: AgentTask,
    taskState: TaskState,
    currentSteps: List<TaskStep>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .testTag("pipeline_flow_card"),
        colors = CardDefaults.cardColors(containerColor = CyberDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PIPELINE STAGE PROGRESS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "Iteration ${currentTask.iterationCount}/${currentTask.maxIterations}",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val stages = listOf(
                TaskState.INSPECTING to "Inspect",
                TaskState.PLANNING to "Plan",
                TaskState.CODING to "Code",
                TaskState.SAVING to "Save",
                TaskState.BUILDING to "Build",
                TaskState.TESTING to "Test",
                TaskState.COMPLETED to "Done"
            )

            // Horizontal visual pipeline stages
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(stages) { (stage, label) ->
                    val isCurrent = taskState == stage
                    val isPast = taskState.stepIndex > stage.stepIndex || taskState == TaskState.COMPLETED

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isCurrent -> NeonGreen.copy(alpha = 0.2f)
                            isPast -> CyberSurfaceVariant
                            else -> CyberSurface
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isCurrent -> NeonGreen
                                isPast -> CyberCyanDim
                                else -> CyberBorder
                            }
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when {
                                    isPast -> "✓"
                                    isCurrent -> stage.icon
                                    else -> "○"
                                },
                                fontSize = 10.sp,
                                color = if (isCurrent) NeonGreen else if (isPast) CyberCyan else TextMuted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) NeonGreen else if (isPast) TextPrimary else TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            if (currentSteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Recorded Steps (${currentSteps.size}):",
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    currentSteps.takeLast(4).forEach { step ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CyberSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = when (step.status) {
                                            "SUCCESS" -> "✓"
                                            "FAILED" -> "✗"
                                            else -> "▶"
                                        },
                                        fontSize = 11.sp,
                                        color = when (step.status) {
                                            "SUCCESS" -> NeonGreen
                                            "FAILED" -> TerminalRed
                                            else -> CyberCyan
                                        },
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${step.stepName}: ${step.details}",
                                        fontSize = 10.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = step.toolUsed,
                                    fontSize = 9.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
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
fun PipelineLogsCard(
    logs: List<TerminalLog>,
    totalLogCount: Int,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onClearLogs: () -> Unit,
    onOpenTerminal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .testTag("pipeline_logs_card"),
        colors = CardDefaults.cardColors(containerColor = CyberDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PIPELINE LOG STREAM ($totalLogCount)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onOpenTerminal,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Full Console", fontSize = 10.sp, color = CyberCyan)
                    }

                    TextButton(
                        onClick = onClearLogs,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Text("Clear", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Severity Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "STDOUT", "STDERR", "SYSTEM").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(filter, fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan,
                            containerColor = CyberSurface,
                            labelColor = TextMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) CyberCyan else CyberBorder,
                            selectedBorderColor = CyberCyan,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Terminal Log View Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, CyberBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No logs recorded in this filter",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        items(logs) { log ->
                            val color = when (log.type) {
                                LogType.COMMAND -> CyberCyan
                                LogType.STDOUT -> NeonGreen
                                LogType.STDERR -> TerminalRed
                                LogType.SUCCESS -> NeonGreen
                                LogType.ERROR -> TerminalRed
                                LogType.SYSTEM -> TerminalYellow
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                            ) {
                                Text(
                                    text = "[${timeFormat.format(Date(log.timestamp))}] ",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = log.text,
                                    fontSize = 10.sp,
                                    color = color,
                                    fontFamily = FontFamily.Monospace,
                                    softWrap = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
