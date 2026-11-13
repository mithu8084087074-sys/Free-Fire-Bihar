package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CatalogMap
import com.example.engine.Battle3DViewport
import com.example.engine.BattleSimulation
import com.example.engine.HudSnapshot
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
import com.example.ui.theme.ZoneBlue
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun BattleMatchScreen(
  viewModel: GameViewModel,
  selectedMap: CatalogMap,
  isBotMode: Boolean,
  roomId: String?,
  onExitMatch: () -> Unit,
) {
  val profile by viewModel.profile.collectAsStateWithLifecycle()
  val characters by viewModel.characters.collectAsStateWithLifecycle()
  val weapons by viewModel.weapons.collectAsStateWithLifecycle()
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val remotePlayers by viewModel.activeRoomPlayers.collectAsStateWithLifecycle()
  val activeRoom by viewModel.activeRoom.collectAsStateWithLifecycle()

  val playerChar = remember(characters, profile) {
    characters.firstOrNull { it.id == profile?.equippedCharacterId } ?: characters.first()
  }

  val simulation = remember(selectedMap.id, isBotMode) {
    BattleSimulation(
      map = selectedMap,
      weaponsCatalog = weapons,
      playerName = profile?.username ?: "Warrior",
      playerCharacter = playerChar,
      initialWeaponId = profile?.equippedWeaponId ?: "wpn_ak_patna",
      isBotMode = isBotMode || activeRoom?.fillBots == true,
      botCount = if (isBotMode) 15 else if (activeRoom?.fillBots == true) 10 else 0,
      audioEngine = viewModel.audioEngine,
    )
  }

  var hud by remember { mutableStateOf(simulation.snapshotHud()) }
  var showFullMap by remember { mutableStateOf(false) }
  var showBackpack by remember { mutableStateOf(false) }
  var isSpectating by remember { mutableStateOf(false) }
  var voiceMicEnabled by remember { mutableStateOf(false) }
  var resultRecorded by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.playMatchMusic()
  }

  // Sync remote multiplayer players into 3D simulation
  LaunchedEffect(remotePlayers) {
    val uid = profile?.userId ?: return@LaunchedEffect
    if (roomId != null) {
      simulation.syncRemotePlayers(remotePlayers, uid)
    }
  }

  // Push local player state to Firestore every 350ms when in an online multiplayer room
  LaunchedEffect(roomId) {
    if (roomId != null) {
      while (true) {
        val snap = simulation.snapshotHud()
        viewModel.syncPlayerInMatch(
          roomId = roomId,
          x = snap.playerX.toDouble(),
          y = simulation.localPlayer.y.toDouble(),
          z = snap.playerZ.toDouble(),
          yaw = snap.playerYawDeg.toDouble(),
          hp = snap.hp,
          armor = snap.armor,
          kills = snap.kills,
          isAlive = snap.isAlive,
          isCrouching = snap.isCrouching,
          isParachuting = snap.isParachuting,
          vehicleId = snap.drivingVehicle?.id ?: "",
        )
        delay(350L)
      }
    }
  }

  LaunchedEffect(hud.isMatchOver) {
    if (hud.isMatchOver && !resultRecorded) {
      resultRecorded = true
      viewModel.playOutcomeMusic(hud.wonMatch)
      viewModel.completeMatch(roomId, hud.kills, hud.wonMatch, hud.placement)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      viewModel.playLobbyMusic()
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(ObsidianVoid)) {
    // 1. Hardware-Accelerated 3D OpenGL ES Battleground
    Battle3DViewport(
      simulation = simulation,
      settings = settings,
      onHudSnapshot = { hud = it },
      modifier = Modifier.fillMaxSize(),
    )

    // 2. Right-Half Screen Touch-Drag Look Camera
    Box(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            simulation.applyLookDelta(dragAmount.x, dragAmount.y)
          }
        }
    )

    // Damage / Blue-Zone Screen Edge Flash
    if (hud.damageFlashTimer > 0f || hud.outsideZone) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .border(
            width = if (hud.damageFlashTimer > 0f) 6.dp else 3.dp,
            color = if (hud.damageFlashTimer > 0f) DangerCrimson.copy(alpha = 0.7f) else ZoneBlue.copy(alpha = 0.55f),
          )
      )
    }

    // Center Crosshair + Hitmarker
    if (hud.isAlive && !hud.isMatchOver) {
      Box(modifier = Modifier.align(Alignment.Center)) {
        val crossColor = if (hud.hitMarkerTimer > 0f) DangerCrimson else Color.White.copy(alpha = 0.85f)
        Icon(
          imageVector = if (hud.isAiming) Icons.Default.CenterFocusStrong else Icons.Default.GpsFixed,
          contentDescription = "Crosshair",
          tint = crossColor,
          modifier = Modifier.size(if (hud.isAiming) 34.dp else 24.dp),
        )
      }
    }

    // 3. TOP TACTICAL HUD (Minimap, Alive, Kills, Timer, Kill Feed)
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .navigationBarsPadding()
        .padding(10.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        // Left: Minimap + Compass + Kill Feed
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            modifier = Modifier
              .size(104.dp)
              .clickable { showFullMap = true }
              .testTag("minimap_button"),
            shape = RoundedCornerShape(10.dp),
            color = ObsidianVoid.copy(alpha = 0.82f),
            border = BorderStroke(1.5.dp, TacticalCyan),
          ) {
            Box(Modifier.fillMaxSize()) {
              Canvas(Modifier.fillMaxSize()) {
                val scale = size.width / 240f
                val cx = size.width / 2f
                val cz = size.height / 2f
                // Blue Zone circle
                drawCircle(
                  color = ZoneBlue.copy(alpha = 0.7f),
                  radius = hud.zoneRadius * scale,
                  center = Offset(cx + hud.zoneCenterX * scale, cz + hud.zoneCenterZ * scale),
                  style = Stroke(width = 2.5f),
                )
                // Enemy/Bot blips
                hud.radarBlips.forEach { (bx, bz, _) ->
                  drawCircle(
                    color = DangerCrimson,
                    radius = 3.5f,
                    center = Offset(cx + bx * scale, cz + bz * scale),
                  )
                }
                // Local player blip
                drawCircle(
                  color = HealthGreen,
                  radius = 5f,
                  center = Offset(cx + hud.playerX * scale, cz + hud.playerZ * scale),
                )
              }
              Text(
                text = "N ${(hud.playerYawDeg.toInt() + 360) % 360}°",
                style = MaterialTheme.typography.labelSmall,
                color = TacticalCyan,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp),
              )
              Text(
                text = "MAP",
                style = MaterialTheme.typography.labelSmall,
                color = BrightFlame,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
              )
            }
          }

          // Kill Feed
          Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            hud.killFeed.forEach { item ->
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = ObsidianVoid.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, if (item.isPlayerEvent) EmberOrange else SteelBorder),
              ) {
                Text(
                  text = item.text,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (item.isPlayerEvent) BrightFlame else TextPrimary,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                )
              }
            }
          }
        }

        // Right: Alive / Kills / Zone / Voice / Exit
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            HudStatChip("ALIVE", "${hud.aliveCount}", HealthGreen)
            HudStatChip("KILLS", "${hud.kills}", EmberOrange)
            HudStatChip("TIME", "${hud.matchElapsedSec / 60}:${(hud.matchElapsedSec % 60).toString().padStart(2, '0')}", TacticalCyan)
            IconButton(
              onClick = { voiceMicEnabled = !voiceMicEnabled },
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ObsidianVoid.copy(alpha = 0.8f)),
            ) {
              Icon(
                imageVector = if (voiceMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Squad Voice",
                tint = if (voiceMicEnabled) HealthGreen else TextSecondary,
                modifier = Modifier.size(18.dp),
              )
            }
            IconButton(
              onClick = onExitMatch,
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ObsidianVoid.copy(alpha = 0.8f))
                .testTag("exit_match_button"),
            ) {
              Icon(Icons.Default.Close, contentDescription = "Exit Match", tint = DangerCrimson, modifier = Modifier.size(18.dp))
            }
          }
          if (hud.isParachuting) {
            Surface(shape = RoundedCornerShape(8.dp), color = EmberOrange.copy(alpha = 0.9f)) {
              Text(
                text = "PARACHUTE DROP • STEER WITH JOYSTICK",
                style = MaterialTheme.typography.labelSmall,
                color = ObsidianVoid,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          } else if (hud.outsideZone) {
            Surface(shape = RoundedCornerShape(8.dp), color = ZoneBlue.copy(alpha = 0.9f)) {
              Text(
                text = "OUTSIDE SAFE ZONE (PHASE ${hud.zonePhase})",
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        }
      }

      // BOTTOM CONTROLS & WEAPON / HEALTH HUD
      if (hud.isAlive && !hud.isMatchOver) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom,
        ) {
          // Left: Virtual Joystick + Medkit + Backpack + Sprint
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              HudActionCircle("BAG", Icons.Default.Backpack, false) { showBackpack = true }
              HudActionCircle("MED (${hud.medkits})", Icons.Default.LocalHospital, false) { simulation.useMedkit() }
              HudActionCircle("RUN", Icons.Default.DirectionsRun, hud.isSprinting) {
                simulation.isSprinting = !simulation.isSprinting
              }
            }
            VirtualJoystick(
              onMove = { ix, iy ->
                simulation.moveInputX = ix
                simulation.moveInputY = iy
              },
            )
          }

          // Center: Health, Armor & Weapon Slots
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.width(230.dp),
          ) {
            // Contextual Loot / Vehicle button
            val ctxLabel = when {
              hud.drivingVehicle != null -> "EXIT ${hud.drivingVehicle!!.name.uppercase()}"
              hud.nearbyVehicle != null -> "DRIVE ${hud.nearbyVehicle!!.name.uppercase()}"
              hud.nearbyLoot != null -> "LOOT ${hud.nearbyLoot!!.name.uppercase()}"
              else -> null
            }
            if (ctxLabel != null) {
              EmberActionButton(
                text = ctxLabel,
                icon = Icons.Default.DirectionsCar,
                onClick = { simulation.interactPickupOrVehicle() },
                accentColor = TacticalCyan,
                testTag = "context_interact_button",
              )
            }

            // Weapon Slots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              WeaponSlotPill(
                label = hud.primaryWeapon?.name?.take(10) ?: "AR",
                ammo = "${hud.primaryAmmoInMag}/${hud.primaryReserveAmmo}",
                selected = hud.activeWeaponSlot == 0,
                onClick = { simulation.switchWeaponSlot(0) },
                modifier = Modifier.weight(1f),
              )
              WeaponSlotPill(
                label = hud.secondaryWeapon?.name?.take(10) ?: "Pistol",
                ammo = "${hud.secondaryAmmoInMag}/${hud.secondaryReserveAmmo}",
                selected = hud.activeWeaponSlot == 1,
                onClick = { simulation.switchWeaponSlot(1) },
                modifier = Modifier.weight(1f),
              )
              WeaponSlotPill(
                label = "Blade",
                ammo = "INF",
                selected = hud.activeWeaponSlot == 2,
                onClick = { simulation.switchWeaponSlot(2) },
                modifier = Modifier.weight(0.65f),
              )
            }

            // Armor & HP Bars
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = ObsidianVoid.copy(alpha = 0.85f),
              border = BorderStroke(1.dp, SteelBorder),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = "Armor", tint = TacticalCyan, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${hud.armor}", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                  }
                  Text("HP ${hud.hp}/${hud.maxHp}", style = MaterialTheme.typography.labelSmall, color = HealthGreen)
                }
                Spacer(Modifier.height(3.dp))
                LinearProgressIndicator(
                  progress = { (hud.hp / hud.maxHp.toFloat()).coerceIn(0f, 1f) },
                  color = if (hud.hp > 35) HealthGreen else DangerCrimson,
                  trackColor = SteelBorder,
                  modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                )
              }
            }
          }

          // Right: Fire, Aim, Jump, Crouch, Reload
          Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              HudActionCircle("AIM", Icons.Default.CenterFocusStrong, hud.isAiming) {
                simulation.isAiming = !simulation.isAiming
              }
              HudActionCircle("RELOAD", Icons.Default.Refresh, hud.isReloading) {
                simulation.triggerReload()
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HudActionCircle("CROUCH", null, hud.isCrouching) { simulation.toggleCrouch() }
                HudActionCircle("JUMP", null, false) { simulation.triggerJump() }
              }
              // Primary FIRE Trigger
              Box(
                modifier = Modifier
                  .size(82.dp)
                  .clip(CircleShape)
                  .background(EmberOrange.copy(alpha = 0.92f))
                  .border(3.dp, BrightFlame, CircleShape)
                  .pointerInput(Unit) {
                    awaitPointerEventScope {
                      while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.any { it.pressed }
                        simulation.isFiringHeld = pressed
                      }
                    }
                  }
                  .testTag("fire_button"),
                contentAlignment = Alignment.Center,
              ) {
                Text("FIRE", style = MaterialTheme.typography.titleMedium, color = ObsidianVoid, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // 4. SPECTATOR OVERLAY OR VICTORY / DEFEAT RESULTS
    if (!hud.isAlive && isSpectating) {
      Surface(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 24.dp),
        shape = RoundedCornerShape(14.dp),
        color = ArmorCard.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, TacticalCyan),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(Icons.Default.Visibility, contentDescription = null, tint = TacticalCyan)
          Column {
            Text("SPECTATING MODE", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
            Text(hud.spectatingName.ifBlank { "Survivor" }, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
          }
          TextButton(onClick = { simulation.cycleSpectator() }) { Text("NEXT", color = BrightFlame) }
          EmberActionButton(text = "RESULTS", onClick = { isSpectating = false })
        }
      }
    }

    if (hud.isMatchOver && !isSpectating) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(ObsidianVoid.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
      ) {
        TacticalCard(
          modifier = Modifier.fillMaxWidth(0.88f),
          borderColor = if (hud.wonMatch) GoldRank else DangerCrimson,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Icon(
              imageVector = Icons.Default.EmojiEvents,
              contentDescription = null,
              tint = if (hud.wonMatch) GoldRank else DangerCrimson,
              modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
              text = if (hud.wonMatch) "BOOYAH! BIHAR CHAMPION" else "ELIMINATED (#${hud.placement})",
              style = MaterialTheme.typography.headlineMedium,
              color = if (hud.wonMatch) GoldRank else DangerCrimson,
            )
            Text(
              text = "Map: ${selectedMap.name} • Eliminations: ${hud.kills}",
              style = MaterialTheme.typography.bodyLarge,
              color = TextPrimary,
            )
            Spacer(Modifier.height(14.dp))
            val coinsGain = (if (hud.wonMatch) 350 else 100) + hud.kills * 35
            Text(
              text = "+$coinsGain Coins • +${if (hud.wonMatch) 20 else 0} Diamonds • Rank Synced",
              style = MaterialTheme.typography.titleMedium,
              color = HealthGreen,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
              if (!hud.wonMatch && hud.aliveCount > 0) {
                EmberActionButton(
                  text = "SPECTATE",
                  accentColor = TacticalCyan,
                  modifier = Modifier.weight(1f),
                  onClick = { isSpectating = true },
                )
              }
              EmberActionButton(
                text = "RETURN TO LOBBY",
                modifier = Modifier.weight(1f),
                testTag = "return_lobby_button",
                onClick = onExitMatch,
              )
            }
          }
        }
      }
    }
  }

  // Full Tactical Map Modal
  if (showFullMap) {
    AlertDialog(
      onDismissRequest = { showFullMap = false },
      containerColor = ArmorCard,
      title = { Text("${selectedMap.name.uppercase()} — TACTICAL MAP", color = TextPrimary) },
      text = {
        Box(modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(12.dp))) {
          Image(
            painter = painterResource(id = R.drawable.img_map_patna_delta),
            contentDescription = selectedMap.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Canvas(Modifier.fillMaxSize()) {
            val scaleX = size.width / 240f
            val scaleZ = size.height / 240f
            val cx = size.width / 2f
            val cz = size.height / 2f
            drawCircle(
              color = ZoneBlue,
              radius = hud.zoneRadius * scaleX,
              center = Offset(cx + hud.zoneCenterX * scaleX, cz + hud.zoneCenterZ * scaleZ),
              style = Stroke(width = 4f),
            )
            drawCircle(
              color = HealthGreen,
              radius = 8f,
              center = Offset(cx + hud.playerX * scaleX, cz + hud.playerZ * scaleZ),
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showFullMap = false }) { Text("CLOSE MAP", color = TacticalCyan) }
      },
    )
  }

  // Backpack / Inventory Modal
  if (showBackpack) {
    AlertDialog(
      onDismissRequest = { showBackpack = false },
      containerColor = ArmorCard,
      title = { Text("TACTICAL BACKPACK", color = TextPrimary) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Primary: ${hud.primaryWeapon?.name} (${hud.primaryAmmoInMag}/${hud.primaryReserveAmmo})", color = BrightFlame)
          Text("Secondary: ${hud.secondaryWeapon?.name} (${hud.secondaryAmmoInMag}/${hud.secondaryReserveAmmo})", color = TextPrimary)
          Text("Armor Vest: ${hud.armor}/100", color = TacticalCyan)
          Text("Medkits: ${hud.medkits}", color = HealthGreen)
        }
      },
      confirmButton = {
        TextButton(onClick = { showBackpack = false }) { Text("CLOSE", color = TacticalCyan) }
      },
    )
  }
}

@Composable
private fun HudStatChip(label: String, value: String, color: Color) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = ObsidianVoid.copy(alpha = 0.82f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
  ) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
      Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
  }
}

@Composable
private fun HudActionCircle(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector?,
  active: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .size(50.dp)
      .clip(CircleShape)
      .clickable(onClick = onClick),
    shape = CircleShape,
    color = if (active) EmberOrange else ObsidianVoid.copy(alpha = 0.82f),
    border = BorderStroke(1.dp, if (active) BrightFlame else SteelBorder),
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
      if (icon != null) {
        Icon(icon, contentDescription = label, tint = if (active) ObsidianVoid else TextPrimary, modifier = Modifier.size(18.dp))
      }
      Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) ObsidianVoid else TextPrimary)
    }
  }
}

@Composable
private fun WeaponSlotPill(
  label: String,
  ammo: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.height(48.dp).clickable(onClick = onClick),
    shape = RoundedCornerShape(8.dp),
    color = if (selected) EmberOrange.copy(alpha = 0.9f) else ObsidianVoid.copy(alpha = 0.82f),
    border = BorderStroke(1.dp, if (selected) BrightFlame else SteelBorder),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) ObsidianVoid else TextPrimary, maxLines = 1)
      Text(ammo, style = MaterialTheme.typography.labelSmall, color = if (selected) ObsidianVoid else TacticalCyan, maxLines = 1)
    }
  }
}

@Composable
private fun VirtualJoystick(
  onMove: (Float, Float) -> Unit,
) {
  var knobX by remember { mutableStateOf(0f) }
  var knobY by remember { mutableStateOf(0f) }
  val maxRadius = 44f

  Box(
    modifier = Modifier
      .size(124.dp)
      .clip(CircleShape)
      .background(ObsidianVoid.copy(alpha = 0.55f))
      .border(2.dp, TacticalCyan.copy(alpha = 0.55f), CircleShape)
      .pointerInput(Unit) {
        detectDragGestures(
          onDragEnd = {
            knobX = 0f
            knobY = 0f
            onMove(0f, 0f)
          },
          onDragCancel = {
            knobX = 0f
            knobY = 0f
            onMove(0f, 0f)
          },
        ) { change, dragAmount ->
          change.consume()
          val nx = knobX + dragAmount.x
          val ny = knobY + dragAmount.y
          val dist = hypot(nx, ny)
          if (dist > maxRadius) {
            knobX = (nx / dist) * maxRadius
            knobY = (ny / dist) * maxRadius
          } else {
            knobX = nx
            knobY = ny
          }
          onMove(knobX / maxRadius, knobY / maxRadius)
        }
      },
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .offset { IntOffset(knobX.roundToInt(), knobY.roundToInt()) }
        .size(46.dp)
        .clip(CircleShape)
        .background(EmberOrange.copy(alpha = 0.9f))
        .border(2.dp, BrightFlame, CircleShape),
    )
  }
}
