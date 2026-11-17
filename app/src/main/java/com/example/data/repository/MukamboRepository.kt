package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.FavoriteItem
import com.example.data.model.Listing
import com.example.data.model.ListingReport
import com.example.data.model.MarketplaceConstants
import com.example.data.model.Order
import com.example.data.model.PaymentGatewayType
import com.example.data.model.SellerProfile
import com.example.data.model.SellerReview
import com.example.data.model.UserProfile
import com.example.data.model.ZimbabweLocations
import com.example.data.model.buildCreateListingPayload
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.google.firebase.messaging.messaging
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

data class ImageUploadResult(
    val imageUrls: List<String>,
    val localImageNames: List<String>,
    val usedLocalFallback: Boolean
)

class MukamboRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.logFirestoreErrors(op: OperationType, path: String, expectedUid: String? = null): Flow<T> {
        return catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
                handleFirestoreError(exceptionToLog, op, path, auth)
            }
            throw error
        }
    }

    // --- Listings ---

    fun observeActiveListings(): Flow<List<Listing>> = flow {
        val uid = requireUserId()
        val path = "listings"
        emitAll(
            db.collection(path)
                .whereEqualTo("status", "active")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Listing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMyListings(sellerId: String): Flow<List<Listing>> = flow {
        val path = "listings"
        emitAll(
            db.collection(path)
                .whereEqualTo("sellerId", sellerId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Listing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, sellerId)
        )
    }

    fun observeListing(listingId: String): Flow<Listing?> = flow {
        val uid = requireUserId()
        val path = "listings/$listingId"
        emitAll(
            db.collection("listings").document(listingId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            Listing::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .logFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    suspend fun createListing(
        sellerName: String,
        title: String,
        description: String,
        category: String,
        condition: String,
        priceUsd: Double,
        currency: String,
        province: String? = null,
        city: String,
        suburb: String,
        latitude: Double,
        longitude: Double,
        localImageName: String? = null,
        localImageNames: List<String> = emptyList(),
        imageUrls: List<String> = emptyList()
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("listings").document()
        val payload = buildCreateListingPayload(
            sellerId = uid,
            sellerName = sellerName,
            title = title,
            description = description,
            category = category,
            condition = condition,
            priceUsd = priceUsd,
            currency = currency,
            province = province,
            city = city,
            suburb = suburb,
            latitude = latitude,
            longitude = longitude,
            localImageName = localImageName,
            localImageNames = localImageNames,
            imageUrls = imageUrls
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun getListing(listingId: String): Result<Listing?> {
        val path = "listings/$listingId"
        return try {
            val snap = db.collection("listings").document(listingId).get().await()
            val item = if (snap.exists()) {
                snap.toObject(Listing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteListing(listingId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("listings").document(listingId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Firebase Storage Product Image Upload (with local fallback) ---

    suspend fun uploadProductImages(context: Context, uris: List<Uri>): ImageUploadResult =
        withContext(Dispatchers.IO) {
            val uid = requireUserId()
            val selectedUris = uris.take(4)
            val remoteUrls = mutableListOf<String>()
            val localNames = mutableListOf<String>()
            var fallbackUsed = false

            for (uri in selectedUris) {
                val compressedBytes = compressUriToJpeg(context, uri) ?: continue
                val filename = "listing_${UUID.randomUUID().toString().take(12)}.jpg"
                val localFile = File(context.filesDir, filename)
                localFile.writeBytes(compressedBytes)
                localNames.add(filename)

                try {
                    val storageRef = Firebase.storage.reference
                        .child("listings/$uid/$filename")
                    withTimeout(12_000L) {
                        storageRef.putBytes(compressedBytes).await()
                        val downloadUrl = storageRef.downloadUrl.await().toString()
                        remoteUrls.add(downloadUrl)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Firebase Storage upload skipped/failed, using local image fallback: ${e.message}")
                    fallbackUsed = true
                }
            }

            ImageUploadResult(
                imageUrls = remoteUrls,
                localImageNames = localNames,
                usedLocalFallback = fallbackUsed
            )
        }

    private fun compressUriToJpeg(context: Context, uri: Uri): ByteArray? {
        return try {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return rawBytes
            val maxDim = 1280
            val ratio = minOf(
                maxDim.toFloat() / decoded.width.coerceAtLeast(1),
                maxDim.toFloat() / decoded.height.coerceAtLeast(1),
                1f
            )
            val scaled = if (ratio < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * ratio).toInt().coerceAtLeast(1),
                    (decoded.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            } else decoded
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
            if (scaled != decoded) scaled.recycle()
            decoded.recycle()
            out.toByteArray()
        } catch (e: Exception) {
            Log.w(TAG, "Image compression failed: ${e.message}")
            null
        }
    }

    // --- User Profile, Favorites, Notifications & FCM ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .logFirestoreErrors(OperationType.GET, path, userId)
        )
    }

    fun observeFavorites(userId: String): Flow<List<FavoriteItem>> = flow {
        val path = "users/$userId/favorites"
        emitAll(
            db.collection("users").document(userId).collection("favorites")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FavoriteItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AppNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    suspend fun ensureUserProfile(displayName: String, email: String?): Result<Unit> {
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        val sellerDocRef = db.collection("sellerProfiles").document(uid)
        return try {
            val safeName = displayName.ifBlank { "Mukambo User" }.take(60)
            val existingUser = userDocRef.get().await()
            if (!existingUser.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "defaultCity" to "Harare",
                    "defaultProvince" to "Harare Metropolitan",
                    "preferredCurrency" to "USD",
                    "notificationsEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!email.isNullOrBlank()) {
                    payload["email"] = email.take(120)
                }
                userDocRef.set(payload).await()
            }
            val existingSeller = sellerDocRef.get().await()
            if (!existingSeller.exists()) {
                sellerDocRef.set(
                    mapOf(
                        "sellerId" to uid,
                        "displayName" to safeName,
                        "city" to "Harare",
                        "province" to "Harare Metropolitan",
                        "verificationStatus" to "unverified",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun syncFcmToken(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        return try {
            val token = Firebase.messaging.token.await()
            if (token.isNotBlank()) {
                db.collection("users").document(uid)
                    .update(
                        mapOf(
                            "fcmToken" to token.take(512),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "FCM token registration skipped: ${e.message}")
            Result.success(Unit)
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        phoneNumber: String?,
        defaultCity: String,
        preferredCurrency: String,
        notificationsEnabled: Boolean = true
    ): Result<Unit> {
        val uid = requireUserId()
        val userDocRef = db.collection("users").document(uid)
        val sellerDocRef = db.collection("sellerProfiles").document(uid)
        val province = ZimbabweLocations.findCity(defaultCity).province
        val cleanName = displayName.trim().ifBlank { "Mukambo User" }.take(60)
        return try {
            val updates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "defaultCity" to defaultCity,
                "defaultProvince" to province,
                "preferredCurrency" to preferredCurrency,
                "notificationsEnabled" to notificationsEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!phoneNumber.isNullOrBlank()) {
                updates["phoneNumber"] = phoneNumber.trim().take(24)
            }
            userDocRef.update(updates).await()

            val sellerSnap = sellerDocRef.get().await()
            if (sellerSnap.exists()) {
                sellerDocRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "city" to defaultCity,
                        "province" to province,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(listing: Listing, isCurrentlyFavorite: Boolean): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("favorites").document(listing.id)
        return try {
            if (isCurrentlyFavorite) {
                docRef.delete().await()
            } else {
                val payload = mutableMapOf<String, Any>(
                    "listingId" to listing.id,
                    "title" to listing.title.take(100),
                    "priceUsd" to listing.priceUsd,
                    "city" to listing.city.take(40),
                    "category" to listing.category.take(40),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                val prov = listing.province ?: ZimbabweLocations.findCity(listing.city).province
                payload["province"] = prov.take(40)
                val primaryUrl = listing.imageUrls.firstOrNull()
                if (!primaryUrl.isNullOrBlank()) {
                    payload["imageUrl"] = primaryUrl.take(600)
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun sendNotification(
        recipientId: String,
        type: String,
        title: String,
        body: String,
        referenceId: String? = null
    ): Result<Unit> {
        val senderId = requireUserId()
        val docRef = db.collection("users").document(recipientId)
            .collection("notifications").document()
        return try {
            val payload = mutableMapOf<String, Any>(
                "recipientId" to recipientId,
                "senderId" to senderId,
                "type" to type,
                "title" to title.take(100),
                "body" to body.take(300),
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (!referenceId.isNullOrBlank()) {
                payload["referenceId"] = referenceId.take(128)
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("notifications").document(notificationId)
        return try {
            docRef.update(mapOf("read" to true)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Seller Verification & Public Profiles ---

    fun observeSellerProfile(sellerId: String): Flow<SellerProfile?> = flow {
        val uid = requireUserId()
        val path = "sellerProfiles/$sellerId"
        emitAll(
            db.collection("sellerProfiles").document(sellerId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            SellerProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .logFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observeVerifiedSellers(): Flow<List<SellerProfile>> = flow {
        val uid = requireUserId()
        val path = "sellerProfiles"
        emitAll(
            db.collection(path)
                .whereEqualTo("verificationStatus", "verified")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SellerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun requestSellerVerification(
        businessName: String,
        city: String,
        note: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("sellerProfiles").document(uid)
        val province = ZimbabweLocations.findCity(city).province
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val displayName = auth.currentUser?.displayName ?: "Mukambo Trader"
                docRef.set(
                    mapOf(
                        "sellerId" to uid,
                        "displayName" to displayName.take(60),
                        "city" to city.take(40),
                        "province" to province.take(40),
                        "verificationStatus" to "unverified",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            docRef.update(
                mapOf(
                    "businessName" to businessName.trim().take(80),
                    "city" to city.take(40),
                    "province" to province.take(40),
                    "verificationStatus" to "pending",
                    "verificationNote" to note.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Orders & Escrow Tracking ---

    fun observeBuyerOrders(buyerId: String): Flow<List<Order>> = flow {
        val path = "orders"
        emitAll(
            db.collection(path)
                .whereEqualTo("buyerId", buyerId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, buyerId)
        )
    }

    fun observeSellerOrders(sellerId: String): Flow<List<Order>> = flow {
        val path = "orders"
        emitAll(
            db.collection(path)
                .whereEqualTo("sellerId", sellerId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, sellerId)
        )
    }

    suspend fun checkoutWithEscrow(
        listing: Listing,
        buyerName: String,
        gateway: PaymentGatewayType,
        maskedAccount: String,
        deliveryCity: String,
        deliveryMethod: String,
        currency: String
    ): Result<String> {
        val uid = requireUserId()
        val listingRef = db.collection("listings").document(listing.id)
        val orderRef = db.collection("orders").document()
        val gatewayRef = "MK-${gateway.name.take(2)}-${UUID.randomUUID().toString().take(8).uppercase()}"
        val cleanBuyerName = buyerName.ifBlank { "Mukambo Buyer" }.take(60)

        val orderPayload = mapOf(
            "listingId" to listing.id,
            "listingTitle" to listing.title.take(100),
            "buyerId" to uid,
            "buyerName" to cleanBuyerName,
            "sellerId" to listing.sellerId,
            "sellerName" to listing.sellerName.take(60),
            "amountUsd" to listing.priceUsd,
            "currency" to currency,
            "paymentGateway" to gateway.displayName,
            "maskedAccount" to maskedAccount.take(30),
            "gatewayReference" to gatewayRef,
            "deliveryCity" to deliveryCity.take(40),
            "deliveryMethod" to deliveryMethod,
            "escrowStatus" to "escrow_held",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            val batch = db.batch()
            batch.update(
                listingRef,
                mapOf(
                    "status" to "sold",
                    "buyerId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(orderRef, orderPayload)
            batch.commit().await()
            sendNotification(
                recipientId = listing.sellerId,
                type = "order_update",
                title = "New Escrow Order: ${listing.title.take(45)}",
                body = "$cleanBuyerName paid via ${gateway.displayName}. Please dispatch to $deliveryCity.",
                referenceId = orderRef.id
            )
            Result.success(orderRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, orderRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateOrderTracking(
        orderId: String,
        nextStatus: String,
        courierName: String? = null,
        trackingCode: String? = null,
        estimatedDelivery: String? = null,
        trackingNotes: String? = null,
        buyerIdToNotify: String? = null,
        listingTitle: String = ""
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("orders").document(orderId)
        val updates = mutableMapOf<String, Any>(
            "escrowStatus" to nextStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!courierName.isNullOrBlank()) updates["courierName"] = courierName.trim().take(60)
        if (!trackingCode.isNullOrBlank()) updates["trackingCode"] = trackingCode.trim().take(40)
        if (!estimatedDelivery.isNullOrBlank()) updates["estimatedDelivery"] = estimatedDelivery.trim().take(60)
        if (!trackingNotes.isNullOrBlank()) updates["trackingNotes"] = trackingNotes.trim().take(200)

        return try {
            docRef.update(updates).await()
            if (!buyerIdToNotify.isNullOrBlank()) {
                val statusLabel = nextStatus.replace('_', ' ').replaceFirstChar { it.uppercase() }
                sendNotification(
                    recipientId = buyerIdToNotify,
                    type = "order_update",
                    title = "Order $statusLabel",
                    body = "${listingTitle.ifBlank { "Your order" }} is now $statusLabel" +
                        (trackingCode?.takeIf { it.isNotBlank() }?.let { " (Tracking: $it)" } ?: ""),
                    referenceId = orderId
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun markOrderDispatched(
        orderId: String,
        courierName: String? = null,
        trackingCode: String? = null,
        estimatedDelivery: String? = null
    ): Result<Unit> = updateOrderTracking(
        orderId = orderId,
        nextStatus = "dispatched",
        courierName = courierName,
        trackingCode = trackingCode,
        estimatedDelivery = estimatedDelivery
    )

    suspend fun releaseEscrow(orderId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("orders").document(orderId)
        return try {
            docRef.update(
                mapOf(
                    "escrowStatus" to "released",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun disputeOrder(orderId: String, reasonNote: String? = null): Result<Unit> {
        requireUserId()
        val docRef = db.collection("orders").document(orderId)
        val updates = mutableMapOf<String, Any>(
            "escrowStatus" to "disputed",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!reasonNote.isNullOrBlank()) {
            updates["trackingNotes"] = reasonNote.trim().take(200)
        }
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Buyer-Seller Chat ---

    fun observeUserChats(userId: String): Flow<List<ChatThread>> = flow {
        val path = "chats"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatThread::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeChatMessages(chatId: String, userId: String): Flow<List<ChatMessage>> = flow {
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    suspend fun startOrGetChat(listing: Listing, buyerName: String): Result<String> {
        val uid = requireUserId()
        require(listing.sellerId != uid) { "You cannot start a chat with yourself." }
        val chatId = "${listing.id}_$uid"
        val chatRef = db.collection("chats").document(chatId)
        return try {
            val existingChats = db.collection("chats")
                .whereEqualTo("listingId", listing.id)
                .whereArrayContains("participantIds", uid)
                .limit(1)
                .get()
                .await()
            if (existingChats.isEmpty) {
                val payload = mapOf(
                    "listingId" to listing.id,
                    "listingTitle" to listing.title.take(100),
                    "listingPriceUsd" to listing.priceUsd,
                    "buyerId" to uid,
                    "buyerName" to buyerName.ifBlank { "Mukambo Buyer" }.take(60),
                    "sellerId" to listing.sellerId,
                    "sellerName" to listing.sellerName.take(60),
                    "participantIds" to listOf(uid, listing.sellerId),
                    "lastMessage" to "Hi, is ${listing.title.take(40)} still available?",
                    "lastSenderId" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val msgRef = chatRef.collection("messages").document()
                val msgPayload = mapOf(
                    "chatId" to chatId,
                    "senderId" to uid,
                    "senderName" to buyerName.ifBlank { "Mukambo Buyer" }.take(60),
                    "text" to "Hi, is ${listing.title.take(60)} still available?",
                    "participantIds" to listOf(uid, listing.sellerId),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                val batch = db.batch()
                batch.set(chatRef, payload)
                batch.set(msgRef, msgPayload)
                batch.commit().await()
                sendNotification(
                    recipientId = listing.sellerId,
                    type = "chat_message",
                    title = "New inquiry on ${listing.title.take(40)}",
                    body = "$buyerName sent you a message.",
                    referenceId = chatId
                )
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, chatRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        chat: ChatThread,
        senderName: String,
        text: String
    ): Result<Unit> {
        val uid = requireUserId()
        val cleanText = text.trim().take(1000)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty"))
        val chatRef = db.collection("chats").document(chat.id)
        val msgRef = chatRef.collection("messages").document()
        return try {
            val msgPayload = mapOf(
                "chatId" to chat.id,
                "senderId" to uid,
                "senderName" to senderName.ifBlank { "Mukambo User" }.take(60),
                "text" to cleanText,
                "participantIds" to chat.participantIds,
                "createdAt" to FieldValue.serverTimestamp()
            )
            val batch = db.batch()
            batch.set(msgRef, msgPayload)
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to cleanText.take(500),
                    "lastSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            val recipientId = chat.participantIds.firstOrNull { it != uid }
            if (recipientId != null) {
                sendNotification(
                    recipientId = recipientId,
                    type = "chat_message",
                    title = "Message from ${senderName.take(30)}",
                    body = cleanText.take(120),
                    referenceId = chat.id
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Seller Ratings & Reviews ---

    fun observeSellerReviews(sellerId: String): Flow<List<SellerReview>> = flow {
        val uid = requireUserId()
        val path = "reviews"
        emitAll(
            db.collection(path)
                .whereEqualTo("status", "published")
                .whereEqualTo("sellerId", sellerId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SellerReview::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMyReviews(reviewerId: String): Flow<List<SellerReview>> = flow {
        val path = "reviews"
        emitAll(
            db.collection(path)
                .whereEqualTo("reviewerId", reviewerId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SellerReview::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, reviewerId)
        )
    }

    suspend fun submitSellerReview(
        order: Order,
        reviewerName: String,
        rating: Int,
        comment: String
    ): Result<String> {
        val uid = requireUserId()
        val reviewId = "${order.id}_$uid"
        val docRef = db.collection("reviews").document(reviewId)
        return try {
            val payload = mapOf(
                "orderId" to order.id,
                "listingId" to order.listingId,
                "listingTitle" to order.listingTitle.take(100),
                "sellerId" to order.sellerId,
                "reviewerId" to uid,
                "reviewerName" to reviewerName.ifBlank { "Mukambo Buyer" }.take(60),
                "rating" to rating.coerceIn(1, 5),
                "comment" to comment.trim().take(500),
                "status" to "published",
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            sendNotification(
                recipientId = order.sellerId,
                type = "system",
                title = "New $rating★ Seller Review",
                body = "$reviewerName reviewed you for ${order.listingTitle.take(40)}.",
                referenceId = order.id
            )
            Result.success(reviewId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Product Reporting ---

    fun observeMyReports(reporterId: String): Flow<List<ListingReport>> = flow {
        val path = "reports"
        emitAll(
            db.collection(path)
                .whereEqualTo("reporterId", reporterId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ListingReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, reporterId)
        )
    }

    suspend fun submitListingReport(
        listing: Listing,
        reporterName: String,
        reason: String,
        details: String?
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("reports").document()
        return try {
            val payload = mutableMapOf<String, Any>(
                "listingId" to listing.id,
                "listingTitle" to listing.title.take(100),
                "sellerId" to listing.sellerId,
                "reporterId" to uid,
                "reporterName" to reporterName.ifBlank { "Mukambo Member" }.take(60),
                "reason" to reason,
                "status" to "open",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!details.isNullOrBlank()) {
                payload["details"] = details.trim().take(500)
            }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Admin Dashboard Operations ---

    suspend fun checkIsAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.isEmailVerified && user.email.equals(MarketplaceConstants.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)) {
            return true
        }
        return try {
            db.collection("admins").document(user.uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    fun observeAllReports(): Flow<List<ListingReport>> = flow {
        val uid = requireUserId()
        val path = "reports"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ListingReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observePendingVerifications(): Flow<List<SellerProfile>> = flow {
        val uid = requireUserId()
        val path = "sellerProfiles"
        emitAll(
            db.collection(path)
                .whereEqualTo("verificationStatus", "pending")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SellerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeDisputedOrders(): Flow<List<Order>> = flow {
        val uid = requireUserId()
        val path = "orders"
        emitAll(
            db.collection(path)
                .whereEqualTo("escrowStatus", "disputed")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun moderateReport(
        report: ListingReport,
        removeListing: Boolean,
        adminNotes: String
    ): Result<Unit> {
        requireUserId()
        val reportRef = db.collection("reports").document(report.id)
        val listingRef = db.collection("listings").document(report.listingId)
        return try {
            val batch = db.batch()
            if (removeListing) {
                batch.update(
                    listingRef,
                    mapOf(
                        "status" to "removed",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.update(
                reportRef,
                mapOf(
                    "status" to if (removeListing) "resolved" else "dismissed",
                    "adminNotes" to adminNotes.trim().ifBlank {
                        if (removeListing) "Listing removed by Mukambo Safety Admin" else "Report reviewed and dismissed"
                    }.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reportRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun decideSellerVerification(
        sellerId: String,
        approve: Boolean,
        badgeLabel: String = "Verified Merchant"
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("sellerProfiles").document(sellerId)
        return try {
            val updates = mutableMapOf<String, Any>(
                "verificationStatus" to if (approve) "verified" else "unverified",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (approve) {
                updates["verificationBadge"] = badgeLabel.take(40)
            }
            docRef.update(updates).await()
            sendNotification(
                recipientId = sellerId,
                type = "verification",
                title = if (approve) "Verified Badge Approved" else "Verification Update",
                body = if (approve) {
                    "Congratulations! Your Mukambo seller account is now $badgeLabel."
                } else {
                    "Your verification request requires additional details. You may re-apply in Profile."
                },
                referenceId = sellerId
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun resolveDisputedOrder(
        order: Order,
        releaseToSeller: Boolean,
        resolutionNote: String
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("orders").document(order.id)
        val nextStatus = if (releaseToSeller) "released" else "refunded"
        return try {
            docRef.update(
                mapOf(
                    "escrowStatus" to nextStatus,
                    "trackingNotes" to resolutionNote.trim().ifBlank {
                        if (releaseToSeller) "Admin released escrow to seller" else "Admin refunded escrow to buyer"
                    }.take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    // --- Sample Listings Seeder ---

    suspend fun seedSampleZimbabweListings(sellerName: String): Result<Int> {
        val uid = requireUserId()
        val samples = listOf(
            Triple(
                "5kVA Must Hybrid Solar Inverter + 200Ah Gel",
                "Complete backup solar kit, lightly used for 4 months in Borrowdale. Pure sine wave, MPPT built-in.",
                Triple("Solar & Power", "Like New", 680.0)
            ) to ("Harare" to "Borrowdale"),
            Triple(
                "iPhone 15 Pro 256GB Natural Titanium",
                "Factory unlocked, battery health 97%, original box and USB-C cable included. Meet in Avondale.",
                Triple("Electronics", "Like New", 890.0)
            ) to ("Harare" to "Avondale"),
            Triple(
                "Hand-Carved Serpentine Stone Sculpture",
                "Authentic Zimbabwean springstone sculpture by Guruve artist. Ideal centrepiece, 42cm tall.",
                Triple("Crafts & Art", "New", 145.0)
            ) to ("Victoria Falls" to "Town Centre"),
            Triple(
                "2014 Toyota Aqua Hybrid (Ex-Japan)",
                "Fuel-efficient city hatch, clean interior, ZIMRA cleared with valid license and fitness.",
                Triple("Vehicles", "Good", 5400.0)
            ) to ("Bulawayo" to "Suburbs"),
            Triple(
                "Mukwa Solid Teak 6-Seater Dining Table",
                "Locally crafted Zimbabwean mukwa hardwood table with six upholstered chairs. Solid joinery.",
                Triple("Home & Furniture", "Good", 520.0)
            ) to ("Mutare" to "Murambi"),
            Triple(
                "2x 40W Drip Irrigation Solar Pump Kit",
                "Submersible borehole solar pump with 50m head, ideal for market gardening plots.",
                Triple("Agriculture", "New", 295.0)
            ) to ("Gweru" to "Mkoba"),
            Triple(
                "MacBook Air M2 16GB / 512GB Space Grey",
                "Mint condition university laptop, cycle count 38, includes MagSafe charger. Meet in Chinhoyi CBD.",
                Triple("Electronics", "Like New", 980.0)
            ) to ("Chinhoyi" to "CBD"),
            Triple(
                "600W Solar Deep Freezer (12V/24V DC)",
                "Energy-saving solar chest freezer ideal for Bindura & Mashonaland farms.",
                Triple("Solar & Power", "New", 410.0)
            ) to ("Bindura" to "CBD")
        )

        return try {
            val batch = db.batch()
            for ((itemSpec, loc) in samples) {
                val (title, desc, catCondPrice) = itemSpec
                val (category, condition, price) = catCondPrice
                val (cityName, suburbName) = loc
                val city = ZimbabweLocations.findCity(cityName)
                val suburb = city.suburbs.firstOrNull { it.name == suburbName } ?: city.suburbs.first()
                val docRef = db.collection("listings").document()
                val payload = buildCreateListingPayload(
                    sellerId = uid,
                    sellerName = sellerName.ifBlank { "Mukambo Verified Seller" },
                    title = title,
                    description = desc,
                    category = category,
                    condition = condition,
                    priceUsd = price,
                    currency = "USD",
                    province = city.province,
                    city = city.name,
                    suburb = suburb.name,
                    latitude = suburb.latitude,
                    longitude = suburb.longitude
                )
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(samples.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "listings", auth)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "MukamboRepository"
    }
}
