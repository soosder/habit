package de.soosser.dopamineparadise.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun JellyButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: List<Color>? = null,
    textSize: TextUnit = 18.sp,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "jellyScale"
    )
    val dy by animateDpAsState(
        targetValue = if (pressed) 7.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "jellyDy"
    )
    val palette = colors ?: listOf(Color(0xFFFF5FA2), Color(0xFFA78BFA))
    Box(
        modifier = modifier
            .height(56.dp)
            .offset(y = dy)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation = 14.dp, shape = CircleShape, ambientColor = palette.first(), spotColor = palette.first())
            .background(Brush.verticalGradient(palette), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = textSize,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
    }
}

private data class ConfettiParticle(val angle: Float, val speed: Float, val color: Color, val radius: Float)

@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger <= 0) return
    val colors = listOf(
        Color(0xFFFF5FA2), Color(0xFF67E8F9), Color(0xFFFFD166),
        Color(0xFFA78BFA), Color(0xFF7EF0A5)
    )
    val particles = remember(trigger) {
        List(42) {
            ConfettiParticle(
                angle = Random.nextFloat() * 360f,
                speed = 0.6f + Random.nextFloat() * 0.9f,
                color = colors.random(),
                radius = 3f + Random.nextFloat() * 5f
            )
        }
    }
    var progress by remember(trigger) { mutableStateOf(0f) }
    LaunchedEffect(trigger) {
        val start = System.currentTimeMillis()
        while (progress < 1f) {
            progress = ((System.currentTimeMillis() - start) / 950f).coerceIn(0f, 1f)
            delay(16)
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height * 0.32f
        val maxDist = size.height * 0.55f
        particles.forEach { p ->
            val rad = Math.toRadians(p.angle.toDouble()).toFloat()
            val dist = p.speed * progress * maxDist
            val x = cx + cos(rad) * dist
            val y = cy + sin(rad) * dist - progress * progress * size.height * 0.18f
            val alpha = (1f - progress).coerceIn(0f, 1f)
            drawCircle(
                color = p.color,
                radius = p.radius * (1f - progress * 0.6f),
                center = Offset(x, y),
                alpha = alpha
            )
        }
    }
}

private fun starPath(size: Size): Path {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension * 0.30f
    val inner = r * 0.45f
    val path = Path()
    for (i in 0 until 10) {
        val angle = Math.toRadians(-90.0 + i * 36).toFloat()
        val rad = if (i % 2 == 0) r else inner
        val x = cx + cos(angle) * rad
        val y = cy + sin(angle) * rad
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

@Composable
fun RarityBadge(rarity: String, modifier: Modifier = Modifier) {
    val color = when (rarity) {
        "legendary" -> Color(0xFFFFD166)
        "epic" -> Color(0xFFFF5FA2)
        "rare" -> Color(0xFFA78BFA)
        else -> Color(0xFF67E8F9)
    }
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.9f), color, Color(0xFF0B1020)),
                center = Offset(size.width * 0.35f, size.height * 0.3f),
                radius = size.width * 0.75f
            ),
            radius = size.minDimension / 2f,
            center = center
        )
        drawPath(path = starPath(size), color = Color.White.copy(alpha = 0.92f))
    }
}
