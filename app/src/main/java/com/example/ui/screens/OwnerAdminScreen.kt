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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CatalogCharacter
import com.example.data.model.CatalogMap
import com.example.data.model.CatalogStoreItem
import com.example.data.model.CatalogVehicle
import com.example.data.model.CatalogWeapon
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
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OwnerAdminScreen(
  viewModel: GameViewModel,
  onBack: () -> Unit,
) {
  val isOwnerAuthorized by viewModel.isOwnerAuthorized.collectAsStateWithLifecycle()
  val ownerUnlocked by viewModel.ownerUnlocked.collectAsStateWithLifecycle()
  val securityDoc by viewModel.ownerSecurityDoc.collectAsStateWithLifecycle()

  LaunchedEffect(isOwnerAuthorized) {
    if (isOwnerAuthorized) {
      viewModel.loadOwnerSecurity()
    }
  }

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
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("owner_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
          }
          Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldRank)
          Spacer(Modifier.width(8.dp))
          Text("OWNER PANEL • RAVI", style = MaterialTheme.typography.headlineMedium, color = GoldRank)
        }
        if (ownerUnlocked) {
          Button(
            onClick = { viewModel.lockOwnerPanel() },
            colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson, contentColor = TextPrimary),
          ) {
            Icon(Icons.Default.Lock, contentDescription = "Lock Panel", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("LOCK")
          }
        }
      }

      Spacer(Modifier.height(10.dp))

      if (!isOwnerAuthorized) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          TacticalCard(borderColor = DangerCrimson, modifier = Modifier.widthIn(max = 460.dp)) {
            Text("ACCESS DENIED", style = MaterialTheme.typography.headlineMedium, color = DangerCrimson)
            Spacer(Modifier.height(6.dp))
            Text(
              "Only the verified Owner account (RAVI) is authorized by Firestore Role-Based Access Control.",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
          }
        }
      } else if (!ownerUnlocked) {
        var username by remember { mutableStateOf("RAVI") }
        var passkey by remember { mutableStateOf("") }
        var pin2fa by remember { mutableStateOf("") }
        var newPasskey by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        val mustChange = securityDoc?.mustChangeInitial ?: true

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          TacticalCard(borderColor = GoldRank, modifier = Modifier.widthIn(max = 480.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Security, contentDescription = null, tint = GoldRank)
              Spacer(Modifier.width(8.dp))
              Text("SUPER ADMIN 2FA GATE", style = MaterialTheme.typography.titleLarge, color = GoldRank)
            }
            Spacer(Modifier.height(8.dp))
            Text(
              if (mustChange) {
                "First-Login Security Setup: Initialize your encrypted Owner Passkey (6+ chars) and 6-digit 2FA PIN."
              } else {
                "Enter Owner Username, Passkey, and 6-digit 2FA PIN. Failed attempts: ${securityDoc?.failedAttempts ?: 0}"
              },
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
              value = username,
              onValueChange = { username = it },
              label = { Text("Owner Username (RAVI)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (mustChange) {
              OutlinedTextField(
                value = newPasskey,
                onValueChange = { newPasskey = it },
                label = { Text("Set New Owner Passkey (6+ chars)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              Spacer(Modifier.height(8.dp))
              OutlinedTextField(
                value = newPin,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
                label = { Text("Set 6-Digit 2FA PIN") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            } else {
              OutlinedTextField(
                value = passkey,
                onValueChange = { passkey = it },
                label = { Text("Owner Passkey") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              Spacer(Modifier.height(8.dp))
              OutlinedTextField(
                value = pin2fa,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin2fa = it },
                label = { Text("6-Digit 2FA PIN") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            }
            Spacer(Modifier.height(14.dp))
            EmberActionButton(
              text = if (mustChange) "INITIALIZE & UNLOCK" else "VERIFY 2FA & UNLOCK",
              accentColor = GoldRank,
              modifier = Modifier.fillMaxWidth(),
              testTag = "owner_unlock_button",
              onClick = {
                viewModel.unlockOwnerPanel(username, passkey, pin2fa, newPasskey, newPin)
              },
            )
          }
        }
      } else {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("CONTROL", "HEROES & GUNS", "MAPS & STORE", "MUSIC", "PLAYERS", "UPDATES & LOGS")

        ScrollableTabRow(
          selectedTabIndex = selectedTab,
          containerColor = ArmorCard,
          contentColor = GoldRank,
          edgePadding = 4.dp,
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
          0 -> OwnerControlTab(viewModel)
          1 -> OwnerHeroesWeaponsTab(viewModel)
          2 -> OwnerMapsStoreTab(viewModel)
          3 -> OwnerAudioTab(viewModel)
          4 -> OwnerModerationTab(viewModel)
          else -> OwnerUpdatesAuditTab(viewModel)
        }
      }
    }
  }
}

@Composable
private fun OwnerControlTab(viewModel: GameViewModel) {
  val config by viewModel.globalConfig.collectAsStateWithLifecycle()
  val allPlayers by viewModel.allPlayersForOwner.collectAsStateWithLifecycle()
  val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
  val rooms by viewModel.availableRooms.collectAsStateWithLifecycle()
  val reports by viewModel.allReportsForOwner.collectAsStateWithLifecycle()

  var maintenance by remember(config) { mutableStateOf(config.maintenanceMode) }
  var maintMsg by remember(config) { mutableStateOf(config.maintenanceMessage) }
  var announcement by remember(config) { mutableStateOf(config.announcement) }
  var maxPlayers by remember(config) { mutableStateOf(config.maxPlayersPerMatch.toString()) }
  var enabledModes by remember(config) {
    mutableStateOf(config.enabledModes.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet())
  }

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    TacticalCard(borderColor = GoldRank) {
      Text("LIVE SERVER TELEMETRY", style = MaterialTheme.typography.titleMedium, color = GoldRank)
      Spacer(Modifier.height(8.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        MetricBox("PLAYERS", "${allPlayers.size}")
        MetricBox("ONLINE", "${leaderboard.count { it.isOnline }}")
        MetricBox("ROOMS", "${rooms.size}")
        MetricBox("OPEN REPORTS", "${reports.count { it.status == "open" }}")
      }
    }

    TacticalCard {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
          Text("MAINTENANCE MODE", style = MaterialTheme.typography.titleMedium, color = DangerCrimson)
          Text("Blocks non-owner matchmaking", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Switch(checked = maintenance, onCheckedChange = { maintenance = it })
      }
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = maintMsg,
        onValueChange = { maintMsg = it },
        label = { Text("Maintenance Banner Message") },
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = announcement,
        onValueChange = { announcement = it },
        label = { Text("Live Lobby Announcement Ticker") },
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = maxPlayers,
        onValueChange = { if (it.all(Char::isDigit)) maxPlayers = it },
        label = { Text("Max Players Per Match (2..50)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(8.dp))
      Text("ENABLED GAME MODES", style = MaterialTheme.typography.labelLarge, color = TacticalCyan)
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("solo", "duo", "squad", "ranked", "classic").forEach { mode ->
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
              checked = mode in enabledModes,
              onCheckedChange = { checked ->
                enabledModes = if (checked) enabledModes + mode else enabledModes - mode
              },
            )
            Text(mode.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextPrimary)
          }
        }
      }
      Spacer(Modifier.height(10.dp))
      EmberActionButton(
        text = "SAVE GLOBAL SERVER CONFIG",
        accentColor = GoldRank,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
          val maxP = (maxPlayers.toLongOrNull() ?: 50L).coerceIn(2L, 50L)
          viewModel.adminSaveGlobalConfig(
            config.copy(
              maintenanceMode = maintenance,
              maintenanceMessage = maintMsg,
              announcement = announcement,
              maxPlayersPerMatch = maxP,
              enabledModes = (enabledModes + "training").joinToString(","),
            )
          )
        },
      )
    }
  }
}

@Composable
private fun OwnerHeroesWeaponsTab(viewModel: GameViewModel) {
  val characters by viewModel.characters.collectAsStateWithLifecycle()
  val weapons by viewModel.weapons.collectAsStateWithLifecycle()

  var cName by remember { mutableStateOf("") }
  var cTitle by remember { mutableStateOf("") }
  var cGender by remember { mutableStateOf("Male") }
  var cSkill by remember { mutableStateOf("") }
  var cCoins by remember { mutableStateOf("500") }

  var wName by remember { mutableStateOf("") }
  var wCat by remember { mutableStateOf("Assault Rifle") }
  var wDmg by remember { mutableStateOf("40") }
  var wRng by remember { mutableStateOf("150") }
  var wMag by remember { mutableStateOf("35") }

  LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item {
      TacticalCard(borderColor = EmberOrange) {
        Text("ADD / PUBLISH NEW 3D CHARACTER", style = MaterialTheme.typography.titleMedium, color = BrightFlame)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = cName, onValueChange = { cName = it }, label = { Text("Character Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = cTitle, onValueChange = { cTitle = it }, label = { Text("Title / Role") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = cSkill, onValueChange = { cSkill = it }, label = { Text("Combat Skill") }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = cCoins,
            onValueChange = { if (it.all(Char::isDigit)) cCoins = it },
            label = { Text("Coins Cost") },
            modifier = Modifier.weight(1f),
          )
          Button(onClick = { cGender = if (cGender == "Male") "Female" else "Male" }) {
            Text(cGender)
          }
        }
        Spacer(Modifier.height(8.dp))
        EmberActionButton(
          text = "PUBLISH CHARACTER",
          modifier = Modifier.fillMaxWidth(),
          onClick = {
            if (cName.isNotBlank()) {
              viewModel.adminSaveCharacter(
                CatalogCharacter(
                  name = cName.trim(),
                  title = cTitle.ifBlank { "Bihar Elite" },
                  gender = cGender,
                  skillName = cSkill.ifBlank { "Tactical Surge" },
                  description = "Custom operative deployed by Commander RAVI.",
                  priceCoins = cCoins.toLongOrNull() ?: 500L,
                  enabled = true,
                )
              )
              cName = ""; cTitle = ""; cSkill = ""
            }
          },
        )
      }
    }

    items(characters, key = { "adm_c_${it.id}" }) { c ->
      TacticalCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text("${c.name} (${c.gender})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("${c.skillName} • ${c.priceCoins} C / ${c.priceDiamonds} D", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
          }
          IconButton(onClick = { viewModel.adminDeleteCatalog("catalog_characters", c.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Character", tint = DangerCrimson)
          }
        }
      }
    }

    item {
      TacticalCard(borderColor = TacticalCyan) {
        Text("ADD / PUBLISH NEW WEAPON", style = MaterialTheme.typography.titleMedium, color = TacticalCyan)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = wName, onValueChange = { wName = it }, label = { Text("Weapon Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = wCat, onValueChange = { wCat = it }, label = { Text("Category (Assault Rifle/SMG/Sniper)") }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedTextField(value = wDmg, onValueChange = { if (it.all(Char::isDigit)) wDmg = it }, label = { Text("DMG") }, modifier = Modifier.weight(1f))
          OutlinedTextField(value = wRng, onValueChange = { if (it.all(Char::isDigit)) wRng = it }, label = { Text("Range") }, modifier = Modifier.weight(1f))
          OutlinedTextField(value = wMag, onValueChange = { if (it.all(Char::isDigit)) wMag = it }, label = { Text("Mag") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        EmberActionButton(
          text = "PUBLISH WEAPON",
          accentColor = TacticalCyan,
          modifier = Modifier.fillMaxWidth(),
          onClick = {
            if (wName.isNotBlank()) {
              viewModel.adminSaveWeapon(
                CatalogWeapon(
                  name = wName.trim(),
                  category = wCat.ifBlank { "Assault Rifle" },
                  damage = wDmg.toLongOrNull() ?: 35L,
                  rangeMeters = wRng.toLongOrNull() ?: 140L,
                  magazineSize = wMag.toLongOrNull() ?: 30L,
                  enabled = true,
                )
              )
              wName = ""
            }
          },
        )
      }
    }

    items(weapons, key = { "adm_w_${it.id}" }) { w ->
      TacticalCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text(w.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("${w.category} • DMG ${w.damage} • RNG ${w.rangeMeters}m", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
          }
          IconButton(onClick = { viewModel.adminDeleteCatalog("catalog_weapons", w.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Weapon", tint = DangerCrimson)
          }
        }
      }
    }
  }
}

@Composable
private fun OwnerMapsStoreTab(viewModel: GameViewModel) {
  val maps by viewModel.maps.collectAsStateWithLifecycle()
  val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
  val storeItems by viewModel.storeItems.collectAsStateWithLifecycle()

  var mName by remember { mutableStateOf("") }
  var mBiome by remember { mutableStateOf("") }
  var vName by remember { mutableStateOf("") }
  var vType by remember { mutableStateOf("Jeep") }
  var sName by remember { mutableStateOf("") }
  var sCoins by remember { mutableStateOf("600") }

  LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item {
      TacticalCard {
        Text("ADD MAP LOCATION", style = MaterialTheme.typography.titleMedium, color = BrightFlame)
        OutlinedTextField(value = mName, onValueChange = { mName = it }, label = { Text("Map Name (e.g. Nalanda Ruins)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = mBiome, onValueChange = { mBiome = it }, label = { Text("Biome / Landmarks") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        EmberActionButton(
          text = "PUBLISH MAP",
          onClick = {
            if (mName.isNotBlank()) {
              viewModel.adminSaveMap(CatalogMap(name = mName.trim(), biome = mBiome.ifBlank { "Tactical Valley" }, enabled = true))
              mName = ""; mBiome = ""
            }
          },
        )
      }
    }
    items(maps, key = { "adm_m_${it.id}" }) { m ->
      TacticalCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text(m.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("${m.biome} • ${if (m.enabled) "LIVE" else "DISABLED"}", style = MaterialTheme.typography.labelSmall, color = if (m.enabled) HealthGreen else DangerCrimson)
          }
          Row {
            Switch(checked = m.enabled, onCheckedChange = { viewModel.adminSaveMap(m.copy(enabled = it)) })
            IconButton(onClick = { viewModel.adminDeleteCatalog("catalog_maps", m.id) }) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Map", tint = DangerCrimson)
            }
          }
        }
      }
    }

    item {
      TacticalCard {
        Text("ADD VEHICLE", style = MaterialTheme.typography.titleMedium, color = TacticalCyan)
        OutlinedTextField(value = vName, onValueChange = { vName = it }, label = { Text("Vehicle Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = vType, onValueChange = { vType = it }, label = { Text("Type (Jeep / Motorcycle / Armored Car)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        EmberActionButton(
          text = "PUBLISH VEHICLE",
          accentColor = TacticalCyan,
          onClick = {
            if (vName.isNotBlank()) {
              viewModel.adminSaveVehicle(CatalogVehicle(name = vName.trim(), vehicleType = vType.ifBlank { "Jeep" }, enabled = true))
              vName = ""
            }
          },
        )
      }
    }
    items(vehicles, key = { "adm_v_${it.id}" }) { v ->
      TacticalCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("${v.name} (${v.vehicleType})", color = TextPrimary)
          IconButton(onClick = { viewModel.adminDeleteCatalog("catalog_vehicles", v.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Vehicle", tint = DangerCrimson)
          }
        }
      }
    }

    item {
      TacticalCard {
        Text("ADD STORE SKIN / OUTFIT", style = MaterialTheme.typography.titleMedium, color = GoldRank)
        OutlinedTextField(value = sName, onValueChange = { sName = it }, label = { Text("Skin / Bundle Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = sCoins, onValueChange = { if (it.all(Char::isDigit)) sCoins = it }, label = { Text("Price (Coins)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        EmberActionButton(
          text = "PUBLISH TO STORE",
          accentColor = GoldRank,
          onClick = {
            if (sName.isNotBlank()) {
              viewModel.adminSaveStoreItem(CatalogStoreItem(name = sName.trim(), priceCoins = sCoins.toLongOrNull() ?: 600L, enabled = true))
              sName = ""
            }
          },
        )
      }
    }
    items(storeItems, key = { "adm_s_${it.id}" }) { s ->
      TacticalCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("${s.name} • ${s.priceCoins} C / ${s.priceDiamonds} D", color = TextPrimary)
          IconButton(onClick = { viewModel.adminDeleteCatalog("catalog_store", s.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Store Item", tint = DangerCrimson)
          }
        }
      }
    }
  }
}

@Composable
internal fun MetricBox(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, style = MaterialTheme.typography.titleLarge, color = BrightFlame)
    Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
  }
}
