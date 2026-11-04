package com.metroplayer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Арт №1 — пасхалка: появляется на месте арта №2 после [EGG_TAPS] тапов по нему. */
private val ArtEgg = """
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@#################################@@@@@@@@
@@@@@@@ :+++++++++++++++++++++++++++++: @@@@@@@@
@@@@@@@ -@@@@@@-.              .*@@@@@- @@@@@@@@
@@@@@@@ -@@%                       +@@- @@@@@@@@
@@@@@@@ -%                           +- @@@@@@@@
@@@@@@@ .                               @@@@@@@@
@@@@@@@ -#.                          .- @@@@@@@@
@@@@@@@ :#@-=@#**@@@-     :*@@%**@+.+#: @@@@@@@@
@@@@@@@ -@@-:     +@@@@ -@@@%.     .@%- @@@@@@@@
@@@@@@@ -#  #    :   :   ..  :    #  :- @@@@@@@@
@@@@@@@     * +@@@@@+ # - :@@@@@#  -    @@@@@@@@
@@@@@@@      *=   -%: @ -+ *=.  :+      @@@@@@@@
@@@@@@@     %       +.    *+      %-    @@@@@@@@
@@@@@@@   @@*       *.    :*       @@   @@@@@@@@
@@@@@@@  -@@@+==+@@@@@#=*@@@@@#==+@@@@  @@@@@@@@
@@@@@@@ -:@@@@@@@@@@@@@@@@@@@@@@@@@@@.  @@@@@@@@
@@@@@@@ -@.#@@@@@@@@@@@@@@@@@@@@@@@%.@- @@@@@@@@
@@@@@@@ -@@-     =@@@@@#%@@@@#      @@- @@@@@@@@
@@@@@@@ -@@@       -#@@@@@@-       *@@- @@@@@@@@
@@@@@@@ -@@@                       *@@- @@@@@@@@
@@@@@@@ -@@@@                     =@@@- @@@@@@@@
@@@@@@@ -@@@@@#                 -%@@@@- @@@@@@@@
@@@@@@@ -@@@@@@@%             -@@@@@@@- @@@@@@@@
@@@@@@@                                 @@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@%@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@ #%@::=@:=@=::@. @@::+@ :-==:#@@@@@@@@@
@@@@@@@@@ @@@. + * @-*%@# @# .=@.@@-- *@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@%+.@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
"""

/** Арт №2 — виден всегда. */
private val Art2 = """
================================================
================================================
================================================
================================================
================================================
======================....======================
=================  .========.  =================
==============: -==============- :==============
============= :==================: =============
============ -==========-    :====- ============
=========== .========= =====.:=====. ===========
=========== ========== =====.:====== ===========
=========== ========== ==. . :====== ===========
=========== .=====- == == == ======. ===========
============ -=====   :===========- ============
============= :==================: =============
==============: -==============- :==============
=================  .========.  =================
======================....======================
================================================
================================================
================================================
================================================
================================================
"""

/** Арт №3 — надпись из точек (рисуется кружками, чтобы не зависеть от шрифта). */
private val Art3 = """
••••  •   •     •••   •••  ••••  ••••• ••• ••••• 
•   •  • •     •     •   • •   • •      •  •     
••••    •      •     •   • ••••  ••••   •  ••••  
•   •   •      •     •   • •  •  •      •      • 
••••    •       •••   •••  •   • ••••• ••• ••••  
"""

private const val EGG_TAPS = 10

private fun artLines(art: String): List<String> = art.trim('\n').lines()

/** Моноширинный ASCII-арт, масштабируется под ширину экрана. */
@Composable
private fun AsciiBlock(art: String, color: Color, modifier: Modifier = Modifier) {
    val lines = remember(art) { artLines(art) }
    val cols = remember(art) { lines.maxOf { it.length } }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // у моноширинного шрифта ширина символа ≈ 0.6 em
        val fs = with(LocalDensity.current) { (maxWidth / (cols * 0.6f)).toSp() }
        BasicText(
            text = lines.joinToString("\n"),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = fs,
                lineHeight = fs,
                color = color
            ),
            softWrap = false
        )
    }
}

/** Арт из «•»: каждая точка — кружок на сетке. */
@Composable
private fun DotBlock(art: String, color: Color, modifier: Modifier = Modifier) {
    val lines = remember(art) { artLines(art) }
    val cols = remember(art) { lines.maxOf { it.length } }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cell = maxWidth / cols
        Canvas(Modifier.fillMaxWidth().height(cell * lines.size)) {
            val step = size.width / cols
            lines.forEachIndexed { y, line ->
                line.forEachIndexed { x, ch ->
                    if (ch == '•') {
                        drawCircle(color, step * 0.42f, Offset((x + 0.5f) * step, (y + 0.5f) * step))
                    }
                }
            }
        }
    }
}

/** Блок арта для экрана «о приложении»: арт №2 (по многократному тапу превращается в арт №1) и арт №3. */
@Composable
fun AboutAsciiArt() {
    val c = Metro.colors
    var taps by remember { mutableIntStateOf(0) }
    val egg = taps >= EGG_TAPS

    Column(Modifier.fillMaxWidth().padding(top = 28.dp)) {
        AsciiBlock(
            art = if (egg) ArtEgg else Art2,
            color = c.accent,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (!egg) taps++ }
        )
        DotBlock(Art3, c.fg, Modifier.padding(top = 16.dp))
    }
}
