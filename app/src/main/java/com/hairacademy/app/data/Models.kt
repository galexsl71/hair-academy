package com.hairacademy.app.data

/** Подраздел внутри раздела. */
enum class Sub(val id: String, val title: String, val shortTitle: String, val emoji: String) {
    SHORT("short", "Короткие волосы", "Короткие", "✂️"),
    MEDIUM("medium", "Средние волосы", "Средние", "💈"),
    LONG("long", "Длинные волосы", "Длинные", "🌊"),
    BEARD("beard", "Борода", "Борода", "🧔");

    companion object {
        fun from(id: String?): Sub? = values().firstOrNull { it.id == id }
    }
}

/** Раздел верхнего уровня. Цвета — градиент карточки на главном экране. */
enum class Section(
    val id: String,
    val title: String,
    val emoji: String,
    val subs: List<Sub>,
    val colorStart: Long,
    val colorEnd: Long,
) {
    WOMEN("women", "Женские стрижки", "💇‍♀️", listOf(Sub.SHORT, Sub.MEDIUM, Sub.LONG), 0xFFEC6A9B, 0xFF9C2A6B),
    MEN("men", "Мужские стрижки", "💇‍♂️", listOf(Sub.SHORT, Sub.MEDIUM, Sub.LONG, Sub.BEARD), 0xFF4A90E2, 0xFF1F4E8C),
    KIDS("kids", "Детские стрижки", "🧒", listOf(Sub.SHORT, Sub.MEDIUM, Sub.LONG), 0xFFFFB347, 0xFFE0701F);

    companion object {
        fun from(id: String?): Section? = values().firstOrNull { it.id == id }
    }
}

data class Video(
    val id: String,          // YouTube ID (11 символов) или пусто, если видео не с YouTube
    val url: String,         // ссылка на видео (для внешнего открытия)
    val title: String,
    val channel: String,
    val language: String,
    val durationMin: Int,    // 0 = неизвестно
    val section: Section,
    val sub: Sub,
    val thumbnail: String,   // свой URL превью (нужен только если нет YouTube ID)
) {
    val isYoutube: Boolean get() = id.isNotEmpty()

    /** Уникальный ключ — по нему хранится избранное и открывается плеер. */
    val key: String get() = if (id.isNotEmpty()) id else url.hashCode().toString()

    val watchUrl: String get() = if (url.isNotEmpty()) url else "https://www.youtube.com/watch?v=$id"

    val thumbnailUrl: String
        get() = when {
            thumbnail.isNotEmpty() -> thumbnail
            isYoutube -> "https://img.youtube.com/vi/$id/hqdefault.jpg"
            else -> ""
        }
}

fun formatDuration(min: Int): String = when {
    min <= 0 -> ""
    min < 60 -> "$min мин"
    min % 60 == 0 -> "${min / 60} ч"
    else -> "${min / 60} ч ${min % 60} мин"
}
