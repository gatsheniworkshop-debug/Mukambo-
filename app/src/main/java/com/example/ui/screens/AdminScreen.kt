package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ListingReport
import com.example.data.model.MarketplaceConstants
import com.example.data.model.Order
import com.example.data.model.SellerProfile
import com.example.ui.components.EscrowStatusBadge
import com.example.ui.theme.EscrowSuccess
import com.example.ui.viewmodel.UiState
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    isAdmin: Boolean,
    reportsState: UiState<List<ListingReport>>,
    pendingVerificationsState: UiState<List<SellerProfile>>,
    disputedOrdersState: UiState<List<Order>>,
    displayCurrency: String,
    isBusy: Boolean,
    onBack: () -> Unit,
    onModerateReport: (report: ListingReport, removeListing: Boolean, adminNotes: String) -> Unit,
    onDecideVerification: (sellerId: String, approve: Boolean, badgeLabel: String) -> Unit,
    onResolveDispute: (order: Order, releaseToSeller: Boolean, resolutionNote: String) -> Unit,
    onOpenListing: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_dashboard_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!isAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(44.dp)
                    )
                    Text("Admin Access Required", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "This moderation dashboard is restricted to authorized Mukambo administrators.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onBack) { Text("Go Back") }
                }
            }
            return@Scaffold
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
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val openReportsCount = (reportsState as? UiState.Success)?.data?.count { it.status == "open" } ?: 0
                val verifCount = (pendingVerificationsState as? UiState.Success)?.data?.size ?: 0
                val disputeCount = (disputedOrdersState as? UiState.Success)?.data?.size ?: 0

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        "Reports ($openReportsCount)",
                        "Verify ($verifCount)",
                        "Disputes ($disputeCount)"
                    ).forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3)
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }

                when (selectedTab) {
                    0 -> ReportsModerationTab(
                        state = reportsState,
                        isBusy = isBusy,
                        onModerate = onModerateReport,
                        onOpenListing = onOpenListing
                    )
                    1 -> VerificationsModerationTab(
                        state = pendingVerificationsState,
                        isBusy = isBusy,
                        onDecide = onDecideVerification
                    )
                    else -> DisputesResolutionTab(
                        state = disputedOrdersState,
                        displayCurrency = displayCurrency,
                        isBusy = isBusy,
                        onResolve = onResolveDispute
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportsModerationTab(
    state: UiState<List<ListingReport>>,
    isBusy: Boolean,
    onModerate: (ListingReport, Boolean, String) -> Unit,
    onOpenListing: (String) -> Unit
) {
    when (state) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
        is UiState.Success -> {
            val sorted = state.data.sortedWith(
                compareBy<ListingReport> { if (it.status == "open") 0 else 1 }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
            if (sorted.isEmpty()) {
                AdminEmptyPane("No reported listings in queue")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sorted, key = { it.id }) { report ->
                        var notes by remember(report.id) { mutableStateOf("") }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = report.listingTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        color = if (report.status == "open") {
                                            MaterialTheme.colorScheme.errorContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text(
                                            text = report.status.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Reason: ${report.reason}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Reported by: ${report.reporterName}" +
                                        (report.details?.let { "\nDetails: $it" } ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (report.status == "open") {
                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text("Moderation note") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onModerate(report, true, notes) },
                                            enabled = !isBusy,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Remove Item")
                                        }
                                        OutlinedButton(
                                            onClick = { onModerate(report, false, notes) },
                                            enabled = !isBusy,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Dismiss")
                                        }
                                    }
                                } else if (!report.adminNotes.isNullOrBlank()) {
                                    Text(
                                        text = "Admin Note: ${report.adminNotes}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onOpenListing(report.listingId) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Inspect Listing")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationsModerationTab(
    state: UiState<List<SellerProfile>>,
    isBusy: Boolean,
    onDecide: (String, Boolean, String) -> Unit
) {
    when (state) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                AdminEmptyPane("No pending seller verification requests")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.data, key = { it.sellerId }) { seller ->
                        var badgeLabel by remember(seller.sellerId) { mutableStateOf("Verified Merchant") }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = seller.businessName?.takeIf { it.isNotBlank() } ?: seller.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Owner: ${seller.displayName} • ${seller.city} (${seller.province ?: "Zimbabwe"})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                seller.verificationNote?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = "Note: $it",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                OutlinedTextField(
                                    value = badgeLabel,
                                    onValueChange = { badgeLabel = it },
                                    label = { Text("Badge Label") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onDecide(seller.sellerId, true, badgeLabel) },
                                        enabled = !isBusy,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Approve")
                                    }
                                    OutlinedButton(
                                        onClick = { onDecide(seller.sellerId, false, badgeLabel) },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Decline")
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

@Composable
private fun DisputesResolutionTab(
    state: UiState<List<Order>>,
    displayCurrency: String,
    isBusy: Boolean,
    onResolve: (Order, Boolean, String) -> Unit
) {
    when (state) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                AdminEmptyPane("No active escrow disputes")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.data, key = { it.id }) { order ->
                        var note by remember(order.id) { mutableStateOf("") }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = order.listingTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    EscrowStatusBadge(status = order.escrowStatus)
                                }
                                Text(
                                    text = MarketplaceConstants.formatPrice(order.amountUsd, displayCurrency),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Buyer: ${order.buyerName} • Seller: ${order.sellerName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                order.trackingNotes?.takeIf { it.isNotBlank() }?.let {
                                    Text("Dispute Note: $it", style = MaterialTheme.typography.bodyMedium)
                                }
                                HorizontalDivider()
                                OutlinedTextField(
                                    value = note,
                                    onValueChange = { note = it },
                                    label = { Text("Resolution note") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onResolve(order, true, note) },
                                        enabled = !isBusy,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Release Seller")
                                    }
                                    OutlinedButton(
                                        onClick = { onResolve(order, false, note) },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Refund Buyer")
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

@Composable
private fun AdminEmptyPane(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = EscrowSuccess,
                modifier = Modifier.size(44.dp)
            )
            Text(text = message, style = MaterialTheme.typography.titleMedium)
        }
    }
}
