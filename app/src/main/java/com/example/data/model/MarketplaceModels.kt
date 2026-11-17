package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val phoneNumber: String? = null,
    val defaultCity: String = "Harare",
    val defaultProvince: String? = "Harare Metropolitan",
    val preferredCurrency: String = "USD",
    val fcmToken: String? = null,
    val notificationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FavoriteItem(
    @DocumentId val id: String = "",
    val listingId: String = "",
    val title: String = "",
    val priceUsd: Double = 0.0,
    val city: String = "Harare",
    val province: String? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val type: String = "system",
    val title: String = "",
    val body: String = "",
    val referenceId: String? = null,
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SellerProfile(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val displayName: String = "",
    val city: String = "Harare",
    val province: String? = "Harare Metropolitan",
    val businessName: String? = null,
    val verificationStatus: String = "unverified",
    val verificationBadge: String? = null,
    val verificationNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Listing(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Electronics",
    val condition: String = "Good",
    val priceUsd: Double = 0.0,
    val currency: String = "USD",
    val province: String? = null,
    val city: String = "Harare",
    val suburb: String = "CBD",
    val latitude: Double = -17.8252,
    val longitude: Double = 31.0335,
    val status: String = "active",
    val buyerId: String? = null,
    val imageUrls: List<String> = emptyList(),
    val localImageNames: List<String> = emptyList(),
    val localImageName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val amountUsd: Double = 0.0,
    val currency: String = "USD",
    val paymentGateway: String = "EcoCash",
    val maskedAccount: String = "",
    val gatewayReference: String = "",
    val deliveryCity: String = "Harare",
    val deliveryMethod: String = "Meetup / Pickup",
    val escrowStatus: String = "escrow_held",
    val courierName: String? = null,
    val trackingCode: String? = null,
    val estimatedDelivery: String? = null,
    val trackingNotes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val listingPriceUsd: Double = 0.0,
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val participantIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SellerReview(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val sellerId: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val status: String = "published",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ListingReport(
    @DocumentId val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val sellerId: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val reason: String = "",
    val details: String? = null,
    val status: String = "open",
    val adminNotes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object MarketplaceConstants {
    const val BOOTSTRAPPED_ADMIN_EMAIL = "gatsheniworkshop@gmail.com"

    val categories = listOf(
        "Electronics",
        "Solar & Power",
        "Vehicles",
        "Home & Furniture",
        "Fashion",
        "Agriculture",
        "Crafts & Art"
    )

    val conditions = listOf("New", "Like New", "Good", "Refurbished")
    val currencies = listOf("USD", "ZIG")
    val deliveryMethods = listOf("Meetup / Pickup", "Courier Delivery")
    val radiusOptionsKm = listOf<Int?>(null, 10, 25, 50, 100)

    val reportReasons = listOf(
        "Suspected Scam / Fraud",
        "Counterfeit or Prohibited Item",
        "Misleading Price or Photos",
        "Already Sold / Unresponsive",
        "Duplicate Spam"
    )

    val courierOptions = listOf(
        "Swift Freight Zimbabwe",
        "ZimPost EMS",
        "FedEx Zimbabwe",
        "Intercape Parcel",
        "Local Seller Meetup"
    )

    val orderTrackingSteps = listOf(
        "escrow_held" to "Escrow Held",
        "dispatched" to "Dispatched",
        "in_transit" to "In Transit",
        "delivered" to "Delivered",
        "released" to "Released"
    )

    // Indicative USD -> ZiG conversion for dual-currency marketplace display
    const val ZIG_PER_USD = 27.5

    fun formatPrice(priceUsd: Double, displayCurrency: String): String {
        return if (displayCurrency == "ZIG") {
            val zig = priceUsd * ZIG_PER_USD
            String.format(Locale.US, "ZiG %,.0f", zig)
        } else {
            if (priceUsd % 1.0 == 0.0) {
                String.format(Locale.US, "$%,.0f", priceUsd)
            } else {
                String.format(Locale.US, "$%,.2f", priceUsd)
            }
        }
    }

    fun formatSecondaryPrice(priceUsd: Double, displayCurrency: String): String {
        return if (displayCurrency == "ZIG") {
            String.format(Locale.US, "≈ $%,.2f USD", priceUsd)
        } else {
            String.format(Locale.US, "≈ ZiG %,.0f", priceUsd * ZIG_PER_USD)
        }
    }
}

enum class PaymentGatewayType(
    val displayName: String,
    val providerTag: String,
    val inputHint: String,
    val description: String,
    val isCard: Boolean = false
) {
    ECOCASH(
        displayName = "EcoCash",
        providerTag = "Instant USSD Push • Econet",
        inputHint = "077 / 078 number (e.g. 0771234567)",
        description = "Pay securely via EcoCash mobile wallet with Mukambo Escrow protection."
    ),
    INNBUCKS(
        displayName = "InnBucks",
        providerTag = "InnBucks Wallet • Simbisa",
        inputHint = "077 / 078 / 071 mobile number",
        description = "Authorize with your InnBucks wallet PIN; zero buyer convenience fee."
    ),
    ONEMONEY(
        displayName = "OneMoney",
        providerTag = "NetOne Mobile Money",
        inputHint = "071 number (e.g. 0712345678)",
        description = "Fast OneMoney wallet transfer held safely until delivery is verified."
    ),
    ZIMSWITCH_CARD(
        displayName = "ZimSwitch / Card",
        providerTag = "ZimSwitch • Visa • Mastercard",
        inputHint = "16-digit card number",
        description = "3D-Secure tokenized card checkout; card digits are never stored.",
        isCard = true
    );

    fun validateAndMask(rawAccount: String, expiry: String = "", cvv: String = ""): Result<String> {
        val cleaned = rawAccount.replace(" ", "").replace("-", "")
        if (isCard) {
            if (cleaned.length < 13 || cleaned.length > 19 || !cleaned.all { it.isDigit() }) {
                return Result.failure(IllegalArgumentException("Enter a valid 13–19 digit card number"))
            }
            if (!passesLuhn(cleaned)) {
                return Result.failure(IllegalArgumentException("Card number failed Luhn security check"))
            }
            if (!expiry.matches(Regex("^(0[1-9]|1[0-2])/[0-9]{2}$"))) {
                return Result.failure(IllegalArgumentException("Enter expiry as MM/YY"))
            }
            if (!cvv.matches(Regex("^[0-9]{3,4}$"))) {
                return Result.failure(IllegalArgumentException("Enter a valid 3 or 4 digit CVV"))
            }
            val last4 = cleaned.takeLast(4)
            return Result.success("Card •••• $last4")
        } else {
            val normalized = when {
                cleaned.startsWith("+263") && cleaned.length == 13 -> cleaned
                cleaned.startsWith("263") && cleaned.length == 12 -> "+$cleaned"
                cleaned.startsWith("07") && cleaned.length == 10 -> "+263${cleaned.drop(1)}"
                else -> return Result.failure(
                    IllegalArgumentException("Enter a valid Zimbabwe mobile number (e.g. 0771234567 or +263771234567)")
                )
            }
            val validPrefix = when (this) {
                ECOCASH -> normalized.startsWith("+26377") || normalized.startsWith("+26378")
                ONEMONEY -> normalized.startsWith("+26371")
                INNBUCKS -> normalized.startsWith("+26377") || normalized.startsWith("+26378") || normalized.startsWith("+26371")
                ZIMSWITCH_CARD -> true
            }
            if (!validPrefix) {
                val hint = if (this == ONEMONEY) "071" else "077 or 078"
                return Result.failure(IllegalArgumentException("$displayName requires a $hint number"))
            }
            val prefix = normalized.take(6)
            val tail = normalized.takeLast(3)
            return Result.success("$prefix****$tail")
        }
    }

    private fun passesLuhn(digits: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in digits.length - 1 downTo 0) {
            var n = digits[i].digitToInt()
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }
}

fun buildCreateListingPayload(
    sellerId: String,
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
): Map<String, Any> {
    val resolvedProvince = province?.takeIf { it.isNotBlank() }
        ?: ZimbabweLocations.findCity(city).province
    val map = mutableMapOf<String, Any>(
        "sellerId" to sellerId,
        "sellerName" to sellerName.take(60),
        "title" to title.trim().take(100),
        "description" to description.trim().take(1000),
        "category" to category,
        "condition" to condition,
        "priceUsd" to priceUsd,
        "currency" to currency,
        "province" to resolvedProvince.take(40),
        "city" to city,
        "suburb" to suburb,
        "latitude" to latitude,
        "longitude" to longitude,
        "status" to "active",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
    val effectiveLocalPrimary = localImageName?.takeIf { it.isNotBlank() }
        ?: localImageNames.firstOrNull()?.takeIf { it.isNotBlank() }
    if (effectiveLocalPrimary != null) {
        map["localImageName"] = effectiveLocalPrimary.take(120)
    }
    val cleanedLocalNames = localImageNames.filter { it.isNotBlank() }.take(4).map { it.take(120) }
    if (cleanedLocalNames.isNotEmpty()) {
        map["localImageNames"] = cleanedLocalNames
    }
    val cleanedUrls = imageUrls.filter { it.isNotBlank() }.take(4).map { it.take(600) }
    if (cleanedUrls.isNotEmpty()) {
        map["imageUrls"] = cleanedUrls
    }
    return map
}
