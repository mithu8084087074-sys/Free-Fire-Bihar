package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val ChakraPetchFontFamily = FontFamily(
  Font(R.font.chakra_petch, FontWeight.Normal),
  Font(R.font.chakra_petch, FontWeight.Bold),
)

val RajdhaniFontFamily = FontFamily(
  Font(R.font.rajdhani, FontWeight.Normal),
  Font(R.font.rajdhani, FontWeight.Medium),
  Font(R.font.rajdhani, FontWeight.Bold),
)

val AppTypography = Typography(
  displayLarge = TextStyle(
    fontFamily = ChakraPetchFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 36.sp,
    lineHeight = 42.sp,
    letterSpacing = 1.2.sp,
  ),
  headlineMedium = TextStyle(
    fontFamily = ChakraPetchFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = 0.8.sp,
  ),
  titleLarge = TextStyle(
    fontFamily = ChakraPetchFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.6.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = ChakraPetchFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.4.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = RajdhaniFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.3.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = RajdhaniFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 19.sp,
    letterSpacing = 0.25.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = ChakraPetchFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.7.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = RajdhaniFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.5.sp,
  ),
)
