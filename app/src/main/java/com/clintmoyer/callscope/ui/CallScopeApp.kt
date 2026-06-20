@file:OptIn(ExperimentalMaterial3Api::class)

package com.clintmoyer.callscope.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CallMade
import androidx.compose.material.icons.outlined.CallMissed
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clintmoyer.callscope.data.AndroidCallLogProvider
import com.clintmoyer.callscope.data.CallAnalyticsRepository
import com.clintmoyer.callscope.data.SampleCallLogProvider
import com.clintmoyer.callscope.domain.AnalyticsSummary
import com.clintmoyer.callscope.domain.CallDirection
import com.clintmoyer.callscope.domain.CallRecord
import com.clintmoyer.callscope.domain.ContactRank
import com.clintmoyer.callscope.domain.ContactStats
import com.clintmoyer.callscope.domain.DailyUsage
import com.clintmoyer.callscope.domain.DateRange
import com.clintmoyer.callscope.ui.theme.CallScopeTheme
import com.clintmoyer.callscope.ui.theme.ThemeMode
import com.clintmoyer.callscope.ui.theme.good
import com.clintmoyer.callscope.ui.theme.warning
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

@Composable
fun CallScopeRoot() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val repository = remember {
        CallAnalyticsRepository(
            productionProvider = AndroidCallLogProvider(appContext),
            sampleProvider = SampleCallLogProvider(),
        )
    }
    val viewModel: CallScopeViewModel = viewModel(factory = CallScopeViewModelFactory(repository))
    val state by viewModel.state.collectAsState()
    val canReadCallLog = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        viewModel.refresh(results[Manifest.permission.READ_CALL_LOG] == true)
    }
    val requestPermissions = {
        permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS))
    }

    LaunchedEffect(canReadCallLog, state.dateRange) {
        viewModel.refresh(canReadCallLog)
    }

    CallScopeTheme(mode = state.themeMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            CallScopeApp(
                state = state,
                rankedContacts = viewModel.rankedContacts(),
                onTabSelected = viewModel::selectTab,
                onContactSelected = viewModel::selectContact,
                onBackFromContact = viewModel::clearContact,
                onRankSelected = viewModel::setRank,
                onRangeSelected = { viewModel.setDateRange(it, canReadCallLog) },
                onThemeSelected = viewModel::setTheme,
                onRequestPermissions = requestPermissions,
                onRefresh = { viewModel.refresh(canReadCallLog) },
            )
        }
    }
}

@Composable
private fun CallScopeApp(
    state: CallScopeUiState,
    rankedContacts: List<ContactStats>,
    onTabSelected: (AppTab) -> Unit,
    onContactSelected: (String) -> Unit,
    onBackFromContact: () -> Unit,
    onRankSelected: (ContactRank) -> Unit,
    onRangeSelected: (DateRange) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
) {
    val selectedContact = state.selectedContactId?.let { id -> state.summary.contacts.firstOrNull { it.contactId == id } }
    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 0.dp,
                modifier = Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            ) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.selectedTab == tab && selectedContact == null,
                        onClick = { onTabSelected(tab) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (selectedContact != null) {
            ContactDetailScreen(
                contact = selectedContact,
                onBack = onBackFromContact,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            when (state.selectedTab) {
                AppTab.Review -> ReviewScreen(
                    state = state,
                    onRangeSelected = onRangeSelected,
                    onContactSelected = onContactSelected,
                    onRequestPermissions = onRequestPermissions,
                    modifier = Modifier.padding(innerPadding),
                )
                AppTab.People -> PeopleScreen(
                    state = state,
                    contacts = rankedContacts,
                    onRankSelected = onRankSelected,
                    onContactSelected = onContactSelected,
                    modifier = Modifier.padding(innerPadding),
                )
                AppTab.Insights -> InsightsScreen(
                    state = state,
                    onRangeSelected = onRangeSelected,
                    onContactSelected = onContactSelected,
                    modifier = Modifier.padding(innerPadding),
                )
                AppTab.Settings -> SettingsScreen(
                    state = state,
                    onThemeSelected = onThemeSelected,
                    onRequestPermissions = onRequestPermissions,
                    onRefresh = onRefresh,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun ReviewScreen(
    state: CallScopeUiState,
    onRangeSelected: (DateRange) -> Unit,
    onContactSelected: (String) -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenList(modifier) {
        item {
            Header(
                title = "Review",
                subtitle = when {
                    !state.usingSampleData -> "Follow-ups and recent changes from this phone"
                    state.canReadCallLog -> "Sample review because this device has no readable call log"
                    else -> "Sample review until call-log access is granted"
                },
            )
        }
        item {
            TrustBanner(state = state, onRequestPermissions = onRequestPermissions)
        }
        item {
            RangeChips(selected = state.dateRange, onSelected = onRangeSelected)
        }
        item {
            SectionTitle("Action queue")
        }
        item {
            AttentionQueue(summary = state.summary, onContactSelected = onContactSelected)
        }
        item {
            SectionTitle("Activity pulse")
        }
        item {
            RecentActivitySummary(state.summary)
        }
        item {
            SectionTitle("Relationship changes")
        }
        item {
            RelationshipChangeCards(state.summary, onContactSelected)
        }
    }
}

@Composable
private fun PeopleScreen(
    state: CallScopeUiState,
    contacts: List<ContactStats>,
    onRankSelected: (ContactRank) -> Unit,
    onContactSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenList(modifier) {
        item {
            Header(title = "People", subtitle = "Ranked by time, frequency, misses, and recency")
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ContactRank.entries) { rank ->
                    FilterChip(
                        selected = state.contactRank == rank,
                        onClick = { onRankSelected(rank) },
                        label = { Text(rank.label) },
                    )
                }
            }
        }
        items(contacts, key = { it.contactId }) { contact ->
            ContactRow(contact = contact, onClick = { onContactSelected(contact.contactId) })
        }
    }
}

@Composable
private fun InsightsScreen(
    state: CallScopeUiState,
    onRangeSelected: (DateRange) -> Unit,
    onContactSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    ScreenList(modifier) {
        item {
            Header(title = "Insights", subtitle = "Patterns across your personal call history")
        }
        item {
            RangeChips(selected = state.dateRange, onSelected = onRangeSelected)
        }
        item {
            InsightAnswerGrid(summary, onContactSelected)
        }
        item {
            SectionTitle("Call time trend")
        }
        item {
            UsageChart(summary.dailyUsage)
        }
        item {
            SectionTitle("Incoming vs outgoing")
        }
        item {
            DirectionBalance(summary)
        }
        item {
            SectionTitle("Longest calls")
        }
        items(summary.longestCalls.take(5), key = { "long-${it.id}" }) { call ->
            CallRow(call = call, onClick = { onContactSelected(call.contactId) })
        }
        item {
            SectionTitle("Busiest days and times")
        }
        item {
            BusyGrid(summary)
        }
        item {
            SectionTitle("Relationship signals")
        }
        item {
            TrendCards(summary, onContactSelected)
        }
    }
}

@Composable
private fun SettingsScreen(
    state: CallScopeUiState,
    onThemeSelected: (ThemeMode) -> Unit,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenList(modifier) {
        item {
            Header(title = "Settings", subtitle = "Privacy, permissions, export, and display")
        }
        item {
            SettingsCard(title = "Privacy posture", icon = Icons.Outlined.Security) {
                Text("Local-first analytics. Call records are read on this phone and are not uploaded by CallScope.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                StatusLine("Call-log access", if (state.canReadCallLog) "Granted" else "Not granted")
                StatusLine("Data source", if (state.usingSampleData) "Sample data" else "This device (${state.productionCallCount} calls)")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRequestPermissions) {
                        Text(if (state.canReadCallLog) "Review access" else "Grant access")
                    }
                    OutlinedButton(onClick = onRefresh) {
                        Text("Refresh")
                    }
                }
            }
        }
        item {
            SettingsCard(title = "Theme", icon = Icons.Outlined.Settings) {
                ThemeMode.entries.forEach { mode ->
                    SettingRow(
                        label = mode.label,
                        value = mode == state.themeMode,
                        onCheckedChange = { onThemeSelected(mode) },
                    )
                }
            }
        }
        item {
            SettingsCard(title = "Export and delete", icon = Icons.Outlined.Download) {
                Text("Exports and deletes are designed for local device data only. The MVP has no cloud account and no remote sync.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { }, enabled = false) {
                        Icon(Icons.Outlined.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export CSV")
                    }
                    OutlinedButton(onClick = { }, enabled = false) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete cache")
                    }
                }
            }
        }
        item {
            SettingsCard(title = "Backup", icon = Icons.Outlined.Security) {
                Text("Backup support is intentionally a placeholder for F-Droid review. No backup destination is configured.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ContactDetailScreen(
    contact: ContactStats,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenList(modifier) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                }
                Avatar(contact.displayName)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(contact.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(contact.phoneNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(8.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Relationship summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    StatGrid(
                        listOf(
                            "Calls" to contact.totalCalls.toString(),
                            "Total" to contact.totalDuration.formatDuration(),
                            "Average" to contact.averageDuration.formatDuration(),
                            "Best time" to "${contact.bestDay}, ${contact.bestHourLabel}",
                        ),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        item {
            DirectionBreakdownCard(contact.breakdown.incoming, contact.breakdown.outgoing, contact.breakdown.missed, contact.breakdown.rejected + contact.breakdown.blocked)
        }
        item {
            SectionTitle("Recent timeline")
        }
        items(contact.calls.take(10), key = { "detail-${it.id}" }) { call ->
            CallRow(call = call, onClick = {})
        }
        item {
            SettingsCard(title = "Notes and tags", icon = Icons.Outlined.Person) {
                Text("No local notes yet. This is reserved for private tags such as family, work, or follow-up.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ScreenList(
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun Header(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrustBanner(state: CallScopeUiState, onRequestPermissions: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    ElevatedCard(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (state.usingSampleData) colorScheme.secondaryContainer else colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.Security, contentDescription = null)
            Column(Modifier.weight(1f)) {
                Text("Local-first and permission-aware", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    when {
                        !state.usingSampleData -> "Reading this device call log locally. Nothing is uploaded."
                        state.canReadCallLog -> "You are viewing sample data because this device has no readable call log."
                        else -> "You are viewing sample data because call-log access has not been granted."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (!state.canReadCallLog) {
                TextButton(onClick = onRequestPermissions) {
                    Text("Grant")
                }
            }
        }
    }
}

@Composable
private fun RangeChips(selected: DateRange, onSelected: (DateRange) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DateRange.entries.forEach { range ->
            FilterChip(
                selected = selected == range,
                onClick = { onSelected(range) },
                label = { Text(range.label) },
            )
        }
    }
}

@Composable
private fun HeroStats(summary: AnalyticsSummary) {
    ElevatedCard(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("This period", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            StatGrid(
                listOf(
                    "Calls" to summary.totalCalls.toString(),
                    "Total time" to summary.totalDuration.formatDuration(),
                    "Average" to summary.averageDuration.formatDuration(),
                    "Contacts" to summary.contacts.size.toString(),
                ),
            )
        }
    }
}

@Composable
private fun StatGrid(items: List<Pair<String, String>>, contentColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(contentColor.copy(alpha = 0.08f))
                            .padding(12.dp),
                    ) {
                        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = contentColor)
                        Text(label, style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.72f), maxLines = 1)
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InsightAnswerGrid(summary: AnalyticsSummary, onContactSelected: (String) -> Unit) {
    val mostTalked = summary.contacts.maxByOrNull { it.totalDuration }
    val callsMe = summary.contacts.maxByOrNull { it.breakdown.incoming }
    val iCall = summary.contacts.maxByOrNull { it.breakdown.outgoing }
    val misses = summary.contacts.maxByOrNull { it.breakdown.missed }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnswerCard("Most time", mostTalked?.displayName ?: "No data", mostTalked?.totalDuration?.formatDuration() ?: "0m", mostTalked?.contactId, onContactSelected, Modifier.weight(1f))
            AnswerCard("Calls me most", callsMe?.displayName ?: "No data", "${callsMe?.breakdown?.incoming ?: 0} incoming", callsMe?.contactId, onContactSelected, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AnswerCard("I call most", iCall?.displayName ?: "No data", "${iCall?.breakdown?.outgoing ?: 0} outgoing", iCall?.contactId, onContactSelected, Modifier.weight(1f))
            AnswerCard("Most missed", misses?.displayName ?: "No data", "${misses?.breakdown?.missed ?: 0} missed", misses?.contactId, onContactSelected, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AttentionQueue(summary: AnalyticsSummary, onContactSelected: (String) -> Unit) {
    val contactsById = summary.contacts.associateBy { it.contactId }
    val followUps = summary.calls
        .filter { it.direction == CallDirection.Missed || it.direction == CallDirection.Rejected || it.direction == CallDirection.Blocked }
        .groupBy { it.contactId }
        .map { (contactId, calls) ->
            val latest = calls.maxBy { it.startedAt }
            AttentionItem(
                contactId = contactId,
                name = contactsById[contactId]?.displayName ?: latest.displayName,
                label = "${calls.size} missed or screened",
                detail = "Last ${latest.direction.label.lowercase()} on ${latest.startedAt.relativeDate()}",
                priority = calls.size * 10,
            )
        }
    val reconnects = summary.calls
        .filter { it.recentContext(summary).startsWith("Reconnected") }
        .groupBy { it.contactId }
        .map { (contactId, calls) ->
            val latest = calls.maxBy { it.startedAt }
            AttentionItem(
                contactId = contactId,
                name = contactsById[contactId]?.displayName ?: latest.displayName,
                label = "Reconnected",
                detail = latest.recentContext(summary),
                priority = 5,
            )
        }
    val items = (followUps + reconnects)
        .groupBy { it.contactId }
        .map { (_, grouped) -> grouped.maxBy { it.priority } }
        .sortedByDescending { it.priority }
        .take(4)

    if (items.isEmpty()) {
        ElevatedCard(shape = RoundedCornerShape(8.dp)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.good)
                Column {
                    Text("No obvious follow-ups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Recent calls do not show missed or screened conversations in this period.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            AttentionCard(item = item, onClick = { onContactSelected(item.contactId) })
        }
    }
}

@Composable
private fun RecentActivitySummary(summary: AnalyticsSummary) {
    val now = Instant.now()
    val lastSevenStart = now.minus(7, ChronoUnit.DAYS)
    val previousSevenStart = now.minus(14, ChronoUnit.DAYS)
    val lastSeven = summary.calls.filter { it.startedAt >= lastSevenStart }
    val previousSeven = summary.calls.filter { it.startedAt >= previousSevenStart && it.startedAt < lastSevenStart }
    val missed = lastSeven.count { it.direction == CallDirection.Missed || it.direction == CallDirection.Rejected }
    val longCalls = lastSeven.count { it.duration >= Duration.ofMinutes(30) }
    val delta = lastSeven.size - previousSeven.size

    ElevatedCard(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Current pace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            StatGrid(
                listOf(
                    "Calls" to lastSeven.size.toString(),
                    "Vs prior week" to delta.signedCount(),
                    "Missed" to missed.toString(),
                    "Long calls" to longCalls.toString(),
                ),
            )
        }
    }
}

@Composable
private fun AttentionCard(item: AttentionItem, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(item.name)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun RelationshipChangeCards(summary: AnalyticsSummary, onContactSelected: (String) -> Unit) {
    val quiet = summary.quietContacts.firstOrNull()
    val active = summary.moreActiveContacts.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnswerCard(
            label = "Going quiet",
            value = quiet?.stats?.displayName ?: "No clear drop-off",
            detail = quiet?.let { "${it.previousCalls} prior, ${it.recentCalls} recent" } ?: "No follow-up signal",
            contactId = quiet?.stats?.contactId,
            onContactSelected = onContactSelected,
            modifier = Modifier.fillMaxWidth(),
        )
        AnswerCard(
            label = "Getting more active",
            value = active?.stats?.displayName ?: "No spike",
            detail = active?.let { "+${it.delta} calls recently" } ?: "No unusual increase",
            contactId = active?.stats?.contactId,
            onContactSelected = onContactSelected,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AnswerCard(
    label: String,
    value: String,
    detail: String,
    contactId: String?,
    onContactSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .height(116.dp)
            .then(if (contactId != null) Modifier.clickable { onContactSelected(contactId) } else Modifier),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun ContactRow(contact: ContactStats, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(contact.displayName)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(contact.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${contact.totalCalls} calls • ${contact.totalDuration.formatDuration()} • ${contact.breakdown.missed} missed", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Meter(value = contact.totalDuration.toMinutes().toFloat(), maxValue = 120f)
            }
            Spacer(Modifier.width(10.dp))
            Text(contact.lastCallAt.relativeDate(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CallRow(call: CallRecord, context: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DirectionIcon(call.direction)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(call.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${call.direction.label} • ${call.startedAt.relativeDate()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (context != null) {
                Text(context, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(call.duration.formatDuration(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun DirectionIcon(direction: CallDirection) {
    val (icon, color) = when (direction) {
        CallDirection.Incoming -> Icons.Outlined.CallReceived to MaterialTheme.colorScheme.good
        CallDirection.Outgoing -> Icons.Outlined.CallMade to MaterialTheme.colorScheme.primary
        CallDirection.Missed -> Icons.Outlined.CallMissed to MaterialTheme.colorScheme.error
        CallDirection.Rejected, CallDirection.Blocked -> Icons.Outlined.Block to MaterialTheme.colorScheme.warning
        CallDirection.Unknown -> Icons.Outlined.Phone to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Avatar(name: String) {
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "?" }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UsageChart(days: List<DailyUsage>) {
    val maxMinutes = max(1f, days.maxOfOrNull { it.totalDuration.toMinutes().toFloat() } ?: 1f)
    ElevatedCard(shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.takeLast(14).forEach { day ->
                val calculatedHeight = (128 * (day.totalDuration.toMinutes().toFloat() / maxMinutes)).dp
                val height = if (calculatedHeight < 8.dp) 8.dp else calculatedHeight
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DirectionBalance(summary: AnalyticsSummary) {
    val incoming = summary.breakdown.incoming
    val outgoing = summary.breakdown.outgoing
    val total = max(1, incoming + outgoing)
    ElevatedCard(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusLine("Incoming", "$incoming calls")
            LinearProgressIndicator(
                progress = { incoming / total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.good,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            StatusLine("Outgoing", "$outgoing calls")
            LinearProgressIndicator(
                progress = { outgoing / total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun DirectionBreakdownCard(incoming: Int, outgoing: Int, missed: Int, blocked: Int) {
    ElevatedCard(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            StatusLine("Incoming", incoming.toString())
            StatusLine("Outgoing", outgoing.toString())
            StatusLine("Missed", missed.toString())
            StatusLine("Rejected or blocked", blocked.toString())
        }
    }
}

@Composable
private fun BusyGrid(summary: AnalyticsSummary) {
    ElevatedCard(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val busiestDay = summary.busiestDays.firstOrNull()
            val busiestHour = summary.hourlyUsage.maxByOrNull { it.callCount }
            StatusLine("Top day", busiestDay?.let { "${it.date.format(DateTimeFormatter.ofPattern("MMM d"))} • ${it.callCount} calls" } ?: "No data")
            StatusLine("Top time", busiestHour?.let { "${it.hour.hourLabel()} • ${it.callCount} calls" } ?: "No data")
        }
    }
}

@Composable
private fun TrendCards(summary: AnalyticsSummary, onContactSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val quiet = summary.quietContacts.firstOrNull()
        val active = summary.moreActiveContacts.firstOrNull()
        AnswerCard("Going quiet", quiet?.stats?.displayName ?: "No signal", quiet?.let { "${it.previousCalls} previous, ${it.recentCalls} recent" } ?: "Need more history", quiet?.stats?.contactId, onContactSelected)
        AnswerCard("More active", active?.stats?.displayName ?: "No signal", active?.let { "+${it.delta} calls recently" } ?: "Need more history", active?.stats?.contactId, onContactSelected)
    }
}

@Composable
private fun SettingsCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun SettingRow(label: String, value: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = value, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(max = 190.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Meter(value: Float, maxValue: Float) {
    LinearProgressIndicator(
        progress = { (value / maxValue).coerceIn(0.04f, 1f) },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

private val AppTab.label: String
    get() = when (this) {
        AppTab.Review -> "Review"
        AppTab.People -> "People"
        AppTab.Insights -> "Insights"
        AppTab.Settings -> "Settings"
    }

private val AppTab.icon: ImageVector
    get() = when (this) {
        AppTab.Review -> Icons.Outlined.Security
        AppTab.People -> Icons.Outlined.Person
        AppTab.Insights -> Icons.Outlined.BarChart
        AppTab.Settings -> Icons.Outlined.Settings
    }

private val ContactRank.label: String
    get() = when (this) {
        ContactRank.CallTime -> "Call time"
        ContactRank.CallCount -> "Call count"
        ContactRank.MissedCalls -> "Missed"
        ContactRank.Recency -> "Recent"
    }

private val DateRange.label: String
    get() = when (this) {
        DateRange.Week -> "7 days"
        DateRange.Month -> "30 days"
        DateRange.Year -> "Year"
        DateRange.All -> "All"
    }

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.System -> "Use system theme"
        ThemeMode.Light -> "Light mode"
        ThemeMode.Dark -> "Dark mode"
    }

private val CallDirection.label: String
    get() = when (this) {
        CallDirection.Incoming -> "Incoming"
        CallDirection.Outgoing -> "Outgoing"
        CallDirection.Missed -> "Missed"
        CallDirection.Rejected -> "Rejected"
        CallDirection.Blocked -> "Blocked"
        CallDirection.Unknown -> "Unknown"
    }

private fun Duration.formatDuration(): String {
    val totalSeconds = toSeconds()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m"
        seconds > 0 -> "${seconds}s"
        else -> "0s"
    }
}

private fun java.time.Instant.relativeDate(): String {
    val local = atZone(ZoneId.systemDefault()).toLocalDate()
    return local.format(DateTimeFormatter.ofPattern("MMM d"))
}

private fun Int.hourLabel(): String = when (this) {
    0 -> "12 AM"
    in 1..11 -> "$this AM"
    12 -> "12 PM"
    else -> "${this - 12} PM"
}

private fun CallRecord.recentContext(summary: AnalyticsSummary): String {
    val contactCalls = summary.contacts
        .firstOrNull { it.contactId == contactId }
        ?.calls
        .orEmpty()
    val previous = contactCalls
        .filter { it.startedAt < startedAt }
        .maxByOrNull { it.startedAt }
    val daysSincePrevious = previous?.startedAt?.until(startedAt, ChronoUnit.DAYS)

    return when {
        direction == CallDirection.Missed -> "Potential follow-up"
        direction == CallDirection.Rejected -> "Screened call"
        direction == CallDirection.Blocked -> "Blocked number"
        duration >= Duration.ofMinutes(45) -> "Long conversation"
        daysSincePrevious != null && daysSincePrevious >= 14 -> "Reconnected after ${daysSincePrevious}d"
        contactCalls.size >= 4 -> "${contactCalls.size} calls this period"
        else -> "Recent touchpoint"
    }
}

private fun Int.signedCount(): String = when {
    this > 0 -> "+$this"
    else -> toString()
}

private data class AttentionItem(
    val contactId: String,
    val name: String,
    val label: String,
    val detail: String,
    val priority: Int,
)
