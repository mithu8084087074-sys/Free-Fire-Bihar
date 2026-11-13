package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.GameViewModel
import com.example.ui.components.EmberActionButton
import com.example.ui.components.TacticalCard
import com.example.ui.theme.BrightFlame
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MatchmakingRoomScreen(
  viewModel: GameViewModel,
  onLeaveRoom: () -> Unit,
  onMatchActive: () -> Unit,
) {
  val profile by viewModel.profile.collectAsStateWithLifecycle()
  val room by viewModel.activeRoom.collectAsStateWithLifecycle()
  val players by viewModel.activeRoomPlayers.collectAsStateWithLifecycle()
  val chatMessages by viewModel.activeRoomChat.collectAsStateWithLifecycle()
  var chatInput by remember { mutableStateOf("") }

  LaunchedEffect(room?.status) {
    if (room?.status == "active") {
      onMatchActive()
    }
  }

  val isHost = room?.hostUid == profile?.userId

  Box(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .navigationBarsPadding()
        .padding(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = {
            viewModel.leaveCurrentRoom()
            onLeaveRoom()
          }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Leave Room", tint = TextPrimary)
          }
          Column {
            Text(
              text = "ROOM ${room?.roomCode ?: "..."}",
              style = MaterialTheme.typography.headlineMedium,
              color = BrightFlame,
            )
            Text(
              text = "${room?.mode?.uppercase() ?: ""} • ${room?.mapName ?: ""} • ${players.size}/${room?.maxPlayers ?: 50} Real Players",
              style = MaterialTheme.typography.bodyMedium,
              color = TacticalCyan,
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = HealthGreen.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, HealthGreen),
        ) {
          Text(
            text = if (room?.fillBots == true) "REAL + [BOT] FILL" else "REAL PLAYERS ONLY",
            style = MaterialTheme.typography.labelSmall,
            color = HealthGreen,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Spacer(Modifier.height(12.dp))

      // Connected Real Players Roster
      TacticalCard(modifier = Modifier.fillMaxWidth().weight(0.48f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Groups, contentDescription = null, tint = EmberOrange, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text("CONNECTED SQUAD / LOBBY PLAYERS", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          items(players, key = { it.uid }) { p ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = ObsidianVoid,
              border = BorderStroke(1.dp, if (p.uid == room?.hostUid) EmberOrange else SteelBorder),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    text = if (p.uid == room?.hostUid) "${p.username} (HOST)" else p.username,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                  )
                  Text("Character: ${p.characterId} • Weapon: ${p.weaponId}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Icon(Icons.Default.CheckCircle, contentDescription = "Ready", tint = HealthGreen)
              }
            }
          }
        }
      }

      Spacer(Modifier.height(10.dp))

      // Live Room Text Chat
      TacticalCard(modifier = Modifier.fillMaxWidth().weight(0.52f)) {
        Text("LIVE TACTICAL SQUAD CHAT", style = MaterialTheme.typography.titleMedium, color = TacticalCyan)
        Spacer(Modifier.height(6.dp))
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          items(chatMessages, key = { it.id }) { msg ->
            Text(
              text = "${msg.senderName}: ${msg.text}",
              style = MaterialTheme.typography.bodyMedium,
              color = TextPrimary,
            )
          }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = chatInput,
            onValueChange = { chatInput = it },
            placeholder = { Text("Send message to squad...") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          Spacer(Modifier.width(8.dp))
          IconButton(
            onClick = {
              if (chatInput.isNotBlank()) {
                viewModel.sendRoomChat(chatInput)
                chatInput = ""
              }
            },
            modifier = Modifier.size(48.dp),
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Chat", tint = EmberOrange)
          }
        }
      }

      Spacer(Modifier.height(12.dp))

      if (isHost) {
        EmberActionButton(
          text = "LAUNCH 3D BATTLE ROYALE",
          icon = Icons.Default.PlayArrow,
          testTag = "launch_match_button",
          modifier = Modifier.fillMaxWidth(),
          onClick = {
            room?.id?.let { viewModel.launchRoomMatch(it) }
          },
        )
      } else {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          color = ObsidianVoid,
          border = BorderStroke(1.dp, TacticalCyan),
        ) {
          Text(
            text = "Waiting for Host (${room?.hostName ?: ""}) to launch the 3D Battle Drop...",
            style = MaterialTheme.typography.titleMedium,
            color = TacticalCyan,
            modifier = Modifier.padding(14.dp),
          )
        }
      }
    }
  }
}
