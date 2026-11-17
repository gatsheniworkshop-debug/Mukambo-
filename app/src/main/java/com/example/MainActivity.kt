package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.MukamboRepository
import com.example.notifications.MukamboNotificationHelper
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.authStateFlow
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ChatThreadScreen
import com.example.ui.screens.ChatsListScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.ListingDetailScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SellScreen
import com.example.ui.theme.MukamboTheme
import com.example.ui.viewmodel.MarketplaceViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.flowOf

const val ROUTE_EXPLORE = "explore"
const val ROUTE_SELL = "sell"
const val ROUTE_MESSAGES = "messages"
const val ROUTE_ORDERS = "orders"
const val ROUTE_PROFILE = "profile"
const val ROUTE_DETAIL = "detail/{listingId}"
const val ROUTE_CHAT_THREAD = "chat/{chatId}"
const val ROUTE_ADMIN = "admin"

private data class TopTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val topTabs = listOf(
    TopTab(ROUTE_EXPLORE, R.string.nav_explore, Icons.Filled.Explore, Icons.Outlined.Explore),
    TopTab(ROUTE_SELL, R.string.nav_sell, Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
    TopTab(
        ROUTE_MESSAGES,
        R.string.nav_chat,
        Icons.AutoMirrored.Filled.Chat,
        Icons.AutoMirrored.Outlined.Chat
    ),
    TopTab(
        ROUTE_ORDERS,
        R.string.nav_orders,
        Icons.AutoMirrored.Filled.ReceiptLong,
        Icons.AutoMirrored.Outlined.ReceiptLong
    ),
    TopTab(ROUTE_PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MukamboNotificationHelper.ensureChannel(applicationContext)
        setContent {
            MukamboTheme {
                MukamboRoot()
            }
        }
    }
}

@Composable
fun MukamboRoot(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            MukamboAppShell(
                currentUserId = user.uid,
                currentUserName = user.displayName ?: "Mukambo User",
                currentUserEmail = user.email
            )
        }
    }
}

@Composable
fun MukamboAppShell(
    currentUserId: String,
    currentUserName: String,
    currentUserEmail: String?
) {
    val context = LocalContext.current
    val viewModel: MarketplaceViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MarketplaceViewModel(
                    repository = MukamboRepository(db),
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    currentUserEmail = currentUserEmail
                )
            }
        }
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showTopLevelNav = currentRoute in topTabs.map { it.route }

    val snackbarHostState = remember { SnackbarHostState() }
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val filteredListingsState by viewModel.filteredListingsState.collectAsStateWithLifecycle()
    val activeListingsState by viewModel.activeListingsState.collectAsStateWithLifecycle()
    val myListingsState by viewModel.myListingsState.collectAsStateWithLifecycle()
    val buyerOrdersState by viewModel.buyerOrdersState.collectAsStateWithLifecycle()
    val sellerOrdersState by viewModel.sellerOrdersState.collectAsStateWithLifecycle()
    val favoritesState by viewModel.favoritesState.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val myReviewsState by viewModel.myReviewsState.collectAsStateWithLifecycle()
    val mySellerProfileState by viewModel.mySellerProfileState.collectAsStateWithLifecycle()
    val verifiedSellers by viewModel.verifiedSellersMapState.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val allReportsState by viewModel.allReportsState.collectAsStateWithLifecycle()
    val pendingVerificationsState by viewModel.pendingVerificationsState.collectAsStateWithLifecycle()
    val disputedOrdersState by viewModel.disputedOrdersState.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val locationFilter by viewModel.locationFilter.collectAsStateWithLifecycle()
    val displayCurrency by viewModel.displayCurrency.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val favoriteIds = remember(favoritesState) {
        (favoritesState as? UiState.Success)?.data?.map { it.listingId }?.toSet() ?: emptySet()
    }
    val userProfile = (userProfileState as? UiState.Success)?.data
    val userDefaultCity = userProfile?.defaultCity ?: "Harare"
    val notificationsEnabled = userProfile?.notificationsEnabled ?: true

    val seenNotificationIds = remember(currentUserId) { mutableSetOf<String>() }
    var hasNotificationBaseline by remember(currentUserId) { mutableStateOf(false) }
    LaunchedEffect(notificationsState, notificationsEnabled) {
        val list = (notificationsState as? UiState.Success)?.data ?: return@LaunchedEffect
        if (!hasNotificationBaseline) {
            seenNotificationIds.addAll(list.map { it.id })
            hasNotificationBaseline = true
        } else {
            val fresh = list.filter { !it.read && seenNotificationIds.add(it.id) }
            if (notificationsEnabled) {
                fresh.forEach { notif ->
                    MukamboNotificationHelper.showLocalNotification(
                        context = context,
                        title = notif.title,
                        body = notif.body,
                        notificationId = notif.id.hashCode()
                    )
                }
            }
        }
    }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showTopLevelNav && !useNavRail) {
                    NavigationBar {
                        topTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes), maxLines = 1) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavRail) {
                    NavigationRail {
                        topTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes), maxLines = 1) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_EXPLORE
                    ) {
                        composable(ROUTE_EXPLORE) {
                            ExploreScreen(
                                listingsState = filteredListingsState,
                                favoriteIds = favoriteIds,
                                verifiedSellers = verifiedSellers,
                                favoritesState = favoritesState,
                                notificationsState = notificationsState,
                                isAdmin = isAdmin,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                locationFilter = locationFilter,
                                displayCurrency = displayCurrency,
                                isBusy = isBusy,
                                onSearchChange = viewModel::setSearchQuery,
                                onCategorySelect = viewModel::setSelectedCategory,
                                onProvinceSelect = viewModel::selectProvinceFilter,
                                onCitySelect = viewModel::selectCityFilter,
                                onRadiusSelect = viewModel::selectRadiusFilter,
                                onSortSelect = viewModel::setSortOrder,
                                onGpsReceived = viewModel::setGpsCoordinates,
                                onShowMessage = viewModel::showMessage,
                                onClearLocationFilters = viewModel::clearLocationFilters,
                                onToggleCurrency = viewModel::toggleCurrency,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onRemoveFavoriteById = viewModel::removeFavoriteByListingId,
                                onMarkNotificationRead = viewModel::markNotificationRead,
                                onOpenListing = { listingId ->
                                    navController.navigate("detail/$listingId")
                                },
                                onNavigateToSell = { navigateTopTab(ROUTE_SELL) },
                                onOpenAdmin = { navController.navigate(ROUTE_ADMIN) },
                                onSeedSamples = viewModel::seedSampleListings
                            )
                        }

                        composable(ROUTE_SELL) {
                            SellScreen(
                                myListingsState = myListingsState,
                                displayCurrency = displayCurrency,
                                isBusy = isBusy,
                                onPublishListing = viewModel::uploadAndCreateListing,
                                onDeleteListing = { id -> viewModel.deleteListing(id) },
                                onOpenListing = { id -> navController.navigate("detail/$id") },
                                onShowMessage = viewModel::showMessage
                            )
                        }

                        composable(ROUTE_MESSAGES) {
                            ChatsListScreen(
                                chatsState = chatsState,
                                currentUserId = currentUserId,
                                displayCurrency = displayCurrency,
                                onOpenChat = { chatId -> navController.navigate("chat/$chatId") }
                            )
                        }

                        composable(ROUTE_ORDERS) {
                            val myReviews = (myReviewsState as? UiState.Success)?.data ?: emptyList()
                            OrdersScreen(
                                buyerOrdersState = buyerOrdersState,
                                sellerOrdersState = sellerOrdersState,
                                myReviews = myReviews,
                                displayCurrency = displayCurrency,
                                isBusy = isBusy,
                                onUpdateTracking = viewModel::updateOrderTracking,
                                onReleaseEscrow = viewModel::releaseEscrow,
                                onDisputeOrder = viewModel::disputeOrder,
                                onSubmitReview = viewModel::submitSellerReview
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            ProfileScreen(
                                userProfileState = userProfileState,
                                sellerProfileState = mySellerProfileState,
                                favoritesState = favoritesState,
                                fallbackName = currentUserName,
                                fallbackEmail = currentUserEmail,
                                displayCurrency = displayCurrency,
                                isAdmin = isAdmin,
                                isBusy = isBusy,
                                onSaveProfile = viewModel::updateProfile,
                                onRequestVerification = viewModel::requestSellerVerification,
                                onOpenListing = { id -> navController.navigate("detail/$id") },
                                onRemoveFavorite = viewModel::removeFavoriteByListingId,
                                onOpenAdmin = { navController.navigate(ROUTE_ADMIN) },
                                onShowMessage = viewModel::showMessage
                            )
                        }

                        composable(
                            route = ROUTE_DETAIL,
                            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
                            val activeItem = (activeListingsState as? UiState.Success)?.data
                                ?.firstOrNull { it.id == listingId }
                            val myItem = (myListingsState as? UiState.Success)?.data
                                ?.firstOrNull { it.id == listingId }
                            val fetchedListing by remember(listingId) {
                                viewModel.observeListing(listingId)
                            }.collectAsStateWithLifecycle(initialValue = null)
                            val listing = activeItem ?: myItem ?: fetchedListing

                            val sellerId = listing?.sellerId
                            val sellerProfile by remember(sellerId) {
                                if (!sellerId.isNullOrBlank()) viewModel.observeSellerProfile(sellerId)
                                else flowOf(null)
                            }.collectAsStateWithLifecycle(initialValue = null)

                            val sellerReviews by remember(sellerId) {
                                if (!sellerId.isNullOrBlank()) viewModel.observeSellerReviews(sellerId)
                                else flowOf(emptyList())
                            }.collectAsStateWithLifecycle(initialValue = emptyList())

                            ListingDetailScreen(
                                listing = listing,
                                sellerProfile = sellerProfile,
                                sellerReviews = sellerReviews,
                                currentUserId = currentUserId,
                                originCoordinates = locationFilter.activeOrigin,
                                displayCurrency = displayCurrency,
                                isFavorite = favoriteIds.contains(listingId),
                                isBusy = isBusy,
                                defaultDeliveryCity = userDefaultCity,
                                onBack = { navController.popBackStack() },
                                onToggleFavorite = viewModel::toggleFavorite,
                                onDeleteListing = { id ->
                                    viewModel.deleteListing(id) {
                                        navController.popBackStack()
                                    }
                                },
                                onChatWithSeller = { item ->
                                    viewModel.startChatWithSeller(item) { chatId ->
                                        navController.navigate("chat/$chatId")
                                    }
                                },
                                onReportListing = { item, reason, details ->
                                    viewModel.submitListingReport(item, reason, details)
                                },
                                onCheckoutSubmit = { item, gateway, masked, city, method, curr, onDone ->
                                    viewModel.checkoutWithEscrow(
                                        listing = item,
                                        gateway = gateway,
                                        maskedAccount = masked,
                                        deliveryCity = city,
                                        deliveryMethod = method,
                                        currency = curr,
                                        onSuccess = onDone
                                    )
                                },
                                onNavigateToOrders = {
                                    navController.popBackStack()
                                    navigateTopTab(ROUTE_ORDERS)
                                }
                            )
                        }

                        composable(
                            route = ROUTE_CHAT_THREAD,
                            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                            val chatThread = (chatsState as? UiState.Success)?.data
                                ?.firstOrNull { it.id == chatId }
                            val messages by remember(chatId) {
                                viewModel.observeChatMessages(chatId)
                            }.collectAsStateWithLifecycle(initialValue = emptyList())

                            ChatThreadScreen(
                                chat = chatThread,
                                messages = messages,
                                currentUserId = currentUserId,
                                displayCurrency = displayCurrency,
                                onBack = { navController.popBackStack() },
                                onSendMessage = { thread, text, onSent ->
                                    viewModel.sendChatMessage(thread, text, onSent)
                                },
                                onOpenListing = { listingId ->
                                    navController.navigate("detail/$listingId")
                                }
                            )
                        }

                        composable(ROUTE_ADMIN) {
                            AdminDashboardScreen(
                                isAdmin = isAdmin,
                                reportsState = allReportsState,
                                pendingVerificationsState = pendingVerificationsState,
                                disputedOrdersState = disputedOrdersState,
                                displayCurrency = displayCurrency,
                                isBusy = isBusy,
                                onBack = { navController.popBackStack() },
                                onModerateReport = viewModel::moderateReport,
                                onDecideVerification = viewModel::decideSellerVerification,
                                onResolveDispute = viewModel::resolveDisputedOrder,
                                onOpenListing = { listingId ->
                                    navController.navigate("detail/$listingId")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
