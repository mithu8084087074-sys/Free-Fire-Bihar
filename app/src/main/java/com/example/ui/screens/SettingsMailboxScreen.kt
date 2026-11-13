package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GraphicsTier
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsMailboxScreen(
  viewModel: GameViewModel,
  initialTab: Int = 0,
  onNavigateOwnerPanel: () -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val profile by viewModel.profile.collectAsStateWithLifecycle()
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val mailbox by viewModel.mailbox.collectAsStateWithLifecycle()
  val isOwner by viewModel.isOwnerAuthorized.collectAsStateWithLifecycle()

  var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
  val tabs = listOf("GAME SETTINGS", "MAILBOX (${mailbox.size})")

  var callsignInput by remember(profile?.username) { mutableStateOf(profile?.username ?: "") }

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
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("settings_back")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        Text("SETTINGS & SUPPLY MAILBOX", style = MaterialTheme.typography.headlineMedium, color = BrightFlame)
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

      if (selectedTab == 0) {
        Column(
          modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Callsign Card
          TacticalCard {
            Text("OPERATIVE CALLSIGN", style = MaterialTheme.typography.titleMedium, color = TacticalCyan)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = callsignInput,
                onValueChange = { callsignInput = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              Button(
                onClick = { viewModel.updateUsername(callsignInput) },
                colors = ButtonDefaults.buttonColors(containerColor = EmberOrange, contentColor = ObsidianVoid),
              ) { Text("SAVE") }
            }
          }

          // Graphics Quality Card
          TacticalCard {
            Text("3D GRAPHICS & PERFORMANCE", style = MaterialTheme.typography.titleMedium, color = BrightFlame)
            Spacer(Modifier.height(8.dp))
            Text("Quality Preset", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              GraphicsTier.entries.forEach { tier ->
                val selected = settings.graphicsTier == tier
                Surface(
                  modifier = Modifier
                    .weight(1f)
                    .clickable {
                      viewModel.updateSettings(
                        settings.copy(
                          graphicsTier = tier,
                          targetFps = tier.targetFps,
                          shadowsEnabled = tier.shadowDetail > 0,
                          textureQuality = tier.label,
                        )
                      )
                    },
                  shape = RoundedCornerShape(8.dp),
                  color = if (selected) EmberOrange else ObsidianVoid,
                  border = BorderStroke(1.dp, if (selected) BrightFlame else SteelBorder),
                ) {
                  Text(
                    text = tier.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) ObsidianVoid else TextPrimary,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                  )
                }
              }
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text("Dynamic Shadows", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
              Switch(
                checked = settings.shadowsEnabled,
                onCheckedChange = { viewModel.updateSettings(settings.copy(shadowsEnabled = it)) },
              )
            }
          }

          // Audio & Controls Card
          TacticalCard {
            Text("AUDIO & TOUCH CONTROLS", style = MaterialTheme.typography.titleMedium, color = TacticalCyan)
            Spacer(Modifier.height(6.dp))
            SliderSetting("Master Volume", settings.masterVolume, 0f..1f) {
              viewModel.updateSettings(settings.copy(masterVolume = it))
            }
            SliderSetting("Music Volume", settings.musicVolume, 0f..1f) {
              viewModel.updateSettings(settings.copy(musicVolume = it))
            }
            SliderSetting("SFX Volume", settings.sfxVolume, 0f..1f) {
              viewModel.updateSettings(settings.copy(sfxVolume = it))
            }
            SliderSetting("Aim / Look Sensitivity", settings.touchSensitivity, 0.5f..2.0f) {
              viewModel.updateSettings(settings.copy(touchSensitivity = it))
            }

            Spacer(Modifier.height(6.dp))
            Text("Control Layout Preset", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf("Classic Thumb", "Claw 3-Finger", "Pro HUD").forEach { preset ->
                val sel = settings.controlPreset == preset
                Surface(
                  modifier = Modifier.weight(1f).clickable { viewModel.updateSettings(settings.copy(controlPreset = preset)) },
                  shape = RoundedCornerShape(8.dp),
                  color = if (sel) TacticalCyan else ObsidianVoid,
                  border = BorderStroke(1.dp, SteelBorder),
                ) {
                  Text(
                    text = preset,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sel) ObsidianVoid else TextPrimary,
                    modifier = Modifier.padding(8.dp),
                  )
                }
              }
            }
          }

          if (isOwner) {
            TacticalCard(borderColor = GoldRank) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldRank)
                  Spacer(Modifier.width(8.dp))
                  Column {
                    Text("OWNER CONTROL PANEL (RAVI)", style = MaterialTheme.typography.titleMedium, color = GoldRank)
                    Text("Super Admin security & live game console", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                  }
                }
                EmberActionButton(
                  text = "OPEN",
                  accentColor = GoldRank,
                  onClick = onNavigateOwnerPanel,
                  testTag = "settings_open_owner",
                )
              }
            }
          }

          OutlinedButton(
            onClick = {
              scope.launch {
                try {
                  withContext(NonCancellable) {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                  }
                } catch (_: Exception) {
                }
                Firebase.auth.signOut()
              }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button"),
            border = BorderStroke(1.dp, DangerCrimson),
          ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", tint = DangerCrimson)
            Spacer(Modifier.width(8.dp))
            Text("SIGN OUT OF GOOGLE ACCOUNT", color = DangerCrimson)
          }
        }
      } else {
        if (mailbox.isEmpty()) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Your tactical mailbox is empty.", color = TextSecondary)
          }
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(mailbox, key = { it.id }) { mail ->
              TacticalCard(borderColor = if (!mail.claimed) GoldRank else SteelBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Mail, contentDescription = null, tint = GoldRank)
                  Spacer(Modifier.width(8.dp))
                  Text(mail.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Spacer(Modifier.height(4.dp))
                Text(mail.body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                if (mail.rewardCoins > 0L || mail.rewardDiamonds > 0L) {
                  Spacer(Modifier.height(6.dp))
                  Text("Attached: +${mail.rewardCoins} Coins, +${mail.rewardDiamonds} Diamonds", style = MaterialTheme.typography.labelLarge, color = GoldRank)
                }
                Spacer(Modifier.height(8.dp))
                if (mail.claimed) {
                  Text("COLLECTED", style = MaterialTheme.typography.labelLarge, color = HealthGreen)
                } else {
                  EmberActionButton(
                    text = "CLAIM SUPPLY DROP",
                    icon = Icons.Default.CardGiftcard,
                    onClick = { viewModel.claimMail(mail) },
                    testTag = "claim_mail_${mail.id}",
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SliderSetting(
  label: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
) {
  Column {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
      Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
    }
    Slider(
      value = value.coerceIn(range.start, range.endInclusive),
      onValueChange = onValueChange,
      valueRange = range,
      colors = SliderDefaults.colors(thumbColor = EmberOrange, activeTrackColor = EmberOrange),
    )
  }
}
