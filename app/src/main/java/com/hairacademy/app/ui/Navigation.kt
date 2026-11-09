package com.hairacademy.app.ui

import android.view.View
import android.webkit.WebChromeClient
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hairacademy.app.data.MainViewModel
import com.hairacademy.app.data.Section
import com.hairacademy.app.data.Sub

private const val HOME = "home"
private const val FAVORITES = "favorites"

@Composable
fun AppRoot(
    vm: MainViewModel,
    onShowFullscreen: (View, WebChromeClient.CustomViewCallback) -> Unit,
    onHideFullscreen: () -> Unit,
) {
    val nav = rememberNavController()
    val favorites by vm.favorites.collectAsState()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()

    Scaffold(
        bottomBar = { if (!route.startsWith("player")) BottomBar(route, nav) },
    ) { pad ->
        NavHost(nav, startDestination = HOME, modifier = Modifier.padding(pad)) {

            composable(HOME) {
                HomeScreen(count = { vm.count(it) }, onSection = { nav.navigate("section/${it.id}") })
            }

            composable(FAVORITES) {
                val list = vm.videos.filter { it.key in favorites }
                VideoListScreen(
                    title = "Избранное",
                    videos = list,
                    favorites = favorites,
                    onBack = null,
                    emptyEmoji = "🤍",
                    emptyTitle = "Пока пусто",
                    emptyText = "Нажмите на сердечко у урока — он появится здесь.",
                    onOpen = { nav.navigate("player/${it.key}") },
                    onToggleFavorite = { vm.toggleFavorite(it.key) },
                )
            }

            composable("section/{section}", listOf(navArgument("section") { type = NavType.StringType })) { e ->
                val section = Section.from(e.arguments?.getString("section"))
                if (section != null) {
                    SectionScreen(
                        section = section,
                        count = { vm.count(section, it) },
                        onBack = { nav.popBackStack() },
                        onSub = { nav.navigate("list/${section.id}/${it.id}") },
                    )
                }
            }

            composable(
                "list/{section}/{sub}",
                listOf(navArgument("section") { type = NavType.StringType }, navArgument("sub") { type = NavType.StringType }),
            ) { e ->
                val section = Section.from(e.arguments?.getString("section"))
                val sub = Sub.from(e.arguments?.getString("sub"))
                if (section != null && sub != null) {
                    VideoListScreen(
                        title = "${section.title}: ${sub.shortTitle.lowercase()}",
                        videos = vm.videosFor(section, sub),
                        favorites = favorites,
                        onBack = { nav.popBackStack() },
                        emptyEmoji = sub.emoji,
                        emptyTitle = "Уроки скоро появятся",
                        emptyText = "В этом подразделе пока нет видео.",
                        onOpen = { nav.navigate("player/${it.key}") },
                        onToggleFavorite = { vm.toggleFavorite(it.key) },
                    )
                }
            }

            composable("player/{key}", listOf(navArgument("key") { type = NavType.StringType })) { e ->
                val key = e.arguments?.getString("key")
                PlayerScreen(
                    video = vm.find(key),
                    isFavorite = key != null && key in favorites,
                    onToggleFavorite = { if (key != null) vm.toggleFavorite(key) },
                    onBack = { nav.popBackStack() },
                    onShowFullscreen = onShowFullscreen,
                    onHideFullscreen = onHideFullscreen,
                )
            }
        }
    }
}

@Composable
private fun BottomBar(route: String, nav: NavHostController) {
    val onFavorites = route == FAVORITES
    NavigationBar {
        NavigationBarItem(
            selected = !onFavorites,
            onClick = { nav.goTab(HOME) },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Уроки") },
        )
        NavigationBarItem(
            selected = onFavorites,
            onClick = { nav.goTab(FAVORITES) },
            icon = { Icon(Icons.Default.Favorite, null) },
            label = { Text("Избранное") },
        )
    }
}

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(HOME) { saveState = true; inclusive = false }
        launchSingleTop = true
        restoreState = true
    }
}
