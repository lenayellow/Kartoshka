package com.lena.kartoshka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lena.kartoshka.data.sampleItemsByList
import com.lena.kartoshka.data.sampleLists
import com.lena.kartoshka.data.sort.LocalSortRepository
import com.lena.kartoshka.ui.screens.listdetail.ListDetailScreen
import com.lena.kartoshka.ui.screens.mylists.MyListsScreen
import com.lena.kartoshka.ui.screens.newlist.NewListScreen
import com.lena.kartoshka.ui.screens.share.ShareScreen
import com.lena.kartoshka.ui.theme.KartoshkaTheme

class MainActivity : ComponentActivity() {

    private val sortRepository by lazy { LocalSortRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KartoshkaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "lists") {
                        composable("lists") {
                            MyListsScreen(
                                onListClick = { listId -> navController.navigate("list/$listId") },
                                onNewListClick = { navController.navigate("new_list") }
                            )
                        }
                        composable("new_list") {
                            NewListScreen(
                                onListCreated = { listId ->
                                    navController.navigate("share_list/$listId")
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
                            val list = sampleLists.find { it.id == listId } ?: return@composable
                            val items = sampleItemsByList[listId].orEmpty()
                            ListDetailScreen(
                                list = list,
                                items = items,
                                sortRepository = sortRepository,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
