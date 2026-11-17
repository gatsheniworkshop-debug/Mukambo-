package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.FavoriteItem
import com.example.data.model.MarketplaceConstants
import com.example.data.model.SellerProfile
import com.example.data.model.UserProfile
import com.example.data.model.ZimbabweLocations
import com.example.ui.auth.signOut
import com.example.ui.components.VerifiedSellerBadge
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userProfileState: UiState<UserProfile?>,
    sellerProfileState: UiState<SellerProfile?>,
    favoritesState: UiState<List<FavoriteItem>>,
    fallbackName: String,
    fallbackEmail: String?,
    displayCurrency: String,
    isAdmin: Boolean,
    isBusy: Boolean,
    onSaveProfile: (
        displayName: String,
        phone: String?,
        defaultCity: String,
        preferredCurrency: String,
        notificationsEnabled: Boolean
    ) -> Unit,
    onRequestVerification: (businessName: String, city: String, note: String, onDone: () -> Unit) -> Unit,
    onOpenListing: (String) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    onOpenAdmin: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val loadedProfile = (userProfileState as? UiState.Success)?.data
    val sellerProfile = (sellerProfileState as? UiState.Success)?.data
    var nameInput by remember { mutableStateOf(fallbackName) }
    var phoneInput by remember { mutableStateOf("") }
    var provinceInput by remember { mutableStateOf(ZimbabweLocations.provinces.first()) }
    var cityInput by remember { mutableStateOf("Harare") }
    var currencyInput by remember { mutableStateOf(displayCurrency) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showVerificationDialog by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        onShowMessage(
            if (granted) "Push notifications enabled"
            else "Notification permission denied — you can still view alerts in Explore"
        )
    }

    LaunchedEffect(loadedProfile) {
        if (loadedProfile != null) {
            nameInput = loadedProfile.displayName
            phoneInput = loadedProfile.phoneNumber ?: ""
            cityInput = loadedProfile.defaultCity
            provinceInput = loadedProfile.defaultProvince?.takeIf { it.isNotBlank() }
                ?: ZimbabweLocations.findCity(loadedProfile.defaultCity).province
            currencyInput = loadedProfile.preferredCurrency
            notificationsEnabled = loadedProfile.notificationsEnabled
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Account & Safety",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            // User Header Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = nameInput.ifBlank { fallbackName },
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (sellerProfile?.verificationStatus == "verified") {
                                VerifiedSellerBadge(
                                    label = sellerProfile.verificationBadge ?: "Verified",
                                    compact = true
                                )
                            }
                        }
                        val emailToShow = loadedProfile?.email ?: fallbackEmail
                        if (!emailToShow.isNullOrBlank()) {
                            Text(
                                text = emailToShow,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Admin Dashboard Access Card (bootstrapped/authorized admins only)
            if (isAdmin) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.admin_dashboard_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Moderate reported listings, approve seller badges, and resolve disputes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Button(
                            onClick = onOpenAdmin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_admin_dashboard_button")
                        ) {
                            Text("Open Admin Moderation Console")
                        }
                    }
                }
            }

            // Seller Verification Badge Card
            Text(
                text = stringResource(R.string.verification_title),
                style = MaterialTheme.typography.titleLarge
            )
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val status = sellerProfile?.verificationStatus ?: "unverified"
                    when (status) {
                        "verified" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VerifiedSellerBadge(
                                    label = sellerProfile?.verificationBadge ?: "Verified Merchant"
                                )
                                sellerProfile?.businessName?.let {
                                    Text(it, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Text(
                                text = "Your verified badge appears next to your listings in Explore and Item Details.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        "pending" -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.HourglassTop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verification Pending Review",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Text(
                                text = "Submitted business: ${sellerProfile?.businessName ?: nameInput}. A Mukambo Admin will verify your profile shortly.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Get a Verified Seller Badge",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = "Build buyer trust across Zimbabwe by verifying your shop or personal trader profile.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { showVerificationDialog = true },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("apply_verification_button")
                            ) {
                                Text("Apply for Verification")
                            }
                        }
                    }
                }
            }

            // Notifications Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Order & Chat Notifications", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Receive instant alerts on new inquiries & parcel dispatches",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        OutlinedButton(
                            onClick = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Allow Device Push Alerts (Android 13+)")
                        }
                    }
                }
            }

            // Saved Wishlist Section
            Text(
                text = stringResource(R.string.wishlist_title),
                style = MaterialTheme.typography.titleLarge
            )
            when (favoritesState) {
                is UiState.Loading -> Text("Loading saved items...", style = MaterialTheme.typography.bodyMedium)
                is UiState.Error -> Text(favoritesState.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    if (favoritesState.data.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Tap the heart on any item in Explore to save it to your wishlist.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            favoritesState.data.forEach { fav ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onOpenListing(fav.listingId) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(fav.title, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = "${fav.city} • ${fav.category ?: "Marketplace"}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = MarketplaceConstants.formatPrice(fav.priceUsd, displayCurrency),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
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

            // Preferences Card
            Text(
                text = "Preferences & Mobile Wallet Contact",
                style = MaterialTheme.typography.titleLarge
            )
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Zimbabwe Mobile (EcoCash / InnBucks)") },
                        placeholder = { Text("+263 77 123 4567") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Default Province", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ZimbabweLocations.provinces.forEach { province ->
                            FilterChip(
                                selected = provinceInput == province,
                                onClick = {
                                    provinceInput = province
                                    val firstCity = ZimbabweLocations.citiesInProvince(province).firstOrNull()
                                    if (firstCity != null) cityInput = firstCity.name
                                },
                                label = { Text(province) }
                            )
                        }
                    }

                    Text("Default City in $provinceInput", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ZimbabweLocations.citiesInProvince(provinceInput).forEach { city ->
                            FilterChip(
                                selected = cityInput == city.name,
                                onClick = { cityInput = city.name },
                                label = { Text(city.name) }
                            )
                        }
                    }

                    Text("Preferred Currency", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MarketplaceConstants.currencies.forEach { curr ->
                            FilterChip(
                                selected = currencyInput == curr,
                                onClick = { currencyInput = curr },
                                label = { Text(curr) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onSaveProfile(
                                nameInput,
                                phoneInput.ifBlank { null },
                                cityInput,
                                currencyInput,
                                notificationsEnabled
                            )
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_profile_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Preferences")
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    signOut(
                        credentialManager = credentialManager,
                        scope = coroutineScope
                    )
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showVerificationDialog) {
        SellerVerificationDialog(
            defaultCity = cityInput,
            isBusy = isBusy,
            onDismiss = { showVerificationDialog = false },
            onSubmit = { businessName, note ->
                onRequestVerification(businessName, cityInput, note) {
                    showVerificationDialog = false
                }
            }
        )
    }
}

@Composable
private fun SellerVerificationDialog(
    defaultCity: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (businessName: String, note: String) -> Unit
) {
    var businessName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply for Seller Verification") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Trading City: $defaultCity. Provide your shop or trader name and details for Mukambo Admin review.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = businessName,
                    onValueChange = {
                        businessName = it
                        error = null
                    },
                    label = { Text("Shop / Trader Name") },
                    placeholder = { Text("e.g. Harare Solar Hub Pvt Ltd") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                        error = null
                    },
                    label = { Text("Verification Details") },
                    placeholder = { Text("Physical address, registration number, or marketplace references...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!error.isNullOrBlank()) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (businessName.trim().length < 2) {
                        error = "Enter a business or trader name"
                    } else {
                        onSubmit(businessName, note)
                    }
                },
                enabled = !isBusy
            ) {
                Text("Submit Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
