package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TacticalDarkColorScheme = darkColorScheme(
  primary = EmberOrange,
  onPrimary = ObsidianVoid,
  primaryContainer = Color(0xFF421C00),
  onPrimaryContainer = BrightFlame,
  secondary = TacticalCyan,
  onSecondary = ObsidianVoid,
  secondaryContainer = Color(0xFF003640),
  onSecondaryContainer = TacticalCyan,
  tertiary = GoldRank,
  onTertiary = ObsidianVoid,
  background = ObsidianVoid,
  onBackground = TextPrimary,
  surface = GunmetalSurface,
  onSurface = TextPrimary,
  surfaceVariant = ArmorCard,
  onSurfaceVariant = TextSecondary,
  outline = SteelBorder,
  error = DangerCrimson,
  onError = TextPrimary,
)

private val TacticalShapes = Shapes(
  extraSmall = RoundedCornerShape(4.dp),
  small = RoundedCornerShape(8.dp),
  medium = RoundedCornerShape(12.dp),
  large = RoundedCornerShape(16.dp),
  extraLarge = RoundedCornerShape(22.dp),
)

@Composable
fun FreeFireBiharTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = TacticalDarkColorScheme,
    typography = AppTypography,
    shapes = TacticalShapes,
    content = content,
  )
}
