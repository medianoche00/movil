package com.unp.emprendepiura.features.chat

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max

/*
 * Archivo: ChatAudioManager.kt
 * Proposito: Grabador nativo de notas de voz (.m4a AAC) y reproductor remoto con estado
 *            reactivo para burbujas de audio en EmprendePiura.
 */
data class GrabacionAudioResultado(
    val bytes: ByteArray,
    val duracionSeg: Int
)

class ChatAudioRecorder {

    private var recorder: MediaRecorder? = null
    private var archivoActual: File? = null
    private var inicioMs: Long = 0L

    fun iniciar(context: Context): Boolean {
        cancelar()
        return runCatching {
            val dir = File(context.cacheDir, "chat_audio").apply { mkdirs() }
            val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            archivoActual = file

            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mr
            inicioMs = System.currentTimeMillis()
            true
        }.getOrElse {
            cancelar()
            false
        }
    }

    fun obtenerNivelAmplitud(): Float {
        val amp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        return (amp / 22_000f).coerceIn(0.14f, 1f)
    }

    fun detenerYLeer(): GrabacionAudioResultado? {
        val file = archivoActual
        val duracionSeg = max(1, ((System.currentTimeMillis() - inicioMs) / 1000L).toInt())

        val detenidoOk = runCatching {
            recorder?.stop()
            true
        }.getOrDefault(false)

        runCatching { recorder?.release() }
        recorder = null
        archivoActual = null

        if (!detenidoOk || file == null || !file.exists()) {
            runCatching { file?.delete() }
            return null
        }

        val bytes = runCatching { file.readBytes() }.getOrNull()
        runCatching { file.delete() }

        if (bytes == null || bytes.size < 256) return null
        return GrabacionAudioResultado(bytes = bytes, duracionSeg = duracionSeg)
    }

    fun cancelar() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { archivoActual?.delete() }
        archivoActual = null
    }
}

data class AudioPlaybackState(
    val activeMessageId: String? = null,
    val isPlaying: Boolean = false,
    val isPreparing: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0
)

class ChatAudioPlayerController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    fun togglePlay(messageId: String, audioUrl: String, fallbackDurationSec: Int) {
        if (audioUrl.isBlank()) return
        val current = _state.value

        if (current.activeMessageId == messageId && mediaPlayer != null) {
            if (current.isPreparing) return
            if (current.isPlaying) {
                runCatching { mediaPlayer?.pause() }
                progressJob?.cancel()
                _state.value = current.copy(isPlaying = false)
            } else {
                runCatching { mediaPlayer?.start() }
                _state.value = current.copy(isPlaying = true)
                iniciarProgresoLoop()
            }
            return
        }

        detenerActual()

        _state.value = AudioPlaybackState(
            activeMessageId = messageId,
            isPlaying = false,
            isPreparing = true,
            positionMs = 0,
            durationMs = (fallbackDurationSec.coerceAtLeast(1)) * 1000
        )

        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(audioUrl)
                setOnPreparedListener { player ->
                    if (mediaPlayer !== player) return@setOnPreparedListener
                    val dur = player.duration.takeIf { it > 0 } ?: (fallbackDurationSec.coerceAtLeast(1) * 1000)
                    player.start()
                    _state.value = AudioPlaybackState(
                        activeMessageId = messageId,
                        isPlaying = true,
                        isPreparing = false,
                        positionMs = 0,
                        durationMs = dur
                    )
                    iniciarProgresoLoop()
                }
                setOnCompletionListener { player ->
                    if (mediaPlayer !== player) return@setOnCompletionListener
                    progressJob?.cancel()
                    _state.value = AudioPlaybackState()
                    runCatching { player.release() }
                    mediaPlayer = null
                }
                setOnErrorListener { player, _, _ ->
                    if (mediaPlayer === player) {
                        detenerActual()
                    }
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        }.onFailure {
            detenerActual()
        }
    }

    private fun iniciarProgresoLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val mp = mediaPlayer ?: break
                val playing = runCatching { mp.isPlaying }.getOrDefault(false)
                if (!playing) break
                val pos = runCatching { mp.currentPosition }.getOrDefault(0)
                val dur = runCatching { mp.duration }.getOrDefault(_state.value.durationMs)
                _state.value = _state.value.copy(
                    positionMs = pos,
                    durationMs = dur.takeIf { it > 0 } ?: _state.value.durationMs
                )
                delay(200L)
            }
        }
    }

    fun detenerActual() {
        progressJob?.cancel()
        progressJob = null
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        _state.value = AudioPlaybackState()
    }

    fun liberar() {
        detenerActual()
        scope.cancel()
    }
}
