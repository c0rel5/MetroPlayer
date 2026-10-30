package com.metroplayer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metroplayer.MetroApp
import com.metroplayer.data.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data object NowPlaying : Screen
    data object Artists : Screen
    data object Albums : Screen
    data object Playlists : Screen
    data object Favorites : Screen
    data object AllTracks : Screen
    data object Settings : Screen
    data object About : Screen
    data class ArtistTracks(val name: String) : Screen
    data class AlbumTracks(val albumId: Long) : Screen
    data class PlaylistTracks(val id: Long) : Screen
}

sealed interface DialogKind {
    data class AddToPlaylist(val trackId: Long) : DialogKind
    data class NewPlaylist(val thenAdd: Long?) : DialogKind
}

/* ═══════════════════ 2 · Живые плитки ═══════════════════ */

private val TileBlue = Color(0xFF0078D7)

@Composable
private fun Tile(
    color: Color,
    w: Dp,
    h: Dp,
    label: String,
    onClick: () -> Unit,
    top: @Composable () -> Unit
) {
    Box(
        Modifier
            .size(w, h)
            .metroPress(tilt = true, onClick = onClick)
            .background(color)
            .padding(12.dp)
    ) {
        Box(Modifier.align(Alignment.TopStart)) { top() }
        MText(label, 16, Modifier.align(Alignment.BottomStart), FontWeight.SemiBold, Color.White, maxLines = 2)
    }
}

@Composable
private fun CountTile(count: Int, label: String, color: Color, size: Dp, onClick: () -> Unit) {
    Tile(color, size, size, label, onClick) {
        MText("$count", 44, color = Color.White, lh = 1f)
    }
}

/** «Живая» плитка: каждые 5 секунд переворачивается между названием и обложкой. */
@Composable
private fun LiveTile(app: MetroApp, w: Dp, h: Dp, onClick: () -> Unit) {
    val st = app.player.state
    val cur = st.current
    var face by remember { mutableIntStateOf(0) }
    val active = cur != null
    LaunchedEffect(active) {
        face = 0
        if (active) {
            while (true) {
                delay(5000)
                face = 1 - face
            }
        }
    }
    Box(
        Modifier
            .size(w, h)
            .metroPress(tilt = true, onClick = onClick)
            .background(TileBlue)
            .clipToBounds()
    ) {
        AnimatedContent(
            targetState = face,
            transitionSpec = { slideInVertically { it } togetherWith slideOutVertically { -it } },
            label = "live"
        ) { f ->
            if (f == 0 || cur == null) {
                Box(Modifier.fillMaxSize().padding(12.dp)) {
                    MetroIcon(
                        if (st.isPlaying) Ic.PAUSE_CIRCLE else Ic.PLAY_CIRCLE,
                        44.dp, Color.White, Modifier.align(Alignment.TopStart)
                    )
                    MText(
                        "сейчас играет · ${cur?.title ?: "тишина"}", 16,
                        Modifier.align(Alignment.BottomStart), FontWeight.SemiBold, Color.White, maxLines = 2
                    )
                }
            } else {
                Box(Modifier.fillMaxSize()) {
                    Cover(cur, Modifier.fillMaxSize(), TileBlue)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .background(Color(0x99000000))
                            .padding(12.dp)
                    ) {
                        MText(cur.artist, 16, color = Color.White, weight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(app: MetroApp, push: (Screen) -> Unit) {
    val c = Metro.colors
    val repo = app.repo
    val store = app.store
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }

    val radio = {
        val all = repo.tracks
        if (all.isNotEmpty()) {
            app.player.playTracks(all.shuffled(), 0)
            push(Screen.NowPlaying)
        }
    }

    val items = listOf(
        MenuEntry("перемешать всё") { radio() },
        MenuEntry("обновить библиотеку") { scope.launch { repo.load() } },
        MenuEntry("настройки") { push(Screen.Settings) },
        MenuEntry("о приложении") { push(Screen.About) }
    )

    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val tile = (maxWidth - 48.dp - 6.dp) / 2
            val wide = tile * 2 + 6.dp
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                MText("МОЯ КОЛЛЕКЦИЯ", 12, Modifier.padding(top = 24.dp), FontWeight.SemiBold, c.subtle, spacing = 2f)
                MText("музыка", 54, Modifier.padding(top = 4.dp, bottom = 20.dp), lh = 1.1f)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LiveTile(app, wide, tile) { push(Screen.NowPlaying) }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CountTile(repo.artists.size, "исполнители", Color(0xFFC8101F), tile) { push(Screen.Artists) }
                        CountTile(repo.albums.size, "альбомы", Color(0xFF3B8A00), tile) { push(Screen.Albums) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CountTile(store.playlists.size, "плейлисты", Color(0xFFC74B00), tile) { push(Screen.Playlists) }
                        CountTile(store.favorites.size, "избранное", Color(0xFF8A2BE2), tile) { push(Screen.Favorites) }
                    }
                    Tile(Color(0xFFD80073), wide, tile, "радио", { radio() }) {
                        MetroIcon(Ic.RADIO, 44.dp, Color.White)
                    }
                    Tile(Color(0xFF00ABA9), wide, tile, "все треки · ${repo.tracks.size}", { push(Screen.AllTracks) }) {
                        MetroIcon(Ic.NOTE, 44.dp, Color.White)
                    }
                }
                Spacer(Modifier.height(96.dp))
            }
        }
        Ellipsis(
            c.subtle, { menu = true },
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 8.dp)
        )
        MetroMenu(menu, items) { menu = false }
    }
}

/* ═══════════════════ Списки ═══════════════════ */

@Composable
private fun ListFrame(
    app: MetroApp,
    caption: String,
    title: String,
    push: (Screen) -> Unit,
    menuItems: List<MenuEntry>,
    leftExtra: (@Composable RowScope.() -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit
) {
    val c = Metro.colors
    val st = app.player.state
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 12.dp)) {
                MText(caption, 12, color = c.subtle, weight = FontWeight.SemiBold, spacing = 2f)
                MText(title, 46, Modifier.padding(top = 4.dp), lh = 1.1f, maxLines = 1)
            }
            Column(Modifier.weight(1f).fillMaxWidth()) { body() }
            MetroBottomBar(onMore = { menu = true }, onMid = { push(Screen.NowPlaying) }) {
                if (leftExtra != null) leftExtra(this)
                if (st.current != null) PlayerButtons(app)
            }
        }
        MetroMenu(menu, menuItems) { menu = false }
    }
}

@Composable
fun ArtistsScreen(app: MetroApp, push: (Screen) -> Unit) {
    val artists = app.repo.artists
    ListFrame(app, "МУЗЫКА", "исполнители", push, listOf(MenuEntry("настройки") { push(Screen.Settings) })) {
        if (artists.isEmpty()) {
            EmptyHint("музыка не найдена")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(artists) { _, a ->
                    TrackRow(a.name, plural(a.count, "трек", "трека", "треков"), false, { push(Screen.ArtistTracks(a.name)) })
                }
            }
        }
    }
}

@Composable
fun AlbumsScreen(app: MetroApp, push: (Screen) -> Unit) {
    val albums = app.repo.albums
    ListFrame(app, "МУЗЫКА", "альбомы", push, listOf(MenuEntry("настройки") { push(Screen.Settings) })) {
        if (albums.isEmpty()) {
            EmptyHint("музыка не найдена")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(albums) { _, a ->
                    TrackRow(
                        a.name,
                        "${a.artist} · ${plural(a.count, "трек", "трека", "треков")}",
                        false,
                        { push(Screen.AlbumTracks(a.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistsScreen(app: MetroApp, push: (Screen) -> Unit, onNewPlaylist: () -> Unit) {
    val playlists = app.store.playlists
    ListFrame(
        app, "МУЗЫКА", "плейлисты", push,
        listOf(MenuEntry("новый плейлист") { onNewPlaylist() }, MenuEntry("настройки") { push(Screen.Settings) }),
        leftExtra = { MetroCircleButton(44.dp, Ic.PLUS, 24.dp, Color.White) { onNewPlaylist() } }
    ) {
        if (playlists.isEmpty()) {
            EmptyHint("нажмите «+», чтобы создать плейлист")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(playlists) { _, p ->
                    TrackRow(p.name, plural(p.trackIds.size, "трек", "трека", "треков"), false, { push(Screen.PlaylistTracks(p.id)) })
                }
            }
        }
    }
}

/** Лента форматов: «все · .flac · .mp3 …». Выбранный подсвечен акцентом. */
@Composable
private fun FormatChips(
    formats: List<Pair<String, Int>>,
    total: Int,
    active: String?,
    onPick: (String?) -> Unit
) {
    val c = Metro.colors
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        @Composable
        fun Chip(label: String, on: Boolean, pick: () -> Unit) {
            MText(
                label, 20,
                Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { pick() },
                color = if (on) c.accent else c.inactive,
                weight = if (on) FontWeight.SemiBold else FontWeight.Light,
                maxLines = 1, softWrap = false
            )
        }
        Chip("все · $total", active == null) { onPick(null) }
        formats.forEach { (f, n) -> Chip(".$f · $n", active == f) { onPick(f) } }
    }
}

@Composable
fun TrackListScreen(
    app: MetroApp,
    caption: String,
    title: String,
    allTracks: List<Track>,
    push: (Screen) -> Unit,
    pop: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    playlistId: Long? = null
) {
    val st = app.player.state
    val store = app.store
    var selected by remember { mutableStateOf<Track?>(null) }
    var trackMenu by remember { mutableStateOf(false) }

    // Фильтр по формату файла: null — показывать все
    var formatFilter by remember { mutableStateOf<String?>(null) }
    val formatCounts = remember(allTracks) {
        allTracks.filter { it.format.isNotEmpty() }
            .groupingBy { it.format }.eachCount()
            .entries.sortedByDescending { it.value }
    }
    val activeFormat = formatFilter?.takeIf { f -> formatCounts.any { it.key == f } }
    val tracks = if (activeFormat == null) allTracks else allTracks.filter { it.format == activeFormat }

    val menuItems = buildList {
        if (tracks.isNotEmpty()) {
            add(MenuEntry("играть всё") {
                app.player.playTracks(tracks, 0)
                push(Screen.NowPlaying)
            })
            add(MenuEntry("перемешать") {
                app.player.playTracks(tracks.shuffled(), 0)
                push(Screen.NowPlaying)
            })
        }
        if (playlistId != null) {
            add(MenuEntry("удалить плейлист") {
                store.deletePlaylist(playlistId)
                pop()
            })
        }
        add(MenuEntry("настройки") { push(Screen.Settings) })
    }

    val sel = selected
    val trackItems = buildList {
        if (sel != null) {
            add(MenuEntry(if (store.isFavorite(sel.id)) "убрать из избранного" else "в избранное") {
                store.toggleFavorite(sel.id)
            })
            add(MenuEntry("играть следующим") { app.player.playNext(sel) })
            add(MenuEntry("в плейлист…") { onAddToPlaylist(sel.id) })
            if (playlistId != null) {
                add(MenuEntry("убрать из плейлиста") { store.removeFromPlaylist(playlistId, sel.id) })
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        ListFrame(app, caption, title, push, menuItems) {
            if (formatCounts.size > 1) {
                FormatChips(formatCounts.map { it.key to it.value }, allTracks.size, activeFormat) { formatFilter = it }
            }
            if (tracks.isEmpty()) {
                EmptyHint("здесь пока пусто")
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(tracks) { i, t ->
                        TrackRow(
                            title = t.title,
                            subtitle = "${t.artist} · ${fmt(t.durationMs)}",
                            highlighted = st.current?.id == t.id,
                            onClick = {
                                app.player.playTracks(tracks, i)
                                push(Screen.NowPlaying)
                            },
                            onLongClick = {
                                selected = t
                                trackMenu = true
                            }
                        )
                    }
                }
            }
        }
        MetroMenu(trackMenu, trackItems) { trackMenu = false }
    }
}

/* ═══════════════════ Настройки ═══════════════════ */

@Composable
fun SettingsScreen(app: MetroApp, push: (Screen) -> Unit) {
    val c = Metro.colors
    val store = app.store
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        MText("МУЗЫКА", 12, Modifier.padding(top = 24.dp), FontWeight.SemiBold, c.subtle, spacing = 2f)
        MText("настройки", 46, Modifier.padding(top = 4.dp, bottom = 16.dp), lh = 1.1f)

        MText("тема", 14, color = c.subtle)
        Row(Modifier.padding(top = 6.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf("тёмная" to 1, "светлая" to 2, "системная" to 0).forEach { (label, mode) ->
                MText(
                    label, 22,
                    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        store.setThemeMode(mode)
                    },
                    color = if (store.themeMode == mode) c.accent else c.inactive
                )
            }
        }

        MText("текст песен", 14, color = c.subtle)
        Row(Modifier.padding(top = 6.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf("синхронно" to 0, "выделение" to 1).forEach { (label, mode) ->
                MText(
                    label, 22,
                    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        store.chooseLyricsMode(mode)
                    },
                    color = if (store.lyricsMode == mode) c.accent else c.inactive
                )
            }
        }

        MText("цвет акцента", 14, color = c.subtle)
        MText(
            "изменить в «о приложении»", 22,
            Modifier
                .padding(top = 6.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { push(Screen.About) },
            color = c.accent
        )
        Spacer(Modifier.height(32.dp))
    }
}

/* ═══════════════════ Разрешение ═══════════════════ */

@Composable
fun PermissionScreen(onRequest: () -> Unit) {
    val c = Metro.colors
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        MText("МУЗЫКА", 12, color = c.subtle, weight = FontWeight.SemiBold, spacing = 2f)
        MText("нужен доступ", 46, Modifier.padding(top = 4.dp), lh = 1.1f)
        MText(
            "Разрешите доступ к аудиофайлам, чтобы плеер увидел вашу музыку. " +
                "Если окно не появляется — включите разрешение в настройках приложения.",
            18, Modifier.padding(top = 12.dp), color = c.subtle
        )
        MText(
            "разрешить", 20,
            Modifier
                .padding(top = 28.dp)
                .metroPress(onClick = onRequest)
                .border(2.dp, c.fg)
                .padding(horizontal = 28.dp, vertical = 12.dp)
        )
    }
}

/* ═══════════════════ Диалоги ═══════════════════ */

@Composable
private fun MetroDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(BarColor)
                .padding(20.dp)
        ) {
            MText(title, 22, color = Color.White)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun NewPlaylistDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    MetroDialog("новый плейлист", onDismiss) {
        BasicTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = MetroFont,
                fontWeight = FontWeight.Light
            ),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .border(2.dp, Color(0xFF8A8A8A))
                .padding(12.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.End)
        ) {
            MText(
                "отмена", 18,
                Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
                color = Color.White
            )
            MText(
                "создать", 18,
                Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCreate(name) },
                color = Color.White, weight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AddToPlaylistDialog(app: MetroApp, trackId: Long, onNew: () -> Unit, onDismiss: () -> Unit) {
    val playlists = app.store.playlists
    MetroDialog("в плейлист", onDismiss) {
        playlists.forEach { p ->
            MText(
                p.name, 22,
                Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        app.store.addToPlaylist(p.id, trackId)
                        onDismiss()
                    }
                    .padding(vertical = 10.dp),
                color = Color.White, maxLines = 1
            )
        }
        MText(
            "новый плейлист…", 22,
            Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onNew() }
                .padding(vertical = 10.dp),
            color = Color(0xFFBBBBBB)
        )
    }
}
