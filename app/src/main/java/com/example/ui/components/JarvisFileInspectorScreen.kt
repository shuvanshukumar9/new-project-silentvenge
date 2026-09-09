package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.FileInspectionResult
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
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun JarvisFileInspectorScreen(
    inspectionResult: FileInspectionResult?,
    isInspecting: Boolean,
    onInspectFile: (fileName: String, content: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var fileNameInput by remember { mutableStateOf("script.py") }
    var fileContentInput by remember { mutableStateOf("") }

    // SAF Document Picker
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Get filename
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val name = cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) it.getString(nameIndex) else "file.txt"
                    } else "file.txt"
                } ?: "file.txt"
                fileNameInput = name

                // Read content
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    fileContentInput = reader.readText()
                }

                // Trigger inspection
                if (fileContentInput.isNotBlank()) {
                    onInspectFile(fileNameInput, fileContentInput)
                }
            } catch (e: Exception) {
                fileContentInput = "Error opening file: ${e.message}"
            }
        }
    }

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
                    Text("📄", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DOCUMENT & CODE INSPECTOR",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                }
                Text(
                    text = "Static security audit, bug finder & performance optimizer",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Pick Document Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberCyan.copy(alpha = 0.15f))
                    .border(1.dp, CyberCyan, RoundedCornerShape(8.dp))
                    .clickable {
                        docPickerLauncher.launch(arrayOf("*/*"))
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("pick_document_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = "Pick Document",
                        tint = CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Open File",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                }
            }
        }

        // Filename field
        OutlinedTextField(
            value = fileNameInput,
            onValueChange = { fileNameInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("File Name / Identifier", fontSize = 11.sp) },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = CyberBorder,
                cursorColor = CyberCyan,
                focusedContainerColor = CyberSurface,
                unfocusedContainerColor = CyberSurface
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        // Content Area
        OutlinedTextField(
            value = fileContentInput,
            onValueChange = { fileContentInput = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .testTag("file_content_input"),
            placeholder = {
                Text(
                    "Paste code (Python, Bash, Kotlin, JSON, YAML) or open a document via 'Open File' above...",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            },
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextPrimary
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = CyberBorder,
                cursorColor = CyberCyan,
                focusedContainerColor = CyberSurface,
                unfocusedContainerColor = CyberSurface
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // Actions Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lines: ${fileContentInput.lines().size} • Bytes: ${fileContentInput.toByteArray().size}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (fileContentInput.isNotBlank()) CyberCyan else CyberSurfaceVariant)
                    .clickable(enabled = fileContentInput.isNotBlank() && !isInspecting) {
                        onInspectFile(fileNameInput, fileContentInput)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("audit_file_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isInspecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Audit",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isInspecting) "AUDITING..." else "AUDIT WITH JARVIS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )
                }
            }
        }

        // Inspection Report Card
        if (inspectionResult != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = inspectionResult.fileName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        Text(
                            text = "${inspectionResult.fileType} • ${inspectionResult.lineCount} lines • ${inspectionResult.fileSize} bytes",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (inspectionResult.securityAudit.contains("RISK") || inspectionResult.securityAudit.contains("CRITICAL"))
                                    TerminalRed.copy(alpha = 0.2f)
                                else
                                    NeonGreen.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (inspectionResult.securityAudit.contains("RISK")) "FLAGGED" else "SAFE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (inspectionResult.securityAudit.contains("RISK")) TerminalRed else NeonGreen
                        )
                    }
                }

                // Summary
                Text(
                    text = inspectionResult.summary,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                // Security Audit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberSurfaceVariant)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = if (inspectionResult.securityAudit.contains("RISK")) TerminalRed else NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = inspectionResult.securityAudit,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                }

                // Detected Bugs / Smells
                if (inspectionResult.detectedBugs.isNotEmpty()) {
                    Column {
                        Text(
                            text = "DETECTED ISSUES & CODE SMELLS:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TerminalYellow
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        inspectionResult.detectedBugs.forEach { bug ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", color = TerminalYellow, fontSize = 12.sp)
                                Text(bug, color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Optimization Tips
                if (inspectionResult.optimizationTips.isNotEmpty()) {
                    Column {
                        Text(
                            text = "OPTIMIZATION RECOMMENDATIONS:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        inspectionResult.optimizationTips.forEach { tip ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("✓ ", color = CyberCyan, fontSize = 12.sp)
                                Text(tip, color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Optional Refactored Code
                if (!inspectionResult.refactoredCode.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF090E17))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REFACTORED CODE:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NeonGreen
                            )
                            IconButton(
                                onClick = { clipboardManager.setText(AnnotatedString(inspectionResult.refactoredCode)) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        SelectionContainer {
                            Text(
                                text = inspectionResult.refactoredCode.take(500),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
