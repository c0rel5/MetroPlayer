package com.metroplayer.data

/** Готовые пресеты эквалайзера. Усиление в дБ для пяти полос (60 Гц · 230 Гц · 910 Гц · 3,6 кГц · 14 кГц). */
object EqPresets {
    class Preset(val id: String, val name: String, val gains: List<Float>)

    const val MAX_DB = 12f

    val labels = listOf("60 Гц", "230 Гц", "910 Гц", "3,6 кГц", "14 кГц")

    val all = listOf(
        Preset("flat", "Нормальный", listOf(0f, 0f, 0f, 0f, 0f)),
        Preset("bass", "Бас", listOf(6f, 4f, 1f, 0f, 0f)),
        Preset("treble", "Высокие", listOf(0f, 0f, 0f, 3f, 6f)),
        Preset("vshape", "V-образный", listOf(6f, 3f, -2f, 3f, 6f)),
        Preset("rock", "Рок", listOf(4f, 2f, -2f, 2f, 4f)),
        Preset("pop", "Поп", listOf(-1f, 2f, 4f, 2f, -1f)),
        Preset("hiphop", "Хип-хоп", listOf(5f, 4f, 0f, 1f, 3f)),
        Preset("electronic", "Электроника", listOf(4f, 3f, 0f, 2f, 4f)),
        Preset("jazz", "Джаз", listOf(3f, 1f, -1f, 1f, 3f)),
        Preset("classical", "Классика", listOf(3f, 2f, -1f, 2f, 3f)),
        Preset("acoustic", "Акустика", listOf(4f, 3f, 1f, 2f, 3f)),
        Preset("vocal", "Вокал", listOf(-2f, 0f, 3f, 3f, 1f))
    )

    fun byId(id: String): Preset? = all.firstOrNull { it.id == id }
}
