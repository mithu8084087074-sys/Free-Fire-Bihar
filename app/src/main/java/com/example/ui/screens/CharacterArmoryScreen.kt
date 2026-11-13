package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.GameViewModel
import com.example.ui.components.CurrencyPill
import com.example.ui.components.EmberActionButton
import com.example.ui.components.RarityBadge
import com.example.ui.components.TacticalCard
import com.example.ui.theme.ArmorCard
import com.example.ui.theme.BrightFlame
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.GoldRank
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CharacterArmoryScreen(
  viewModel: GameViewModel,
  initialTab: Int = 0,
  onBack: () -> Unit,
) {
  val profile by viewModel.profile.collectAsStateWithLifecycle()
  val characters by viewModel.characters.collectAsStateWithLifecycle()
  val weapons by viewModel.weapons.collectAsStateWithLifecycle()
  val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
  val inventory by viewModel.inventory.collectAsStateWithLifecycle()

  var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
  val tabs = listOf("CHARACTERS", "WEAPONS", "VEHICLES & GEAR")

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
          IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("armory_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
          }
          Text("ARMORY & ROSTER", style = MaterialTheme.typography.headlineMedium, color = BrightFlame)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          CurrencyPill("Coins", profile?.coins ?: 0L, GoldRank, Icons.Default.MonetizationOn)
          CurrencyPill("Diamonds", profile?.diamonds ?: 0L, TacticalCyan, Icons.Default.Diamond)
        }
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
            text = { Text(label, style = MaterialTheme.typography.labelLarge) },
          )
        }
      }

      Spacer(Modifier.height(10.dp))

      when (selectedTab) {
        0 -> {
          val activeChars = remember(characters) { characters.filter { it.enabled } }
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
              Surface(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SteelBorder),
                modifier = Modifier.fillMaxWidth().height(120.dp),
              ) {
                Box {
                  Image(
                    painter = painterResource(id = R.drawable.img_character_showcase),
                    contentDescription = "Character Showcase",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                  )
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(ObsidianVoid.copy(alpha = 0.55f))
                      .padding(14.dp),
                    contentAlignment = Alignment.BottomStart,
                  ) {
                    Text(
                      text = "SELECT & UPGRADE ELITE OPERATIVES",
                      style = MaterialTheme.typography.titleLarge,
                      color = TextPrimary,
                    )
                  }
                }
              }
            }
            items(activeChars, key = { it.id }) { char ->
              val invItem = inventory.firstOrNull { it.itemId == char.id }
              val isFree = char.priceCoins == 0L && char.priceDiamonds == 0L
              val isOwned = invItem != null || isFree
              val isEquipped = profile?.equippedCharacterId == char.id

              TacticalCard(
                borderColor = if (isEquipped) EmberOrange else SteelBorder,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Box(
                    modifier = Modifier
                      .size(54.dp)
                      .clip(CircleShape)
                      .background(Color(char.primaryColorHex.toInt()))
                      .padding(6.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    Box(
                      modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(char.accentColorHex.toInt()))
                    )
                  }
                  Spacer(Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text("${char.name} • ${char.gender}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                      Spacer(Modifier.width(8.dp))
                      if (invItem != null) {
                        Text("LV.${invItem.level}", style = MaterialTheme.typography.labelSmall, color = GoldRank)
                      }
                    }
                    Text("${char.title} | ${char.skillName}", style = MaterialTheme.typography.labelLarge, color = TacticalCyan)
                    Text(char.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("Style: ${char.hairStyle} • ${char.outfitStyle}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                  }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  if (isOwned) {
                    if (isEquipped) {
                      Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HealthGreen.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, HealthGreen),
                      ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                          Icon(Icons.Default.CheckCircle, contentDescription = "Equipped", tint = HealthGreen, modifier = Modifier.size(18.dp))
                          Spacer(Modifier.width(6.dp))
                          Text("EQUIPPED", style = MaterialTheme.typography.labelLarge, color = HealthGreen)
                        }
                      }
                    } else {
                      EmberActionButton(
                        text = "EQUIP",
                        onClick = { viewModel.equipLoadout(characterId = char.id) },
                        testTag = "equip_char_${char.id}",
                      )
                    }
                    if (invItem != null) {
                      val cost = 200L * invItem.level
                      OutlinedButton(
                        onClick = { viewModel.upgradeInventoryItem(invItem) },
                        border = BorderStroke(1.dp, GoldRank),
                      ) {
                        Icon(Icons.Default.Upgrade, contentDescription = null, tint = GoldRank, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("UPGRADE ($cost C)", color = GoldRank)
                      }
                    }
                  } else {
                    val costLabel = if (char.priceDiamonds > 0L) "RECRUIT (${char.priceDiamonds} D)" else "RECRUIT (${char.priceCoins} C)"
                    EmberActionButton(
                      text = costLabel,
                      accentColor = TacticalCyan,
                      onClick = { viewModel.purchaseCharacter(char) },
                      testTag = "recruit_char_${char.id}",
                    )
                  }
                }
              }
            }
          }
        }

        1 -> {
          val activeWeapons = remember(weapons) { weapons.filter { it.enabled } }
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(activeWeapons, key = { it.id }) { w ->
              val isEquipped = profile?.equippedWeaponId == w.id
              val invItem = inventory.firstOrNull { it.itemId == w.id }
              TacticalCard(borderColor = if (isEquipped) EmberOrange else SteelBorder) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(Modifier.weight(1f)) {
                    Text(w.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("${w.category} • Attachments: ${w.attachments}", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                  }
                  if (invItem != null) {
                    RarityBadge("LV.${invItem.level}")
                  }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("DMG: ${w.damage}", style = MaterialTheme.typography.bodyMedium, color = BrightFlame)
                  Text("RNG: ${w.rangeMeters}m", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                  Text("MAG: ${w.magazineSize}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                  Text("ROF: ${w.fireRateMs}ms", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  if (isEquipped) {
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = HealthGreen.copy(alpha = 0.16f),
                      border = BorderStroke(1.dp, HealthGreen),
                    ) {
                      Text("PRIMARY EQUIPPED", style = MaterialTheme.typography.labelLarge, color = HealthGreen, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                  } else {
                    EmberActionButton(
                      text = "EQUIP PRIMARY",
                      onClick = { viewModel.equipLoadout(weaponId = w.id) },
                      testTag = "equip_weapon_${w.id}",
                    )
                  }
                  if (invItem != null) {
                    OutlinedButton(onClick = { viewModel.upgradeInventoryItem(invItem) }, border = BorderStroke(1.dp, GoldRank)) {
                      Text("UPGRADE (${200L * invItem.level} C)", color = GoldRank)
                    }
                  }
                }
              }
            }
          }
        }

        else -> {
          val activeVehicles = remember(vehicles) { vehicles.filter { it.enabled } }
          val gearItems = remember(inventory) {
            inventory.filter { it.category.lowercase() in listOf("outfit", "skin", "parachute", "pet", "vehicle", "emote") }
          }
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
              Text("DEPLOYABLE VEHICLES", style = MaterialTheme.typography.titleMedium, color = TacticalCyan)
            }
            items(activeVehicles, key = { it.id }) { v ->
              val isEquipped = profile?.equippedVehicleId == v.id
              TacticalCard(borderColor = if (isEquipped) EmberOrange else SteelBorder) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(v.colorHex.toInt()), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                      Text(v.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                      Text("${v.vehicleType} • Speed: ${v.topSpeed.toInt()} m/s • HP: ${v.maxHp} • Seats: ${v.seats}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                  }
                  Button(
                    onClick = { viewModel.equipLoadout(vehicleId = v.id) },
                    enabled = !isEquipped,
                    colors = ButtonDefaults.buttonColors(containerColor = EmberOrange, contentColor = ObsidianVoid),
                  ) {
                    Text(if (isEquipped) "ACTIVE" else "SET SKIN")
                  }
                }
              }
            }
            item {
              Spacer(Modifier.height(6.dp))
              Text("OWNED OUTFITS, PARACHUTES & PETS", style = MaterialTheme.typography.titleMedium, color = BrightFlame)
            }
            items(gearItems, key = { it.id }) { item ->
              val isEquipped = profile?.equippedOutfitId == item.itemId ||
                profile?.equippedParachuteId == item.itemId ||
                profile?.equippedPetId == item.itemId
              TacticalCard(borderColor = if (isEquipped) HealthGreen else SteelBorder) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Security, contentDescription = null, tint = EmberOrange, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(6.dp))
                      Text(item.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    Text("${item.category.uppercase()} • ${item.rarity} • Lv.${item.level}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { viewModel.upgradeInventoryItem(item) }) {
                      Text("UP (${200L * item.level}C)", color = GoldRank)
                    }
                    if (!isEquipped && item.category.lowercase() in listOf("outfit", "skin", "parachute", "pet")) {
                      Button(
                        onClick = {
                          when (item.category.lowercase()) {
                            "parachute" -> viewModel.equipLoadout(parachuteId = item.itemId)
                            "pet" -> viewModel.equipLoadout(petId = item.itemId)
                            else -> viewModel.equipLoadout(outfitId = item.itemId)
                          }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmberOrange, contentColor = ObsidianVoid),
                      ) {
                        Text("EQUIP")
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
