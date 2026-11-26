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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metroplayer.MetroApp
import com.metroplayer.data.EqPresets
import com.metroplayer.data.Stats
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
    data object Search : Screen
    data object Recent : Screen
    data object Stats : Screen
    data object DailyMix : Screen
    data class ArtistTracks(val name: String) : Screen
    data class AlbumTracks(val albumId: Long) : Screen
    data class PlaylistTracks(val id: Long) : Screen
}

sealed interface DialogKind {
    data class AddToPlaylist(val trackId: Long) : DialogKind
    data class NewPlaylist(val thenAdd: Long?) : DialogKind
}

/* ═══════════════════ 2 · Живые плитки ═══════════════════ */

@Composable
private fun Tile(
    color: Color,
    w: Dp,
    h: Dp,
    label: String,
    onClick: () -> Unit,
    enter: Int = -1,
    top: @Composable () -> Unit
) {
    Box(
        Modifier
            .size(w, h)
            .metroEnter(enter, stepMs = 60)
            .metroPress(tilt = true, onClick = onClick)
            .skinBg(color)
            .padding(12.dp)
    ) {
        Box(Modifier.align(Alignment.TopStart)) { top() }
        MText(label, 16, Modifier.align(Alignment.BottomStart), FontWeight.SemiBold, Color.White, maxLines = 2)
    }
}

@Composable
private fun CountTile(count: Int, label: String, color: Color, size: Dp, enter: Int, onClick: () -> Unit) {
    Tile(color, size, size, label, onClick, enter) {
        MText("$count", 44, color = Color.White, lh = 1f)
    }
}

/** «Живая» плитка: каждые 5 секунд переворачивается между названием и обложкой. */
@Composable
private fun LiveTile(app: MetroApp, w: Dp, h: Dp, enter: Int, onClick: () -> Unit) {
    val st = app.player.state
    val cur = st.current
    val tileColor = Metro.colors.accent
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
            .metroEnter(enter, stepMs = 60)
            .metroPress(tilt = true, onClick = onClick)
            .skinBg(tileColor)
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
                    Cover(cur, Modifier.fillMaxSize(), tileColor)
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

    // Все плитки — цвета акцента (как в Windows Phone), оттенки слегка различаются
    val accent = c.accent
    val shade1 = lerp(accent, Color.Black, 0.18f)
    val shade2 = lerp(accent, Color.Black, 0.32f)

    val items = listOf(
        MenuEntry("поиск") { push(Screen.Search) },
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
                MText("музыка", 54, Modifier.padding(top = 4.dp, bottom = 12.dp), lh = 1.1f)
                if (!repo.loaded) {
                    MetroProgressDots(Modifier.padding(bottom = 12.dp))
                } else if (repo.tracks.isEmpty()) {
                    MText(
                        "На устройстве не найдено музыки. Добавьте файлы и выберите «обновить библиотеку» в меню «···».",
                        16, Modifier.padding(bottom = 12.dp), color = c.subtle
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LiveTile(app, wide, tile, 0) { push(Screen.NowPlaying) }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CountTile(repo.artists.size, "исполнители", shade1, tile, 1) { push(Screen.Artists) }
                        CountTile(repo.albums.size, "альбомы", shade2, tile, 2) { push(Screen.Albums) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CountTile(store.playlists.size, "плейлисты", shade2, tile, 3) { push(Screen.Playlists) }
                        CountTile(store.favorites.size, "избранное", shade1, tile, 4) { push(Screen.Favorites) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CountTile(recentTracks(app).size, "недавние", shade1, tile, 5) { push(Screen.Recent) }
                        Tile(shade2, tile, tile, "радио", { radio() }, 6) {
                            MetroIcon(Ic.RADIO, 44.dp, Color.White)
                        }
                    }
                    val weekPlays = remember(app.playLog.version) {
                        val since = System.currentTimeMillis() - 7 * 86_400_000L
                        app.playLog.plays.count { it.time >= since }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tile(shade2, tile, tile, "daily mix", { push(Screen.DailyMix) }, 7) {
                            MText("MIX", 36, color = Color.White, lh = 1f)
                        }
                        Tile(shade1, tile, tile, "статистика · 7 дн.", { push(Screen.Stats) }, 8) {
                            MText("$weekPlays", 44, color = Color.White, lh = 1f)
                        }
                    }
                    Tile(shade1, wide, tile, "все треки · ${repo.tracks.size}", { push(Screen.AllTracks) }, 9) {
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
    subtitle: String? = null,
    body: @Composable ColumnScope.() -> Unit
) {
    val c = Metro.colors
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 12.dp)) {
                MText(caption, 12, color = c.subtle, weight = FontWeight.SemiBold, spacing = 2f)
                MText(title, 46, Modifier.padding(top = 4.dp), lh = 1.1f, maxLines = 1)
                if (subtitle != null) MText(subtitle, 14, Modifier.padding(top = 2.dp), color = c.subtle, maxLines = 1)
            }
            Column(Modifier.weight(1f).fillMaxWidth()) { body() }
            MiniPlayerBar(app, onOpen = { push(Screen.NowPlaying) }, onMore = { menu = true }, leftExtra = leftExtra)
        }
        MetroMenu(menu, menuItems) { menu = false }
    }
}

private fun fmtTotal(ms: Long): String {
    val min = ms / 60_000
    return if (min >= 60) "${min / 60} ч ${min % 60} мин" else "$min мин"
}

@Composable
fun ArtistsScreen(app: MetroApp, push: (Screen) -> Unit) {
    val artists = app.repo.artists
    ListFrame(app, "МУЗЫКА", "исполнители", push, listOf(MenuEntry("настройки") { push(Screen.Settings) })) {
        if (artists.isEmpty()) {
            EmptyHint("музыка не найдена")
        } else {
            JumpLazyList(artists, { it.name }) { i, a ->
                TrackRow(a.name, plural(a.count, "трек", "трека", "треков"), false, { push(Screen.ArtistTracks(a.name)) }, enterIndex = i)
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
            JumpLazyList(albums, { it.name }) { i, a ->
                TrackRow(
                    a.name,
                    "${a.artist} · ${plural(a.count, "трек", "трека", "треков")}",
                    false,
                    { push(Screen.AlbumTracks(a.id)) },
                    enterIndex = i
                )
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
        leftExtra = { MetroCircleButton(42.dp, Ic.PLUS, 22.dp, Color.White, "новый плейлист") { onNewPlaylist() } }
    ) {
        if (playlists.isEmpty()) {
            EmptyHint("нажмите «+», чтобы создать плейлист")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(playlists) { i, p ->
                    TrackRow(p.name, plural(p.trackIds.size, "трек", "трека", "треков"), false, { push(Screen.PlaylistTracks(p.id)) }, enterIndex = i)
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onChange: (String) -> Unit, onClose: () -> Unit) {
    val c = Metro.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).skinBorder(2.dp, c.fg).padding(10.dp)) {
            if (query.isEmpty()) MText("поиск", 20, color = c.inactive)
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = c.fg, fontSize = 20.sp, fontFamily = MetroFont, fontWeight = FontWeight.Light),
                cursorBrush = SolidColor(c.fg),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        }
        MText(
            "×", 32,
            Modifier
                .padding(start = 14.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
            color = c.fg
        )
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
    playlistId: Long? = null,
    alphabetical: Boolean = false,
    extraMenu: List<MenuEntry> = emptyList()
) {
    val st = app.player.state
    val store = app.store
    var selected by remember { mutableStateOf<Track?>(null) }
    var trackMenu by remember { mutableStateOf(false) }

    // Фильтр по формату файла: null — показывать все
    // начальное значение берётся из настроек; если такого формата в списке нет, показываются все треки
    var formatFilter by remember { mutableStateOf<String?>(store.defaultFormat.takeIf { it.isNotEmpty() }) }
    val formatCounts = remember(allTracks) {
        allTracks.filter { it.format.isNotEmpty() }
            .groupingBy { it.format }.eachCount()
            .entries.sortedByDescending { it.value }
    }
    val activeFormat = formatFilter?.takeIf { f -> formatCounts.any { it.key == f } }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val byFormat = if (activeFormat == null) allTracks else allTracks.filter { it.format == activeFormat }
    val q = query.trim().lowercase()
    val tracks = if (!searching || q.isEmpty()) {
        byFormat
    } else {
        byFormat.filter {
            it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
        }
    }

    val menuItems = buildList {
        add(MenuEntry(if (searching) "закрыть поиск" else "поиск") {
            searching = !searching
            if (!searching) query = ""
        })
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
        addAll(extraMenu)
        add(MenuEntry("настройки") { push(Screen.Settings) })
    }

    val sel = selected
    val trackItems = buildList {
        if (sel != null) {
            add(MenuEntry(if (store.isFavorite(sel.id)) "убрать из избранного" else "в избранное") {
                store.toggleFavorite(sel.id)
            })
            add(MenuEntry("играть следующим") { app.player.playNext(sel) })
            add(MenuEntry("в конец очереди") { app.player.addToQueue(sel) })
            add(MenuEntry("в плейлист…") { onAddToPlaylist(sel.id) })
            add(MenuEntry("к исполнителю") { push(Screen.ArtistTracks(sel.artist)) })
            add(MenuEntry("к альбому") { push(Screen.AlbumTracks(sel.albumId)) })
            if (playlistId != null) {
                add(MenuEntry("убрать из плейлиста") { store.removeFromPlaylist(playlistId, sel.id) })
            }
        }
    }

    val subtitle = if (allTracks.isEmpty()) {
        null
    } else {
        "${plural(allTracks.size, "трек", "трека", "треков")} · ${fmtTotal(allTracks.sumOf { it.durationMs })}"
    }

    val rowFor: @Composable (Int, Track) -> Unit = { i, t ->
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
            },
            onMore = {
                selected = t
                trackMenu = true
            },
            enterIndex = i
        )
    }

    Box(Modifier.fillMaxSize()) {
        ListFrame(app, caption, title, push, menuItems, subtitle = subtitle) {
            if (searching) {
                SearchBar(query, { query = it }) {
                    searching = false
                    query = ""
                }
            }
            if (tracks.isNotEmpty() && !searching) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetroTextButton("играть", Ic.PLAY, Modifier.weight(1f)) {
                        app.player.playTracks(tracks, 0)
                        push(Screen.NowPlaying)
                    }
                    MetroTextButton("перемешать", Ic.SHUFFLE, Modifier.weight(1f)) {
                        app.player.playTracks(tracks.shuffled(), 0)
                        push(Screen.NowPlaying)
                    }
                }
            }
            if (formatCounts.size > 1) {
                FormatChips(formatCounts.map { it.key to it.value }, allTracks.size, activeFormat) { formatFilter = it }
            }
            if (tracks.isEmpty()) {
                EmptyHint(if (searching && q.isNotEmpty()) "ничего не найдено" else "здесь пока пусто")
            } else if (alphabetical) {
                JumpLazyList(tracks, { it.title }, row = rowFor)
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(tracks) { i, t -> rowFor(i, t) }
                }
            }
        }
        MetroMenu(trackMenu, trackItems, header = sel?.title) { trackMenu = false }
    }
}

/* ═══════════════════ Глобальный поиск ═══════════════════ */

@Composable
fun SearchScreen(app: MetroApp, push: (Screen) -> Unit) {
    val c = Metro.colors
    val repo = app.repo
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val artists = remember(q, repo.artists) {
        if (q.isEmpty()) emptyList() else repo.artists.filter { it.name.lowercase().contains(q) }.take(5)
    }
    val albums = remember(q, repo.albums) {
        if (q.isEmpty()) {
            emptyList()
        } else {
            repo.albums.filter { it.name.lowercase().contains(q) || it.artist.lowercase().contains(q) }.take(5)
        }
    }
    val tracks = remember(q, repo.tracks) {
        if (q.isEmpty()) {
            emptyList()
        } else {
            repo.tracks
                .filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q) }
                .sortedByDescending { it.title.lowercase().startsWith(q) }
                .take(60)
        }
    }
    val st = app.player.state

    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        Column(Modifier.padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 12.dp)) {
            MText("МУЗЫКА", 12, color = c.subtle, weight = FontWeight.SemiBold, spacing = 2f)
            MText("поиск", 46, Modifier.padding(top = 4.dp), lh = 1.1f, maxLines = 1)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f).skinBorder(2.dp, c.fg).padding(12.dp)) {
                if (query.isEmpty()) MText("трек, исполнитель, альбом", 20, color = c.inactive, maxLines = 1)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = c.fg, fontSize = 20.sp, fontFamily = MetroFont, fontWeight = FontWeight.Light),
                    cursorBrush = SolidColor(c.fg),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
            }
            if (query.isNotEmpty()) {
                MText(
                    "×", 32,
                    Modifier
                        .padding(start = 14.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { query = "" },
                    color = c.fg
                )
            }
        }
        if (q.isEmpty()) {
            EmptyHint("Введите название трека, исполнителя или альбома.")
        } else if (artists.isEmpty() && albums.isEmpty() && tracks.isEmpty()) {
            EmptyHint("ничего не найдено")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                if (artists.isNotEmpty()) {
                    item { SectionLabel("исполнители") }
                    items(artists) { a ->
                        TrackRow(a.name, plural(a.count, "трек", "трека", "треков"), false, { push(Screen.ArtistTracks(a.name)) })
                    }
                }
                if (albums.isNotEmpty()) {
                    item { SectionLabel("альбомы") }
                    items(albums) { a ->
                        TrackRow(a.name, "${a.artist} · ${plural(a.count, "трек", "трека", "треков")}", false, { push(Screen.AlbumTracks(a.id)) })
                    }
                }
                if (tracks.isNotEmpty()) {
                    item { SectionLabel("треки") }
                    itemsIndexed(tracks) { i, t ->
                        TrackRow(
                            title = t.title,
                            subtitle = "${t.artist} · ${fmt(t.durationMs)}",
                            highlighted = st.current?.id == t.id,
                            onClick = {
                                app.player.playTracks(tracks, i)
                                push(Screen.NowPlaying)
                            }
                        )
                    }
                }
            }
        }
    }
}

/* ═══════════════════ Настройки ═══════════════════ */

@Composable
fun SettingsScreen(app: MetroApp, push: (Screen) -> Unit) {
    val c = Metro.colors
    val store = app.store
    val gifScope = rememberCoroutineScope()
    var gifMsg by remember { mutableStateOf<String?>(null) }
    val gifPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            gifScope.launch {
                gifMsg = if (store.importNoteGif(uri)) null else "не удалось загрузить файл (нужна картинка или гифка до 12 МБ)"
            }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        MText("МУЗЫКА", 12, Modifier.padding(top = 24.dp), FontWeight.SemiBold, c.subtle, spacing = 2f)
        MText("настройки", 46, Modifier.padding(top = 4.dp, bottom = 16.dp), lh = 1.1f)

        MText("ВНЕШНИЙ ВИД", 12, Modifier.padding(bottom = 8.dp), FontWeight.SemiBold, c.accent, spacing = 2f)
        OptionGroup(
            "стиль",
            listOf("Metro" to 0, "Windows 7" to 1, "Aero" to 2, "Material You" to 3),
            store.skin,
            Modifier.padding(bottom = 20.dp)
        ) { id ->
            store.chooseSkin(id)
            com.metroplayer.SkinIcons.apply(app, id)
        }
        OptionGroup(
            "тема",
            listOf("тёмная" to 1, "светлая" to 2, "как в системе" to 0),
            store.themeMode,
            Modifier.padding(bottom = 20.dp)
        ) { store.setThemeMode(it) }

        MText("цвет акцента", 14, color = c.subtle)
        Column(Modifier.padding(top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AccentPalette.chunked(6).forEach { rowColors ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowColors.forEach { argb ->
                        val on = store.accent == argb
                        Box(
                            Modifier
                                .size(44.dp)
                                .metroPress(pressScale = 0.9f) { store.setAccent(argb) }
                                .background(Color(argb))
                                .then(if (on) Modifier.border(3.dp, c.fg) else Modifier)
                        )
                    }
                }
            }
        }
        MText(
            if (store.accent == 0) "по умолчанию для темы" else "сбросить на цвет по умолчанию", 16,
            Modifier
                .padding(bottom = 24.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { store.setAccent(0) },
            color = if (store.accent == 0) c.subtle else c.accent
        )

        MText("ВОСПРОИЗВЕДЕНИЕ", 12, Modifier.padding(bottom = 8.dp), FontWeight.SemiBold, c.accent, spacing = 2f)
        OptionGroup(
            "экран «сейчас играет»",
            listOf("обложка и строка текста" to 0, "текст вместо обложки" to 1),
            store.nowMode,
            Modifier.padding(bottom = 20.dp)
        ) { store.chooseNowMode(it) }

        SettingToggle(
            "кнопки под обложкой", "Перемешать, повтор и избранное на экране «сейчас играет». Меню «···» остаётся.",
            store.quickBar
        ) { store.chooseQuickBar(!store.quickBar) }
        Spacer(Modifier.height(12.dp))

        val libFormats = remember(app.repo.tracks) {
            app.repo.tracks.filter { it.format.isNotEmpty() }
                .groupingBy { it.format }.eachCount()
                .entries.sortedByDescending { it.value }.map { it.key }
        }
        val missingFormat = store.defaultFormat.takeIf { it.isNotEmpty() && it !in libFormats }
        val formatOptions = buildList<Pair<String, String>> {
            add("все форматы" to "")
            libFormats.forEach { add(".$it" to it) }
            if (missingFormat != null) add(".$missingFormat (нет в библиотеке)" to missingFormat)
        }
        OptionGroup(
            "формат по умолчанию в списках",
            formatOptions,
            store.defaultFormat,
            Modifier.padding(bottom = 4.dp)
        ) { store.chooseDefaultFormat(it) }
        MText(
            "В любом списке сразу откроются треки только этого формата, если они там есть. Если нет — покажутся все.",
            14, Modifier.padding(bottom = 20.dp), color = c.subtle
        )

        MText("ЭКВАЛАЙЗЕР", 12, Modifier.padding(top = 4.dp, bottom = 8.dp), FontWeight.SemiBold, c.accent, spacing = 2f)
        SettingToggle(
            "включить эквалайзер",
            "Действует на всю музыку в приложении.",
            store.eqOn
        ) { store.chooseEqOn(!store.eqOn) }
        if (store.eqOn) {
            Row(
                Modifier.fillMaxWidth().height(200.dp).padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EqPresets.labels.forEachIndexed { b, label ->
                    EqBand(label, store.eqGains[b]) { store.setEqGain(b, it) }
                }
            }
            val presetOptions = buildList<Pair<String, String>> {
                EqPresets.all.forEach { add(it.name to it.id) }
                if (store.eqPreset == "custom") add("Своё" to "custom")
            }
            OptionGroup("пресет", presetOptions, store.eqPreset, Modifier.padding(top = 12.dp)) {
                if (it != "custom") store.chooseEqPreset(it)
            }
        }
        Spacer(Modifier.height(20.dp))

        MText("ТЕКСТЫ ПЕСЕН", 12, Modifier.padding(bottom = 8.dp), FontWeight.SemiBold, c.accent, spacing = 2f)
        OptionGroup(
            "режим текста",
            listOf("синхронно — автопрокрутка" to 0, "выделение — можно копировать" to 1),
            store.lyricsMode,
            Modifier.padding(bottom = 12.dp)
        ) { store.chooseLyricsMode(it) }
        OptionGroup(
            "анимация текста",
            listOf("новая — строки плывут, слова загораются" to 0, "старая — строки сменяются слайдом" to 1),
            store.lyricsAnim,
            Modifier.padding(bottom = 12.dp)
        ) { store.chooseLyricsAnim(it) }
        SettingToggle(
            "искать тексты автоматически", "lrclib.net — для текущего и следующего трека",
            store.autoLyrics
        ) { store.chooseAutoLyrics(!store.autoLyrics) }
        SettingToggle(
            "строка песни в мини-плеере",
            "Заменяет исполнителя в уведомлении и на экране блокировки. Применится со следующей очереди.",
            store.lyricsInNotif
        ) { store.chooseLyricsInNotif(!store.lyricsInNotif) }

        MText("СТАТИСТИКА", 12, Modifier.padding(top = 20.dp, bottom = 8.dp), FontWeight.SemiBold, c.accent, spacing = 2f)
        SettingToggle(
            "вести статистику прослушиваний",
            "Нужна для экрана статистики и Daily Mix. Данные хранятся только на этом телефоне.",
            store.statsOn
        ) { store.chooseStatsOn(!store.statsOn) }

        MText("ГИФКА ВМЕСТО НОТЫ", 12, Modifier.padding(top = 20.dp, bottom = 8.dp), FontWeight.SemiBold, c.accent, spacing = 2f)
        MText(
            "Показывается в тексте песни на проигрышах и вступлениях, когда в строке нет слов.",
            14, Modifier.padding(bottom = 12.dp), color = c.subtle
        )
        NoteGif(96.dp, Modifier.padding(bottom = 4.dp))
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetroTextButton("выбрать свою", null) { gifPicker.launch(arrayOf("image/*")) }
            if (store.noteGif.isNotEmpty()) {
                MetroTextButton("стандартная", null) {
                    store.resetNoteGif()
                    gifMsg = null
                }
            }
        }
        gifMsg?.let { MText(it, 14, Modifier.padding(top = 10.dp), color = c.accent) }

        MText(
            "о приложении", 22,
            Modifier
                .padding(top = 20.dp)
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
                .skinBorder(2.dp, c.fg)
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
                .skinBg(Metro.colors.barColor)
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
                .skinBorder(2.dp, Color(0xFF8A8A8A))
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

/** Одна вертикальная полоса эквалайзера: тап или перетаскивание задаёт усиление от −12 до +12 дБ. */
@Composable
private fun EqBand(label: String, gain: Float, onChange: (Float) -> Unit) {
    val c = Metro.colors
    val onChangeNow by rememberUpdatedState(onChange)
    val trackColor = c.inactive
    val zeroColor = c.subtle
    val thumbColor = c.accent
    Column(Modifier.width(58.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        val g = gain.roundToInt()
        MText(if (g > 0) "+$g" else "$g", 14, color = c.fg, maxLines = 1)
        Box(
            Modifier
                .weight(1f)
                .width(44.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        onChangeNow(eqGainAt(change.position.y, size.height.toFloat(), 8.dp.toPx()))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { o -> onChangeNow(eqGainAt(o.y, size.height.toFloat(), 8.dp.toPx())) }
                }
                .drawBehind {
                    val pad = 8.dp.toPx()
                    val cx = size.width / 2f
                    val span = size.height - 2f * pad
                    drawRect(trackColor, Offset(cx - 1.dp.toPx(), pad), Size(2.dp.toPx(), span))
                    drawRect(zeroColor, Offset(cx - 9.dp.toPx(), pad + span / 2f - 0.5.dp.toPx()), Size(18.dp.toPx(), 1.dp.toPx()))
                    val y = pad + span * ((EqPresets.MAX_DB - gain) / (2f * EqPresets.MAX_DB))
                    drawRect(thumbColor, Offset(cx - 11.dp.toPx(), y - 5.dp.toPx()), Size(22.dp.toPx(), 10.dp.toPx()))
                }
        )
        MText(label, 11, Modifier.padding(top = 4.dp), color = c.subtle, maxLines = 1)
    }
}

private fun eqGainAt(y: Float, height: Float, pad: Float): Float {
    val frac = ((y - pad) / (height - 2f * pad)).coerceIn(0f, 1f)
    return (EqPresets.MAX_DB - frac * 2f * EqPresets.MAX_DB).roundToInt().toFloat()
}

/* ═══════════════════ Недавние, Daily Mix, статистика ═══════════════════ */

/** Недавно прослушанные: сначала то, что играет сейчас, затем история (новые сверху). */
fun recentTracks(app: MetroApp): List<Track> {
    val ids = ArrayList<Long>()
    app.player.state.current?.let { ids.add(it.id) }
    app.store.history.forEach { if (it !in ids) ids.add(it) }
    return ids.mapNotNull { app.repo.find(it) }
}

@Composable
fun DailyMixScreen(app: MetroApp, push: (Screen) -> Unit, pop: () -> Unit, onAdd: (Long) -> Unit) {
    LaunchedEffect(app.repo.loaded, app.repo.tracks.size) { app.ensureDailyMix() }
    val tracks = app.store.mixIds.mapNotNull { app.repo.find(it) }
    TrackListScreen(
        app, "ДЛЯ ВАС", "daily mix", tracks, push, pop, onAdd,
        extraMenu = listOf(MenuEntry("собрать заново") { app.ensureDailyMix(force = true) })
    )
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .height(92.dp)
            .background(Metro.colors.accent)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        MText(value, 32, color = Color.White, lh = 1f, maxLines = 1)
        MText(label, 13, color = Color.White.copy(alpha = 0.85f), maxLines = 1)
    }
}

@Composable
fun StatsScreen(app: MetroApp, push: (Screen) -> Unit) {
    val c = Metro.colors
    val log = app.playLog
    val repo = app.repo
    val version = log.version
    var period by remember { mutableIntStateOf(1) }   // 0 — 7 дней, 1 — 30 дней, 2 — всё время
    var confirmClear by remember { mutableStateOf(false) }
    val pad = Modifier.padding(horizontal = 24.dp)

    val since = remember(period, version) {
        val now = System.currentTimeMillis()
        when (period) {
            0 -> now - 7 * 86_400_000L
            1 -> now - 30 * 86_400_000L
            else -> 0L
        }
    }
    val artistOf: (Long) -> String? = { repo.find(it)?.artist }
    val summary = remember(since, version, repo.tracks.size) { Stats.summary(log, since, artistOf) }
    val streak = remember(version) { Stats.streak(log) }
    val chartDays = if (period == 0) 7 else 30
    val daily = remember(version, chartDays) { Stats.dailyMs(log, chartDays) }
    val topTracks = remember(since, version, repo.tracks.size) {
        Stats.topTracks(log, since, 5).mapNotNull { (id, n) -> repo.find(id)?.let { it to n } }
    }
    val topArtists = remember(since, version, repo.tracks.size) { Stats.topArtists(log, since, 5, artistOf) }
    val recent = recentTracks(app).take(5)
    val empty = log.plays.isEmpty() && log.listens.isEmpty()

    ListFrame(
        app, "МУЗЫКА", "статистика", push,
        menuItems = listOf(MenuEntry("настройки") { push(Screen.Settings) })
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            OptionGroup(
                "период",
                listOf("7 дней" to 0, "30 дней" to 1, "всё время" to 2),
                period,
                pad.padding(bottom = 12.dp)
            ) { period = it }

            if (empty) {
                MText(
                    if (app.store.statsOn) {
                        "Статистика появится, когда вы послушаете несколько треков."
                    } else {
                        "Ведение статистики выключено. Включить: настройки → «статистика»."
                    },
                    16, pad.padding(top = 8.dp), color = c.subtle
                )
            } else {
                Row(pad, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatCard(fmtTotal(summary.listenMs), "время", Modifier.weight(1f))
                    StatCard("${summary.plays}", "прослушиваний", Modifier.weight(1f))
                }
                Row(pad.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatCard("${summary.tracks}", "разных треков", Modifier.weight(1f))
                    StatCard("${summary.artists}", "исполнителей", Modifier.weight(1f))
                }
                if (streak > 0) {
                    MText(
                        "серия: ${plural(streak, "день", "дня", "дней")} подряд",
                        16, pad.padding(top = 12.dp), color = c.fg
                    )
                }

                MText("ПО ДНЯМ", 12, pad.padding(top = 24.dp, bottom = 8.dp), FontWeight.SemiBold, c.accent, spacing = 2f)
                val maxMs = (daily.maxOrNull() ?: 0L).coerceAtLeast(1L)
                Row(
                    pad.fillMaxWidth().height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (chartDays > 10) 2.dp else 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    daily.forEach { ms ->
                        val frac = (ms.toFloat() / maxMs).coerceIn(0.03f, 1f)
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(frac)
                                .background(if (ms > 0L) c.accent else c.inactive)
                        )
                    }
                }
                Row(pad.padding(top = 4.dp)) {
                    MText("${chartDays} дн. назад", 12, color = c.subtle)
                    Spacer(Modifier.weight(1f))
                    MText("рекорд: ${fmtTotal(maxMs)}", 12, color = c.subtle)
                    Spacer(Modifier.weight(1f))
                    MText("сегодня", 12, color = c.subtle)
                }

                if (topTracks.isNotEmpty()) {
                    MText("ЧАЩЕ ВСЕГО", 12, pad.padding(top = 24.dp, bottom = 4.dp), FontWeight.SemiBold, c.accent, spacing = 2f)
                    val queue = topTracks.map { it.first }
                    topTracks.forEachIndexed { i, (t, n) ->
                        TrackRow(
                            title = t.title,
                            subtitle = "${t.artist} · ${plural(n, "раз", "раза", "раз")}",
                            highlighted = app.player.state.current?.id == t.id,
                            onClick = {
                                app.player.playTracks(queue, i)
                                push(Screen.NowPlaying)
                            }
                        )
                    }
                }

                if (topArtists.isNotEmpty()) {
                    MText("ИСПОЛНИТЕЛИ", 12, pad.padding(top = 24.dp, bottom = 4.dp), FontWeight.SemiBold, c.accent, spacing = 2f)
                    val top = topArtists.first().second.coerceAtLeast(1)
                    topArtists.forEach { (name, n) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .metroPress(onClick = { push(Screen.ArtistTracks(name)) })
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                MText(name, 18, maxLines = 1)
                                Box(
                                    Modifier
                                        .padding(top = 4.dp)
                                        .fillMaxWidth(n.toFloat() / top)
                                        .height(4.dp)
                                        .background(c.accent)
                                )
                            }
                            MText("$n", 16, Modifier.padding(start = 12.dp), color = c.subtle)
                        }
                    }
                }
            }

            if (recent.isNotEmpty()) {
                MText("НЕДАВНО ПРОСЛУШАНО", 12, pad.padding(top = 24.dp, bottom = 4.dp), FontWeight.SemiBold, c.accent, spacing = 2f)
                recent.forEachIndexed { i, t ->
                    TrackRow(
                        title = t.title,
                        subtitle = "${t.artist} · ${fmt(t.durationMs)}",
                        highlighted = app.player.state.current?.id == t.id,
                        onClick = {
                            app.player.playTracks(recent, i)
                            push(Screen.NowPlaying)
                        }
                    )
                }
                MetroTextButton("все недавние", null, pad.padding(top = 8.dp)) { push(Screen.Recent) }
            }

            if (!empty) {
                MetroTextButton(
                    if (confirmClear) "точно очистить? нажмите ещё раз" else "очистить статистику",
                    null, pad.padding(top = 20.dp)
                ) {
                    if (confirmClear) {
                        log.clear()
                        confirmClear = false
                    } else {
                        confirmClear = true
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
