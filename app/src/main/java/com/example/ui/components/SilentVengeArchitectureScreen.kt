package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun SilentVengeArchitectureScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(14.dp)
            .testTag("architecture_screen_root"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title Card
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
                        text = "SILENTVENGE SYSTEM ARCHITECTURE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lightweight Autonomous Execution Engine & Model-Agnostic Mini Agent",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        // 1. Core Architecture Flow
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
                        text = "1. ARCHITECTURAL TOPOLOGY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = """
Android Mini App (SilentVenge)
  │
  ▼
Agent / Task Manager (Autonomous Pipeline)
  │
  ├─► Tool Executor (Deterministic, 0-token tools)
  │     ├─ File System (Read, Write, Diff, Move, Delete)
  │     ├─ Project Inspector (Type, Tree, Grep)
  │     ├─ Build Runner (Gradle, Python, Node, Cargo)
  │     └─ Server Daemon Supervisor (Start/Stop/Port)
  │
  ├─► Termux Bridge (Controlled Execution Protocol)
  │     ├─ com.termux.RUN_COMMAND intent dispatch
  │     ├─ Kali NetHunter chroot hook
  │     └─ Local POSIX ProcessBuilder fallback
  │
  ├─► Security Policy Engine (SAFE / PROMPT / DENY)
  │
  ▼
AI Model Router ◄──► Local Models (Ollama, LM Studio)
                   └──► Cloud Models (Gemini 2.5 Flash)
                   └──► Offline Heuristic Planner (0 tokens)
                            """.trimIndent(),
                            fontSize = 10.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // 2. Android Components & Termux Bridge
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
                        text = "2. ANDROID COMPONENTS & TERMUX BRIDGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Termux RunCommandService: Dispatches execution via intent com.termux.RUN_COMMAND with background execution, custom workdir, and session handling.\n" +
                               "• Kali NetHunter Integration: Detects com.offsec.nethunter package, enabling chroot-based security tooling when available.\n" +
                               "• Local Process Sandbox: Injects PATH=/data/data/com.termux/files/usr/bin to run shell and python scripts directly in Android POSIX runtime.\n" +
                               "• Room Database Persistence: Offline storage of tasks, execution steps, server daemons, and script repository.",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 3. Permission & Security Model
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
                        text = "3. SECURITY & PERMISSION POLICY MATRIX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalRed,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• SAFE (Auto-Execute): Read files, list directory tree, run compiler/linters, execute unit test runners, grep code.\n" +
                               "• PROMPT (Requires User Confirmation): File deletion, overwriting existing code, destructive shell commands (rm, pkill, chmod), privilege escalation (sudo/su/chroot), listening sockets.\n" +
                               "• DENY (Blocked by Policy): Modification of /system, /vendor, /apex, recursive root deletes (rm -rf /), and unverified downloaded arbitrary binaries.",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 4. Model Router Design & Low-Token Economy
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
                        text = "4. MODEL-ROUTER & TOKEN ECONOMY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• AI is restricted strictly to planning, code synthesis, and diff patching.\n" +
                               "• Zero-token operations: directory walking, regex searching, build commands, test runs, process kills, and error line extraction are all executed deterministically in Kotlin.\n" +
                               "• Pluggable local models: Hot-swap to Ollama or LM Studio without recompiling the app.\n" +
                               "• Graceful Degradation: If local is unreachable, falls back to cloud; if cloud is token-limited or offline, falls back to Offline Heuristic Engine.",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 5. State Machine & Implementation Roadmap
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
                        text = "5. TASK STATE MACHINE & ROADMAP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Autonomous Loop:\n" +
                               "[1. INSPECT] ➔ [2. PLAN] ➔ [3. CODE] ➔ [4. SAVE] ➔ [5. BUILD] ➔ [6. TEST] ➔ [7. ANALYZE ERRORS] ➔ [8. FIX] ➔ [9. REPORT]\n\n" +
                               "Roadmap:\n" +
                               "• M1: Local Deterministic Tool Executor & Termux Bridge (Completed)\n" +
                               "• M2: Autonomous Pipeline & Security Policy Engine (Completed)\n" +
                               "• M3: Multi-Model Router & Ollama / LM Studio Integrator (Completed)\n" +
                               "• M4: Development Daemon & Server Manager (Completed)\n" +
                               "• M5: Project Workspace Git Bridge & Multi-file AST Refactoring (Active)",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
