package com.lena.kartoshka

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import com.lena.kartoshka.analytics.Analytics
import com.lena.kartoshka.data.AppRepository
import com.lena.kartoshka.data.LastUsedRepository
import com.lena.kartoshka.data.SyncRepository
import com.lena.kartoshka.data.TokenStore
import com.lena.kartoshka.data.UserPrefsRepository
import com.lena.kartoshka.data.db.KartoshkaDatabase
import com.lena.kartoshka.network.ApiClient
import com.lena.kartoshka.network.LogoutRequest
import com.lena.kartoshka.data.sort.LocalSortRepository
import com.lena.kartoshka.ui.screens.auth.AuthScreen
import com.lena.kartoshka.ui.screens.auth.AuthViewModel
import com.lena.kartoshka.ui.screens.listdetail.ListDetailScreen
import com.lena.kartoshka.ui.screens.mylists.MyListsScreen
import com.lena.kartoshka.ui.screens.newlist.NewListScreen
import com.lena.kartoshka.ui.screens.share.ShareScreen
import com.lena.kartoshka.data.ThemeMode
import com.lena.kartoshka.ui.theme.SuperListsTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val sortRepository by lazy { LocalSortRepository(applicationContext) }
    private val lastUsedRepository by lazy { LastUsedRepository(applicationContext) }
    private val appRepository by lazy { AppRepository(KartoshkaDatabase.get(applicationContext), ApiClient.api, applicationContext) }
    private val userPrefsRepository by lazy { UserPrefsRepository(applicationContext) }
    private val tokenStore by lazy {
        TokenStore(applicationContext).also { ApiClient.init(it) }
    }
    private val syncRepository by lazy {
        SyncRepository(ApiClient.api, KartoshkaDatabase.get(applicationContext))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        when {
            uri.scheme == "kartoshka" && uri.host == "auth" ->
                uri.getQueryParameter("code")?.let { YandexAuthBus.code.value = it }
            uri.scheme == "kartoshka" && uri.host == "invite" -> {
                val token = uri.pathSegments.firstOrNull() ?: return
                userPrefsRepository.setPendingInviteToken(token)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        tokenStore // triggers lazy init → ApiClient.init()
        enableEdgeToEdge()
        setContent {
            val themeMode by userPrefsRepository.themeMode.collectAsState()
            val avatarPath by userPrefsRepository.avatarPath.collectAsState()
            val userName by userPrefsRepository.userName.collectAsState()
            val userEmail by userPrefsRepository.userEmail.collectAsState()
            SuperListsTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val scope = rememberCoroutineScope()

                    val lastListId by lastUsedRepository.observeLastListId().collectAsState(initial = "")
                    var navigatedToLastList by remember { mutableStateOf(false) }

                    // Seed only when not logged in; sync from server when logged in
                    LaunchedEffect(Unit) {
                        if (tokenStore.isLoggedIn) {
                            syncRepository.syncLists()
                            appRepository.getCurrentUser()?.let {
                                userPrefsRepository.saveUserInfo(it.name, it.email)
                            }
                            userPrefsRepository.getPendingInviteToken()?.let { token ->
                                userPrefsRepository.setPendingInviteToken(null)
                                runCatching { ApiClient.api.acceptInvite(token) }
                                    .onSuccess { resp ->
                                        navController.navigate("list/${resp.list_id}") {
                                            popUpTo("lists") { inclusive = false }
                                        }
                                    }
                            }
                        } else {
                            appRepository.seedIfEmpty()
                        }
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

                    LaunchedEffect(Unit) {
                        tokenStore.logoutEvents.collect { reason ->
                            when (reason) {
                                TokenStore.LogoutReason.SESSION_EXPIRED -> {
                                    Toast.makeText(
                                        this@MainActivity,
                                        this@MainActivity.getString(R.string.error_session_expired),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    Analytics.trackEvent(
                                        "force_logout",
                                        mapOf("reason" to reason.name)
                                    )
                                    navController.navigate("auth") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                                TokenStore.LogoutReason.USER_INITIATED -> {
                                    Analytics.trackEvent(
                                        "force_logout",
                                        mapOf("reason" to reason.name)
                                    )
                                }
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = if (tokenStore.isLoggedIn) "lists" else "auth"
                    ) {
                        composable("auth") {
                            val vm: AuthViewModel = viewModel(
                                factory = AuthViewModel.Factory(ApiClient.api, tokenStore)
                            )
                            AuthScreen(vm = vm, onSuccess = { name, email ->
                                if (name.isNotEmpty() || email.isNotEmpty()) {
                                    userPrefsRepository.saveUserInfo(name, email)
                                }
                                scope.launch {
                                    syncRepository.syncLists()
                                    val pendingToken = userPrefsRepository.getPendingInviteToken()
                                    if (pendingToken != null) {
                                        userPrefsRepository.setPendingInviteToken(null)
                                        runCatching { ApiClient.api.acceptInvite(pendingToken) }
                                            .onSuccess { resp ->
                                                navController.navigate("list/${resp.list_id}") {
                                                    popUpTo("auth") { inclusive = true }
                                                }
                                                return@launch
                                            }
                                    }
                                    navController.navigate("lists") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            })
                        }
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
                                },
                                syncRepository = syncRepository
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
                                api = ApiClient.api,
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
                                if (tokenStore.isLoggedIn) syncRepository.syncItems(listId)
                            }

                            ListDetailScreen(
                                list = list,
                                items = items,
                                allLists = lists,
                                sortRepository = sortRepository,
                                appRepository = appRepository,
                                onBack = { navController.popBackStack() },
                                themeMode = themeMode,
                                onThemeChange = { userPrefsRepository.setThemeMode(it) },
                                avatarPath = avatarPath,
                                onAvatarChange = { userPrefsRepository.saveAvatarPath(it) },
                                userName = userName,
                                userEmail = userEmail,
                                onLogout = {
                                    scope.launch {
                                        runCatching {
                                            val rt = tokenStore.refreshToken
                                            if (rt != null) ApiClient.api.logout(LogoutRequest(rt))
                                        }
                                        Analytics.setUserId(null)
                                        tokenStore.clearWithReason(TokenStore.LogoutReason.USER_INITIATED)
                                        userPrefsRepository.clearUserInfo()
                                        navController.navigate("auth") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                },
                                syncRepository = syncRepository
                            )
                        }
                    }
                }
            }
        }
    }
}
