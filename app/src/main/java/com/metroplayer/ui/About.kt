package com.metroplayer.ui

import android.os.Build
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.metroplayer.MetroApp
import com.metroplayer.R
import kotlinx.coroutines.delay

private val AccentPalette = listOf(
    0xFFA4C400, 0xFF60A917, 0xFF008A00, 0xFF00ABA9, 0xFF1BA1E2,
    0xFF0050EF, 0xFF6A00FF, 0xFFAA00FF, 0xFFF472D0, 0xFFD80073,
    0xFFA20025, 0xFFE51400, 0xFFFA6800, 0xFFF0A30A, 0xFFE3C800,
    0xFF825A2C, 0xFF6D8764, 0xFF647687, 0xFF76608A, 0xFF87794E
).map { it.toInt() }

/** «Живая» плитка: грани сменяются каждые [periodMs] мс (с задержкой старта, чтобы плитки не мигали разом). */
@Composable
private fun FlipTile(
    w: Dp,
    h: Dp,
    color: Color,
    periodMs: Long,
    startDelayMs: Long,
    faces: List<@Composable BoxScope.() -> Unit>
) {
    var face by remember { mutableIntStateOf(0) }
    val count = faces.size
    LaunchedEffect(count) {
        delay(startDelayMs)
        while (true) {
            delay(periodMs)
            face = (face + 1) % count
        }
    }
    Box(
        Modifier
            .size(w, h)
            .metroPress(tilt = true, onClick = { face = (face + 1) % count })
            .skinBg(color)
            .clipToBounds()
    ) {
        AnimatedContent(
            targetState = face,
            transitionSpec = { slideInVertically { it } togetherWith slideOutVertically { -it } },
            label = "aboutTile"
        ) { f ->
            Box(Modifier.fillMaxSize().padding(12.dp)) { faces[f % count](this) }
        }
    }
}

@Composable
private fun BoxScope.Big(text: String) =
    MText(text, 44, Modifier.align(Alignment.TopStart), color = Color.White, lh = 1f, maxLines = 1)

@Composable
private fun BoxScope.Label(text: String) =
    MText(text, 16, Modifier.align(Alignment.BottomStart), FontWeight.SemiBold, Color.White, maxLines = 2)

@Composable
private fun CreditPerson(name: String, role: String) {
    val c = Metro.colors
    MText(name, 24, Modifier.padding(top = 14.dp), color = c.fg, maxLines = 1)
    MText(role, 14, Modifier.padding(top = 2.dp), color = c.subtle)
}

@Composable
private fun CreditRow(label: String, value: String) {
    val c = Metro.colors
    Column(Modifier.padding(top = 12.dp)) {
        MText(label, 14, color = c.subtle)
        MText(value, 20, Modifier.padding(top = 1.dp), color = c.fg)
    }
}

@Composable
fun AboutScreen(app: MetroApp, push: (Screen) -> Unit) {
    val c = Metro.colors
    val store = app.store
    val repo = app.repo
    val ctx = LocalContext.current

    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
    val loader = remember {
        ImageLoader.Builder(ctx)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
            }
            .build()
    }

    val formats = remember(repo.tracks) {
        repo.tracks.filter { it.format.isNotEmpty() }
            .groupingBy { it.format }.eachCount()
            .entries.sortedByDescending { it.value }
    }
    val accent = c.accent
    val accentDark = lerp(accent, Color.Black, 0.25f)
    val accentDeep = lerp(accent, Color.Black, 0.45f)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        MText("МУЗЫКА", 12, Modifier.padding(top = 24.dp), FontWeight.SemiBold, c.subtle, spacing = 2f)
        MText("о приложении", 46, Modifier.padding(top = 4.dp, bottom = 20.dp), lh = 1.1f, maxLines = 1)

        // ── живые плитки ──
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val tile = (maxWidth - 6.dp) / 2
            val wide = tile * 2 + 6.dp
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FlipTile(
                    wide, tile, accent, 4500, 0,
                    listOf(
                        { Big("Metro"); Label("Metro Player") },
                        { Big("v$version"); Label("версия приложения") },
                        { Big("corei5"); Label("автор") }
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlipTile(
                        tile, tile, accentDark, 5000, 1200,
                        listOf(
                            { Big("${repo.tracks.size}"); Label("треков в библиотеке") },
                            { Big("${repo.artists.size}"); Label("исполнителей") },
                            { Big("${repo.albums.size}"); Label("альбомов") }
                        )
                    )
                    FlipTile(
                        tile, tile, accentDeep, 5500, 2400,
                        if (formats.isEmpty()) {
                            listOf<@Composable BoxScope.() -> Unit>({ Big("—"); Label("форматов нет") })
                        } else {
                            formats.take(4).map { (f, n) ->
                                val face: @Composable BoxScope.() -> Unit = { Big(".$f"); Label("$n шт.") }
                                face
                            }
                        }
                    )
                }
            }
        }

        // ── акцент ──
        MText("цвет акцента", 14, Modifier.padding(top = 28.dp), color = c.subtle)
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AccentPalette.chunked(5).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { argb ->
                        val selected = store.accent == argb
                        Box(
                            Modifier
                                .size(54.dp)
                                .background(Color(argb))
                                .then(if (selected) Modifier.border(3.dp, c.fg) else Modifier)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    store.setAccent(argb)
                                }
                        )
                    }
                }
            }
        }
        MText(
            "по умолчанию для темы", 18,
            Modifier
                .padding(top = 16.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { store.setAccent(0) },
            color = if (store.accent == 0) c.accent else c.inactive
        )

        // ── гифка (над подписью автора) ──
        Spacer(Modifier.height(28.dp))
        AsyncImage(
            model = R.raw.author,
            imageLoader = loader,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(0.7f).align(Alignment.CenterHorizontally)
        )

        // ── кредиты ──
        MText("КРЕДИТЫ", 12, Modifier.padding(top = 28.dp), FontWeight.SemiBold, c.subtle, spacing = 2f)

        MText("разработчики", 34, Modifier.padding(top = 4.dp), color = c.accent)
        CreditPerson("corei5", "идея, дизайн и разработка")

        MText("тестер", 34, Modifier.padding(top = 28.dp), color = c.accent)
        CreditPerson("meoww", "тестирование · aka vanilla")

        MText("разработка", 34, Modifier.padding(top = 28.dp), color = c.accent)
        CreditRow("язык", "Kotlin 2.0")
        CreditRow("интерфейс", "Jetpack Compose")
        CreditRow("стиль", "Metro / Windows Phone")
        CreditRow("плеер", "AndroidX Media3 (ExoPlayer)")
        CreditRow("картинки и gif", "Coil")
        CreditRow("тексты песен", "lrclib.net")
        CreditRow("шрифт", "Segoe UI")
        CreditRow("платформа", "Android 8.0+ (API 26)")
        CreditRow("сборка", "Gradle · GitHub Actions")

        MText("Metro Player v$version", 14, Modifier.padding(top = 28.dp), color = c.subtle)
        MText("сделано с любовью к музыке", 14, Modifier.padding(top = 2.dp), color = c.inactive)

        Spacer(Modifier.height(32.dp))
    }
}
