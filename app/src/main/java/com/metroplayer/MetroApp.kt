package com.metroplayer

import android.app.Application
import androidx.compose.runtime.snapshotFlow
import com.metroplayer.data.DailyMix
import com.metroplayer.data.LyricsStore
import com.metroplayer.data.MusicRepository
import com.metroplayer.data.PlayLog
import com.metroplayer.data.Stats
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
    lateinit var playLog: PlayLog

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val misses = HashMap<Long, Long>()   // id трека -> когда не нашли (повтор не раньше чем через 30 минут)
    private val inFlight = HashSet<Long>()

    override fun onCreate() {
        super.onCreate()
        repo = MusicRepository(this)
        store = Store(this)
        lyrics = LyricsStore(this)
        playLog = PlayLog(this)
        player = PlayerController(this, repo, store)
        startAutoLyrics()
        startPlayTracker()
    }

    // ---------- Статистика прослушиваний ----------
    private var trId = -1L            // трек, который сейчас учитывается
    private var trAccumMs = 0L        // сколько он уже реально проигран
    private var trLogged = false      // засчитан ли как прослушивание
    private var trPendingMs = 0L      // время, которое ещё не записано в журнал
    private var trLastPos = 0L
    private var trLastTick = 0L

    private fun startPlayTracker() {
        scope.launch {
            while (true) {
                delay(1000)
                try {
                    trackTick()
                } catch (e: Exception) {
                    // следующий тик попробует снова
                }
            }
        }
    }

    private fun flushListen() {
        if (trPendingMs >= 1000L) playLog.addListen(trPendingMs)
        trPendingMs = 0L
    }

    private fun trackTick() {
        val now = System.currentTimeMillis()
        val dt = (now - trLastTick).coerceIn(0L, 2500L)
        trLastTick = now
        if (!store.statsOn) {
            trPendingMs = 0L
            return
        }
        val st = player.state
        val t = st.current
        val id = t?.id ?: -1L
        if (id != trId) {
            flushListen()
            trId = id
            trAccumMs = 0L
            trLogged = false
        }
        if (t == null || !st.isPlaying) {
            flushListen()
            return
        }
        // повтор того же трека (режим «повторять один»): позиция прыгнула в начало
        val pos = player.positionMs()
        if (trLogged && pos < trLastPos - 5000L && pos < 10_000L) {
            trAccumMs = 0L
            trLogged = false
        }
        trLastPos = pos
        trAccumMs += dt
        trPendingMs += dt
        if (trPendingMs >= 20_000L) flushListen()
        val dur = if (st.durationMs > 0) st.durationMs else t.durationMs
        // прослушивание засчитывается после 30 секунд или половины короткого трека
        if (!trLogged && trAccumMs >= minOf(30_000L, (dur / 2).coerceAtLeast(5_000L))) {
            trLogged = true
            playLog.addPlay(id)
        }
    }

    /** Собирает Daily Mix на сегодня (или заново, если [force]). Один и тот же день даёт одну и ту же подборку. */
    fun ensureDailyMix(force: Boolean = false) {
        if (!repo.loaded) return
        val today = Stats.today()
        if (!force && store.mixDay == today && store.mixIds.isNotEmpty()) return
        val salt = if (force) store.mixSalt + 1 else store.mixSalt
        store.saveMix(today, salt, DailyMix.build(repo.tracks, playLog.plays, store.favorites, today, salt))
    }

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
