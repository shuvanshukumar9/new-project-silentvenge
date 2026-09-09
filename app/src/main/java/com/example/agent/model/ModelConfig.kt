package com.example.agent.model

enum class AiProviderType(val displayName: String, val isLocal: Boolean, val defaultEndpoint: String) {
    LOCAL_OLLAMA("Local Ollama", true, "http://localhost:11434"),
    LOCAL_LM_STUDIO("Local LM Studio / OpenAI Compat", true, "http://localhost:1234/v1"),
    CLOUD_GEMINI("Google Gemini 2.5 Flash", false, "https://generativelanguage.googleapis.com"),
    OFFLINE_DETERMINISTIC("Offline Deterministic Planner", true, "local://engine")
}

data class ModelRouteConfig(
    val selectedProvider: AiProviderType = AiProviderType.CLOUD_GEMINI,
    val localOllamaEndpoint: String = "http://localhost:11434",
    val localOllamaModel: String = "qwen2.5-coder:7b",
    val localLmStudioEndpoint: String = "http://localhost:1234/v1",
    val localLmStudioModel: String = "codellama",
    val geminiModel: String = "gemini-2.5-flash",
    val customApiKey: String = "",
    val preferLocalWhenAvailable: Boolean = true,
    val maxTokensPerCall: Int = 1024,
    val isLocalOnline: Boolean = false
)

data class TokenTelemetry(
    val tokensConsumed: Long = 0,
    val tokensSavedByDeterministicTools: Long = 0,
    val offlineOperationsCount: Int = 0,
    val apiCallsCount: Int = 0
)
