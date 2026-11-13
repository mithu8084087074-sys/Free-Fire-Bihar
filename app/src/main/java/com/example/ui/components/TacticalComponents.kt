package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArmorCard
import com.example.ui.theme.BrightFlame
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.GoldRank
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SteelBorder
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary

@Composable
fun TacticalCard(
  modifier: Modifier = Modifier,
  borderColor: Color = SteelBorder,
  containerColor: Color = ArmorCard.copy(alpha = 0.92f),
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    modifier = modifier.then(
      if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ),
    shape = RoundedCornerShape(12.dp),
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
    tonalElevation = 4.dp,
  ) {
    Column(modifier = Modifier.padding(12.dp), content = content)
  }
}

@Composable
fun EmberActionButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  accentColor: Color = EmberOrange,
  testTag: String = "",
) {
  val gradient = if (enabled) {
    Brush.horizontalGradient(listOf(accentColor, BrightFlame))
  } else {
    Brush.horizontalGradient(listOf(Color(0xFF2B3240), Color(0xFF2B3240)))
  }
  Box(
    modifier = modifier
      .defaultMinSize(minHeight = 48.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(gradient)
      .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (enabled) ObsidianVoid else Color.Gray,
          modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) ObsidianVoid else Color.Gray,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun CurrencyPill(
  label: String,
  value: Long,
  tint: Color,
  icon: ImageVector,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = ObsidianVoid.copy(alpha = 0.84f),
    border = BorderStroke(1.dp, tint.copy(alpha = 0.5f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
      Spacer(Modifier.width(5.dp))
      Text(
        text = value.toString(),
        style = MaterialTheme.typography.labelLarge,
        color = TextPrimary,
      )
    }
  }
}

@Composable
fun RarityBadge(rarity: String) {
  val color = when (rarity.lowercase()) {
    "legendary", "hero" -> GoldRank
    "epic" -> EmberOrange
    "rare" -> TacticalCyan
    else -> Color(0xFF90A4AE)
  }
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = color.copy(alpha = 0.16f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.65f)),
  ) {
    Text(
      text = rarity.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      color = color,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
    )
  }
}
