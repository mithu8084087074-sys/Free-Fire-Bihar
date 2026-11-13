const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const ADMIN_UID = "ravi_owner_uid";
const ADMIN_EMAIL = "mithu8084087074@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function makeValidUser(uid, overrides = {}) {
  const now = new Date();
  return {
    userId: uid,
    playerId: "FFB-100001",
    username: "PatnaRanger",
    email: "player@example.com",
    role: "player",
    coins: 500,
    diamonds: 50,
    level: 1,
    xp: 0,
    rankPoints: 1000,
    kills: 0,
    wins: 0,
    matchesPlayed: 0,
    equippedCharacterId: "char_veer",
    equippedWeaponId: "wpn_ak47_bihar",
    isSuspended: false,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user cannot read or write users, catalog, or match_rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("catalog_characters").get());
  await assertFails(unauthDb.collection("match_rooms").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(makeValidUser(ALICE_UID)));
});

test("Player can create and read own profile, cannot escalate role or read Bob's private profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(makeValidUser(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Bob cannot read Alice's private user document (PII isolation)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Alice cannot list all private user profiles
  await assertFails(aliceDb.collection("users").get());

  // Alice cannot self-assign owner role or extra coins above initial cap
  await assertFails(bobDb.collection("users").doc(BOB_UID).set(makeValidUser(BOB_UID, { role: "owner" })));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ role: "owner" }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ isSuspended: true }));

  // Ghost field shadow update rejected
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ ghostFlag: true }));
});

test("Public players readable by authenticated users, writable only by owner of record or admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("public_players").doc(ALICE_UID).set({
    userId: ALICE_UID,
    playerId: "FFB-100001",
    username: "PatnaRanger",
    level: 1,
    rankPoints: 1000,
    kills: 0,
    wins: 0,
    matchesPlayed: 0,
    equippedCharacterId: "char_veer",
    isOnline: true,
    isSuspended: false,
    updatedAt: new Date(),
  }));

  await assertSucceeds(bobDb.collection("public_players").get());
  await assertFails(bobDb.collection("public_players").doc(ALICE_UID).update({ kills: 999 }));
});

test("Players can read catalog_characters/weapons/maps, ONLY verified Owner Admin can write them", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  const ownerDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();

  await assertFails(aliceDb.collection("catalog_characters").doc("char_new").set({ name: "Hacker", enabled: true }));
  await assertFails(spoofedAdminDb.collection("catalog_characters").doc("char_new").set({ name: "Spoof", enabled: true }));
  await assertSucceeds(ownerDb.collection("catalog_characters").doc("char_new").set({ name: "Veer", enabled: true }));
  await assertSucceeds(aliceDb.collection("catalog_characters").get());
});

test("Owner can suspend player and write audit_logs, regular player cannot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(makeValidUser(ALICE_UID)));
  await assertSucceeds(ownerDb.collection("users").doc(ALICE_UID).update({ isSuspended: true, suspensionReason: "Cheating" }));
  await assertSucceeds(ownerDb.collection("audit_logs").doc("log_1").set({ name: "SUSPEND_PLAYER", enabled: true }));
  await assertFails(aliceDb.collection("audit_logs").get());
});

test("Match room creation, player sync, and host controls enforce ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(aliceDb.collection("match_rooms").doc("room_1").set({
    roomCode: "FFB4821",
    hostUid: ALICE_UID,
    hostName: "PatnaRanger",
    mode: "squad",
    mapId: "map_patna_delta",
    mapName: "Patna Delta",
    status: "waiting",
    isPrivate: false,
    passwordHash: "",
    maxPlayers: 50,
    playerCount: 1,
    fillBots: false,
    safeZoneRadius: 120.0,
    safeZoneCenterX: 0.0,
    safeZoneCenterZ: 0.0,
    phase: 0,
    winnerName: "",
    createdAt: now,
    updatedAt: now,
  }));

  // Bob can join and sync his own player doc, but cannot overwrite Alice's player doc or host status
  await assertSucceeds(bobDb.collection("match_rooms").doc("room_1").collection("players").doc(BOB_UID).set({
    uid: BOB_UID,
    username: "GayaSniper",
    characterId: "char_veer",
    weaponId: "wpn_ak47_bihar",
    x: 10,
    y: 0,
    z: 10,
    yaw: 0,
    hp: 100,
    armor: 50,
    kills: 0,
    isAlive: true,
    isReady: true,
    updatedAt: now,
  }));

  await assertFails(bobDb.collection("match_rooms").doc("room_1").collection("players").doc(ALICE_UID).set({
    uid: ALICE_UID,
    username: "Hijack",
    characterId: "char_veer",
    weaponId: "wpn_ak47_bihar",
    x: 0, y: 0, z: 0, yaw: 0, hp: 0, armor: 0, kills: 0, isAlive: false, isReady: false,
  }));

  await assertFails(bobDb.collection("match_rooms").doc("room_1").update({ status: "active" }));
  await assertSucceeds(aliceDb.collection("match_rooms").doc("room_1").update({ status: "active", updatedAt: new Date() }));
});

test("Reports allow reporter creation and scoped reads only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();

  await assertSucceeds(aliceDb.collection("reports").doc("rep_1").set({
    reporterUid: ALICE_UID,
    targetPlayerId: "FFB-999999",
    reason: "Aimbot",
    details: "Headshots through wall",
    status: "open",
    createdAt: new Date(),
  }));

  await assertSucceeds(aliceDb.collection("reports").where("reporterUid", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("reports").get());
  await assertSucceeds(ownerDb.collection("reports").get());
  await assertSucceeds(ownerDb.collection("reports").doc("rep_1").update({ status: "resolved" }));
});
