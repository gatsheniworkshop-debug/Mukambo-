package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.FavoriteItem
import com.example.data.model.Listing
import com.example.data.model.MarketplaceConstants
import com.example.data.model.SellerProfile
import com.example.data.model.ZimbabweLocations
import com.example.ui.components.CurrencyToggleChip
import com.example.ui.components.ListingVisualBox
import com.example.ui.components.LocationFilterSheet
import com.example.ui.components.NotificationInboxSheet
import com.example.ui.components.VerifiedSellerBadge
import com.example.ui.components.WishlistBottomSheet
import com.example.ui.components.categoryIcon
import com.example.ui.viewmodel.ListingSortOrder
import com.example.ui.viewmodel.ListingWithDistance
import com.example.ui.viewmodel.LocationFilterState
import com.example.ui.viewmodel.UiState

@Composable
fun ExploreScreen(
    listingsState: UiState<List<ListingWithDistance>>,
    favoriteIds: Set<String>,
    verifiedSellers: Map<String, SellerProfile>,
    favoritesState: UiState<List<FavoriteItem>>,
    notificationsState: UiState<List<AppNotification>>,
    isAdmin: Boolean,
    searchQuery: String,
    selectedCategory: String,
    locationFilter: LocationFilterState,
    displayCurrency: String,
    isBusy: Boolean,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onProvinceSelect: (String) -> Unit,
    onCitySelect: (String) -> Unit,
    onRadiusSelect: (Int?) -> Unit,
    onSortSelect: (ListingSortOrder) -> Unit,
    onGpsReceived: (Double, Double) -> Unit,
    onShowMessage: (String) -> Unit,
    onClearLocationFilters: () -> Unit,
    onToggleCurrency: () -> Unit,
    onToggleFavorite: (Listing, Boolean) -> Unit,
    onRemoveFavoriteById: (String) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onOpenListing: (String) -> Unit,
    onNavigateToSell: () -> Unit,
    onOpenAdmin: () -> Unit,
    onSeedSamples: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLocationSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showWishlistSheet by remember { mutableStateOf(false) }

    val unreadCount = remember(notificationsState) {
        (notificationsState as? UiState.Success)?.data?.count { !it.read } ?: 0
    }

    if (showLocationSheet) {
        LocationFilterSheet(
            state = locationFilter,
            onSelectProvince = onProvinceSelect,
            onSelectCity = onCitySelect,
            onSelectRadius = onRadiusSelect,
            onSelectSort = onSortSelect,
            onGpsReceived = onGpsReceived,
            onGpsMessage = onShowMessage,
            onClearAll = onClearLocationFilters,
            onDismiss = { showLocationSheet = false }
        )
    }

    if (showNotificationsSheet) {
        NotificationInboxSheet(
            notificationsState = notificationsState,
            onMarkRead = onMarkNotificationRead,
            onDismiss = { showNotificationsSheet = false }
        )
    }

    if (showWishlistSheet) {
        WishlistBottomSheet(
            favoritesState = favoritesState,
            displayCurrency = displayCurrency,
            onOpenListing = onOpenListing,
            onRemoveFavorite = onRemoveFavoriteById,
            onDismiss = { showWishlistSheet = false }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 166.dp),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Brand & Location Header
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { showLocationSheet = true }
                            .testTag("location_filter_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Filter by location",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = locationFilter.summaryLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (isAdmin) {
                        IconButton(
                            onClick = onOpenAdmin,
                            modifier = Modifier.testTag("explore_admin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = stringResource(R.string.admin_dashboard_title),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = { showWishlistSheet = true },
                        modifier = Modifier.testTag("explore_wishlist_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (favoriteIds.isNotEmpty()) {
                                    Badge { Text(favoriteIds.size.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FavoriteBorder,
                                contentDescription = stringResource(R.string.wishlist_title),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    IconButton(
                        onClick = { showNotificationsSheet = true },
                        modifier = Modifier.testTag("explore_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(unreadCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = stringResource(R.string.notifications_title),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    CurrencyToggleChip(
                        currency = displayCurrency,
                        onToggle = onToggleCurrency
                    )
                }
            }
        }

        // Hero Discovery Card
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_mukambo_hero),
                    contentDescription = stringResource(R.string.cd_hero_banner),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xDE083B2B),
                                    Color(0x55083B2B)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = Color(0xFFF2B705),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EcoCash • InnBucks • OneMoney • ZimSwitch",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF2B705)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Trusted Local Trade Across Zimbabwe",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "All 10 provinces • Verified sellers • Escrow protection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE6EFEA)
                    )
                }
            }
        }

        // Search Field
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text(stringResource(R.string.explore_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_listings_input")
            )
        }

        // Category Chips
        item(span = { GridItemSpan(maxLineSpan) }) {
            val categories = listOf("All") + MarketplaceConstants.categories
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelect(category) },
                        label = { Text(category) },
                        leadingIcon = if (category != "All") {
                            {
                                Icon(
                                    imageVector = categoryIcon(category),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Listings Feed States
        when (listingsState) {
            is UiState.Loading -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is UiState.Error -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Could not load listings",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = listingsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            is UiState.Success -> {
                val itemsList = listingsState.data
                if (itemsList.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyExploreCard(
                            isBusy = isBusy,
                            hasActiveFilters = searchQuery.isNotBlank() ||
                                selectedCategory != "All" ||
                                locationFilter.selectedProvince != "All Provinces" ||
                                locationFilter.selectedCity != "All Zimbabwe" ||
                                locationFilter.maxRadiusKm != null,
                            onResetFilters = {
                                onSearchChange("")
                                onCategorySelect("All")
                                onClearLocationFilters()
                            },
                            onPostItem = onNavigateToSell,
                            onSeedSamples = onSeedSamples
                        )
                    }
                } else {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${itemsList.size} items available",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = locationFilter.sortOrder.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { showLocationSheet = true }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    items(itemsList, key = { it.listing.id }) { itemWithDistance ->
                        val listing = itemWithDistance.listing
                        val isFav = favoriteIds.contains(listing.id)
                        val isVerified = verifiedSellers.containsKey(listing.sellerId)
                        MarketplaceListingCard(
                            listing = listing,
                            distanceKm = itemWithDistance.distanceKm,
                            displayCurrency = displayCurrency,
                            isFavorite = isFav,
                            isSellerVerified = isVerified,
                            onToggleFavorite = { onToggleFavorite(listing, isFav) },
                            onClick = { onOpenListing(listing.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketplaceListingCard(
    listing: Listing,
    distanceKm: Double,
    displayCurrency: String,
    isFavorite: Boolean,
    isSellerVerified: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val photoCount = maxOf(
        listing.imageUrls.size,
        listing.localImageNames.size,
        if (!listing.localImageName.isNullOrBlank()) 1 else 0
    )

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("listing_card_${listing.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            ) {
                ListingVisualBox(
                    listing = listing,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(36.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Save listing",
                            tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (photoCount > 1) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.62f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Collections,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = photoCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = MarketplaceConstants.formatPrice(listing.priceUsd, displayCurrency),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = MarketplaceConstants.formatSecondaryPrice(listing.priceUsd, displayCurrency),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = listing.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )

            if (isSellerVerified) {
                Spacer(modifier = Modifier.height(4.dp))
                VerifiedSellerBadge(label = "Verified", compact = true)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${listing.suburb}, ${listing.city}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = ZimbabweLocations.formatDistance(distanceKm),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyExploreCard(
    isBusy: Boolean,
    hasActiveFilters: Boolean,
    onResetFilters: () -> Unit,
    onPostItem: () -> Unit,
    onSeedSamples: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_basket),
                contentDescription = stringResource(R.string.cd_empty_basket),
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = stringResource(R.string.explore_empty_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.explore_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hasActiveFilters) {
                OutlinedButton(
                    onClick = onResetFilters,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Location & Search Filters")
                }
            }

            Button(
                onClick = onPostItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.explore_post_item))
            }

            OutlinedButton(
                onClick = onSeedSamples,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("seed_samples_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.explore_load_samples))
            }
        }
    }
}
