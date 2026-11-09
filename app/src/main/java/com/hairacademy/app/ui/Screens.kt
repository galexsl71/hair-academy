package com.hairacademy.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hairacademy.app.data.Section
import com.hairacademy.app.data.Style
import com.hairacademy.app.data.Sub
import com.hairacademy.app.data.Video

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(title: String, onBack: (() -> Unit)?, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        content = content,
    )
}

@Composable
fun HomeScreen(count: (Section) -> Int, onSection: (Section) -> Unit) {
    AppScaffold("Школа стрижки", null) { pad ->
        LazyColumn(
            Modifier.padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Видеоуроки по парикмахерскому делу", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(Section.values().toList()) { s ->
                SectionCard(s, count(s)) { onSection(s) }
            }
        }
    }
}

@Composable
fun SectionScreen(section: Section, count: (Sub) -> Int, onBack: () -> Unit, onSub: (Sub) -> Unit) {
    AppScaffold(section.title, onBack) { pad ->
        LazyColumn(
            Modifier.padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(section.subs) { sub -> SubCard(sub, count(sub)) { onSub(sub) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    title: String,
    videos: List<Video>,
    favorites: Set<String>,
    onBack: (() -> Unit)?,
    emptyEmoji: String,
    emptyTitle: String,
    emptyText: String,
    onOpen: (Video) -> Unit,
    onToggleFavorite: (Video) -> Unit,
) {
    var style by remember { mutableStateOf<Style?>(null) }
    var lang by remember { mutableStateOf<String?>(null) }

    val styles = Style.values().filter { st -> videos.any { it.style == st } }
    val langs = videos.map { it.language.uppercase() }.filter { it.isNotEmpty() }.distinct().sorted()
    val shown = videos.filter { v ->
        (style == null || v.style == style) && (lang == null || v.language.uppercase() == lang)
    }

    AppScaffold(title, onBack) { pad ->
        if (videos.isEmpty()) {
            EmptyState(emptyEmoji, emptyTitle, emptyText, Modifier.padding(pad))
        } else {
            LazyColumn(
                Modifier.padding(pad),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (styles.size > 1) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { FilterChip(selected = style == null, onClick = { style = null }, label = { Text("Все стрижки") }) }
                            items(styles) { st ->
                                FilterChip(selected = style == st, onClick = { style = if (style == st) null else st }, label = { Text(st.title) })
                            }
                        }
                    }
                }
                if (langs.size > 1) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { FilterChip(selected = lang == null, onClick = { lang = null }, label = { Text("Все языки") }) }
                            items(langs) { l ->
                                FilterChip(selected = lang == l, onClick = { lang = if (lang == l) null else l }, label = { Text(l) })
                            }
                        }
                    }
                }
                if (shown.isEmpty()) {
                    item { Text("По выбранным фильтрам уроков нет.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(shown, key = { it.key }) { v ->
                    VideoCard(
                        video = v,
                        isFavorite = v.key in favorites,
                        onClick = { onOpen(v) },
                        onToggleFavorite = { onToggleFavorite(v) },
                    )
                }
            }
        }
    }
}
