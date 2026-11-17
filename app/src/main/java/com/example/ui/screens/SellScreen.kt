package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.Listing
import com.example.data.model.MarketplaceConstants
import com.example.data.model.ZimbabweLocations
import com.example.ui.components.ListingVisualBox
import com.example.ui.components.fetchCoarseLocation
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SellScreen(
    myListingsState: UiState<List<Listing>>,
    displayCurrency: String,
    isBusy: Boolean,
    onPublishListing: (
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
    ) -> Unit,
    onDeleteListing: (String) -> Unit,
    onOpenListing: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Sell on Mukambo",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Post Item", "My Listings").forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2)
                    ) {
                        Text(label)
                    }
                }
            }

            if (selectedTab == 0) {
                PostListingForm(
                    isBusy = isBusy,
                    onPublishListing = { ctx, uris, title, desc, cat, cond, price, curr, prov, city, sub, lat, lon, cb ->
                        onPublishListing(ctx, uris, title, desc, cat, cond, price, curr, prov, city, sub, lat, lon) { newId ->
                            selectedTab = 1
                            cb(newId)
                        }
                    },
                    onShowMessage = onShowMessage
                )
            } else {
                MyListingsList(
                    state = myListingsState,
                    displayCurrency = displayCurrency,
                    onOpenListing = onOpenListing,
                    onDeleteListing = onDeleteListing
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PostListingForm(
    isBusy: Boolean,
    onPublishListing: (
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
    ) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MarketplaceConstants.categories.first()) }
    var selectedCondition by remember { mutableStateOf(MarketplaceConstants.conditions[1]) }
    var selectedCurrency by remember { mutableStateOf("USD") }
    var selectedProvince by remember { mutableStateOf(ZimbabweLocations.provinces.first()) }
    var selectedCity by remember { mutableStateOf(ZimbabweLocations.cities.first()) }
    var selectedSuburb by remember { mutableStateOf(selectedCity.suburbs.first()) }
    var customCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var selectedPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var formError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(4)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedPhotoUris = (selectedPhotoUris + uris).distinct().take(4)
        }
    }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchCoarseLocation(
                context = context,
                onResult = { lat, lon ->
                    val nearest = ZimbabweLocations.findNearestCity(lat, lon)
                    selectedProvince = nearest.province
                    selectedCity = nearest
                    selectedSuburb = nearest.suburbs.first()
                    customCoords = lat to lon
                    onShowMessage("Pinned near ${nearest.name}, ${nearest.province}")
                },
                onUnavailable = onShowMessage
            )
        } else {
            onShowMessage("Location permission denied. Pick province & city manually.")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Multi-Photo Zero-Permission Photo Picker (up to 4 photos)
        Card(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .testTag("pick_photo_card")
        ) {
            if (selectedPhotoUris.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(selectedPhotoUris) { idx, uri ->
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Selected product photo ${idx + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .clickable {
                                            selectedPhotoUris = selectedPhotoUris.filterIndexed { i, _ -> i != idx }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = "${selectedPhotoUris.size}/4 photos ready (uploads to Firebase Storage • tap to add more)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Add Product Photos (Up to 4)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Android Photo Picker • zero storage permission required",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                formError = null
            },
            label = { Text("Item Title") },
            placeholder = { Text("e.g. 5kVA Solar Inverter or MacBook Air M2") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sell_title_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = priceText,
                onValueChange = {
                    priceText = it
                    formError = null
                },
                label = { Text("Price (USD)") },
                placeholder = { Text("150") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("sell_price_input")
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Display", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MarketplaceConstants.currencies.forEach { curr ->
                        FilterChip(
                            selected = selectedCurrency == curr,
                            onClick = { selectedCurrency = curr },
                            label = { Text(curr) }
                        )
                    }
                }
            }
        }

        Text("Category", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MarketplaceConstants.categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat) }
                )
            }
        }

        Text("Condition", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MarketplaceConstants.conditions.forEach { cond ->
                FilterChip(
                    selected = selectedCondition == cond,
                    onClick = { selectedCondition = cond },
                    label = { Text(cond) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Province & City (All 10 Provinces)", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        fetchCoarseLocation(
                            context = context,
                            onResult = { lat, lon ->
                                val nearest = ZimbabweLocations.findNearestCity(lat, lon)
                                selectedProvince = nearest.province
                                selectedCity = nearest
                                selectedSuburb = nearest.suburbs.first()
                                customCoords = lat to lon
                                onShowMessage("Pinned near ${nearest.name}, ${nearest.province}")
                            },
                            onUnavailable = onShowMessage
                        )
                    } else {
                        gpsPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                }
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pin GPS")
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ZimbabweLocations.provinces.forEach { province ->
                FilterChip(
                    selected = selectedProvince == province,
                    onClick = {
                        selectedProvince = province
                        val provCities = ZimbabweLocations.citiesInProvince(province)
                        if (provCities.isNotEmpty()) {
                            selectedCity = provCities.first()
                            selectedSuburb = selectedCity.suburbs.first()
                            customCoords = null
                        }
                    },
                    label = { Text(province) }
                )
            }
        }

        Text("City in $selectedProvince", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ZimbabweLocations.citiesInProvince(selectedProvince).forEach { city ->
                FilterChip(
                    selected = selectedCity.name == city.name,
                    onClick = {
                        selectedCity = city
                        selectedSuburb = city.suburbs.first()
                        customCoords = null
                    },
                    label = { Text(city.name) }
                )
            }
        }

        Text("Suburb in ${selectedCity.name}", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            selectedCity.suburbs.forEach { sub ->
                FilterChip(
                    selected = selectedSuburb.name == sub.name,
                    onClick = {
                        selectedSuburb = sub
                        customCoords = null
                    },
                    leadingIcon = if (selectedSuburb.name == sub.name) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(15.dp)) }
                    } else null,
                    label = { Text(sub.name) }
                )
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                formError = null
            },
            label = { Text("Description & Meetup Notes") },
            placeholder = { Text("Include condition details, accessories, and preferred meetup area...") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sell_description_input")
        )

        if (!formError.isNullOrBlank()) {
            Text(
                text = formError ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                val price = priceText.toDoubleOrNull()
                when {
                    title.trim().length < 2 -> formError = "Enter an item title (at least 2 characters)"
                    price == null || price <= 0.0 -> formError = "Enter a valid price greater than 0"
                    description.trim().length < 2 -> formError = "Add a brief description of the item"
                    else -> {
                        val lat = customCoords?.first ?: selectedSuburb.latitude
                        val lon = customCoords?.second ?: selectedSuburb.longitude
                        onPublishListing(
                            context,
                            selectedPhotoUris,
                            title,
                            description,
                            selectedCategory,
                            selectedCondition,
                            price,
                            selectedCurrency,
                            selectedProvince,
                            selectedCity.name,
                            selectedSuburb.name,
                            lat,
                            lon
                        ) {
                            title = ""
                            description = ""
                            priceText = ""
                            selectedPhotoUris = emptyList()
                        }
                    }
                }
            },
            enabled = !isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("publish_listing_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uploading & Publishing...")
            } else {
                Icon(Icons.Filled.Publish, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publish Listing")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MyListingsList(
    state: UiState<List<Listing>>,
    displayCurrency: String,
    onOpenListing: (String) -> Unit,
    onDeleteListing: (String) -> Unit
) {
    when (state) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "You haven't posted any listings yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.data, key = { it.id }) { listing ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenListing(listing.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ListingVisualBox(
                                    listing = listing,
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = listing.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${MarketplaceConstants.formatPrice(listing.priceUsd, displayCurrency)} • ${listing.suburb}, ${listing.city}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Status: ${listing.status.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (listing.status == "active") {
                                    IconButton(onClick = { onDeleteListing(listing.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete listing",
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
