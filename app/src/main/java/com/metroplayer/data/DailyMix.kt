package com.metroplayer.data

import kotlin.math.pow
import kotlin.random.Random

/**
 * Daily Mix: подборка на день из истории прослушиваний. Всё считается на устройстве, без интернета.
 * Состав: любимые треки (чем чаще и свежее слушали, тем вероятнее), новые треки любимых исполнителей
 * и давно не звучавшие. Для одного и того же дня подборка одна и та же, пока её не пересоберут вручную.
 */
object DailyMix {
    private const val DAY_MS = 86_400_000L

    private class Scored(val id: Long, val w: Double)

    /** Случайная выборка без повторов, где вероятность пропорциональна весу. */
    private fun sample(items: List<Scored>, n: Int, rnd: Random): List<Long> {
        if (n <= 0) return emptyList()
        return items
            .map { it to rnd.nextDouble().pow(1.0 / it.w.coerceAtLeast(1e-6)) }
            .sortedByDescending { it.second }
            .take(n)
            .map { it.first.id }
    }

    fun build(
        tracks: List<Track>,
        plays: List<Play>,
        favorites: Set<Long>,
        day: Long,
        salt: Int,
        size: Int = 30
    ): List<Long> {
        if (tracks.isEmpty()) return emptyList()
        val rnd = Random(day * 1_000_003L + salt * 7_919L + 17L)
        val now = System.currentTimeMillis()
        val byId = tracks.associateBy { it.id }

        val score = HashMap<Long, Double>()
        val count = HashMap<Long, Int>()
        val last = HashMap<Long, Long>()
        for (p in plays) {
            if (p.id !in byId) continue
            val ageDays = (now - p.time).coerceAtLeast(0L) / DAY_MS.toDouble()
            score[p.id] = (score[p.id] ?: 0.0) + 0.5.pow(ageDays / 14.0)   // полураспад — две недели
            count[p.id] = (count[p.id] ?: 0) + 1
            if (p.time > (last[p.id] ?: 0L)) last[p.id] = p.time
        }

        val picked = LinkedHashSet<Long>()
        if (score.size < 3) {
            // истории почти нет: избранное и случайные треки
            tracks.filter { it.id in favorites }.shuffled(rnd).take(size / 2).forEach { picked.add(it.id) }
        } else {
            // 1) любимое
            picked.addAll(sample(score.map { Scored(it.key, it.value) }, size * 2 / 5, rnd))

            // 2) новое у любимых исполнителей
            val artistW = HashMap<String, Double>()
            for ((id, s) in score) {
                val a = byId.getValue(id).artist
                artistW[a] = (artistW[a] ?: 0.0) + s
            }
            val topArtists = artistW.entries.sortedByDescending { it.value }.take(8).map { it.key }.toSet()
            val discovery = tracks
                .filter { it.artist in topArtists && (count[it.id] ?: 0) <= 1 && it.id !in picked }
                .map { Scored(it.id, artistW[it.artist] ?: 1.0) }
            picked.addAll(sample(discovery, size * 2 / 5, rnd))

            // 3) давно не слушали (избранное — с повышенным шансом)
            val rediscover = tracks
                .filter { it.id !in picked && (last[it.id] ?: 0L) < now - 30 * DAY_MS }
                .map { Scored(it.id, if (it.id in favorites) 3.0 else 1.0) }
            picked.addAll(sample(rediscover, size - picked.size, rnd))
        }
        // добираем случайными, если не хватило
        if (picked.size < size) {
            tracks.shuffled(rnd).forEach { if (picked.size < size) picked.add(it.id) }
        }

        return spreadArtists(picked.toList().shuffled(rnd), byId).take(size)
    }

    /** Переставляет так, чтобы один и тот же исполнитель по возможности не шёл подряд. */
    private fun spreadArtists(ids: List<Long>, byId: Map<Long, Track>): List<Long> {
        val rest = ArrayList(ids)
        val out = ArrayList<Long>()
        while (rest.isNotEmpty()) {
            val prev = out.lastOrNull()?.let { byId[it]?.artist }
            val idx = rest.indexOfFirst { byId[it]?.artist != prev }.let { if (it < 0) 0 else it }
            out.add(rest.removeAt(idx))
        }
        return out
    }
}
