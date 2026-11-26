package com.metroplayer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Одно засчитанное прослушивание (трек сыграл 30 секунд или половину). */
data class Play(val time: Long, val id: Long)

/** Кусок реального времени прослушивания: [ms] миллисекунд, закончившийся в момент [time]. */
data class Listen(val time: Long, val ms: Long)

/**
 * Журнал прослушиваний для статистики и Daily Mix. Хранится в файле filesDir/plays.log,
 * по строке на событие: «p,время,id трека» или «l,время,миллисекунды». Данные остаются только на устройстве.
 */
class PlayLog(context: Context) {
    private val file = File(context.applicationContext.filesDir, "plays.log")

    var plays: List<Play> = emptyList()
        private set
    var listens: List<Listen> = emptyList()
        private set

    /** Растёт при каждом изменении, чтобы экраны перерисовывались. */
    var version by mutableIntStateOf(0)
        private set

    init {
        load()
    }

    private fun load() {
        val p = ArrayList<Play>()
        val l = ArrayList<Listen>()
        val limit = System.currentTimeMillis() - KEEP_DAYS * DAY_MS
        var dropped = false
        fun parse(line: String) {
            val a = line.split(",")
            if (a.size != 3) return
            val t = a[1].toLongOrNull() ?: return
            val v = a[2].toLongOrNull() ?: return
            if (t < limit) {
                dropped = true
                return
            }
            if (a[0] == "p") p.add(Play(t, v)) else if (a[0] == "l") l.add(Listen(t, v))
        }
        try {
            if (file.exists()) file.forEachLine { parse(it) }
        } catch (e: Exception) {
            // повреждённые строки просто пропускаем
        }
        plays = p
        listens = l
        if (dropped) rewrite()
    }

    private fun rewrite() {
        try {
            file.bufferedWriter().use { w ->
                plays.forEach { w.write("p,${it.time},${it.id}\n") }
                listens.forEach { w.write("l,${it.time},${it.ms}\n") }
            }
        } catch (e: Exception) {
            // не критично
        }
    }

    fun addPlay(id: Long) {
        val t = System.currentTimeMillis()
        plays = plays + Play(t, id)
        append("p,$t,$id")
        version++
    }

    fun addListen(ms: Long) {
        val t = System.currentTimeMillis()
        listens = listens + Listen(t, ms)
        append("l,$t,$ms")
        version++
    }

    fun clear() {
        plays = emptyList()
        listens = emptyList()
        try {
            file.delete()
        } catch (e: Exception) {
            // ничего
        }
        version++
    }

    private fun append(line: String) {
        try {
            file.appendText(line + "\n")
        } catch (e: Exception) {
            // не критично
        }
    }

    private companion object {
        const val DAY_MS = 86_400_000L
        const val KEEP_DAYS = 400L
    }
}

/** Расчёты для экрана статистики. */
object Stats {
    class Summary(val listenMs: Long, val plays: Int, val tracks: Int, val artists: Int, val activeDays: Int)

    fun dayOf(t: Long): Long = Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

    fun today(): Long = LocalDate.now().toEpochDay()

    /** [since] — начало периода в миллисекундах (0 — всё время). */
    fun summary(log: PlayLog, since: Long, artistOf: (Long) -> String?): Summary {
        val p = log.plays.filter { it.time >= since }
        val l = log.listens.filter { it.time >= since }
        val days = HashSet<Long>()
        p.forEach { days.add(dayOf(it.time)) }
        l.forEach { days.add(dayOf(it.time)) }
        val artists = HashSet<String>()
        p.forEach { x -> artistOf(x.id)?.let { artists.add(it) } }
        return Summary(l.sumOf { it.ms }, p.size, p.map { it.id }.toSet().size, artists.size, days.size)
    }

    /** Сколько дней подряд (до сегодня или вчера) что-то слушали. */
    fun streak(log: PlayLog): Int {
        val days = HashSet<Long>()
        log.plays.forEach { days.add(dayOf(it.time)) }
        log.listens.forEach { days.add(dayOf(it.time)) }
        var d = today()
        if (d !in days) d -= 1
        var n = 0
        while (d in days) {
            n++
            d--
        }
        return n
    }

    fun topTracks(log: PlayLog, since: Long, limit: Int): List<Pair<Long, Int>> =
        log.plays.filter { it.time >= since }
            .groupingBy { it.id }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(limit).map { it.key to it.value }

    fun topArtists(log: PlayLog, since: Long, limit: Int, artistOf: (Long) -> String?): List<Pair<String, Int>> {
        val counts = HashMap<String, Int>()
        log.plays.filter { it.time >= since }.forEach { p ->
            val a = artistOf(p.id) ?: return@forEach
            counts[a] = (counts[a] ?: 0) + 1
        }
        return counts.entries.sortedByDescending { it.value }.take(limit).map { it.key to it.value }
    }

    /** Миллисекунды прослушивания по дням, от старого к новому; последний элемент — сегодня. */
    fun dailyMs(log: PlayLog, days: Int): List<Long> {
        val arr = LongArray(days)
        val today = today()
        log.listens.forEach {
            val i = (days - 1) - (today - dayOf(it.time))
            if (i >= 0L && i < days.toLong()) arr[i.toInt()] += it.ms
        }
        return arr.toList()
    }
}
