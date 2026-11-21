package de.soosser.dopamineparadise.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object StateOps {
    const val DROP_URL = "https://soosser.github.io/habit/daily_drop.json"
    private val LEVEL_XP = intArrayOf(0, 80, 180, 320, 500, 720, 980)
    private val DEFAULT_CONCEPT_TEXTS = listOf(
        "10 Minuten an die frische Luft",
        "Eine Mini-Kiste öffnen",
        "Erstes Paradies-Beet pflanzen",
        "5 Minuten dehnen",
        "Eine kleine Errungenschaft notieren"
    )
    private val PLOT_DEFS = listOf(
        Triple("bank", "Sonnenbank", 15),
        Triple("lamp", "Lichterkette", 20),
        Triple("tree", "Glücksbaum", 25),
        Triple("pond", "Klarteich", 30),
        Triple("bridge", "Regenbogenbrücke", 40),
        Triple("statue", "Dopamin-Skulptur", 55)
    )
    private val CREW_NAMES = listOf("Nova", "Pixel", "Momo", "Zed", "Luna", "Echo", "Blaise", "Kiki")
    private val CREW_ROLES = listOf("Motivator", "Loot-Koordinator", "Story-Guide", "Paradies-Architekt", "Kistenwächter", "XP-Sammler")
    private val FRIEND_NAMES = listOf("Ari", "Juno", "Max", "Sana", "Theo", "Vera")
    private val RARITY_MULT = mapOf("common" to 1.0, "rare" to 1.5, "epic" to 2.2, "legendary" to 3.5)

    fun nowIso(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date())
    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    fun uid(prefix: String): String = "${prefix}_${System.currentTimeMillis().toString(36)}_${Random.nextInt(1000, 9999)}"

    fun defaultState(): JSONObject {
        val s = JSONObject()
        s.put("version", 1)
        val concepts = JSONArray()
        DEFAULT_CONCEPT_TEXTS.forEachIndexed { i, text ->
            concepts.put(JSONObject().apply {
                put("id", "c$i")
                put("text", text)
                put("kind", if (i == 1) "loot" else if (i == 2) "paradise" else "quest")
                put("active", true)
                put("created_at", nowIso())
            })
        }
        s.put("concepts", concepts)
        s.put("progress", JSONObject().apply {
            put("xp", 0); put("coins", 20); put("completed", 0); put("streak", 0)
        })
        s.put("activeQuest", JSONObject.NULL)
        s.put("pendingDaily", JSONObject.NULL)
        s.put("dropQueue", JSONArray())
        s.put("seenDropIds", JSONArray())
        s.put("importedDrops", JSONArray())
        s.put("lastCrateTemplate", JSONObject.NULL)
        s.put("lastLoot", JSONObject.NULL)
        s.put("lastSync", JSONObject.NULL)
        s.put("lastDropSignature", JSONObject.NULL)
        s.put("claimedCrates", JSONObject())
        s.put("storyChapters", JSONArray().apply {
            put(JSONObject().apply {
                put("id", "s0"); put("title", "Kapitel 0 – Die erste Belohnung")
                put("text", "Du hast das Paradies aktiviert. Jeder kleine Schritt füllt den Himmel mit Licht.")
                put("source", "local"); put("created_at", nowIso())
            })
        })
        s.put("paradise", JSONObject().apply {
            put("plots", JSONArray().apply {
                PLOT_DEFS.forEach { (id, name, cost) ->
                    put(JSONObject().apply { put("id", id); put("name", name); put("cost", cost); put("built", false) })
                }
            })
        })
        s.put("crew", JSONArray())
        s.put("friends", JSONArray())
        return s
    }

    fun ensureObj(parent: JSONObject, key: String): JSONObject {
        val existing = parent.optJSONObject(key)
        if (existing != null) return existing
        val created = JSONObject()
        parent.put(key, created)
        return created
    }

    fun ensureArr(parent: JSONObject, key: String): JSONArray {
        val existing = parent.optJSONArray(key)
        if (existing != null) return existing
        val created = JSONArray()
        parent.put(key, created)
        return created
    }

    fun progress(s: JSONObject): JSONObject = ensureObj(s, "progress")
    fun xp(s: JSONObject): Int = progress(s).optInt("xp", 0)
    fun coins(s: JSONObject): Int = progress(s).optInt("coins", 0)
    fun completed(s: JSONObject): Int = progress(s).optInt("completed", 0)
    fun streak(s: JSONObject): Int = progress(s).optInt("streak", 0)

    fun levelFromXp(xpValue: Int): Int {
        var level = 1
        for (i in 1 until LEVEL_XP.size) if (xpValue >= LEVEL_XP[i]) level = i + 1
        return level
    }

    fun level(s: JSONObject): Int = levelFromXp(xp(s))
    fun nextLevelXp(levelValue: Int): Int? = if (levelValue < LEVEL_XP.size) LEVEL_XP[levelValue] else null
    fun levelProgress(s: JSONObject): Float {
        val lvl = level(s)
        val start = LEVEL_XP[lvl - 1]
        val end = nextLevelXp(lvl) ?: return 1f
        val span = (end - start).coerceAtLeast(1)
        return ((xp(s) - start).toFloat() / span).coerceIn(0f, 1f)
    }

    fun addXp(s: JSONObject, amount: Int): Boolean {
        val before = level(s)
        progress(s).put("xp", xp(s) + amount)
        return level(s) > before
    }

    fun addCoins(s: JSONObject, amount: Int) {
        progress(s).put("coins", (coins(s) + amount).coerceAtLeast(0))
    }

    fun updateStreak(s: JSONObject) {
        val todayKey = today()
        val p = progress(s)
        if (p.optString("lastDay", "") == todayKey) return
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - 86400000))
        val last = p.optString("lastDay", "")
        p.put("streak", if (last == yesterday) streak(s) + 1 else 1)
        p.put("lastDay", todayKey)
    }

    fun activeConcepts(s: JSONObject): List<String> {
        val out = mutableListOf<String>()
        val arr = s.optJSONArray("concepts")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                if (c.optBoolean("active", true)) out.add(c.optString("text", ""))
            }
        }
        return out.filter { it.isNotBlank() }.ifEmpty { DEFAULT_CONCEPT_TEXTS }
    }

    fun addConcept(s: JSONObject, text: String, kind: String): JSONObject {
        val concept = JSONObject().apply {
            put("id", uid("concept")); put("text", text.take(220)); put("kind", kind)
            put("active", true); put("created_at", nowIso())
        }
        ensureArr(s, "concepts").put(0, concept)
        return concept
    }

    fun deleteConcept(s: JSONObject, id: String) {
        val arr = ensureArr(s, "concepts")
        val kept = JSONArray()
        for (i in 0 until arr.length()) {
            val c = arr.optJSONObject(i) ?: continue
            if (c.optString("id") != id) kept.put(c)
        }
        s.put("concepts", kept)
    }

    fun concepts(s: JSONObject): JSONArray = ensureArr(s, "concepts")

    fun startQuest(s: JSONObject, title: String, focusMinutes: Int) {
        val quest = JSONObject().apply {
            put("id", uid("quest")); put("title", title); put("focus_minutes", focusMinutes)
            put("reward_xp", 0); put("reward_coins", 0); put("source", "manual"); put("created_at", nowIso())
        }
        addConcept(s, title, "quest")
        s.put("activeQuest", quest)
        addXp(s, 10)
        addCoins(s, 2)
    }

    fun cancelQuest(s: JSONObject) { s.remove("activeQuest") }

    fun completeQuest(s: JSONObject): JSONObject? {
        val q = s.optJSONObject("activeQuest") ?: return null
        val focus = q.optInt("focus_minutes", 0)
        val xpGain = 40 + focus * 2 + q.optInt("reward_xp", 0)
        val coinGain = 6 + focus + q.optInt("reward_coins", 0)
        val leveled = addXp(s, xpGain)
        addCoins(s, coinGain)
        progress(s).put("completed", completed(s) + 1)
        updateStreak(s)
        val crew = ensureArr(s, "crew")
        for (i in 0 until crew.length()) {
            val c = crew.optJSONObject(i) ?: continue
            c.put("mood", (c.optInt("mood", 50) + 6).coerceAtMost(100))
        }
        val dropId = q.optString("dropId", "")
        if (dropId.isNotEmpty()) removeDropQueueById(s, dropId)
        s.remove("activeQuest")
        return JSONObject().apply {
            put("xp", xpGain); put("coins", coinGain); put("leveled", leveled); put("level", level(s))
        }
    }

    fun removeDropQueueById(s: JSONObject, id: String) {
        val arr = ensureArr(s, "dropQueue")
        val kept = JSONArray()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            if (item.optString("id") != id) kept.put(item)
        }
        s.put("dropQueue", kept)
    }

    fun pendingDaily(s: JSONObject): JSONObject? = s.optJSONObject("pendingDaily")
    fun usePendingDaily(s: JSONObject): Boolean {
        val d = s.optJSONObject("pendingDaily") ?: return false
        val quest = JSONObject().apply {
            put("id", uid("quest")); put("title", d.optString("title", d.optString("text", "Tages-Impuls")))
            put("focus_minutes", d.optInt("focus_minutes", d.optInt("focusMinutes", 15)))
            put("reward_xp", d.optInt("reward_xp", d.optInt("xp", 0)))
            put("reward_coins", d.optInt("reward_coins", d.optInt("coins", 0)))
            put("source", "daily_drop"); put("dropId", d.optString("id", "")); put("created_at", nowIso())
        }
        s.put("activeQuest", quest)
        s.remove("pendingDaily")
        return true
    }
    fun dismissPendingDaily(s: JSONObject) { s.remove("pendingDaily") }

    fun dropQueue(s: JSONObject): JSONArray = ensureArr(s, "dropQueue")

    fun useDropItem(s: JSONObject, index: Int): String? {
        val arr = ensureArr(s, "dropQueue")
        if (index < 0 || index >= arr.length()) return null
        val item = arr.optJSONObject(index) ?: return null
        val kept = JSONArray()
        for (i in 0 until arr.length()) if (i != index) kept.put(arr.get(i))
        s.put("dropQueue", kept)
        val type = item.optString("type", "quest")
        when (type) {
            "story" -> addStoryChapter(s, item.optString("title", "Drop-Story"), "drop-queue")
            "loot", "crew", "paradise" -> applyLoot(s, generateLoot(item.optString("rarity", "rare"), type, item))
            else -> {
                s.put("activeQuest", JSONObject().apply {
                    put("id", uid("quest")); put("title", item.optString("title", "Drop"))
                    put("focus_minutes", item.optInt("focus_minutes", 0))
                    put("reward_xp", item.optInt("reward_xp", 0))
                    put("reward_coins", item.optInt("reward_coins", 0))
                    put("source", "drop_queue"); put("dropId", item.optString("id", "")); put("created_at", nowIso())
                })
            }
        }
        return type
    }

    fun signatureFromRaw(drop: JSONObject): String {
        val queue = drop.optJSONArray("unused_queue") ?: drop.optJSONArray("queue") ?: JSONArray()
        return listOf(
            drop.optString("date", ""), drop.optString("generated_at", ""),
            drop.optString("buffer_after", queue.length().toString()), queue.length().toString(),
            drop.optJSONObject("daily")?.optString("id", "") ?: "",
            drop.optJSONObject("story")?.optString("id", "") ?: "",
            drop.optJSONObject("crate")?.optString("id", "") ?: ""
        ).joinToString("|")
    }

    fun importDrop(state: JSONObject, drop: JSONObject): Int {
        val signature = signatureFromRaw(drop)
        if (signature.isNotEmpty() && state.optString("lastDropSignature", "") == signature) return 0
        val queue = drop.optJSONArray("unused_queue") ?: drop.optJSONArray("queue") ?: drop.optJSONArray("buffer") ?: JSONArray()
        val seenArr = ensureArr(state, "seenDropIds")
        val seen = mutableSetOf<String>()
        for (i in 0 until seenArr.length()) seen.add(seenArr.optString(i, ""))
        val dropQueueArr = ensureArr(state, "dropQueue")
        var added = 0
        for (i in 0 until queue.length()) {
            val item = queue.optJSONObject(i) ?: continue
            val key = item.optString("id", "${item.optString("title", "")}_${item.optString("created_at", "")}")
            if (key.isNotEmpty() && seen.add(key)) {
                dropQueueArr.put(item)
                added++
            }
        }
        val newSeen = JSONArray()
        seen.toList().takeLast(500).forEach { newSeen.put(it) }
        state.put("seenDropIds", newSeen)
        val import = JSONObject().apply {
            put("id", uid("drop")); put("source", "auto"); put("imported_at", nowIso())
            put("generated_at", drop.optString("generated_at", "")); put("drop_source", drop.optString("source", ""))
            put("buffer_before", drop.optInt("buffer_before", 0)); put("buffer_after", drop.optInt("buffer_after", 0))
            put("queue_count", queue.length()); put("daily", drop.opt("daily") ?: JSONObject.NULL)
            put("story", drop.opt("story") ?: JSONObject.NULL); put("crate", drop.opt("crate") ?: JSONObject.NULL)
        }
        unshift(state, "importedDrops", import)
        val daily = drop.optJSONObject("daily")
        if (daily != null) state.put("pendingDaily", daily)
        val story = drop.optJSONObject("story")
        if (story != null && (story.optString("title").isNotEmpty() || story.optString("text").isNotEmpty())) {
            unshift(state, "storyChapters", JSONObject().apply {
                put("id", uid("story")); put("title", story.optString("title", "Drop-Story").take(120))
                put("text", story.optString("text", "").take(1200)); put("source", "daily_drop"); put("created_at", nowIso())
            })
        }
        val crate = drop.optJSONObject("crate")
        if (crate != null) state.put("lastCrateTemplate", crate)
        state.put("lastDropSignature", signature)
        state.put("lastSync", JSONObject().apply {
            put("type", "daily_drop"); put("source", "auto"); put("at", nowIso())
            put("queue_added", added); put("buffer_before", drop.optInt("buffer_before", 0))
            put("buffer_after", drop.optInt("buffer_after", 0)); put("signature", signature)
        })
        return added
    }

    fun unshift(state: JSONObject, key: String, value: JSONObject) {
        val old = ensureArr(state, key)
        val arr = JSONArray()
        arr.put(value)
        for (i in 0 until old.length()) arr.put(old.get(i))
        state.put(key, arr)
    }

    fun generateLoot(rarity: String? = null, type: String? = null, template: JSONObject? = null): JSONObject {
        val roll = Random.nextDouble()
        val rar = rarity ?: template?.optString("rarity", "")?.takeIf { it.isNotEmpty() }
            ?: if (roll > 0.96) "legendary" else if (roll > 0.82) "epic" else if (roll > 0.58) "rare" else "common"
        val mult = RARITY_MULT[rar] ?: 1.0
        val types = listOf("coins", "xp", "concept", "story", "crew")
        val t = type ?: template?.optString("type", "")?.takeIf { it in types } ?: types.random()
        return JSONObject().apply {
            put("id", uid("loot")); put("type", t); put("rarity", rar)
            put("coins", ((template?.optInt("coins", 0) ?: 0).takeIf { it > 0 } ?: (5 + Random.nextInt(0, 15))).let { (it * mult).toInt() })
            put("xp", ((template?.optInt("xp", 0) ?: 0).takeIf { it > 0 } ?: (8 + Random.nextInt(0, 18))).let { (it * mult).toInt() })
            put("title", template?.optString("title", "")?.takeIf { it.isNotEmpty() } ?: "${rar.replaceFirstChar { it.uppercase() }}-Belohnung")
            put("text", template?.optString("text", "")?.takeIf { it.isNotEmpty() } ?: "Die Kiste hat ein surprises Paket für dich.")
            put("created_at", nowIso())
        }
    }

    fun latestCrateTemplate(s: JSONObject): JSONObject? {
        s.optJSONObject("lastCrateTemplate")?.let { return it }
        val imports = ensureArr(s, "importedDrops")
        for (i in 0 until imports.length()) {
            val imp = imports.optJSONObject(i) ?: continue
            imp.optJSONObject("crate")?.let { return it }
        }
        return null
    }

    fun applyLoot(s: JSONObject, loot: JSONObject) {
        addCoins(s, loot.optInt("coins", 0))
        addXp(s, loot.optInt("xp", 0))
        when (loot.optString("type", "")) {
            "concept" -> {
                val concepts = activeConcepts(s)
                if (concepts.isNotEmpty()) addConcept(s, concepts.random() + " (aus Kiste)", "quest")
            }
            "story" -> addStoryChapter(s, "Kisten-Loot", "loot")
            "crew" -> recruitCrew(s, free = true)
        }
        s.put("lastLoot", loot)
    }

    fun claimedCrates(s: JSONObject): JSONObject = ensureObj(s, "claimedCrates")

    fun crateStatus(s: JSONObject, kind: String): String {
        val key = "${kind}_${today()}"
        if (claimedCrates(s).has(key)) return "claimed"
        if (kind == "mini" && coins(s) < 15) return "poor"
        return "ok"
    }

    fun openCrate(s: JSONObject, kind: String): JSONObject? {
        if (crateStatus(s, kind) != "ok") return null
        val key = "${kind}_${today()}"
        claimedCrates(s).put(key, nowIso())
        if (kind == "mini") addCoins(s, -15)
        val template = latestCrateTemplate(s)
        val crateRarity = template?.optString("rarity", "")?.takeIf { it.isNotEmpty() }
        val templateLoot = template?.optJSONObject("loot")
        val loot = generateLoot(crateRarity, templateLoot?.optString("type", "")?.takeIf { it.isNotEmpty() }, templateLoot)
        applyLoot(s, loot)
        return loot
    }

    fun lastLoot(s: JSONObject): JSONObject? = s.optJSONObject("lastLoot")

    fun plots(s: JSONObject): JSONArray {
        val paradise = ensureObj(s, "paradise")
        val arr = paradise.optJSONArray("plots")
        if (arr != null && arr.length() > 0) return arr
        val created = JSONArray()
        PLOT_DEFS.forEach { (id, name, cost) ->
            created.put(JSONObject().apply { put("id", id); put("name", name); put("cost", cost); put("built", false) })
        }
        paradise.put("plots", created)
        return created
    }

    fun buildPlot(s: JSONObject, index: Int): Boolean {
        val arr = plots(s)
        if (index < 0 || index >= arr.length()) return false
        val plot = arr.optJSONObject(index) ?: return false
        if (plot.optBoolean("built", false)) return false
        val cost = plot.optInt("cost", 0)
        if (coins(s) < cost) return false
        addCoins(s, -cost)
        plot.put("built", true)
        addXp(s, 15)
        return true
    }

    fun crew(s: JSONObject): JSONArray = ensureArr(s, "crew")
    fun friends(s: JSONObject): JSONArray = ensureArr(s, "friends")

    fun recruitCrew(s: JSONObject, free: Boolean = false): Boolean {
        if (!free && coins(s) < 25) return false
        if (!free) addCoins(s, -25)
        crew(s).put(JSONObject().apply {
            put("id", uid("crew")); put("name", "${CREW_NAMES.random()} ${crew(s).length() + 1}")
            put("role", CREW_ROLES.random()); put("mood", 55 + Random.nextInt(0, 35)); put("created_at", nowIso())
        })
        return true
    }

    fun inviteFriend(s: JSONObject): Boolean {
        if (coins(s) < 10) return false
        addCoins(s, -10)
        friends(s).put(JSONObject().apply {
            put("id", uid("friend")); put("name", FRIEND_NAMES.random()); put("created_at", nowIso())
        })
        return true
    }

    fun addStoryChapter(s: JSONObject, seedText: String, source: String) {
        val concepts = activeConcepts(s)
        val concept = concepts.randomOrNull() ?: seedText
        unshift(s, "storyChapters", JSONObject().apply {
            put("id", uid("story")); put("title", "Kapitel ${storyChapters(s).length() + 1} – ${seedText.take(34)}")
            put("text", "Aus dem Impuls „$concept“ entsteht eine Szene: Die Stadt leuchtet auf, eine Kiste schwebt herein, und das Paradies rückt ein Stück näher.")
            put("source", source); put("created_at", nowIso())
        })
    }

    fun storyChapters(s: JSONObject): JSONArray = ensureArr(s, "storyChapters")

    fun nextStory(s: JSONObject): Boolean {
        if (coins(s) < 5) return false
        addCoins(s, -5)
        addStoryChapter(s, activeConcepts(s).randomOrNull() ?: "Neue Idee", "lokal")
        addXp(s, 10)
        return true
    }

    fun lastSync(s: JSONObject): JSONObject? = s.optJSONObject("lastSync")
    fun importedDrops(s: JSONObject): JSONArray = ensureArr(s, "importedDrops")
}
