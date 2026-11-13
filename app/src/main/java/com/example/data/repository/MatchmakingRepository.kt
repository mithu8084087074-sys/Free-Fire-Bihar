package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.MatchPlayer
import com.example.data.model.MatchRoom
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

class MatchmakingRepository(private val db: FirebaseFirestore) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing matchmaking.")
  }

  fun observeAvailableRooms(): Flow<List<MatchRoom>> = flow {
    val col = db.collection("match_rooms")
    emitAll(
      col.snapshots()
        .map { snap ->
          snap.toObjects(MatchRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .filter { it.status == "waiting" || it.status == "active" }
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
          throw e
        }
    )
  }

  fun observeRoom(roomId: String): Flow<MatchRoom?> = flow {
    val ref = db.collection("match_rooms").document(roomId)
    emitAll(
      ref.snapshots()
        .map { it.toObject(MatchRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.GET, ref.path)
          throw e
        }
    )
  }

  fun observeRoomPlayers(roomId: String): Flow<List<MatchPlayer>> = flow {
    val col = db.collection("match_rooms").document(roomId).collection("players")
    emitAll(
      col.snapshots()
        .map { snap -> snap.toObjects(MatchPlayer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
          throw e
        }
    )
  }

  fun observeRoomChat(roomId: String): Flow<List<ChatMessage>> = flow {
    val col = db.collection("match_rooms").document(roomId).collection("chat")
    emitAll(
      col.snapshots()
        .map { snap ->
          snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt?.seconds ?: 0L }
        }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
          throw e
        }
    )
  }

  suspend fun createRoom(
    profile: UserProfile,
    mode: String,
    mapId: String,
    mapName: String,
    isPrivate: Boolean,
    passwordPlain: String,
    maxPlayers: Long,
    fillBots: Boolean,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val roomCode = "FFB${(1000..9999).random()}"
    val pwHash = if (isPrivate && passwordPlain.isNotBlank()) sha256(passwordPlain) else ""

    val roomRef = db.collection("match_rooms").document(roomId)
    val playerRef = roomRef.collection("players").document(uid)

    val batch = db.batch()
    batch.set(
      roomRef,
      mapOf(
        "roomCode" to roomCode,
        "hostUid" to uid,
        "hostName" to profile.username,
        "mode" to mode.lowercase(),
        "mapId" to mapId,
        "mapName" to mapName,
        "status" to "waiting",
        "isPrivate" to isPrivate,
        "passwordHash" to pwHash,
        "maxPlayers" to maxPlayers.coerceIn(2L, 50L),
        "playerCount" to 1L,
        "fillBots" to fillBots,
        "safeZoneRadius" to 130.0,
        "safeZoneCenterX" to 0.0,
        "safeZoneCenterZ" to 0.0,
        "phase" to 0L,
        "winnerName" to "",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.set(
      playerRef,
      mapOf(
        "uid" to uid,
        "username" to profile.username,
        "characterId" to profile.equippedCharacterId,
        "weaponId" to profile.equippedWeaponId,
        "x" to 0.0,
        "y" to 38.0,
        "z" to 0.0,
        "yaw" to 0.0,
        "hp" to 100L,
        "armor" to 50L,
        "kills" to 0L,
        "isAlive" to true,
        "isReady" to true,
        "isCrouching" to false,
        "isParachuting" to true,
        "vehicleId" to "",
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.commit().await()
    roomId
  }.onFailure { handleFirestoreError(it, OperationType.CREATE, "match_rooms") }

  suspend fun joinRoom(
    room: MatchRoom,
    profile: UserProfile,
    passwordPlain: String = "",
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    if (room.isPrivate && room.passwordHash.isNotBlank()) {
      require(sha256(passwordPlain) == room.passwordHash) { "Invalid Room Password." }
    }
    val roomRef = db.collection("match_rooms").document(room.id)
    val playerRef = roomRef.collection("players").document(uid)
    val existingPlayer = playerRef.get().await()

    val batch = db.batch()
    if (!existingPlayer.exists()) {
      batch.update(
        roomRef,
        mapOf(
          "playerCount" to (room.playerCount + 1L).coerceAtMost(room.maxPlayers),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
    }
    batch.set(
      playerRef,
      mapOf(
        "uid" to uid,
        "username" to profile.username,
        "characterId" to profile.equippedCharacterId,
        "weaponId" to profile.equippedWeaponId,
        "x" to ((-18..18).random().toDouble()),
        "y" to 38.0,
        "z" to ((-18..18).random().toDouble()),
        "yaw" to 0.0,
        "hp" to 100L,
        "armor" to 50L,
        "kills" to 0L,
        "isAlive" to true,
        "isReady" to true,
        "isCrouching" to false,
        "isParachuting" to true,
        "vehicleId" to "",
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    )
    batch.commit().await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.WRITE, "match_rooms/${room.id}") }

  suspend fun leaveRoom(room: MatchRoom): Result<Unit> = runCatching {
    val uid = requireUserId()
    val roomRef = db.collection("match_rooms").document(room.id)
    val batch = db.batch()
    batch.delete(roomRef.collection("players").document(uid))
    if (room.hostUid != uid) {
      batch.update(
        roomRef,
        mapOf(
          "playerCount" to (room.playerCount - 1L).coerceAtLeast(0L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
    }
    batch.commit().await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.DELETE, "match_rooms/${room.id}") }

  suspend fun startMatch(roomId: String): Result<Unit> = runCatching {
    requireUserId()
    db.collection("match_rooms").document(roomId).update(
      mapOf(
        "status" to "active",
        "phase" to 1L,
        "safeZoneRadius" to 130.0,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    ).await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "match_rooms/$roomId") }

  suspend fun finishMatch(roomId: String, winnerName: String): Result<Unit> = runCatching {
    requireUserId()
    db.collection("match_rooms").document(roomId).update(
      mapOf(
        "status" to "finished",
        "winnerName" to winnerName.take(60),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    ).await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "match_rooms/$roomId") }

  suspend fun syncPlayerTransform(
    roomId: String,
    profile: UserProfile,
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
    val uid = auth.currentUser?.uid ?: return
    try {
      db.collection("match_rooms").document(roomId).collection("players").document(uid)
        .set(
          mapOf(
            "uid" to uid,
            "username" to profile.username,
            "characterId" to profile.equippedCharacterId,
            "weaponId" to profile.equippedWeaponId,
            "x" to x,
            "y" to y,
            "z" to z,
            "yaw" to yaw,
            "hp" to hp.toLong().coerceIn(0L, 200L),
            "armor" to armor.toLong().coerceIn(0L, 200L),
            "kills" to kills.toLong().coerceAtLeast(0L),
            "isAlive" to isAlive,
            "isReady" to true,
            "isCrouching" to isCrouching,
            "isParachuting" to isParachuting,
            "vehicleId" to vehicleId,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "match_rooms/$roomId/players/$uid")
    }
  }

  suspend fun sendChatMessage(roomId: String, senderName: String, text: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val clean = text.trim().take(280)
    require(clean.isNotEmpty())
    val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    db.collection("match_rooms").document(roomId).collection("chat").document(msgId)
      .set(
        mapOf(
          "senderUid" to uid,
          "senderName" to senderName.take(30),
          "text" to clean,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    Unit
  }.onFailure { handleFirestoreError(it, OperationType.CREATE, "match_rooms/$roomId/chat") }

  private fun sha256(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
  }
}
