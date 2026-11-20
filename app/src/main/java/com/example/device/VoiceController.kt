package com.example.device

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.AppLanguage
import com.example.model.MoodTone
import com.example.model.SathiVoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class VoiceController(context: Context) {
    private val appContext = context.applicationContext

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            } else {
                Log.w(TAG, "TextToSpeech init failed with status $status")
            }
        }
    }

    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(appContext)
    }

    fun startListening(
        language: AppLanguage,
        onFinalResult: (String) -> Unit,
        onErrorMessage: (String) -> Unit
    ) {
        // Interrupt Sathi immediately if she is currently speaking (barge-in)
        stopSpeaking()

        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onErrorMessage(
                if (language == AppLanguage.BENGALI) {
                    "এই ডিভাইসে ভয়েস রিকগনিশন সার্ভিস পাওয়া যায়নি। নিচে লিখে কথা বলতে পারেন।"
                } else {
                    "Speech recognition service is unavailable on this device. You can type below."
                }
            )
            return
        }

        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _partialTranscript.value = ""
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    _rmsLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                    val msg = mapSpeechError(error, language)
                    if (msg != null) onErrorMessage(msg)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()?.trim().orEmpty()
                    _partialTranscript.value = ""
                    if (best.isNotBlank()) {
                        onFinalResult(best)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.localeTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.localeTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognizer", e)
            _isListening.value = false
            onErrorMessage(
                if (language == AppLanguage.BENGALI) {
                    "ভয়েস ইনপুট শুরু করা যায়নি। দয়া করে আবার চেষ্টা করুন।"
                } else {
                    "Could not start voice input. Please try again."
                }
            )
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        _isListening.value = false
        _rmsLevel.value = 0f
    }

    fun speak(
        text: String,
        language: AppLanguage,
        voice: SathiVoice,
        mood: MoodTone
    ) {
        val engine = tts ?: return
        if (!ttsReady || text.isBlank()) return

        // Clean demo tag brackets so spoken output sounds sweet and natural
        val cleanText = text
            .replace(Regex("\\[.*?]"), "")
            .trim()
            .ifBlank { text }

        val targetLocale = if (language == AppLanguage.BENGALI) {
            Locale("bn", "BD")
        } else {
            Locale.US
        }

        val langStatus = engine.setLanguage(targetLocale)
        if (langStatus == TextToSpeech.LANG_MISSING_DATA || langStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }

        applySyntheticVoiceProfile(engine, targetLocale, voice, mood)
        _isSpeaking.value = true
        engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun previewVoice(voice: SathiVoice, language: AppLanguage) {
        val sample = when (voice) {
            SathiVoice.FEMALE_MISTY -> if (language == AppLanguage.BENGALI) {
                "নমস্কার, আমি মিষ্টি। আপনার সাথী হিসেবে সবসময় পাশে আছি।"
            } else {
                "Hello! I am Misty, your warm companion voice in Sathi."
            }

            SathiVoice.MALE_AYAN -> if (language == AppLanguage.BENGALI) {
                "হ্যালো, আমি অয়ন। শান্ত ও আন্তরিক কণ্ঠে আপনার সাথে কথা বলব।"
            } else {
                "Hi there! I am Ayan, your calm and friendly voice in Sathi."
            }
        }
        speak(sample, language, voice, MoodTone.WARM)
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        speechRecognizer = null
        tts = null
    }

    private fun applySyntheticVoiceProfile(
        engine: TextToSpeech,
        locale: Locale,
        voice: SathiVoice,
        mood: MoodTone
    ) {
        val basePitch = when (voice) {
            SathiVoice.FEMALE_MISTY -> 1.14f
            SathiVoice.MALE_AYAN -> 0.84f
        }
        val baseRate = when (voice) {
            SathiVoice.FEMALE_MISTY -> 0.98f
            SathiVoice.MALE_AYAN -> 0.95f
        }

        val moodPitchOffset = when (mood) {
            MoodTone.JOYFUL -> 0.07f
            MoodTone.COMFORTING -> -0.05f
            MoodTone.SERIOUS -> -0.03f
            MoodTone.WARM -> 0f
        }
        val moodRateOffset = when (mood) {
            MoodTone.JOYFUL -> 0.05f
            MoodTone.COMFORTING -> -0.09f
            MoodTone.SERIOUS -> -0.04f
            MoodTone.WARM -> 0f
        }

        engine.setPitch((basePitch + moodPitchOffset).coerceIn(0.65f, 1.45f))
        engine.setSpeechRate((baseRate + moodRateOffset).coerceIn(0.75f, 1.25f))

        // Select matching system synthetic voice if available for the locale
        try {
            val matching = engine.voices
                ?.filter { it.locale.language == locale.language && !it.isNetworkConnectionRequired }
                .orEmpty()
            val preferred = when (voice) {
                SathiVoice.FEMALE_MISTY -> matching.firstOrNull {
                    it.name.contains("female", ignoreCase = true) ||
                        it.name.contains("bn-bd-x-ban", ignoreCase = true)
                }
                SathiVoice.MALE_AYAN -> matching.firstOrNull {
                    it.name.contains("male", ignoreCase = true) ||
                        it.name.contains("bng", ignoreCase = true)
                }
            }
            if (preferred != null) {
                engine.voice = preferred
            }
        } catch (_: Exception) {
        }
    }

    private fun mapSpeechError(error: Int, language: AppLanguage): String? {
        val isBn = language == AppLanguage.BENGALI
        return when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> if (isBn) {
                "আমি কথাটা স্পষ্ট শুনতে পাইনি। আবার বলবেন?"
            } else {
                "I didn't catch that clearly. Could you say it again?"
            }
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> if (isBn) {
                "কিছু শুনতে পাইনি। মাইকে ট্যাপ করে আবার বলুন।"
            } else {
                "No speech detected. Tap the mic to try again."
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> if (isBn) {
                "মাইক্রোফোন অনুমতি প্রয়োজন।"
            } else {
                "Microphone permission is required."
            }
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> if (isBn) {
                "ভয়েস রিকগনিশনে নেটওয়ার্ক সমস্যা হয়েছে। নিচে টাইপ করতে পারেন।"
            } else {
                "Network issue during voice recognition. You can also type below."
            }
            SpeechRecognizer.ERROR_CLIENT -> null
            else -> if (isBn) {
                "ভয়েস ইনপুটে সমস্যা হয়েছে। আবার চেষ্টা করুন।"
            } else {
                "Voice input error. Please try again."
            }
        }
    }

    companion object {
        private const val TAG = "VoiceController"
    }
}
