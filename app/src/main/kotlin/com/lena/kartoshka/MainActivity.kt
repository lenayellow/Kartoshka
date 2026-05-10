package com.lena.kartoshka

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lena.kartoshka.data.AppRepository
import com.lena.kartoshka.data.LastUsedRepository
import com.lena.kartoshka.data.UserPrefsRepository
import com.lena.kartoshka.data.db.KartoshkaDatabase
import com.lena.kartoshka.data.sort.LocalSortRepository
import com.lena.kartoshka.ui.screens.listdetail.ListDetailScreen
import com.lena.kartoshka.ui.screens.mylists.MyListsScreen
import com.lena.kartoshka.ui.screens.newlist.NewListScreen
import com.lena.kartoshka.ui.screens.share.ShareScreen
import com.lena.kartoshka.ui.theme.KartoshkaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val sortRepository by lazy { LocalSortRepository(applicationContext) }
    private val lastUsedRepository by lazy { LastUsedRepository(applicationContext) }
    private val appRepository by lazy { AppRepository(KartoshkaDatabase.get(applicationContext)) }
    private val userPrefsRepository by lazy { UserPrefsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by userPrefsRepository.isDark.collectAsState()
            val avatarPath by userPrefsRepository.avatarPath.collectAsState()
            KartoshkaTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val scope = rememberCoroutineScope()

                    val lastListId by lastUsedRepository.observeLastListId().collectAsState(initial = "")
                    var navigatedToLastList by remember { mutableStateOf(false) }

                    // Seed sample data on first launch
                    LaunchedEffect(Unit) {
                        appRepository.seedIfEmpty()
                    }

                    // Reopen last list on next launch
                    val allLists by appRepository.observeLists().collectAsState(initial = emptyList())
                    LaunchedEffect(lastListId, allLists) {
                        if (!navigatedToLastList && lastListId.isNotEmpty() && allLists.isNotEmpty()) {
                            val listExists = allLists.any { it.id == lastListId }
                            if (listExists) {
                                navController.navigate("list/$lastListId") {
                                    popUpTo("lists") { inclusive = false }
                                }
                            }
                            navigatedToLastList = true
                        }
                    }

                    NavHost(navController = navController, startDestination = "lists") {
                        composable("lists") {
                            val lists by appRepository.observeLists().collectAsState(initial = emptyList())
                            MyListsScreen(
                                lists = lists,
                                sortRepository = sortRepository,
                                appRepository = appRepository,
                                onListClick = { listId -> navController.navigate("list/$listId") },
                                onNewListClick = { navController.navigate("new_list") },
                                onSuggestionClick = { name ->
                                    navController.navigate("new_list?name=${Uri.encode(name)}")
                                }
                            )
                        }
                        composable(
                            route = "new_list?name={name}",
                            arguments = listOf(navArgument("name") {
                                type = NavType.StringType
                                defaultValue = ""
                            })
                        ) { backStackEntry ->
                            NewListScreen(
                                initialName = backStackEntry.arguments?.getString("name") ?: "",
                                onListCreated = { list ->
                                    scope.launch { appRepository.insertList(list) }
                                    navController.navigate("share_list/${list.id}")
                                }
                            )
                        }
                        composable("share_list/{listId}") { backStackEntry ->
                            val listId = backStackEntry.arguments?.getString("listId") ?: return@composable
                            ShareScreen(
                                listId = listId,
                                onSkip = {
                                    navController.navigate("list/$listId") {
                                        popUpTo("lists")
                                    }
                                }
                            )
                        }
                        composable("list/{listId}") { backStackEntry ->
                            val listId = backStackEntry.arguments?.getString("listId") ?: return@composable
                            val lists by appRepository.observeLists().collectAsState(initial = emptyList())
                            val list = lists.find { it.id == listId } ?: return@composable
                            val items by appRepository.observeItems(listId).collectAsState(initial = emptyList())

                            LaunchedEffect(listId) {
                                lastUsedRepository.saveLastListId(listId)
                            }

                            ListDetailScreen(
                                list = list,
                                items = items,
                                allLists = lists,
                                sortRepository = sortRepository,
                                appRepository = appRepository,
                                onBack = { navController.popBackStack() },
                                isDarkTheme = isDarkTheme,
                                onThemeChange = { userPrefsRepository.setDark(it) },
                                avatarPath = avatarPath,
                                onAvatarChange = { userPrefsRepository.saveAvatarPath(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
