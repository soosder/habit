package de.soosser.dopamineparadise.data

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SyncResult(val ok: Boolean, val added: Int, val message: String)

class AppRepository(context: Context) {
    private val prefs = context.getSharedPreferences("dopamine_state", Context.MODE_PRIVATE)
    private val key = "state_v1"

    fun loadState(): String = prefs.getString(key, null) ?: StateOps.defaultState().toString()
    fun saveState(state: String) { prefs.edit().putString(key, state).apply() }

    suspend fun syncDrop(url: String = StateOps.DROP_URL): SyncResult = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("Cache-Control", "no-cache")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return@withContext SyncResult(false, 0, "HTTP $code")
            }
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val drop = JSONObject(text)
            val state = JSONObject(loadState())
            val added = StateOps.importDrop(state, drop)
            saveState(state.toString())
            SyncResult(true, added, "ok")
        } catch (e: Exception) {
            SyncResult(false, 0, e.message ?: "offline")
        }
    }
}
