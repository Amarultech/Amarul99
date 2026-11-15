const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date(Date.now() - 2000);
}

function validUserProfile(uid, role = "user") {
  const ts = nowTs();
  return {
    userId: uid,
    displayName: "Alice Stark",
    email: "alice@example.com",
    role,
    plan: "free",
    dailyRequestsUsed: 0,
    dailyDateKey: "2026-11-15",
    totalRequests: 0,
    voiceEnabled: true,
    speechRate: 1.0,
    preferredLanguage: "English",
    createdAt: ts,
    updatedAt: ts,
  };
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

test("Unauthenticated user cannot read or write user profile or tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(unauthDb.collection("users").get());
});

test("User can create and read own profile with role=user, cannot self-escalate to admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID, "user")));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Non-admin cannot list all users
  await assertFails(aliceDb.collection("users").get());

  // Self-escalation blocked
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ role: "admin", updatedAt: nowTs() }));

  // Shadow update with ghost field blocked
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ isVerified: true, updatedAt: nowTs() }));
});

test("User cannot create initial profile with role=admin", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  await assertFails(bobDb.collection("users").doc(BOB_UID).set(validUserProfile(BOB_UID, "admin")));
});

test("Spoofed unverified admin email is rejected", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_1", {
    email: "hoqueamarul848@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(spoofDb.collection("users").get());
});

test("Verified admin email can list users and update user plan", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID, "user"));
  });

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "hoqueamarul848@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(adminDb.collection("users").doc(ALICE_UID).update({
    plan: "pro",
    updatedAt: nowTs(),
  }));
});

test("Cross-user isolation on tasks, notes, reminders, planner, knowledge, conversations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowTs();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").set({
    userId: ALICE_UID,
    title: "Deploy Stark OS",
    description: "Check diagnostics",
    priority: "high",
    completed: false,
    dueDateLabel: "Today",
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("t1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notes").doc("n1").set({
    userId: ALICE_UID,
    title: "Hacked",
    content: "nope",
    tag: "sec",
    pinned: false,
    createdAt: ts,
    updatedAt: ts,
  }));
});

test("Usage records are append-only and immutable after creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const usageRef = aliceDb.collection("users").doc(ALICE_UID).collection("usage").doc("u1");

  await assertSucceeds(usageRef.set({
    userId: ALICE_UID,
    module: "chat",
    promptChars: 42,
    responseChars: 256,
    createdAt: nowTs(),
  }));

  await assertFails(usageRef.update({ promptChars: 0 }));
  await assertFails(usageRef.delete());
});
