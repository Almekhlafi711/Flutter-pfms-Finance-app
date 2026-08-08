package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.DesignTokens
import com.example.core.util.DebtPdfReportGenerator
import com.example.domain.model.DebtType
import com.example.domain.model.Person
import com.example.domain.model.PersonDebtAccount
import com.example.ui.components.*
import com.example.ui.viewmodel.PfmsViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtCenterScreen(
    viewModel: PfmsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val personAccounts by viewModel.personDebtAccounts.collectAsState()
    val persons by viewModel.persons.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // Dynamic currencies based on actual user accounts
    val availableCurrencies = remember(personAccounts) {
        val currs = personAccounts.map { it.mainDebt.currency.uppercase() }.distinct()
        if (currs.isEmpty()) listOf("SAR", "USD", "YER") else currs
    }

    val currencyTabs = availableCurrencies.map { currencyCode ->
        val count = personAccounts.count { it.mainDebt.currency.equals(currencyCode, ignoreCase = true) }
        CurrencyTabInfo(code = currencyCode, count = count)
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { availableCurrencies.size }
    )

    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Navigation state
    var selectedPersonForProfile by remember { mutableStateOf<Person?>(null) }
    var selectedCurrencyForProfile by remember { mutableStateOf<String?>(null) }
    var selectedAccountForDetails by remember { mutableStateOf<PersonDebtAccount?>(null) }

    // Bottom Sheet states
    var showSpeedDialSheet by remember { mutableStateOf(false) }
    var showAddDebtSheet by remember { mutableStateOf(false) }
    var activeOpTypeInFlow by remember { mutableStateOf<LedgerOperationType?>(null) }
    var showReceivePaymentSheet by remember { mutableStateOf(false) }

    // Filter Bottom Sheet state
    var filterState by remember { mutableStateOf(DebtFilterState()) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }

    // 1. If a person profile is open
    if (selectedPersonForProfile != null) {
        val currentPerson = persons.find { it.id == selectedPersonForProfile?.id } ?: selectedPersonForProfile!!
        val profileCurrency = selectedCurrencyForProfile ?: availableCurrencies.getOrNull(pagerState.currentPage) ?: "SAR"
        PersonProfileScreen(
            initialPerson = currentPerson,
            currency = profileCurrency,
            viewModel = viewModel,
            isArabic = isArabic,
            onNavigateBack = {
                selectedPersonForProfile = null
                selectedCurrencyForProfile = null
            }
        )
        return
    }

    // 2. If a specific account timeline is open
    val currentSelectedAccount = selectedAccountForDetails?.let { sel ->
        personAccounts.find { it.person.id == sel.person.id || it.mainDebt.id == sel.mainDebt.id } ?: sel
    }

    if (currentSelectedAccount != null) {
        DebtDetailsScreen(
            account = currentSelectedAccount,
            onNavigateBack = { selectedAccountForDetails = null },
            onRecordPayment = { showReceivePaymentSheet = true }
        )

        if (showReceivePaymentSheet) {
            val isReceivable = currentSelectedAccount.mainDebt.type == DebtType.RECEIVABLE
            val defaultOpType = if (isReceivable) LedgerOperationType.RECEIVE_PAYMENT else LedgerOperationType.PAY_DEBT

            DebtOperationFlow(
                persons = persons,
                accounts = accounts,
                personDebtAccounts = personAccounts,
                isArabic = isArabic,
                initialPersonId = currentSelectedAccount.person.id,
                initialCurrency = currentSelectedAccount.mainDebt.currency,
                initialOperationType = defaultOpType,
                onDismiss = { showReceivePaymentSheet = false },
                onExecuteOperation = { person, curr, opType, dir, amt, accId, notes ->
                    showReceivePaymentSheet = false
                    when (opType) {
                        LedgerOperationType.ADD_DEBT -> {
                            val debtType = dir ?: DebtType.RECEIVABLE
                            viewModel.addDebtForPerson(person, debtType, amt, accId, "General", curr, notes)
                        }
                        LedgerOperationType.RECEIVE_PAYMENT -> {
                            viewModel.addPaymentForPerson(person, amt, accId, curr, notes, isReceive = true)
                        }
                        LedgerOperationType.PAY_DEBT -> {
                            viewModel.addPaymentForPerson(person, amt, accId, curr, notes, isReceive = false)
                        }
                    }
                },
                onCreatePerson = { newPrs -> viewModel.addPerson(newPrs) }
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isArabic) "مركز الديون" else "Debt Center",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isArabic) {
                                "${persons.size} شخصاً • ${personAccounts.size} حساب دين"
                            } else {
                                "${persons.size} Persons • ${personAccounts.size} Debt Ledgers"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }

                    IconButton(onClick = {
                        val firstAccount = personAccounts.firstOrNull()
                        if (firstAccount != null) {
                            val pdfFile = DebtPdfReportGenerator.generatePersonDebtStatementPdf(context, firstAccount)
                            if (pdfFile != null) {
                                Toast.makeText(context, if (isArabic) "تم تصدير كشف الديون PDF: ${pdfFile.name}" else "Exported Debt Statement PDF: ${pdfFile.name}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, if (isArabic) "لا توجد حسابات ديون للتصدير" else "No debt accounts to export", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isArabic) "✨ إجراء جديد" else "✨ New Action") },
                            onClick = {
                                showMoreMenu = false
                                activeOpTypeInFlow = null
                                showAddDebtSheet = true
                            },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isArabic) "بحث في الحسابات" else "Search Accounts") },
                            onClick = {
                                showMoreMenu = false
                                isSearchVisible = true
                            },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isArabic) "خيارات الفلترة" else "Filter Options") },
                            onClick = {
                                showMoreMenu = false
                                showFilterBottomSheet = true
                            },
                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    activeOpTypeInFlow = null
                    showAddDebtSheet = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                text = {
                    Text(
                        text = if (isArabic) "✨ إجراء جديد" else "✨ New Action",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                containerColor = Color(0xFF6B46C1),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        },
        floatingActionButtonPosition = if (isArabic) FabPosition.Start else FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Animated Search Bar
            AnimatedVisibility(
                visible = isSearchVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                DebtSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = if (isArabic) "البحث في الاسم، الهاتف، البيان، الحساب..." else "Search name, phone, notes, account..."
                )
            }

            // Dynamic Currency Tabs Header (SAR, USD, YER, etc. - No ALL tab)
            CurrencyFilterTabs(
                currencies = currencyTabs,
                selectedIndex = pagerState.currentPage,
                onTabSelected = { targetPage ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(targetPage)
                    }
                }
            )

            // Filter Trigger Button Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filterState.isFiltered,
                    onClick = { showFilterBottomSheet = true },
                    label = {
                        Text(
                            text = if (filterState.isFiltered) {
                                if (isArabic) "فلترة (مفعّلة)" else "Filter (Active)"
                            } else {
                                if (isArabic) "فلترة" else "Filter"
                            },
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(DesignTokens.RadiusSmall)
                )

                if (filterState.isFiltered) {
                    TextButton(
                        onClick = { filterState = DebtFilterState() }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "إعادة ضبط" else "Reset",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Main Content Area with Horizontal Pager (No Side Arrows)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { pageIndex ->
                        val pageCurrency = availableCurrencies.getOrNull(pageIndex) ?: "SAR"

                        val pageAccounts = personAccounts.filter { acc ->
                            val matchesCurrency = acc.mainDebt.currency.equals(pageCurrency, ignoreCase = true)

                            val matchesStatus = when (filterState.status) {
                                DebtStatusFilter.ALL -> true
                                DebtStatusFilter.ACTIVE -> acc.person.isActive
                                DebtStatusFilter.INACTIVE -> !acc.person.isActive
                            }

                            val matchesCategory = when (filterState.category) {
                                DebtCategoryFilter.ALL -> true
                                DebtCategoryFilter.PERSONAL -> acc.person.category.equals("Personal", ignoreCase = true) || acc.person.category.contains("شخصي", ignoreCase = true)
                                DebtCategoryFilter.CORPORATE -> acc.person.category.equals("Corporate", ignoreCase = true) || acc.person.category.contains("مؤسسي", ignoreCase = true)
                            }

                            val matchesType = when (filterState.type) {
                                DebtTypeFilter.ALL -> true
                                DebtTypeFilter.RECEIVABLE -> acc.mainDebt.type == DebtType.RECEIVABLE
                                DebtTypeFilter.PAYABLE -> acc.mainDebt.type == DebtType.PAYABLE
                            }

                            val matchesDateRange = when (filterState.dateRange) {
                                DebtDateRangeFilter.ALL -> true
                                DebtDateRangeFilter.TODAY -> {
                                    val cal = Calendar.getInstance()
                                    cal.set(Calendar.HOUR_OF_DAY, 0)
                                    cal.set(Calendar.MINUTE, 0)
                                    cal.set(Calendar.SECOND, 0)
                                    cal.set(Calendar.MILLISECOND, 0)
                                    acc.lastTransactionDate >= cal.timeInMillis
                                }
                                DebtDateRangeFilter.LAST_7_DAYS -> {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.DAY_OF_YEAR, -7)
                                    acc.lastTransactionDate >= cal.timeInMillis
                                }
                                DebtDateRangeFilter.LAST_30_DAYS -> {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.DAY_OF_YEAR, -30)
                                    acc.lastTransactionDate >= cal.timeInMillis
                                }
                                DebtDateRangeFilter.THIS_MONTH -> {
                                    val cal = Calendar.getInstance()
                                    cal.set(Calendar.DAY_OF_MONTH, 1)
                                    cal.set(Calendar.HOUR_OF_DAY, 0)
                                    cal.set(Calendar.MINUTE, 0)
                                    cal.set(Calendar.SECOND, 0)
                                    cal.set(Calendar.MILLISECOND, 0)
                                    acc.lastTransactionDate >= cal.timeInMillis
                                }
                                DebtDateRangeFilter.THIS_YEAR -> {
                                    val cal = Calendar.getInstance()
                                    cal.set(Calendar.DAY_OF_YEAR, 1)
                                    cal.set(Calendar.HOUR_OF_DAY, 0)
                                    cal.set(Calendar.MINUTE, 0)
                                    cal.set(Calendar.SECOND, 0)
                                    cal.set(Calendar.MILLISECOND, 0)
                                    acc.lastTransactionDate >= cal.timeInMillis
                                }
                                DebtDateRangeFilter.CUSTOM -> {
                                    val startOk = filterState.customStartDate?.let { acc.lastTransactionDate >= it } ?: true
                                    val endOk = filterState.customEndDate?.let { acc.lastTransactionDate <= (it + 86400000) } ?: true
                                    startOk && endOk
                                }
                            }

                            val matchesSearch = if (searchQuery.isBlank()) true else {
                                acc.person.name.contains(searchQuery, ignoreCase = true) ||
                                        (acc.person.phone?.contains(searchQuery, ignoreCase = true) == true) ||
                                        acc.person.category.contains(searchQuery, ignoreCase = true) ||
                                        acc.mainDebt.notes.contains(searchQuery, ignoreCase = true)
                            }

                            matchesCurrency && matchesStatus && matchesCategory && matchesType && matchesDateRange && matchesSearch
                        }

                        val totalReceivables = pageAccounts.filter { it.mainDebt.type == DebtType.RECEIVABLE }.sumOf { it.totalRemainingAmount }
                        val totalPayables = pageAccounts.filter { it.mainDebt.type == DebtType.PAYABLE }.sumOf { it.totalRemainingAmount }
                        val distinctPersonsCount = pageAccounts.map { it.person.id }.distinct().size

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                DebtSummaryCard(
                                    totalReceivables = totalReceivables,
                                    totalPayables = totalPayables,
                                    currency = pageCurrency,
                                    personsCount = distinctPersonsCount,
                                    ledgersCount = pageAccounts.size,
                                    isArabic = isArabic
                                )
                            }

                            if (pageAccounts.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        shape = RoundedCornerShape(DesignTokens.RadiusMedium),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (searchQuery.isNotEmpty() || filterState.isFiltered) {
                                                    if (isArabic) "لم يتم العثور على نتائج تطابق خيارات الفلترة أو البحث." else "No debt accounts found matching your filters."
                                                } else {
                                                    if (isArabic) "لا توجد مديونيات مسجلة لعملة $pageCurrency حالياً." else "No registered debts for $pageCurrency at present."
                                                },
                                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(pageAccounts, key = { it.person.id + "_" + it.mainDebt.id }) { account ->
                                    DebtCard(
                                        account = account,
                                        isArabic = isArabic,
                                        onClick = {
                                            selectedPersonForProfile = account.person
                                            selectedCurrencyForProfile = account.mainDebt.currency
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Page Dots Indicator (Apple Wallet Style - Dots below cards)
                    if (availableCurrencies.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(availableCurrencies.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (isSelected) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. UNIFIED DEBT OPERATION FLOW
    if (showAddDebtSheet) {
        val currentCurrencyFilter = availableCurrencies.getOrNull(pagerState.currentPage) ?: "SAR"

        DebtOperationFlow(
            persons = persons,
            accounts = accounts,
            personDebtAccounts = personAccounts,
            isArabic = isArabic,
            initialCurrency = currentCurrencyFilter,
            initialOperationType = activeOpTypeInFlow,
            onDismiss = { showAddDebtSheet = false },
            onExecuteOperation = { person, curr, opType, dir, amt, accId, notes ->
                showAddDebtSheet = false
                when (opType) {
                    LedgerOperationType.ADD_DEBT -> {
                        val debtType = dir ?: DebtType.RECEIVABLE
                        viewModel.addDebtForPerson(person, debtType, amt, accId, "General", curr, notes)
                    }
                    LedgerOperationType.RECEIVE_PAYMENT -> {
                        viewModel.addPaymentForPerson(person, amt, accId, curr, notes, isReceive = true)
                    }
                    LedgerOperationType.PAY_DEBT -> {
                        viewModel.addPaymentForPerson(person, amt, accId, curr, notes, isReceive = false)
                    }
                }
            },
            onCreatePerson = { newPrs -> viewModel.addPerson(newPrs) }
        )
    }

    // 5. FILTER BOTTOM SHEET
    if (showFilterBottomSheet) {
        DebtFilterBottomSheet(
            initialFilterState = filterState,
            isArabic = isArabic,
            onDismiss = { showFilterBottomSheet = false },
            onApplyFilter = { newFilter ->
                filterState = newFilter
            }
        )
    }
}
