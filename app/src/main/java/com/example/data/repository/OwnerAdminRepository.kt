package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.AuditLogEntry
import com.example.data.model.CatalogAudioTrack
import com.example.data.model.CatalogCharacter
import com.example.data.model.CatalogEvent
import com.example.data.model.CatalogMap
import com.example.data.model.CatalogStoreItem
import com.example.data.model.CatalogVehicle
import com.example.data.model.CatalogWeapon
import com.example.data.model.GameReport
import com.example.data.model.GlobalGameConfig
import com.example.data.model.OwnerSecurityDoc
import com.example.data.model.UpdateHistoryEntry
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class OwnerAdminRepository(private val db: FirebaseFirestore) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("Owner must be signed in.")
  }

  fun hashPasskey(passkey: String): String {
    val salted = "FFB_RAVI_SALT_2026::$passkey"
    return MessageDigest.getInstance("SHA-256").digest(salted.toByteArray())
      .joinToString("") { "%02x".format(it) }
  }

  suspend fun checkAdminAccess(): Boolean {
    val user = auth.currentUser ?: return false
    return try {
      // Attempt reading owner_security/{uid} - Firestore Security Rules enforce isAdmin() server-side!
      db.collection("owner_security").document(user.uid).get().await()
      true
    } catch (_: Exception) {
      user.email.equals("mithu8084087074@gmail.com", ignoreCase = true) && user.isEmailVerified
    }
  }

  suspend fun getOrInitOwnerSecurity(): Result<OwnerSecurityDoc> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("owner_security").document(uid)
    val snap = ref.get().await()
    if (snap.exists()) {
      snap.toObject(OwnerSecurityDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: OwnerSecurityDoc(id = uid)
    } else {
      val initial = mapOf(
        "name" to "RAVI",
        "passkeyHash" to "",
        "pin2fa" to "202601",
        "mustChangeInitial" to true,
        "failedAttempts" to 0L,
        "enabled" to true,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      ref.set(initial).await()
      OwnerSecurityDoc(id = uid, name = "RAVI", passkeyHash = "", pin2fa = "202601", mustChangeInitial = true)
    }
  }.onFailure { handleFirestoreError(it, OperationType.GET, "owner_security") }

  suspend fun configureOwnerCredentials(newPasskey: String, newPin2fa: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(newPasskey.length >= 6) { "Passkey must be at least 6 characters." }
    require(newPin2fa.length == 6 && newPin2fa.all { it.isDigit() }) { "2FA PIN must be 6 digits." }
    db.collection("owner_security").document(uid).set(
      mapOf(
        "name" to "RAVI",
        "passkeyHash" to hashPasskey(newPasskey),
        "pin2fa" to newPin2fa,
        "mustChangeInitial" to false,
        "failedAttempts" to 0L,
        "enabled" to true,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    ).await()
    writeAuditLog("OWNER_CREDENTIALS_UPDATED", "Updated Owner RAVI passkey & 2FA PIN.")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.WRITE, "owner_security") }

  suspend fun recordFailedOwnerLogin(currentAttempts: Long) {
    val uid = auth.currentUser?.uid ?: return
    try {
      db.collection("owner_security").document(uid).update(
        mapOf("failedAttempts" to (currentAttempts + 1L), "updatedAt" to FieldValue.serverTimestamp())
      ).await()
      writeAuditLog("OWNER_LOGIN_FAILED", "Failed Owner gate attempt #${currentAttempts + 1}")
    } catch (_: Exception) {}
  }

  fun observeAllPlayers(): Flow<List<UserProfile>> = flow {
    val col = db.collection("users")
    emitAll(
      col.snapshots()
        .map { snap -> snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
          throw e
        }
    )
  }

  fun observeAllReports(): Flow<List<GameReport>> = flow {
    val col = db.collection("reports")
    emitAll(
      col.snapshots()
        .map { snap -> snap.toObjects(GameReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
          throw e
        }
    )
  }

  fun observeUpdateHistory(): Flow<List<UpdateHistoryEntry>> = flow {
    val col = db.collection("update_history")
    emitAll(
      col.snapshots()
        .map { snap ->
          snap.toObjects(UpdateHistoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt?.seconds ?: 0L }
        }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
          throw e
        }
    )
  }

  fun observeAuditLogs(): Flow<List<AuditLogEntry>> = flow {
    val col = db.collection("audit_logs")
    emitAll(
      col.snapshots()
        .map { snap ->
          snap.toObjects(AuditLogEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.updatedAt?.seconds ?: 0L }
        }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
          throw e
        }
    )
  }

  suspend fun saveGlobalConfig(config: GlobalGameConfig): Result<Unit> = runCatching {
    requireUserId()
    db.collection("game_config").document("global").set(
      mapOf(
        "name" to config.name,
        "maintenanceMode" to config.maintenanceMode,
        "maintenanceMessage" to config.maintenanceMessage,
        "announcement" to config.announcement,
        "maxPlayersPerMatch" to config.maxPlayersPerMatch,
        "enabledModes" to config.enabledModes,
        "activeLobbyMusicId" to config.activeLobbyMusicId,
        "activeMatchMusicId" to config.activeMatchMusicId,
        "lobbyThemeAccent" to config.lobbyThemeAccent,
        "serverVersion" to config.serverVersion,
        "enabled" to config.enabled,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    ).await()
    writeAuditLog("UPDATE_GLOBAL_CONFIG", "Maintenance=${config.maintenanceMode}, Version=${config.serverVersion}")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.WRITE, "game_config/global") }

  suspend fun saveCharacter(c: CatalogCharacter): Result<Unit> = runCatching {
    requireUserId()
    val id = c.id.ifBlank { "char_${UUID.randomUUID().toString().replace("-", "").take(10)}" }
    db.collection("catalog_characters").document(id).set(
      mapOf(
        "name" to c.name,
        "title" to c.title,
        "gender" to c.gender,
        "skillName" to c.skillName,
        "description" to c.description,
        "primaryColorHex" to c.primaryColorHex,
        "accentColorHex" to c.accentColorHex,
        "hairStyle" to c.hairStyle,
        "outfitStyle" to c.outfitStyle,
        "priceCoins" to c.priceCoins,
        "priceDiamonds" to c.priceDiamonds,
        "enabled" to c.enabled,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    ).await()
    writeAuditLog("SAVE_CHARACTER", "Saved character ${c.name} ($id)")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.WRITE, "catalog_characters") }

  suspend fun deleteCatalogItem(collection: String, id: String): Result<Unit> = runCatching {
    requireUserId()
    db.collection(collection).document(id).delete().await()
    writeAuditLog("DELETE_CATALOG", "Deleted $id from $collection")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.DELETE, "$collection/$id") }

  suspend fun saveWeapon(w: CatalogWeapon): Result<Unit> = runCatching {
    requireUserId()
    val id = w.id.ifBlank { "wpn_${UUID.randomUUID().toString().replace("-", "").take(10)}" }
    db.collection("catalog_weapons").document(id).set(
      mapOf(
        "name" to w.name,
        "category" to w.category,
        "damage" to w.damage,
        "rangeMeters" to w.rangeMeters,
        "fireRateMs" to w.fireRateMs,
        "magazineSize" to w.magazineSize,
        "reloadTimeMs" to w.reloadTimeMs,
        "recoil" to w.recoil,
        "colorHex" to w.colorHex,
        "attachments" to w.attachments,
        "enabled" to w.enabled,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    ).await()
    writeAuditLog("SAVE_WEAPON", "Saved weapon ${w.name} (DMG=${w.damage})")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.WRITE, "catalog_weapons") }

  suspend fun saveMap(m: CatalogMap): Result<Unit> = runCatching {
    requireUserId()
    val id = m.id.ifBlank { "map_${UUID.randomUUID().toString().replace("-", "").take(10)}" }
    db.collection("catalog_maps").document(id).set(
      mapOf(
        "name" to m.name,
        "biome" to m.biome,
        "landmarks" to m.landmarks,
        "maxPlayers" to m.maxPlayers,
        "groundColorHex" to m.groundColorHex,
        "skyColorHex" to m.skyColorHex,
        "description" to m.description,
        "enabled" to m.enabled,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    ).await()
    writeAuditLog("SAVE_MAP", "Saved map ${m.name} (enabled=${m.enabled})")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.WRITE, "catalog_maps") }

  suspend fun saveVehicle(v: CatalogVehicle): Result<Unit> = runCatching {
    requireUserId()
    val id = v.id.ifBlank { "veh_${UUID.randomUUID().toString().replace("-", "").take(10)}" }
    db.collection("catalog_vehicles").document(id).set(
      mapOf(
        "name" to v.name,
        "vehicleType" to v.vehicleType,
        "topSpeed" to v.topSpeed,
        "maxHp" to v.maxHp,
        "seats" to v.seats,
        "colorHex" to v.colorHex,
        "enabled" to v.enabled,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    ).await()
    writeAuditLog("SAVE_VEHICLE", "Saved vehicle ${v.name}")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.WRITE, "catalog_vehicles") }

  suspend fun saveEvent(e: CatalogEvent): Result<Unit> = runCatching {
    requireUserId()
    val id = e.id.ifBlank { "evt_${UUID.randomUUID().toString().replace("-", "").take(10)}" }
    db.collection("catalog_events").document(id).set(
      mapOf(
        "name" to e.name,
        "eventType" to e.eventType,
        "description" to e.description,
        "rewardSummary" to e.rewardSummary,
        "enabled" to e.enabled,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    ).await()
    writeAuditLog("SAVE_EVENT", "Published event ${e.name}")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.WRITE, "catalog_events") }

  suspend fun saveStoreItem(s: CatalogStoreItem): Result<Unit> = runCatching {
    requireUserId()
    val id = s.id.ifBlank { "skin_${UUID.randomUUID().toString().replace("-", "").take(10)}" }
    db.collection("catalog_store").document(id).set(
      mapOf(
        "name" to s.name,
        "category" to s.category,
        "rarity" to s.rarity,
        "priceCoins" to s.priceCoins,
        "priceDiamonds" to s.priceDiamonds,
        "description" to s.description,
        "colorHex" to s.colorHex,
        "enabled" to s.enabled,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    ).await()
    writeAuditLog("SAVE_STORE_ITEM", "Saved store item ${s.name}")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.WRITE, "catalog_store") }

  suspend fun saveAudioTrack(a: CatalogAudioTrack): Result<Unit> = runCatching {
    requireUserId()
    val id = a.id.ifBlank { "audio_${UUID.randomUUID().toString().replace("-", "").take(10)}" }
    db.collection("catalog_audio").document(id).set(
      mapOf(
        "name" to a.name,
        "slot" to a.slot,
        "bpm" to a.bpm,
        "pitchBaseHz" to a.pitchBaseHz,
        "volume" to a.volume,
        "customUri" to a.customUri,
        "enabled" to a.enabled,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    ).await()
    writeAuditLog("SAVE_AUDIO", "Updated audio track ${a.name} (${a.slot})")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.WRITE, "catalog_audio") }

  suspend fun togglePlayerSuspension(targetUid: String, suspend: Boolean, reason: String): Result<Unit> = runCatching {
    requireUserId()
    val batch = db.batch()
    batch.update(
      db.collection("users").document(targetUid),
      mapOf("isSuspended" to suspend, "suspensionReason" to reason.take(250), "updatedAt" to FieldValue.serverTimestamp())
    )
    batch.update(
      db.collection("public_players").document(targetUid),
      mapOf("isSuspended" to suspend, "updatedAt" to FieldValue.serverTimestamp())
    )
    batch.commit().await()
    writeAuditLog(if (suspend) "SUSPEND_PLAYER" else "RESTORE_PLAYER", "Target=$targetUid Reason=$reason")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "users/$targetUid") }

  suspend fun grantRewardToPlayer(targetProfile: UserProfile, coins: Long, diamonds: Long, title: String): Result<Unit> = runCatching {
    requireUserId()
    val mailId = "mail_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    db.collection("users").document(targetProfile.userId).collection("mailbox").document(mailId).set(
      mapOf(
        "title" to title.ifBlank { "Owner Supply Drop from RAVI" },
        "body" to "Special reward package authorized by Commander RAVI.",
        "rewardCoins" to coins.coerceAtLeast(0L),
        "rewardDiamonds" to diamonds.coerceAtLeast(0L),
        "claimed" to false,
        "isRead" to false,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    ).await()
    writeAuditLog("GRANT_REWARD", "Sent $coins Coins / $diamonds Diamonds to ${targetProfile.playerId}")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.CREATE, "mailbox") }

  suspend fun resolveReport(reportId: String, newStatus: String): Result<Unit> = runCatching {
    requireUserId()
    db.collection("reports").document(reportId).update(mapOf("status" to newStatus)).await()
    writeAuditLog("RESOLVE_REPORT", "Report $reportId -> $newStatus")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "reports/$reportId") }

  suspend fun publishGameUpdate(version: String, title: String, notes: String): Result<Unit> = runCatching {
    requireUserId()
    val id = "upd_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val batch = db.batch()
    batch.set(
      db.collection("update_history").document(id),
      mapOf(
        "name" to title.take(90),
        "version" to version.take(24),
        "notes" to notes.take(400),
        "status" to "published",
        "author" to "RAVI",
        "enabled" to true,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.set(
      db.collection("game_config").document("global"),
      mapOf("serverVersion" to version.take(24), "updatedAt" to FieldValue.serverTimestamp()),
      com.google.firebase.firestore.SetOptions.merge()
    )
    batch.commit().await()
    writeAuditLog("PUBLISH_UPDATE", "Published v$version: $title")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.WRITE, "update_history") }

  suspend fun rollbackUpdate(entry: UpdateHistoryEntry): Result<Unit> = runCatching {
    requireUserId()
    val batch = db.batch()
    batch.update(
      db.collection("update_history").document(entry.id),
      mapOf("status" to "rolled_back", "enabled" to false, "updatedAt" to FieldValue.serverTimestamp())
    )
    batch.set(
      db.collection("game_config").document("global"),
      mapOf("serverVersion" to "rollback-${entry.version}", "updatedAt" to FieldValue.serverTimestamp()),
      com.google.firebase.firestore.SetOptions.merge()
    )
    batch.commit().await()
    writeAuditLog("ROLLBACK_UPDATE", "Rolled back ${entry.version} (${entry.name})")
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "update_history/${entry.id}") }

  private suspend fun writeAuditLog(action: String, details: String) {
    val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    try {
      db.collection("audit_logs").document(logId).set(
        mapOf(
          "name" to action.take(90),
          "actor" to "RAVI",
          "details" to details.take(400),
          "enabled" to true,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (_: Exception) {}
  }
}
