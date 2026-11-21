package de.soosser.dopamineparadise.ui

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.soosser.dopamineparadise.data.AppRepository
import de.soosser.dopamineparadise.data.StateOps
import de.soosser.dopamineparadise.ui.theme.DopaminePalette
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.min

@Composable
fun DopamineApp() {
    val context = LocalContext.current
    val repo = remember { AppRepository(context) }
    var stateJson by remember { mutableStateOf(repo.loadState()) }
    var tab by remember { mutableStateOf(0) }
    var confettiKey by remember { mutableStateOf(0) }
    var loot by remember { mutableStateOf<JSONObject?>(null) }
    val scope = rememberCoroutineScope()
    val state = remember(stateJson) { runCatching { JSONObject(stateJson) }.getOrElse { StateOps.defaultState() } }

    fun onState(s: String) {
        repo.saveState(s)
        stateJson = s
    }

    LaunchedEffect(Unit) {
        val res = repo.syncDrop()
        if (res.ok && res.added > 0) {
            onState(repo.loadState())
            Toast.makeText(context, "Neuer Drop importiert: +${res.added}", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF0B1020),
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Heute") },
                        label = { Text("Heute") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0x33FFFFFF),
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = DopaminePalette.Muted,
                            unselectedTextColor = DopaminePalette.Muted
                        )
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Module") },
                        label = { Text("Module") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0x33FFFFFF),
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = DopaminePalette.Muted,
                            unselectedTextColor = DopaminePalette.Muted
                        )
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Sync") },
                        label = { Text("Sync") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0x33FFFFFF),
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = DopaminePalette.Muted,
                            unselectedTextColor = DopaminePalette.Muted
                        )
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Brush.verticalGradient(listOf(Color(0xFF0B1020), Color(0xFF131A33))))
            ) {
                when (tab) {
                    0 -> HomeScreen(state, onState, { confettiKey++ })
                    1 -> ModulesScreen(state, onState, { loot = it }, { confettiKey++ })
                    else -> SyncScreen(state, onState, repo, scope)
                }
            }
        }
        loot?.let { LootDialog(it) { loot = null } }
        ConfettiBurst(trigger = confettiKey)
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White.copy(alpha = 0.08f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = DopaminePalette.Cyan,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)

@Composable
private fun StatChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = DopaminePalette.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HomeScreen(state: JSONObject, onState: (String) -> Unit, onCelebrate: () -> Unit) {
    fun commit() { onState(state.toString()) }
    val level = StateOps.level(state)
    val prog = StateOps.levelProgress(state)
    val xp = StateOps.xp(state)
    val coins = StateOps.coins(state)
    val streak = StateOps.streak(state)
    val quest = state.optJSONObject("activeQuest")
    val pending = StateOps.pendingDaily(state)
    val queue = StateOps.dropQueue(state)
    var task by remember { mutableStateOf("") }
    var focus by remember { mutableStateOf("15") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "DOPAMIN PARADISE",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Level $level", Modifier.weight(1f))
            StatChip("$xp XP", Modifier.weight(1f))
            StatChip("$coins", Modifier.weight(1f))
            StatChip("Serie $streak", Modifier.weight(1f))
        }
        LinearProgressIndicator(
            progress = prog,
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
            color = DopaminePalette.Pink,
            trackColor = Color.White.copy(alpha = 0.12f)
        )

        if (queue.length() > 0) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0 until minOf(6, queue.length())) {
                    val item = queue.optJSONObject(i) ?: continue
                    val type = item.optString("type", "quest")
                    val color = when (type) {
                        "loot" -> DopaminePalette.Gold
                        "story" -> DopaminePalette.Purple
                        "paradise" -> DopaminePalette.Green
                        "crew" -> DopaminePalette.Cyan
                        else -> DopaminePalette.Pink
                    }
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, color.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                val used = StateOps.useDropItem(state, i)
                                if (used != null) { commit(); onCelebrate() }
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                        Text(
                            item.optString("title", "Impulse").take(24),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                if (queue.length() > 6) {
                    Text(
                        "+${queue.length() - 6}",
                        color = DopaminePalette.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }

        pending?.let { p ->
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tages-Impuls", color = DopaminePalette.Gold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(p.optString("title", p.optString("text", "Neue Quest")), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        JellyButton("Übernehmen", modifier = Modifier.weight(1f), textSize = 15.sp) {
                            StateOps.usePendingDaily(state); commit()
                        }
                        JellyButton("Später", modifier = Modifier.weight(1f), textSize = 15.sp, colors = listOf(Color(0xFF3A4457), Color(0xFF5B6B8C))) {
                            StateOps.dismissPendingDaily(state); commit()
                        }
                    }
                }
            }
        }

        if (quest != null) {
            val focusMin = quest.optInt("focus_minutes", 0)
            val xpGain = 40 + focusMin * 2 + quest.optInt("reward_xp", 0)
            val coinGain = 6 + focusMin + quest.optInt("reward_coins", 0)
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Aktive Aufgabe", color = DopaminePalette.Cyan, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text(quest.optString("title", "Aufgabe"), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                    Text("+${xpGain} XP · +${coinGain} Münzen", color = DopaminePalette.Muted, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        JellyButton("Erledigt", modifier = Modifier.weight(1f), colors = listOf(Color(0xFF67E8F9), Color(0xFF7EF0A5)), textSize = 16.sp) {
                            val res = StateOps.completeQuest(state)
                            if (res != null) { commit(); onCelebrate() }
                        }
                        JellyButton("Pause", modifier = Modifier.weight(1f), textSize = 15.sp, colors = listOf(Color(0xFF5B6B8C), Color(0xFF3A4457))) {
                            StateOps.cancelQuest(state); commit()
                        }
                    }
                }
            }
        } else {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Eine Aufgabe für heute", color = DopaminePalette.Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    TextField(
                        value = task,
                        onValueChange = { task = it.take(140) },
                        placeholder = { Text("z. B. 10 Minuten rausgehen", color = DopaminePalette.Muted) },
                        colors = fieldColors(),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = focus,
                            onValueChange = { focus = it.filter { c -> c.isDigit() }.take(3) },
                            placeholder = { Text("Min", color = DopaminePalette.Muted) },
                            colors = fieldColors(),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true,
                            modifier = Modifier.width(110.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Fokus-Minuten", color = DopaminePalette.Muted, fontSize = 13.sp)
                    }
                    JellyButton(
                        text = "Start",
                        onClick = {
                            val title = task.trim()
                            if (title.isNotEmpty()) {
                                StateOps.startQuest(state, title, focus.toIntOrNull()?.coerceIn(0, 240) ?: 15)
                                task = ""
                                commit()
                            }
                        },
                        enabled = task.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        textSize = 20.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(28.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
private fun ModulesScreen(state: JSONObject, onState: (String) -> Unit, onLoot: (JSONObject) -> Unit, onConfetti: () -> Unit) {
    fun commit() { onState(state.toString()) }
    val level = StateOps.level(state)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Module", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        if (level < 2) {
            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Neue Module ab Level 2", color = DopaminePalette.Gold, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("Erledige Aufgaben, um Kisten, Paradies, Crew und Story freizuschalten.", color = DopaminePalette.Muted, fontSize = 14.sp)
                    LinearProgressIndicator(
                        progress = StateOps.levelProgress(state),
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        color = DopaminePalette.Pink,
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )
                }
            }
        } else {
            if (level >= 2) CrateModule(state, commit, onLoot, onConfetti)
            if (level >= 3) ParadiseModule(state, commit, onConfetti)
            if (level >= 4) CrewModule(state, commit)
            if (level >= 5) StoryModule(state, commit, onConfetti)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CrateVisual(open: Boolean, onClick: () -> Unit) {
    val lidY by animateFloatAsState(
        targetValue = if (open) -56f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "lidY"
    )
    val lidRot by animateFloatAsState(
        targetValue = if (open) -20f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "lidR"
    )
    val glow by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "glow"
    )
    Box(
        modifier = Modifier.size(190.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(180.dp)
                .background(Color(0xFFFFD166).copy(alpha = 0.16f + 0.42f * glow), CircleShape)
        )
        Box(
            Modifier.size(120.dp)
                .background(Color(0xFFFFD166).copy(alpha = 0.30f * glow), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(width = 150.dp, height = 96.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF8A5A2B), Color(0xFF5B3418), Color(0xFF3D2210))), RoundedCornerShape(18.dp))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
        ) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .size(width = 150.dp, height = 46.dp)
                    .graphicsLayer {
                        translationY = lidY
                        rotationZ = lidRot
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFC98A3D), Color(0xFF7A4B1D), Color(0xFF4D2A10))))
                    .border(1.dp, Color.White.copy(alpha = 0.26f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(width = 8.dp, height = 56.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFD166), Color(0xFFB7791F), Color(0xFFFFD166))))
            )
        }
    }
}

@Composable
private fun CrateModule(state: JSONObject, commit: () -> Unit, onLoot: (JSONObject) -> Unit, onConfetti: () -> Unit) {
    var animOpen by remember { mutableStateOf(false) }
    val today = StateOps.today()
    val dailyOpen = animOpen || StateOps.claimedCrates(state).has("daily_$today")
    val dailyStatus = StateOps.crateStatus(state, "daily")
    val miniStatus = StateOps.crateStatus(state, "mini")
    val lastLoot = StateOps.lastLoot(state)

    ModuleShell(title = "Kisten & Loot", subtitle = "Runde Badges und Belohnungen") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CrateVisual(open = dailyOpen) {
                if (dailyStatus == "ok") {
                    animOpen = true
                    val lootObj = StateOps.openCrate(state, "daily")
                    if (lootObj != null) { commit(); onLoot(lootObj); onConfetti() }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                JellyButton(
                    text = if (dailyStatus == "claimed") "Daily offen" else "Daily-Kiste",
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f),
                    textSize = 14.sp,
                    colors = listOf(Color(0xFF5B6B8C), Color(0xFF3A4457))
                )
                JellyButton(
                    text = if (miniStatus == "claimed") "Mini offen" else "Mini (-15)",
                    onClick = {
                        if (miniStatus == "ok") {
                            val lootObj = StateOps.openCrate(state, "mini")
                            if (lootObj != null) { commit(); onLoot(lootObj); onConfetti() }
                        }
                    },
                    enabled = miniStatus == "ok",
                    modifier = Modifier.weight(1f),
                    textSize = 14.sp
                )
            }
            lastLoot?.let { loot ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RarityBadge(loot.optString("rarity", "common"), Modifier.size(44.dp))
                    Column {
                        Text(loot.optString("title", "Loot"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("+${loot.optInt("coins", 0)} Münzen · +${loot.optInt("xp", 0)} XP", color = DopaminePalette.Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleShell(title: String, subtitle: String, content: @Composable () -> Unit) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(subtitle, color = DopaminePalette.Muted, fontSize = 13.sp)
                }
                Box(
                    Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Neu", color = DopaminePalette.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            content()
        }
    }
}

@Composable
private fun ParadiseModule(state: JSONObject, commit: () -> Unit, onConfetti: () -> Unit) {
    val plots = StateOps.plots(state)
    val built = (0 until plots.length()).count { plots.optJSONObject(it)?.optBoolean("built", false) == true }
    ModuleShell(title = "Paradies-Aufbau", subtitle = "$built/${plots.length()} Parzellen") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (rowStart in 0 until plots.length() step 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in rowStart until minOf(rowStart + 3, plots.length())) {
                        val plot = plots.optJSONObject(i) ?: continue
                        val isBuilt = plot.optBoolean("built", false)
                        val cost = plot.optInt("cost", 0)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(96.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isBuilt) Color(0xFF7EF0A5).copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
                                .border(1.dp, if (isBuilt) Color(0xFF7EF0A5).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                .clickable {
                                    if (StateOps.buildPlot(state, i)) { commit(); onConfetti() }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(plot.optString("name", "Parzelle"), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Text(if (isBuilt) "Gebaut" else "$cost", color = DopaminePalette.Muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CrewModule(state: JSONObject, commit: () -> Unit) {
    val crew = StateOps.crew(state)
    val friends = StateOps.friends(state)
    ModuleShell(title = "Begleiter & Crew", subtitle = "Helfer und Freunde") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                JellyButton("Begleiter (25)", modifier = Modifier.weight(1f), textSize = 14.sp) {
                    if (StateOps.recruitCrew(state)) commit()
                }
                JellyButton("Freund (10)", modifier = Modifier.weight(1f), textSize = 14.sp, colors = listOf(Color(0xFF67E8F9), Color(0xFF7EF0A5))) {
                    if (StateOps.inviteFriend(state)) commit()
                }
            }
            if (crew.length() == 0) {
                Text("Noch keine Begleiter. Die erste Crew wartet auf dich.", color = DopaminePalette.Muted, fontSize = 13.sp)
            } else {
                for (i in 0 until crew.length()) {
                    val c = crew.optJSONObject(i) ?: continue
                    CrewRow(c.optString("name", "?"), c.optString("role", ""), c.optInt("mood", 50))
                }
            }
            if (friends.length() > 0) {
                Text("Freunde", color = DopaminePalette.Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                for (i in 0 until friends.length()) {
                    val f = friends.optJSONObject(i) ?: continue
                    CrewRow(f.optString("name", "?"), "Freund", 80)
                }
            }
        }
    }
}

@Composable
private fun CrewRow(name: String, role: String, mood: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(DopaminePalette.Pink, DopaminePalette.Purple)))
                .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black)
        }
        Column(Modifier.weight(1f)) {
            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("$role · Laune $mood%", color = DopaminePalette.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StoryModule(state: JSONObject, commit: () -> Unit, onConfetti: () -> Unit) {
    val chapters = StateOps.storyChapters(state)
    ModuleShell(title = "Story", subtitle = "Mikro-Kapitel aus Impulsen") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            JellyButton("Nächstes Kapitel (5 Münzen)", modifier = Modifier.fillMaxWidth(), textSize = 15.sp) {
                if (StateOps.nextStory(state)) { commit(); onConfetti() }
            }
            if (chapters.length() == 0) {
                Text("Noch keine Kapitel.", color = DopaminePalette.Muted, fontSize = 13.sp)
            } else {
                for (i in 0 until minOf(5, chapters.length())) {
                    val c = chapters.optJSONObject(i) ?: continue
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                            .padding(12.dp)
                    ) {
                        Text(c.optString("title", "Kapitel"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(c.optString("text", ""), color = DopaminePalette.Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncScreen(state: JSONObject, onState: (String) -> Unit, repo: AppRepository, scope: kotlinx.coroutines.CoroutineScope) {
    var conceptText by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("quest") }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val kinds = listOf("quest" to "Quest", "loot" to "Loot", "story" to "Story", "paradise" to "Paradise", "crew" to "Crew")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Konzepte & Sync", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Neues Konzept", color = DopaminePalette.Cyan, fontWeight = FontWeight.Black, fontSize = 15.sp)
                TextField(
                    value = conceptText,
                    onValueChange = { conceptText = it.take(220) },
                    placeholder = { Text("z. B. 5 Minuten Musik", color = DopaminePalette.Muted) },
                    colors = fieldColors(),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    kinds.forEach { (k, label) ->
                        val selected = kind == k
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) DopaminePalette.Pink.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.08f))
                                .border(1.dp, if (selected) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f), CircleShape)
                                .clickable { kind = k }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                JellyButton(
                    text = "Hinzufügen",
                    onClick = {
                        val text = conceptText.trim()
                        if (text.isNotEmpty()) {
                            StateOps.addConcept(state, text, kind)
                            conceptText = ""
                            onState(state.toString())
                        }
                    },
                    enabled = conceptText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    textSize = 16.sp
                )
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Konzepte (${StateOps.concepts(state).length()})", color = DopaminePalette.Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val concepts = StateOps.concepts(state)
                if (concepts.length() == 0) {
                    Text("Noch keine Konzepte.", color = DopaminePalette.Muted, fontSize = 13.sp)
                } else {
                    for (i in 0 until concepts.length()) {
                        val c = concepts.optJSONObject(i) ?: continue
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(c.optString("text", ""), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(c.optString("kind", "quest"), color = DopaminePalette.Muted, fontSize = 11.sp)
                            }
                            IconButton(onClick = {
                                StateOps.deleteConcept(state, c.optString("id", ""))
                                onState(state.toString())
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Löschen", tint = DopaminePalette.Muted)
                            }
                        }
                    }
                }
            }
        }

        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("daily_drop.json", color = DopaminePalette.Cyan, fontWeight = FontWeight.Black, fontSize = 15.sp)
                val sync = StateOps.lastSync(state)
                if (sync != null) {
                    Text("Letzter Sync: ${sync.optString("source", "auto")} · ${sync.optInt("queue_added", 0)} neu", color = DopaminePalette.Muted, fontSize = 13.sp)
                } else {
                    Text("Noch kein Sync.", color = DopaminePalette.Muted, fontSize = 13.sp)
                }
                JellyButton(
                    text = "Drop jetzt laden",
                    onClick = {
                        scope.launch {
                            val res = repo.syncDrop()
                            onState(repo.loadState())
                            Toast.makeText(context, if (res.ok) "Sync: +${res.added} Einträge" else "Sync fehlgeschlagen", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textSize = 16.sp,
                    colors = listOf(Color(0xFF67E8F9), Color(0xFF7EF0A5))
                )
                JellyButton(
                    text = "state.json kopieren",
                    onClick = {
                        clipboard.setText(AnnotatedString(state.toString()))
                        Toast.makeText(context, "State kopiert", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textSize = 15.sp,
                    colors = listOf(Color(0xFF5B6B8C), Color(0xFF3A4457))
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LootDialog(loot: JSONObject, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Loot!", color = Color.White, fontWeight = FontWeight.Black) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RarityBadge(loot.optString("rarity", "common"), Modifier.size(96.dp))
                Text(loot.optString("title", "Belohnung"), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, textAlign = TextAlign.Center)
                Text(loot.optString("text", ""), color = DopaminePalette.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
                Text("+${loot.optInt("coins", 0)} Münzen · +${loot.optInt("xp", 0)} XP", color = DopaminePalette.Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        confirmButton = {
            JellyButton("Nice!", onClick = onDismiss, modifier = Modifier.fillMaxWidth(), textSize = 16.sp)
        }
    )
}
