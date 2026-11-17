package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.FavoriteItem
import com.example.data.model.Listing
import com.example.data.model.ListingReport
import com.example.data.model.Order
import com.example.data.model.PaymentGatewayType
import com.example.data.model.SellerProfile
import com.example.data.model.SellerReview
import com.example.data.model.UserProfile
import com.example.data.model.ZimbabweLocations
import com.example.data.repository.MukamboRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ListingSortOrder(val label: String) {
    NEAREST("Nearest First"),
    NEWEST("Newest"),
    PRICE_LOW("Price: Low to High"),
    PRICE_HIGH("Price: High to Low")
}

data class ListingWithDistance(
    val listing: Listing,
    val distanceKm: Double
)

data class LocationFilterState(
    val selectedProvince: String = "All Provinces",
    val selectedCity: String = "All Zimbabwe",
    val maxRadiusKm: Int? = null,
    val gpsCoordinates: Pair<Double, Double>? = null,
    val gpsCityLabel: String? = null,
    val sortOrder: ListingSortOrder = ListingSortOrder.NEAREST
) {
    val activeOrigin: Pair<Double, Double>
        get() = when {
            selectedCity != "All Zimbabwe" -> {
                val c = ZimbabweLocations.findCity(selectedCity)
                c.latitude to c.longitude
            }
            selectedProvince != "All Provinces" -> {
                val c = ZimbabweLocations.citiesInProvince(selectedProvince).firstOrNull()
                    ?: ZimbabweLocations.findCity("Harare")
                c.latitude to c.longitude
            }
            gpsCoordinates != null -> gpsCoordinates
            else -> {
                val harare = ZimbabweLocations.findCity("Harare")
                harare.latitude to harare.longitude
            }
        }

    val summaryLabel: String
        get() {
            val base = when {
                selectedCity != "All Zimbabwe" -> selectedCity
                selectedProvince != "All Provinces" -> selectedProvince
                gpsCityLabel != null -> "Near $gpsCityLabel (GPS)"
                else -> "All Zimbabwe"
            }
            return if (maxRadiusKm != null) "$base • ≤ $maxRadiusKm km" else base
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceViewModel(
    private val repository: MukamboRepository,
    val currentUserId: String,
    val currentUserName: String,
    val currentUserEmail: String?
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _locationFilter = MutableStateFlow(LocationFilterState())
    val locationFilter: StateFlow<LocationFilterState> = _locationFilter.asStateFlow()

    private val _displayCurrency = MutableStateFlow("USD")
    val displayCurrency: StateFlow<String> = _displayCurrency.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserName, currentUserEmail)
            _isAdmin.value = repository.checkIsAdmin()
            repository.syncFcmToken()
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> {
        return map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )
    }

    val activeListingsState: StateFlow<UiState<List<Listing>>> =
        repository.observeActiveListings().toUiState("marketplace listings")

    val myListingsState: StateFlow<UiState<List<Listing>>> =
        repository.observeMyListings(currentUserId).toUiState("your listings")

    val buyerOrdersState: StateFlow<UiState<List<Order>>> =
        repository.observeBuyerOrders(currentUserId).toUiState("your purchases")

    val sellerOrdersState: StateFlow<UiState<List<Order>>> =
        repository.observeSellerOrders(currentUserId).toUiState("your sales")

    val favoritesState: StateFlow<UiState<List<FavoriteItem>>> =
        repository.observeFavorites(currentUserId).toUiState("saved items")

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiState("profile")

    val notificationsState: StateFlow<UiState<List<AppNotification>>> =
        repository.observeNotifications(currentUserId).toUiState("notifications")

    val chatsState: StateFlow<UiState<List<ChatThread>>> =
        repository.observeUserChats(currentUserId).toUiState("messages")

    val myReviewsState: StateFlow<UiState<List<SellerReview>>> =
        repository.observeMyReviews(currentUserId).toUiState("reviews")

    val mySellerProfileState: StateFlow<UiState<SellerProfile?>> =
        repository.observeSellerProfile(currentUserId).toUiState("seller verification")

    val verifiedSellersMapState: StateFlow<Map<String, SellerProfile>> =
        repository.observeVerifiedSellers()
            .map { list -> list.associateBy { it.sellerId } }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing verified sellers", error)
                emit(emptyMap())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyMap()
            )

    val allReportsState: StateFlow<UiState<List<ListingReport>>> =
        _isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAllReports() else flowOf(emptyList())
        }.toUiState("reported listings")

    val pendingVerificationsState: StateFlow<UiState<List<SellerProfile>>> =
        _isAdmin.flatMapLatest { admin ->
            if (admin) repository.observePendingVerifications() else flowOf(emptyList())
        }.toUiState("pending verifications")

    val disputedOrdersState: StateFlow<UiState<List<Order>>> =
        _isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeDisputedOrders() else flowOf(emptyList())
        }.toUiState("disputed orders")

    val filteredListingsState: StateFlow<UiState<List<ListingWithDistance>>> = combine(
        activeListingsState,
        _searchQuery,
        _selectedCategory,
        _locationFilter
    ) { listingsState, query, category, locFilter ->
        when (listingsState) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> UiState.Error(listingsState.message)
            is UiState.Success -> {
                val (originLat, originLon) = locFilter.activeOrigin
                val trimmedQuery = query.trim().lowercase()

                val mapped = listingsState.data
                    .filter { item ->
                        val resolvedProvince = item.province?.takeIf { it.isNotBlank() }
                            ?: ZimbabweLocations.findCity(item.city).province
                        val matchesCategory = category == "All" || item.category == category
                        val matchesProvince = locFilter.selectedProvince == "All Provinces" ||
                            resolvedProvince.equals(locFilter.selectedProvince, ignoreCase = true)
                        val matchesCity = locFilter.selectedCity == "All Zimbabwe" ||
                            item.city.equals(locFilter.selectedCity, ignoreCase = true)
                        val matchesQuery = trimmedQuery.isEmpty() ||
                            item.title.lowercase().contains(trimmedQuery) ||
                            item.description.lowercase().contains(trimmedQuery) ||
                            item.suburb.lowercase().contains(trimmedQuery) ||
                            item.city.lowercase().contains(trimmedQuery) ||
                            resolvedProvince.lowercase().contains(trimmedQuery)
                        matchesCategory && matchesProvince && matchesCity && matchesQuery
                    }
                    .map { item ->
                        val dist = ZimbabweLocations.distanceKm(
                            originLat, originLon,
                            item.latitude, item.longitude
                        )
                        ListingWithDistance(item, dist)
                    }
                    .filter { itemWithDist ->
                        val maxKm = locFilter.maxRadiusKm
                        maxKm == null || itemWithDist.distanceKm <= maxKm.toDouble()
                    }

                val sorted = when (locFilter.sortOrder) {
                    ListingSortOrder.NEAREST -> mapped.sortedBy { it.distanceKm }
                    ListingSortOrder.NEWEST -> mapped.sortedByDescending {
                        it.listing.createdAt ?: Timestamp(0, 0)
                    }
                    ListingSortOrder.PRICE_LOW -> mapped.sortedBy { it.listing.priceUsd }
                    ListingSortOrder.PRICE_HIGH -> mapped.sortedByDescending { it.listing.priceUsd }
                }

                UiState.Success(sorted)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    fun observeListing(listingId: String): Flow<Listing?> =
        repository.observeListing(listingId).catch { emit(null) }

    fun observeSellerProfile(sellerId: String): Flow<SellerProfile?> =
        repository.observeSellerProfile(sellerId).catch { emit(null) }

    fun observeSellerReviews(sellerId: String): Flow<List<SellerReview>> =
        repository.observeSellerReviews(sellerId).catch { emit(emptyList()) }

    fun observeChatMessages(chatId: String): Flow<List<ChatMessage>> =
        repository.observeChatMessages(chatId, currentUserId)
            .map { list -> list.sortedBy { it.createdAt ?: Timestamp(0, 0) } }
            .catch { emit(emptyList()) }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectProvinceFilter(province: String) {
        val currentCity = _locationFilter.value.selectedCity
        val nextCity = if (province == "All Provinces" || currentCity == "All Zimbabwe") {
            currentCity
        } else {
            val cityProv = ZimbabweLocations.findCity(currentCity).province
            if (cityProv.equals(province, ignoreCase = true)) currentCity else "All Zimbabwe"
        }
        _locationFilter.value = _locationFilter.value.copy(
            selectedProvince = province,
            selectedCity = nextCity
        )
    }

    fun selectCityFilter(cityName: String) {
        val nextProvince = if (cityName == "All Zimbabwe") {
            _locationFilter.value.selectedProvince
        } else {
            ZimbabweLocations.findCity(cityName).province
        }
        _locationFilter.value = _locationFilter.value.copy(
            selectedProvince = nextProvince,
            selectedCity = cityName
        )
    }

    fun selectRadiusFilter(radiusKm: Int?) {
        _locationFilter.value = _locationFilter.value.copy(maxRadiusKm = radiusKm)
    }

    fun setSortOrder(order: ListingSortOrder) {
        _locationFilter.value = _locationFilter.value.copy(sortOrder = order)
    }

    fun setGpsCoordinates(latitude: Double, longitude: Double) {
        val nearest = ZimbabweLocations.findNearestCity(latitude, longitude)
        _locationFilter.value = _locationFilter.value.copy(
            gpsCoordinates = latitude to longitude,
            gpsCityLabel = nearest.name,
            selectedProvince = "All Provinces",
            selectedCity = "All Zimbabwe",
            sortOrder = ListingSortOrder.NEAREST
        )
        _actionMessage.value = "Location updated near ${nearest.name}"
    }

    fun clearLocationFilters() {
        _locationFilter.value = LocationFilterState()
    }

    fun toggleCurrency() {
        _displayCurrency.value = if (_displayCurrency.value == "USD") "ZIG" else "USD"
    }

    fun showMessage(message: String) {
        _actionMessage.value = message
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun syncFcmToken() {
        viewModelScope.launch {
            repository.syncFcmToken()
        }
    }

    fun uploadAndCreateListing(
        context: Context,
        photoUris: List<Uri>,
        title: String,
        description: String,
        category: String,
        condition: String,
        priceUsd: Double,
        currency: String,
        province: String,
        city: String,
        suburb: String,
        latitude: Double,
        longitude: Double,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val uploadResult = if (photoUris.isNotEmpty()) {
                repository.uploadProductImages(context.applicationContext, photoUris)
            } else {
                null
            }
            val profileName = (userProfileState.value as? UiState.Success)?.data?.displayName
                ?: currentUserName
            val result = repository.createListing(
                sellerName = profileName,
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
                localImageName = uploadResult?.localImageNames?.firstOrNull(),
                localImageNames = uploadResult?.localImageNames ?: emptyList(),
                imageUrls = uploadResult?.imageUrls ?: emptyList()
            )
            _isBusy.value = false
            result.onSuccess { newId ->
                val suffix = if (uploadResult?.usedLocalFallback == true) {
                    " (photos saved locally)"
                } else ""
                _actionMessage.value = "Listing published to $suburb, $city$suffix"
                onSuccess(newId)
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to publish listing"
            }
        }
    }

    fun deleteListing(listingId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteListing(listingId)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Listing removed"
                onDeleted()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not remove listing"
            }
        }
    }

    fun toggleFavorite(listing: Listing, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleFavorite(listing, isCurrentlyFavorite)
            result.onSuccess {
                _actionMessage.value = if (isCurrentlyFavorite) {
                    "Removed from saved items"
                } else {
                    "Saved to wishlist"
                }
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to update wishlist"
            }
        }
    }

    fun removeFavoriteByListingId(listingId: String) {
        toggleFavorite(
            listing = Listing(id = listingId),
            isCurrentlyFavorite = true
        )
    }

    fun checkoutWithEscrow(
        listing: Listing,
        gateway: PaymentGatewayType,
        maskedAccount: String,
        deliveryCity: String,
        deliveryMethod: String,
        currency: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val buyerName = (userProfileState.value as? UiState.Success)?.data?.displayName
                ?: currentUserName
            val result = repository.checkoutWithEscrow(
                listing = listing,
                buyerName = buyerName,
                gateway = gateway,
                maskedAccount = maskedAccount,
                deliveryCity = deliveryCity,
                deliveryMethod = deliveryMethod,
                currency = currency
            )
            _isBusy.value = false
            result.onSuccess { orderId ->
                _actionMessage.value = "Payment secured in Mukambo Escrow!"
                onSuccess(orderId)
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Checkout failed"
            }
        }
    }

    fun updateOrderTracking(
        order: Order,
        nextStatus: String,
        courierName: String?,
        trackingCode: String?,
        estimatedDelivery: String?,
        trackingNotes: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateOrderTracking(
                orderId = order.id,
                nextStatus = nextStatus,
                courierName = courierName,
                trackingCode = trackingCode,
                estimatedDelivery = estimatedDelivery,
                trackingNotes = trackingNotes,
                buyerIdToNotify = order.buyerId,
                listingTitle = order.listingTitle
            )
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Tracking updated to ${nextStatus.replace('_', ' ')}"
                onDone()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to update tracking"
            }
        }
    }

    fun markOrderDispatched(orderId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.markOrderDispatched(orderId)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Order marked as dispatched"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to update order"
            }
        }
    }

    fun releaseEscrow(orderId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.releaseEscrow(orderId)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Delivery confirmed & funds released to seller"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to release funds"
            }
        }
    }

    fun disputeOrder(orderId: String, reasonNote: String? = null) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.disputeOrder(orderId, reasonNote)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Dispute opened — escrow funds frozen for review"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to open dispute"
            }
        }
    }

    fun startChatWithSeller(listing: Listing, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val buyerName = (userProfileState.value as? UiState.Success)?.data?.displayName
                ?: currentUserName
            val result = repository.startOrGetChat(listing, buyerName)
            _isBusy.value = false
            result.onSuccess { chatId ->
                onChatReady(chatId)
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not open chat"
            }
        }
    }

    fun sendChatMessage(chat: ChatThread, text: String, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            val senderName = (userProfileState.value as? UiState.Success)?.data?.displayName
                ?: currentUserName
            val result = repository.sendChatMessage(chat, senderName, text)
            result.onSuccess {
                onSent()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to send message"
            }
        }
    }

    fun submitSellerReview(
        order: Order,
        rating: Int,
        comment: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val reviewerName = (userProfileState.value as? UiState.Success)?.data?.displayName
                ?: currentUserName
            val result = repository.submitSellerReview(order, reviewerName, rating, comment)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Thank you! Your $rating★ seller review is live."
                onDone()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to submit review"
            }
        }
    }

    fun submitListingReport(
        listing: Listing,
        reason: String,
        details: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val reporterName = (userProfileState.value as? UiState.Success)?.data?.displayName
                ?: currentUserName
            val result = repository.submitListingReport(listing, reporterName, reason, details)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Report submitted to Mukambo Safety Team"
                onDone()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to submit report"
            }
        }
    }

    fun requestSellerVerification(
        businessName: String,
        city: String,
        note: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.requestSellerVerification(businessName, city, note)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Verification request submitted for admin review"
                onDone()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to request verification"
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun updateProfile(
        displayName: String,
        phoneNumber: String?,
        defaultCity: String,
        preferredCurrency: String,
        notificationsEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateUserProfile(
                displayName = displayName,
                phoneNumber = phoneNumber,
                defaultCity = defaultCity,
                preferredCurrency = preferredCurrency,
                notificationsEnabled = notificationsEnabled
            )
            _isBusy.value = false
            result.onSuccess {
                _displayCurrency.value = preferredCurrency
                _actionMessage.value = "Profile preferences saved"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to save profile"
            }
        }
    }

    fun moderateReport(
        report: ListingReport,
        removeListing: Boolean,
        adminNotes: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.moderateReport(report, removeListing, adminNotes)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = if (removeListing) {
                    "Listing removed & report resolved"
                } else {
                    "Report dismissed"
                }
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Admin action failed"
            }
        }
    }

    fun decideSellerVerification(
        sellerId: String,
        approve: Boolean,
        badgeLabel: String = "Verified Merchant"
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.decideSellerVerification(sellerId, approve, badgeLabel)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = if (approve) {
                    "Seller verification badge approved"
                } else {
                    "Verification request returned"
                }
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to update verification"
            }
        }
    }

    fun resolveDisputedOrder(
        order: Order,
        releaseToSeller: Boolean,
        resolutionNote: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.resolveDisputedOrder(order, releaseToSeller, resolutionNote)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = if (releaseToSeller) {
                    "Dispute resolved: Escrow released to seller"
                } else {
                    "Dispute resolved: Refund issued to buyer"
                }
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to resolve dispute"
            }
        }
    }

    fun seedSampleListings() {
        viewModelScope.launch {
            _isBusy.value = true
            val name = (userProfileState.value as? UiState.Success)?.data?.displayName
                ?: currentUserName
            val result = repository.seedSampleZimbabweListings(name)
            _isBusy.value = false
            result.onSuccess { count ->
                _actionMessage.value = "Added $count sample Zimbabwe listings"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not load sample listings"
            }
        }
    }

    private companion object {
        const val TAG = "MarketplaceVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
