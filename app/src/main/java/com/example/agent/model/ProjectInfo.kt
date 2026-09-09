package com.example.agent.model

enum class ProjectType(val label: String, val defaultBuildCmd: String, val defaultTestCmd: String) {
    PYTHON("Python", "python -m py_compile main.py", "python -m unittest discover"),
    SHELL_SCRIPT("Bash / Shell", "bash -n script.sh", "bash script.sh --test"),
    ANDROID_GRADLE("Android (Gradle)", "./gradlew assembleDebug", "./gradlew test"),
    NODE_JS("Node.js", "npm run build", "npm test"),
    RUST("Rust (Cargo)", "cargo build", "cargo test"),
    C_CPP("C/C++ (Make/GCC)", "make || gcc main.c -o app", "./app --test"),
    UNKNOWN("Generic / Script", "sh -n script.sh", "sh script.sh")
}

data class FileEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val extension: String = ""
)

data class ProjectInfo(
    val rootPath: String,
    val projectType: ProjectType,
    val fileCount: Int,
    val sourceFiles: List<String>,
    val buildTool: String,
    val testTool: String,
    val dependenciesSummary: String = ""
)
