package com.metroplayer.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.metroplayer.MetroApp

@Composable
fun MetroRoot(app: MetroApp, granted: Boolean, requestPermission: () -> Unit) {
    val store = app.store
    val dark = when (store.themeMode) {
        1 -> true
        2 -> false
        else -> isSystemInDarkTheme()
    }

    MetroTheme(dark, store.accent) {
        val c = Metro.colors
        val view = LocalView.current
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val ctrl = WindowCompat.getInsetsController(window, view)
                ctrl.isAppearanceLightStatusBars = !dark
                ctrl.isAppearanceLightNavigationBars = !dark
            }
        }

        LaunchedEffect(Unit) { app.player.connect() }
        LaunchedEffect(granted) { if (granted) app.repo.load() }

        Box(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
            if (granted) Navigator(app) else PermissionScreen(requestPermission)
        }
    }
}

@Composable
private fun Navigator(app: MetroApp) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    var dialog by remember { mutableStateOf<DialogKind?>(null) }

    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

    val push: (Screen) -> Unit = { s -> if (stack.last() != s) stack.add(s) }
    val pop: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    val top = stack.last()

    // «Турникет» — фирменный переход Windows Phone: страница заходит, вращаясь вокруг левого края.
    AnimatedContent(
        targetState = top,
        transitionSpec = { fadeIn(tween(220, delayMillis = 100)) togetherWith fadeOut(tween(140)) },
        label = "nav"
    ) { screen ->
        val rot by transition.animateFloat(transitionSpec = { tween(300) }, label = "rot") { s ->
            when (s) {
                EnterExitState.PreEnter -> -70f
                EnterExitState.Visible -> 0f
                EnterExitState.PostExit -> 70f
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rot
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    cameraDistance = 14f * density
                }
        ) {
            ScreenHost(
                screen = screen,
                app = app,
                push = push,
                pop = pop,
                onAdd = { id -> dialog = DialogKind.AddToPlaylist(id) },
                onNewPlaylist = { dialog = DialogKind.NewPlaylist(null) }
            )
        }
    }

    when (val d = dialog) {
        is DialogKind.AddToPlaylist -> AddToPlaylistDialog(
            app = app,
            trackId = d.trackId,
            onNew = { dialog = DialogKind.NewPlaylist(d.trackId) },
            onDismiss = { dialog = null }
        )
        is DialogKind.NewPlaylist -> NewPlaylistDialog(
            onCreate = { name ->
                val id = app.store.createPlaylist(name)
                d.thenAdd?.let { app.store.addToPlaylist(id, it) }
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        null -> Unit
    }
}

@Composable
private fun ScreenHost(
    screen: Screen,
    app: MetroApp,
    push: (Screen) -> Unit,
    pop: () -> Unit,
    onAdd: (Long) -> Unit,
    onNewPlaylist: () -> Unit
) {
    val repo = app.repo
    val store = app.store
    when (screen) {
        Screen.Home -> HomeScreen(app, push)
        Screen.NowPlaying -> NowPlayingScreen(app, push, onAdd)
        Screen.Artists -> ArtistsScreen(app, push)
        Screen.Albums -> AlbumsScreen(app, push)
        Screen.Playlists -> PlaylistsScreen(app, push, onNewPlaylist)
        Screen.Favorites -> TrackListScreen(
            app, "МУЗЫКА", "избранное",
            repo.tracks.filter { it.id in store.favorites }, push, pop, onAdd
        )
        Screen.AllTracks -> TrackListScreen(app, "МУЗЫКА", "все треки", repo.tracks, push, pop, onAdd)
        Screen.Settings -> SettingsScreen(app, push)
        Screen.About -> AboutScreen(app, push)
        is Screen.ArtistTracks -> TrackListScreen(
            app, "ИСПОЛНИТЕЛЬ", screen.name,
            repo.tracks.filter { it.artist == screen.name }, push, pop, onAdd
        )
        is Screen.AlbumTracks -> {
            val album = repo.albums.firstOrNull { it.id == screen.albumId }
            TrackListScreen(
                app, "АЛЬБОМ", album?.name ?: "альбом",
                repo.tracks.filter { it.albumId == screen.albumId }.sortedBy { it.trackNo },
                push, pop, onAdd
            )
        }
        is Screen.PlaylistTracks -> {
            val p = store.playlists.firstOrNull { it.id == screen.id }
            TrackListScreen(
                app, "ПЛЕЙЛИСТ", p?.name ?: "плейлист",
                p?.trackIds?.mapNotNull { repo.find(it) }.orEmpty(),
                push, pop, onAdd, playlistId = screen.id
            )
        }
    }
}
