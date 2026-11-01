package com.metroplayer.ui

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
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
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

/** Слои иконки приложения для каждого стиля: (фон — null, если однотонный; передний план). */
private fun iconLayers(skin: Int): Pair<Int?, Int> = when (skin) {
    1 -> R.drawable.ic_launcher_background_aero to R.drawable.ic_launcher_foreground
    2 -> R.drawable.ic_launcher_background_glass to R.drawable.ic_launcher_fg_glass
    3 -> null to R.drawable.ic_launcher_fg_material
    else -> null to R.drawable.ic_launcher_fg_metro
}

/** Иконка приложения выбранного стиля: Metro — плоский квадрат, Win7 — глянец, Aero — стекло, Material — «сквиркл». */
@Composable
private fun AppIconArt(skin: Int, size: Dp, accent: Color, modifier: Modifier = Modifier) {
    val (bgRes, fgRes) = iconLayers(skin)
    val shape = RoundedCornerShape(size * (if (skin == 0) 0f else if (skin == 1) 0.22f else if (skin == 2) 0.28f else 0.36f))
    val flat = if (skin == 3) lerp(accent, Color.Black, 0.25f) else accent
    val glossy = skin == 1 || skin == 2
    Box(
        modifier
            .size(size)
            .shadow(if (skin == 0) 0.dp else size / 10, shape, clip = false, spotColor = flat)
            .clip(shape)
            .then(if (glossy) Modifier.border(1.5.dp, Color.White.copy(alpha = if (skin == 2) 0.65f else 0.45f), shape) else Modifier)
    ) {
        // слои рисуем крупнее на 1.5×: так же, как лаунчер показывает центральную часть адаптивной иконки
        if (bgRes != null) {
            Image(painterResource(bgRes), null, Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f })
        } else {
            Box(Modifier.fillMaxSize().background(flat))
        }
        Image(painterResource(fgRes), null, Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f })
        if (glossy) {
            // стеклянный отблеск по диагонали
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.32f),
                        0.45f to Color.White.copy(alpha = 0.05f),
                        0.46f to Color.Transparent,
                        1f to Color.Transparent
                    )
                )
            )
        }
    }
}

private val SkinNames = listOf("Metro", "Windows 7", "Aero", "Material You")

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

        // ── арт: иконка приложения выбранного стиля ──
        val float = rememberInfiniteTransition(label = "iconFloat")
        val dy by float.animateFloat(
            initialValue = 0f,
            targetValue = -8f,
            animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "iconDy"
        )
        Column(
            Modifier.fillMaxWidth().padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Crossfade(targetState = c.skin, label = "iconArt") { sk ->
                AppIconArt(sk, 148.dp, accent, Modifier.graphicsLayer { translationY = dy.dp.toPx() })
            }
            MText(SkinNames.getOrElse(c.skin) { "Metro" }, 30, Modifier.padding(top = 22.dp), color = c.accent)
            MText("значок приложения меняется вместе со стилем", 13, Modifier.padding(top = 2.dp), color = c.subtle)
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (sk in 0..3) {
                    AppIconArt(
                        sk, 52.dp, accent,
                        Modifier
                            .alpha(if (sk == c.skin) 1f else 0.45f)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                store.chooseSkin(sk)
                                com.metroplayer.SkinIcons.apply(app, sk)
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

        // ── автор ──
        MText("автор", 14, Modifier.padding(top = 20.dp), color = c.subtle)
        MText("corei5", 34, Modifier.padding(top = 2.dp), color = c.accent)

        Spacer(Modifier.height(32.dp))
    }
}
