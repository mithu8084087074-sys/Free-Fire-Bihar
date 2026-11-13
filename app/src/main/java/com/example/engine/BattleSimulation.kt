package com.example.engine

import com.example.audio.GameAudioEngine
import com.example.data.model.CatalogCharacter
import com.example.data.model.CatalogMap
import com.example.data.model.CatalogWeapon
import com.example.data.model.LocalPlayerSettings
import com.example.data.model.MatchPlayer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class AabbObstacle(
  val minX: Float,
  val maxX: Float,
  val minZ: Float,
  val maxZ: Float,
  val height: Float,
  val colorRgb: FloatArray,
  val label: String,
)

data class TreeProp(
  val x: Float,
  val z: Float,
  val scale: Float,
)

data class LootDrop(
  val id: String,
  val x: Float,
  val z: Float,
  val kind: LootKind,
  val weaponId: String = "",
  val name: String,
  val amount: Int,
  var collected: Boolean = false,
)

enum class LootKind {
  WEAPON,
  AMMO,
  MEDKIT,
  ARMOR,
}

data class VehicleEntity(
  val id: String,
  val name: String,
  val type: String,
  var x: Float,
  var z: Float,
  var yawDeg: Float,
  var hp: Int,
  val maxHp: Int,
  val topSpeed: Float,
  val colorRgb: FloatArray,
  var driverId: String? = null,
)

data class CombatantEntity(
  val id: String,
  val name: String,
  val isBot: Boolean,
  val isRemotePlayer: Boolean,
  var x: Float,
  var y: Float,
  var z: Float,
  var yawDeg: Float,
  var hp: Int,
  var maxHp: Int,
  var armor: Int,
  var kills: Int,
  var isAlive: Boolean,
  var isCrouching: Boolean,
  var isParachuting: Boolean,
  var walkCycle: Float,
  var weaponId: String,
  val primaryRgb: FloatArray,
  val accentRgb: FloatArray,
  var vehicleId: String = "",
  var aiDecisionTimer: Float = 0f,
  var aiTargetX: Float = 0f,
  var aiTargetZ: Float = 0f,
  var aiFireCooldown: Float = 0f,
)

data class BulletTracer(
  val startX: Float,
  val startY: Float,
  val startZ: Float,
  val endX: Float,
  val endY: Float,
  val endZ: Float,
  var ttl: Float = 0.14f,
)

data class KillFeedItem(
  val id: String,
  val text: String,
  val isPlayerEvent: Boolean,
)

data class HudSnapshot(
  val hp: Int = 100,
  val maxHp: Int = 100,
  val armor: Int = 50,
  val kills: Int = 0,
  val aliveCount: Int = 1,
  val totalCount: Int = 1,
  val matchElapsedSec: Int = 0,
  val zonePhase: Int = 1,
  val zoneRadius: Float = 115f,
  val zoneCenterX: Float = 0f,
  val zoneCenterZ: Float = 0f,
  val outsideZone: Boolean = false,
  val isParachuting: Boolean = true,
  val isCrouching: Boolean = false,
  val isSprinting: Boolean = false,
  val isAiming: Boolean = false,
  val isReloading: Boolean = false,
  val activeWeaponSlot: Int = 0,
  val primaryWeapon: CatalogWeapon? = null,
  val secondaryWeapon: CatalogWeapon? = null,
  val primaryAmmoInMag: Int = 30,
  val primaryReserveAmmo: Int = 90,
  val secondaryAmmoInMag: Int = 15,
  val secondaryReserveAmmo: Int = 45,
  val medkits: Int = 2,
  val nearbyLoot: LootDrop? = null,
  val nearbyVehicle: VehicleEntity? = null,
  val drivingVehicle: VehicleEntity? = null,
  val playerX: Float = 0f,
  val playerZ: Float = 0f,
  val playerYawDeg: Float = 0f,
  val hitMarkerTimer: Float = 0f,
  val damageFlashTimer: Float = 0f,
  val killFeed: List<KillFeedItem> = emptyList(),
  val isAlive: Boolean = true,
  val isMatchOver: Boolean = false,
  val wonMatch: Boolean = false,
  val placement: Int = 1,
  val spectatingName: String = "",
  val radarBlips: List<Triple<Float, Float, Boolean>> = emptyList(),
)

/**
 * Full 3D Battle Royale physics, AI bot, ballistics, loot, vehicle, and safe-zone simulation.
 * Thread-safe snapshot exposed to both the OpenGL ES renderer and Jetpack Compose HUD.
 */
class BattleSimulation(
  val map: CatalogMap,
  val weaponsCatalog: List<CatalogWeapon>,
  val playerName: String,
  val playerCharacter: CatalogCharacter,
  val initialWeaponId: String,
  val isBotMode: Boolean,
  val botCount: Int,
  val audioEngine: GameAudioEngine,
) {
  val worldHalfSize = 115f
  val obstacles = mutableListOf<AabbObstacle>()
  val trees = mutableListOf<TreeProp>()
  val lootDrops = mutableListOf<LootDrop>()
  val vehicles = mutableListOf<VehicleEntity>()
  val combatants = mutableListOf<CombatantEntity>()
  val tracers = mutableListOf<BulletTracer>()
  private val killFeed = ArrayDeque<KillFeedItem>()

  // Local player state
  @Volatile var moveInputX: Float = 0f
  @Volatile var moveInputY: Float = 0f
  @Volatile var isSprinting: Boolean = false
  @Volatile var isCrouching: Boolean = false
  @Volatile var isAiming: Boolean = false
  @Volatile var isFiringHeld: Boolean = false
  @Volatile var settings: LocalPlayerSettings = LocalPlayerSettings()

  var cameraYawDeg: Float = 0f
  var cameraPitchDeg: Float = 16f
  private var verticalVel: Float = 0f
  private var jumpCooldown: Float = 0f
  private var fireCooldown: Float = 0f
  private var reloadTimer: Float = 0f
  private var stepSfxTimer: Float = 0f
  private var zoneDamageTimer: Float = 0f
  private var hitMarkerTimer: Float = 0f
  private var damageFlashTimer: Float = 0f
  private var matchTimeSec: Float = 0f
  private var zonePhase: Int = 1
  private var zoneRadius: Float = 115f
  private var zoneCenterX: Float = 0f
  private var zoneCenterZ: Float = 0f

  private var activeSlot: Int = 0 // 0=primary, 1=secondary, 2=melee
  private var primaryWeapon: CatalogWeapon =
    weaponsCatalog.firstOrNull { it.id == initialWeaponId } ?: weaponsCatalog.first()
  private var secondaryWeapon: CatalogWeapon =
    weaponsCatalog.firstOrNull { it.category.equals("Pistol", true) }
      ?: weaponsCatalog.firstOrNull { it.id != primaryWeapon.id }
      ?: primaryWeapon
  private val meleeWeapon: CatalogWeapon =
    weaponsCatalog.firstOrNull { it.category.equals("Melee", true) } ?: primaryWeapon

  private var primaryMag: Int = primaryWeapon.magazineSize.toInt()
  private var primaryReserve: Int = primaryWeapon.magazineSize.toInt() * 3
  private var secondaryMag: Int = secondaryWeapon.magazineSize.toInt()
  private var secondaryReserve: Int = secondaryWeapon.magazineSize.toInt() * 3
  private var medkits: Int = 2
  private var medkitCastTimer: Float = 0f

  private var spectateIndex: Int = 0
  private var matchFinished: Boolean = false
  private var wonMatch: Boolean = false
  private var finalPlacement: Int = 1

  val localPlayer: CombatantEntity

  init {
    buildWorldMap()
    val pRgb = hexToRgb(playerCharacter.primaryColorHex)
    val aRgb = hexToRgb(playerCharacter.accentColorHex)
    localPlayer = CombatantEntity(
      id = "local_player",
      name = playerName,
      isBot = false,
      isRemotePlayer = false,
      x = (-22..22).random().toFloat(),
      y = 36f,
      z = (-22..22).random().toFloat(),
      yawDeg = 0f,
      hp = 100,
      maxHp = 100,
      armor = 50,
      kills = 0,
      isAlive = true,
      isCrouching = false,
      isParachuting = true,
      walkCycle = 0f,
      weaponId = primaryWeapon.id,
      primaryRgb = pRgb,
      accentRgb = aRgb,
    )
    combatants.add(localPlayer)

    if (isBotMode && botCount > 0) {
      val botNames = listOf(
        "[BOT] Patna_Tiger", "[BOT] Gaya_Hawkeye", "[BOT] Nalanda_Scout",
        "[BOT] Kosi_Viper", "[BOT] Mithila_Wolf", "[BOT] Bhagalpur_Fox",
        "[BOT] Siwan_Ranger", "[BOT] Darbhanga_Ace", "[BOT] Rajgir_Sniper",
        "[BOT] Ara_Commando", "[BOT] Muzaffar_Strike", "[BOT] Buxar_Blade",
        "[BOT] Begusarai_Tank", "[BOT] Purnia_Ghost", "[BOT] Chapra_Fury",
      )
      for (i in 0 until botCount.coerceAtMost(24)) {
        val angle = (i.toFloat() / botCount) * 2f * PI.toFloat()
        val dist = 30f + Random.nextFloat() * 58f
        val bx = cos(angle) * dist
        val bz = sin(angle) * dist
        combatants.add(
          CombatantEntity(
            id = "bot_$i",
            name = botNames[i % botNames.size],
            isBot = true,
            isRemotePlayer = false,
            x = bx,
            y = 0f,
            z = bz,
            yawDeg = Random.nextFloat() * 360f,
            hp = 100,
            maxHp = 100,
            armor = 25,
            kills = 0,
            isAlive = true,
            isCrouching = false,
            isParachuting = false,
            walkCycle = Random.nextFloat() * 6f,
            weaponId = weaponsCatalog[i % weaponsCatalog.size].id,
            primaryRgb = floatArrayOf(0.82f, 0.22f, 0.22f),
            accentRgb = floatArrayOf(0.95f, 0.65f, 0.15f),
            aiDecisionTimer = Random.nextFloat() * 2f,
            aiTargetX = bx,
            aiTargetZ = bz,
          )
        )
      }
    }
  }

  private fun buildWorldMap() {
    // City buildings (NE)
    val cityCol = floatArrayOf(0.32f, 0.36f, 0.42f)
    val milCol = floatArrayOf(0.28f, 0.34f, 0.25f)
    val factCol = floatArrayOf(0.45f, 0.28f, 0.22f)
    val villageCol = floatArrayOf(0.55f, 0.44f, 0.32f)
    val bridgeCol = floatArrayOf(0.42f, 0.44f, 0.48f)

    obstacles += AabbObstacle(24f, 34f, -44f, -32f, 8.5f, cityCol, "Patna Tower A")
    obstacles += AabbObstacle(44f, 56f, -56f, -44f, 10.5f, cityCol, "Patna Plaza")
    obstacles += AabbObstacle(28f, 38f, -20f, -10f, 6.5f, cityCol, "City Depot")
    obstacles += AabbObstacle(52f, 62f, -24f, -12f, 7.5f, cityCol, "Metro Block")

    // Military Airfield & Bunkers (SE)
    obstacles += AabbObstacle(28f, 44f, 22f, 34f, 6.0f, milCol, "Military Hangar")
    obstacles += AabbObstacle(54f, 62f, 36f, 44f, 11.0f, milCol, "Watchtower")
    obstacles += AabbObstacle(26f, 34f, 52f, 60f, 4.5f, milCol, "Ammo Bunker")

    // Industrial Factory (SW)
    obstacles += AabbObstacle(-56f, -38f, 24f, 38f, 8.0f, factCol, "Steel Mill")
    obstacles += AabbObstacle(-32f, -22f, 46f, 56f, 5.0f, factCol, "Container Yard")
    obstacles += AabbObstacle(-64f, -52f, 50f, 60f, 6.5f, factCol, "Refinery")

    // Village Compounds (NW)
    obstacles += AabbObstacle(-46f, -36f, -44f, -34f, 4.2f, villageCol, "Village House 1")
    obstacles += AabbObstacle(-30f, -22f, -60f, -52f, 4.2f, villageCol, "Village House 2")
    obstacles += AabbObstacle(-62f, -52f, -26f, -16f, 4.5f, villageCol, "Barn Compound")

    // Bridge guardrails
    obstacles += AabbObstacle(-9f, 9f, -41f, -39f, 1.3f, bridgeCol, "North Bridge Rail")
    obstacles += AabbObstacle(-9f, 9f, 39f, 41f, 1.3f, bridgeCol, "South Bridge Rail")

    // Trees in forests
    val treeCoords = listOf(
      -70f to -65f, -78f to -45f, -65f to -50f, -48f to -70f, -25f to -78f,
      -74f to 10f, -82f to -12f, 68f to -68f, 76f to -42f, 74f to 14f,
      66f to 64f, 42f to 76f, -18f to 74f, -72f to 72f, 18f to -72f,
    )
    treeCoords.forEachIndexed { idx, (tx, tz) ->
      trees += TreeProp(tx, tz, 0.9f + (idx % 3) * 0.22f)
    }

    // Ground Loot Drops
    val lootSpots = listOf(
      Triple(20f, -30f, LootKind.WEAPON),
      Triple(42f, -34f, LootKind.ARMOR),
      Triple(36f, 18f, LootKind.WEAPON),
      Triple(50f, 30f, LootKind.MEDKIT),
      Triple(-36f, 20f, LootKind.WEAPON),
      Triple(-45f, 42f, LootKind.AMMO),
      Triple(-38f, -30f, LootKind.WEAPON),
      Triple(-24f, -48f, LootKind.MEDKIT),
      Triple(0f, -35f, LootKind.ARMOR),
      Triple(0f, 35f, LootKind.WEAPON),
      Triple(18f, 48f, LootKind.AMMO),
      Triple(-58f, -10f, LootKind.MEDKIT),
    )
    lootSpots.forEachIndexed { idx, (lx, lz, kind) ->
      val w = weaponsCatalog[idx % weaponsCatalog.size]
      lootDrops += when (kind) {
        LootKind.WEAPON -> LootDrop("loot_$idx", lx, lz, LootKind.WEAPON, w.id, w.name, w.magazineSize.toInt())
        LootKind.AMMO -> LootDrop("loot_$idx", lx, lz, LootKind.AMMO, "", "Tactical Ammo x45", 45)
        LootKind.MEDKIT -> LootDrop("loot_$idx", lx, lz, LootKind.MEDKIT, "", "Combat Medkit", 1)
        LootKind.ARMOR -> LootDrop("loot_$idx", lx, lz, LootKind.ARMOR, "", "Lv.3 Armor Vest", 50)
      }
    }

    // Drivable Vehicles
    vehicles += VehicleEntity("veh_jeep_1", "Sher-E-Bihar 4x4", "Jeep", 16f, -35f, 90f, 500, 500, 24f, floatArrayOf(0.22f, 0.42f, 0.28f))
    vehicles += VehicleEntity("veh_moto_1", "Storm Moto 350", "Motorcycle", -20f, 35f, 0f, 260, 260, 31f, floatArrayOf(1.0f, 0.45f, 0.05f))
    vehicles += VehicleEntity("veh_suv_1", "Vajra Armored SUV", "SUV", 38f, 46f, 180f, 850, 850, 21f, floatArrayOf(0.24f, 0.29f, 0.35f))
  }

  @Synchronized
  fun syncRemotePlayers(remotePlayers: List<MatchPlayer>, localUid: String) {
    val activeRemoteIds = remotePlayers.filter { it.uid != localUid }.map { it.uid }.toSet()
    combatants.removeAll { it.isRemotePlayer && it.id !in activeRemoteIds }

    for (rp in remotePlayers) {
      if (rp.uid == localUid) continue
      val existing = combatants.firstOrNull { it.id == rp.uid }
      if (existing != null) {
        val moved = hypot(rp.x.toFloat() - existing.x, rp.z.toFloat() - existing.z)
        existing.x = rp.x.toFloat()
        existing.y = rp.y.toFloat()
        existing.z = rp.z.toFloat()
        existing.yawDeg = rp.yaw.toFloat()
        existing.hp = rp.hp.toInt()
        existing.armor = rp.armor.toInt()
        existing.kills = rp.kills.toInt()
        existing.isAlive = rp.isAlive
        existing.isCrouching = rp.isCrouching
        existing.isParachuting = rp.isParachuting
        existing.weaponId = rp.weaponId
        existing.vehicleId = rp.vehicleId
        if (moved > 0.05f) existing.walkCycle += 0.35f
      } else {
        combatants.add(
          CombatantEntity(
            id = rp.uid,
            name = rp.username,
            isBot = false,
            isRemotePlayer = true,
            x = rp.x.toFloat(),
            y = rp.y.toFloat(),
            z = rp.z.toFloat(),
            yawDeg = rp.yaw.toFloat(),
            hp = rp.hp.toInt(),
            maxHp = 100,
            armor = rp.armor.toInt(),
            kills = rp.kills.toInt(),
            isAlive = rp.isAlive,
            isCrouching = rp.isCrouching,
            isParachuting = rp.isParachuting,
            walkCycle = 0f,
            weaponId = rp.weaponId,
            primaryRgb = floatArrayOf(0.15f, 0.65f, 0.95f),
            accentRgb = floatArrayOf(1.0f, 0.85f, 0.15f),
            vehicleId = rp.vehicleId,
          )
        )
      }
    }
  }

  @Synchronized
  fun applyLookDelta(dx: Float, dy: Float) {
    val sens = settings.touchSensitivity * (if (isAiming) 0.48f else 1.0f)
    cameraYawDeg = (cameraYawDeg + dx * 0.22f * sens) % 360f
    cameraPitchDeg = (cameraPitchDeg + dy * 0.16f * sens).coerceIn(-12f, 48f)
  }

  @Synchronized
  fun triggerJump() {
    if (!localPlayer.isAlive || localPlayer.isParachuting || localPlayer.vehicleId.isNotEmpty()) return
    if (localPlayer.y <= 0.05f && jumpCooldown <= 0f) {
      verticalVel = 7.6f
      jumpCooldown = 0.45f
      isCrouching = false
    }
  }

  @Synchronized
  fun toggleCrouch() {
    if (!localPlayer.isParachuting && localPlayer.vehicleId.isEmpty()) {
      isCrouching = !isCrouching
      localPlayer.isCrouching = isCrouching
    }
  }

  @Synchronized
  fun switchWeaponSlot(slot: Int) {
    activeSlot = slot.coerceIn(0, 2)
    reloadTimer = 0f
    localPlayer.weaponId = currentWeapon().id
    audioEngine.playSfx(GameAudioEngine.SfxType.UI_TAP)
  }

  @Synchronized
  fun triggerReload() {
    if (activeSlot == 2 || reloadTimer > 0f) return
    val w = currentWeapon()
    val maxMag = w.magazineSize.toInt()
    if (activeSlot == 0 && primaryMag < maxMag && primaryReserve > 0) {
      reloadTimer = w.reloadTimeMs / 1000f
      audioEngine.playSfx(GameAudioEngine.SfxType.RELOAD)
    } else if (activeSlot == 1 && secondaryMag < maxMag && secondaryReserve > 0) {
      reloadTimer = w.reloadTimeMs / 1000f
      audioEngine.playSfx(GameAudioEngine.SfxType.RELOAD)
    }
  }

  @Synchronized
  fun useMedkit() {
    if (!localPlayer.isAlive || medkits <= 0 || localPlayer.hp >= localPlayer.maxHp) return
    medkits--
    localPlayer.hp = (localPlayer.hp + 55).coerceAtMost(localPlayer.maxHp)
    audioEngine.playSfx(GameAudioEngine.SfxType.MEDKIT)
    pushKillFeed("${localPlayer.name} used Combat Medkit (+55 HP)", true)
  }

  @Synchronized
  fun interactPickupOrVehicle() {
    if (!localPlayer.isAlive) return
    val currentVeh = vehicles.firstOrNull { it.id == localPlayer.vehicleId }
    if (currentVeh != null) {
      currentVeh.driverId = null
      localPlayer.vehicleId = ""
      localPlayer.x += 2.2f
      audioEngine.playSfx(GameAudioEngine.SfxType.UI_TAP)
      return
    }
    val nearVeh = vehicles.firstOrNull { it.hp > 0 && hypot(it.x - localPlayer.x, it.z - localPlayer.z) < 5.5f }
    if (nearVeh != null) {
      nearVeh.driverId = localPlayer.id
      localPlayer.vehicleId = nearVeh.id
      localPlayer.isParachuting = false
      isCrouching = false
      audioEngine.playSfx(GameAudioEngine.SfxType.VEHICLE_HUM)
      return
    }
    val nearLoot = lootDrops.firstOrNull { !it.collected && hypot(it.x - localPlayer.x, it.z - localPlayer.z) < 4.5f }
    if (nearLoot != null) {
      collectLoot(nearLoot)
    }
  }

  @Synchronized
  fun cycleSpectator() {
    val aliveOthers = combatants.filter { it.isAlive && it.id != localPlayer.id }
    if (aliveOthers.isNotEmpty()) {
      spectateIndex = (spectateIndex + 1) % aliveOthers.size
    }
  }

  private fun collectLoot(loot: LootDrop) {
    loot.collected = true
    when (loot.kind) {
      LootKind.WEAPON -> {
        val found = weaponsCatalog.firstOrNull { it.id == loot.weaponId }
        if (found != null) {
          primaryWeapon = found
          primaryMag = found.magazineSize.toInt()
          primaryReserve += found.magazineSize.toInt() * 2
          activeSlot = 0
          localPlayer.weaponId = found.id
        }
      }
      LootKind.AMMO -> {
        primaryReserve += loot.amount
        secondaryReserve += 20
      }
      LootKind.MEDKIT -> medkits += loot.amount
      LootKind.ARMOR -> localPlayer.armor = (localPlayer.armor + loot.amount).coerceAtMost(100)
    }
    audioEngine.playSfx(GameAudioEngine.SfxType.RELOAD)
  }

  fun currentWeapon(): CatalogWeapon = when (activeSlot) {
    0 -> primaryWeapon
    1 -> secondaryWeapon
    else -> meleeWeapon
  }

  @Synchronized
  fun step(dtRaw: Float) {
    val dt = dtRaw.coerceIn(0.001f, 0.05f)
    matchTimeSec += dt
    if (hitMarkerTimer > 0f) hitMarkerTimer = max(0f, hitMarkerTimer - dt)
    if (damageFlashTimer > 0f) damageFlashTimer = max(0f, damageFlashTimer - dt)
    if (jumpCooldown > 0f) jumpCooldown = max(0f, jumpCooldown - dt)
    if (fireCooldown > 0f) fireCooldown = max(0f, fireCooldown - dt)

    // Update tracers
    val trIt = tracers.iterator()
    while (trIt.hasNext()) {
      val t = trIt.next()
      t.ttl -= dt
      if (t.ttl <= 0f) trIt.remove()
    }

    // Safe Zone shrinking
    if (!matchFinished) {
      val shrinkRate = 1.6f
      if (zoneRadius > 16f) {
        zoneRadius = max(16f, zoneRadius - shrinkRate * dt)
      }
      zonePhase = when {
        zoneRadius > 90f -> 1
        zoneRadius > 65f -> 2
        zoneRadius > 40f -> 3
        zoneRadius > 22f -> 4
        else -> 5
      }
    }

    // Reload completion
    if (reloadTimer > 0f) {
      reloadTimer -= dt
      if (reloadTimer <= 0f) {
        reloadTimer = 0f
        finishReload()
      }
    }

    // Local Player Movement / Parachute / Vehicle
    if (localPlayer.isAlive) {
      updateLocalPlayer(dt)
      // Auto-pickup ammo & medkits when walking right over them
      for (loot in lootDrops) {
        if (!loot.collected && loot.kind != LootKind.WEAPON &&
          hypot(loot.x - localPlayer.x, loot.z - localPlayer.z) < 2.2f
        ) {
          collectLoot(loot)
        }
      }
      if (isFiringHeld) {
        attemptLocalFire()
      }
    }

    // AI Bots simulation
    updateBots(dt)

    // Zone damage tick
    zoneDamageTimer += dt
    if (zoneDamageTimer >= 1.0f) {
      zoneDamageTimer = 0f
      val dmg = zonePhase * 4
      for (c in combatants) {
        if (!c.isAlive) continue
        val dist = hypot(c.x - zoneCenterX, c.z - zoneCenterZ)
        if (dist > zoneRadius) {
          applyDamage(c, dmg, null, isZone = true)
        }
      }
    }

    // Check Win / Loss condition
    if (!matchFinished && (isBotMode || combatants.size > 1)) {
      val alive = combatants.filter { it.isAlive }
      if (!localPlayer.isAlive) {
        matchFinished = true
        wonMatch = false
        finalPlacement = alive.size + 1
      } else if (alive.size == 1 && alive.first().id == localPlayer.id) {
        matchFinished = true
        wonMatch = true
        finalPlacement = 1
      }
    }
  }

  private fun updateLocalPlayer(dt: Float) {
    val yawRad = Math.toRadians(cameraYawDeg.toDouble())
    val forwardX = sin(yawRad).toFloat()
    val forwardZ = -cos(yawRad).toFloat()
    val rightX = cos(yawRad).toFloat()
    val rightZ = sin(yawRad).toFloat()

    val veh = vehicles.firstOrNull { it.id == localPlayer.vehicleId }
    if (veh != null && veh.hp > 0) {
      // Vehicle driving physics
      val speed = veh.topSpeed * (-moveInputY)
      veh.yawDeg = (veh.yawDeg + moveInputX * 110f * dt) % 360f
      val vRad = Math.toRadians(veh.yawDeg.toDouble())
      val nx = (veh.x + sin(vRad).toFloat() * speed * dt).coerceIn(-worldHalfSize, worldHalfSize)
      val nz = (veh.z - cos(vRad).toFloat() * speed * dt).coerceIn(-worldHalfSize, worldHalfSize)
      if (!collidesObstacle(nx, nz, 1.5f)) {
        veh.x = nx
        veh.z = nz
      }
      localPlayer.x = veh.x
      localPlayer.z = veh.z
      localPlayer.y = 0.6f
      localPlayer.yawDeg = veh.yawDeg
      cameraYawDeg = veh.yawDeg
      return
    }

    if (localPlayer.isParachuting) {
      val glideSpeed = 12f
      val dx = (forwardX * (-moveInputY) + rightX * moveInputX) * glideSpeed * dt
      val dz = (forwardZ * (-moveInputY) + rightZ * moveInputX) * glideSpeed * dt
      localPlayer.x = (localPlayer.x + dx).coerceIn(-worldHalfSize, worldHalfSize)
      localPlayer.z = (localPlayer.z + dz).coerceIn(-worldHalfSize, worldHalfSize)
      localPlayer.y -= 6.5f * dt
      localPlayer.yawDeg = cameraYawDeg
      if (localPlayer.y <= 0f) {
        localPlayer.y = 0f
        localPlayer.isParachuting = false
        audioEngine.playSfx(GameAudioEngine.SfxType.PARACHUTE)
      }
      return
    }

    val moveMag = min(1f, hypot(moveInputX, moveInputY))
    val baseSpeed = when {
      isCrouching -> 4.2f
      isSprinting -> 11.2f
      else -> 7.6f
    }
    if (moveMag > 0.05f) {
      val dirX = forwardX * (-moveInputY) + rightX * moveInputX
      val dirZ = forwardZ * (-moveInputY) + rightZ * moveInputX
      val len = max(0.001f, hypot(dirX, dirZ))
      val stepX = (dirX / len) * baseSpeed * moveMag * dt
      val stepZ = (dirZ / len) * baseSpeed * moveMag * dt

      val tryX = (localPlayer.x + stepX).coerceIn(-worldHalfSize, worldHalfSize)
      if (!collidesObstacle(tryX, localPlayer.z, 0.65f)) localPlayer.x = tryX
      val tryZ = (localPlayer.z + stepZ).coerceIn(-worldHalfSize, worldHalfSize)
      if (!collidesObstacle(localPlayer.x, tryZ, 0.65f)) localPlayer.z = tryZ

      localPlayer.walkCycle += dt * baseSpeed * 1.4f
      stepSfxTimer += dt
      val stepInterval = if (isSprinting) 0.30f else 0.44f
      if (stepSfxTimer >= stepInterval && localPlayer.y <= 0.05f) {
        stepSfxTimer = 0f
        audioEngine.playSfx(GameAudioEngine.SfxType.FOOTSTEP)
      }
    }
    localPlayer.yawDeg = cameraYawDeg

    // Vertical jump / gravity
    if (localPlayer.y > 0f || verticalVel > 0f) {
      localPlayer.y += verticalVel * dt
      verticalVel -= 20f * dt
      if (localPlayer.y <= 0f) {
        localPlayer.y = 0f
        verticalVel = 0f
      }
    }
  }

  private fun attemptLocalFire() {
    if (fireCooldown > 0f || reloadTimer > 0f || localPlayer.isParachuting || localPlayer.vehicleId.isNotEmpty()) return
    val w = currentWeapon()
    if (activeSlot == 0) {
      if (primaryMag <= 0) {
        triggerReload()
        return
      }
      primaryMag--
    } else if (activeSlot == 1) {
      if (secondaryMag <= 0) {
        triggerReload()
        return
      }
      secondaryMag--
    }

    fireCooldown = w.fireRateMs / 1000f
    cameraPitchDeg = (cameraPitchDeg - (w.recoil.toFloat() * 24f)).coerceIn(-12f, 48f)

    val sfx = when {
      w.category.contains("Sniper", true) -> GameAudioEngine.SfxType.SNIPER_SHOT
      w.category.contains("Shotgun", true) -> GameAudioEngine.SfxType.SHOTGUN_SHOT
      w.category.contains("SMG", true) -> GameAudioEngine.SfxType.SMG_SHOT
      else -> GameAudioEngine.SfxType.RIFLE_SHOT
    }
    audioEngine.playSfx(sfx)

    val yawRad = Math.toRadians(cameraYawDeg.toDouble())
    val pitchRad = Math.toRadians((cameraPitchDeg - 12f).toDouble())
    val dirX = (sin(yawRad) * cos(pitchRad)).toFloat()
    val dirY = (-sin(pitchRad)).toFloat()
    val dirZ = (-cos(yawRad) * cos(pitchRad)).toFloat()

    val muzzleX = localPlayer.x + sin(yawRad).toFloat() * 0.6f
    val muzzleY = localPlayer.y + (if (isCrouching) 0.95f else 1.4f)
    val muzzleZ = localPlayer.z - cos(yawRad).toFloat() * 0.6f

    val maxRange = w.rangeMeters.toFloat()
    var bestDist = maxRange
    var hitTarget: CombatantEntity? = null
    var isHeadshot = false

    for (target in combatants) {
      if (!target.isAlive || target.id == localPlayer.id) continue
      val tx = target.x - muzzleX
      val ty = (target.y + 1.15f) - muzzleY
      val tz = target.z - muzzleZ
      val proj = tx * dirX + ty * dirY + tz * dirZ
      if (proj < 0f || proj > bestDist) continue
      val closestX = muzzleX + dirX * proj
      val closestY = muzzleY + dirY * proj
      val closestZ = muzzleZ + dirZ * proj
      val perpXZ = hypot(target.x - closestX, target.z - closestZ)
      val headDist = abs((target.y + 1.72f) - closestY)
      val bodyDist = abs((target.y + 1.05f) - closestY)
      if (perpXZ < 0.85f && (headDist < 0.45f || bodyDist < 0.95f)) {
        if (!rayBlockedByObstacle(muzzleX, muzzleZ, closestX, closestZ)) {
          bestDist = proj
          hitTarget = target
          isHeadshot = headDist < 0.38f
        }
      }
    }

    val endX = muzzleX + dirX * bestDist
    val endY = (muzzleY + dirY * bestDist).coerceAtLeast(0.1f)
    val endZ = muzzleZ + dirZ * bestDist
    tracers += BulletTracer(muzzleX, muzzleY, muzzleZ, endX, endY, endZ)

    if (hitTarget != null) {
      val dmg = if (isHeadshot) (w.damage * 1.65f).toInt() else w.damage.toInt()
      hitMarkerTimer = 0.22f
      audioEngine.playSfx(GameAudioEngine.SfxType.HIT_MARKER)
      applyDamage(hitTarget, dmg, localPlayer, isHeadshot = isHeadshot)
    }
  }

  private fun updateBots(dt: Float) {
    for (bot in combatants) {
      if (!bot.isBot || !bot.isAlive) continue
      bot.aiDecisionTimer -= dt
      if (bot.aiFireCooldown > 0f) bot.aiFireCooldown -= dt

      val distToCenter = hypot(bot.x - zoneCenterX, bot.z - zoneCenterZ)
      if (bot.aiDecisionTimer <= 0f) {
        bot.aiDecisionTimer = 1.6f + Random.nextFloat() * 1.8f
        if (distToCenter > zoneRadius * 0.82f) {
          bot.aiTargetX = zoneCenterX + Random.nextFloat() * 16f - 8f
          bot.aiTargetZ = zoneCenterZ + Random.nextFloat() * 16f - 8f
        } else {
          bot.aiTargetX = (bot.x + Random.nextFloat() * 28f - 14f).coerceIn(-worldHalfSize, worldHalfSize)
          bot.aiTargetZ = (bot.z + Random.nextFloat() * 28f - 14f).coerceIn(-worldHalfSize, worldHalfSize)
        }
      }

      // Look for nearest alive target
      val nearestEnemy = combatants
        .filter { it.isAlive && it.id != bot.id }
        .minByOrNull { hypot(it.x - bot.x, it.z - bot.z) }
      val enemyDist = nearestEnemy?.let { hypot(it.x - bot.x, it.z - bot.z) } ?: 999f

      if (nearestEnemy != null && enemyDist < 42f && !nearestEnemy.isParachuting) {
        val dx = nearestEnemy.x - bot.x
        val dz = nearestEnemy.z - bot.z
        bot.yawDeg = Math.toDegrees(kotlin.math.atan2(dx.toDouble(), (-dz).toDouble())).toFloat()
        if (bot.aiFireCooldown <= 0f) {
          bot.aiFireCooldown = 0.85f + Random.nextFloat() * 0.7f
          if (!rayBlockedByObstacle(bot.x, bot.z, nearestEnemy.x, nearestEnemy.z)) {
            tracers += BulletTracer(bot.x, bot.y + 1.35f, bot.z, nearestEnemy.x, nearestEnemy.y + 1.15f, nearestEnemy.z)
            if (Random.nextFloat() < 0.42f) {
              applyDamage(nearestEnemy, (10..18).random(), bot)
            }
          }
        }
      } else {
        val dx = bot.aiTargetX - bot.x
        val dz = bot.aiTargetZ - bot.z
        val d = hypot(dx, dz)
        if (d > 1.2f) {
          val speed = 5.2f
          val nx = bot.x + (dx / d) * speed * dt
          val nz = bot.z + (dz / d) * speed * dt
          if (!collidesObstacle(nx, nz, 0.65f)) {
            bot.x = nx
            bot.z = nz
            bot.walkCycle += dt * 7f
          } else {
            bot.aiDecisionTimer = 0f
          }
          bot.yawDeg = Math.toDegrees(kotlin.math.atan2(dx.toDouble(), (-dz).toDouble())).toFloat()
        }
      }
    }
  }

  private fun applyDamage(
    victim: CombatantEntity,
    rawDmg: Int,
    attacker: CombatantEntity?,
    isHeadshot: Boolean = false,
    isZone: Boolean = false,
  ) {
    if (!victim.isAlive) return
    var remaining = rawDmg
    if (!isZone && victim.armor > 0) {
      val absorbed = min(victim.armor, (rawDmg * 0.45f).toInt().coerceAtLeast(1))
      victim.armor -= absorbed
      remaining -= absorbed
    }
    victim.hp = max(0, victim.hp - remaining)
    if (victim.id == localPlayer.id) {
      damageFlashTimer = 0.25f
    }

    if (victim.hp <= 0) {
      victim.isAlive = false
      if (attacker != null && attacker.id != victim.id) {
        attacker.kills++
        if (attacker.id == localPlayer.id) {
          audioEngine.playSfx(GameAudioEngine.SfxType.ELIMINATION)
        }
        val tag = if (isHeadshot) " [HEADSHOT]" else ""
        pushKillFeed("${attacker.name} eliminated ${victim.name}$tag", attacker.id == localPlayer.id)
      } else {
        pushKillFeed("${victim.name} succumbed to the Blue Zone", victim.id == localPlayer.id)
      }
      // Drop loot crate at victim location
      lootDrops += LootDrop(
        id = "drop_${victim.id}_${System.nanoTime()}",
        x = victim.x,
        z = victim.z,
        kind = LootKind.MEDKIT,
        name = "${victim.name} Loot Box (Medkit)",
        amount = 1,
      )
    }
  }

  private fun finishReload() {
    val w = currentWeapon()
    val maxMag = w.magazineSize.toInt()
    if (activeSlot == 0) {
      val need = maxMag - primaryMag
      val take = min(need, primaryReserve)
      primaryMag += take
      primaryReserve -= take
    } else if (activeSlot == 1) {
      val need = maxMag - secondaryMag
      val take = min(need, secondaryReserve)
      secondaryMag += take
      secondaryReserve -= take
    }
  }

  private fun pushKillFeed(msg: String, isPlayer: Boolean) {
    killFeed.addFirst(KillFeedItem("${System.nanoTime()}", msg, isPlayer))
    while (killFeed.size > 4) killFeed.removeLast()
  }

  private fun collidesObstacle(x: Float, z: Float, radius: Float): Boolean {
    for (o in obstacles) {
      if (x + radius > o.minX && x - radius < o.maxX && z + radius > o.minZ && z - radius < o.maxZ) {
        return true
      }
    }
    return false
  }

  private fun rayBlockedByObstacle(x0: Float, z0: Float, x1: Float, z1: Float): Boolean {
    val steps = 10
    for (i in 1 until steps) {
      val t = i / steps.toFloat()
      val sx = x0 + (x1 - x0) * t
      val sz = z0 + (z1 - z0) * t
      if (collidesObstacle(sx, sz, 0.1f)) return true
    }
    return false
  }

  @Synchronized
  fun getSpectateTarget(): CombatantEntity {
    if (localPlayer.isAlive) return localPlayer
    val aliveOthers = combatants.filter { it.isAlive }
    if (aliveOthers.isEmpty()) return localPlayer
    return aliveOthers[spectateIndex % aliveOthers.size]
  }

  @Synchronized
  fun snapshotHud(): HudSnapshot {
    val aliveList = combatants.filter { it.isAlive }
    val nearLoot = lootDrops.firstOrNull { !it.collected && hypot(it.x - localPlayer.x, it.z - localPlayer.z) < 4.5f }
    val nearVeh = if (localPlayer.vehicleId.isEmpty()) {
      vehicles.firstOrNull { it.hp > 0 && hypot(it.x - localPlayer.x, it.z - localPlayer.z) < 5.5f }
    } else null
    val drivingVeh = vehicles.firstOrNull { it.id == localPlayer.vehicleId }
    val distZone = hypot(localPlayer.x - zoneCenterX, localPlayer.z - zoneCenterZ)
    val blips = combatants.filter { it.isAlive && it.id != localPlayer.id }
      .map { Triple(it.x, it.z, it.isBot) }

    return HudSnapshot(
      hp = localPlayer.hp,
      maxHp = localPlayer.maxHp,
      armor = localPlayer.armor,
      kills = localPlayer.kills,
      aliveCount = aliveList.size,
      totalCount = combatants.size,
      matchElapsedSec = matchTimeSec.toInt(),
      zonePhase = zonePhase,
      zoneRadius = zoneRadius,
      zoneCenterX = zoneCenterX,
      zoneCenterZ = zoneCenterZ,
      outsideZone = distZone > zoneRadius,
      isParachuting = localPlayer.isParachuting,
      isCrouching = localPlayer.isCrouching,
      isSprinting = isSprinting,
      isAiming = isAiming,
      isReloading = reloadTimer > 0f,
      activeWeaponSlot = activeSlot,
      primaryWeapon = primaryWeapon,
      secondaryWeapon = secondaryWeapon,
      primaryAmmoInMag = primaryMag,
      primaryReserveAmmo = primaryReserve,
      secondaryAmmoInMag = secondaryMag,
      secondaryReserveAmmo = secondaryReserve,
      medkits = medkits,
      nearbyLoot = nearLoot,
      nearbyVehicle = nearVeh,
      drivingVehicle = drivingVeh,
      playerX = localPlayer.x,
      playerZ = localPlayer.z,
      playerYawDeg = localPlayer.yawDeg,
      hitMarkerTimer = hitMarkerTimer,
      damageFlashTimer = damageFlashTimer,
      killFeed = killFeed.toList(),
      isAlive = localPlayer.isAlive,
      isMatchOver = matchFinished,
      wonMatch = wonMatch,
      placement = finalPlacement,
      spectatingName = if (!localPlayer.isAlive) getSpectateTarget().name else "",
      radarBlips = blips,
    )
  }

  companion object {
    fun hexToRgb(hex: Long): FloatArray {
      val r = ((hex shr 16) and 0xFF) / 255f
      val g = ((hex shr 8) and 0xFF) / 255f
      val b = (hex and 0xFF) / 255f
      return floatArrayOf(r, g, b)
    }
  }
}
