package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceLanguage(val code: String, val displayName: String, val locale: Locale) {
    HINDI("hi-IN", "हिन्दी (Hindi)", Locale.forLanguageTag("hi-IN")),
    HINGLISH("en-IN", "English / Hinglish", Locale.forLanguageTag("en-IN"))
}

sealed class VoiceState {
    data object Idle : VoiceState()
    data class Listening(val rmsDb: Float = 0f) : VoiceState()
    data object Processing : VoiceState()
    data class Recognized(val text: String) : VoiceState()
    data class Error(val error: String) : VoiceState()
}

class VoiceAutomationManager(private val context: Context) : RecognitionListener, TextToSpeech.OnInitListener {

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(VoiceLanguage.HINDI)
    val selectedLanguage: StateFlow<VoiceLanguage> = _selectedLanguage.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    var isTtsEnabled: Boolean = true

    init {
        initTts()
    }

    fun setLanguage(language: VoiceLanguage) {
        _selectedLanguage.value = language
        updateTtsLanguage(language)
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error initializing TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            updateTtsLanguage(_selectedLanguage.value)
        } else {
            isTtsReady = false
        }
    }

    private fun updateTtsLanguage(language: VoiceLanguage) {
        if (!isTtsReady || tts == null) return
        try {
            val result = tts?.setLanguage(language.locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English (India) or default
                tts?.setLanguage(Locale.forLanguageTag("en-IN"))
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error setting TTS language", e)
        }
    }

    fun speak(text: String) {
        if (!isTtsEnabled || !isTtsReady) return
        try {
            // Check if text contains Devanagari Hindi characters
            val containsHindi = text.any { it in '\u0900'..'\u097F' }
            if (containsHindi) {
                tts?.setLanguage(Locale.forLanguageTag("hi-IN"))
            } else if (_selectedLanguage.value == VoiceLanguage.HINDI) {
                tts?.setLanguage(Locale.forLanguageTag("hi-IN"))
            } else {
                tts?.setLanguage(Locale.forLanguageTag("en-IN"))
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "codex_tts_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error speaking text", e)
        }
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        if (!isSpeechRecognitionAvailable()) {
            _voiceState.value = VoiceState.Error("Speech recognition is not supported on this device.")
            return
        }

        try {
            destroyRecognizer()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceAutomationManager)
            }

            val langCode = _selectedLanguage.value.code
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langCode)
                // Also support multi-language listening so Hindi & English words (e.g. 'Termux update karo') are recognized smoothly
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "en-US"))
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            _voiceState.value = VoiceState.Listening(0f)
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Failed to start listening", e)
            _voiceState.value = VoiceState.Error("Could not activate microphone: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _voiceState.value = VoiceState.Processing
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error stopping listening", e)
            _voiceState.value = VoiceState.Idle
        }
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error destroying speech recognizer", e)
        }
        speechRecognizer = null
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _voiceState.value = VoiceState.Listening(0f)
    }

    override fun onBeginningOfSpeech() {
        _voiceState.value = VoiceState.Listening(2f)
    }

    override fun onRmsChanged(rmsdB: Float) {
        if (_voiceState.value is VoiceState.Listening) {
            _voiceState.value = VoiceState.Listening(rmsdB)
        }
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _voiceState.value = VoiceState.Processing
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permission required"
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking closer to mic."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
            else -> "Speech recognition error ($error)"
        }
        _voiceState.value = VoiceState.Error(errorMessage)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()
        if (!text.isNullOrBlank()) {
            _voiceState.value = VoiceState.Recognized(text)
        } else {
            _voiceState.value = VoiceState.Error("Could not recognize voice query.")
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()
        if (!text.isNullOrBlank()) {
            // Keep listening state active
            _voiceState.value = VoiceState.Listening(4f)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun resetState() {
        _voiceState.value = VoiceState.Idle
    }

    fun release() {
        destroyRecognizer()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
