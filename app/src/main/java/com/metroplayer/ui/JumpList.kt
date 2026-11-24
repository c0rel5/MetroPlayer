package com.metroplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Ключ группы для имени: первая буква в верхнем регистре, всё остальное (цифры, символы) — «#». */
fun jumpKey(name: String): String {
    val ch = name.trim().firstOrNull() ?: return "#"
    val u = ch.uppercaseChar()
    return if (u.isLetter()) u.toString() else "#"
}

private sealed interface JumpRow {
    data class Header(val key: String) : JumpRow
    data class Entry(val index: Int) : JumpRow
}

/**
 * Список с буквенными заголовками и «прыжком» по буквам, как в Windows Phone: тап по букве-заголовку
 * открывает сетку букв, тап по букве переносит в нужное место. Список должен быть отсортирован по имени.
 * Для коротких списков (меньше [minItems]) заголовки не показываются.
 */
@Composable
fun <T> JumpLazyList(
    data: List<T>,
    nameOf: (T) -> String,
    modifier: Modifier = Modifier,
    minItems: Int = 12,
    row: @Composable (Int, T) -> Unit
) {
    val c = Metro.colors
    val useJump = data.size >= minItems
    val rows = remember(data, useJump) {
        if (!useJump) {
            data.indices.map { JumpRow.Entry(it) }
        } else {
            val out = ArrayList<JumpRow>(data.size + 32)
            var last: String? = null
            data.forEachIndexed { i, t ->
                val k = jumpKey(nameOf(t))
                if (k != last) {
                    out.add(JumpRow.Header(k))
                    last = k
                }
                out.add(JumpRow.Entry(i))
            }
            out
        }
    }
    // позиция первого заголовка каждой буквы
    val firstPos = remember(rows) {
        val m = LinkedHashMap<String, Int>()
        rows.forEachIndexed { pos, r -> if (r is JumpRow.Header && r.key !in m) m[r.key] = pos }
        m
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    BackHandler(enabled = open) { open = false }

    Box(modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            items(rows.size) { pos ->
                when (val r = rows[pos]) {
                    is JumpRow.Header -> Box(
                        Modifier
                            .padding(start = 24.dp, top = 10.dp, bottom = 4.dp)
                            .size(44.dp)
                            .metroPress(pressScale = 0.92f) { open = true }
                            .skinBorder(2.dp, c.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        MText(r.key, 26, color = c.accent, lh = 1f, weight = FontWeight.Normal)
                    }
                    is JumpRow.Entry -> row(r.index, data[r.index])
                }
            }
        }

        if (open) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(c.bg.copy(alpha = 0.97f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { open = false }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MText("перейти к", 14, Modifier.padding(bottom = 6.dp), color = c.subtle)
                firstPos.entries.toList().chunked(4).forEach { chunk ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        chunk.forEach { (key, pos) ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(76.dp)
                                    .metroPress(pressScale = 0.92f) {
                                        open = false
                                        scope.launch { listState.scrollToItem(pos) }
                                    }
                                    .skinBg(c.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                MText(key, 34, color = Color.White, lh = 1f)
                            }
                        }
                        // добиваем неполную строку пустыми ячейками, чтобы плитки были одной ширины
                        repeat(4 - chunk.size) { Box(Modifier.weight(1f).height(76.dp)) }
                    }
                }
            }
        }
    }
}
