package com.hairacademy.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Читает список уроков из assets/videos.json.
 * Формат записи — см. README.md.
 */
object VideoRepository {

    /** Ролики короче этого значения (в минутах) не показываются. Если durationMin не указан — показываются. */
    const val MIN_LESSON_MINUTES = 20

    private val YT_ID = Regex("""(?:v=|youtu\.be/|embed/|shorts/|live/)([A-Za-z0-9_-]{11})""")

    fun load(context: Context): List<Video> = try {
        val text = context.assets.open("videos.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                parse(obj)?.let { add(it) }
            }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun parse(o: JSONObject): Video? {
        val section = Section.from(o.optString("section")) ?: return null
        val sub = Sub.from(o.optString("sub")) ?: return null
        if (sub !in section.subs) return null

        val url = o.optString("url").trim()
        var id = o.optString("id").trim()
        if (id.isEmpty() && url.isNotEmpty()) {
            id = YT_ID.find(url)?.groupValues?.get(1).orEmpty()
        }
        if (id.isEmpty() && url.isEmpty()) return null

        val minutes = o.optInt("durationMin", 0)
        if (minutes in 1 until MIN_LESSON_MINUTES) return null

        return Video(
            id = id,
            url = url,
            title = o.optString("title").ifBlank { "Без названия" },
            channel = o.optString("channel"),
            language = o.optString("language"),
            durationMin = minutes,
            section = section,
            sub = sub,
            thumbnail = o.optString("thumbnail").trim(),
            style = Style.from(o.optString("style")),
        )
    }
}
