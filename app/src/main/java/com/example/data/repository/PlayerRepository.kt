package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.FriendRecord
import com.example.data.model.GameReport
import com.example.data.model.InventoryItem
import com.example.data.model.MailItem
import com.example.data.model.MissionRecord
import com.example.data.model.PublicPlayer
import com.example.data.model.TransactionRecord
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.abs

class PlayerRepository(private val db: FirebaseFirestore) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  suspend fun ensurePlayerProfile(
    uid: String,
    displayName: String?,
    email: String?,
    photoUrl: String?,
  ): Result<UserProfile> = runCatching {
    val userRef = db.collection("users").document(uid)
    val publicRef = db.collection("public_players").document(uid)
    val snap = userRef.get().await()
    if (snap.exists()) {
      val profile = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: UserProfile(userId = uid)
      // Update online status in public_players
      publicRef.set(
        mapOf(
          "userId" to uid,
          "playerId" to profile.playerId,
          "username" to profile.username,
          "level" to profile.level,
          "rankPoints" to profile.rankPoints,
          "kills" to profile.kills,
          "wins" to profile.wins,
          "matchesPlayed" to profile.matchesPlayed,
          "equippedCharacterId" to profile.equippedCharacterId,
          "isOnline" to true,
          "isSuspended" to profile.isSuspended,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      return@runCatching profile
    }

    val cleanName = (displayName?.trim()?.take(20)?.ifBlank { null }) ?: "BiharWarrior"
    val numericSuffix = (abs(uid.hashCode()) % 900000 + 100000)
    val playerId = "FFB-$numericSuffix"

    val userPayload = mutableMapOf<String, Any>(
      "userId" to uid,
      "playerId" to playerId,
      "username" to cleanName,
      "role" to "player",
      "coins" to 1200L,
      "diamonds" to 120L,
      "level" to 1L,
      "xp" to 0L,
      "rankPoints" to 1000L,
      "kills" to 0L,
      "wins" to 0L,
      "matchesPlayed" to 0L,
      "equippedCharacterId" to "char_veer",
      "equippedWeaponId" to "wpn_ak_patna",
      "equippedOutfitId" to "outfit_obsidian_commando",
      "equippedParachuteId" to "chute_ember_wing",
      "equippedVehicleId" to "veh_thar_x",
      "equippedPetId" to "pet_cyber_falcon",
      "isSuspended" to false,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (!email.isNullOrBlank()) userPayload["email"] = email.take(140)
    if (!photoUrl.isNullOrBlank()) userPayload["photoUrl"] = photoUrl.take(480)

    val publicPayload = mapOf(
      "userId" to uid,
      "playerId" to playerId,
      "username" to cleanName,
      "level" to 1L,
      "rankPoints" to 1000L,
      "kills" to 0L,
      "wins" to 0L,
      "matchesPlayed" to 0L,
      "equippedCharacterId" to "char_veer",
      "isOnline" to true,
      "isSuspended" to false,
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    val batch = db.batch()
    batch.set(userRef, userPayload)
    batch.set(publicRef, publicPayload)

    // Starter inventory items
    val starterItems = listOf(
      Triple("char_veer", "character", "Veer (Bihar Commando)"),
      Triple("char_tara", "character", "Tara (Patna Recon)"),
      Triple("wpn_ak_patna", "weapon", "AK-Patna 47"),
      Triple("wpn_gaya_sniper", "weapon", "Gaya-X Sniper"),
      Triple("outfit_obsidian_commando", "outfit", "Obsidian Commando"),
      Triple("chute_ember_wing", "parachute", "Ember Wing"),
      Triple("veh_thar_x", "vehicle", "Sher-E-Bihar 4x4"),
      Triple("pet_cyber_falcon", "pet", "Garuda Cyber Drone"),
    )
    starterItems.forEach { (id, cat, name) ->
      batch.set(
        userRef.collection("inventory").document(id),
        mapOf(
          "itemId" to id,
          "category" to cat,
          "name" to name,
          "rarity" to "Standard",
          "level" to 1L,
          "acquiredAt" to FieldValue.serverTimestamp(),
        )
      )
    }

    // Starter daily missions
    val starterMissions = listOf(
      mapOf("missionId" to "mis_play_1", "title" to "Drop into 1 Battle Royale Match", "progress" to 0L, "target" to 1L, "rewardCoins" to 250L, "rewardDiamonds" to 15L, "claimed" to false, "updatedAt" to FieldValue.serverTimestamp()),
      mapOf("missionId" to "mis_elim_3", "title" to "Eliminate 3 Opponents", "progress" to 0L, "target" to 3L, "rewardCoins" to 400L, "rewardDiamonds" to 25L, "claimed" to false, "updatedAt" to FieldValue.serverTimestamp()),
      mapOf("missionId" to "mis_win_1", "title" to "Achieve Top 3 or Booyah Victory", "progress" to 0L, "target" to 1L, "rewardCoins" to 600L, "rewardDiamonds" to 50L, "claimed" to false, "updatedAt" to FieldValue.serverTimestamp()),
    )
    starterMissions.forEach { m ->
      batch.set(userRef.collection("missions").document(m["missionId"] as String), m)
    }

    // Welcome mailbox gift
    batch.set(
      userRef.collection("mailbox").document("welcome_gift"),
      mapOf(
        "title" to "Welcome to FREE FIRE BIHAR!",
        "body" to "Claim your Season 1 Launch Supply Drop from Commander RAVI. Gear up and dominate Patna Delta!",
        "rewardCoins" to 500L,
        "rewardDiamonds" to 50L,
        "claimed" to false,
        "isRead" to false,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.commit().await()

    userRef.get().await().toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      ?: UserProfile(userId = uid, playerId = playerId, username = cleanName)
  }.onFailure { e ->
    handleFirestoreError(e, OperationType.WRITE, "users/$uid")
  }

  fun observeProfile(uid: String): Flow<UserProfile?> = flow {
    val docRef = db.collection("users").document(uid)
    emitAll(
      docRef.snapshots()
        .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          if (auth.currentUser?.uid == uid) {
            handleFirestoreError(fsErr ?: e, OperationType.GET, docRef.path)
          }
          throw e
        }
    )
  }

  fun observeInventory(uid: String): Flow<List<InventoryItem>> = flow {
    val colRef = db.collection("users").document(uid).collection("inventory")
    emitAll(
      colRef.snapshots()
        .map { snap -> snap.toObjects(InventoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: e, OperationType.LIST, colRef.path)
          throw e
        }
    )
  }

  fun observeMissions(uid: String): Flow<List<MissionRecord>> = flow {
    val colRef = db.collection("users").document(uid).collection("missions")
    emitAll(
      colRef.snapshots()
        .map { snap -> snap.toObjects(MissionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: e, OperationType.LIST, colRef.path)
          throw e
        }
    )
  }

  fun observeMailbox(uid: String): Flow<List<MailItem>> = flow {
    val colRef = db.collection("users").document(uid).collection("mailbox")
    emitAll(
      colRef.snapshots()
        .map { snap ->
          snap.toObjects(MailItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: e, OperationType.LIST, colRef.path)
          throw e
        }
    )
  }

  fun observeTransactions(uid: String): Flow<List<TransactionRecord>> = flow {
    val colRef = db.collection("users").document(uid).collection("transactions")
    emitAll(
      colRef.snapshots()
        .map { snap ->
          snap.toObjects(TransactionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: e, OperationType.LIST, colRef.path)
          throw e
        }
    )
  }

  fun observeFriends(uid: String): Flow<List<FriendRecord>> = flow {
    val colRef = db.collection("users").document(uid).collection("friends")
    emitAll(
      colRef.snapshots()
        .map { snap -> snap.toObjects(FriendRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          if (auth.currentUser?.uid == uid) handleFirestoreError(fsErr ?: e, OperationType.LIST, colRef.path)
          throw e
        }
    )
  }

  fun observeLeaderboard(): Flow<List<PublicPlayer>> = flow {
    val query = db.collection("public_players").orderBy("rankPoints", Query.Direction.DESCENDING).limit(50)
    emitAll(
      query.snapshots()
        .map { snap -> snap.toObjects(PublicPlayer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, "public_players")
          throw e
        }
    )
  }

  suspend fun updateUsername(newUsername: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val clean = newUsername.trim().take(24)
    require(clean.length >= 2) { "Username must be at least 2 characters" }
    val batch = db.batch()
    batch.update(db.collection("users").document(uid), mapOf("username" to clean, "updatedAt" to FieldValue.serverTimestamp()))
    batch.update(db.collection("public_players").document(uid), mapOf("username" to clean, "updatedAt" to FieldValue.serverTimestamp()))
    batch.commit().await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "users") }

  suspend fun equipLoadout(
    characterId: String? = null,
    weaponId: String? = null,
    outfitId: String? = null,
    parachuteId: String? = null,
    vehicleId: String? = null,
    petId: String? = null,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
    characterId?.let { updates["equippedCharacterId"] = it }
    weaponId?.let { updates["equippedWeaponId"] = it }
    outfitId?.let { updates["equippedOutfitId"] = it }
    parachuteId?.let { updates["equippedParachuteId"] = it }
    vehicleId?.let { updates["equippedVehicleId"] = it }
    petId?.let { updates["equippedPetId"] = it }

    val batch = db.batch()
    batch.update(db.collection("users").document(uid), updates)
    if (characterId != null) {
      batch.update(
        db.collection("public_players").document(uid),
        mapOf("equippedCharacterId" to characterId, "updatedAt" to FieldValue.serverTimestamp())
      )
    }
    batch.commit().await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "users") }

  suspend fun purchaseStoreItem(
    profile: UserProfile,
    itemId: String,
    itemName: String,
    category: String,
    rarity: String,
    costCoins: Long,
    costDiamonds: Long,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(profile.coins >= costCoins) { "Not enough Gold Coins." }
    require(profile.diamonds >= costDiamonds) { "Not enough Diamonds." }

    val userRef = db.collection("users").document(uid)
    val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val batch = db.batch()
    batch.update(
      userRef,
      mapOf(
        "coins" to (profile.coins - costCoins),
        "diamonds" to (profile.diamonds - costDiamonds),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.set(
      userRef.collection("inventory").document(itemId),
      mapOf(
        "itemId" to itemId,
        "category" to category.lowercase(),
        "name" to itemName,
        "rarity" to rarity,
        "level" to 1L,
        "acquiredAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.set(
      userRef.collection("transactions").document(txId),
      mapOf(
        "txId" to txId,
        "type" to "store_purchase",
        "itemName" to itemName,
        "coinsDelta" to -costCoins,
        "diamondsDelta" to -costDiamonds,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.commit().await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.WRITE, "users") }

  suspend fun upgradeInventoryItem(profile: UserProfile, item: InventoryItem): Result<Unit> = runCatching {
    val uid = requireUserId()
    val upgradeCost = item.level * 250L
    require(profile.coins >= upgradeCost) { "Need $upgradeCost Coins to upgrade." }
    val userRef = db.collection("users").document(uid)
    val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val batch = db.batch()
    batch.update(
      userRef,
      mapOf("coins" to (profile.coins - upgradeCost), "updatedAt" to FieldValue.serverTimestamp())
    )
    batch.update(
      userRef.collection("inventory").document(item.itemId),
      mapOf("level" to (item.level + 1L))
    )
    batch.set(
      userRef.collection("transactions").document(txId),
      mapOf(
        "txId" to txId,
        "type" to "upgrade",
        "itemName" to "${item.name} Lv.${item.level + 1}",
        "coinsDelta" to -upgradeCost,
        "diamondsDelta" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.commit().await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "users") }

  suspend fun claimMissionReward(profile: UserProfile, mission: MissionRecord): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(!mission.claimed) { "Already claimed." }
    require(mission.progress >= mission.target) { "Mission not completed yet." }
    val userRef = db.collection("users").document(uid)
    val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val batch = db.batch()
    batch.update(
      userRef,
      mapOf(
        "coins" to (profile.coins + mission.rewardCoins),
        "diamonds" to (profile.diamonds + mission.rewardDiamonds),
        "xp" to (profile.xp + 150L),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.update(
      userRef.collection("missions").document(mission.missionId),
      mapOf("claimed" to true, "updatedAt" to FieldValue.serverTimestamp())
    )
    batch.set(
      userRef.collection("transactions").document(txId),
      mapOf(
        "txId" to txId,
        "type" to "mission_reward",
        "itemName" to mission.title,
        "coinsDelta" to mission.rewardCoins,
        "diamondsDelta" to mission.rewardDiamonds,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.commit().await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "users") }

  suspend fun claimMailReward(profile: UserProfile, mail: MailItem): Result<Unit> = runCatching {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val mailRef = userRef.collection("mailbox").document(mail.id)
    val batch = db.batch()
    if (!mail.claimed && (mail.rewardCoins > 0 || mail.rewardDiamonds > 0)) {
      batch.update(
        userRef,
        mapOf(
          "coins" to (profile.coins + mail.rewardCoins),
          "diamonds" to (profile.diamonds + mail.rewardDiamonds),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      val txId = "tx_${UUID.randomUUID().toString().replace("-", "").take(16)}"
      batch.set(
        userRef.collection("transactions").document(txId),
        mapOf(
          "txId" to txId,
          "type" to "mail_reward",
          "itemName" to mail.title,
          "coinsDelta" to mail.rewardCoins,
          "diamondsDelta" to mail.rewardDiamonds,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      )
    }
    batch.update(mailRef, mapOf("claimed" to true, "isRead" to true))
    batch.commit().await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "users") }

  suspend fun recordMatchResult(
    profile: UserProfile,
    killsEarned: Int,
    won: Boolean,
    placement: Int,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val pubRef = db.collection("public_players").document(uid)
    val coinsEarned = (if (won) 350L else 100L) + killsEarned * 35L
    val diamondsEarned = if (won) 20L else 0L
    val xpEarned = (if (won) 400L else 120L) + killsEarned * 50L
    val newXp = profile.xp + xpEarned
    val newLevel = 1L + (newXp / 500L)
    val rankDelta = (if (won) 45L else if (placement <= 5) 20L else 5L) + killsEarned * 8L

    val batch = db.batch()
    batch.update(
      userRef,
      mapOf(
        "coins" to (profile.coins + coinsEarned),
        "diamonds" to (profile.diamonds + diamondsEarned),
        "xp" to newXp,
        "level" to newLevel,
        "rankPoints" to (profile.rankPoints + rankDelta),
        "kills" to (profile.kills + killsEarned),
        "wins" to (profile.wins + if (won) 1L else 0L),
        "matchesPlayed" to (profile.matchesPlayed + 1L),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.update(
      pubRef,
      mapOf(
        "level" to newLevel,
        "rankPoints" to (profile.rankPoints + rankDelta),
        "kills" to (profile.kills + killsEarned),
        "wins" to (profile.wins + if (won) 1L else 0L),
        "matchesPlayed" to (profile.matchesPlayed + 1L),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )
    // Progress missions
    val misSnap = userRef.collection("missions").get().await()
    misSnap.documents.forEach { doc ->
      val mId = doc.id
      val cur = doc.getLong("progress") ?: 0L
      val target = doc.getLong("target") ?: 1L
      val next = when (mId) {
        "mis_play_1" -> (cur + 1L).coerceAtMost(target)
        "mis_elim_3" -> (cur + killsEarned).coerceAtMost(target)
        "mis_win_1" -> if (won || placement <= 3) target else cur
        else -> cur
      }
      if (next != cur) {
        batch.update(doc.reference, mapOf("progress" to next, "updatedAt" to FieldValue.serverTimestamp()))
      }
    }
    batch.commit().await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "users") }

  suspend fun findPlayerByIdOrName(queryText: String): Result<List<PublicPlayer>> = runCatching {
    val clean = queryText.trim()
    require(clean.isNotEmpty())
    val all = db.collection("public_players").limit(100).get().await()
      .toObjects(PublicPlayer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    all.filter {
      it.playerId.equals(clean, ignoreCase = true) ||
        it.username.contains(clean, ignoreCase = true)
    }
  }.onFailure { handleFirestoreError(it, OperationType.LIST, "public_players") }

  suspend fun addFriend(currentProfile: UserProfile, target: PublicPlayer): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(target.userId != uid) { "Cannot add yourself." }
    val batch = db.batch()
    batch.set(
      db.collection("users").document(uid).collection("friends").document(target.userId),
      mapOf(
        "friendUid" to target.userId,
        "friendPlayerId" to target.playerId,
        "friendUsername" to target.username,
        "status" to "pending_sent",
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.set(
      db.collection("users").document(target.userId).collection("friends").document(uid),
      mapOf(
        "friendUid" to uid,
        "friendPlayerId" to currentProfile.playerId,
        "friendUsername" to currentProfile.username,
        "status" to "pending_received",
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.commit().await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.WRITE, "friends") }

  suspend fun acceptFriend(friendUid: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val batch = db.batch()
    batch.update(
      db.collection("users").document(uid).collection("friends").document(friendUid),
      mapOf("status" to "accepted", "updatedAt" to FieldValue.serverTimestamp())
    )
    batch.update(
      db.collection("users").document(friendUid).collection("friends").document(uid),
      mapOf("status" to "accepted", "updatedAt" to FieldValue.serverTimestamp())
    )
    batch.commit().await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "friends") }

  suspend fun removeFriend(friendUid: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val batch = db.batch()
    batch.delete(db.collection("users").document(uid).collection("friends").document(friendUid))
    batch.delete(db.collection("users").document(friendUid).collection("friends").document(uid))
    batch.commit().await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.DELETE, "friends") }

  suspend fun submitReport(targetPlayerId: String, reason: String, details: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    db.collection("reports").document(reportId).set(
      mapOf(
        "reporterUid" to uid,
        "targetPlayerId" to targetPlayerId.take(60),
        "reason" to reason.take(90),
        "details" to details.take(450),
        "status" to "open",
        "createdAt" to FieldValue.serverTimestamp(),
      )
    ).await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.CREATE, "reports") }

  fun observeMyReports(uid: String): Flow<List<GameReport>> = flow {
    val query = db.collection("reports").whereEqualTo("reporterUid", uid)
    emitAll(
      query.snapshots()
        .map { snap -> snap.toObjects(GameReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, "reports")
          throw e
        }
    )
  }
}
