package ai.framex.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FrameXColors = darkColorScheme(
  primary = Color(0xFF00D4FF),
  onPrimary = Color(0xFF001018),
  secondary = Color(0xFFFFD700),
  onSecondary = Color(0xFF171200),
  tertiary = Color(0xFF80C6A0),
  background = Color(0xFF050B14),
  surface = Color(0xFF091522),
  surfaceVariant = Color(0xFF0B1722),
  onBackground = Color(0xFFEDF7FF),
  onSurface = Color(0xFFEDF7FF),
  onSurfaceVariant = Color(0xFF89A5BB),
  outline = Color(0x2B00D4FF),
  error = Color(0xFFE37B7B)
)

@Composable
fun FrameXTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = FrameXColors,
    typography = androidx.compose.material3.Typography(
      headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
      ),
      headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
      ),
      titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
      )
    ),
    content = content
  )
}
