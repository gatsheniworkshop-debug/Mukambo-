package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.PaymentGatewayType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class MukamboRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createListingAndObserveActiveListings_succeedsForAuthenticatedSeller() = runBlocking {
        signInTestUser("seller_${UUID.randomUUID().toString().take(6)}@mukambo.co.zw")
        val repo = MukamboRepository(firestore, auth)

        val title = "Solar Panel 550W Monocrystalline ${UUID.randomUUID().toString().take(4)}"
        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createListing(
                sellerName = "Rudo Moyo",
                title = title,
                description = "Brand new Tier-1 monocrystalline solar panel in Harare.",
                category = "Solar & Power",
                condition = "New",
                priceUsd = 140.0,
                currency = "USD",
                province = "Harare Metropolitan",
                city = "Harare",
                suburb = "Msasa",
                latitude = -17.8380,
                longitude = 31.1120
            )
        }
        assertTrue("Expected createListing to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val listingId = createResult.getOrThrow()

        val activeListings = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeActiveListings().first { list -> list.any { it.id == listingId } }
        }
        assertTrue(activeListings.any { it.id == listingId && it.title == title })
    }

    @Test
    fun checkoutWithEscrow_tracking_review_andTerminalLock_succeeds() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6)
        val sellerEmail = "seller_$suffix@mukambo.co.zw"
        val buyerEmail = "buyer_$suffix@mukambo.co.zw"

        signInTestUser(sellerEmail)
        val sellerRepo = MukamboRepository(firestore, auth)
        val listingId = withTimeout(DEFAULT_TIMEOUT_MS) {
            sellerRepo.createListing(
                sellerName = "Kuda Ndlovu",
                title = "Borehole Pump Kit $suffix",
                description = "Clean submersible pump tested in Bulawayo.",
                category = "Agriculture",
                condition = "Like New",
                priceUsd = 260.0,
                currency = "USD",
                province = "Bulawayo Metropolitan",
                city = "Bulawayo",
                suburb = "Hillside",
                latitude = -20.1945,
                longitude = 28.6105
            ).getOrThrow()
        }
        val createdListing = withTimeout(DEFAULT_TIMEOUT_MS) {
            sellerRepo.getListing(listingId).getOrThrow()
        }
        assertNotNull(createdListing)

        signInTestUser(buyerEmail)
        val buyerRepo = MukamboRepository(firestore, auth)
        val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            buyerRepo.checkoutWithEscrow(
                listing = createdListing!!,
                buyerName = "Tendai Sibanda",
                gateway = PaymentGatewayType.ECOCASH,
                maskedAccount = "+26377****567",
                deliveryCity = "Harare",
                deliveryMethod = "Courier Delivery",
                currency = "USD"
            ).getOrThrow()
        }
        assertTrue(orderId.isNotBlank())

        // Seller marks dispatched with courier tracking
        signInTestUser(sellerEmail)
        val dispatchResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            sellerRepo.markOrderDispatched(
                orderId = orderId,
                courierName = "Swift Freight Zimbabwe",
                trackingCode = "SW-$suffix",
                estimatedDelivery = "Next business day"
            )
        }
        assertTrue("Expected dispatch to succeed: ${dispatchResult.exceptionOrNull()}", dispatchResult.isSuccess)

        // Seller updates tracking to in_transit
        val transitResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            sellerRepo.updateOrderTracking(
                orderId = orderId,
                nextStatus = "in_transit",
                trackingNotes = "Left Bulawayo hub"
            )
        }
        assertTrue("Expected in_transit update to succeed: ${transitResult.exceptionOrNull()}", transitResult.isSuccess)

        // Buyer releases escrow
        signInTestUser(buyerEmail)
        val releaseResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            buyerRepo.releaseEscrow(orderId)
        }
        assertTrue("Expected releaseEscrow to succeed: ${releaseResult.exceptionOrNull()}", releaseResult.isSuccess)

        // Buyer submits seller review for released order
        val orderSnap = withTimeout(DEFAULT_TIMEOUT_MS) {
            buyerRepo.observeBuyerOrders(auth.currentUser!!.uid).first { list -> list.any { it.id == orderId } }
                .first { it.id == orderId }
        }
        val reviewResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            buyerRepo.submitSellerReview(
                order = orderSnap,
                reviewerName = "Tendai Sibanda",
                rating = 5,
                comment = "Pump arrived in Harare in top condition!"
            )
        }
        assertTrue("Expected submitSellerReview to succeed: ${reviewResult.exceptionOrNull()}", reviewResult.isSuccess)

        // Terminal lock prevents subsequent dispute after release
        val disputeAfterRelease = withTimeout(DEFAULT_TIMEOUT_MS) {
            buyerRepo.disputeOrder(orderId)
        }
        assertTrue(disputeAfterRelease.isFailure)
        val fsEx = disputeAfterRelease.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun buyerSellerChat_andProductReporting_succeedsForParticipantsAndDeniesOutsiders() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6)
        val sellerEmail = "chat_seller_$suffix@mukambo.co.zw"
        val buyerEmail = "chat_buyer_$suffix@mukambo.co.zw"
        val outsiderEmail = "chat_outsider_$suffix@mukambo.co.zw"

        signInTestUser(sellerEmail)
        val sellerRepo = MukamboRepository(firestore, auth)
        val listingId = withTimeout(DEFAULT_TIMEOUT_MS) {
            sellerRepo.createListing(
                sellerName = "Nyasha Chikwanda",
                title = "Mukwa Coffee Table $suffix",
                description = "Handcrafted timber table in Mutare.",
                category = "Home & Furniture",
                condition = "New",
                priceUsd = 180.0,
                currency = "USD",
                province = "Manicaland",
                city = "Mutare",
                suburb = "Murambi",
                latitude = -18.9600,
                longitude = 32.6850
            ).getOrThrow()
        }
        val listing = withTimeout(DEFAULT_TIMEOUT_MS) { sellerRepo.getListing(listingId).getOrThrow() }!!

        val buyerUid = signInTestUser(buyerEmail)
        val buyerRepo = MukamboRepository(firestore, auth)
        val chatId = withTimeout(DEFAULT_TIMEOUT_MS) {
            buyerRepo.startOrGetChat(listing, "Farai Moyo").getOrThrow()
        }

        val chats = withTimeout(FLOW_TIMEOUT_MS) {
            buyerRepo.observeUserChats(buyerUid).first { list -> list.any { it.id == chatId } }
        }
        val thread = chats.first { it.id == chatId }

        val sendResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            buyerRepo.sendChatMessage(thread, "Farai Moyo", "Can we meet in Mutare CBD?")
        }
        assertTrue("Expected sendChatMessage to succeed: ${sendResult.exceptionOrNull()}", sendResult.isSuccess)

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            buyerRepo.observeChatMessages(chatId, buyerUid).first { it.size >= 2 }
        }
        assertTrue(messages.any { it.text.contains("Mutare CBD") })

        // Buyer submits product report
        val reportResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            buyerRepo.submitListingReport(
                listing = listing,
                reporterName = "Farai Moyo",
                reason = "Misleading Price or Photos",
                details = "Confirming actual price in ZIG vs USD"
            )
        }
        assertTrue("Expected submitListingReport to succeed: ${reportResult.exceptionOrNull()}", reportResult.isSuccess)

        // Outsider cannot read buyer's chat messages
        val outsiderUid = signInTestUser(outsiderEmail)
        val outsiderRepo = MukamboRepository(firestore, auth)
        val outsiderMessages = withTimeout(FLOW_TIMEOUT_MS) {
            outsiderRepo.observeChatMessages(chatId, outsiderUid).first()
        }
        assertTrue("Outsider should receive 0 messages", outsiderMessages.isEmpty())
    }

    @Test
    fun observeListings_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = MukamboRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeActiveListings().first()
            }
            fail("Expected unauthenticated query to fail")
        } catch (e: Exception) {
            val isIllegalState = e is IllegalStateException
            val fsEx = e.unwrapFirestoreException()
            assertTrue(
                "Expected IllegalStateException or PERMISSION_DENIED, got $e",
                isIllegalState || fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
        }
    }

    @Test
    fun userProfileAndFavorites_crossUserAccessDenied() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6)
        val aliceUid = signInTestUser("alice_$suffix@mukambo.co.zw")
        val aliceRepo = MukamboRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserProfile("Alice Harare", "alice_$suffix@mukambo.co.zw").getOrThrow()
        }

        // Bob tries to read Alice's private user profile directly
        signInTestUser("bob_$suffix@mukambo.co.zw")
        val bobRepo = MukamboRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeUserProfile(aliceUid).first()
            }
            fail("Expected cross-user profile read to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
