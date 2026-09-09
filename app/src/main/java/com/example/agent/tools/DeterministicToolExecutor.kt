package com.example.agent.tools

import android.content.Context
import com.example.agent.bridge.TermuxBridge
import com.example.executor.ExecutionOutput
import com.example.agent.model.FileEntry
import com.example.agent.model.ProjectInfo
import com.example.agent.model.ProjectType
import com.example.agent.model.ServerDaemon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DeterministicToolExecutor(
    private val context: Context,
    private val bridge: TermuxBridge
) {

    // Server process registry
    private val runningServers = mutableMapOf<Long, Process>()

    // Default workspace
    val defaultWorkspace: File = File(context.filesDir, "workspace").apply { mkdirs() }

    // ==========================================
    // File Tools
    // ==========================================

    suspend fun readFile(filePath: String): String = withContext(Dispatchers.IO) {
        val file = resolveFile(filePath)
        if (!file.exists()) throw IllegalArgumentException("File does not exist: $filePath")
        file.readText()
    }

    suspend fun createFile(filePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        val file = resolveFile(filePath)
        file.parentFile?.mkdirs()
        file.writeText(content)
        true
    }

    suspend fun editFile(filePath: String, newContent: String): Boolean = withContext(Dispatchers.IO) {
        val file = resolveFile(filePath)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
        }
        file.writeText(newContent)
        true
    }

    suspend fun writeFile(filePath: String, newContent: String): Boolean = editFile(filePath, newContent)

    suspend fun moveFile(sourcePath: String, destPath: String): Boolean = withContext(Dispatchers.IO) {
        val src = resolveFile(sourcePath)
        val dst = resolveFile(destPath)
        if (!src.exists()) return@withContext false
        dst.parentFile?.mkdirs()
        src.renameTo(dst)
    }

    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = resolveFile(filePath)
        if (!file.exists()) return@withContext false
        if (file.isDirectory) file.deleteRecursively() else file.delete()
    }

    suspend fun listFiles(dirPath: String = ""): List<FileEntry> = withContext(Dispatchers.IO) {
        val dir = resolveFile(dirPath)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()
        dir.listFiles()?.map { f ->
            FileEntry(
                path = f.absolutePath,
                name = f.name,
                isDirectory = f.isDirectory,
                sizeBytes = if (f.isFile) f.length() else 0L,
                lastModified = f.lastModified(),
                extension = f.extension
            )
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
    }

    // ==========================================
    // Project Inspection & Understanding
    // ==========================================

    suspend fun inspectProject(rootPath: String = ""): ProjectInfo = withContext(Dispatchers.IO) {
        val root = resolveFile(rootPath)
        if (!root.exists()) root.mkdirs()

        val allFiles = mutableListOf<String>()
        root.walkTopDown().maxDepth(5).forEach { f ->
            if (f.isFile && !f.name.startsWith(".")) {
                allFiles.add(f.relativeTo(root).path)
            }
        }

        // Deterministically detect project type
        val projectType = when {
            allFiles.any { it == "build.gradle" || it == "build.gradle.kts" } -> ProjectType.ANDROID_GRADLE
            allFiles.any { it.endsWith(".py") || it == "requirements.txt" } -> ProjectType.PYTHON
            allFiles.any { it == "package.json" } -> ProjectType.NODE_JS
            allFiles.any { it == "Cargo.toml" } -> ProjectType.RUST
            allFiles.any { it == "Makefile" || it.endsWith(".c") || it.endsWith(".cpp") } -> ProjectType.C_CPP
            allFiles.any { it.endsWith(".sh") || it.endsWith(".bash") } -> ProjectType.SHELL_SCRIPT
            else -> ProjectType.UNKNOWN
        }

        val dependencies = when (projectType) {
            ProjectType.PYTHON -> {
                val reqFile = File(root, "requirements.txt")
                if (reqFile.exists()) reqFile.readLines().take(5).joinToString(", ") else "Standard Library"
            }
            ProjectType.NODE_JS -> {
                val pkgFile = File(root, "package.json")
                if (pkgFile.exists()) "npm dependencies declared" else "None"
            }
            ProjectType.ANDROID_GRADLE -> "Gradle build dependencies"
            else -> "Self-contained script"
        }

        ProjectInfo(
            rootPath = root.absolutePath,
            projectType = projectType,
            fileCount = allFiles.size,
            sourceFiles = allFiles,
            buildTool = projectType.defaultBuildCmd,
            testTool = projectType.defaultTestCmd,
            dependenciesSummary = dependencies
        )
    }

    suspend fun searchCode(rootPath: String = "", query: String): List<String> = withContext(Dispatchers.IO) {
        val root = resolveFile(rootPath)
        val matches = mutableListOf<String>()
        val regex = Regex(query, RegexOption.IGNORE_CASE)

        root.walkTopDown().maxDepth(4).forEach { f ->
            if (f.isFile && f.length() < 500_000) { // skip large binary files
                try {
                    f.readLines().forEachIndexed { index, line ->
                        if (regex.containsMatchIn(line)) {
                            matches.add("${f.name}:${index + 1}: ${line.trim().take(100)}")
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        matches.take(30)
    }

    // ==========================================
    // Build & Test Tools
    // ==========================================

    suspend fun runBuild(command: String, rootPath: String): ExecutionOutput = withContext(Dispatchers.IO) {
        val root = resolveFile(rootPath)
        bridge.executeLocal(command, workDir = root)
    }

    suspend fun runTests(command: String, rootPath: String): ExecutionOutput = withContext(Dispatchers.IO) {
        val root = resolveFile(rootPath)
        bridge.executeLocal(command, workDir = root)
    }

    fun extractErrors(output: ExecutionOutput): String {
        val text = (output.stderr + "\n" + output.stdout).trim()
        val errorLines = mutableListOf<String>()

        text.lines().forEach { line ->
            val lower = line.lowercase()
            if (lower.contains("error:") || lower.contains("fatal:") || lower.contains("exception") ||
                lower.contains("failed") || lower.contains("traceback") || lower.contains("assert")) {
                errorLines.add(line.trim())
            }
        }

        return if (errorLines.isNotEmpty()) {
            errorLines.take(15).joinToString("\n")
        } else {
            text.lines().takeLast(10).joinToString("\n")
        }
    }

    // ==========================================
    // Server Daemon Tools
    // ==========================================

    suspend fun startServer(name: String, command: String, port: Int): ServerDaemon = withContext(Dispatchers.IO) {
        val id = System.currentTimeMillis()
        try {
            val process = ProcessBuilder("sh", "-c", command)
                .directory(defaultWorkspace)
                .redirectErrorStream(true)
                .start()

            runningServers[id] = process

            ServerDaemon(
                id = id,
                name = name,
                command = command,
                port = port,
                pid = id % 10000,
                isRunning = true,
                startedAt = System.currentTimeMillis(),
                logSnippet = "Started server on port $port ($command)"
            )
        } catch (e: Exception) {
            ServerDaemon(
                id = id,
                name = name,
                command = command,
                port = port,
                pid = 0,
                isRunning = false,
                startedAt = 0L,
                logSnippet = "Failed to start: ${e.message}"
            )
        }
    }

    fun stopServer(daemonId: Long): Boolean {
        val process = runningServers[daemonId] ?: return false
        return try {
            process.destroy()
            runningServers.remove(daemonId)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getActiveServersCount(): Int = runningServers.size

    private fun resolveFile(path: String): File {
        return if (path.startsWith("/")) {
            File(path)
        } else {
            File(defaultWorkspace, path)
        }
    }
}
