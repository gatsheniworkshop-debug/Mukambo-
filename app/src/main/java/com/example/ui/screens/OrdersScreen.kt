package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.MarketplaceConstants
import com.example.data.model.Order
import com.example.data.model.SellerReview
import com.example.ui.components.EscrowStatusBadge
import com.example.ui.theme.EscrowSuccess
import com.example.ui.theme.GoldHighlight
import com.example.ui.viewmodel.UiState
import com.google.firebase.Timestamp

@Composable
fun OrdersScreen(
    buyerOrdersState: UiState<List<Order>>,
    sellerOrdersState: UiState<List<Order>>,
    myReviews: List<SellerReview>,
    displayCurrency: String,
    isBusy: Boolean,
    onUpdateTracking: (
        order: Order,
        nextStatus: String,
        courierName: String?,
        trackingCode: String?,
        estimatedDelivery: String?,
        trackingNotes: String?,
        onDone: () -> Unit
    ) -> Unit,
    onReleaseEscrow: (String) -> Unit,
    onDisputeOrder: (String, String?) -> Unit,
    onSubmitReview: (order: Order, rating: Int, comment: String, onDone: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var trackingDialogOrder by remember { mutableStateOf<Pair<Order, String>?>(null) }
    var reviewDialogOrder by remember { mutableStateOf<Order?>(null) }
    var disputeDialogOrder by remember { mutableStateOf<Order?>(null) }

    val reviewedOrderIds = remember(myReviews) {
        myReviews.map { it.orderId }.toSet()
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Orders & Tracking",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Purchases", "Sales").forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2)
                    ) {
                        Text(label)
                    }
                }
            }

            val activeState = if (selectedTab == 0) buyerOrdersState else sellerOrdersState
            val isBuyerView = selectedTab == 0

            when (activeState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Text(activeState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    val sorted = activeState.data.sortedByDescending {
                        it.createdAt ?: Timestamp(0, 0)
                    }
                    if (sorted.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = if (isBuyerView) "No purchases yet" else "No sales orders yet",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "All EcoCash, InnBucks, OneMoney & ZimSwitch orders appear here with live dispatch tracking.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(sorted, key = { it.id }) { order ->
                                val alreadyReviewed = reviewedOrderIds.contains(order.id)
                                OrderEscrowCard(
                                    order = order,
                                    isBuyerView = isBuyerView,
                                    displayCurrency = displayCurrency,
                                    isBusy = isBusy,
                                    alreadyReviewed = alreadyReviewed,
                                    onAdvanceTracking = { nextStatus ->
                                        trackingDialogOrder = order to nextStatus
                                    },
                                    onReleaseEscrow = { onReleaseEscrow(order.id) },
                                    onOpenDispute = { disputeDialogOrder = order },
                                    onOpenReview = { reviewDialogOrder = order }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    trackingDialogOrder?.let { (order, nextStatus) ->
        TrackingUpdateDialog(
            order = order,
            nextStatus = nextStatus,
            isBusy = isBusy,
            onDismiss = { trackingDialogOrder = null },
            onConfirm = { courier, code, eta, notes ->
                onUpdateTracking(order, nextStatus, courier, code, eta, notes) {
                    trackingDialogOrder = null
                }
            }
        )
    }

    reviewDialogOrder?.let { order ->
        RateSellerDialog(
            order = order,
            isBusy = isBusy,
            onDismiss = { reviewDialogOrder = null },
            onSubmit = { rating, comment ->
                onSubmitReview(order, rating, comment) {
                    reviewDialogOrder = null
                }
            }
        )
    }

    disputeDialogOrder?.let { order ->
        DisputeOrderDialog(
            order = order,
            isBusy = isBusy,
            onDismiss = { disputeDialogOrder = null },
            onSubmit = { reasonNote ->
                onDisputeOrder(order.id, reasonNote)
                disputeDialogOrder = null
            }
        )
    }
}

@Composable
private fun OrderEscrowCard(
    order: Order,
    isBuyerView: Boolean,
    displayCurrency: String,
    isBusy: Boolean,
    alreadyReviewed: Boolean,
    onAdvanceTracking: (String) -> Unit,
    onReleaseEscrow: () -> Unit,
    onOpenDispute: () -> Unit,
    onOpenReview: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
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
                Text(
                    text = order.listingTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                EscrowStatusBadge(status = order.escrowStatus)
            }

            Text(
                text = MarketplaceConstants.formatPrice(order.amountUsd, displayCurrency),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            // 5-step Order Tracking Timeline
            OrderTrackingTimeline(escrowStatus = order.escrowStatus)

            HorizontalDivider()

            OrderMetaRow("Gateway", "${order.paymentGateway} (${order.maskedAccount})")
            OrderMetaRow("Reference", order.gatewayReference)
            OrderMetaRow("Fulfillment", "${order.deliveryMethod} • ${order.deliveryCity}")
            OrderMetaRow(
                if (isBuyerView) "Seller" else "Buyer",
                if (isBuyerView) order.sellerName else order.buyerName
            )
            if (!order.courierName.isNullOrBlank()) {
                OrderMetaRow("Courier", order.courierName)
            }
            if (!order.trackingCode.isNullOrBlank()) {
                OrderMetaRow("Tracking Code", order.trackingCode)
            }
            if (!order.estimatedDelivery.isNullOrBlank()) {
                OrderMetaRow("Estimated Arrival", order.estimatedDelivery)
            }
            if (!order.trackingNotes.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tracking Note: ${order.trackingNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            val buyerCanAct = isBuyerView &&
                order.escrowStatus in listOf("escrow_held", "dispatched", "in_transit", "delivered")
            if (buyerCanAct) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onReleaseEscrow,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm & Release")
                    }
                    OutlinedButton(
                        onClick = onOpenDispute,
                        enabled = !isBusy,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Filled.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dispute")
                    }
                }
            } else if (isBuyerView && order.escrowStatus == "released") {
                if (alreadyReviewed) {
                    Surface(
                        color = EscrowSuccess.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = GoldHighlight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Seller review submitted — thank you!",
                                style = MaterialTheme.typography.labelLarge,
                                color = EscrowSuccess
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onOpenReview,
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rate_seller_${order.id}")
                    ) {
                        Icon(Icons.Filled.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rate & Review Seller")
                    }
                }
            } else if (!isBuyerView) {
                val nextSellerAction = when (order.escrowStatus) {
                    "escrow_held" -> "dispatched" to "Mark Dispatched & Add Tracking"
                    "dispatched" -> "in_transit" to "Update: In Transit"
                    "in_transit" -> "delivered" to "Mark Delivered"
                    else -> null
                }
                if (nextSellerAction != null) {
                    Button(
                        onClick = { onAdvanceTracking(nextSellerAction.first) },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(nextSellerAction.second)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderTrackingTimeline(escrowStatus: String) {
    val steps = MarketplaceConstants.orderTrackingSteps
    val currentStepIndex = steps.indexOfFirst { it.first == escrowStatus }

    if (escrowStatus == "disputed" || escrowStatus == "refunded") {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (escrowStatus == "disputed") {
                    "Order is under Admin Dispute Review — escrow funds frozen safely."
                } else {
                    "Order resolved & refunded to buyer by Mukambo Admin."
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(10.dp)
            )
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { idx, (_, label) ->
            val completed = idx <= currentStepIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = if (completed) EscrowSuccess else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (completed) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = (idx + 1).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (completed) EscrowSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (completed) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrackingUpdateDialog(
    order: Order,
    nextStatus: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (courier: String?, trackingCode: String?, eta: String?, notes: String?) -> Unit
) {
    var courier by remember { mutableStateOf(order.courierName ?: MarketplaceConstants.courierOptions.first()) }
    var trackingCode by remember { mutableStateOf(order.trackingCode ?: "") }
    var eta by remember { mutableStateOf(order.estimatedDelivery ?: "1-2 business days") }
    var notes by remember { mutableStateOf("") }

    val statusTitle = nextStatus.replace('_', ' ').replaceFirstChar { it.uppercase() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Order: $statusTitle") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Courier / Handover", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MarketplaceConstants.courierOptions.forEach { opt ->
                        FilterChip(
                            selected = courier == opt,
                            onClick = { courier = opt },
                            label = { Text(opt) }
                        )
                    }
                }
                OutlinedTextField(
                    value = trackingCode,
                    onValueChange = { trackingCode = it },
                    label = { Text("Waybill / Tracking Code") },
                    placeholder = { Text("e.g. SW-884210") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = eta,
                    onValueChange = { eta = it },
                    label = { Text("Estimated Arrival") },
                    placeholder = { Text("Tomorrow afternoon") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Tracking Note for Buyer") },
                    placeholder = { Text("Parcel dropped at Harare Roadport depot") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        courier.ifBlank { null },
                        trackingCode.ifBlank { null },
                        eta.ifBlank { null },
                        notes.ifBlank { null }
                    )
                },
                enabled = !isBusy
            ) {
                Text("Save Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RateSellerDialog(
    order: Order,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate ${order.sellerName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Share your experience for \"${order.listingTitle}\"",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (star in 1..5) {
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarOutline,
                                contentDescription = "$star stars",
                                tint = GoldHighlight,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = {
                        comment = it
                        error = null
                    },
                    label = { Text("Review Comment") },
                    placeholder = { Text("Fast dispatch, item matched photos...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_comment_input")
                )
                if (!error.isNullOrBlank()) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (comment.trim().length < 2) {
                        error = "Please enter a brief review comment."
                    } else {
                        onSubmit(rating, comment)
                    }
                },
                enabled = !isBusy
            ) {
                Text("Submit Review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DisputeOrderDialog(
    order: Order,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String?) -> Unit
) {
    var reasonNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dispute Order") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Freezing escrow for \"${order.listingTitle}\" pauses seller payout while Mukambo Admin reviews.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = reasonNote,
                    onValueChange = { reasonNote = it },
                    label = { Text("Reason for Dispute") },
                    placeholder = { Text("Item not received / different from description...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(reasonNote.ifBlank { null }) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Freeze Escrow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun OrderMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
