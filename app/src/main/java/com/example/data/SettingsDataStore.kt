package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.GraphicsTier
import com.example.data.model.LocalPlayerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ffb_player_settings")

class SettingsDataStore(private val context: Context) {
  private object Keys {
    val GRAPHICS_TIER = stringPreferencesKey("graphics_tier")
    val TARGET_FPS = intPreferencesKey("target_fps")
    val SHADOWS_ENABLED = booleanPreferencesKey("shadows_enabled")
    val TEXTURE_QUALITY = stringPreferencesKey("texture_quality")
    val MASTER_VOLUME = floatPreferencesKey("master_volume")
    val MUSIC_VOLUME = floatPreferencesKey("music_volume")
    val SFX_VOLUME = floatPreferencesKey("sfx_volume")
    val TOUCH_SENSITIVITY = floatPreferencesKey("touch_sensitivity")
    val CONTROL_PRESET = stringPreferencesKey("control_preset")
  }

  val settingsFlow: Flow<LocalPlayerSettings> = context.settingsDataStore.data.map { prefs ->
    val tierName = prefs[Keys.GRAPHICS_TIER] ?: GraphicsTier.HIGH.name
    val tier = runCatching { GraphicsTier.valueOf(tierName) }.getOrDefault(GraphicsTier.HIGH)
    LocalPlayerSettings(
      graphicsTier = tier,
      targetFps = prefs[Keys.TARGET_FPS] ?: tier.targetFps,
      shadowsEnabled = prefs[Keys.SHADOWS_ENABLED] ?: true,
      textureQuality = prefs[Keys.TEXTURE_QUALITY] ?: "High",
      masterVolume = prefs[Keys.MASTER_VOLUME] ?: 0.85f,
      musicVolume = prefs[Keys.MUSIC_VOLUME] ?: 0.55f,
      sfxVolume = prefs[Keys.SFX_VOLUME] ?: 0.9f,
      touchSensitivity = prefs[Keys.TOUCH_SENSITIVITY] ?: 1.0f,
      controlPreset = prefs[Keys.CONTROL_PRESET] ?: "Classic Thumb",
    )
  }

  suspend fun updateSettings(settings: LocalPlayerSettings) {
    context.settingsDataStore.edit { prefs ->
      prefs[Keys.GRAPHICS_TIER] = settings.graphicsTier.name
      prefs[Keys.TARGET_FPS] = settings.targetFps
      prefs[Keys.SHADOWS_ENABLED] = settings.shadowsEnabled
      prefs[Keys.TEXTURE_QUALITY] = settings.textureQuality
      prefs[Keys.MASTER_VOLUME] = settings.masterVolume
      prefs[Keys.MUSIC_VOLUME] = settings.musicVolume
      prefs[Keys.SFX_VOLUME] = settings.sfxVolume
      prefs[Keys.TOUCH_SENSITIVITY] = settings.touchSensitivity
      prefs[Keys.CONTROL_PRESET] = settings.controlPreset
    }
  }
}
