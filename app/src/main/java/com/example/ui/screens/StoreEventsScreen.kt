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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
fun StoreEventsScreen(
  viewModel: GameViewModel,
  initialTab: Int = 0,
  onBack: () -> Unit,
) {
  val profile by viewModel.profile.collectAsStateWithLifecycle()
  val storeItems by viewModel.storeItems.collectAsStateWithLifecycle()
  val events by viewModel.events.collectAsStateWithLifecycle()
  val missions by viewModel.missions.collectAsStateWithLifecycle()
  val inventory by viewModel.inventory.collectAsStateWithLifecycle()
  val transactions by viewModel.transactions.collectAsStateWithLifecycle()

  var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
  val tabs = listOf("STORE", "EVENTS", "MISSIONS", "HISTORY")

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
          IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("store_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
          }
          Text("STORE & MISSIONS", style = MaterialTheme.typography.headlineMedium, color = BrightFlame)
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
            text = { Text(label, style = MaterialTheme.typography.labelSmall) },
          )
        }
      }

      Spacer(Modifier.height(10.dp))

      when (selectedTab) {
        0 -> {
          val activeItems = remember(storeItems) { storeItems.filter { it.enabled } }
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(activeItems, key = { it.id }) { item ->
              val isOwned = inventory.any { it.itemId == item.id }
              TacticalCard(borderColor = Color(item.colorHex.toInt()).copy(alpha = 0.65f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color(item.colorHex.toInt()), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.width(6.dp))
                        RarityBadge(item.rarity)
                      }
                      Text("${item.category} • ${item.description}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                  }
                }
                Spacer(Modifier.height(10.dp))
                if (isOwned) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HealthGreen.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, HealthGreen),
                  ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.CheckCircle, contentDescription = "Owned", tint = HealthGreen, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.width(6.dp))
                      Text("OWNED IN ARMORY", style = MaterialTheme.typography.labelLarge, color = HealthGreen)
                    }
                  }
                } else {
                  val priceText = buildString {
                    if (item.priceCoins > 0) append("${item.priceCoins} COINS")
                    if (item.priceCoins > 0 && item.priceDiamonds > 0) append(" + ")
                    if (item.priceDiamonds > 0) append("${item.priceDiamonds} DIAMONDS")
                    if (item.priceCoins == 0L && item.priceDiamonds == 0L) append("FREE CLAIM")
                  }
                  EmberActionButton(
                    text = "BUY • $priceText",
                    onClick = { viewModel.purchaseStoreItem(item) },
                    testTag = "buy_store_${item.id}",
                  )
                }
              }
            }
          }
        }

        1 -> {
          val activeEvents = remember(events) { events.filter { it.enabled } }
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(activeEvents, key = { it.id }) { ev ->
              TacticalCard(borderColor = GoldRank.copy(alpha = 0.6f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldRank, modifier = Modifier.size(28.dp))
                  Spacer(Modifier.width(10.dp))
                  Column(Modifier.weight(1f)) {
                    Text(ev.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(ev.eventType.uppercase(), style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                  }
                }
                Spacer(Modifier.height(6.dp))
                Text(ev.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Text("REWARD: ${ev.rewardSummary}", style = MaterialTheme.typography.labelLarge, color = GoldRank)
              }
            }
          }
        }

        2 -> {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(missions, key = { it.id }) { m ->
              val readyToClaim = m.progress >= m.target && !m.claimed
              TacticalCard(borderColor = if (readyToClaim) HealthGreen else SteelBorder) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Column(Modifier.weight(1f)) {
                    Text(m.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Reward: +${m.rewardCoins} Coins, +${m.rewardDiamonds} Diamonds", style = MaterialTheme.typography.labelSmall, color = GoldRank)
                  }
                  Text("${m.progress.coerceAtMost(m.target)}/${m.target}", style = MaterialTheme.typography.labelLarge, color = TacticalCyan)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                  progress = { (m.progress.toFloat() / m.target.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f) },
                  modifier = Modifier.fillMaxWidth().height(8.dp),
                  color = if (readyToClaim) HealthGreen else EmberOrange,
                  trackColor = ObsidianVoid,
                )
                Spacer(Modifier.height(8.dp))
                when {
                  m.claimed -> Text("REWARD CLAIMED", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                  readyToClaim -> EmberActionButton(
                    text = "CLAIM REWARD",
                    icon = Icons.Default.CardGiftcard,
                    onClick = { viewModel.claimMission(m) },
                    testTag = "claim_mission_${m.missionId}",
                  )
                  else -> Text("IN PROGRESS — Play matches to complete", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
              }
            }
          }
        }

        else -> {
          if (transactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text("No store purchases or upgrades recorded yet.", color = TextSecondary)
            }
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              items(transactions, key = { it.id }) { tx ->
                TacticalCard {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                      Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TacticalCyan)
                      Spacer(Modifier.width(10.dp))
                      Column {
                        Text(tx.itemName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(tx.type.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                      }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                      if (tx.coinsDelta != 0L) Text("${tx.coinsDelta} C", style = MaterialTheme.typography.labelLarge, color = GoldRank)
                      if (tx.diamondsDelta != 0L) Text("${tx.diamondsDelta} D", style = MaterialTheme.typography.labelLarge, color = TacticalCyan)
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
