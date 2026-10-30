package com.metroplayer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** Избранное, плейлисты, история и настройки. Всё хранится в SharedPreferences. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("metro", Context.MODE_PRIVATE)

    // 0 — как в системе, 1 — тёмная, 2 — светлая
    var themeMode by mutableIntStateOf(prefs.getInt("theme", 1))
        private set

    // ARGB, 0 — цвет по умолчанию для темы
    var accent by mutableIntStateOf(prefs.getInt("accent", 0))
        private set

    // 0 — синхронно (автопрокрутка, тап по строке — перемотка), 1 — выделение (статичный текст, можно копировать)
    var lyricsMode by mutableIntStateOf(prefs.getInt("lyricsMode", 0))
        private set

    // Экран «сейчас играет»: 0 — обложка (+ полоска текста), 1 — текст вместо обложки
    var nowMode by mutableIntStateOf(prefs.getInt("nowMode", 0))
        private set

    var favorites by mutableStateOf(readIds("fav").toSet())
        private set

    var history by mutableStateOf(readIds("history"))
        private set

    var playlists by mutableStateOf(readPlaylists())
        private set

    @JvmName("applyThemeMode")
    fun setThemeMode(v: Int) {
        themeMode = v
        prefs.edit().putInt("theme", v).apply()
    }

    @JvmName("applyAccent")
    fun setAccent(argb: Int) {
        accent = argb
        prefs.edit().putInt("accent", argb).apply()
    }

    fun chooseNowMode(v: Int) {
        nowMode = v
        prefs.edit().putInt("nowMode", v).apply()
    }

    fun chooseLyricsMode(v: Int) {
        lyricsMode = v
        prefs.edit().putInt("lyricsMode", v).apply()
    }

    fun isFavorite(id: Long) = id in favorites

    fun toggleFavorite(id: Long) {
        favorites = if (id in favorites) favorites - id else favorites + id
        prefs.edit().putString("fav", favorites.joinToString(",")).apply()
    }

    fun pushHistory(id: Long) {
        val next = ArrayList<Long>(history.size + 1)
        next.add(id)
        for (h in history) if (h != id) next.add(h)
        history = next.take(100)
        prefs.edit().putString("history", history.joinToString(",")).apply()
    }

    fun createPlaylist(name: String): Long {
        val id = System.currentTimeMillis()
        playlists = playlists + Playlist(id, name.trim().ifEmpty { "плейлист" }, emptyList())
        savePlaylists()
        return id
    }

    fun deletePlaylist(id: Long) {
        playlists = playlists.filter { it.id != id }
        savePlaylists()
    }

    fun addToPlaylist(playlistId: Long, trackId: Long) {
        playlists = playlists.map {
            if (it.id == playlistId && trackId !in it.trackIds) it.copy(trackIds = it.trackIds + trackId) else it
        }
        savePlaylists()
    }

    fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        playlists = playlists.map {
            if (it.id == playlistId) it.copy(trackIds = it.trackIds.filter { t -> t != trackId }) else it
        }
        savePlaylists()
    }

    private fun readIds(key: String): List<Long> =
        prefs.getString(key, "").orEmpty().split(",").mapNotNull { it.toLongOrNull() }

    private fun readPlaylists(): List<Playlist> {
        val s = prefs.getString("playlists", null) ?: return emptyList()
        return try {
            val arr = JSONArray(s)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val t = o.getJSONArray("t")
                Playlist(o.getLong("id"), o.getString("n"), (0 until t.length()).map { t.getLong(it) })
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun savePlaylists() {
        val arr = JSONArray()
        playlists.forEach { p ->
            val ids = JSONArray()
            p.trackIds.forEach { ids.put(it) }
            arr.put(JSONObject().put("id", p.id).put("n", p.name).put("t", ids))
        }
        prefs.edit().putString("playlists", arr.toString()).apply()
    }
}
