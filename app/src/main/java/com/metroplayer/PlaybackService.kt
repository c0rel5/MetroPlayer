package com.metroplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.snapshotFlow
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.metroplayer.data.LrcParser
import com.metroplayer.data.Lyrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val metroApp get() = application as MetroApp

    // кнопки мини-плеера (уведомление / экран блокировки)
    private val favCmd = SessionCommand("metro.FAV", Bundle.EMPTY)
    private val shuffleCmd = SessionCommand("metro.SHUFFLE", Bundle.EMPTY)
    private val repeatCmd = SessionCommand("metro.REPEAT", Bundle.EMPTY)

    // текущая строка текста вместо исполнителя
    private var lyricsId = -1L
    private var lyricsVersion = -1
    private var parsed: Lyrics? = null
    private var shownText: String? = null

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(favCmd).add(shuffleCmd).add(repeatCmd)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .setCustomLayout(buildLayout())
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            val p = session.player
            when (customCommand.customAction) {
                "metro.FAV" -> p.currentMediaItem?.mediaId?.toLongOrNull()?.let { metroApp.store.toggleFavorite(it) }
                "metro.SHUFFLE" -> p.shuffleModeEnabled = !p.shuffleModeEnabled
                "metro.REPEAT" -> p.repeatMode = when (p.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            }
            refreshLayout()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            updateLyricLine()
            handler.postDelayed(this, 400L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .setCallback(sessionCallback)
            .build()

        player.addListener(object : Player.Listener {
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = refreshLayout()
            override fun onRepeatModeChanged(repeatMode: Int) = refreshLayout()
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                shownText = null
                refreshLayout()
                updateLyricLine()
            }
        })

        // «избранное» можно менять и из самого приложения — обновляем сердечко в мини-плеере
        scope.launch {
            snapshotFlow { metroApp.store.favorites }.collect { refreshLayout() }
        }
        handler.post(tick)

        // иконка ноты + цвет акцента (0 — стандартный синий Metro)
        setMediaNotificationProvider(
            MetroNotificationProvider(
                this,
                accent = { metroApp.store.accent.takeIf { it != 0 } ?: 0xFF1BA1E2.toInt() },
                skin = { metroApp.store.skin }
            )
        )
    }

    private fun buildLayout(): List<CommandButton> {
        val p = session?.player
        val id = p?.currentMediaItem?.mediaId?.toLongOrNull()
        val fav = id != null && metroApp.store.isFavorite(id)
        val shuffle = p?.shuffleModeEnabled == true
        val repeat = p?.repeatMode ?: Player.REPEAT_MODE_OFF
        return listOf(
            CommandButton.Builder()
                .setDisplayName(if (fav) "Убрать из избранного" else "В избранное")
                .setIconResId(if (fav) R.drawable.ic_cmd_heart_on else R.drawable.ic_cmd_heart_off)
                .setSessionCommand(favCmd)
                .build(),
            CommandButton.Builder()
                .setDisplayName(if (shuffle) "Перемешивание: вкл" else "Перемешивание: выкл")
                .setIconResId(if (shuffle) R.drawable.ic_cmd_shuffle_on else R.drawable.ic_cmd_shuffle_off)
                .setSessionCommand(shuffleCmd)
                .build(),
            CommandButton.Builder()
                .setDisplayName(
                    when (repeat) {
                        Player.REPEAT_MODE_ONE -> "Повтор: один трек"
                        Player.REPEAT_MODE_ALL -> "Повтор: всё"
                        else -> "Повтор: выкл"
                    }
                )
                .setIconResId(
                    when (repeat) {
                        Player.REPEAT_MODE_ONE -> R.drawable.ic_cmd_repeat_one
                        Player.REPEAT_MODE_ALL -> R.drawable.ic_cmd_repeat_all
                        else -> R.drawable.ic_cmd_repeat_off
                    }
                )
                .setSessionCommand(repeatCmd)
                .build()
        )
    }

    private fun refreshLayout() {
        session?.setCustomLayout(buildLayout())
    }

    /**
     * В мини-плеере вместо исполнителя показываем текущую строку песни (если текст синхронный).
     * Исполнитель и строка передаются через playlistMetadata — в MediaItem исполнителя нет
     * (см. toMediaItem(hideArtist)), поэтому именно эта подмена попадает в уведомление.
     */
    private fun updateLyricLine() {
        val p = session?.player ?: return
        val item = p.currentMediaItem ?: return
        val id = item.mediaId.toLongOrNull() ?: return
        val app = metroApp

        if (id != lyricsId || app.lyrics.version != lyricsVersion) {
            lyricsId = id
            lyricsVersion = app.lyrics.version
            parsed = null
            scope.launch {
                val raw = app.lyrics.load(id)
                if (id == lyricsId) parsed = raw?.let { LrcParser.parse(it) }
            }
        }

        var text = app.repo.find(id)?.artist.orEmpty()
        val l = parsed
        if (app.store.lyricsInNotif && l != null && l.synced) {
            val pos = p.currentPosition + app.lyrics.offset(id)
            val line = l.lines.getOrNull(l.lines.indexOfLast { it.timeMs <= pos })?.text
            if (!line.isNullOrBlank()) text = line
        }
        if (text != shownText) {
            shownText = text
            p.playlistMetadata = MediaMetadata.Builder().setArtist(text).build()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        scope.cancel()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
