package com.metroplayer

import android.app.Application
import androidx.compose.runtime.snapshotFlow
import com.metroplayer.data.LyricsStore
import com.metroplayer.data.MusicRepository
import com.metroplayer.data.Store
import com.metroplayer.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        scope.launch {
            if (lyrics.load(id) != null) return@launch
            val t = repo.find(id) ?: return@launch
            inFlight.add(id)
            try {
                val res = lyrics.fetchOnline(t)
                if (res != null) lyrics.save(id, res) else misses[id] = System.currentTimeMillis()
            } finally {
                inFlight.remove(id)
            }
        }
    }
}
