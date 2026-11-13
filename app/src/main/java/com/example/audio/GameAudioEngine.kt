package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import com.example.data.model.CatalogAudioTrack
import com.example.data.model.LocalPlayerSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural + custom-URI audio engine for FREE FIRE BIHAR.
 * Generates tactical BGM loops (lobby, match, victory, defeat) and SFX
 * (gunshots, footsteps, parachute wind, vehicle engine, reload, medkit, UI clicks)
 * without external binary asset bloat, while also supporting Owner-configured
 * BPM/pitch/volume and custom audio URIs.
 */
class GameAudioEngine(private val context: Context) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var bgmJob: Job? = null
  private var mediaPlayer: MediaPlayer? = null
  private var currentSlot: String = ""

  @Volatile
  var settings: LocalPlayerSettings = LocalPlayerSettings()

  enum class SfxType {
    RIFLE_SHOT,
    SNIPER_SHOT,
    SHOTGUN_SHOT,
    SMG_SHOT,
    RELOAD,
    FOOTSTEP,
    PARACHUTE,
    VEHICLE_HUM,
    MEDKIT,
    HIT_MARKER,
    ELIMINATION,
    UI_TAP,
  }

  fun startBgm(track: CatalogAudioTrack) {
    val slotKey = "${track.id}_${track.bpm}_${track.pitchBaseHz}_${track.customUri}"
    if (currentSlot == slotKey && (bgmJob?.isActive == true || mediaPlayer?.isPlaying == true)) return
    stopBgm()
    currentSlot = slotKey

    if (!track.enabled) return

    if (track.customUri.isNotBlank()) {
      try {
        val player = MediaPlayer().apply {
          setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .build()
          )
          setDataSource(context, Uri.parse(track.customUri))
          isLooping = true
          val vol = (settings.masterVolume * settings.musicVolume * track.volume.toFloat()).coerceIn(0f, 1f)
          setVolume(vol, vol)
          prepare()
          start()
        }
        mediaPlayer = player
        return
      } catch (_: Exception) {
        // Fallback to procedural tactical synth BGM
      }
    }

    bgmJob = scope.launch {
      val sampleRate = 22050
      val bpm = track.bpm.coerceIn(60L, 180L).toFloat()
      val beatDurationSec = 60f / bpm
      val barSamples = (sampleRate * beatDurationSec * 4f).toInt().coerceAtLeast(4096)
      val baseHz = track.pitchBaseHz.coerceIn(55L, 260L).toDouble()
      val scale = when (track.slot) {
        "victory_bgm" -> doubleArrayOf(1.0, 1.25, 1.5, 2.0, 1.5, 1.25, 1.5, 2.0)
        "defeat_bgm" -> doubleArrayOf(1.0, 0.94, 0.84, 0.75, 0.75, 0.84, 0.75, 0.66)
        "match_bgm" -> doubleArrayOf(1.0, 1.0, 1.189, 1.334, 1.0, 1.498, 1.334, 1.189)
        else -> doubleArrayOf(1.0, 1.189, 1.498, 1.334, 1.0, 1.189, 1.587, 1.498)
      }

      val buffer = ShortArray(barSamples)
      val stepSamples = barSamples / scale.size
      for (i in 0 until barSamples) {
        val stepIdx = (i / stepSamples).coerceAtMost(scale.lastIndex)
        val localT = (i % stepSamples).toDouble() / sampleRate
        val freq = baseHz * scale[stepIdx]
        val env = exp(-localT * 4.5)
        val bass = sin(2.0 * PI * freq * localT) * env * 0.45
        val sub = sin(2.0 * PI * (freq * 0.5) * localT) * env * 0.35
        val kickT = (i % (barSamples / 4)).toDouble() / sampleRate
        val kick = if (kickT < 0.09) sin(2.0 * PI * (120.0 - kickT * 750.0) * kickT) * exp(-kickT * 28.0) * 0.55 else 0.0
        val hat = if ((i % (barSamples / 8)) < 260) (Random.nextDouble(-1.0, 1.0) * 0.09) else 0.0
        val mixed = ((bass + sub + kick + hat) * 0.5).coerceIn(-0.95, 0.95)
        buffer[i] = (mixed * Short.MAX_VALUE).toInt().toShort()
      }

      var audioTrack: AudioTrack? = null
      try {
        val minBuf = AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
        )
        audioTrack = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(maxOf(minBuf, barSamples * 2))
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()
        audioTrack.play()

        while (isActive) {
          val vol = (settings.masterVolume * settings.musicVolume * track.volume.toFloat()).coerceIn(0f, 1f)
          audioTrack.setVolume(vol)
          if (vol <= 0.01f) {
            delay(180)
          } else {
            audioTrack.write(buffer, 0, buffer.size)
          }
        }
      } catch (_: Throwable) {
      } finally {
        runCatching {
          audioTrack?.stop()
          audioTrack?.release()
        }
      }
    }
  }

  fun stopBgm() {
    bgmJob?.cancel()
    bgmJob = null
    runCatching {
      mediaPlayer?.stop()
      mediaPlayer?.release()
    }
    mediaPlayer = null
    currentSlot = ""
  }

  fun playSfx(type: SfxType) {
    val vol = (settings.masterVolume * settings.sfxVolume).coerceIn(0f, 1f)
    if (vol <= 0.01f) return
    scope.launch {
      val sampleRate = 22050
      val durationMs = when (type) {
        SfxType.SNIPER_SHOT -> 260
        SfxType.SHOTGUN_SHOT -> 210
        SfxType.RIFLE_SHOT -> 130
        SfxType.SMG_SHOT -> 90
        SfxType.RELOAD -> 180
        SfxType.FOOTSTEP -> 75
        SfxType.PARACHUTE -> 320
        SfxType.VEHICLE_HUM -> 180
        SfxType.MEDKIT -> 340
        SfxType.HIT_MARKER -> 70
        SfxType.ELIMINATION -> 280
        SfxType.UI_TAP -> 45
      }
      val count = (sampleRate * durationMs) / 1000
      val pcm = ShortArray(count)
      for (i in 0 until count) {
        val t = i.toDouble() / sampleRate
        val sample = when (type) {
          SfxType.RIFLE_SHOT, SfxType.SMG_SHOT -> {
            val punch = sin(2.0 * PI * (190.0 - t * 900.0) * t) * exp(-t * 28.0)
            val crack = Random.nextDouble(-1.0, 1.0) * exp(-t * 22.0) * 0.7
            (punch + crack) * 0.65
          }
          SfxType.SNIPER_SHOT -> {
            val boom = sin(2.0 * PI * (140.0 - t * 420.0) * t) * exp(-t * 14.0)
            val tail = Random.nextDouble(-1.0, 1.0) * exp(-t * 12.0) * 0.85
            (boom + tail) * 0.75
          }
          SfxType.SHOTGUN_SHOT -> {
            val blast = sin(2.0 * PI * (110.0 - t * 350.0) * t) * exp(-t * 16.0)
            val noise = Random.nextDouble(-1.0, 1.0) * exp(-t * 15.0) * 0.9
            (blast + noise) * 0.75
          }
          SfxType.RELOAD -> {
            val click = if (t < 0.04 || t > 0.11) sin(2.0 * PI * 980.0 * t) * exp(-t * 35.0) else 0.0
            click * 0.6
          }
          SfxType.FOOTSTEP -> {
            val thud = sin(2.0 * PI * 85.0 * t) * exp(-t * 45.0)
            val grit = Random.nextDouble(-1.0, 1.0) * exp(-t * 50.0) * 0.3
            (thud + grit) * 0.55
          }
          SfxType.PARACHUTE -> Random.nextDouble(-1.0, 1.0) * sin(PI * t / 0.32) * 0.25
          SfxType.VEHICLE_HUM -> sin(2.0 * PI * 68.0 * t) * 0.3 + sin(2.0 * PI * 136.0 * t) * 0.15
          SfxType.MEDKIT -> {
            val f = if (t < 0.17) 523.25 else 783.99
            sin(2.0 * PI * f * t) * exp(-t * 5.0) * 0.45
          }
          SfxType.HIT_MARKER -> sin(2.0 * PI * 1550.0 * t) * exp(-t * 55.0) * 0.6
          SfxType.ELIMINATION -> {
            val f = if (t < 0.12) 660.0 else 990.0
            sin(2.0 * PI * f * t) * exp(-t * 9.0) * 0.55
          }
          SfxType.UI_TAP -> sin(2.0 * PI * 1100.0 * t) * exp(-t * 80.0) * 0.4
        }
        pcm[i] = (sample.coerceIn(-0.98, 0.98) * Short.MAX_VALUE).toInt().toShort()
      }

      var track: AudioTrack? = null
      try {
        track = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(pcm.size * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()
        track.write(pcm, 0, pcm.size)
        track.setVolume(vol)
        track.play()
        delay(durationMs.toLong() + 40L)
      } catch (_: Throwable) {
      } finally {
        runCatching {
          track?.stop()
          track?.release()
        }
      }
    }
  }

  fun release() {
    stopBgm()
  }
}
