package com.translator.universal.service

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _recordingAmplitude = MutableStateFlow(0)
    val recordingAmplitude: StateFlow<Int> = _recordingAmplitude.asStateFlow()

    suspend fun startRecording(outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            _isRecording.value = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun stopRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isRecording.value = false
            // Return the recorded file
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun playAudio(file: File, onComplete: () -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    onComplete()
                }
                start()
            }
            _isPlaying.value = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun stopPlaying() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
    }

    fun setPlaybackSpeed(speed: Float) {
        mediaPlayer?.let { player ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                player.playbackParams = player.playbackParams.setSpeed(speed)
            }
        }
    }

    fun release() {
        mediaRecorder?.release()
        mediaPlayer?.release()
        mediaRecorder = null
        mediaPlayer = null
    }
}

@Singleton
class VoiceCloneService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class VoiceProfile(
        val id: String,
        val name: String,
        val audioFile: File?,
        val createdAt: Long = System.currentTimeMillis()
    )

    private val profiles = mutableListOf<VoiceProfile>()

    fun createProfile(name: String, audioFile: File): VoiceProfile {
        val profile = VoiceProfile(
            id = System.currentTimeMillis().toString(),
            name = name,
            audioFile = audioFile
        )
        profiles.add(profile)
        return profile
    }

    fun getProfiles(): List<VoiceProfile> = profiles.toList()

    fun deleteProfile(id: String): Boolean {
        return profiles.removeIf { it.id == id }
    }

    // In production, would use ML model for voice cloning
    suspend fun synthesize(text: String, voiceProfile: VoiceProfile): File? {
        // Placeholder - would use actual TTS with custom voice
        return null
    }
}

@Singleton
class MeetingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voiceService: VoiceService
) {
    data class Participant(
        val id: String,
        val name: String,
        val language: String,
        val isSpeaking: Boolean = false,
        val audioBuffer: ByteArray? = null
    )

    data class MeetingSession(
        val id: String,
        val participants: List<Participant>,
        val startTime: Long,
        val translations: Map<String, String>
    )

    private var currentSession: MeetingSession? = null
    private val _isInMeeting = MutableStateFlow(false)
    val isInMeeting: StateFlow<Boolean> = _isInMeeting.asStateFlow()

    suspend fun startMeeting(participantLanguages: Map<String, String>): MeetingSession {
        val participants = participantLanguages.map { (id, lang) ->
            Participant(id = id, name = "Participant $id", language = lang)
        }

        currentSession = MeetingSession(
            id = System.currentTimeMillis().toString(),
            participants = participants,
            startTime = System.currentTimeMillis(),
            translations = emptyMap()
        )
        _isInMeeting.value = true

        return currentSession!!
    }

    fun endMeeting() {
        currentSession = null
        _isInMeeting.value = false
    }

    fun addTranslation(participantId: String, translatedText: String) {
        currentSession?.let { session ->
            val updatedTranslations = session.translations.toMutableMap()
            updatedTranslations[participantId] = translatedText
            currentSession = session.copy(translations = updatedTranslations)
        }
    }

    fun getCurrentSession(): MeetingSession? = currentSession
}