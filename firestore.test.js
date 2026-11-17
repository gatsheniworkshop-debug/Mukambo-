const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_harare_123";
const BOB_UID = "bob_bulawayo_456";
const CHARLIE_UID = "charlie_mutare_789";
const ADMIN_UID = "admin_mukambo_001";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function sampleListing(sellerId, overrides = {}) {
  return {
    sellerId,
    sellerName: "Tatenda Moyo",
    title: "5kVA Hybrid Solar Inverter",
    description: "Clean 48V pure sine wave hybrid inverter in Borrowdale.",
    category: "Solar & Power",
    condition: "Like New",
    priceUsd: 420.0,
    currency: "USD",
    province: "Harare Metropolitan",
    city: "Harare",
    suburb: "Borrowdale",
    latitude: -17.756,
    longitude: 31.088,
    status: "active",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

async function seedAdminDoc(uid) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("admins").doc(uid).set({ role: "admin" });
  });
}

test("Unauthenticated user cannot read or list any protected collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("listings").where("status", "==", "active").get());
  await assertFails(unauthDb.collection("orders").where("buyerId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("chats").where("participantIds", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("reports").where("reporterId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("reviews").where("status", "==", "published").get());
});

test("User profile PII isolation: owner can create/read, other user cannot read", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Harare",
      email: "alice@example.co.zw",
      phoneNumber: "+263771234567",
      defaultCity: "Harare",
      defaultProvince: "Harare Metropolitan",
      preferredCurrency: "USD",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Seller verification badge: user cannot self-verify, only admin can approve", async () => {
  await seedAdminDoc(ADMIN_UID);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Self-creating as 'verified' fails
  await assertFails(
    aliceDb.collection("sellerProfiles").doc(ALICE_UID).set({
      sellerId: ALICE_UID,
      displayName: "Alice Solar",
      city: "Harare",
      verificationStatus: "verified",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Creating as 'unverified' succeeds
  await assertSucceeds(
    aliceDb.collection("sellerProfiles").doc(ALICE_UID).set({
      sellerId: ALICE_UID,
      displayName: "Alice Solar",
      city: "Harare",
      province: "Harare Metropolitan",
      verificationStatus: "unverified",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Owner can request 'pending'
  await assertSucceeds(
    aliceDb.collection("sellerProfiles").doc(ALICE_UID).update({
      businessName: "Alice Solar Pvt Ltd",
      verificationStatus: "pending",
      verificationNote: "Registered Harare solar trader",
      updatedAt: serverTimestamp(),
    })
  );

  // Owner cannot jump from pending -> verified
  await assertFails(
    aliceDb.collection("sellerProfiles").doc(ALICE_UID).update({
      verificationStatus: "verified",
      updatedAt: serverTimestamp(),
    })
  );

  // Admin can approve verification badge
  await assertSucceeds(
    adminDb.collection("sellerProfiles").doc(ALICE_UID).update({
      verificationStatus: "verified",
      verificationBadge: "Verified Merchant",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Buyer-seller chat: only participants can read/send messages; non-participants denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("listings").doc("listing_chat_1").set(sampleListing(ALICE_UID))
  );

  const chatId = `listing_chat_1_${BOB_UID}`;
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).set({
      listingId: "listing_chat_1",
      listingTitle: "5kVA Hybrid Solar Inverter",
      listingPriceUsd: 420.0,
      buyerId: BOB_UID,
      buyerName: "Bob Bulawayo",
      sellerId: ALICE_UID,
      sellerName: "Tatenda Moyo",
      participantIds: [BOB_UID, ALICE_UID],
      lastMessage: "Is this still available?",
      lastSenderId: BOB_UID,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_1").set({
      chatId,
      senderId: BOB_UID,
      senderName: "Bob Bulawayo",
      text: "Is this still available?",
      participantIds: [BOB_UID, ALICE_UID],
      createdAt: serverTimestamp(),
    })
  );

  // Seller Alice can query messages using participantIds filter
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages")
      .where("participantIds", "array-contains", ALICE_UID).get()
  );

  // Non-participant Charlie cannot read chat or post messages
  await assertFails(charlieDb.collection("chats").doc(chatId).get());
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_spoof").set({
      chatId,
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      text: "Hijack",
      participantIds: [BOB_UID, ALICE_UID],
      createdAt: serverTimestamp(),
    })
  );
});

test("Order tracking, escrow release, terminal lock, and verified seller review", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("listings").doc("solar_1").set(sampleListing(ALICE_UID))
  );

  const batch = bobDb.batch();
  batch.update(bobDb.collection("listings").doc("solar_1"), {
    status: "sold",
    buyerId: BOB_UID,
    updatedAt: serverTimestamp(),
  });
  batch.set(bobDb.collection("orders").doc("order_1"), {
    listingId: "solar_1",
    listingTitle: "5kVA Hybrid Solar Inverter",
    buyerId: BOB_UID,
    buyerName: "Bob Bulawayo",
    sellerId: ALICE_UID,
    sellerName: "Tatenda Moyo",
    amountUsd: 420.0,
    currency: "USD",
    paymentGateway: "EcoCash",
    maskedAccount: "+26377****890",
    gatewayReference: "MK-EC-998811",
    deliveryCity: "Bulawayo",
    deliveryMethod: "Courier Delivery",
    escrowStatus: "escrow_held",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  await assertSucceeds(batch.commit());

  // Cannot review before escrow is released
  await assertFails(
    bobDb.collection("reviews").doc("order_1_" + BOB_UID).set({
      orderId: "order_1",
      listingId: "solar_1",
      listingTitle: "5kVA Hybrid Solar Inverter",
      sellerId: ALICE_UID,
      reviewerId: BOB_UID,
      reviewerName: "Bob Bulawayo",
      rating: 5,
      comment: "Great solar kit!",
      status: "published",
      createdAt: serverTimestamp(),
    })
  );

  // Seller advances tracking: dispatched -> in_transit
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_1").update({
      escrowStatus: "dispatched",
      courierName: "Swift Freight",
      trackingCode: "SW-88291",
      estimatedDelivery: "24 hours",
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("orders").doc("order_1").update({
      escrowStatus: "in_transit",
      trackingNotes: "Departed Harare depot for Bulawayo",
      updatedAt: serverTimestamp(),
    })
  );

  // Buyer releases escrow
  await assertSucceeds(
    bobDb.collection("orders").doc("order_1").update({
      escrowStatus: "released",
      updatedAt: serverTimestamp(),
    })
  );

  // Now buyer can submit verified seller review
  await assertSucceeds(
    bobDb.collection("reviews").doc("order_1_" + BOB_UID).set({
      orderId: "order_1",
      listingId: "solar_1",
      listingTitle: "5kVA Hybrid Solar Inverter",
      sellerId: ALICE_UID,
      reviewerId: BOB_UID,
      reviewerName: "Bob Bulawayo",
      rating: 5,
      comment: "Clean inverter, fast delivery to Bulawayo!",
      status: "published",
      createdAt: serverTimestamp(),
    })
  );

  // Terminal state lock: once released, no further updates allowed
  await assertFails(
    bobDb.collection("orders").doc("order_1").update({
      escrowStatus: "disputed",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Product reporting & Admin moderation: non-admin cannot resolve reports or remove others' listings", async () => {
  await seedAdminDoc(ADMIN_UID);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("listings").doc("listing_rep_1").set(sampleListing(ALICE_UID))
  );

  await assertSucceeds(
    bobDb.collection("reports").doc("report_1").set({
      listingId: "listing_rep_1",
      listingTitle: "5kVA Hybrid Solar Inverter",
      sellerId: ALICE_UID,
      reporterId: BOB_UID,
      reporterName: "Bob Bulawayo",
      reason: "Misleading Price or Photos",
      details: "Price listed does not match description",
      status: "open",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Non-admin cannot resolve report or remove listing
  await assertFails(
    bobDb.collection("reports").doc("report_1").update({
      status: "resolved",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    bobDb.collection("listings").doc("listing_rep_1").update({
      status: "removed",
      updatedAt: serverTimestamp(),
    })
  );

  // Admin can remove listing and resolve report
  await assertSucceeds(
    adminDb.collection("listings").doc("listing_rep_1").update({
      status: "removed",
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    adminDb.collection("reports").doc("report_1").update({
      status: "resolved",
      adminNotes: "Removed misleading listing",
      updatedAt: serverTimestamp(),
    })
  );
});
