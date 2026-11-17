package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.FavoriteItem
import com.example.data.model.Listing
import com.example.data.model.MarketplaceConstants
import com.example.data.model.ZimbabweLocations
import com.example.ui.theme.DisputeError
import com.example.ui.theme.EscrowSuccess
import com.example.ui.theme.GoldHighlight
import com.example.ui.viewmodel.ListingSortOrder
import com.example.ui.viewmodel.LocationFilterState
import com.example.ui.viewmodel.UiState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Timestamp
import java.io.File
import java.util.Locale

fun categoryIcon(category: String): ImageVector = when (category) {
    "Electronics" -> Icons.Filled.Devices
    "Solar & Power" -> Icons.Filled.Bolt
    "Vehicles" -> Icons.Filled.DirectionsCar
    "Home & Furniture" -> Icons.Filled.Chair
    "Fashion" -> Icons.Filled.Checkroom
    "Agriculture" -> Icons.Filled.Agriculture
    "Crafts & Art" -> Icons.Filled.Palette
    else -> Icons.Filled.Storefront
}

fun categoryGradientColors(category: String): Pair<Color, Color> = when (category) {
    "Solar & Power" -> Color(0xFF0B4F3A) to Color(0xFF1F7A54)
    "Electronics" -> Color(0xFF1B3B4B) to Color(0xFF2D6A82)
    "Vehicles" -> Color(0xFF3E352F) to Color(0xFF6B594C)
    "Home & Furniture" -> Color(0xFF8C4227) to Color(0xFFC85A32)
    "Fashion" -> Color(0xFF6C2E4E) to Color(0xFFA64B76)
    "Agriculture" -> Color(0xFF2E5E2E) to Color(0xFF538D3F)
    "Crafts & Art" -> Color(0xFF8F6500) to Color(0xFFD69F1A)
    else -> Color(0xFF0B4F3A) to Color(0xFF296E52)
}

@Composable
fun ListingVisualBox(
    listing: Listing,
    modifier: Modifier = Modifier,
    photoIndex: Int = 0,
    showConditionBadge: Boolean = true
) {
    val context = LocalContext.current
    val remoteUrl = listing.imageUrls.getOrNull(photoIndex)?.takeIf { it.isNotBlank() }
    val localName = listing.localImageNames.getOrNull(photoIndex)?.takeIf { it.isNotBlank() }
        ?: if (photoIndex == 0) listing.localImageName?.takeIf { it.isNotBlank() } else null
    val localFile = localName?.let { File(context.filesDir, it) }?.takeIf { it.exists() }
    val (startColor, endColor) = categoryGradientColors(listing.category)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(startColor, endColor)))
    ) {
        when {
            remoteUrl != null -> {
                AsyncImage(
                    model = remoteUrl,
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            localFile != null -> {
                AsyncImage(
                    model = localFile,
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = categoryIcon(listing.category),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier
                            .size(76.dp)
                            .align(Alignment.BottomEnd)
                    )
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.Center)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = categoryIcon(listing.category),
                                contentDescription = listing.category,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showConditionBadge) {
            Surface(
                color = Color.Black.copy(alpha = 0.56f),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = listing.condition,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun VerifiedSellerBadge(
    label: String = "Verified",
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = EscrowSuccess.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 7.dp else 10.dp,
                vertical = if (compact) 2.dp else 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = "Verified Seller",
                tint = EscrowSuccess,
                modifier = Modifier.size(if (compact) 12.dp else 15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = EscrowSuccess,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StarRatingRow(
    rating: Double,
    reviewCount: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 1..5) {
            val icon = when {
                rating >= i - 0.25 -> Icons.Filled.Star
                rating >= i - 0.75 -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Filled.StarOutline
            }
            val tint = if (rating >= i - 0.75) GoldHighlight else MaterialTheme.colorScheme.outline
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = String.format(Locale.US, "%.1f", rating) +
                (reviewCount?.let { " ($it)" } ?: ""),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CurrencyToggleChip(
    currency: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onToggle)
            .testTag("currency_toggle_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currency == "USD") "USD $" else "ZiG",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EscrowStatusBadge(status: String, modifier: Modifier = Modifier) {
    val (label, bg, fg, icon) = when (status) {
        "escrow_held" -> Quad(
            "In Escrow",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Filled.Shield
        )
        "dispatched" -> Quad(
            "Dispatched",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Filled.Inventory2
        )
        "in_transit" -> Quad(
            "In Transit",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Filled.LocalShipping
        )
        "delivered" -> Quad(
            "Delivered",
            EscrowSuccess.copy(alpha = 0.14f),
            EscrowSuccess,
            Icons.Filled.CheckCircle
        )
        "released" -> Quad(
            "Released",
            EscrowSuccess.copy(alpha = 0.14f),
            EscrowSuccess,
            Icons.Filled.Verified
        )
        "disputed" -> Quad(
            "Disputed",
            DisputeError.copy(alpha = 0.14f),
            DisputeError,
            Icons.Filled.Gavel
        )
        "refunded" -> Quad(
            "Refunded",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Filled.Payments
        )
        else -> Quad(
            status,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Filled.Shield
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@SuppressLint("MissingPermission")
fun fetchCoarseLocation(
    context: Context,
    onResult: (Double, Double) -> Unit,
    onUnavailable: (String) -> Unit
) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .addOnSuccessListener { location ->
            if (location != null) {
                onResult(location.latitude, location.longitude)
            } else {
                client.lastLocation.addOnSuccessListener { last ->
                    if (last != null) {
                        onResult(last.latitude, last.longitude)
                    } else {
                        onUnavailable("GPS signal unavailable. Pick a city below instead.")
                    }
                }.addOnFailureListener {
                    onUnavailable("Could not read device location. Pick a city below.")
                }
            }
        }
        .addOnFailureListener {
            onUnavailable("Could not read device location. Pick a city below.")
        }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LocationFilterSheet(
    state: LocationFilterState,
    onSelectProvince: (String) -> Unit,
    onSelectCity: (String) -> Unit,
    onSelectRadius: (Int?) -> Unit,
    onSelectSort: (ListingSortOrder) -> Unit,
    onGpsReceived: (Double, Double) -> Unit,
    onGpsMessage: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchCoarseLocation(
                context = context,
                onResult = { lat, lon ->
                    onGpsReceived(lat, lon)
                    onDismiss()
                },
                onUnavailable = onGpsMessage
            )
        } else {
            onGpsMessage("Location permission denied. Select a Zimbabwe city manually.")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zimbabwe Location Filter",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Browse across all 10 provinces & cities",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = {
                    onClearAll()
                    onDismiss()
                }) {
                    Text("Reset")
                }
            }

            OutlinedButton(
                onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        fetchCoarseLocation(
                            context = context,
                            onResult = { lat, lon ->
                                onGpsReceived(lat, lon)
                                onDismiss()
                            },
                            onUnavailable = onGpsMessage
                        )
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("use_gps_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use My Current GPS Location")
            }

            HorizontalDivider()

            Text(
                text = "Province (All 10 Zimbabwe Provinces)",
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val provinceOptions = listOf("All Provinces") + ZimbabweLocations.provinces
                provinceOptions.forEach { province ->
                    val selected = state.selectedProvince == province
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectProvince(province) },
                        label = { Text(province) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            Text(
                text = if (state.selectedProvince == "All Provinces") {
                    "City / Town"
                } else {
                    "Cities in ${state.selectedProvince}"
                },
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val visibleCities = listOf("All Zimbabwe") +
                    ZimbabweLocations.citiesInProvince(state.selectedProvince).map { it.name }
                visibleCities.forEach { city ->
                    val selected = state.selectedCity == city
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectCity(city) },
                        label = { Text(city) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Text(
                text = "Maximum Distance Radius",
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MarketplaceConstants.radiusOptionsKm.forEach { km ->
                    val label = if (km == null) "Any Distance" else "Within $km km"
                    FilterChip(
                        selected = state.maxRadiusKm == km,
                        onClick = { onSelectRadius(km) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            Text(
                text = "Sort Results",
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ListingSortOrder.entries.forEach { order ->
                    FilterChip(
                        selected = state.sortOrder == order,
                        onClick = { onSelectSort(order) },
                        label = { Text(order.label) }
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Apply Location Filter")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInboxSheet(
    notificationsState: UiState<List<AppNotification>>,
    onMarkRead: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.notifications_title),
                style = MaterialTheme.typography.headlineMedium
            )
            when (notificationsState) {
                is UiState.Loading -> Text("Loading updates...", style = MaterialTheme.typography.bodyMedium)
                is UiState.Error -> Text(notificationsState.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    val sorted = notificationsState.data.sortedByDescending {
                        it.createdAt ?: Timestamp(0, 0)
                    }
                    if (sorted.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsNone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "You're all caught up",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Order dispatches, escrow alerts, messages, and verification updates appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(340.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sorted, key = { it.id }) { notif ->
                                Card(
                                    onClick = { if (!notif.read) onMarkRead(notif.id) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (notif.read) {
                                            MaterialTheme.colorScheme.background
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        }
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        if (!notif.read) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 6.dp, end = 10.dp)
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondary)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = notif.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = if (notif.read) FontWeight.Medium else FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = notif.body,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistBottomSheet(
    favoritesState: UiState<List<FavoriteItem>>,
    displayCurrency: String,
    onOpenListing: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.wishlist_title),
                style = MaterialTheme.typography.headlineMedium
            )
            when (favoritesState) {
                is UiState.Loading -> Text("Loading wishlist...", style = MaterialTheme.typography.bodyMedium)
                is UiState.Error -> Text(favoritesState.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    if (favoritesState.data.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text("No saved items yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Tap the heart icon on any listing in Explore to save it to your wishlist.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(340.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(favoritesState.data, key = { it.listingId }) { fav ->
                                Card(
                                    onClick = {
                                        onDismiss()
                                        onOpenListing(fav.listingId)
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = fav.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${fav.city} • ${fav.category ?: "Marketplace"}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = MarketplaceConstants.formatPrice(fav.priceUsd, displayCurrency),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        IconButton(onClick = { onRemoveFavorite(fav.listingId) }) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "Remove from wishlist",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
