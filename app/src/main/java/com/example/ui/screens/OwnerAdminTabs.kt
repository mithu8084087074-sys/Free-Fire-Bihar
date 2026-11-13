package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CatalogAudioTrack
import com.example.data.model.CatalogEvent
import com.example.ui.GameViewModel
import com.example.ui.components.EmberActionButton
import com.example.ui.components.TacticalCard
import com.example.ui.theme.BrightFlame
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.GoldRank
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
internal fun OwnerAudioTab(viewModel: GameViewModel) {
  val context = LocalContext.current
  val tracks by viewModel.audioTracks.collectAsStateWithLifecycle()
  val config by viewModel.globalConfig.collectAsStateWithLifecycle()

  var tName by remember { mutableStateOf("Bihar Cyber Anthem") }
  var tSlot by remember { mutableStateOf("lobby_bgm") }
  var tBpm by remember { mutableFloatStateOf(122f) }
  var tPitch by remember { mutableFloatStateOf(115f) }
  var tUri by remember { mutableStateOf("") }

  val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) {
      try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
      } catch (_: Exception) {}
      tUri = uri.toString()
      viewModel.showMessage("Custom audio attached!")
    }
  }

  LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item {
      TacticalCard(borderColor = TacticalCyan) {
        Text("MUSIC & SOUNDTRACK MANAGER", style = MaterialTheme.typography.titleMedium, color = TacticalCyan)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = tName, onValueChange = { tName = it }, label = { Text("Track Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("lobby_bgm", "match_bgm", "victory_bgm", "defeat_bgm").forEach { slot ->
            OutlinedButton(onClick = { tSlot = slot }, modifier = Modifier.weight(1f)) {
              Text(slot.removeSuffix("_bgm").uppercase(), color = if (tSlot == slot) BrightFlame else TextSecondary)
            }
          }
        }
        Spacer(Modifier.height(6.dp))
        Text("Tempo: ${tBpm.toInt()} BPM", color = TextPrimary)
        Slider(value = tBpm, onValueChange = { tBpm = it }, valueRange = 70f..170f)
        Text("Synth Base Pitch: ${tPitch.toInt()} Hz", color = TextPrimary)
        Slider(value = tPitch, onValueChange = { tPitch = it }, valueRange = 65f..220f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = { audioPicker.launch(arrayOf("audio/*")) }) {
            Icon(Icons.Default.AudioFile, contentDescription = null, tint = TacticalCyan)
            Spacer(Modifier.width(4.dp))
            Text(if (tUri.isBlank()) "PICK LOCAL MP3/WAV" else "AUDIO ATTACHED", color = TacticalCyan)
          }
          OutlinedButton(onClick = {
            viewModel.audioEngine.startBgm(
              CatalogAudioTrack(id = "preview", name = tName, slot = tSlot, bpm = tBpm.toLong(), pitchBaseHz = tPitch.toLong(), customUri = tUri)
            )
          }) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Preview", tint = HealthGreen)
            Text("PREVIEW", color = HealthGreen)
          }
          OutlinedButton(onClick = { viewModel.audioEngine.stopBgm() }) {
            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = DangerCrimson)
          }
        }
        Spacer(Modifier.height(8.dp))
        EmberActionButton(
          text = "PUBLISH TRACK & ACTIVATE",
          modifier = Modifier.fillMaxWidth(),
          onClick = {
            val id = "audio_${tSlot}_${System.currentTimeMillis() % 10000}"
            val track = CatalogAudioTrack(
              id = id,
              name = tName.trim(),
              slot = tSlot,
              bpm = tBpm.toLong(),
              pitchBaseHz = tPitch.toLong(),
              volume = 0.75,
              customUri = tUri,
              enabled = true,
            )
            viewModel.adminSaveAudioTrack(track)
            if (tSlot == "lobby_bgm") {
              viewModel.adminSaveGlobalConfig(config.copy(activeLobbyMusicId = id))
            } else if (tSlot == "match_bgm") {
              viewModel.adminSaveGlobalConfig(config.copy(activeMatchMusicId = id))
            }
          },
        )
      }
    }

    items(tracks, key = { "adm_a_${it.id}" }) { tr ->
      TacticalCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text(tr.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("${tr.slot} • ${tr.bpm} BPM • ${tr.pitchBaseHz} Hz", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
          }
          Button(
            onClick = { viewModel.audioEngine.startBgm(tr) },
            colors = ButtonDefaults.buttonColors(containerColor = EmberOrange, contentColor = ObsidianVoid),
          ) { Text("PLAY") }
        }
      }
    }
  }
}

@Composable
internal fun OwnerModerationTab(viewModel: GameViewModel) {
  val players by viewModel.allPlayersForOwner.collectAsStateWithLifecycle()
  val reports by viewModel.allReportsForOwner.collectAsStateWithLifecycle()
  var filter by remember { mutableStateOf("") }

  val filteredPlayers = remember(players, filter) {
    if (filter.isBlank()) players else players.filter {
      it.username.contains(filter, ignoreCase = true) || it.playerId.contains(filter, ignoreCase = true)
    }
  }

  LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item {
      OutlinedTextField(
        value = filter,
        onValueChange = { filter = it },
        label = { Text("Filter Players by Callsign or FFB ID") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
    }
    item { Text("PLAYER ACCOUNTS (${filteredPlayers.size})", style = MaterialTheme.typography.titleMedium, color = BrightFlame) }
    items(filteredPlayers, key = { "adm_u_${it.userId}" }) { u ->
      TacticalCard(borderColor = if (u.isSuspended) DangerCrimson else TacticalCyan.copy(alpha = 0.4f)) {
        Column {
          Text("${u.username} • ${u.playerId}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
          Text("LV.${u.level} • ${u.rankPoints} RP • ${u.wins}W / ${u.kills}K • ${u.coins}C / ${u.diamonds}D", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
          Spacer(Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { viewModel.adminGrantReward(u, 500L, 50L, "Commander RAVI Bonus Drop") },
              colors = ButtonDefaults.buttonColors(containerColor = GoldRank, contentColor = ObsidianVoid),
            ) { Text("+500C / +50D") }
            OutlinedButton(
              onClick = {
                viewModel.adminToggleSuspension(u.userId, !u.isSuspended, if (u.isSuspended) "" else "Suspended by Owner RAVI")
              },
            ) {
              Text(if (u.isSuspended) "RESTORE" else "SUSPEND", color = if (u.isSuspended) HealthGreen else DangerCrimson)
            }
          }
        }
      }
    }

    item {
      Spacer(Modifier.height(6.dp))
      Text("PLAYER REPORTS (${reports.size})", style = MaterialTheme.typography.titleMedium, color = DangerCrimson)
    }
    items(reports, key = { "adm_r_${it.id}" }) { r ->
      TacticalCard {
        Text("Target: ${r.targetPlayerId} • Status: ${r.status.uppercase()}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text("Reason: ${r.reason} — ${r.details}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        if (r.status == "open") {
          Spacer(Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.adminResolveReport(r.id, "resolved") }) { Text("RESOLVE") }
            OutlinedButton(onClick = { viewModel.adminResolveReport(r.id, "dismissed") }) { Text("DISMISS") }
          }
        }
      }
    }
  }
}

@Composable
internal fun OwnerUpdatesAuditTab(viewModel: GameViewModel) {
  val updates by viewModel.updateHistory.collectAsStateWithLifecycle()
  val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

  var eName by remember { mutableStateOf("") }
  var eReward by remember { mutableStateOf("") }
  var uVer by remember { mutableStateOf("1.1.0") }
  var uTitle by remember { mutableStateOf("") }
  var uNotes by remember { mutableStateOf("") }

  LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item {
      TacticalCard {
        Text("PUBLISH EVENT / TOURNAMENT", style = MaterialTheme.typography.titleMedium, color = GoldRank)
        OutlinedTextField(value = eName, onValueChange = { eName = it }, label = { Text("Event Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = eReward, onValueChange = { eReward = it }, label = { Text("Reward Summary") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        EmberActionButton(
          text = "PUBLISH EVENT",
          accentColor = GoldRank,
          onClick = {
            if (eName.isNotBlank()) {
              viewModel.adminSaveEvent(CatalogEvent(name = eName.trim(), rewardSummary = eReward.ifBlank { "1,000 Coins" }, enabled = true))
              eName = ""; eReward = ""
            }
          },
        )
      }
    }

    item {
      TacticalCard(borderColor = BrightFlame) {
        Text("UPDATE MANAGER (LIVE OTA CONFIG)", style = MaterialTheme.typography.titleMedium, color = BrightFlame)
        OutlinedTextField(value = uVer, onValueChange = { uVer = it }, label = { Text("Version (e.g. 1.1.0)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = uTitle, onValueChange = { uTitle = it }, label = { Text("Update Headline") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = uNotes, onValueChange = { uNotes = it }, label = { Text("Patch Notes") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        EmberActionButton(
          text = "PUBLISH UPDATE TO ALL PLAYERS",
          onClick = {
            if (uTitle.isNotBlank()) {
              viewModel.adminPublishUpdate(uVer.trim(), uTitle.trim(), uNotes.trim())
              uTitle = ""; uNotes = ""
            }
          },
        )
      }
    }

    items(updates, key = { "upd_${it.id}" }) { upd ->
      TacticalCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text("v${upd.version} • ${upd.name}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("${upd.status.uppercase()} — ${upd.notes}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
          }
          if (upd.status == "published") {
            OutlinedButton(onClick = { viewModel.adminRollbackUpdate(upd) }) {
              Text("ROLLBACK", color = DangerCrimson)
            }
          }
        }
      }
    }

    item { Text("OWNER SECURITY AUDIT LOGS", style = MaterialTheme.typography.titleMedium, color = TacticalCyan) }
    items(auditLogs.take(25), key = { "aud_${it.id}" }) { log ->
      TacticalCard {
        Text("${log.name} • ${log.actor}", style = MaterialTheme.typography.labelLarge, color = BrightFlame)
        Text(log.details, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
      }
    }
  }
}
