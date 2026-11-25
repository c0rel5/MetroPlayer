package com.metroplayer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

/** Одно прослушивание, которое нужно отправить в Last.fm. [timestamp] — когда трек начал играть (секунды Unix). */
data class Scrobble(
    val artist: String,
    val title: String,
    val album: String,
    val durationSec: Int,
    val timestamp: Long
)

class LastFmLogin(val session: String?, val name: String?, val error: String?)

/** Минимальный клиент Last.fm Scrobbling 2.0: вход, «сейчас играет» и скроббл. */
object LastFm {
    enum class Result { OK, RETRY, DROP }

    private const val ENDPOINT = "https://ws.audioscrobbler.com/2.0/"

    private fun md5(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    /** POST с подписью: параметры по алфавиту + секрет, md5. Параметр format в подпись не входит. */
    private fun call(params: Map<String, String>, secret: String): JSONObject? {
        val sorted = params.toSortedMap()
        val sig = StringBuilder()
        sorted.forEach { (k, v) -> sig.append(k).append(v) }
        sig.append(secret)
        val all = LinkedHashMap<String, String>(sorted)
        all["api_sig"] = md5(sig.toString())
        all["format"] = "json"
        val body = all.entries.joinToString("&") {
            URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
        }
        val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "MetroPlayer")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: return null
            return JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    suspend fun login(key: String, secret: String, user: String, pass: String): LastFmLogin =
        withContext(Dispatchers.IO) {
            try {
                val j = call(
                    mapOf(
                        "method" to "auth.getMobileSession",
                        "api_key" to key,
                        "username" to user,
                        "password" to pass
                    ),
                    secret
                )
                val s = j?.optJSONObject("session")
                if (s != null) {
                    LastFmLogin(s.optString("key"), s.optString("name"), null)
                } else {
                    val msg = j?.optString("message").orEmpty()
                    LastFmLogin(null, null, if (msg.isEmpty()) "нет ответа от Last.fm" else msg)
                }
            } catch (e: Exception) {
                LastFmLogin(null, null, "нет сети или Last.fm недоступен")
            }
        }

    /** «Сейчас играет» — не критично, при ошибке не повторяется. */
    suspend fun nowPlaying(key: String, secret: String, session: String, artist: String, title: String, album: String, durationSec: Int) {
        withContext(Dispatchers.IO) {
            try {
                val p = HashMap<String, String>()
                p["method"] = "track.updateNowPlaying"
                p["api_key"] = key
                p["sk"] = session
                p["artist"] = artist
                p["track"] = title
                if (album.isNotBlank() && album != "<unknown>") p["album"] = album
                if (durationSec > 0) p["duration"] = durationSec.toString()
                call(p, secret)
            } catch (e: Exception) {
                // не повторяем
            }
        }
    }

    suspend fun scrobble(key: String, secret: String, session: String, s: Scrobble): Result =
        withContext(Dispatchers.IO) {
            try {
                val p = HashMap<String, String>()
                p["method"] = "track.scrobble"
                p["api_key"] = key
                p["sk"] = session
                p["artist"] = s.artist
                p["track"] = s.title
                p["timestamp"] = s.timestamp.toString()
                if (s.album.isNotBlank() && s.album != "<unknown>") p["album"] = s.album
                if (s.durationSec > 0) p["duration"] = s.durationSec.toString()
                val j = call(p, secret) ?: return@withContext Result.RETRY
                if (j.has("error")) {
                    // 11 и 16 — временные сбои сервиса, остальное повторять бесполезно
                    if (j.optInt("error") == 11 || j.optInt("error") == 16) Result.RETRY else Result.DROP
                } else {
                    Result.OK
                }
            } catch (e: Exception) {
                Result.RETRY
            }
        }
}
