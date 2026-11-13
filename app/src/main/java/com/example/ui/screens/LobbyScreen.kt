package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CatalogMap
import com.example.data.model.MatchRoom
import com.example.engine.LobbyCharacter3DView
import com.example.ui.GameViewModel
import com.example.ui.components.CurrencyPill
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
fun LobbyScreen(
  viewModel: GameViewModel,
  onNavigateCharacters: () -> Unit,
  onNavigateArmory: () -> Unit,
  onNavigateStore: () -> Unit,
  onNavigateEvents: () -> Unit,
  onNavigateSocial: () -> Unit,
  onNavigateLeaderboard: () -> Unit,
  onNavigateProfile: () -> Unit,
  onNavigateMailbox: () -> Unit,
  onNavigateSettings: () -> Unit,
  onNavigateOwnerPanel: () -> Unit,
  onEnterRoomLobby: () -> Unit,
  onStartDirectBotTraining: (mode: String, map: CatalogMap) -> Unit,
) {
  val profile by viewModel.profile.collectAsStateWithLifecycle()
  val config by viewModel.globalConfig.collectAsStateWithLifecycle()
  val characters by viewModel.characters.collectAsStateWithLifecycle()
  val weapons by viewModel.weapons.collectAsStateWithLifecycle()
  val maps by viewModel.maps.collectAsStateWithLifecycle()
  val mailbox by viewModel.mailbox.collectAsStateWithLifecycle()
  val rooms by viewModel.availableRooms.collectAsStateWithLifecycle()
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val isOwner by viewModel.isOwnerAuthorized.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) {
    viewModel.playLobbyMusic()
  }

  val equippedChar = remember(characters, profile) {
    characters.firstOrNull { it.id == profile?.equippedCharacterId } ?: characters.first()
  }
  val equippedWeapon = remember(weapons, profile) {
    weapons.firstOrNull { it.id == profile?.equippedWeaponId } ?: weapons.firstOrNull()
  }
  val enabledMaps = remember(maps) { maps.filter { it.enabled }.ifEmpty { maps } }
  var selectedMapIndex by remember { mutableStateOf(0) }
  val activeMap = enabledMaps.getOrElse(selectedMapIndex) { enabledMaps.first() }

  val allModes = listOf(
    "solo" to "SOLO",
    "duo" to "DUO",
    "squad" to "SQUAD",
    "ranked" to "RANKED",
    "classic" to "CLASSIC",
    "training" to "BOT TRAINING",
  )
  val enabledModesSet = remember(config.enabledModes) {
    config.enabledModes.split(",").map { it.trim().lowercase() }.toSet()
  }
  var selectedMode by remember { mutableStateOf("solo") }

  var showRoomBrowser by remember { mutableStateOf(false) }
  var showCreateRoomDialog by remember { mutableStateOf(false) }
  var passwordPromptRoom by remember { mutableStateOf<MatchRoom?>(null) }

  val unreadMailCount = mailbox.count { !it.isRead }
  val isSuspended = profile?.isSuspended == true

  Box(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
    // Live 3D Character Turntable Background
    LobbyCharacter3DView(
      character = equippedChar,
      weapon = equippedWeapon,
      settings = settings,
      modifier = Modifier.fillMaxSize(),
    )

    // Subtle edge gradient for crisp HUD contrast
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              ObsidianVoid.copy(alpha = 0.78f),
              Color.Transparent,
              Color.Transparent,
              ObsidianVoid.copy(alpha = 0.90f),
            )
          )
        )
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .navigationBarsPadding()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // TOP HEADER BAR
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Player Profile Pill
          Surface(
            modifier = Modifier
              .weight(1f, fill = false)
              .clickable(onClick = onNavigateProfile)
              .testTag("profile_button"),
            shape = RoundedCornerShape(12.dp),
            color = ObsidianVoid.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, EmberOrange.copy(alpha = 0.55f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(EmberOrange),
                contentAlignment = Alignment.Center,
              ) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = ObsidianVoid)
              }
              Spacer(Modifier.width(8.dp))
              Column {
                Text(
                  text = profile?.username ?: "Loading...",
                  style = MaterialTheme.typography.titleMedium,
                  color = TextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "LV.${profile?.level ?: 1} • ${profile?.playerId ?: "FFB-..."}",
                  style = MaterialTheme.typography.labelSmall,
                  color = TacticalCyan,
                )
              }
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            CurrencyPill("Coins", profile?.coins ?: 0L, GoldRank, Icons.Default.MonetizationOn)
            CurrencyPill("Diamonds", profile?.diamonds ?: 0L, TacticalCyan, Icons.Default.Diamond)

            Box {
              IconButton(
                onClick = onNavigateMailbox,
                modifier = Modifier.size(48.dp).testTag("mailbox_button"),
              ) {
                Icon(Icons.Default.Mail, contentDescription = "Mailbox & Notifications", tint = TextPrimary)
              }
              if (unreadMailCount > 0) {
                Box(
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(DangerCrimson),
                  contentAlignment = Alignment.Center,
                ) {
                  Text("$unreadMailCount", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                }
              }
            }

            IconButton(
              onClick = onNavigateSettings,
              modifier = Modifier.size(48.dp).testTag("settings_button"),
            ) {
              Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
            }

            if (isOwner) {
              IconButton(
                onClick = onNavigateOwnerPanel,
                modifier = Modifier.size(48.dp).testTag("owner_panel_button"),
              ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Owner Control Panel", tint = GoldRank)
              }
            }
          }
        }

        Spacer(Modifier.height(6.dp))

        // Brand & Announcement Banner
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          color = ObsidianVoid.copy(alpha = 0.78f),
          border = BorderStroke(1.dp, SteelBorder),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Default.Campaign, contentDescription = "Announcement", tint = BrightFlame, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
              text = if (config.maintenanceMode) "MAINTENANCE: ${config.maintenanceMessage}" else config.announcement,
              style = MaterialTheme.typography.bodyMedium,
              color = if (config.maintenanceMode) DangerCrimson else TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = "v${config.serverVersion}",
              style = MaterialTheme.typography.labelSmall,
              color = TacticalCyan,
            )
          }
        }
      }

      // MIDDLE STAGE: Left Navigation Rail + Character Nameplate
      Row(
        modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Left Vertical Action Menu
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          LobbyRailTile(Icons.Default.SportsKabaddi, "Characters", onNavigateCharacters, "nav_characters")
          LobbyRailTile(Icons.Default.MilitaryTech, "Armory", onNavigateArmory, "nav_armory")
          LobbyRailTile(Icons.Default.ShoppingBag, "Store", onNavigateStore, "nav_store")
          LobbyRailTile(Icons.Default.EmojiEvents, "Events", onNavigateEvents, "nav_events")
          LobbyRailTile(Icons.Default.Groups, "Friends", onNavigateSocial, "nav_friends")
          LobbyRailTile(Icons.Default.Leaderboard, "Rankings", onNavigateLeaderboard, "nav_leaderboard")
        }

        // Right Character Showcase Info Card (drag hint + active hero)
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.Bottom,
          modifier = Modifier.align(Alignment.Bottom),
        ) {
          TacticalCard(
            borderColor = EmberOrange.copy(alpha = 0.55f),
            containerColor = ObsidianVoid.copy(alpha = 0.78f),
          ) {
            Text(equippedChar.name.uppercase(), style = MaterialTheme.typography.titleMedium, color = BrightFlame)
            Text(equippedChar.skillName, style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
            Text(equippedWeapon?.name ?: "AK-Patna 47", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text("Drag character to spin 3D", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
          }
        }
      }

      // BOTTOM LAUNCH CONSOLE
      TacticalCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = EmberOrange.copy(alpha = 0.6f),
        containerColor = ArmorCard.copy(alpha = 0.94f),
      ) {
        if (isSuspended) {
          Text(
            text = "ACCOUNT SUSPENDED: ${profile?.suspensionReason?.ifBlank { "Violation of Fair Play Policy" }}",
            style = MaterialTheme.typography.titleMedium,
            color = DangerCrimson,
          )
        } else {
          // Mode Chips
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            allModes.forEach { (key, label) ->
              val isModeEnabled = key == "training" || key in enabledModesSet
              val selected = selectedMode == key
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable(enabled = isModeEnabled) { selectedMode = key },
                shape = RoundedCornerShape(8.dp),
                color = when {
                  selected -> EmberOrange
                  !isModeEnabled -> ObsidianVoid.copy(alpha = 0.35f)
                  else -> ObsidianVoid
                },
                border = BorderStroke(1.dp, if (selected) BrightFlame else SteelBorder),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  if (key == "training") {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = if (selected) ObsidianVoid else TacticalCyan, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                  }
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                      selected -> ObsidianVoid
                      !isModeEnabled -> TextSecondary.copy(alpha = 0.4f)
                      else -> TextPrimary
                    },
                  )
                }
              }
            }
          }

          Spacer(Modifier.height(10.dp))

          // Map Picker + Launch Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // Map selector pill
            Surface(
              modifier = Modifier
                .weight(1f)
                .clickable { selectedMapIndex = (selectedMapIndex + 1) % enabledMaps.size },
              shape = RoundedCornerShape(10.dp),
              color = ObsidianVoid,
              border = BorderStroke(1.dp, SteelBorder),
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Image(
                  painter = painterResource(id = R.drawable.img_map_patna_delta),
                  contentDescription = activeMap.name,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(activeMap.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1)
                  Text("Tap to switch • ${activeMap.maxPlayers}P", style = MaterialTheme.typography.labelSmall, color = TacticalCyan, maxLines = 1)
                }
              }
            }

            if (selectedMode != "training") {
              Surface(
                modifier = Modifier
                  .height(60.dp)
                  .clickable(enabled = !config.maintenanceMode) { showRoomBrowser = true }
                  .testTag("rooms_button"),
                shape = RoundedCornerShape(10.dp),
                color = ObsidianVoid,
                border = BorderStroke(1.dp, TacticalCyan.copy(alpha = 0.65f)),
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 12.dp),
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(Icons.Default.Public, contentDescription = "Online Rooms", tint = TacticalCyan, modifier = Modifier.size(20.dp))
                  Text("ROOMS (${rooms.size})", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                }
              }
            }

            EmberActionButton(
              text = if (selectedMode == "training") "START BOT MATCH" else "PLAY MATCH",
              icon = Icons.Default.PlayArrow,
              enabled = !config.maintenanceMode || isOwner,
              testTag = "play_match_button",
              modifier = Modifier.height(60.dp),
              onClick = {
                viewModel.audioEngine.playSfx(com.example.audio.GameAudioEngine.SfxType.UI_TAP)
                if (selectedMode == "training") {
                  onStartDirectBotTraining(selectedMode, activeMap)
                } else {
                  val openPublic = rooms.firstOrNull { !it.isPrivate && it.mode == selectedMode && it.status == "waiting" }
                  if (openPublic != null) {
                    viewModel.joinMatchRoom(openPublic) { onEnterRoomLobby() }
                  } else {
                    viewModel.createMatchRoom(selectedMode, activeMap, false, "", false) {
                      onEnterRoomLobby()
                    }
                  }
                }
              },
            )
          }
        }
      }
    }
  }

  // Online Room Browser & Private Room Dialog
  if (showRoomBrowser) {
    AlertDialog(
      onDismissRequest = { showRoomBrowser = false },
      containerColor = ArmorCard,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
          Text("ONLINE MATCH ROOMS", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
          TextButton(onClick = {
            showRoomBrowser = false
            showCreateRoomDialog = true
          }) {
            Text("+ PRIVATE ROOM", color = BrightFlame)
          }
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth().height(300.dp)) {
          if (rooms.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text("No active public rooms yet. Tap PLAY MATCH or + PRIVATE ROOM!", color = TextSecondary)
            }
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              items(rooms, key = { it.id }) { room ->
                TacticalCard(
                  onClick = {
                    if (room.isPrivate) {
                      passwordPromptRoom = room
                    } else {
                      viewModel.joinMatchRoom(room) {
                        showRoomBrowser = false
                        onEnterRoomLobby()
                      }
                    }
                  }
                ) {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        if (room.isPrivate) {
                          Icon(Icons.Default.Lock, contentDescription = "Private", tint = GoldRank, modifier = Modifier.size(15.dp))
                          Spacer(Modifier.width(4.dp))
                        }
                        Text("${room.roomCode} • ${room.mode.uppercase()}", style = MaterialTheme.typography.titleMedium, color = BrightFlame)
                      }
                      Text("Host: ${room.hostName} • Map: ${room.mapName}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Text("${room.playerCount}/${room.maxPlayers}", style = MaterialTheme.typography.labelLarge, color = HealthGreen)
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showRoomBrowser = false }) {
          Text("CLOSE", color = TacticalCyan)
        }
      },
    )
  }

  if (showCreateRoomDialog) {
    var pw by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(true) }
    var fillBots by remember { mutableStateOf(true) }
    AlertDialog(
      onDismissRequest = { showCreateRoomDialog = false },
      containerColor = ArmorCard,
      title = { Text("CREATE CUSTOM ROOM", color = TextPrimary) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Mode: ${selectedMode.uppercase()} • Map: ${activeMap.name}", color = TacticalCyan)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it })
            Text("Private Room (Password Protected)", color = TextPrimary)
          }
          if (isPrivate) {
            OutlinedTextField(
              value = pw,
              onValueChange = { pw = it },
              label = { Text("Room Password") },
              singleLine = true,
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = fillBots, onCheckedChange = { fillBots = it })
            Text("Allow Labeled [BOT] Fill Opponents", color = TextPrimary)
          }
        }
      },
      confirmButton = {
        EmberActionButton(
          text = "CREATE ROOM",
          onClick = {
            showCreateRoomDialog = false
            viewModel.createMatchRoom(selectedMode, activeMap, isPrivate, pw, fillBots) {
              onEnterRoomLobby()
            }
          },
        )
      },
      dismissButton = {
        TextButton(onClick = { showCreateRoomDialog = false }) { Text("CANCEL", color = TextSecondary) }
      },
    )
  }

  passwordPromptRoom?.let { targetRoom ->
    var pwInput by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { passwordPromptRoom = null },
      containerColor = ArmorCard,
      title = { Text("ENTER ROOM PASSWORD (${targetRoom.roomCode})", color = TextPrimary) },
      text = {
        OutlinedTextField(value = pwInput, onValueChange = { pwInput = it }, label = { Text("Password") }, singleLine = true)
      },
      confirmButton = {
        EmberActionButton(
          text = "JOIN",
          onClick = {
            val r = targetRoom
            passwordPromptRoom = null
            showRoomBrowser = false
            viewModel.joinMatchRoom(r, pwInput) { onEnterRoomLobby() }
          },
        )
      },
      dismissButton = {
        TextButton(onClick = { passwordPromptRoom = null }) { Text("CANCEL", color = TextSecondary) }
      },
    )
  }
}

@Composable
private fun LobbyRailTile(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  tag: String,
) {
  Surface(
    modifier = Modifier
      .width(104.dp)
      .height(48.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .testTag(tag),
    shape = RoundedCornerShape(10.dp),
    color = ObsidianVoid.copy(alpha = 0.82f),
    border = BorderStroke(1.dp, SteelBorder),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(icon, contentDescription = label, tint = EmberOrange, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(7.dp))
      Text(label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}
