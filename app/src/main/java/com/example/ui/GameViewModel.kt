package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.audio.GameAudioEngine
import com.example.data.SettingsDataStore
import com.example.data.model.AuditLogEntry
import com.example.data.model.CatalogAudioTrack
import com.example.data.model.CatalogCharacter
import com.example.data.model.CatalogEvent
import com.example.data.model.CatalogMap
import com.example.data.model.CatalogStoreItem
import com.example.data.model.CatalogVehicle
import com.example.data.model.CatalogWeapon
import com.example.data.model.ChatMessage
import com.example.data.model.FriendRecord
import com.example.data.model.GameReport
import com.example.data.model.GlobalGameConfig
import com.example.data.model.LocalPlayerSettings
import com.example.data.model.MailItem
import com.example.data.model.MatchPlayer
import com.example.data.model.MatchRoom
import com.example.data.model.MissionRecord
import com.example.data.model.OwnerSecurityDoc
import com.example.data.model.PublicPlayer
import com.example.data.model.TransactionRecord
import com.example.data.model.UpdateHistoryEntry
import com.example.data.model.UserProfile
import com.example.data.repository.CatalogRepository
import com.example.data.repository.MatchmakingRepository
import com.example.data.repository.OwnerAdminRepository
import com.example.data.repository.PlayerRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModel(
  private val currentUserId: String,
  private val playerRepo: PlayerRepository,
  private val catalogRepo: CatalogRepository,
  private val matchRepo: MatchmakingRepository,
  private val adminRepo: OwnerAdminRepository,
  private val settingsStore: SettingsDataStore,
  val audioEngine: GameAudioEngine,
) : ViewModel() {

  private val _toastMessage = MutableStateFlow<String?>(null)
  val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

  private val _isOwnerAuthorized = MutableStateFlow(false)
  val isOwnerAuthorized: StateFlow<Boolean> = _isOwnerAuthorized.asStateFlow()

  private val _ownerSecurityDoc = MutableStateFlow<OwnerSecurityDoc?>(null)
  val ownerSecurityDoc: StateFlow<OwnerSecurityDoc?> = _ownerSecurityDoc.asStateFlow()

  private val _ownerUnlocked = MutableStateFlow(false)
  val ownerUnlocked: StateFlow<Boolean> = _ownerUnlocked.asStateFlow()

  private val _activeRoomId = MutableStateFlow<String?>(null)
  val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

  val profile: StateFlow<UserProfile?> = playerRepo.observeProfile(currentUserId)
    .safeCatch("profile", null)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val inventory = playerRepo.observeInventory(currentUserId)
    .safeCatch("inventory", emptyList())
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val missions = playerRepo.observeMissions(currentUserId)
    .safeCatch("missions", emptyList())
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val mailbox = playerRepo.observeMailbox(currentUserId)
    .safeCatch("mailbox", emptyList())
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val transactions = playerRepo.observeTransactions(currentUserId)
    .safeCatch("transactions", emptyList())
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val friends = playerRepo.observeFriends(currentUserId)
    .safeCatch("friends", emptyList())
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val leaderboard: StateFlow<List<PublicPlayer>> = playerRepo.observeLeaderboard()
    .safeCatch("leaderboard", emptyList())
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val globalConfig: StateFlow<GlobalGameConfig> = catalogRepo.observeGlobalConfig()
    .safeCatch("globalConfig", GlobalGameConfig())
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), GlobalGameConfig())

  val characters: StateFlow<List<CatalogCharacter>> = catalogRepo.observeCharacters()
    .safeCatch("characters", CatalogRepository.defaultCharacters)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CatalogRepository.defaultCharacters)

  val weapons: StateFlow<List<CatalogWeapon>> = catalogRepo.observeWeapons()
    .safeCatch("weapons", CatalogRepository.defaultWeapons)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CatalogRepository.defaultWeapons)

  val maps: StateFlow<List<CatalogMap>> = catalogRepo.observeMaps()
    .safeCatch("maps", CatalogRepository.defaultMaps)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CatalogRepository.defaultMaps)

  val vehicles: StateFlow<List<CatalogVehicle>> = catalogRepo.observeVehicles()
    .safeCatch("vehicles", CatalogRepository.defaultVehicles)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CatalogRepository.defaultVehicles)

  val events: StateFlow<List<CatalogEvent>> = catalogRepo.observeEvents()
    .safeCatch("events", CatalogRepository.defaultEvents)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CatalogRepository.defaultEvents)

  val storeItems: StateFlow<List<CatalogStoreItem>> = catalogRepo.observeStoreItems()
    .safeCatch("storeItems", CatalogRepository.defaultStoreItems)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CatalogRepository.defaultStoreItems)

  val audioTracks: StateFlow<List<CatalogAudioTrack>> = catalogRepo.observeAudioTracks()
    .safeCatch("audioTracks", CatalogRepository.defaultAudioTracks)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CatalogRepository.defaultAudioTracks)

  val availableRooms: StateFlow<List<MatchRoom>> = matchRepo.observeAvailableRooms()
    .safeCatch("availableRooms", emptyList())
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val activeRoom: StateFlow<MatchRoom?> = _activeRoomId.flatMapLatest { id ->
    if (id == null) flowOf(null) else matchRepo.observeRoom(id).safeCatch("activeRoom", null)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

  val activeRoomPlayers: StateFlow<List<MatchPlayer>> = _activeRoomId.flatMapLatest { id ->
    if (id == null) flowOf(emptyList()) else matchRepo.observeRoomPlayers(id).safeCatch("activeRoomPlayers", emptyList())
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val activeRoomChat: StateFlow<List<ChatMessage>> = _activeRoomId.flatMapLatest { id ->
    if (id == null) flowOf(emptyList()) else matchRepo.observeRoomChat(id).safeCatch("activeRoomChat", emptyList())
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val settings: StateFlow<LocalPlayerSettings> = settingsStore.settingsFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalPlayerSettings())

  // Owner-only streams (activated only when ownerUnlocked is true)
  val allPlayersForOwner: StateFlow<List<UserProfile>> = _ownerUnlocked.flatMapLatest { unlocked ->
    if (!unlocked) flowOf(emptyList()) else adminRepo.observeAllPlayers().safeCatch("allPlayers", emptyList())
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val allReportsForOwner: StateFlow<List<GameReport>> = _ownerUnlocked.flatMapLatest { unlocked ->
    if (!unlocked) flowOf(emptyList()) else adminRepo.observeAllReports().safeCatch("allReports", emptyList())
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val updateHistory: StateFlow<List<UpdateHistoryEntry>> = _ownerUnlocked.flatMapLatest { unlocked ->
    if (!unlocked) flowOf(emptyList()) else adminRepo.observeUpdateHistory().safeCatch("updateHistory", emptyList())
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val auditLogs: StateFlow<List<AuditLogEntry>> = _ownerUnlocked.flatMapLatest { unlocked ->
    if (!unlocked) flowOf(emptyList()) else adminRepo.observeAuditLogs().safeCatch("auditLogs", emptyList())
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  init {
    viewModelScope.launch {
      val user = Firebase.auth.currentUser
      playerRepo.ensurePlayerProfile(
        uid = currentUserId,
        displayName = user?.displayName,
        email = user?.email,
        photoUrl = user?.photoUrl?.toString(),
      )
      _isOwnerAuthorized.value = adminRepo.checkAdminAccess()
    }
    viewModelScope.launch {
      settings.collect { s ->
        audioEngine.settings = s
      }
    }
  }

  fun clearToast() {
    _toastMessage.value = null
  }

  fun showMessage(msg: String) {
    _toastMessage.value = msg
  }

  fun playLobbyMusic() {
    val cfg = globalConfig.value
    val track = audioTracks.value.firstOrNull { it.id == cfg.activeLobbyMusicId }
      ?: audioTracks.value.firstOrNull { it.slot == "lobby_bgm" }
      ?: CatalogRepository.defaultAudioTracks.first()
    audioEngine.startBgm(track)
  }

  fun playMatchMusic() {
    val cfg = globalConfig.value
    val track = audioTracks.value.firstOrNull { it.id == cfg.activeMatchMusicId }
      ?: audioTracks.value.firstOrNull { it.slot == "match_bgm" }
      ?: CatalogRepository.defaultAudioTracks[1]
    audioEngine.startBgm(track)
  }

  fun playOutcomeMusic(won: Boolean) {
    val slot = if (won) "victory_bgm" else "defeat_bgm"
    val track = audioTracks.value.firstOrNull { it.slot == slot }
      ?: CatalogRepository.defaultAudioTracks.first { it.slot == slot }
    audioEngine.startBgm(track)
  }

  fun updateSettings(newSettings: LocalPlayerSettings) {
    viewModelScope.launch {
      settingsStore.updateSettings(newSettings)
    }
  }

  fun updateUsername(newName: String) {
    viewModelScope.launch {
      playerRepo.updateUsername(newName)
        .onSuccess { showMessage("Callsign updated to $newName") }
        .onFailure { showMessage(it.message ?: "Failed to update callsign") }
    }
  }

  fun equipLoadout(
    characterId: String? = null,
    weaponId: String? = null,
    outfitId: String? = null,
    parachuteId: String? = null,
    vehicleId: String? = null,
    petId: String? = null,
  ) {
    viewModelScope.launch {
      playerRepo.equipLoadout(characterId, weaponId, outfitId, parachuteId, vehicleId, petId)
        .onSuccess { showMessage("Loadout equipped!") }
        .onFailure { showMessage(it.message ?: "Equip failed") }
    }
  }

  fun purchaseStoreItem(item: CatalogStoreItem) {
    val p = profile.value ?: return
    viewModelScope.launch {
      playerRepo.purchaseStoreItem(
        profile = p,
        itemId = item.id,
        itemName = item.name,
        category = item.category,
        rarity = item.rarity,
        costCoins = item.priceCoins,
        costDiamonds = item.priceDiamonds,
      ).onSuccess { showMessage("Unlocked ${item.name}!") }
        .onFailure { showMessage(it.message ?: "Purchase failed") }
    }
  }

  fun purchaseCharacter(char: CatalogCharacter) {
    val p = profile.value ?: return
    viewModelScope.launch {
      playerRepo.purchaseStoreItem(
        profile = p,
        itemId = char.id,
        itemName = char.name,
        category = "character",
        rarity = "Hero",
        costCoins = char.priceCoins,
        costDiamonds = char.priceDiamonds,
      ).onSuccess {
        playerRepo.equipLoadout(characterId = char.id)
        showMessage("Recruited ${char.name}!")
      }.onFailure { showMessage(it.message ?: "Unlock failed") }
    }
  }

  fun upgradeInventoryItem(item: com.example.data.model.InventoryItem) {
    val p = profile.value ?: return
    viewModelScope.launch {
      playerRepo.upgradeInventoryItem(p, item)
        .onSuccess { showMessage("${item.name} upgraded to Lv.${item.level + 1}!") }
        .onFailure { showMessage(it.message ?: "Upgrade failed") }
    }
  }

  fun claimMission(mission: MissionRecord) {
    val p = profile.value ?: return
    viewModelScope.launch {
      playerRepo.claimMissionReward(p, mission)
        .onSuccess { showMessage("Claimed +${mission.rewardCoins} Coins, +${mission.rewardDiamonds} Diamonds!") }
        .onFailure { showMessage(it.message ?: "Claim failed") }
    }
  }

  fun claimMail(mail: MailItem) {
    val p = profile.value ?: return
    viewModelScope.launch {
      playerRepo.claimMailReward(p, mail)
        .onSuccess { showMessage("Supply drop collected!") }
        .onFailure { showMessage(it.message ?: "Failed to claim mail") }
    }
  }

  fun searchPlayers(query: String, onResult: (List<PublicPlayer>) -> Unit) {
    viewModelScope.launch {
      playerRepo.findPlayerByIdOrName(query)
        .onSuccess(onResult)
        .onFailure { showMessage(it.message ?: "Search failed") }
    }
  }

  fun sendFriendRequest(target: PublicPlayer) {
    val p = profile.value ?: return
    viewModelScope.launch {
      playerRepo.addFriend(p, target)
        .onSuccess { showMessage("Friend invite sent to ${target.username}") }
        .onFailure { showMessage(it.message ?: "Invite failed") }
    }
  }

  fun acceptFriend(friendUid: String) {
    viewModelScope.launch {
      playerRepo.acceptFriend(friendUid)
        .onSuccess { showMessage("Friend added to squad roster!") }
    }
  }

  fun removeFriend(friendUid: String) {
    viewModelScope.launch {
      playerRepo.removeFriend(friendUid)
        .onSuccess { showMessage("Removed from friends") }
    }
  }

  fun submitReport(targetPlayerId: String, reason: String, details: String) {
    viewModelScope.launch {
      playerRepo.submitReport(targetPlayerId, reason, details)
        .onSuccess { showMessage("Report submitted to Owner RAVI security desk.") }
        .onFailure { showMessage(it.message ?: "Report failed") }
    }
  }

  // --- MULTIPLAYER ROOMS ---
  fun createMatchRoom(
    mode: String,
    map: CatalogMap,
    isPrivate: Boolean,
    password: String,
    fillBots: Boolean,
    onRoomCreated: (String) -> Unit,
  ) {
    val p = profile.value ?: return
    viewModelScope.launch {
      matchRepo.createRoom(
        profile = p,
        mode = mode,
        mapId = map.id,
        mapName = map.name,
        isPrivate = isPrivate,
        passwordPlain = password,
        maxPlayers = globalConfig.value.maxPlayersPerMatch.coerceIn(2L, 50L),
        fillBots = fillBots,
      ).onSuccess { roomId ->
        _activeRoomId.value = roomId
        onRoomCreated(roomId)
      }.onFailure { showMessage(it.message ?: "Could not create room") }
    }
  }

  fun joinMatchRoom(room: MatchRoom, password: String = "", onJoined: () -> Unit) {
    val p = profile.value ?: return
    viewModelScope.launch {
      matchRepo.joinRoom(room, p, password)
        .onSuccess {
          _activeRoomId.value = room.id
          onJoined()
        }
        .onFailure { showMessage(it.message ?: "Failed to join room") }
    }
  }

  fun leaveCurrentRoom() {
    val room = activeRoom.value
    _activeRoomId.value = null
    if (room != null) {
      viewModelScope.launch { matchRepo.leaveRoom(room) }
    }
  }

  fun launchRoomMatch(roomId: String) {
    viewModelScope.launch {
      matchRepo.startMatch(roomId)
        .onFailure { showMessage(it.message ?: "Could not start match") }
    }
  }

  fun sendRoomChat(text: String) {
    val roomId = _activeRoomId.value ?: return
    val name = profile.value?.username ?: "Warrior"
    viewModelScope.launch {
      matchRepo.sendChatMessage(roomId, name, text)
    }
  }

  fun syncPlayerInMatch(
    roomId: String,
    x: Double,
    y: Double,
    z: Double,
    yaw: Double,
    hp: Int,
    armor: Int,
    kills: Int,
    isAlive: Boolean,
    isCrouching: Boolean,
    isParachuting: Boolean,
    vehicleId: String,
  ) {
    val p = profile.value ?: return
    viewModelScope.launch {
      matchRepo.syncPlayerTransform(roomId, p, x, y, z, yaw, hp, armor, kills, isAlive, isCrouching, isParachuting, vehicleId)
    }
  }

  fun completeMatch(roomId: String?, kills: Int, won: Boolean, placement: Int) {
    val p = profile.value ?: return
    viewModelScope.launch {
      playerRepo.recordMatchResult(p, kills, won, placement)
      if (roomId != null && won) {
        matchRepo.finishMatch(roomId, p.username)
      }
    }
  }

  // --- OWNER SECURITY & ADMIN ACTIONS ---
  fun loadOwnerSecurity() {
    viewModelScope.launch {
      adminRepo.getOrInitOwnerSecurity()
        .onSuccess { _ownerSecurityDoc.value = it }
        .onFailure { showMessage("Owner authorization check failed") }
    }
  }

  fun unlockOwnerPanel(
    usernameInput: String,
    passkeyInput: String,
    pin2faInput: String,
    newPasskeyOnFirstLogin: String,
    newPinOnFirstLogin: String,
  ) {
    val sec = _ownerSecurityDoc.value
    if (!usernameInput.trim().equals("RAVI", ignoreCase = false)) {
      showMessage("Invalid Owner username.")
      return
    }
    if (sec == null) {
      showMessage("Security record not loaded.")
      return
    }
    if (sec.mustChangeInitial) {
      if (newPasskeyOnFirstLogin.length < 6 || newPinOnFirstLogin.length != 6) {
        showMessage("First login requires setting a 6+ char Owner Passkey and 6-digit 2FA PIN.")
        return
      }
      viewModelScope.launch {
        adminRepo.configureOwnerCredentials(newPasskeyOnFirstLogin, newPinOnFirstLogin)
          .onSuccess {
            _ownerUnlocked.value = true
            loadOwnerSecurity()
            showMessage("Owner security initialized. Welcome, Commander RAVI!")
          }
          .onFailure { showMessage(it.message ?: "Failed to save Owner credentials") }
      }
    } else {
      val matchesPass = adminRepo.hashPasskey(passkeyInput) == sec.passkeyHash
      val matches2fa = pin2faInput == sec.pin2fa
      if (matchesPass && matches2fa) {
        _ownerUnlocked.value = true
        showMessage("Owner Panel Unlocked (2FA Verified)")
      } else {
        viewModelScope.launch { adminRepo.recordFailedOwnerLogin(sec.failedAttempts) }
        showMessage("Invalid Owner Passkey or 2FA PIN.")
      }
    }
  }

  fun lockOwnerPanel() {
    _ownerUnlocked.value = false
  }

  fun adminSaveGlobalConfig(cfg: GlobalGameConfig) {
    viewModelScope.launch {
      adminRepo.saveGlobalConfig(cfg)
        .onSuccess { showMessage("Global server settings updated!") }
        .onFailure { showMessage(it.message ?: "Failed") }
    }
  }

  fun adminSaveCharacter(c: CatalogCharacter) {
    viewModelScope.launch {
      adminRepo.saveCharacter(c)
        .onSuccess { showMessage("Character ${c.name} published!") }
        .onFailure { showMessage(it.message ?: "Failed") }
    }
  }

  fun adminSaveWeapon(w: CatalogWeapon) {
    viewModelScope.launch {
      adminRepo.saveWeapon(w)
        .onSuccess { showMessage("Weapon ${w.name} published!") }
        .onFailure { showMessage(it.message ?: "Failed") }
    }
  }

  fun adminSaveMap(m: CatalogMap) {
    viewModelScope.launch {
      adminRepo.saveMap(m)
        .onSuccess { showMessage("Map ${m.name} published!") }
        .onFailure { showMessage(it.message ?: "Failed") }
    }
  }

  fun adminSaveVehicle(v: CatalogVehicle) {
    viewModelScope.launch {
      adminRepo.saveVehicle(v)
        .onSuccess { showMessage("Vehicle ${v.name} published!") }
        .onFailure { showMessage(it.message ?: "Failed") }
    }
  }

  fun adminSaveEvent(e: CatalogEvent) {
    viewModelScope.launch {
      adminRepo.saveEvent(e)
        .onSuccess { showMessage("Event ${e.name} published!") }
        .onFailure { showMessage(it.message ?: "Failed") }
    }
  }

  fun adminSaveStoreItem(s: CatalogStoreItem) {
    viewModelScope.launch {
      adminRepo.saveStoreItem(s)
        .onSuccess { showMessage("Store item ${s.name} published!") }
        .onFailure { showMessage(it.message ?: "Failed") }
    }
  }

  fun adminSaveAudioTrack(a: CatalogAudioTrack) {
    viewModelScope.launch {
      adminRepo.saveAudioTrack(a)
        .onSuccess { showMessage("Audio track ${a.name} updated!") }
        .onFailure { showMessage(it.message ?: "Failed") }
    }
  }

  fun adminDeleteCatalog(collection: String, id: String) {
    viewModelScope.launch {
      adminRepo.deleteCatalogItem(collection, id)
        .onSuccess { showMessage("Removed item $id") }
        .onFailure { showMessage(it.message ?: "Delete failed") }
    }
  }

  fun adminToggleSuspension(targetUid: String, suspend: Boolean, reason: String) {
    viewModelScope.launch {
      adminRepo.togglePlayerSuspension(targetUid, suspend, reason)
        .onSuccess { showMessage(if (suspend) "Account suspended" else "Account restored") }
        .onFailure { showMessage(it.message ?: "Action failed") }
    }
  }

  fun adminGrantReward(target: UserProfile, coins: Long, diamonds: Long, title: String) {
    viewModelScope.launch {
      adminRepo.grantRewardToPlayer(target, coins, diamonds, title)
        .onSuccess { showMessage("Reward sent to ${target.username}!") }
        .onFailure { showMessage(it.message ?: "Reward failed") }
    }
  }

  fun adminResolveReport(reportId: String, status: String) {
    viewModelScope.launch {
      adminRepo.resolveReport(reportId, status)
        .onSuccess { showMessage("Report marked $status") }
    }
  }

  fun adminPublishUpdate(version: String, title: String, notes: String) {
    viewModelScope.launch {
      adminRepo.publishGameUpdate(version, title, notes)
        .onSuccess { showMessage("Published v$version live to all players!") }
        .onFailure { showMessage(it.message ?: "Publish failed") }
    }
  }

  fun adminRollbackUpdate(entry: UpdateHistoryEntry) {
    viewModelScope.launch {
      adminRepo.rollbackUpdate(entry)
        .onSuccess { showMessage("Rolled back ${entry.version}") }
        .onFailure { showMessage(it.message ?: "Rollback failed") }
    }
  }

  override fun onCleared() {
    super.onCleared()
    audioEngine.release()
  }

  private fun <T> Flow<T>.safeCatch(tag: String, fallback: T): Flow<T> = catch { error ->
    val fsErr = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (fsErr == null && error is CancellationException) throw error
    Log.w("GameViewModel", "Stream error in $tag", fsErr ?: error)
    emit(fallback)
  }

  companion object {
    fun factory(context: Context, currentUserId: String): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          val appCtx = context.applicationContext
          val dbId = appCtx.getString(R.string.firestore_database_id)
          val db = FirebaseFirestore.getInstance(dbId)
          return GameViewModel(
            currentUserId = currentUserId,
            playerRepo = PlayerRepository(db),
            catalogRepo = CatalogRepository(db),
            matchRepo = MatchmakingRepository(db),
            adminRepo = OwnerAdminRepository(db),
            settingsStore = SettingsDataStore(appCtx),
            audioEngine = GameAudioEngine(appCtx),
          ) as T
        }
      }
  }
}
