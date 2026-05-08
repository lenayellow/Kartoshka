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
import com.lena.kartoshka.ui.screens.listdetail.ListDetailScreen
import com.lena.kartoshka.ui.screens.mylists.MyListsScreen
import com.lena.kartoshka.ui.theme.KartoshkaTheme

class MainActivity : ComponentActivity() {
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
                                onListClick = { listId -> navController.navigate("list/$listId") }
                            )
                        }
                        composable("list/{listId}") { backStackEntry ->
                            val listId = backStackEntry.arguments?.getString("listId") ?: return@composable
                            val list = sampleLists.find { it.id == listId } ?: return@composable
                            val items = sampleItemsByList[listId].orEmpty()
                            ListDetailScreen(
                                list = list,
                                items = items,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
