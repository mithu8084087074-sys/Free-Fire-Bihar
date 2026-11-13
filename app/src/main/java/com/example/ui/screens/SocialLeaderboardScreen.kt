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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PublicPlayer
import com.example.ui.GameViewModel
import com.example.ui.components.EmberActionButton
import com.example.ui.components.TacticalCard
import com.example.ui.theme.ArmorCard
import com.example.ui.theme.BrightFlame
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.GoldRank
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SocialLeaderboardScreen(
  viewModel: GameViewModel,
  initialTab: Int = 0,
  onBack: () -> Unit,
) {
  val profile by viewModel.profile.collectAsStateWithLifecycle()
  val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
  val friends by viewModel.friends.collectAsStateWithLifecycle()

  var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
  val tabs = listOf("LEADERBOARD", "FRIENDS", "CAREER PROFILE")

  var searchQuery by remember { mutableStateOf("") }
  var searchResults by remember { mutableStateOf<List<PublicPlayer>>(emptyList()) }
  var reportTarget by remember { mutableStateOf<PublicPlayer?>(null) }

  Box(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .navigationBarsPadding()
        .padding(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("social_back")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        Text("RANKINGS & SQUAD SOCIAL", style = MaterialTheme.typography.headlineMedium, color = BrightFlame)
      }

      Spacer(Modifier.height(8.dp))

      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = ArmorCard,
        contentColor = BrightFlame,
      ) {
        tabs.forEachIndexed { idx, label ->
          Tab(
            selected = selectedTab == idx,
            onClick = { selectedTab = idx },
            text = { Text(label, style = MaterialTheme.typography.labelSmall) },
          )
        }
      }

      Spacer(Modifier.height(10.dp))

      when (selectedTab) {
        0 -> {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(leaderboard, key = { _, p -> p.userId }) { idx, p ->
              val rankColor = when (idx) {
                0 -> GoldRank
                1 -> Color(0xFFE0E0E0)
                2 -> Color(0xFFFF8A65)
                else -> TacticalCyan
              }
              TacticalCard(borderColor = if (p.userId == profile?.userId) EmberOrange else SteelBorder) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = rankColor.copy(alpha = 0.18f),
                      border = BorderStroke(1.dp, rankColor),
                    ) {
                      Text(
                        text = "#${idx + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        color = rankColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                          modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (p.isOnline) HealthGreen else Color.Gray)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(p.username, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                      }
                      Text("${p.playerId} • LV.${p.level} • ${p.wins} Wins • ${p.kills} Kills", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${p.rankPoints} RP", style = MaterialTheme.typography.titleMedium, color = GoldRank)
                    if (p.userId != profile?.userId) {
                      IconButton(onClick = { reportTarget = p }) {
                        Icon(Icons.Default.Flag, contentDescription = "Report Player", tint = DangerCrimson.copy(alpha = 0.8f))
                      }
                    }
                  }
                }
              }
            }
          }
        }

        1 -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              label = { Text("Search Player ID (FFB-...) or Name") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            IconButton(
              onClick = {
                viewModel.searchPlayers(searchQuery) { searchResults = it }
              },
              modifier = Modifier.size(52.dp).background(EmberOrange, RoundedCornerShape(10.dp)),
            ) {
              Icon(Icons.Default.Search, contentDescription = "Search", tint = ObsidianVoid)
            }
          }

          Spacer(Modifier.height(10.dp))

          LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (searchResults.isNotEmpty()) {
              item { Text("SEARCH RESULTS", style = MaterialTheme.typography.labelLarge, color = TacticalCyan) }
              items(searchResults, key = { "sr_${it.userId}" }) { p ->
                TacticalCard {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                      Text(p.username, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                      Text("${p.playerId} • ${p.rankPoints} RP", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    if (p.userId != profile?.userId) {
                      Button(
                        onClick = { viewModel.sendFriendRequest(p) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmberOrange, contentColor = ObsidianVoid),
                      ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ADD")
                      }
                    }
                  }
                }
              }
            }

            item {
              Spacer(Modifier.height(4.dp))
              Text("MY SQUAD FRIENDS (${friends.size})", style = MaterialTheme.typography.labelLarge, color = BrightFlame)
            }
            if (friends.isEmpty()) {
              item {
                Text("No friends yet. Search by FFB ID or callsign above to build your squad!", color = TextSecondary)
              }
            } else {
              items(friends, key = { it.id }) { f ->
                TacticalCard {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                      Text(f.friendUsername, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                      Text("${f.friendPlayerId} • Status: ${f.status.replace('_', ' ').uppercase()}", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      if (f.status == "pending_received") {
                        Button(
                          onClick = { viewModel.acceptFriend(f.friendUid) },
                          colors = ButtonDefaults.buttonColors(containerColor = HealthGreen, contentColor = ObsidianVoid),
                        ) { Text("ACCEPT") }
                      }
                      OutlinedButton(onClick = { viewModel.removeFriend(f.friendUid) }) {
                        Text("REMOVE", color = DangerCrimson)
                      }
                    }
                  }
                }
              }
            }
          }
        }

        else -> {
          val p = profile
          val matches = (p?.matchesPlayed ?: 0L).coerceAtLeast(1L)
          val winRate = (((p?.wins ?: 0L).toDouble() / matches.toDouble()) * 100.0).toInt()
          val kd = String.format("%.2f", (p?.kills ?: 0L).toDouble() / matches.toDouble())
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
              TacticalCard(borderColor = EmberOrange) {
                Text(p?.username ?: "Operative", style = MaterialTheme.typography.headlineMedium, color = BrightFlame)
                Text("PLAYER ID: ${p?.playerId ?: "..."} • ROLE: ${p?.role?.uppercase() ?: "PLAYER"}", style = MaterialTheme.typography.labelLarge, color = TacticalCyan)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  StatBlock("LEVEL", "${p?.level ?: 1}")
                  StatBlock("RANK PTS", "${p?.rankPoints ?: 1000}")
                  StatBlock("BOOYAH WINS", "${p?.wins ?: 0}")
                  StatBlock("KILLS", "${p?.kills ?: 0}")
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  StatBlock("MATCHES", "${p?.matchesPlayed ?: 0}")
                  StatBlock("WIN RATE", "$winRate%")
                  StatBlock("K/M RATIO", kd)
                  StatBlock("XP", "${p?.xp ?: 0}")
                }
              }
            }
            item {
              TacticalCard {
                Text("ACHIEVEMENTS", style = MaterialTheme.typography.titleMedium, color = GoldRank)
                Spacer(Modifier.height(8.dp))
                AchievementRow("First Parachute Drop", (p?.matchesPlayed ?: 0L) >= 1L)
                AchievementRow("Bihar Sharpshooter (10+ Kills)", (p?.kills ?: 0L) >= 10L)
                AchievementRow("Patna Delta Champion (1+ Booyah)", (p?.wins ?: 0L) >= 1L)
                AchievementRow("Rank Challenger (1200+ RP)", (p?.rankPoints ?: 1000L) >= 1200L)
              }
            }
          }
        }
      }
    }
  }

  reportTarget?.let { target ->
    var reason by remember { mutableStateOf("Cheating / Aimbot") }
    var details by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { reportTarget = null },
      containerColor = ArmorCard,
      title = { Text("REPORT ${target.username}", color = DangerCrimson) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") }, singleLine = true)
          OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Details") })
        }
      },
      confirmButton = {
        EmberActionButton(
          text = "SUBMIT REPORT",
          accentColor = DangerCrimson,
          onClick = {
            viewModel.submitReport(target.playerId, reason, details)
            reportTarget = null
          },
        )
      },
      dismissButton = {
        TextButton(onClick = { reportTarget = null }) { Text("CANCEL", color = TextSecondary) }
      },
    )
  }
}

@Composable
private fun StatBlock(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
    Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
  }
}

@Composable
private fun AchievementRow(title: String, unlocked: Boolean) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        Icons.Default.EmojiEvents,
        contentDescription = null,
        tint = if (unlocked) GoldRank else Color.Gray,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(8.dp))
      Text(title, style = MaterialTheme.typography.bodyMedium, color = if (unlocked) TextPrimary else TextSecondary)
    }
    Text(
      text = if (unlocked) "UNLOCKED" else "LOCKED",
      style = MaterialTheme.typography.labelSmall,
      color = if (unlocked) HealthGreen else TextSecondary,
    )
  }
}
