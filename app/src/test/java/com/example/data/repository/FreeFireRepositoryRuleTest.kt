package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CatalogCharacter
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FreeFireRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensurePlayerProfile_authenticatedUser_createsProfileAndStarterLoadout() = runBlocking {
    val uid = signInTestUser("player_${UUID.randomUUID().toString().take(8)}@test.com")
    val repo = PlayerRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensurePlayerProfile(uid, "PatnaHero", "player@test.com", null)
    }
    assertTrue("Expected profile creation success: ${result.exceptionOrNull()}", result.isSuccess)
    val profile = result.getOrThrow()
    assertEquals(uid, profile.userId)
    assertEquals("PatnaHero", profile.username)
    assertEquals("player", profile.role)

    val inventory = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeInventory(uid).first { it.isNotEmpty() }
    }
    assertTrue(inventory.any { it.itemId == "char_veer" })
  }

  @Test
  fun createAndJoinMatchRoom_authenticatedPlayers_succeedsAndSyncsState() = runBlocking {
    val hostUid = signInTestUser("host_${UUID.randomUUID().toString().take(8)}@test.com")
    val playerRepo = PlayerRepository(firestore)
    val matchRepo = MatchmakingRepository(firestore)
    val hostProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
      playerRepo.ensurePlayerProfile(hostUid, "HostVeer", "host@test.com", null).getOrThrow()
    }

    val roomId = withTimeout(DEFAULT_TIMEOUT_MS) {
      matchRepo.createRoom(
        profile = hostProfile,
        mode = "squad",
        mapId = "map_patna_delta",
        mapName = "Patna Delta",
        isPrivate = false,
        passwordPlain = "",
        maxPlayers = 50L,
        fillBots = false,
      ).getOrThrow()
    }
    assertTrue(roomId.isNotBlank())

    val room = withTimeout(FLOW_TIMEOUT_MS) {
      matchRepo.observeRoom(roomId).first { it != null }
    }
    assertEquals(hostUid, room?.hostUid)

    val guestUid = signInTestUser("guest_${UUID.randomUUID().toString().take(8)}@test.com")
    val guestProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
      playerRepo.ensurePlayerProfile(guestUid, "GuestTara", "guest@test.com", null).getOrThrow()
    }
    val joinResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      matchRepo.joinRoom(room!!, guestProfile)
    }
    assertTrue("Guest join failed: ${joinResult.exceptionOrNull()}", joinResult.isSuccess)
  }

  @Test
  fun ownerAdminCatalogWrite_nonAdminPlayer_failsWithPermissionDenied() = runBlocking {
    signInTestUser("regular_${UUID.randomUUID().toString().take(8)}@test.com")
    val adminRepo = OwnerAdminRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      adminRepo.saveCharacter(
        CatalogCharacter(id = "char_illegal_${UUID.randomUUID().toString().take(6)}", name = "Unauthorized")
      )
    }
    assertTrue("Regular player should not be allowed to write catalog_characters", result.isFailure)
    val fsErr = result.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
  }

  @Test
  fun observeInventory_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = PlayerRepository(firestore)
    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeInventory("some_uid_123").first()
      }
    } catch (e: Exception) {
      caught = e
    }
    val fsErr = caught?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 6000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
