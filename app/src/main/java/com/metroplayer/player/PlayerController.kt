package com.metroplayer.player

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.metroplayer.PlaybackService
import com.metroplayer.data.MusicRepository
import com.metroplayer.data.Store
import com.metroplayer.data.Track

@Immutable
data class PlayerState(
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val currentIndex: Int = -1,
    val queue: List<Track> = emptyList(),
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f
)

fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artUri)
                .build()
        )
        .build()

class PlayerController(
    private val context: Context,
    private val repo: MusicRepository,
    private val store: Store
) {
    var state by mutableStateOf(PlayerState())
        private set

    /** Выбранный таймер сна, минуты (0 — выключен). */
    var sleepMinutes by mutableIntStateOf(0)
        private set

    private val handler = Handler(Looper.getMainLooper())
    private val sleepRunnable = Runnable {
        sleepMinutes = 0
        controller?.pause()
    }

    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()
    private var lastId: Long = -1L

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh(rebuildQueue = events.contains(Player.EVENT_TIMELINE_CHANGED))
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId?.toLongOrNull() ?: -1L
            if (lastId != -1L && lastId != id) store.pushHistory(lastId)
            lastId = id
        }
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener({
            try {
                val c = f.get()
                controller = c
                lastId = c.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L
                c.addListener(listener)
                refresh(rebuildQueue = true)
                pending.toList().forEach { it(c) }
                pending.clear()
            } catch (e: Exception) {
                future = null
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            block(c)
        } else {
            pending.add(block)
            connect()
        }
    }

    private fun trackOf(item: MediaItem): Track {
        val id = item.mediaId.toLongOrNull() ?: -1L
        return repo.find(id) ?: Track(
            id = id,
            title = item.mediaMetadata.title?.toString() ?: "без названия",
            artist = item.mediaMetadata.artist?.toString() ?: "",
            album = item.mediaMetadata.albumTitle?.toString() ?: "",
            albumId = 0L,
            durationMs = 0L,
            trackNo = 0
        )
    }

    private fun refresh(rebuildQueue: Boolean) {
        val c = controller ?: return
        val item = c.currentMediaItem
        val cur = item?.let { trackOf(it) }
        var queue = state.queue
        if (rebuildQueue) {
            queue = (0 until c.mediaItemCount).map { trackOf(c.getMediaItemAt(it)) }
        }
        val dur = c.duration
        val playing = c.playWhenReady &&
            c.playbackState != Player.STATE_ENDED &&
            c.playbackState != Player.STATE_IDLE
        state = PlayerState(
            current = cur,
            isPlaying = playing,
            durationMs = if (dur != C.TIME_UNSET && dur > 0) dur else (cur?.durationMs ?: 0L),
            currentIndex = c.currentMediaItemIndex,
            queue = queue,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            speed = c.playbackParameters.speed
        )
    }

    fun positionMs(): Long = controller?.currentPosition ?: 0L

    fun playTracks(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        withController { c ->
            c.setMediaItems(tracks.map { it.toMediaItem() }, startIndex.coerceIn(0, tracks.size - 1), 0L)
            c.prepare()
            c.play()
        }
    }

    fun togglePlay() = withController { c ->
        if (c.playWhenReady && c.playbackState != Player.STATE_ENDED) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0L)
            c.play()
        }
    }

    fun next() = withController { it.seekToNext() }

    fun previous() = withController { it.seekToPrevious() }

    fun seekTo(ms: Long) = withController { it.seekTo(ms) }

    fun seekToIndex(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) {
            c.seekTo(index, 0L)
            c.play()
        }
    }

    fun playNext(track: Track) = withController { c ->
        val at = if (c.mediaItemCount == 0) 0 else c.currentMediaItemIndex + 1
        c.addMediaItem(at, track.toMediaItem())
        if (c.mediaItemCount == 1) {
            c.prepare()
            c.play()
        }
    }

    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /** Таймер сна: выкл → 15 → 30 → 45 → 60 → выкл. */
    fun cycleSleep() {
        val steps = intArrayOf(0, 15, 30, 45, 60)
        val next = steps[(steps.indexOf(sleepMinutes) + 1) % steps.size]
        handler.removeCallbacks(sleepRunnable)
        sleepMinutes = next
        if (next > 0) handler.postDelayed(sleepRunnable, next * 60_000L)
    }

    /** Скорость: 0.75× → 1× → 1.25× → 1.5× → 2× → 0.75×. */
    fun cycleSpeed() = withController { c ->
        val speeds = floatArrayOf(0.75f, 1f, 1.25f, 1.5f, 2f)
        val cur = c.playbackParameters.speed
        val i = speeds.indexOfFirst { kotlin.math.abs(it - cur) < 0.01f }
        c.setPlaybackSpeed(speeds[(i + 1) % speeds.size])
    }
}
