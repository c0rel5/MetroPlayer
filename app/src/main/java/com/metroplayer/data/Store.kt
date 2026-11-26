package com.metroplayer.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Избранное, плейлисты, история и настройки. Всё хранится в SharedPreferences. */
class Store(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("metro", Context.MODE_PRIVATE)

    // Имя файла своей гифки вместо ноты (лежит в filesDir/gif). Пусто — стандартная гифка.
    var noteGif by mutableStateOf(prefs.getString("noteGif", "").orEmpty())
        private set

    // 0 — как в системе, 1 — тёмная, 2 — светлая
    var themeMode by mutableIntStateOf(prefs.getInt("theme", 1))
        private set

    // Скин: 0 — Metro, 1 — Windows 7, 2 — Aero, 3 — Material You
    var skin by mutableIntStateOf(prefs.getInt("skin", 0))
        private set

    // Автоматически искать текст песни в интернете (lrclib.net) для текущего и следующего трека
    var autoLyrics by mutableStateOf(prefs.getBoolean("autoLyrics", true))
        private set

    // Показывать текущую строку текста в мини-плеере (уведомление / экран блокировки) вместо исполнителя
    var lyricsInNotif by mutableStateOf(prefs.getBoolean("lyricsInNotif", true))
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

    // Эквалайзер: включён ли, выбранный пресет и усиление пяти полос в дБ
    var eqOn by mutableStateOf(prefs.getBoolean("eqOn", false))
        private set
    var eqPreset by mutableStateOf(prefs.getString("eqPreset", "flat").orEmpty())
        private set
    var eqGains by mutableStateOf(readEqGains())
        private set

    private fun readEqGains(): List<Float> {
        val parsed = prefs.getString("eqGains", null)?.split(",")?.mapNotNull { it.toFloatOrNull() }
        return if (parsed != null && parsed.size == EqPresets.labels.size) parsed else EqPresets.all.first().gains
    }

    private fun saveEq() {
        prefs.edit()
            .putBoolean("eqOn", eqOn)
            .putString("eqPreset", eqPreset)
            .putString("eqGains", eqGains.joinToString(","))
            .apply()
    }

    fun chooseEqOn(v: Boolean) {
        eqOn = v
        saveEq()
    }

    fun chooseEqPreset(id: String) {
        val p = EqPresets.byId(id) ?: return
        eqPreset = id
        eqGains = p.gains
        saveEq()
    }

    /** Ручная правка одной полосы: пресет становится «своим». */
    fun setEqGain(band: Int, db: Float) {
        if (band !in eqGains.indices) return
        val next = eqGains.toMutableList()
        next[band] = db.coerceIn(-EqPresets.MAX_DB, EqPresets.MAX_DB)
        eqGains = next
        eqPreset = "custom"
        saveEq()
    }

    // Daily Mix: на какой день собран, «соль» для ручного обновления и сами треки
    var mixDay by mutableStateOf(prefs.getLong("mixDay", -1L))
        private set
    var mixSalt by mutableStateOf(prefs.getInt("mixSalt", 0))
        private set
    var mixIds by mutableStateOf(readIds("mixIds"))
        private set

    // Главный экран: какие плитки показывать и рисовать ли на них значки
    var showMixTile by mutableStateOf(prefs.getBoolean("showMix", true))
        private set
    var showStatsTile by mutableStateOf(prefs.getBoolean("showStats", true))
        private set
    var tileIcons by mutableStateOf(prefs.getBoolean("tileIcons", true))
        private set

    fun chooseShowMixTile(v: Boolean) {
        showMixTile = v
        prefs.edit().putBoolean("showMix", v).apply()
    }

    fun chooseShowStatsTile(v: Boolean) {
        showStatsTile = v
        prefs.edit().putBoolean("showStats", v).apply()
    }

    fun chooseTileIcons(v: Boolean) {
        tileIcons = v
        prefs.edit().putBoolean("tileIcons", v).apply()
    }

    // вести ли журнал прослушиваний (статистика и Daily Mix)
    var statsOn by mutableStateOf(prefs.getBoolean("statsOn", true))
        private set

    fun saveMix(day: Long, salt: Int, ids: List<Long>) {
        mixDay = day
        mixSalt = salt
        mixIds = ids
        prefs.edit().putLong("mixDay", day).putInt("mixSalt", salt).putString("mixIds", ids.joinToString(",")).apply()
    }

    fun chooseStatsOn(v: Boolean) {
        statsOn = v
        prefs.edit().putBoolean("statsOn", v).apply()
    }

    // Анимация текста песни: 0 — новая (плывёт, слова загораются), 1 — старая (слайд окна строк)
    var lyricsAnim by mutableIntStateOf(prefs.getInt("lyricsAnim", 0))
        private set

    // Формат, который автоматически выбирается в фильтре списков («flac»); пусто — показывать все
    var defaultFormat by mutableStateOf(prefs.getString("defaultFormat", "").orEmpty())
        private set

    // Ряд кнопок под управлением на экране «сейчас играет»: перемешать · повтор · избранное
    var quickBar by mutableStateOf(prefs.getBoolean("quickBar", true))
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

    fun chooseAutoLyrics(v: Boolean) {
        autoLyrics = v
        prefs.edit().putBoolean("autoLyrics", v).apply()
    }

    fun chooseLyricsInNotif(v: Boolean) {
        lyricsInNotif = v
        prefs.edit().putBoolean("lyricsInNotif", v).apply()
    }

    fun chooseSkin(v: Int) {
        skin = v
        prefs.edit().putInt("skin", v).apply()
    }

    fun chooseLyricsAnim(v: Int) {
        lyricsAnim = v
        prefs.edit().putInt("lyricsAnim", v).apply()
    }

    fun chooseDefaultFormat(v: String) {
        defaultFormat = v
        prefs.edit().putString("defaultFormat", v).apply()
    }

    fun chooseQuickBar(v: Boolean) {
        quickBar = v
        prefs.edit().putBoolean("quickBar", v).apply()
    }

    fun chooseNowMode(v: Int) {
        nowMode = v
        prefs.edit().putInt("nowMode", v).apply()
    }

    fun chooseLyricsMode(v: Int) {
        lyricsMode = v
        prefs.edit().putInt("lyricsMode", v).apply()
    }

    private fun gifDir() = File(appContext.filesDir, "gif")

    /** Файл своей гифки или null, если выбрана стандартная (или файл пропал). */
    fun noteGifFile(): File? =
        noteGif.takeIf { it.isNotEmpty() }?.let { File(gifDir(), it) }?.takeIf { it.exists() }

    /**
     * Копирует выбранную пользователем картинку/гифку в приватную папку приложения
     * (доступ к исходному файлу потом не нужен). Возвращает false, если файл не подошёл.
     */
    suspend fun importNoteGif(uri: Uri): Boolean {
        val name = withContext(Dispatchers.IO) {
            try {
                val dir = gifDir().apply { mkdirs() }
                val out = File(dir, "note_${System.currentTimeMillis()}.gif")
                var total = 0L
                var tooBig = false
                val opened = appContext.contentResolver.openInputStream(uri)?.use { input ->
                    out.outputStream().use { o ->
                        val buf = ByteArray(64 * 1024)
                        var n = input.read(buf)
                        while (n >= 0) {
                            total += n
                            if (total > MAX_GIF_BYTES) {
                                tooBig = true
                                break
                            }
                            o.write(buf, 0, n)
                            n = input.read(buf)
                        }
                    }
                    true
                } ?: false
                if (!opened || tooBig || total == 0L) {
                    out.delete()
                    null
                } else {
                    out.name
                }
            } catch (e: Exception) {
                null
            }
        } ?: return false
        val old = noteGif
        noteGif = name
        prefs.edit().putString("noteGif", name).apply()
        if (old.isNotEmpty()) withContext(Dispatchers.IO) { File(gifDir(), old).delete() }
        return true
    }

    /** Вернуть стандартную гифку и удалить свою. */
    fun resetNoteGif() {
        val old = noteGif
        noteGif = ""
        prefs.edit().putString("noteGif", "").apply()
        if (old.isNotEmpty()) File(gifDir(), old).delete()
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

    private companion object {
        const val MAX_GIF_BYTES = 12L * 1024 * 1024
    }
}
