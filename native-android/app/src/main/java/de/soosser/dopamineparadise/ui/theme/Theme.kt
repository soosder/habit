package de.soosser.dopamineparadise.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Bg = Color(0xFF0B1020)
private val Bg2 = Color(0xFF131A33)
private val Ink = Color(0xFFF4F7FF)
private val Muted = Color(0xFFAAB4D0)
private val Pink = Color(0xFFFF5FA2)
private val Cyan = Color(0xFF67E8F9)
private val Gold = Color(0xFFFFD166)
private val Purple = Color(0xFFA78BFA)
private val Green = Color(0xFF7EF0A5)

private val DopamineColors = darkColorScheme(
    primary = Pink,
    secondary = Purple,
    tertiary = Cyan,
    background = Bg,
    surface = Bg2,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color(0xFF07111F),
    onBackground = Ink,
    onSurface = Ink
)

object DopaminePalette {
    val Bg = Bg
    val Bg2 = Bg2
    val Ink = Ink
    val Muted = Muted
    val Pink = Pink
    val Cyan = Cyan
    val Gold = Gold
    val Purple = Purple
    val Green = Green
}

@Composable
fun DopamineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DopamineColors,
        content = content
    )
}
