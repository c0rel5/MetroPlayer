package com.metroplayer

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Иконка приложения на рабочем столе зависит от скина.
 * В манифесте четыре activity-alias (Metro / Windows 7 / Aero / Material You);
 * включён всегда ровно один, остальные выключены.
 */
object SkinIcons {
    private val aliases = listOf("IconMetro", "IconWin7", "IconAero", "IconMaterial")

    /** Иконка в строке состояния (уведомление плеера) для скина. */
    fun statusIcon(skin: Int): Int = when (skin) {
        1 -> R.drawable.ic_stat_note        // Windows 7: кольцо-шар + нота
        2 -> R.drawable.ic_stat_aero        // Aero: двойное стеклянное кольцо
        3 -> R.drawable.ic_stat_material    // Material You: скруглённый квадрат
        else -> R.drawable.ic_stat_metro    // Metro: плоская нота
    }

    fun apply(context: Context, skin: Int) {
        val pm = context.packageManager
        val target = skin.coerceIn(0, aliases.lastIndex)
        try {
            // сначала включаем нужный, потом выключаем остальные — чтобы ярлык не пропал
            pm.setComponentEnabledSetting(
                name(context, aliases[target]),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            aliases.forEachIndexed { i, a ->
                if (i != target) {
                    pm.setComponentEnabledSetting(
                        name(context, a),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        } catch (e: Exception) {
            // не критично: иконка просто останется прежней
        }
    }

    private fun name(context: Context, alias: String) =
        ComponentName(context.packageName, "com.metroplayer.$alias")
}
