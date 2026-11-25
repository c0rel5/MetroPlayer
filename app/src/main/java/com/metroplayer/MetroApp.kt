package com.metroplayer

import android.app.Application
import androidx.compose.runtime.snapshotFlow
import com.metroplayer.data.LastFm
import com.metroplayer.data.LyricsStore
import com.metroplayer.data.MusicRepository
import com.metroplayer.data.Scrobble
import com.metroplayer.data.Store
import com.metroplayer.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MetroApp : Application() {
    lateinit var repo: MusicRepository
    lateinit var store: Store
    lateinit var player: PlayerController
    lateinit var lyrics: LyricsStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val misses = HashMap<Long, Long>()   // id трека -> когда не нашли (повтор не раньше чем через 30 минут)
    private val inFlight = HashSet<Long>()

    override fun onCreate() {
        super.onCreate()
        repo = MusicRepository(this)
        store = Store(this)
        lyrics = LyricsStore(this)
        player = PlayerController(this, repo, store)
        startAutoLyrics()
        startScrobbler()
    }

    // ---------- Last.fm ----------
    private var scId = -1L              // трек, который сейчас учитывается
    private var scStartSec = 0L         // когда он начал играть (для метки времени скроббла)
    private var scAccumMs = 0L          // сколько реально прослушано
    private var scScrobbled = false
    private var scNowSent = false
    private var scLastPos = 0L
    private var scLastTick = 0L
    private var scTicks = 0
    private var flushing = false

    private fun startScrobbler() {
        scope.launch {
            while (true) {
                delay(1000)
                try {
                    scrobbleTick()
                } catch (e: Exception) {
                    // следующий тик попробует снова
                }
            }
        }
    }

    private fun scrobbleTick() {
        val now = System.currentTimeMillis()
        val dt = (now - scLastTick).coerceIn(0L, 2500L)
        scLastTick = now
        if (!store.lastfmReady) {
            scId = -1L
            return
        }
        scTicks++
        if (scTicks % 60 == 0 && store.readScrobbleQueue().isNotEmpty()) scope.launch { flushScrobbles(null) }
        val st = player.state
        val t = st.current
        if (t == null) {
            scId = -1L
            return
        }
        val pos = player.positionMs()
        // повтор того же трека (режим «повторять один»): позиция прыгнула в начало после скроббла
        val restarted = scScrobbled && pos < scLastPos - 5000L && pos < 10_000L
        scLastPos = pos
        if (t.id != scId || restarted) {
            scId = t.id
            scStartSec = now / 1000
            scAccumMs = 0L
            scScrobbled = false
            scNowSent = false
        }
        if (!st.isPlaying) return
        scAccumMs += dt
        val validArtist = t.artist.isNotBlank() && t.artist != "<unknown>"
        if (!validArtist) return
        if (!scNowSent) {
            scNowSent = true
            scope.launch {
                LastFm.nowPlaying(store.lastfmKey, store.lastfmSecret, store.lastfmSession, t.artist, t.title, t.album, (t.durationMs / 1000).toInt())
            }
        }
        val dur = if (st.durationMs > 0) st.durationMs else t.durationMs
        // правило Last.fm: трек длиннее 30 секунд, прослушана половина или 4 минуты
        if (!scScrobbled && dur >= 30_000L && scAccumMs >= minOf(dur / 2, 240_000L)) {
            scScrobbled = true
            val item = Scrobble(t.artist, t.title, t.album, (dur / 1000).toInt(), scStartSec)
            scope.launch { flushScrobbles(item) }
        }
    }

    private suspend fun flushScrobbles(add: Scrobble?) {
        if (add != null) {
            val q = store.readScrobbleQueue().toMutableList()
            q.add(add)
            while (q.size > 200) q.removeAt(0)
            store.writeScrobbleQueue(q)
        }
        if (flushing) return
        flushing = true
        try {
            while (true) {
                val first = store.readScrobbleQueue().firstOrNull() ?: break
                val res = LastFm.scrobble(store.lastfmKey, store.lastfmSecret, store.lastfmSession, first)
                if (res == LastFm.Result.RETRY) break
                // перечитываем: пока шёл запрос, в очередь могли добавить новые
                store.writeScrobbleQueue(store.readScrobbleQueue().drop(1))
            }
        } finally {
            flushing = false
        }
    }

    /** Следит за очередью: для текущего и следующего трека ищет текст, если его ещё нет. */
    private fun startAutoLyrics() {
        scope.launch {
            snapshotFlow {
                val s = player.state
                s.queue.getOrNull(s.currentIndex)?.id to s.queue.getOrNull(s.currentIndex + 1)?.id
            }.distinctUntilChanged().collect { (cur, next) ->
                cur?.let { fetchIfMissing(it) }
                next?.let { fetchIfMissing(it) }
            }
        }
    }

    private fun fetchIfMissing(id: Long) {
        if (!store.autoLyrics || id in inFlight) return
        val missAt = misses[id]
        if (missAt != null && System.currentTimeMillis() - missAt < 30 * 60_000L) return
        // помечаем сразу, а не после suspend-вызова: иначе один трек могли искать два раза параллельно
        inFlight.add(id)
        scope.launch {
            try {
                if (lyrics.load(id) != null) return@launch
                val t = repo.find(id) ?: return@launch
                val res = lyrics.fetchOnline(t)
                if (res != null) lyrics.save(id, res) else misses[id] = System.currentTimeMillis()
            } finally {
                inFlight.remove(id)
            }
        }
    }

    /** Перечитать музыку с устройства (например, после возвращения в приложение). */
    fun refreshLibrary() {
        scope.launch { if (repo.loaded) repo.load() }
    }
}
