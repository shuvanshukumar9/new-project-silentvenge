package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.model.FileEntry
import com.example.agent.model.ProjectInfo
import com.example.agent.model.ServerDaemon
import com.example.ui.theme.*

@Composable
fun SilentVengeToolsScreen(
    files: List<FileEntry>,
    projectInfo: ProjectInfo?,
    serverDaemons: List<ServerDaemon>,
    onRefreshFiles: () -> Unit,
    onCreateFile: (String, String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onSearchCode: (String) -> Unit,
    searchResults: List<String>,
    onStartServer: (String, String, Int) -> Unit,
    onStopServer: (Long) -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("app.py") }
    var newFileContent by remember { mutableStateOf("# Created in SilentVenge Workspace\n") }

    var showStartServerDialog by remember { mutableStateOf(false) }
    var serverName by remember { mutableStateOf("HTTP Static Server") }
    var serverCmd by remember { mutableStateOf("python3 -m http.server 8080") }
    var serverPort by remember { mutableStateOf("8080") }

    var selectedFileContent by remember { mutableStateOf<Pair<String, String>?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(14.dp)
            .testTag("tools_screen_root"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Project Inspection Summary Card (0 tokens!)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROJECT CODE INSPECTOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CyberSurfaceVariant
                        ) {
                            Text(
                                text = "Zero-Token Tool",
                                fontSize = 10.sp,
                                color = NeonGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (projectInfo != null) {
                        Text(
                            text = "Detected: ${projectInfo.projectType.label}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Build: ${projectInfo.buildTool}",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Test: ${projectInfo.testTool}",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Files: ${projectInfo.fileCount} items | Dependencies: ${projectInfo.dependenciesSummary}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    } else {
                        Text("No project loaded yet. Workspace will auto-inspect.", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
        }

        // 2. Code Grep Tool (Search without tokens!)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DETERMINISTIC CODE SEARCH (GREP)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Regex query (e.g. def test_)", fontSize = 11.sp, color = TextMuted) },
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSearchCode(searchQuery) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Grep", fontSize = 11.sp, color = CyberBlack, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (searchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            searchResults.take(8).forEach { match ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CyberSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = match,
                                        fontSize = 10.sp,
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Local Development Servers & Daemons
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LOCAL SERVERS & DAEMONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Button(
                            onClick = { showStartServerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = CyberCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Launch Server", fontSize = 10.sp, color = CyberCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (serverDaemons.isEmpty()) {
                        Text("No active servers running.", fontSize = 11.sp, color = TextMuted)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            serverDaemons.forEach { daemon ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CyberSurface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(daemon.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(
                                                text = "Port: ${daemon.port} | PID: ${daemon.pid} | ${daemon.command}",
                                                fontSize = 10.sp,
                                                color = TextMuted,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Button(
                                            onClick = { onStopServer(daemon.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = TerminalRed),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Stop", fontSize = 10.sp, color = androidx.compose.ui.graphics.Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. File Manager & Workspace Files
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WORKSPACE FILES (${files.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Row {
                    IconButton(onClick = onRefreshFiles, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyberCyan, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showCreateDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Create File", tint = NeonGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        items(files) { file ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, CyberBorder, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                        contentDescription = null,
                        tint = if (file.isDirectory) CyberCyan else NeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(file.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = "${file.sizeBytes} bytes | ${file.extension.ifBlank { "dir" }}",
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Delete file button (triggers PROMPT security)
                    IconButton(
                        onClick = { onDeleteFile(file.path) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete File", tint = TerminalRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    // Create File Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Workspace File", fontSize = 14.sp, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("File Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newFileContent,
                        onValueChange = { newFileContent = it },
                        label = { Text("Initial Content") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateFile(newFileName, newFileContent)
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Create", color = CyberBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Launch Server Dialog
    if (showStartServerDialog) {
        AlertDialog(
            onDismissRequest = { showStartServerDialog = false },
            title = { Text("Start Local Development Server", fontSize = 14.sp, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text("Server Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = serverCmd,
                        onValueChange = { serverCmd = it },
                        label = { Text("Command") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = serverPort,
                        onValueChange = { serverPort = it },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val port = serverPort.toIntOrNull() ?: 8080
                        onStartServer(serverName, serverCmd, port)
                        showStartServerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("Launch", color = CyberBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartServerDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}
