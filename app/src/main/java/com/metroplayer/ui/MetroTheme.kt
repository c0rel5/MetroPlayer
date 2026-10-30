package com.metroplayer.ui

import androidx.compose.foundation.text.BasicText
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
    val dark: Boolean,
    val bg: Color,
    val fg: Color,
    val subtle: Color,
    val inactive: Color,
    val track: Color,
    val accent: Color
)

/** Нижняя панель приложения — одинаково тёмная в обеих темах (как в концепте). */
val BarColor = Color(0xFF1F1F1F)
val BarSubtle = Color(0xFFBBBBBB)

private val DefaultAccentDark = Color(0xFF1BA1E2)
private val DefaultAccentLight = Color(0xFFE51400)

private val LocalMetro = staticCompositionLocalOf<MetroColors> { error("MetroTheme не задана") }

object Metro {
    val colors: MetroColors
        @Composable @ReadOnlyComposable get() = LocalMetro.current
}

@Composable
fun MetroTheme(dark: Boolean, accentOverride: Int, content: @Composable () -> Unit) {
    val accent = when {
        accentOverride != 0 -> Color(accentOverride)
        dark -> DefaultAccentDark
        else -> DefaultAccentLight
    }
    val colors = if (dark) {
        MetroColors(true, Color.Black, Color.White, Color(0xFF8A8A8A), Color(0xFF6B6B6B), Color(0xFF333333), accent)
    } else {
        MetroColors(false, Color.White, Color.Black, Color(0xFF666666), Color(0xFF8A8A8A), Color(0xFFCCCCCC), accent)
    }
    CompositionLocalProvider(LocalMetro provides colors, content = content)
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
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontFamily = MetroFont,
            fontSize = size.sp,
            fontWeight = weight,
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
