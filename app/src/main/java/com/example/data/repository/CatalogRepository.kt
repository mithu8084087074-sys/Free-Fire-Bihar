package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.CatalogAudioTrack
import com.example.data.model.CatalogCharacter
import com.example.data.model.CatalogEvent
import com.example.data.model.CatalogMap
import com.example.data.model.CatalogStoreItem
import com.example.data.model.CatalogVehicle
import com.example.data.model.CatalogWeapon
import com.example.data.model.GlobalGameConfig
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class CatalogRepository(private val db: FirebaseFirestore) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  fun observeGlobalConfig(): Flow<GlobalGameConfig> = flow {
    val docRef = db.collection("game_config").document("global")
    emitAll(
      docRef.snapshots()
        .map { snap ->
          if (snap.exists()) {
            snap.toObject(GlobalGameConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              ?: GlobalGameConfig()
          } else {
            GlobalGameConfig()
          }
        }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.GET, docRef.path)
          emit(GlobalGameConfig())
        }
    )
  }

  fun observeCharacters(): Flow<List<CatalogCharacter>> = flow {
    val col = db.collection("catalog_characters")
    emitAll(
      col.snapshots()
        .map { snap ->
          val list = snap.toObjects(CatalogCharacter::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          if (list.isEmpty()) defaultCharacters else (defaultCharacters.filter { def -> list.none { it.id == def.id } } + list)
        }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
          emit(defaultCharacters)
        }
    )
  }

  fun observeWeapons(): Flow<List<CatalogWeapon>> = flow {
    val col = db.collection("catalog_weapons")
    emitAll(
      col.snapshots()
        .map { snap ->
          val list = snap.toObjects(CatalogWeapon::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          if (list.isEmpty()) defaultWeapons else (defaultWeapons.filter { def -> list.none { it.id == def.id } } + list)
        }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
          emit(defaultWeapons)
        }
    )
  }

  fun observeMaps(): Flow<List<CatalogMap>> = flow {
    val col = db.collection("catalog_maps")
    emitAll(
      col.snapshots()
        .map { snap ->
          val list = snap.toObjects(CatalogMap::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          if (list.isEmpty()) defaultMaps else (defaultMaps.filter { def -> list.none { it.id == def.id } } + list)
        }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
          emit(defaultMaps)
        }
    )
  }

  fun observeVehicles(): Flow<List<CatalogVehicle>> = flow {
    val col = db.collection("catalog_vehicles")
    emitAll(
      col.snapshots()
        .map { snap ->
          val list = snap.toObjects(CatalogVehicle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          if (list.isEmpty()) defaultVehicles else (defaultVehicles.filter { def -> list.none { it.id == def.id } } + list)
        }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
          emit(defaultVehicles)
        }
    )
  }

  fun observeEvents(): Flow<List<CatalogEvent>> = flow {
    val col = db.collection("catalog_events")
    emitAll(
      col.snapshots()
        .map { snap ->
          val list = snap.toObjects(CatalogEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          if (list.isEmpty()) defaultEvents else (defaultEvents.filter { def -> list.none { it.id == def.id } } + list)
        }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
          emit(defaultEvents)
        }
    )
  }

  fun observeStoreItems(): Flow<List<CatalogStoreItem>> = flow {
    val col = db.collection("catalog_store")
    emitAll(
      col.snapshots()
        .map { snap ->
          val list = snap.toObjects(CatalogStoreItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          if (list.isEmpty()) defaultStoreItems else (defaultStoreItems.filter { def -> list.none { it.id == def.id } } + list)
        }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
          emit(defaultStoreItems)
        }
    )
  }

  fun observeAudioTracks(): Flow<List<CatalogAudioTrack>> = flow {
    val col = db.collection("catalog_audio")
    emitAll(
      col.snapshots()
        .map { snap ->
          val list = snap.toObjects(CatalogAudioTrack::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          if (list.isEmpty()) defaultAudioTracks else (defaultAudioTracks.filter { def -> list.none { it.id == def.id } } + list)
        }
        .catch { e ->
          val fsErr = generateSequence(e) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
          if (fsErr == null && e is CancellationException) throw e
          handleFirestoreError(fsErr ?: e, OperationType.LIST, col.path)
          emit(defaultAudioTracks)
        }
    )
  }

  companion object {
    val defaultCharacters = listOf(
      CatalogCharacter("char_veer", "Veer", "Patna Vanguard", "Male", "Iron Pulse (+20 Armor)", "Elite tactical commander built for frontline urban assaults.", 0xFFFF6D00L, 0xFF00E5FFL, "Spiked Undercut", "Ember Tactical Vest", 0L, 0L, true),
      CatalogCharacter("char_tara", "Tara", "Ganga Shadow", "Female", "Silent Step (+12% Sprint)", "High-mobility recon operative specialized in flank maneuvers.", 0xFF00BFA5L, 0xFFFFAB40L, "Neon Ponytail", "Stealth Carbon Suit", 0L, 0L, true),
      CatalogCharacter("char_arjun", "Arjun", "Nalanda Marksman", "Male", "Steady Scope (-25% Recoil)", "Precision long-range sniper with enhanced ballistic stability.", 0xFF3D5AFEL, 0xFFFFD600L, "Combat Crop", "Sniper Ghillie Coat", 800L, 0L, true),
      CatalogCharacter("char_kavya", "Kavya", "Magadh Medic", "Female", "Quick Aid (Fast Medkit)", "Combat field medic who restores squad health 35% faster.", 0xFFE91E63L, 0xFF69F0AEL, "Braided Tactical", "Apex Bio-Armor", 0L, 60L, true),
    )

    val defaultWeapons = listOf(
      CatalogWeapon("wpn_ak_patna", "AK-Patna 47", "Assault Rifle", 36L, 140L, 115L, 30L, 1900L, 0.045, 0xFFFF8F00L, "Holographic Sight, Compensator", true),
      CatalogWeapon("wpn_son_smg", "Son-River Vector", "SMG", 22L, 85L, 70L, 38L, 1400L, 0.03, 0xFF00E5FFL, "Laser Sight, Extended Mag", true),
      CatalogWeapon("wpn_gaya_sniper", "Gaya-X Barrett", "Sniper Rifle", 92L, 260L, 950L, 6L, 2600L, 0.08, 0xFF76FF03L, "8x Thermal Scope, Suppressor", true),
      CatalogWeapon("wpn_kosi_shotgun", "Kosi Thunder-12", "Shotgun", 78L, 45L, 620L, 8L, 2100L, 0.09, 0xFFFF3D00L, "Choke Barrel, Shell Rack", true),
      CatalogWeapon("wpn_darbhanga_pistol", "M9-Bihar Sidearm", "Pistol", 26L, 70L, 240L, 15L, 1200L, 0.02, 0xFFB0BEC5L, "Tactical Reflex", true),
      CatalogWeapon("wpn_talwar_melee", "Loha Tactical Blade", "Melee", 65L, 5L, 420L, 999L, 0L, 0.0, 0xFFFFD54FL, "Carbon Grip", true),
    )

    val defaultMaps = listOf(
      CatalogMap("map_patna_delta", "Patna Delta", "Riverside Urban & Military", "Gandhi Bridge, Military Airfield, Industrial Mill, Forest Ridge, River Village", 50L, 0xFF284237L, 0xFF101F33L, "Expansive 3D river-split battleground with bridges, multi-story compounds, factories, and dense forest cover.", true),
      CatalogMap("map_rajgir_highlands", "Rajgir Highlands", "Mountain Fortress", "Highland Citadel, Rope Bridge, Bunker Valley, Pine Woods", 50L, 0xFF3E4A35L, 0xFF1B192BL, "Elevated mountain valleys, sniper ridges, and fortified stone compounds.", true),
    )

    val defaultVehicles = listOf(
      CatalogVehicle("veh_thar_x", "Sher-E-Bihar 4x4", "Jeep", 25.0, 500L, 4L, 0xFF2E4D3AL, true),
      CatalogVehicle("veh_bullet_350", "Storm Moto 350", "Motorcycle", 32.0, 260L, 2L, 0xFFFF6D00L, true),
      CatalogVehicle("veh_armored_suv", "Vajra Armored SUV", "Armored Car", 22.0, 850L, 4L, 0xFF37474FL, true),
    )

    val defaultEvents = listOf(
      CatalogEvent("evt_patna_cup", "Bihar Premier Battle Cup", "Tournament", "Compete in Ranked Squad matches to climb the Season 1 leaderboard.", "2,500 Coins + Crimson Phoenix Skin", true),
      CatalogEvent("evt_login_streak", "7-Day Supply Drop", "Daily Event", "Drop into battle daily to unlock weapon skins and Diamonds.", "120 Diamonds + 1,500 Coins", true),
    )

    val defaultStoreItems = listOf(
      CatalogStoreItem("skin_crimson_phoenix", "Crimson Phoenix Armor", "Outfit", "Legendary", 0L, 80L, "Blazing carbon-weave tactical suit with glowing ember trim.", 0xFFFF3D00L, true),
      CatalogStoreItem("skin_cyber_ganga", "Cyber Ganga Recon", "Outfit", "Epic", 750L, 0L, "Electric cyan urban stealth exo-suit.", 0xFF00E5FFL, true),
      CatalogStoreItem("wpn_golden_ak", "Golden Rajgir AK Skin", "Weapon Skin", "Legendary", 900L, 0L, "Engraved gold-plated tactical finish for AK-Patna 47.", 0xFFFFD600L, true),
      CatalogStoreItem("chute_solar_flare", "Solar Flare Parachute", "Parachute", "Epic", 450L, 0L, "High-visibility amber tactical canopy.", 0xFFFF9100L, true),
      CatalogStoreItem("emote_booyah_salute", "Bihar Victory Salute", "Emote", "Rare", 300L, 0L, "Signature championship tactical salute.", 0xFF7C4DFFL, true),
    )

    val defaultAudioTracks = listOf(
      CatalogAudioTrack("audio_lobby_cyber", "Patna Cyber Pulse (Lobby)", "lobby_bgm", 116L, 110L, 0.7, "", true),
      CatalogAudioTrack("audio_match_combat", "Delta Strike (Match)", "match_bgm", 136L, 98L, 0.65, "", true),
      CatalogAudioTrack("audio_victory_fanfare", "Bihar Booyah Anthem", "victory_bgm", 128L, 130L, 0.85, "", true),
      CatalogAudioTrack("audio_defeat_sting", "Fallen Operative", "defeat_bgm", 82L, 73L, 0.75, "", true),
    )
  }
}
