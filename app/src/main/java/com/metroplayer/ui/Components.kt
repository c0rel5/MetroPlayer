package com.metroplayer.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.metroplayer.MetroApp
import com.metroplayer.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/** SVG-пути иконок из концепта (viewBox 24×24, обводка 2). */
object Ic {
    const val PREV = "M7 5v14M19 5L9 12l10 7z"
    const val NEXT = "M17 5v14M5 5l10 7L5 19z"
    const val PAUSE = "M8 5v14M16 5v14"
    const val PLAY = "M8 5l11 7-11 7z"
    const val PLUS = "M12 4v16M4 12h16"
    const val PLAY_CIRCLE = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0M10 8.5l5 3.5-5 3.5z"
    const val PAUSE_CIRCLE = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0M10 9v6M14 9v6"
    const val RADIO = "M3 9h18v12H3zM7 9l9-5M5.5 15a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0M15 13h3M15 17h3"
    const val SEARCH = "M4 10.5a6.5 6.5 0 1 0 13 0a6.5 6.5 0 1 0-13 0M15.5 15.5L21 21"
    const val SHUFFLE = "M3 7h4l10 10h4M3 17h4l3-3M14 10l3-3h4M18 4l3 3-3 3M18 14l3 3-3 3"
    const val REPEAT = "M4 11V9a3 3 0 0 1 3-3h13M17 3l3 3-3 3M20 13v2a3 3 0 0 1-3 3H4M7 21l-3-3 3-3"
    const val REPEAT_ONE = "M4 11V9a3 3 0 0 1 3-3h13M17 3l3 3-3 3M20 13v2a3 3 0 0 1-3 3H4M7 21l-3-3 3-3M11 10.5l1.5-1v6"
    const val HEART = "M12 20C5 15.6 4 11 4 9a4 4 0 0 1 8-1.2A4 4 0 0 1 20 9c0 2-1 6.6-8 11z"
    const val ARTIST = "M8 8a4 4 0 1 0 8 0a4 4 0 1 0-8 0M4 21a8 8 0 0 1 16 0"
    const val ALBUM = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0M9.5 12a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0-5 0"
    const val PLAYLIST = "M4 6h12M4 11h12M4 16h7M17 20V12l4-1M17 20a2 2 0 1 1-4 0a2 2 0 1 1 4 0"
    const val RECENT = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0M12 7v5l3.5 2"
    const val MIX = "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8zM19 16l.8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8z"
    const val STATS = "M5 20V12M12 20V5M19 20V9M3 20h18"
    const val NOTE = "M9 18V5l11-2v13M9 18a3 3 0 1 1-6 0a3 3 0 1 1 6 0M20 16a3 3 0 1 1-6 0a3 3 0 1 1 6 0"
}

@Composable
fun MetroIcon(path: String, iconSize: Dp, color: Color, modifier: Modifier = Modifier, filled: Boolean = false) {
    val parsed = remember(path) { PathParser().parsePathString(path).toPath() }
    val skin = Metro.colors.skin
    val soft = skin != 0                       // скругления у всех скинов кроме Metro
    val solid = skin == 1 || skin == 2         // Windows 7 / Aero: «залитые» глянцевые значки
    val fillable = solid && (path == Ic.PLAY || path == Ic.PREV || path == Ic.NEXT)
    Canvas(modifier.size(iconSize)) {
        val s = this.size.minDimension / 24f
        withTransform({ scale(s, s, Offset.Zero) }) {
            if (fillable || filled) drawPath(parsed, color)
            drawPath(
                parsed, color,
                style = Stroke(
                    width = if (solid && path == Ic.PAUSE) 3.4f else 2f,
                    cap = if (soft) StrokeCap.Round else StrokeCap.Butt,
                    join = if (soft) StrokeJoin.Round else StrokeJoin.Miter
                )
            )
        }
    }
}

/**
 * Metro-нажатие: плитка слегка уменьшается, а при tilt=true ещё и наклоняется
 * в сторону точки касания.
 */
@Composable
fun Modifier.metroPress(
    tilt: Boolean = false,
    pressScale: Float = 0.94f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    var pressed by remember { mutableStateOf(false) }
    var pos by remember { mutableStateOf(Offset.Zero) }
    var sz by remember { mutableStateOf(IntSize.Zero) }
    val pressK by animateFloatAsState(if (pressed) pressScale else 1f, tween(120), label = "press")
    val nx = if (sz.width > 0) (pos.x / sz.width - 0.5f) * 2f else 0f
    val ny = if (sz.height > 0) (pos.y / sz.height - 0.5f) * 2f else 0f
    // наклон нужен только плиткам (tilt = true); у строк списков эти анимации не создаются вовсе
    val ry = if (tilt) animateFloatAsState(if (pressed) nx * 10f else 0f, tween(120), label = "ry").value else 0f
    val rx = if (tilt) animateFloatAsState(if (pressed) -ny * 10f else 0f, tween(120), label = "rx").value else 0f
    val click by rememberUpdatedState(onClick)
    val longClick by rememberUpdatedState(onLongClick)
    val haptic = LocalHapticFeedback.current
    // Для плиток — слой с наклоном. Для строк списков — лёгкое масштабирование при рисовании, без отдельного слоя
    // на каждую строку: так список прокручивается плавнее.
    val visual = if (tilt) {
        Modifier
            .onSizeChanged { sz = it }
            .graphicsLayer {
                scaleX = pressK
                scaleY = pressK
                rotationX = rx
                rotationY = ry
                cameraDistance = 16f * density
            }
    } else {
        Modifier.drawWithContent {
            val k = pressK
            if (k == 1f) {
                drawContent()
            } else {
                scale(k, k, center) { this@drawWithContent.drawContent() }
            }
        }
    }
    return this
        .then(visual)
        .pointerInput(onLongClick != null) {
            detectTapGestures(
                onPress = { o ->
                    pos = o
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                },
                onLongPress = if (onLongClick != null) {
                    { _: Offset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        longClick?.invoke()
                    }
                } else {
                    null
                },
                onTap = { click() }
            )
        }
}

/**
 * Вход элемента как в Windows Phone: плитка/строка «влетает» справа, поворачиваясь вокруг левого края,
 * с задержкой по номеру ([index]) — получается каскад. Элементы дальше [maxAnimated] не анимируются,
 * чтобы длинный список не «дёргался» при прокрутке.
 */
@Composable
fun Modifier.metroEnter(index: Int, stepMs: Int = 45, maxAnimated: Int = 14, tiltDeg: Float = 55f): Modifier {
    // Анимация нужна только первым строкам. Остальные при прокрутке не получают ни состояния, ни слоя —
    // это убирает лаги при быстрой прокрутке длинных списков.
    if (index !in 0 until maxAnimated) return this
    // Строка «влетает» один раз; rememberSaveable переживает выход строки за экран внутри LazyColumn,
    // поэтому при прокрутке назад первые строки не влетают заново.
    var played by rememberSaveable { mutableStateOf(false) }
    var done by remember { mutableStateOf(played) }
    val p = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            delay(index * stepMs.toLong())
            p.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
        played = true
        done = true
    }
    // когда анимация закончилась, отдельный слой строке больше не нужен
    if (done) return this
    return this.graphicsLayer {
        val v = p.value
        alpha = (v * 1.6f).coerceAtMost(1f)
        rotationY = (1f - v) * -tiltDeg
        translationX = (1f - v) * 48.dp.toPx()
        transformOrigin = TransformOrigin(0f, 0.5f)
        cameraDistance = 14f * density
    }
}

@Composable
fun MetroCircleButton(
    size: Dp,
    path: String,
    iconSize: Dp,
    color: Color,
    label: String? = null,
    onClick: () -> Unit
) {
    val skin = Metro.colors.skin
    val a11y = if (label != null) Modifier.semantics { contentDescription = label } else Modifier
    if (skin == 1 || skin == 2) {
        // Windows 7 / Aero: глянцевая «стеклянная» кнопка-шар, как в проигрывателе Windows Media Player 12
        val acc = Metro.colors.accent
        Box(
            Modifier
                .size(size)
                .then(a11y)
                .metroPress(onClick = onClick)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(lerp(acc, Color.White, 0.40f), acc, lerp(acc, Color.Black, 0.30f))
                    )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                .drawWithContent {
                    // блик на верхней половине шара
                    drawOval(
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.70f), Color.White.copy(alpha = 0.08f))
                        ),
                        topLeft = Offset(this.size.width * 0.12f, this.size.height * 0.04f),
                        size = Size(this.size.width * 0.76f, this.size.height * 0.48f)
                    )
                    drawContent()
                },
            contentAlignment = Alignment.Center
        ) {
            MetroIcon(path, iconSize, Color.White)
        }
    } else {
        Box(
            Modifier
                .size(size)
                .then(a11y)
                .metroPress(onClick = onClick)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MetroIcon(path, iconSize, color)
        }
    }
}

@Composable
fun Ellipsis(color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    MText(
        "···", 28,
        modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        weight = FontWeight.Normal, color = color, spacing = 4f, lh = 1f
    )
}

/* ───────────────────────── обложка ───────────────────────── */

private fun loadCover(context: Context, t: Track): ImageBitmap? = try {
    val bmp = if (Build.VERSION.SDK_INT >= 29) {
        context.contentResolver.loadThumbnail(t.uri, android.util.Size(640, 640), null)
    } else {
        context.contentResolver.openInputStream(t.artUri)?.use { BitmapFactory.decodeStream(it) }
    }
    bmp?.asImageBitmap()
} catch (e: Exception) {
    null
}

/** Квадрат цвета акцента с концентрическими кругами (как в концепте), либо настоящая обложка. */
@Composable
fun Cover(track: Track?, modifier: Modifier = Modifier, bg: Color = Metro.colors.accent) {
    val context = LocalContext.current
    val bmp by produceState<ImageBitmap?>(initialValue = null, track?.id) {
        value = if (track == null) null else withContext(Dispatchers.IO) { loadCover(context, track) }
    }
    Box(modifier.skinBg(bg), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) {
            Image(bitmap = b, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Canvas(Modifier.fillMaxSize(0.76f)) {
                val s = this.size.minDimension / 260f
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                drawCircle(Color.White, 120f * s, c, style = Stroke(2f * s))
                drawCircle(Color.White, 80f * s, c, alpha = 0.6f, style = Stroke(2f * s))
                drawCircle(Color.White, 40f * s, c, alpha = 0.4f, style = Stroke(2f * s))
                drawLine(Color.White, Offset(c.x - 120f * s, c.y), Offset(c.x + 120f * s, c.y), 2f * s, alpha = 0.3f)
            }
        }
    }
}

/* ───────────────────────── перемотка ───────────────────────── */

@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onScrub: (Long?) -> Unit = {}
) {
    val c = Metro.colors
    var dragFrac by remember { mutableStateOf<Float?>(null) }
    val seek by rememberUpdatedState(onSeek)
    val scrub by rememberUpdatedState(onScrub)
    val frac = dragFrac ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(durationMs) {
                detectTapGestures { o -> seek((o.x / size.width * durationMs).toLong()) }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        val f = (o.x / size.width).coerceIn(0f, 1f)
                        dragFrac = f
                        scrub((f * durationMs).toLong())
                    },
                    onDragEnd = {
                        dragFrac?.let { seek((it * durationMs).toLong()) }
                        dragFrac = null
                        scrub(null)
                    },
                    onDragCancel = {
                        dragFrac = null
                        scrub(null)
                    },
                    onHorizontalDrag = { change, _ ->
                        val f = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragFrac = f
                        scrub((f * durationMs).toLong())
                    }
                )
            }
    ) {
        Canvas(Modifier.fillMaxWidth().height(44.dp)) {
            val y = this.size.height / 2f
            val w = this.size.width
            val dragging = dragFrac != null
            val th = if (dragging) 6.dp.toPx() else 4.dp.toPx()
            // дорожка и заполненная часть — прямые «металлические» линии без скруглений
            drawRect(c.track, Offset(0f, y - th / 2f), Size(w, th))
            drawRect(c.accent, Offset(0f, y - th / 2f), Size(w * frac, th))
            // прямоугольный бегунок Metro: виден всегда, при перетаскивании крупнее
            val tw = if (dragging) 14.dp.toPx() else 8.dp.toPx()
            val tH = if (dragging) 30.dp.toPx() else 22.dp.toPx()
            val tx = (w * frac - tw / 2f).coerceIn(0f, w - tw)
            drawRect(if (dragging) c.accent else c.fg, Offset(tx, y - tH / 2f), Size(tw, tH))
        }
    }
}

/* ───────────────────────── pivot ───────────────────────── */

@Composable
fun MetroPivot(
    state: PagerState,
    caption: String,
    titles: List<String>,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit
) {
    val c = Metro.colors
    val scope = rememberCoroutineScope()
    val widths = remember(titles.size) { mutableStateListOf<Int>().also { l -> repeat(titles.size) { l.add(0) } } }
    val gapPx = with(LocalDensity.current) { 20.dp.roundToPx() }

    Column(modifier) {
        MText(caption, 12, Modifier.padding(start = 24.dp, top = 24.dp), FontWeight.SemiBold, c.subtle, spacing = 2f)
        Box(Modifier.fillMaxWidth().clipToBounds().padding(top = 4.dp, bottom = 12.dp)) {
            Row(
                Modifier
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .padding(start = 24.dp)
                    .offset {
                        val pos = (state.currentPage + state.currentPageOffsetFraction)
                            .coerceIn(0f, (titles.size - 1).toFloat())
                        val lo = floor(pos).toInt()
                        val hi = min(lo + 1, titles.size - 1)
                        val f = pos - lo
                        fun start(i: Int): Int {
                            var s = 0
                            for (k in 0 until i) s += widths[k] + gapPx
                            return s
                        }
                        IntOffset(-(start(lo) + (start(hi) - start(lo)) * f).roundToInt(), 0)
                    }
            ) {
                titles.forEachIndexed { i, t ->
                    val a = 1f - abs(state.currentPage + state.currentPageOffsetFraction - i).coerceIn(0f, 1f)
                    MText(
                        t, 46,
                        Modifier
                            .onSizeChanged { widths[i] = it.width }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                scope.launch { state.animateScrollToPage(i) }
                            },
                        color = lerp(c.inactive, c.fg, a), maxLines = 1, softWrap = false, lh = 1.1f
                    )
                    Spacer(Modifier.width(20.dp))
                }
            }
        }
        HorizontalPager(state = state, modifier = Modifier.weight(1f).fillMaxWidth()) { page -> content(page) }
    }
}

/* ───────────────────────── списки ───────────────────────── */

@Composable
fun TrackRow(
    title: String,
    subtitle: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    enterIndex: Int = -1
) {
    val c = Metro.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(68.dp)
            .metroEnter(enterIndex, stepMs = 30, tiltDeg = 35f)
            .metroPress(pressScale = 0.98f, onLongClick = onLongClick, onClick = onClick)
    ) {
        // акцентный маркер у левого края — «сейчас играет»
        if (highlighted) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .height(40.dp)
                    .background(c.accent)
            )
        }
        Row(
            Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = if (onMore != null) 8.dp else 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                MText(title, 22, color = if (highlighted) c.accent else c.fg, maxLines = 1)
                MText(subtitle, 14, color = c.subtle, maxLines = 1)
            }
            if (onMore != null) {
                Box(
                    Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "меню трека" }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onMore),
                    contentAlignment = Alignment.Center
                ) {
                    MText("···", 22, color = c.subtle, weight = FontWeight.Normal, spacing = 2f, lh = 1f)
                }
            }
        }
    }
}

/** Заголовок раздела внутри списка («исполнители», «треки»…). */
@Composable
fun SectionLabel(text: String) {
    MText(
        text, 14,
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
        FontWeight.SemiBold, Metro.colors.accent, spacing = 1f
    )
}

/** Кнопка с прямоугольной рамкой (иконка + подпись) — «играть всё», «перемешать». */
@Composable
fun MetroTextButton(label: String, icon: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Metro.colors
    Row(
        modifier
            .metroPress(onClick = onClick)
            .skinBorder(2.dp, c.fg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) MetroIcon(icon, 20.dp, c.fg)
        MText(label, 17, color = c.fg, maxLines = 1)
    }
}

/* ───────────────────────── переключатель и индикатор ───────────────────────── */

/** Переключатель Windows Phone: контурный прямоугольник, заливка акцентом и вертикальный бегунок. */
@Composable
fun MetroSwitch(checked: Boolean, onToggle: () -> Unit) {
    val c = Metro.colors
    val x by animateDpAsState(if (checked) 30.dp else 0.dp, tween(150), label = "switch")
    Box(
        Modifier
            .size(50.dp, 22.dp)
            .metroPress(pressScale = 0.94f, onClick = onToggle)
            .skinBorder(2.dp, if (checked) c.accent else c.fg)
            .padding(3.dp)
    ) {
        if (checked) Box(Modifier.fillMaxSize().background(c.accent))
        Box(
            Modifier
                .offset(x = x)
                .width(10.dp)
                .fillMaxHeight()
                .background(if (checked) Color.White else c.fg)
        )
    }
}

/** Строка настройки: название + пояснение слева, переключатель справа. Нажимается вся строка. */
@Composable
fun SettingToggle(label: String, hint: String?, checked: Boolean, onToggle: () -> Unit) {
    val c = Metro.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            MText(label, 20, color = c.fg)
            if (hint != null) MText(hint, 13, Modifier.padding(top = 2.dp), color = c.subtle)
        }
        MText(
            if (checked) "вкл" else "выкл", 14,
            Modifier.padding(end = 10.dp), color = if (checked) c.accent else c.subtle
        )
        MetroSwitch(checked, onToggle)
    }
}

/** Выбор одного из вариантов: выбранный — акцентом и с квадратным маркером. */
@Composable
fun <T> OptionGroup(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    modifier: Modifier = Modifier,
    onPick: (T) -> Unit
) {
    val c = Metro.colors
    Column(modifier) {
        MText(title, 14, color = c.subtle)
        Column(Modifier.padding(top = 4.dp)) {
            options.forEach { (label, value) ->
                val on = value == selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPick(value) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .then(if (on) Modifier.background(c.accent) else Modifier.border(2.dp, c.inactive))
                    )
                    MText(
                        label, 22, Modifier.padding(start = 14.dp),
                        color = if (on) c.accent else c.fg, weight = if (on) FontWeight.SemiBold else FontWeight.Light
                    )
                }
            }
        }
    }
}

/** Индикатор «бегущие точки» из Windows Phone. */
@Composable
fun MetroProgressDots(modifier: Modifier = Modifier) {
    val c = Metro.colors
    val t = rememberInfiniteTransition(label = "dots")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "dotsPhase"
    )
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val d = 3.dp.toPx()
        for (i in 0 until 5) {
            val p = (((phase - i * 0.05f) % 1f) + 1f) % 1f
            val q = p * 2f - 1f
            val e = (q * q * q + 1f) / 2f          // медленно в середине, быстро по краям
            drawRect(c.accent, Offset((this.size.width - d) * e, (this.size.height - d) / 2f), Size(d, d))
        }
    }
}

/* ───────────────────────── нижняя панель ───────────────────────── */

@Composable
fun PlayerButtons(app: MetroApp) {
    val st = app.player.state
    MetroCircleButton(44.dp, Ic.PREV, 24.dp, Color.White, "предыдущий") { app.player.previous() }
    MetroCircleButton(44.dp, if (st.isPlaying) Ic.PAUSE else Ic.PLAY, 24.dp, Color.White, if (st.isPlaying) "пауза" else "играть") { app.player.togglePlay() }
    MetroCircleButton(44.dp, Ic.NEXT, 24.dp, Color.White, "следующий") { app.player.next() }
}

@Composable
fun MetroBottomBar(
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    onMid: (() -> Unit)? = null,
    left: @Composable RowScope.() -> Unit
) {
    Column(modifier.fillMaxWidth().skinBar().navigationBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(76.dp)
                .then(
                    if (onMid != null) {
                        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onMid)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                left()
            }
            Ellipsis(BarSubtle, onMore)
        }
    }
}

/**
 * Мини-плеер в нижней панели: обложка, название, исполнитель, пауза/далее и «···».
 * Нажатие на название открывает «сейчас играет». [leftExtra] — дополнительные кнопки слева (поиск, «+»).
 */
@Composable
fun MiniPlayerBar(
    app: MetroApp,
    onOpen: () -> Unit,
    onMore: () -> Unit,
    leftExtra: (@Composable RowScope.() -> Unit)? = null
) {
    val st = app.player.state
    val cur = st.current
    Column(Modifier.fillMaxWidth().skinBar().navigationBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leftExtra != null) leftExtra(this)
            if (cur != null) {
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpen),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leftExtra == null) Cover(cur, Modifier.size(50.dp))
                    Column(Modifier.weight(1f)) {
                        MText(cur.title, 17, color = Color.White, weight = FontWeight.SemiBold, maxLines = 1)
                        MText(cur.artist, 13, color = BarSubtle, maxLines = 1)
                    }
                }
                MetroCircleButton(
                    42.dp, if (st.isPlaying) Ic.PAUSE else Ic.PLAY, 22.dp, Color.White,
                    if (st.isPlaying) "пауза" else "играть"
                ) { app.player.togglePlay() }
                MetroCircleButton(42.dp, Ic.NEXT, 22.dp, Color.White, "следующий") { app.player.next() }
            } else {
                Spacer(Modifier.weight(1f))
            }
            Ellipsis(BarSubtle, onMore)
        }
    }
}

/* ───────────────────────── меню приложения ───────────────────────── */

class MenuEntry(val label: String, val onClick: () -> Unit)

@Composable
fun MetroMenu(visible: Boolean, items: List<MenuEntry>, header: String? = null, onDismiss: () -> Unit) {
    // «Назад» закрывает меню, а не уходит с экрана
    BackHandler(enabled = visible, onBack = onDismiss)
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(200)) { it },
            exit = slideOutVertically(tween(150)) { it }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .skinBar()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                if (header != null) {
                    MText(
                        header, 14,
                        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
                        FontWeight.SemiBold, BarSubtle, maxLines = 1, align = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
                items.forEach { e ->
                    MText(
                        e.label, 20,
                        Modifier
                            .fillMaxWidth()
                            .metroPress(pressScale = 0.97f) {
                                onDismiss()
                                e.onClick()
                            }
                            .padding(vertical = 12.dp),
                        color = Color.White, align = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}
