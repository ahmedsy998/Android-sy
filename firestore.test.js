const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

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

test("Unauthenticated users are denied all access", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("devices").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("activity_logs")
      .get()
  );
});

test("User profile: owner CRUD works, PII isolated, ghost fields rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Sentinel",
      email: "alice@example.com",
      biometricEnabled: true,
      sessionTimeoutMinutes: 30,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      sessionTimeoutMinutes: 60,
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user PII read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: pastDate(500),
    })
  );

  // Ghost field & immutable createdAt mutation rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Future",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Enrolled devices: owner create, permission toggle, revocation lock, and cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("devices")
      .doc("dev_001")
      .set({
        userId: ALICE_UID,
        deviceAlias: "Pixel 9 Pro Ops",
        deviceModel: "Google Pixel 9 Pro",
        androidVersion: "Android 15",
        enrollmentCode: "SNTL-8942",
        connectionStatus: "ONLINE",
        consentVerified: true,
        cameraAllowed: false,
        screenShareAllowed: true,
        mediaPickerAllowed: true,
        filesAllowed: true,
        enrolledAt: now,
        lastActivityAt: now,
        updatedAt: now,
      })
  );

  // List subcollection succeeds for owner, fails for Bob
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("devices").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("devices").get()
  );

  // Reject document body containing @DocumentId 'id'
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("devices")
      .doc("dev_002")
      .set({
        id: "dev_002",
        userId: ALICE_UID,
        deviceAlias: "Bad Device",
        deviceModel: "Pixel",
        androidVersion: "Android 15",
        enrollmentCode: "SNTL-1111",
        connectionStatus: "ONLINE",
        consentVerified: true,
        cameraAllowed: false,
        screenShareAllowed: false,
        mediaPickerAllowed: true,
        filesAllowed: false,
        enrolledAt: now,
        lastActivityAt: now,
        updatedAt: now,
      })
  );

  // Permission update succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("devices")
      .doc("dev_001")
      .update({
        cameraAllowed: true,
        updatedAt: pastDate(1000),
      })
  );

  // Revoke device succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("devices")
      .doc("dev_001")
      .update({
        connectionStatus: "REVOKED",
        cameraAllowed: false,
        screenShareAllowed: false,
        mediaPickerAllowed: false,
        filesAllowed: false,
        updatedAt: pastDate(500),
      })
  );

  // Once REVOKED, cannot re-enable online status
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("devices")
      .doc("dev_001")
      .update({
        connectionStatus: "ONLINE",
        updatedAt: pastDate(100),
      })
  );
});

test("Enrollment requests: terminal status locking prevents tampering after decision", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate(5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("enrollments")
      .doc("enr_001")
      .set({
        userId: ALICE_UID,
        enrollmentCode: "SNTL-4021",
        targetAlias: "Field Tablet",
        requestedPermissions: ["SCREEN_SHARE", "MEDIA_PICKER"],
        status: "PENDING",
        deviceModel: "Galaxy Tab S9",
        androidVersion: "Android 14",
        createdAt: now,
        updatedAt: now,
      })
  );

  // Approve pending request succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("enrollments")
      .doc("enr_001")
      .update({
        status: "APPROVED",
        updatedAt: pastDate(1000),
      })
  );

  // Terminal state lock blocks re-updating APPROVED request
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("enrollments")
      .doc("enr_001")
      .update({
        status: "REJECTED",
        updatedAt: pastDate(500),
      })
  );
});

test("Monitoring sessions: requires existing parent device and enforces TERMINATED lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate(6000);

  // Orphan session without enrolled device fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_orphan")
      .set({
        userId: ALICE_UID,
        deviceId: "missing_dev",
        deviceAlias: "Ghost",
        sessionType: "SCREEN_SHARE",
        status: "ACTIVE",
        participantName: "Alice",
        connectionQuality: "EXCELLENT",
        consentGranted: true,
        activePermissions: ["SCREEN_SHARE"],
        sharedItemSummary: "",
        startedAt: now,
        updatedAt: now,
      })
  );

  // Create enrolled device first
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("devices")
      .doc("dev_live")
      .set({
        userId: ALICE_UID,
        deviceAlias: "Pixel 9",
        deviceModel: "Google Pixel 9",
        androidVersion: "Android 15",
        enrollmentCode: "SNTL-7777",
        connectionStatus: "ONLINE",
        consentVerified: true,
        cameraAllowed: true,
        screenShareAllowed: true,
        mediaPickerAllowed: true,
        filesAllowed: true,
        enrolledAt: now,
        lastActivityAt: now,
        updatedAt: now,
      })
  );

  // Session create succeeds when parent device exists
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_001")
      .set({
        userId: ALICE_UID,
        deviceId: "dev_live",
        deviceAlias: "Pixel 9",
        sessionType: "SCREEN_SHARE",
        status: "ACTIVE",
        participantName: "Alice",
        connectionQuality: "EXCELLENT",
        consentGranted: true,
        activePermissions: ["SCREEN_SHARE"],
        sharedItemSummary: "Display stream active",
        startedAt: now,
        updatedAt: now,
      })
  );

  // Pause succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_001")
      .update({
        status: "PAUSED",
        updatedAt: pastDate(2000),
      })
  );

  // Terminate succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_001")
      .update({
        status: "TERMINATED",
        connectionQuality: "OFFLINE",
        activePermissions: [],
        updatedAt: pastDate(1000),
        endedAt: pastDate(1000),
      })
  );

  // Reopening a TERMINATED session fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_001")
      .update({
        status: "ACTIVE",
        updatedAt: pastDate(500),
      })
  );
});

test("Activity logs: append-only immutable audit trail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(3000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("activity_logs")
      .doc("log_001")
      .set({
        userId: ALICE_UID,
        category: "CONSENT",
        action: "Screen Share Approved",
        details: "Owner approved MediaProjection session on Pixel 9.",
        severity: "SUCCESS",
        deviceId: "dev_live",
        sessionId: "sess_001",
        createdAt: now,
      })
  );

  // Updating audit logs is strictly forbidden
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("activity_logs")
      .doc("log_001")
      .update({
        details: "Tampered audit log",
      })
  );

  // Cross-user read denied
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("activity_logs")
      .doc("log_001")
      .get()
  );
});
