package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.BugDiagnosis
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun JarvisBugHunterScreen(
    diagnosis: BugDiagnosis?,
    isDiagnosing: Boolean,
    onDiagnose: (String) -> Unit,
    onCaptureLogcat: () -> Unit,
    logcatOutput: String?,
    isCapturingLogcat: Boolean,
    onExecuteFix: (script: String, runCmd: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var errorInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Sync captured logcat into input field when captured
    if (logcatOutput != null && errorInput.isBlank()) {
        errorInput = logcatOutput
    }

    val crashSamples = listOf(
        "NullPointer Crash" to "java.lang.NullPointerException: Attempt to invoke virtual method on a null object reference at com.example.app.MainActivity.initView(MainActivity.kt:42)",
        "Permission Denied" to "java.lang.SecurityException: Permission Denial: opening provider requires android.permission.READ_EXTERNAL_STORAGE or EACCES permission denied",
        "Python Missing Module" to "Traceback (most recent call last):\n  File 'scanner.py', line 3, in <module>\n    import scapy.all as scapy\nModuleNotFoundError: No module named 'scapy'",
        "Termux Command Missing" to "bash: nmap: command not found\nE: Unable to locate package or binary in PATH"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🐞", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BUG HUNTER & LOGCAT DOCTOR",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TerminalRed
                    )
                }
                Text(
                    text = "Autonomous crash dump parsing, Logcat dissection & 1-click patches",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Quick capture logcat button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberCyan.copy(alpha = 0.15f))
                    .border(1.dp, CyberCyan, RoundedCornerShape(8.dp))
                    .clickable { onCaptureLogcat() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCapturingLogcat) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = CyberCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Capture Logcat",
                            tint = CyberCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Capture Logcat",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                }
            }
        }

        // Preset Crash Chips
        Column {
            Text(
                text = "SAMPLE CRASH TRACES (TAP TO LOAD):",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(crashSamples) { (label, sampleTrace) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberSurface)
                            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                            .clickable { errorInput = sampleTrace }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Error Input Field
        OutlinedTextField(
            value = errorInput,
            onValueChange = { errorInput = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .testTag("bug_error_input"),
            placeholder = {
                Text(
                    "Paste Android crash log, Termux error trace, Python stack trace, or compiler error here...",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            },
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextPrimary
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TerminalRed,
                unfocusedBorderColor = CyberBorder,
                cursorColor = TerminalRed,
                focusedContainerColor = CyberSurface,
                unfocusedContainerColor = CyberSurface
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // Diagnose Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (errorInput.isNotBlank()) {
                Text(
                    text = "Characters: ${errorInput.length} • Lines: ${errorInput.lines().size}",
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (errorInput.isNotBlank()) TerminalRed else CyberSurfaceVariant)
                    .clickable(enabled = errorInput.isNotBlank() && !isDiagnosing) {
                        onDiagnose(errorInput)
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("diagnose_bug_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDiagnosing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Diagnose",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isDiagnosing) "DIAGNOSING..." else "DIAGNOSE WITH JARVIS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }

        // Diagnosis Result Card
        if (diagnosis != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurface)
                    .border(1.dp, Color(diagnosis.severity.colorHex), RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header & Severity Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = diagnosis.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(diagnosis.severity.colorHex).copy(alpha = 0.2f))
                            .border(1.dp, Color(diagnosis.severity.colorHex), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = diagnosis.severity.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(diagnosis.severity.colorHex)
                        )
                    }
                }

                // Root Cause Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberSurfaceVariant)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "ROOT CAUSE:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TerminalYellow
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = diagnosis.rootCause,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "AFFECTED COMPONENT: ${diagnosis.affectedComponent}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                }

                // Solution Explanation
                Column {
                    Text(
                        text = "EXPLANATION & RESOLUTION:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = NeonGreen
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = diagnosis.solutionExplanation,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }

                // Automated Fix Script
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF090E17))
                        .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AUTOMATED FIX SCRIPT (${diagnosis.language.uppercase()}):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = NeonGreen
                        )

                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(diagnosis.fixScript)) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy fix script",
                                tint = NeonGreen,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    SelectionContainer {
                        Text(
                            text = diagnosis.fixScript,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 1-Click Execute Fix Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonGreen)
                            .clickable {
                                onExecuteFix(diagnosis.fixScript, diagnosis.runCommand)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Execute fix",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "APPLY & EXECUTE FIX NOW (${diagnosis.runCommand})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
