package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.CatalogMap
import com.example.data.repository.CatalogRepository
import com.example.ui.GameViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BattleMatchScreen
import com.example.ui.screens.CharacterArmoryScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.MatchmakingRoomScreen
import com.example.ui.screens.OwnerAdminScreen
import com.example.ui.screens.SettingsMailboxScreen
import com.example.ui.screens.SocialLeaderboardScreen
import com.example.ui.screens.StoreEventsScreen
import com.example.ui.theme.FreeFireBiharTheme
import com.example.ui.theme.ObsidianVoid
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object AppRoutes {
  const val LOBBY = "lobby"
  const val ROOM_LOBBY = "room_lobby"
  const val BATTLE_MATCH = "battle_match/{isBotMode}/{mapId}?roomId={roomId}"
  const val ARMORY = "armory/{initialTab}"
  const val STORE = "store/{initialTab}"
  const val SOCIAL = "social/{initialTab}"
  const val SETTINGS = "settings/{initialTab}"
  const val OWNER_ADMIN = "owner_admin"

  fun battleMatch(isBotMode: Boolean, mapId: String, roomId: String? = null): String {
    val encodedMap = Uri.encode(mapId)
    val base = "battle_match/$isBotMode/$encodedMap"
    return if (roomId != null) "$base?roomId=${Uri.encode(roomId)}" else base
  }

  fun armory(tab: Int = 0) = "armory/$tab"
  fun store(tab: Int = 0) = "store/$tab"
  fun social(tab: Int = 0) = "social/$tab"
  fun settings(tab: Int = 0) = "settings/$tab"
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
    trySend(firebaseAuth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      FreeFireBiharTheme {
        val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
          initialValue = Firebase.auth.currentUser
        )
        var signedInNonce by remember { mutableIntStateOf(0) }

        val activeUser = currentUser
        if (activeUser == null) {
          AuthScreen(onAuthSuccess = { signedInNonce++ })
        } else {
          key(activeUser.uid, signedInNonce) {
            FreeFireBiharApp(uid = activeUser.uid)
          }
        }
      }
    }
  }
}

@Composable
private fun FreeFireBiharApp(uid: String) {
  val context = LocalContext.current
  val viewModel: GameViewModel = viewModel(
    key = "ffb_vm_$uid",
    factory = GameViewModel.factory(context, uid),
  )
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }
  val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
  val maps by viewModel.maps.collectAsStateWithLifecycle()
  val activeRoom by viewModel.activeRoom.collectAsStateWithLifecycle()

  LaunchedEffect(toastMessage) {
    val msg = toastMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearToast()
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize().background(ObsidianVoid),
    containerColor = ObsidianVoid,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
  ) { _ ->
    Box(modifier = Modifier.fillMaxSize()) {
      NavHost(navController = navController, startDestination = AppRoutes.LOBBY) {
        composable(AppRoutes.LOBBY) {
          LobbyScreen(
            viewModel = viewModel,
            onNavigateCharacters = { navController.navigate(AppRoutes.armory(0)) },
            onNavigateArmory = { navController.navigate(AppRoutes.armory(1)) },
            onNavigateStore = { navController.navigate(AppRoutes.store(0)) },
            onNavigateEvents = { navController.navigate(AppRoutes.store(1)) },
            onNavigateSocial = { navController.navigate(AppRoutes.social(1)) },
            onNavigateLeaderboard = { navController.navigate(AppRoutes.social(0)) },
            onNavigateProfile = { navController.navigate(AppRoutes.social(2)) },
            onNavigateMailbox = { navController.navigate(AppRoutes.settings(1)) },
            onNavigateSettings = { navController.navigate(AppRoutes.settings(0)) },
            onNavigateOwnerPanel = { navController.navigate(AppRoutes.OWNER_ADMIN) },
            onEnterRoomLobby = { navController.navigate(AppRoutes.ROOM_LOBBY) },
            onStartDirectBotTraining = { _, map ->
              navController.navigate(AppRoutes.battleMatch(isBotMode = true, mapId = map.id, roomId = null))
            },
          )
        }

        composable(AppRoutes.ROOM_LOBBY) {
          BackHandler {
            viewModel.leaveCurrentRoom()
            navController.popBackStack()
          }
          MatchmakingRoomScreen(
            viewModel = viewModel,
            onLeaveRoom = { navController.popBackStack() },
            onMatchActive = {
              val room = activeRoom
              navController.navigate(
                AppRoutes.battleMatch(
                  isBotMode = room?.fillBots == true,
                  mapId = room?.mapId ?: "map_patna_delta",
                  roomId = room?.id,
                )
              ) {
                popUpTo(AppRoutes.ROOM_LOBBY) { inclusive = true }
              }
            },
          )
        }

        composable(
          route = AppRoutes.BATTLE_MATCH,
          arguments = listOf(
            navArgument("isBotMode") { type = NavType.BoolType },
            navArgument("mapId") { type = NavType.StringType },
            navArgument("roomId") {
              type = NavType.StringType
              nullable = true
              defaultValue = null
            },
          ),
        ) { backStackEntry ->
          val isBotMode = backStackEntry.arguments?.getBoolean("isBotMode") ?: true
          val mapId = backStackEntry.arguments?.getString("mapId") ?: "map_patna_delta"
          val roomId = backStackEntry.arguments?.getString("roomId")
          val selectedMap: CatalogMap = remember(maps, mapId) {
            maps.firstOrNull { it.id == mapId } ?: CatalogRepository.defaultMaps.first()
          }
          BackHandler {
            viewModel.leaveCurrentRoom()
            navController.popBackStack(AppRoutes.LOBBY, inclusive = false)
          }
          BattleMatchScreen(
            viewModel = viewModel,
            selectedMap = selectedMap,
            isBotMode = isBotMode,
            roomId = roomId,
            onExitMatch = {
              viewModel.leaveCurrentRoom()
              navController.popBackStack(AppRoutes.LOBBY, inclusive = false)
            },
          )
        }

        composable(
          route = AppRoutes.ARMORY,
          arguments = listOf(navArgument("initialTab") { type = NavType.IntType }),
        ) { backStackEntry ->
          val tab = backStackEntry.arguments?.getInt("initialTab") ?: 0
          CharacterArmoryScreen(
            viewModel = viewModel,
            initialTab = tab,
            onBack = { navController.popBackStack() },
          )
        }

        composable(
          route = AppRoutes.STORE,
          arguments = listOf(navArgument("initialTab") { type = NavType.IntType }),
        ) { backStackEntry ->
          val tab = backStackEntry.arguments?.getInt("initialTab") ?: 0
          StoreEventsScreen(
            viewModel = viewModel,
            initialTab = tab,
            onBack = { navController.popBackStack() },
          )
        }

        composable(
          route = AppRoutes.SOCIAL,
          arguments = listOf(navArgument("initialTab") { type = NavType.IntType }),
        ) { backStackEntry ->
          val tab = backStackEntry.arguments?.getInt("initialTab") ?: 0
          SocialLeaderboardScreen(
            viewModel = viewModel,
            initialTab = tab,
            onBack = { navController.popBackStack() },
          )
        }

        composable(
          route = AppRoutes.SETTINGS,
          arguments = listOf(navArgument("initialTab") { type = NavType.IntType }),
        ) { backStackEntry ->
          val tab = backStackEntry.arguments?.getInt("initialTab") ?: 0
          SettingsMailboxScreen(
            viewModel = viewModel,
            initialTab = tab,
            onNavigateOwnerPanel = { navController.navigate(AppRoutes.OWNER_ADMIN) },
            onBack = { navController.popBackStack() },
          )
        }

        composable(AppRoutes.OWNER_ADMIN) {
          OwnerAdminScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
          )
        }
      }
    }
  }
}
