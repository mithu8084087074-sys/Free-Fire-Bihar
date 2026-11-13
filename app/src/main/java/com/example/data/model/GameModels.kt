package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val playerId: String = "",
  val username: String = "Warrior",
  val email: String = "",
  val photoUrl: String = "",
  val role: String = "player",
  val coins: Long = 1000L,
  val diamonds: Long = 100L,
  val level: Long = 1L,
  val xp: Long = 0L,
  val rankPoints: Long = 1000L,
  val kills: Long = 0L,
  val wins: Long = 0L,
  val matchesPlayed: Long = 0L,
  val equippedCharacterId: String = "char_veer",
  val equippedWeaponId: String = "wpn_ak_patna",
  val equippedOutfitId: String = "outfit_obsidian_commando",
  val equippedParachuteId: String = "chute_ember_wing",
  val equippedVehicleId: String = "veh_thar_x",
  val equippedPetId: String = "pet_cyber_falcon",
  val isSuspended: Boolean = false,
  val suspensionReason: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PublicPlayer(
  @DocumentId val id: String = "",
  val userId: String = "",
  val playerId: String = "",
  val username: String = "Warrior",
  val level: Long = 1L,
  val rankPoints: Long = 1000L,
  val kills: Long = 0L,
  val wins: Long = 0L,
  val matchesPlayed: Long = 0L,
  val equippedCharacterId: String = "char_veer",
  val isOnline: Boolean = true,
  val isSuspended: Boolean = false,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class InventoryItem(
  @DocumentId val id: String = "",
  val itemId: String = "",
  val category: String = "character",
  val name: String = "",
  val rarity: String = "Common",
  val level: Long = 1L,
  @ServerTimestamp val acquiredAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FriendRecord(
  @DocumentId val id: String = "",
  val friendUid: String = "",
  val friendPlayerId: String = "",
  val friendUsername: String = "",
  val status: String = "pending_sent",
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MissionRecord(
  @DocumentId val id: String = "",
  val missionId: String = "",
  val title: String = "",
  val progress: Long = 0L,
  val target: Long = 1L,
  val rewardCoins: Long = 100L,
  val rewardDiamonds: Long = 10L,
  val claimed: Boolean = false,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TransactionRecord(
  @DocumentId val id: String = "",
  val txId: String = "",
  val type: String = "purchase",
  val itemName: String = "",
  val coinsDelta: Long = 0L,
  val diamondsDelta: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MailItem(
  @DocumentId val id: String = "",
  val title: String = "",
  val body: String = "",
  val rewardCoins: Long = 0L,
  val rewardDiamonds: Long = 0L,
  val claimed: Boolean = false,
  val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CatalogCharacter(
  @DocumentId val id: String = "",
  val name: String = "",
  val title: String = "",
  val gender: String = "Male",
  val skillName: String = "",
  val description: String = "",
  val primaryColorHex: Long = 0xFFFF6D00L,
  val accentColorHex: Long = 0xFF00E5FFL,
  val hairStyle: String = "Tactical Fade",
  val outfitStyle: String = "Shadow Recon",
  val priceCoins: Long = 0L,
  val priceDiamonds: Long = 0L,
  val enabled: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CatalogWeapon(
  @DocumentId val id: String = "",
  val name: String = "",
  val category: String = "Assault Rifle",
  val damage: Long = 32L,
  val rangeMeters: Long = 120L,
  val fireRateMs: Long = 120L,
  val magazineSize: Long = 30L,
  val reloadTimeMs: Long = 1800L,
  val recoil: Double = 0.04,
  val colorHex: Long = 0xFFFF9100L,
  val attachments: String = "Red Dot, Extended Mag",
  val enabled: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CatalogMap(
  @DocumentId val id: String = "",
  val name: String = "",
  val biome: String = "River Delta",
  val landmarks: String = "Patna Bridge, Military Base, Mill, Forest",
  val maxPlayers: Long = 50L,
  val groundColorHex: Long = 0xFF2B463CL,
  val skyColorHex: Long = 0xFF132238L,
  val description: String = "",
  val enabled: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CatalogVehicle(
  @DocumentId val id: String = "",
  val name: String = "",
  val vehicleType: String = "Jeep",
  val topSpeed: Double = 24.0,
  val maxHp: Long = 450L,
  val seats: Long = 4L,
  val colorHex: Long = 0xFF3E5641L,
  val enabled: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CatalogEvent(
  @DocumentId val id: String = "",
  val name: String = "",
  val eventType: String = "Tournament",
  val description: String = "",
  val rewardSummary: String = "",
  val enabled: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CatalogStoreItem(
  @DocumentId val id: String = "",
  val name: String = "",
  val category: String = "Skin",
  val rarity: String = "Epic",
  val priceCoins: Long = 500L,
  val priceDiamonds: Long = 0L,
  val description: String = "",
  val colorHex: Long = 0xFFFF6D00L,
  val enabled: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CatalogAudioTrack(
  @DocumentId val id: String = "",
  val name: String = "",
  val slot: String = "lobby_bgm",
  val bpm: Long = 118L,
  val pitchBaseHz: Long = 110L,
  val volume: Double = 0.75,
  val customUri: String = "",
  val enabled: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class GlobalGameConfig(
  @DocumentId val id: String = "global",
  val name: String = "Free Fire Bihar Global Config",
  val maintenanceMode: Boolean = false,
  val maintenanceMessage: String = "Scheduled tactical server maintenance in progress.",
  val announcement: String = "SEASON 1: PATNA DELTA DROP — 50-Player 3D Battle Royale Live!",
  val maxPlayersPerMatch: Long = 50L,
  val enabledModes: String = "solo,duo,squad,ranked,classic,training",
  val activeLobbyMusicId: String = "audio_lobby_cyber",
  val activeMatchMusicId: String = "audio_match_combat",
  val lobbyThemeAccent: Long = 0xFFFF6D00L,
  val serverVersion: String = "1.0.0",
  val enabled: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class UpdateHistoryEntry(
  @DocumentId val id: String = "",
  val name: String = "",
  val version: String = "1.0.0",
  val notes: String = "",
  val status: String = "published",
  val author: String = "RAVI",
  val enabled: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MatchRoom(
  @DocumentId val id: String = "",
  val roomCode: String = "",
  val hostUid: String = "",
  val hostName: String = "",
  val mode: String = "solo",
  val mapId: String = "map_patna_delta",
  val mapName: String = "Patna Delta",
  val status: String = "waiting",
  val isPrivate: Boolean = false,
  val passwordHash: String = "",
  val maxPlayers: Long = 50L,
  val playerCount: Long = 1L,
  val fillBots: Boolean = false,
  val safeZoneRadius: Double = 130.0,
  val safeZoneCenterX: Double = 0.0,
  val safeZoneCenterZ: Double = 0.0,
  val phase: Long = 0L,
  val winnerName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MatchPlayer(
  @DocumentId val id: String = "",
  val uid: String = "",
  val username: String = "",
  val characterId: String = "char_veer",
  val weaponId: String = "wpn_ak_patna",
  val x: Double = 0.0,
  val y: Double = 38.0,
  val z: Double = 0.0,
  val yaw: Double = 0.0,
  val hp: Long = 100L,
  val armor: Long = 50L,
  val kills: Long = 0L,
  val isAlive: Boolean = true,
  val isReady: Boolean = true,
  val isCrouching: Boolean = false,
  val isParachuting: Boolean = true,
  val vehicleId: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val senderUid: String = "",
  val senderName: String = "",
  val text: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class GameReport(
  @DocumentId val id: String = "",
  val reporterUid: String = "",
  val targetPlayerId: String = "",
  val reason: String = "",
  val details: String = "",
  val status: String = "open",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AuditLogEntry(
  @DocumentId val id: String = "",
  val name: String = "",
  val actor: String = "RAVI",
  val details: String = "",
  val enabled: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class OwnerSecurityDoc(
  @DocumentId val id: String = "",
  val name: String = "RAVI",
  val passkeyHash: String = "",
  val pin2fa: String = "",
  val mustChangeInitial: Boolean = true,
  val failedAttempts: Long = 0L,
  val enabled: Boolean = true,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class GraphicsTier(val label: String, val renderScale: Float, val shadowDetail: Int, val targetFps: Int) {
  LOW("Low", 0.65f, 0, 30),
  MEDIUM("Medium", 0.8f, 1, 45),
  HIGH("High", 0.92f, 2, 60),
  ULTRA("Ultra", 1.0f, 3, 90),
}

data class LocalPlayerSettings(
  val graphicsTier: GraphicsTier = GraphicsTier.HIGH,
  val targetFps: Int = 60,
  val shadowsEnabled: Boolean = true,
  val textureQuality: String = "High",
  val masterVolume: Float = 0.85f,
  val musicVolume: Float = 0.55f,
  val sfxVolume: Float = 0.9f,
  val touchSensitivity: Float = 1.0f,
  val controlPreset: String = "Classic Thumb",
)
