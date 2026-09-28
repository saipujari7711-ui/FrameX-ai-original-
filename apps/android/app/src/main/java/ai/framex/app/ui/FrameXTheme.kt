package ai.framex.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val FrameXColors = darkColorScheme(
    primary = Color(0xFF4FD8FF),
    onPrimary = Color(0xFF00212B),
    primaryContainer = Color(0xFF123B48),
    onPrimaryContainer = Color(0xFFBCEEFF),
    secondary = Color(0xFFB6C8D1),
    onSecondary = Color(0xFF172126),
    secondaryContainer = Color(0xFF25353C),
    onSecondaryContainer = Color(0xFFD7E7EE),
    background = Color(0xFF071017),
    surface = Color(0xFF0B1720),
    surfaceVariant = Color(0xFF12232D),
    onBackground = Color(0xFFF0F7FA),
    onSurface = Color(0xFFF0F7FA),
    onSurfaceVariant = Color(0xFF9BB0BA),
    outline = Color(0xFF304650),
    error = Color(0xFFFF8A80),
    errorContainer = Color(0xFF4A1D1A),
    onErrorContainer = Color(0xFFFFDAD5)
)

@Composable
fun FrameXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FrameXColors,
        typography = Typography(
            headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.SemiBold),
            headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold)
        ),
        content = content
    )
}
