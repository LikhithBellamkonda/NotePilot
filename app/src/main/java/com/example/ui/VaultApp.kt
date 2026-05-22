package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VaultItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultApp(viewModel: VaultViewModel) {
    val context = LocalContext.current
    val items by viewModel.filteredItems.collectAsStateWithLifecycle()
    val rawItemsList by viewModel.allItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val aiNecessityQuery by viewModel.aiNecessityQuery.collectAsStateWithLifecycle()
    val isAiSearching by viewModel.isAiSearching.collectAsStateWithLifecycle()
    val aiSearchError by viewModel.aiSearchError.collectAsStateWithLifecycle()
    val aiSearchExplanation by viewModel.aiSearchExplanation.collectAsStateWithLifecycle()
    val aiMatchedIds by viewModel.aiMatchedIds.collectAsStateWithLifecycle()
    val aiMatchedReasons by viewModel.aiMatchedReasons.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val isGoogleLoggedIn by viewModel.isGoogleLoggedIn.collectAsStateWithLifecycle()
    val googleUserEmail by viewModel.googleUserEmail.collectAsStateWithLifecycle()
    val googleUserName by viewModel.googleUserName.collectAsStateWithLifecycle()
    val googleUserPhoto by viewModel.googleUserPhoto.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val sharedIncomingText by viewModel.sharedIncomingText.collectAsStateWithLifecycle()
    var showNotificationTray by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showHistoryTimeline by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAddDeadlineDialog by remember { mutableStateOf(false) }

    // Navigation Tab state tracking
    var activeTab by remember { mutableStateOf("Feed") } // "Feed", "Search", "Alerts", "Profile"
    var selectedTopTab by remember { mutableStateOf("Activity") } // "Activity", "Deadlines", "System"

    // Automatic intent prefill detection
    LaunchedEffect(sharedIncomingText) {
        if (sharedIncomingText != null) {
            showAddDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_note_pilot_logo_1779335751799),
                            contentDescription = "NotePilot Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Column {
                            Text(
                                "NotePilot",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Smart notes, deadlines & clips pilot",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (isGoogleLoggedIn) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { activeTab = "Profile" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSyncing) Color(0xFFFFB300) else Color(0xFF25D366))
                                )
                                Text(
                                    text = googleUserPhoto ?: "G",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { showNotificationTray = !showNotificationTray },
                        modifier = Modifier.testTag("notification_bell_icon")
                    ) {
                        val activeDeadlinesCount = rawItemsList.count { it.hasDeadline && it.deadlineConfirmStatus == "CONFIRMED" }
                        if (activeDeadlinesCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text(activeDeadlinesCount.toString(), color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notification bell icon indicator",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notification bell icon indicator",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_preferences_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Preferences settings gear icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == "Feed",
                    onClick = { activeTab = "Feed" },
                    icon = { Icon(Icons.Default.List, "Feed Lists") },
                    label = { Text("Feed", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = activeTab == "Search",
                    onClick = { activeTab = "Search" },
                    icon = { Icon(Icons.Default.Search, "AI Necessity Search") },
                    label = { Text("AI Search", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = activeTab == "Alerts",
                    onClick = { activeTab = "Alerts" },
                    icon = {
                        val alertPending = rawItemsList.count { it.hasDeadline && it.deadlineConfirmStatus == "PENDING" }
                        if (alertPending > 0) {
                            BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error) { Text(alertPending.toString()) } }) {
                                Icon(Icons.Default.Notifications, "Alerts inbox")
                            }
                        } else {
                            Icon(Icons.Default.Notifications, "Alerts inbox")
                        }
                    },
                    label = { Text("Alerts", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationBarItem(
                    selected = activeTab == "Profile",
                    onClick = { activeTab = "Profile" },
                    icon = { Icon(Icons.Default.AccountCircle, "Google sync info") },
                    label = { Text("Cloud Sync", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        floatingActionButton = {
            if (activeTab == "Feed" && (selectedTopTab == "Activity" || selectedTopTab == "Deadlines")) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (selectedTopTab == "Deadlines") {
                            showAddDeadlineDialog = true
                        } else {
                            showAddDialog = true
                        }
                    },
                    icon = { Icon(Icons.Default.Add, "Import new data") },
                    text = { Text("New") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("save_content_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top tab category row in Feed page to represent Activity, Deadlines, and System configurations
                if (activeTab == "Feed") {
                    TabRow(
                        selectedTabIndex = when (selectedTopTab) {
                            "Activity" -> 0
                            "Deadlines" -> 1
                            "System" -> 2
                            else -> 0
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = { HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)) }
                    ) {
                        Tab(
                            selected = selectedTopTab == "Activity",
                            onClick = { selectedTopTab = "Activity" },
                            text = { Text("Activity Feed", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = selectedTopTab == "Deadlines",
                            onClick = { selectedTopTab = "Deadlines" },
                            text = { Text("Deadlines", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = selectedTopTab == "System",
                            onClick = { selectedTopTab = "System" },
                            text = { Text("System Sync", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }
                }

                // Page Body Rendering
                when (activeTab) {
                    "Feed" -> {
                        when (selectedTopTab) {
                            "Activity" -> {
                                // Category filters
                                CategoryFilterTabs(
                                    selectedCategory = selectedCategory,
                                    onCategorySelect = { viewModel.setCategory(it) }
                                )

                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))

                                // Saves Chronological History Timeline Accordion Section
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Column(
                                        modifier = Modifier.animateContentSize()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showHistoryTimeline = !showHistoryTimeline }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = "Chronological History icon",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Saves Chronological History Logs",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${items.size} logs",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Icon(
                                                imageVector = if (showHistoryTimeline) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Expand history indicator",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        if (showHistoryTimeline) {
                                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                            
                                            if (items.isEmpty()) {
                                                Text(
                                                    text = "No import logs available yet. Share some content to populate history logs!",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(16.dp),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            } else {
                                                val sortedHistoryItems = remember(items) {
                                                    items.sortedByDescending { it.timestamp }
                                                }
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 180.dp)
                                                        .verticalScroll(rememberScrollState())
                                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    sortedHistoryItems.forEach { item ->
                                                        val sourceColor = when (item.contentType) {
                                                            "REEL" -> Color(0xFFE1306C)
                                                            "WHATSAPP" -> Color(0xFF25D366)
                                                            "SHORT" -> Color(0xFFFF0000)
                                                            else -> MaterialTheme.colorScheme.primary
                                                        }
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    // Focus and search for this specific item
                                                                    viewModel.updateSearchQuery(item.title)
                                                                    Toast.makeText(context, "Filtering for: ${item.title}", Toast.LENGTH_SHORT).show()
                                                                }
                                                                .padding(vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // Timeline visual node representation
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                modifier = Modifier.width(20.dp)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(8.dp)
                                                                        .clip(CircleShape)
                                                                        .background(sourceColor)
                                                                )
                                                                Box(
                                                                    modifier = Modifier
                                                                        .width(1.dp)
                                                                        .height(18.dp)
                                                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                                                )
                                                            }
                                                            
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            
                                                            Text(
                                                                text = "Saved ${item.contentType}: \"${item.title}\"",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            
                                                            Text(
                                                                text = getRelativeTimeSpan(item.timestamp),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Fuzzy context search
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.updateSearchQuery(it) },
                                    placeholder = { Text("Fuzzy filter items and custom tags...", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.List, "Filter icon", modifier = Modifier.size(16.dp)) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                                Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .height(48.dp)
                                        .testTag("fuzzy_search_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                    ),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
                                )

                                if (items.isEmpty()) {
                                    EmptyFeedPlaceholder { showAddDialog = true }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(items, key = { it.id }) { item ->
                                            VaultItemCard(
                                                item = item,
                                                aiReason = aiMatchedReasons[item.id],
                                                onConfirmDeadline = { viewModel.confirmDeadlineHighlight(item) },
                                                onDeclineDeadline = { viewModel.declineDeadlineHighlight(item) },
                                                onToggleHighlight = { viewModel.toggleManualHighlight(item) },
                                                onDelete = { viewModel.deleteItem(item.id) }
                                            )
                                        }
                                    }
                                }
                            }
                            "Deadlines" -> {
                                val activeDeadlines = rawItemsList.filter { it.hasDeadline && it.deadlineConfirmStatus != "DECLINED" }
                                if (activeDeadlines.isEmpty()) {
                                    EmptyDeadlinesPlaceholder()
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(activeDeadlines, key = { "dl_tab_${it.id}" }) { item ->
                                            VaultItemCard(
                                                item = item,
                                                aiReason = null,
                                                onConfirmDeadline = { viewModel.confirmDeadlineHighlight(item) },
                                                onDeclineDeadline = { viewModel.declineDeadlineHighlight(item) },
                                                onToggleHighlight = { viewModel.toggleManualHighlight(item) },
                                                onDelete = { viewModel.deleteItem(item.id) }
                                            )
                                        }
                                    }
                                }
                            }
                            "System" -> {
                                SystemSyncScreen(viewModel = viewModel)
                            }
                        }
                    }
                    "Search" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp)
                        ) {
                            AiNecessitySearchCard(
                                query = aiNecessityQuery,
                                onQueryChange = { viewModel.updateAiNecessityQuery(it) },
                                onSearchClick = { viewModel.performAiNecessitySearch() },
                                onResetClick = { viewModel.resetAiSearch() },
                                isSearching = isAiSearching,
                                errorMessage = aiSearchError,
                                explanation = aiSearchExplanation,
                                isFiltered = aiMatchedIds != null,
                                isApiKeyOk = viewModel.isApiKeyConfigured()
                            )

                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                            Text(
                                "Semantic Search Filter Outcomes:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                            )

                            if (aiMatchedIds == null) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No semantic criteria active. Explain what you need above!",
                                        fontSize = 11.sp,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            } else if (items.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No items match your necessity criteria details in your feed.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(items, key = { "search_${it.id}" }) { item ->
                                        VaultItemCard(
                                            item = item,
                                            aiReason = aiMatchedReasons[item.id],
                                            onConfirmDeadline = { viewModel.confirmDeadlineHighlight(item) },
                                            onDeclineDeadline = { viewModel.declineDeadlineHighlight(item) },
                                            onToggleHighlight = { viewModel.toggleManualHighlight(item) },
                                            onDelete = { viewModel.deleteItem(item.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "Alerts" -> {
                        ApproachingDeadlinesInbox(
                            rawItemsList = rawItemsList,
                            viewModel = viewModel,
                            context = context,
                            onShowSettings = { showSettingsDialog = true }
                        )
                    }
                    "Profile" -> {
                        SystemSyncScreen(viewModel = viewModel)
                    }
                }
            }

            // Sliding Notification Tray Overlay
            AnimatedVisibility(
                visible = showNotificationTray,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(99f)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .stitchedBorder(MaterialTheme.colorScheme.tertiary),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                    ),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, "Alerts", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Active Confirmed Alarm Deadlines", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { showNotificationTray = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, "Dismiss close", modifier = Modifier.size(16.dp))
                            }
                        }

                        val verifiedDeadlines = rawItemsList.filter { it.hasDeadline && it.deadlineConfirmStatus == "CONFIRMED" }
                        if (verifiedDeadlines.isEmpty()) {
                            Text(
                                "No upcoming confirmed deadlines shown. Go to the Alerts page to accept new action triggers! ⏰",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            verifiedDeadlines.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("⏰ Alarm: ${item.deadlineText}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        val sharedText = sharedIncomingText
        AddContentDialog(
            initialContent = sharedText ?: "",
            originalViewModel = viewModel,
            onDismiss = {
                viewModel.clearSharedIncomingText()
                showAddDialog = false
            },
            onConfirm = { type, title, url, notes, tags ->
                viewModel.clearSharedIncomingText()
                viewModel.addNewVaultItem(type, title, url, notes, tags)
                showAddDialog = false
                Toast.makeText(context, "Saved successfully! Checking for action items...", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Preferences Settings Dialog
    if (showSettingsDialog) {
        NotePilotSettingsDialog(
            onDismiss = { showSettingsDialog = false },
            viewModel = viewModel
        )
    }

    // Add Deadline Dialog
    if (showAddDeadlineDialog) {
        AddDeadlineDialog(
            viewModel = viewModel,
            onDismiss = { showAddDeadlineDialog = false },
            onConfirm = { title, deadlineText, contentOrUrl, notes, blockOnCalendar ->
                viewModel.saveDeadlineItem(title, contentOrUrl, notes, "", deadlineText)
                showAddDeadlineDialog = false
                Toast.makeText(context, "Deadline saved and loaded!", Toast.LENGTH_SHORT).show()
                if (blockOnCalendar) {
                    try {
                        val startTimeMs = System.currentTimeMillis() + 3600000 // 1 hour later
                        val endTimeMs = startTimeMs + 3600000
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            data = Uri.parse("content://com.android.calendar/events")
                            putExtra("title", "NotePilot: $title")
                            putExtra("description", "Deadline Alert: $deadlineText\nSource: $contentOrUrl\nNotes: $notes")
                            putExtra("beginTime", startTimeMs)
                            putExtra("endTime", endTimeMs)
                            putExtra("allDay", false)
                            putExtra("hasAlarm", 1)
                            putExtra("availability", 0) // Busy Slot
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        Toast.makeText(context, "Opening Google Calendar to block slot...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Google Calendar app is not integrated or available.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

/**
 * Google multi-device cloud synchronization panel
 */
@Composable
fun SystemSyncScreen(viewModel: VaultViewModel) {
    val isGoogleLoggedIn by viewModel.isGoogleLoggedIn.collectAsStateWithLifecycle()
    val googleUserEmail by viewModel.googleUserEmail.collectAsStateWithLifecycle()
    val googleUserName by viewModel.googleUserName.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Cloud logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            "Google Account Multi-Device Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Sync database across devices beautifully",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                Text(
                    text = syncStatus,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isGoogleLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isSyncing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isGoogleLoggedIn) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = googleUserName?.take(1)?.uppercase() ?: "G",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text(
                                text = googleUserName ?: "Likhith Bellamkonda",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = googleUserEmail ?: "likhithbellamkonda@gmail.com",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.logout() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ExitToApp, "Sign Out", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sign Out", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.triggerManualBackup() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Share, "Back Up", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back Up Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "By signing in with your Google Account, OmniFeed secures your bookmarks and automatically mirrors your saved Reels, Shorts, and clips so you can pick up exactly where you left off on any mobile or tablet.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp),
                            lineHeight = 15.sp
                        )

                        Button(
                            onClick = {
                                viewModel.loginWithGoogle("likhithbellamkonda@gmail.com", "Likhith Bellamkonda")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("google_signin_button")
                        ) {
                            Icon(Icons.Default.AccountCircle, "Google login")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In with Google Account", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Gemini AI status configuration box
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Build, "Gemini", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text("Gemini AI Core Status", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Text(
                    text = if (viewModel.isApiKeyConfigured()) "🟢 Gemini Pro API Service Active and Connected." 
                           else "🟡 Local Fallback Mode (No active API Key found).",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.isApiKeyConfigured()) Color(0xFF25D366) else Color(0xFFFFB300)
                )

                Text(
                    text = "OmniFeed uses Google's Gemini models to run server-side semantic queries, auto-generate more than 10 contextual hashtags, and highlight deadlines from messy WhatsApp texts or YouTube Short descriptions.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

/**
 * Empty placeholders
 */
@Composable
fun EmptyFeedPlaceholder(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.List, "Empty list icon", modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
            Text("Your Activity Feed is Empty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Click Save Shared Content or share link fragments directly from Whatsapp, Instagram Reels or YouTube Shorts!",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = onAddClick, shape = RoundedCornerShape(10.dp)) {
                Text("Add First Item", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyDeadlinesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Notifications, "Empty deadlines icon", modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
            Text("No Approaching Deadlines Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "When Gemini detects any deadline-related descriptions in saved WhatsApp texts, Instagram titles, or Gmail reminders, they will appear highlighted here.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

/**
 * Dedicated alerts feed presenting deadline confirmation states matching card template 1 from second image
 */
@Composable
fun ApproachingDeadlinesInbox(
    rawItemsList: List<com.example.data.VaultItem>,
    viewModel: VaultViewModel,
    context: Context,
    onShowSettings: () -> Unit
) {
    val pendingDeadlines = rawItemsList.filter { it.hasDeadline && it.deadlineConfirmStatus == "PENDING" }
    val acceptedDeadlines = rawItemsList.filter { it.hasDeadline && it.deadlineConfirmStatus == "CONFIRMED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Notifications, "Alerts header", tint = MaterialTheme.colorScheme.error)
                Text("AI Approaching Deadlines", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            
            IconButton(
                onClick = onShowSettings,
                modifier = Modifier.size(28.dp).testTag("deadlines_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Deadlines Settings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            "Gemini monitors your clips and text dumps for dates or deadlines. Accept pending triggers below to pin them as alarm banners in your Activity feed.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 15.sp
        )

        // Standard Alerts List (Pending approval)
        Text("Pending Confirmation Alerts", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

        if (pendingDeadlines.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Text(
                    "No pending deadline confirmations. All clear! 🎉",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            pendingDeadlines.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Email, "Chat msg", tint = Color(0xFF25D366), modifier = Modifier.size(14.dp))
                            }
                            Text("WhatsApp • Potential Deadline Alert", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFE11D48))
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Warning, "Deadline critical warning", tint = Color(0xFFE11D48), modifier = Modifier.size(14.dp))
                        }

                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Is this an approaching deadline? \"${item.deadlineText}\"",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.declineDeadlineHighlight(item) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Dismiss", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { viewModel.confirmDeadlineHighlight(item) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C49ED)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Confirm as Deadline", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Active highlighted Pin board
        Text("Active Confirmed Highlights (" + acceptedDeadlines.size + ")", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        if (acceptedDeadlines.isNotEmpty()) {
            acceptedDeadlines.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Notifications, "Alarm status Active", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("⏰ Active Alarm: ${item.deadlineText}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

/**
 * Beautiful Glassmorphic Wave Drawing showing bezier flows representing design highlights
 */
@Composable
fun GlassmorphicWaveCanvas() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        val width = size.width
        val height = size.height

        // Simple vector representation of beautiful overlapping bezier curve waveforms
        val path1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height * 0.6f)
            cubicTo(
                width * 0.3f, height * 0.1f,
                width * 0.7f, height * 0.9f,
                width, height * 0.4f
            )
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = path1,
            brush = Brush.linearGradient(
                colors = listOf(Color(0x774C49ED), Color(0x77818CF8))
            )
        )

        val path2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height * 0.8f)
            cubicTo(
                width * 0.4f, height * 0.9f,
                width * 0.6f, height * 0.2f,
                width, height * 0.5f
            )
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = path2,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xBB818CF8), Color(0xBB4C49ED))
            )
        )
    }
}

/**
 * AI Necessity Search input and Response feedback block
 */
@Composable
fun AiNecessitySearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onResetClick: () -> Unit,
    isSearching: Boolean,
    errorMessage: String?,
    explanation: String?,
    isFiltered: Boolean,
    isApiKeyOk: Boolean
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isFiltered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "AI search logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "AI Smart Necessity Search",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isFiltered) {
                    SuggestionChip(
                        onClick = onResetClick,
                        label = { Text("Clear Filter", fontSize = 11.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            Text(
                text = "Explain your current necessity, and Gemini will analyze your collection matching the perfect save clip and detailing why.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("e.g. \"show me my coding tips\" or \"is anything due soon?\"", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("ai_search_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (isApiKeyOk && query.isNotBlank()) {
                            keyboardController?.hide()
                            onSearchClick()
                        }
                    }),
                    enabled = isApiKeyOk && !isSearching
                )

                Button(
                    onClick = {
                        keyboardController?.hide()
                        onSearchClick()
                    },
                    enabled = isApiKeyOk && query.isNotBlank() && !isSearching,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("ai_search_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Search necessity trigger"
                    )
                }
            }

            // Loading Progress
            if (isSearching) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Gemini is parsing items for match coordinates...",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Error Message If Present
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Gemini Reason Output Display
            if (explanation != null) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "AI feedback advice icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Category filters: ALL, REEL, WHATSAPP, SHORT
 */
@Composable
fun CategoryFilterTabs(
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    val categories = listOf(
        "ALL" to "All Vault",
        "REEL" to "Reels 📸",
        "WHATSAPP" to "Msgs 💬",
        "SHORT" to "Shorts 🎥"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { (code, label) ->
            val isSelected = selectedCategory == code
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelect(code) },
                label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

/**
 * Beautiful Individual Content Card with deadline alerts and actions
 */
@Composable
fun VaultItemCard(
    item: VaultItem,
    aiReason: String?,
    onConfirmDeadline: () -> Unit,
    onDeclineDeadline: () -> Unit,
    onToggleHighlight: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    // Styling configurations based on Type
    val (typeIcon, typeLabel, categoryColor, cardBg) = when (item.contentType) {
        "REEL" -> Quadruple(
            Icons.Default.Share,
            "Instagram Reel",
            Color(0xFFE1306C),
            MaterialTheme.colorScheme.surface
        )
        "WHATSAPP" -> Quadruple(
            Icons.Default.Info,
            "WhatsApp Msg",
            Color(0xFF25D366),
            MaterialTheme.colorScheme.surface
        )
        "SHORT" -> Quadruple(
            Icons.Default.PlayArrow,
            "YouTube Short",
            Color(0xFFFF0000),
            MaterialTheme.colorScheme.surface
        )
        else -> Quadruple(
            Icons.Default.Share,
            "External Content",
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.surface
        )
    }

    val formattedDate = remember(item.timestamp) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    // Determine custom border if highlighted
    val cardBorder = if (item.isHighlighted) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.error)
    } else if (aiReason != null) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isHighlighted) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f) else cardBg
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = "Content type icon",
                        tint = categoryColor,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = typeLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = categoryColor
                    )
                    Text(
                        text = getRelativeTimeSpan(item.timestamp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Share Item Button
                IconButton(
                    onClick = {
                        try {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                val textBody = buildString {
                                    appendLine("📢 Shared from NotePilot: ${item.title}")
                                    if (item.contentOrUrl.isNotBlank()) {
                                        appendLine("🔗 Link/Message: ${item.contentOrUrl}")
                                    }
                                    if (item.notes.isNotBlank()) {
                                        appendLine("📝 Context Notes: ${item.notes}")
                                    }
                                    if (item.tags.isNotBlank()) {
                                        appendLine("🏷️ Tags: ${item.tags.split(",").joinToString(" ") { "#${it.trim()}" }}")
                                    }
                                }
                                putExtra(Intent.EXTRA_TEXT, textBody)
                                type = "text/plain"
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Info via:").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Sharing is not available right now.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share item content",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Highlight Star Button
                IconButton(
                    onClick = onToggleHighlight,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Highlight bookmark action",
                        tint = if (item.isHighlighted) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Title
            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Dynamic presentation of main body/link
            val isLink = item.contentOrUrl.startsWith("http://", ignoreCase = true) ||
                         item.contentOrUrl.startsWith("https://", ignoreCase = true)

            if (isLink) {
                // Clickable link row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.contentOrUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Invalid link or no browser found", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Open link symbol",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = item.contentOrUrl,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Speech bubble text format for messaging
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.contentType == "WHATSAPP") Color(0xFFE8F8EE) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                    ),
                    shape = RoundedCornerShape(10.dp, 10.dp, 10.dp, 2.dp)
                ) {
                    Text(
                        text = item.contentOrUrl,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp),
                        lineHeight = 18.sp
                    )
                }
            }

            // Personal Notes (If provided)
            if (item.notes.isNotBlank()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "Notes & Context:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.notes,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tags row (If provided)
            if (item.tags.isNotBlank()) {
                val tagsList = item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (tagsList.isNotEmpty()) {
                    FlowRowWithSpacing(spacing = 6.dp) {
                        tagsList.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Semantic Relevance Explanation Box (When AI Search is active)
            if (aiReason != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Search match reason icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = aiReason,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // AI Confirmation Flow Overlay inside Card
            if (item.hasDeadline && item.deadlineConfirmStatus == "PENDING" && item.deadlineText != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Detected event detail info",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "🤖 Gemini Detected an Actionable/Deadline:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Text(
                            text = "\"${item.deadlineText}\"",
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(start = 24.dp)
                        )

                        Text(
                            text = "Would you like to confirm and highlight this in your feed alerts?",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            OutlinedButton(
                                onClick = onDeclineDeadline,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.3f)),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("No, Ignore", fontSize = 11.sp)
                            }

                            Button(
                                onClick = onConfirmDeadline,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Yes, Highlight", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // If confirmed, view persistent orange alarm indicator strip
            if (item.isHighlighted && item.deadlineText != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Deadline highlighted alert icon",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "⚠️ DEADLINE: ${item.deadlineText}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        TextButton(
                            onClick = {
                                try {
                                    val startTimeMs = System.currentTimeMillis() + 3600000 // 1 hour later
                                    val endTimeMs = startTimeMs + 3600000
                                    val intent = Intent(Intent.ACTION_INSERT).apply {
                                        data = Uri.parse("content://com.android.calendar/events")
                                        putExtra("title", "NotePilot: ${item.title}")
                                        putExtra("description", "Deadline Alert: ${item.deadlineText}\nSource url/message: ${item.contentOrUrl}\nContext notes: ${item.notes}")
                                        putExtra("beginTime", startTimeMs)
                                        putExtra("endTime", endTimeMs)
                                        putExtra("allDay", false)
                                        putExtra("hasAlarm", 1)
                                        putExtra("availability", 0) // Busy Slot
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                    Toast.makeText(context, "Opening Google Calendar to block slot...", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Google Calendar app is not integrated or available.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Calendar Sync Icon",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Block Time",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Floating FlowRow layout to draw hashtag badges with wrap support
 */
@Composable
fun FlowRowWithSpacing(
    spacing: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content = content) { measurables, constraints ->
        var xCoord = 0
        var yCoord = 0
        var rowHeight = 0
        val spacePx = spacing.roundToPx()

        val placeables = measurables.map { measurable ->
            val placeable = measurable.measure(constraints)
            if (xCoord + placeable.width > constraints.maxWidth) {
                xCoord = 0
                yCoord += rowHeight + spacePx
                rowHeight = 0
            }
            rowHeight = maxOf(rowHeight, placeable.height)
            xCoord += placeable.width + spacePx
            placeable
        }

        layout(constraints.maxWidth, yCoord + rowHeight) {
            var currentX = 0
            var currentY = 0
            var currentRowHeight = 0
            placeables.forEach { placeable ->
                if (currentX + placeable.width > constraints.maxWidth) {
                    currentX = 0
                    currentY += currentRowHeight + spacePx
                    currentRowHeight = 0
                }
                placeable.placeRelative(currentX, currentY)
                currentRowHeight = maxOf(currentRowHeight, placeable.height)
                currentX += placeable.width + spacePx
            }
        }
    }
}

/**
 * Add Content Dialog Flow
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContentDialog(
    initialContent: String = "",
    originalViewModel: VaultViewModel,
    onDismiss: () -> Unit,
    onConfirm: (type: String, title: String, content: String, notes: String, tags: String) -> Unit
) {
    var selectedType by remember { mutableStateOf("REEL") } // "REEL", "WHATSAPP", "SHORT"
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    var isTitleError by remember { mutableStateOf(false) }
    var isContentError by remember { mutableStateOf(false) }

    // Prefill share handlers
    LaunchedEffect(initialContent) {
        if (initialContent.isNotEmpty()) {
            val linkIndex = initialContent.indexOf("http")
            if (linkIndex != -1) {
                content = initialContent.substring(linkIndex).trim()
                val prefix = initialContent.substring(0, linkIndex).trim()
                if (prefix.isNotEmpty()) {
                    title = prefix.removeSuffix(":").removeSuffix("-").trim()
                } else {
                    title = "Imported Shared Content"
                }
            } else {
                content = initialContent
                title = if (initialContent.length > 40) initialContent.take(37) + "..." else initialContent
            }

            // Intuitive auto categorization
            if (content.contains("instagram.com") || content.contains("/reels/") || content.contains("/reel/")) {
                selectedType = "REEL"
            } else if (content.contains("youtube.com") || content.contains("youtu.be") || content.contains("/shorts/")) {
                selectedType = "SHORT"
            } else {
                selectedType = "WHATSAPP"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "New Note / Clip",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Content Type Grid Picker
                Text(
                    text = "Select Content Source Category",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        Triple("REEL", "Reel 📸", Color(0xFFE1306C)),
                        Triple("WHATSAPP", "Chat 💬", Color(0xFF25D366)),
                        Triple("SHORT", "Short 🎥", Color(0xFFFF0000))
                    )

                    types.forEach { (code, label, color) ->
                        val isPicked = selectedType == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isPicked) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .clickable { selectedType = code }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                color = if (isPicked) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) isTitleError = false
                    },
                    label = { Text("Title / Summary") },
                    placeholder = { Text("e.g. Kotlin flow cheat sheet or Hackathon registration message") },
                    isError = isTitleError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_item_title"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Link / Content body Input
                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        if (it.isNotBlank()) isContentError = false
                    },
                    label = { Text(if (selectedType == "WHATSAPP") "Message Content body" else "Content URL link") },
                    placeholder = { Text(if (selectedType == "WHATSAPP") "Paste the text message details" else "Paste the https:// link here") },
                    isError = isContentError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_item_content")
                        .height(110.dp),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 4
                )

                // Personal Private Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("My Context & Notes (Optional)") },
                    placeholder = { Text("Add any personal takeaways or tags") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_item_notes"),
                    shape = RoundedCornerShape(10.dp)
                )

                // 10 Suggested Tags Generator Row
                val scope = rememberCoroutineScope()
                var isTagGenLoading by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isTagGenLoading = true
                                val suggested = originalViewModel.suggestTagsForContent(
                                    title.ifBlank { "Smart Item" },
                                    content.ifBlank { "General content details" },
                                    notes
                                )
                                if (suggested.isNotEmpty()) {
                                    tags = suggested.joinToString(", ")
                                }
                                isTagGenLoading = false
                            }
                        },
                        enabled = !isTagGenLoading && title.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("generate_tags_button")
                    ) {
                        if (isTagGenLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating 10+ Tags...", fontSize = 11.sp, color = Color.White)
                        } else {
                            Icon(Icons.Default.Star, "Auto tags spark", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Suggest 10+ Tags (Gemini AI)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    
                    Text(
                        text = "Generates a clean package of minimum 10 contextual tags to categorize saved items automatically.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Tags CSV Input
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Comma-separated tags (Suggested or Custom)") },
                    placeholder = { Text("e.g. coding, study, science, funny, life hacks") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_item_tags"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Action buttons spacing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                isTitleError = true
                            }
                            if (content.isBlank()) {
                                isContentError = true
                            }
                            if (title.isNotBlank() && content.isNotBlank()) {
                                onConfirm(selectedType, title, content, notes, tags)
                            }
                        },
                        modifier = Modifier.weight(1.5f).testTag("dialog_confirm_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Verify & Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Inline standard container wrapper to simplify passing 4 parameters without generic array allocations
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

fun Modifier.stitchedBorder(
    color: Color,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp
): Modifier = this.drawBehind {
    val stroke = Stroke(
        width = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )
    val sizePx = size
    val radiusPx = cornerRadius.toPx()
    
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(1.dp.toPx(), 1.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(sizePx.width - 2.dp.toPx(), sizePx.height - 2.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx, radiusPx),
        style = stroke
    )
}

fun getDeadlineTimeStatus(text: String?): String {
    if (text.isNullOrEmpty()) return "Flexible scheduled"
    val hash = kotlin.math.abs(text.hashCode())
    val daysLeft = (hash % 6) + 1
    val hoursLeft = (hash % 23) + 1
    
    return when {
        daysLeft <= 1 -> "⚠️ Urgent: $hoursLeft hrs remaining Today!"
        daysLeft == 2 -> "⏳ Approaching: 2 days left (Critical)"
        else -> "📅 Upcoming: $daysLeft days left"
    }
}

fun getRelativeTimeSpan(timeMs: Long): String {
    val diffMs = System.currentTimeMillis() - timeMs
    if (diffMs < 0) return "Just now"
    val diffSecs = diffMs / 1000
    if (diffSecs < 60) return "Just now"
    val diffMins = diffSecs / 60
    if (diffMins < 60) return "${diffMins}m ago"
    val diffHours = diffMins / 60
    if (diffHours < 24) return "${diffHours}h ago"
    val diffDays = diffHours / 24
    if (diffDays == 1L) return "Yesterday"
    if (diffDays < 7) return "${diffDays}d ago"
    val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
    return sdf.format(Date(timeMs))
}

@Composable
fun NotePilotSettingsDialog(
    onDismiss: () -> Unit,
    viewModel: VaultViewModel
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsStateWithLifecycle()
    val priorTimeIndex by viewModel.priorTimeIndex.collectAsStateWithLifecycle()
    val isDndModeEnabled by viewModel.isDndModeEnabled.collectAsStateWithLifecycle()
    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()

    var localApiKey by remember { mutableStateOf(geminiApiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

    val priorTimes = listOf("15m Alert", "1h Alert", "1d Alert", "3d Alert")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "NotePilot Preferences",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close description")
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Theme selection
                Text(
                    text = "Aesthetic Style Mode",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isDarkTheme) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewModel.setDarkTheme(false) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Light Theme",
                                tint = if (!isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Light Mode UI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDarkTheme) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewModel.setDarkTheme(true) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Dark Theme",
                                tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Dark Mode UI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Notifications Toggle
                Text(
                    text = "Deadline Alerts & Notifications",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Notifications",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Receive notifications when deadlines approach",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isNotificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                }

                // Prior Alerts Time Dropdown / Selection
                if (isNotificationsEnabled) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Deadline Prior Warning Time",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Select timing to dispatch notification warning banner",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            priorTimes.forEachIndexed { index, title ->
                                val isSelected = priorTimeIndex == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                        .clickable { viewModel.setPriorTimeIndex(index) }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // DND Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Do Not Disturb (DND)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Mute alert warnings during bedtime hours (10PM - 7AM)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDndModeEnabled,
                        onCheckedChange = { viewModel.setDndModeEnabled(it) }
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Gemini AI API Key input section
                Text(
                    text = "Gemini AI API Configuration",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = localApiKey,
                    onValueChange = { localApiKey = it },
                    label = { Text("Custom Gemini API Key") },
                    placeholder = { Text("Paste AI Studio API Key (AI-...)") },
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isKeyVisible) "Hide Key" else "Show Key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("settings_gemini_api_key"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Text(
                    text = if (localApiKey.isNotBlank()) "🟢 Custom API Key will be saved and used."
                           else if (viewModel.isApiKeyConfigured()) "🟡 Using System Default Key (BuildConfig)."
                           else "🔴 AI features disabled (No Key configured).",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (localApiKey.isNotBlank()) Color(0xFF25D366)
                            else if (viewModel.isApiKeyConfigured()) Color(0xFFFFB300)
                            else MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(10.dp))
                
                Button(
                    onClick = {
                        viewModel.setGeminiApiKey(localApiKey)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Changes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeadlineDialog(
    viewModel: VaultViewModel,
    onDismiss: () -> Unit,
    onConfirm: (title: String, deadlineText: String, contentOrUrl: String, notes: String, blockOnCalendar: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var deadlineText by remember { mutableStateOf("") }
    var contentOrUrl by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var blockOnCalendar by remember { mutableStateOf(true) }

    var isTitleError by remember { mutableStateOf(false) }
    var isDeadlineError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Deadline Alert Logo",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "New Tracker Deadline",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) isTitleError = false
                    },
                    label = { Text("Task / Deadline Title") },
                    placeholder = { Text("e.g. submit research paper proposal") },
                    isError = isTitleError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_deadline_title"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Date Time input
                OutlinedTextField(
                    value = deadlineText,
                    onValueChange = {
                        deadlineText = it
                        if (it.isNotBlank()) isDeadlineError = false
                    },
                    label = { Text("Deadline Date & TIme") },
                    placeholder = { Text("e.g. May 26, 2026 at 5:00 PM") },
                    isError = isDeadlineError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_deadline_text"),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                            val calendar = Calendar.getInstance()
                            calendar.add(Calendar.DAY_OF_YEAR, 1) // Tomorrow
                            calendar.set(Calendar.HOUR_OF_DAY, 17) // 5 PM
                            calendar.set(Calendar.MINUTE, 0)
                            deadlineText = sdf.format(calendar.time)
                        }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Prefill Tomorrow 5PM",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                Text(
                    text = "Tip: Tap deadline calendar symbol to auto-prefill tomorrow 5:00 PM",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp)
                )

                // Source Link/Text Input (Optional)
                OutlinedTextField(
                    value = contentOrUrl,
                    onValueChange = { contentOrUrl = it },
                    label = { Text("Context Link or Text Message (Optional)") },
                    placeholder = { Text("e.g. https://github.com/project-repo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_deadline_content"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Personal Notes Input (Optional)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("My Context & Notes (Optional)") },
                    placeholder = { Text("Personal instructions, criteria or details") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_deadline_notes"),
                    shape = RoundedCornerShape(10.dp)
                )

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // calendar check
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto Link & Block Calendar Slot",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Add to Google Calendar to lock slot & fire native system notifications",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = blockOnCalendar,
                        onCheckedChange = { blockOnCalendar = it }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                isTitleError = true
                            }
                            if (deadlineText.isBlank()) {
                                isDeadlineError = true
                            }
                            if (title.isNotBlank() && deadlineText.isNotBlank()) {
                                onConfirm(title, deadlineText, contentOrUrl, notes, blockOnCalendar)
                            }
                        },
                        modifier = Modifier.weight(1.5f).testTag("deadline_save_confirm_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Block & Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
