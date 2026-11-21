package de.soosser.dopamineparadise.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import de.soosser.dopamineparadise.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object AppUpdater {
    private const val VERSION_URL = "https://soosser.github.io/habit/version.json"

    suspend fun checkForUpdate(): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(VERSION_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("Cache-Control", "no-cache")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return@withContext null
            }
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val info = JSONObject(text)
            if (info.optInt("versionCode", 0) > BuildConfig.VERSION_CODE) info else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun download(context: Context, url: String, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 60000
        val total = connection.contentLengthLong
        val file = File(context.cacheDir, "update.apk")
        connection.inputStream.use { input ->
            file.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                var done = 0L
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    done += read
                    if (total > 0) onProgress((done * 100 / total).toInt().coerceIn(0, 100))
                }
            }
        }
        connection.disconnect()
        file
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
