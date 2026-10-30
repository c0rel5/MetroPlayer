package com.metroplayer.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Строка текста. [timeMs] = -1, если текст без временных меток. */
data class LyricLine(val timeMs: Long, val text: String)

class Lyrics(val lines: List<LyricLine>, val synced: Boolean)

object LrcParser {
    private val tag = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val wordTag = Regex("""<\d{1,3}:\d{2}(?:[.:]\d{1,3})?>""")

    fun parse(raw: String): Lyrics {
        val timed = ArrayList<LyricLine>()
        val plain = ArrayList<LyricLine>()
        for (src in raw.replace("\r", "").lines()) {
            var line = src
            val times = ArrayList<Long>()
            while (true) {
                val m = tag.find(line.trimStart()) ?: break
                if (m.range.first != 0) break
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = when (frac.length) {
                    0 -> 0L
                    1 -> frac.toLong() * 100
                    2 -> frac.toLong() * 10
                    else -> frac.take(3).toLong()
                }
                times.add((min * 60 + sec) * 1000 + ms)
                line = line.trimStart().substring(m.range.last + 1)
            }
            val text = line.replace(wordTag, "").trim()
            if (times.isNotEmpty()) {
                times.forEach { timed.add(LyricLine(it, text)) }
            } else if (!(src.trim().startsWith("[") && src.trim().endsWith("]") && src.contains(":"))) {
                // служебные теги вида [ar:Artist] пропускаем
                plain.add(LyricLine(-1L, src.trim()))
            }
        }
        return if (timed.isNotEmpty()) {
            Lyrics(timed.sortedBy { it.timeMs }, true)
        } else {
            Lyrics(plain, false)
        }
    }
}

/** Тексты хранятся файлами в приватной папке приложения: lyrics/<id трека>.txt */
class LyricsStore(private val context: Context) {
    private val dir = File(context.filesDir, "lyrics").apply { mkdirs() }
    private val offsetPrefs = context.getSharedPreferences("lyrics_offsets", Context.MODE_PRIVATE)
    private val offsets = mutableStateMapOf<Long, Long>()

    /** Растёт при каждом сохранении/удалении — по нему экраны перечитывают текст. */
    var version by mutableIntStateOf(0)
        private set

    /** Сдвиг синхронизации для трека, мс (плюс — текст идёт раньше). */
    fun offset(id: Long): Long = offsets[id] ?: offsetPrefs.getLong(id.toString(), 0L)

    fun nudge(id: Long, deltaMs: Long) {
        val v = (offset(id) + deltaMs).coerceIn(-30_000L, 30_000L)
        offsets[id] = v
        offsetPrefs.edit().putLong(id.toString(), v).apply()
    }

    private fun file(id: Long) = File(dir, "$id.txt")

    suspend fun load(id: Long): String? = withContext(Dispatchers.IO) {
        try {
            file(id).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun save(id: Long, text: String) {
        withContext(Dispatchers.IO) {
            try {
                file(id).writeText(text)
            } catch (e: Exception) {
                // нет места — просто не сохраняем
            }
        }
        version++
    }

    suspend fun delete(id: Long) {
        withContext(Dispatchers.IO) {
            try {
                file(id).delete()
            } catch (e: Exception) {
            }
        }
        version++
    }

    /** Читает выбранный пользователем .lrc / .txt (не больше 512 КБ). */
    suspend fun readUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { s ->
                s.readBytes().takeIf { it.size <= 512 * 1024 }?.toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Ищет текст на lrclib.net (бесплатный сервис без ключа). Приоритет у синхронного. */
    suspend fun fetchOnline(t: Track): String? = withContext(Dispatchers.IO) {
        try {
            val title = URLEncoder.encode(t.title, "UTF-8")
            val hasArtist = !t.artist.startsWith("неизвестн")
            val artist = if (hasArtist) URLEncoder.encode(t.artist, "UTF-8") else ""
            val seconds = t.durationMs / 1000

            var obj: JSONObject? = null
            if (hasArtist) {
                val album = if (t.album.startsWith("неизвестн")) "" else URLEncoder.encode(t.album, "UTF-8")
                val url = "https://lrclib.net/api/get?artist_name=$artist&track_name=$title" +
                    (if (album.isNotEmpty()) "&album_name=$album" else "") +
                    "&duration=$seconds"
                obj = request(url)?.let { JSONObject(it) }
            }
            if (obj == null || pick(obj) == null) {
                val q = URLEncoder.encode(if (hasArtist) "${t.artist} ${t.title}" else t.title, "UTF-8")
                val arr = request("https://lrclib.net/api/search?q=$q")?.let { JSONArray(it) }
                if (arr != null) {
                    var best: String? = null
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val s = o.optString("syncedLyrics", "").takeIf { o.has("syncedLyrics") && !o.isNull("syncedLyrics") && it.isNotBlank() }
                        if (s != null) {
                            best = s
                            break
                        }
                        if (best == null) best = pick(o)
                    }
                    best
                } else {
                    null
                }
            } else {
                pick(obj)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun pick(o: JSONObject): String? {
        fun s(key: String): String? =
            if (o.has(key) && !o.isNull(key)) o.getString(key).takeIf { it.isNotBlank() } else null
        return s("syncedLyrics") ?: s("plainLyrics")
    }

    private fun request(url: String): String? {
        val c = URL(url).openConnection() as HttpURLConnection
        return try {
            c.connectTimeout = 10_000
            c.readTimeout = 10_000
            c.setRequestProperty("User-Agent", "MetroPlayer/1.0 (Android)")
            if (c.responseCode != 200) null else c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}
