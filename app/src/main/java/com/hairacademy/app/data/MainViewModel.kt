package com.hairacademy.app.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val videos: List<Video> = VideoRepository.load(app)

    private val prefs = app.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    private val _favorites = MutableStateFlow(prefs.getStringSet(KEY, emptySet())?.toSet() ?: emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    fun toggleFavorite(key: String) {
        val new = _favorites.value.toMutableSet().apply { if (!add(key)) remove(key) }
        _favorites.value = new
        prefs.edit().putStringSet(KEY, new).apply()
    }

    fun videosFor(section: Section, sub: Sub): List<Video> =
        videos.filter { it.section == section && it.sub == sub }

    fun count(section: Section, sub: Sub? = null): Int =
        videos.count { it.section == section && (sub == null || it.sub == sub) }

    fun find(key: String?): Video? = videos.firstOrNull { it.key == key }

    private companion object {
        const val KEY = "keys"
    }
}
