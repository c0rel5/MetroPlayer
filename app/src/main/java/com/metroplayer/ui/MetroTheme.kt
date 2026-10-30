package com.metroplayer.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@Immutable
class MetroColors(
    /** 0 — Metro, 1 — Windows 7, 2 — Aero, 3 — Material You */
    val skin: Int,
    val dark: Boolean,
    val bg: Color,
    val fg: Color,
    val subtle: Color,
    val inactive: Color,
    val track: Color,
    val accent: Color,
    val corner: Dp,
    val barColor: Color,
    val bgBrush: Brush?
) {
    fun withAccent(a: Color) = MetroColors(skin, dark, bg, fg, subtle, inactive, track, a, corner, barColor, bgBrush)
}

/** Нижняя панель приложения в теме Metro — одинаково тёмная в обеих темах (как в концепте). */
val BarColor = Color(0xFF1F1F1F)
val BarSubtle = Color(0xFFBBBBBB)

private val LocalMetro = staticCompositionLocalOf<MetroColors> { error("MetroTheme не задана") }

object Metro {
    val colors: MetroColors
        @Composable @ReadOnlyComposable get() = LocalMetro.current
}

private fun metroColors(dark: Boolean) =
    if (dark) MetroColors(
        0, true, Color.Black, Color.White, Color(0xFF8A8A8A), Color(0xFF6B6B6B), Color(0xFF333333),
        Color(0xFF1BA1E2), 0.dp, BarColor, null
    ) else MetroColors(
        0, false, Color.White, Color.Black, Color(0xFF666666), Color(0xFF8A8A8A), Color(0xFFCCCCCC),
        Color(0xFFE51400), 0.dp, BarColor, null
    )

/** Windows 7: серебристо-голубой градиент, глянцевые плитки с жёстким бликом посередине. */
private fun win7Colors(dark: Boolean) =
    if (dark) MetroColors(
        1, true, Color(0xFF1B1F24), Color(0xFFF0F4F8), Color(0xFF9AA6B2), Color(0xFF6C7884), Color(0xFF3A434D),
        Color(0xFF4E9BE6), 4.dp, Color(0xFF22282F),
        Brush.verticalGradient(listOf(Color(0xFF2B323A), Color(0xFF1B1F24), Color(0xFF171A1E)))
    ) else MetroColors(
        1, false, Color(0xFFF2F5F9), Color(0xFF1E1E1E), Color(0xFF5B6770), Color(0xFF8C98A3), Color(0xFFC9D3DD),
        Color(0xFF2F7FD0), 4.dp, Color(0xFF2A3F57),
        Brush.verticalGradient(listOf(Color(0xFFE8EEF5), Color(0xFFFFFFFF), Color(0xFFDCE5F0)))
    )

/** Aero Glass: стеклянный голубой фон, скруглённые полупрозрачные на вид плитки со светлой каймой. */
private fun aeroColors(dark: Boolean) =
    if (dark) MetroColors(
        2, true, Color(0xFF0A2744), Color.White, Color(0xFFA9C4E0), Color(0xFF6F8CAB), Color(0xFF2A4C70),
        Color(0xFF4DA6EA), 10.dp, Color(0xFF0E2F52),
        Brush.verticalGradient(listOf(Color(0xFF123F6B), Color(0xFF0A2744), Color(0xFF061B33)))
    ) else MetroColors(
        2, false, Color(0xFFD6E6F5), Color(0xFF10243A), Color(0xFF4A6785), Color(0xFF7D96B0), Color(0xFFB5CBE0),
        Color(0xFF1E73BE), 10.dp, Color(0xFF1B4F82),
        Brush.verticalGradient(listOf(Color(0xFFBBD6EF), Color(0xFFE9F2FB), Color(0xFFC3D9EE)))
    )

private fun sys(ctx: Context, id: Int, fallback: Long): Color =
    if (Build.VERSION.SDK_INT >= 31) Color(ctx.getColor(id)) else Color(fallback)

/** Material You (Material 3): на Android 12+ берёт цвета из обоев (dynamic color), иначе — базовая фиолетовая схема. */
private fun materialColors(ctx: Context, dark: Boolean): MetroColors =
    if (dark) MetroColors(
        3, true,
        sys(ctx, android.R.color.system_neutral1_900, 0xFF1C1B1F),
        sys(ctx, android.R.color.system_neutral1_100, 0xFFE6E1E5),
        sys(ctx, android.R.color.system_neutral2_300, 0xFFCAC4D0),
        sys(ctx, android.R.color.system_neutral2_400, 0xFF938F99),
        sys(ctx, android.R.color.system_neutral2_700, 0xFF49454F),
        sys(ctx, android.R.color.system_accent1_200, 0xFFD0BCFF),
        20.dp,
        sys(ctx, android.R.color.system_neutral2_800, 0xFF2B2930),
        null
    ) else MetroColors(
        3, false,
        sys(ctx, android.R.color.system_neutral1_10, 0xFFFFFBFE),
        sys(ctx, android.R.color.system_neutral1_900, 0xFF1C1B1F),
        sys(ctx, android.R.color.system_neutral2_700, 0xFF49454F),
        sys(ctx, android.R.color.system_neutral2_500, 0xFF79747E),
        sys(ctx, android.R.color.system_neutral2_200, 0xFFCAC4D0),
        sys(ctx, android.R.color.system_accent1_600, 0xFF6750A4),
        20.dp,
        sys(ctx, android.R.color.system_accent1_800, 0xFF4F378B),
        null
    )

@Composable
fun MetroTheme(dark: Boolean, accentOverride: Int, skin: Int, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val base = when (skin) {
        1 -> win7Colors(dark)
        2 -> aeroColors(dark)
        3 -> materialColors(ctx, dark)
        else -> metroColors(dark)
    }
    val colors = if (accentOverride != 0) base.withAccent(Color(accentOverride)) else base
    CompositionLocalProvider(LocalMetro provides colors, content = content)
}

/** Фон плитки/панели с учётом скина: Metro — плоский квадрат, Win7 — глянец, Aero — стекло, Material — скруглённая плашка. */
@Composable
fun Modifier.skinBg(color: Color): Modifier {
    val c = Metro.colors
    if (c.skin == 0) return this.background(color)
    val shape = RoundedCornerShape(c.corner)
    return when (c.skin) {
        1 -> this.clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to lerp(color, Color.White, 0.32f),
                    0.5f to lerp(color, Color.White, 0.10f),
                    0.5f to lerp(color, Color.Black, 0.06f),
                    1f to lerp(color, Color.Black, 0.20f)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.45f), shape)
        2 -> this.clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to lerp(color, Color.White, 0.35f),
                    0.45f to lerp(color, Color.White, 0.08f),
                    1f to lerp(color, Color.Black, 0.12f)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.6f), shape)
        else -> this.clip(shape).background(color)
    }
}

/** Нижняя панель: у Win7/Aero — стеклянный градиент «как таскбар», иначе сплошной цвет. */
@Composable
fun Modifier.skinBar(): Modifier {
    val c = Metro.colors
    return if (c.skin == 1 || c.skin == 2) {
        this.background(Brush.verticalGradient(listOf(lerp(c.barColor, Color.White, 0.16f), c.barColor)))
    } else {
        this.background(c.barColor)
    }
}

/** Рамка кнопки/поля: прямоугольная в Metro, скруглённая в остальных скинах. */
@Composable
fun Modifier.skinBorder(width: Dp, color: Color): Modifier {
    val c = Metro.colors
    if (c.skin == 0) return this.border(width, color)
    val r = if (c.corner < 12.dp) c.corner else 12.dp
    return this.border(width, color, RoundedCornerShape(r))
}

/**
 * Шрифт. В концепте Segoe UI / Open Sans. На Android берём системный Sans (Roboto) —
 * он имеет тот же Light-вес. Чтобы поставить Open Sans: положите OpenSans-Light.ttf,
 * OpenSans-Regular.ttf, OpenSans-SemiBold.ttf в res/font и замените значение ниже на
 * FontFamily(Font(R.font.opensans_light, FontWeight.Light), ...).
 */
val MetroFont: FontFamily = FontFamily.SansSerif

@Composable
fun MText(
    text: String,
    size: Int,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Light,
    color: Color = Metro.colors.fg,
    spacing: Float = 0f,
    lh: Float = 1.25f,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    align: TextAlign = TextAlign.Start
) {
    val w = if (weight == FontWeight.Light && Metro.colors.skin != 0) FontWeight.Normal else weight
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontFamily = MetroFont,
            fontSize = size.sp,
            fontWeight = w,
            color = color,
            letterSpacing = spacing.sp,
            lineHeight = (size * lh).sp,
            textAlign = align
        ),
        overflow = if (maxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip,
        softWrap = softWrap,
        maxLines = maxLines
    )
}

fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

fun plural(n: Int, one: String, few: String, many: String): String {
    val m100 = n % 100
    val m10 = n % 10
    val w = if (m100 in 11..14) many else when (m10) {
        1 -> one
        in 2..4 -> few
        else -> many
    }
    return "$n $w"
}
