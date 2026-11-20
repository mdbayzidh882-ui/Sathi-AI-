package com.example.device

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
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
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VoiceController(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private var mediaPlayer: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var activeFocusRequest: AudioFocusRequest? = null
    private var activeTempFile: File? = null

    private data class PendingPlayback(
        val file: File,
        val cleanText: String,
        val loudBoost: Boolean
    )

    private val pendingBoostedFiles = ConcurrentHashMap<String, PendingPlayback>()

    private val mediaAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

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
                tts?.setAudioAttributes(mediaAudioAttributes)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        if (utteranceId == null || !pendingBoostedFiles.containsKey(utteranceId)) {
                            _isSpeaking.value = true
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        val pending = utteranceId?.let { pendingBoostedFiles.remove(it) }
                        if (pending != null && pending.file.exists() && pending.file.length() > 44L) {
                            playSynthesizedFileLoudly(pending.file, pending.loudBoost)
                        } else {
                            pending?.file?.delete()
                            _isSpeaking.value = false
                            abandonAudioFocus()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        val pending = utteranceId?.let { pendingBoostedFiles.remove(it) }
                        pending?.file?.delete()
                        _isSpeaking.value = false
                        abandonAudioFocus()
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
        mood: MoodTone,
        speechSpeed: Float = 1.12f,
        loudCinemaVoice: Boolean = true
    ) {
        val engine = tts ?: return
        if (!ttsReady || text.isBlank()) return

        stopSpeaking()

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

        engine.setAudioAttributes(mediaAudioAttributes)
        applySyntheticVoiceProfile(engine, targetLocale, voice, mood, speechSpeed)
        requestAudioFocus()

        if (loudCinemaVoice) {
            ensureCinemaMediaVolume()
        }

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        _isSpeaking.value = true

        if (loudCinemaVoice) {
            val outFile = File(appContext.cacheDir, "sathi_tts_$utteranceId.wav")
            pendingBoostedFiles[utteranceId] = PendingPlayback(
                file = outFile,
                cleanText = cleanText,
                loudBoost = true
            )
            val synthResult = engine.synthesizeToFile(cleanText, params, outFile, utteranceId)
            if (synthResult == TextToSpeech.SUCCESS) {
                return
            }
            pendingBoostedFiles.remove(utteranceId)
            outFile.delete()
        }

        engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun previewVoice(
        voice: SathiVoice,
        language: AppLanguage,
        speechSpeed: Float = 1.12f,
        loudCinemaVoice: Boolean = true
    ) {
        val sample = when (voice) {
            SathiVoice.FEMALE_MISTY -> if (language == AppLanguage.BENGALI) {
                "নমস্কার, আমি মিষ্টি! এখন কি আমার কণ্ঠ একদম স্পষ্ট আর জোরে শুনতে পাচ্ছেন?"
            } else {
                "Hello! I am Misty! My voice is now loud, crisp, and clear like cinema audio."
            }

            SathiVoice.MALE_AYAN -> if (language == AppLanguage.BENGALI) {
                "হ্যালো, আমি অয়ন! এখন জোরে, প্রাণবন্ত ও পরিষ্কার কণ্ঠে আপনার সাথে কথা বলব।"
            } else {
                "Hi there! I am Ayan, speaking loudly and clearly right from your speaker."
            }
        }
        speak(sample, language, voice, MoodTone.WARM, speechSpeed, loudCinemaVoice)
    }

    fun stopSpeaking() {
        pendingBoostedFiles.values.forEach { it.file.delete() }
        pendingBoostedFiles.clear()

        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (_: Exception) {
        }
        loudnessEnhancer = null

        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null

        activeTempFile?.delete()
        activeTempFile = null

        try {
            tts?.stop()
        } catch (_: Exception) {
        }

        abandonAudioFocus()
        _isSpeaking.value = false
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        stopSpeaking()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
        speechRecognizer = null
        tts = null
    }

    private fun playSynthesizedFileLoudly(file: File, loudBoost: Boolean) {
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(mediaAudioAttributes)
                setDataSource(file.absolutePath)
                setVolume(1.0f, 1.0f)
                prepare()
            }
            activeTempFile = file
            mediaPlayer = player

            if (loudBoost) {
                try {
                    val enhancer = LoudnessEnhancer(player.audioSessionId).apply {
                        setTargetGain(1100) // +11 dB movie-speaker clarity boost
                        enabled = true
                    }
                    loudnessEnhancer = enhancer
                } catch (e: Exception) {
                    Log.w(TAG, "LoudnessEnhancer unavailable on this device: ${e.message}")
                }
            }

            player.setOnCompletionListener {
                cleanupPlaybackOnly()
            }
            player.setOnErrorListener { _, _, _ ->
                cleanupPlaybackOnly()
                true
            }
            _isSpeaking.value = true
            player.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed boosted MediaPlayer playback", e)
            file.delete()
            cleanupPlaybackOnly()
        }
    }

    private fun cleanupPlaybackOnly() {
        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (_: Exception) {
        }
        loudnessEnhancer = null

        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null

        activeTempFile?.delete()
        activeTempFile = null
        abandonAudioFocus()
        _isSpeaking.value = false
    }

    private fun ensureCinemaMediaVolume() {
        try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val targetFloor = (maxVol * 0.85f).toInt().coerceAtLeast(1)
            if (currentVol < targetFloor) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetFloor, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not adjust media volume floor: ${e.message}")
        }
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(mediaAudioAttributes)
                    .build()
                activeFocusRequest = req
                audioManager.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activeFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                activeFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (_: Exception) {
        }
    }

    private fun applySyntheticVoiceProfile(
        engine: TextToSpeech,
        locale: Locale,
        voice: SathiVoice,
        mood: MoodTone,
        speechSpeed: Float
    ) {
        val basePitch = when (voice) {
            SathiVoice.FEMALE_MISTY -> 1.12f
            SathiVoice.MALE_AYAN -> 0.86f
        }
        val baseRate = when (voice) {
            SathiVoice.FEMALE_MISTY -> speechSpeed
            SathiVoice.MALE_AYAN -> (speechSpeed - 0.03f).coerceAtLeast(0.85f)
        }

        val moodPitchOffset = when (mood) {
            MoodTone.JOYFUL -> 0.06f
            MoodTone.COMFORTING -> -0.04f
            MoodTone.SERIOUS -> -0.02f
            MoodTone.WARM -> 0f
        }
        val moodRateOffset = when (mood) {
            MoodTone.JOYFUL -> 0.05f
            MoodTone.COMFORTING -> -0.04f
            MoodTone.SERIOUS -> -0.02f
            MoodTone.WARM -> 0f
        }

        engine.setPitch((basePitch + moodPitchOffset).coerceIn(0.65f, 1.45f))
        engine.setSpeechRate((baseRate + moodRateOffset).coerceIn(0.80f, 1.60f))

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
