package de.soosser.dopamineparadise.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BgColor = Color(0xFF0B1020)
private val Bg2Color = Color(0xFF131A33)
private val InkColor = Color(0xFFF4F7FF)
private val MutedColor = Color(0xFFAAB4D0)
private val PinkColor = Color(0xFFFF5FA2)
private val CyanColor = Color(0xFF67E8F9)
private val GoldColor = Color(0xFFFFD166)
private val PurpleColor = Color(0xFFA78BFA)
private val GreenColor = Color(0xFF7EF0A5)

private val DopamineColors = darkColorScheme(
    primary = PinkColor,
    secondary = PurpleColor,
    tertiary = CyanColor,
    background = BgColor,
    surface = Bg2Color,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color(0xFF07111F),
    onBackground = InkColor,
    onSurface = InkColor
)

object DopaminePalette {
    val Bg = BgColor
    val Bg2 = Bg2Color
    val Ink = InkColor
    val Muted = MutedColor
    val Pink = PinkColor
    val Cyan = CyanColor
    val Gold = GoldColor
    val Purple = PurpleColor
    val Green = GreenColor
}

@Composable
fun DopamineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DopamineColors,
        content = content
    )
}
