package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Listing
import com.example.data.model.MarketplaceConstants
import com.example.data.model.PaymentGatewayType
import com.example.data.model.SellerProfile
import com.example.data.model.SellerReview
import com.example.data.model.ZimbabweLocations
import com.example.ui.components.ListingVisualBox
import com.example.ui.components.StarRatingRow
import com.example.ui.components.VerifiedSellerBadge
import com.example.ui.theme.EscrowSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    listing: Listing?,
    sellerProfile: SellerProfile?,
    sellerReviews: List<SellerReview>,
    currentUserId: String,
    originCoordinates: Pair<Double, Double>,
    displayCurrency: String,
    isFavorite: Boolean,
    isBusy: Boolean,
    defaultDeliveryCity: String,
    onBack: () -> Unit,
    onToggleFavorite: (Listing, Boolean) -> Unit,
    onDeleteListing: (String) -> Unit,
    onChatWithSeller: (Listing) -> Unit,
    onReportListing: (Listing, String, String?) -> Unit,
    onCheckoutSubmit: (
        listing: Listing,
        gateway: PaymentGatewayType,
        maskedAccount: String,
        deliveryCity: String,
        deliveryMethod: String,
        currency: String,
        onComplete: (String) -> Unit
    ) -> Unit,
    onNavigateToOrders: () -> Unit
) {
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var completedOrderId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (listing != null) {
                        if (listing.sellerId != currentUserId) {
                            IconButton(
                                onClick = { showReportDialog = true },
                                modifier = Modifier.testTag("report_listing_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Flag,
                                    contentDescription = stringResource(R.string.report_listing_title),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onToggleFavorite(listing, isFavorite) }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove favorite" else "Save item",
                                tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (listing == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Listing is no longer available", style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = onBack) { Text("Return to Explore") }
                }
            }
            return@Scaffold
        }

        val distanceKm = ZimbabweLocations.distanceKm(
            originCoordinates.first,
            originCoordinates.second,
            listing.latitude,
            listing.longitude
        )
        val isOwner = listing.sellerId == currentUserId
        val photoCount = maxOf(
            listing.imageUrls.size,
            listing.localImageNames.size,
            if (!listing.localImageName.isNullOrBlank()) 1 else 0,
            1
        )
        val pagerState = rememberPagerState(pageCount = { photoCount })
        val isSellerVerified = sellerProfile?.verificationStatus == "verified"
        val avgRating = remember(sellerReviews) {
            if (sellerReviews.isEmpty()) null else sellerReviews.map { it.rating.toDouble() }.average()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(248.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        ListingVisualBox(
                            listing = listing,
                            photoIndex = page,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (photoCount > 1) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.62f),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / $photoCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = MarketplaceConstants.formatPrice(listing.priceUsd, displayCurrency),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = MarketplaceConstants.formatSecondaryPrice(listing.priceUsd, displayCurrency),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = listing.category,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.headlineMedium
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val resolvedProvince = listing.province ?: ZimbabweLocations.findCity(listing.city).province
                            Text(
                                text = "${listing.suburb}, ${listing.city} ($resolvedProvince)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = ZimbabweLocations.formatDistance(distanceKm),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = listing.status.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Description", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = listing.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Seller",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.VerifiedUser,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isOwner) "${listing.sellerName} (You)" else listing.sellerName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                            if (isSellerVerified) {
                                VerifiedSellerBadge(
                                    label = sellerProfile?.verificationBadge?.takeIf { it.isNotBlank() }
                                        ?: "Verified Seller"
                                )
                            }
                        }

                        if (avgRating != null) {
                            StarRatingRow(rating = avgRating, reviewCount = sellerReviews.size)
                        } else {
                            Text(
                                text = "New seller — no completed order reviews yet",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (sellerReviews.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Seller Reviews (${sellerReviews.size})",
                                style = MaterialTheme.typography.titleMedium
                            )
                            sellerReviews.take(4).forEach { rev ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = rev.reviewerName,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        StarRatingRow(rating = rev.rating.toDouble())
                                    }
                                    Text(
                                        text = rev.comment,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.checkout_escrow_banner),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (completedOrderId != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = EscrowSuccess.copy(alpha = 0.14f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EscrowSuccess)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.checkout_success_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EscrowSuccess
                                )
                            }
                            Text(
                                text = "Order ID: $completedOrderId. Track dispatch and release escrow once you receive the item.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = onNavigateToOrders,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("View Order & Escrow Status")
                            }
                        }
                    }
                } else if (isOwner) {
                    OutlinedButton(
                        onClick = { onDeleteListing(listing.id) },
                        enabled = !isBusy && listing.status == "active",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (listing.status == "active") "Remove Your Listing" else "Listing ${listing.status}")
                    }
                } else if (listing.status == "active") {
                    OutlinedButton(
                        onClick = { onChatWithSeller(listing) },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("chat_seller_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Message Seller")
                    }

                    Button(
                        onClick = { showCheckoutSheet = true },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("buy_now_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buy Securely • ${MarketplaceConstants.formatPrice(listing.priceUsd, displayCurrency)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    TextButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("report_listing_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Report this listing",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog && listing != null) {
        ReportListingDialog(
            listingTitle = listing.title,
            isBusy = isBusy,
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                onReportListing(listing, reason, details)
                showReportDialog = false
            }
        )
    }

    if (showCheckoutSheet && listing != null) {
        SecureCheckoutSheet(
            listing = listing,
            displayCurrency = displayCurrency,
            defaultDeliveryCity = defaultDeliveryCity,
            isBusy = isBusy,
            onDismiss = { showCheckoutSheet = false },
            onConfirmPayment = { gateway, maskedAccount, deliveryCity, deliveryMethod ->
                onCheckoutSubmit(
                    listing,
                    gateway,
                    maskedAccount,
                    deliveryCity,
                    deliveryMethod,
                    displayCurrency
                ) { orderId ->
                    completedOrderId = orderId
                    showCheckoutSheet = false
                }
            }
        )
    }
}

@Composable
private fun ReportListingDialog(
    listingTitle: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String?) -> Unit
) {
    var selectedReason by remember { mutableStateOf(MarketplaceConstants.reportReasons.first()) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_listing_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Why are you reporting \"$listingTitle\"?",
                    style = MaterialTheme.typography.bodyMedium
                )
                MarketplaceConstants.reportReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Additional details (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, details.ifBlank { null }) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SecureCheckoutSheet(
    listing: Listing,
    displayCurrency: String,
    defaultDeliveryCity: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirmPayment: (
        gateway: PaymentGatewayType,
        maskedAccount: String,
        deliveryCity: String,
        deliveryMethod: String
    ) -> Unit
) {
    var selectedGateway by remember { mutableStateOf(PaymentGatewayType.ECOCASH) }
    var accountInput by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var deliveryMethod by remember { mutableStateOf(MarketplaceConstants.deliveryMethods.first()) }
    var deliveryCity by remember { mutableStateOf(defaultDeliveryCity.ifBlank { listing.city }) }
    var validationError by remember { mutableStateOf<String?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_secure_pay),
                    contentDescription = stringResource(R.string.cd_escrow_shield),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.checkout_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${listing.title} • ${MarketplaceConstants.formatPrice(listing.priceUsd, displayCurrency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text("1. Delivery / Handover", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MarketplaceConstants.deliveryMethods.forEach { method ->
                    FilterChip(
                        selected = deliveryMethod == method,
                        onClick = { deliveryMethod = method },
                        label = { Text(method) }
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZimbabweLocations.cityNames.take(10).forEach { city ->
                    FilterChip(
                        selected = deliveryCity == city,
                        onClick = { deliveryCity = city },
                        label = { Text(city) }
                    )
                }
            }

            Text("2. Select Zimbabwe Payment Gateway", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentGatewayType.entries.forEach { gateway ->
                    val isSelected = selectedGateway == gateway
                    val borderColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedGateway = gateway
                                validationError = null
                            }
                            .testTag("gateway_${gateway.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (gateway) {
                                    PaymentGatewayType.ZIMSWITCH_CARD -> Icons.Filled.CreditCard
                                    PaymentGatewayType.INNBUCKS -> Icons.Filled.AccountBalance
                                    else -> Icons.Filled.PhoneAndroid
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = gateway.displayName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = gateway.providerTag,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = accountInput,
                onValueChange = {
                    accountInput = it
                    validationError = null
                },
                label = {
                    Text(
                        if (selectedGateway.isCard) "Card Number" else "${selectedGateway.displayName} Mobile Number"
                    )
                },
                placeholder = { Text(selectedGateway.inputHint) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gateway_account_input")
            )

            if (selectedGateway.isCard) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = cardExpiry,
                        onValueChange = {
                            cardExpiry = it
                            validationError = null
                        },
                        label = { Text("Expiry (MM/YY)") },
                        placeholder = { Text("08/28") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cardCvv,
                        onValueChange = {
                            cardCvv = it
                            validationError = null
                        },
                        label = { Text("CVV") },
                        placeholder = { Text("123") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                text = selectedGateway.description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!validationError.isNullOrBlank()) {
                Text(
                    text = validationError ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val maskedResult = selectedGateway.validateAndMask(
                        rawAccount = accountInput,
                        expiry = cardExpiry,
                        cvv = cardCvv
                    )
                    maskedResult.onSuccess { masked ->
                        onConfirmPayment(selectedGateway, masked, deliveryCity, deliveryMethod)
                    }.onFailure { err ->
                        validationError = err.message
                    }
                },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("authorize_payment_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(Icons.Filled.Shield, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.checkout_authorize_cta))
            }
        }
    }
}
