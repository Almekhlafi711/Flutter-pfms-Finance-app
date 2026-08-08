package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.GreenIncome
import com.example.core.theme.RedExpense
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.Account
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.ui.components.*
import com.example.ui.viewmodel.PfmsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class TimeFilter(val labelEn: String, val labelAr: String) {
    TODAY("Today", "اليوم"),
    YESTERDAY("Yesterday", "الأمس"),
    LAST_7_DAYS("Last 7 Days", "آخر 7 أيام"),
    LAST_30_DAYS("Last 30 Days", "آخر 30 يوم"),
    THIS_MONTH("This Month", "هذا الشهر"),
    THIS_YEAR("This Year", "هذه السنة"),
    ALL_TIME("All Time", "كل الأوقات")
}

enum class TransactionSortOrder(val labelEn: String, val labelAr: String) {
    NEWEST("Newest First", "الأحدث أولاً"),
    OLDEST("Oldest First", "الأقدم أولاً"),
    HIGHEST_AMOUNT("Highest Amount", "الأعلى مبلغاً"),
    LOWEST_AMOUNT("Lowest Amount", "الأقل مبلغاً")
}

private fun checkTimeFilter(
    txDate: Long,
    filterTimeRange: TimeFilter,
    now: Long,
    calendar: Calendar
): Boolean {
    return when (filterTimeRange) {
        TimeFilter.TODAY -> {
            calendar.timeInMillis = now
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            txDate >= calendar.timeInMillis
        }
        TimeFilter.YESTERDAY -> {
            calendar.timeInMillis = now
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfYesterday = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val endOfYesterday = calendar.timeInMillis
            txDate in startOfYesterday until endOfYesterday
        }
        TimeFilter.LAST_7_DAYS -> txDate >= (now - 7L * 24 * 3600 * 1000)
        TimeFilter.LAST_30_DAYS -> txDate >= (now - 30L * 24 * 3600 * 1000)
        TimeFilter.THIS_MONTH -> {
            calendar.timeInMillis = now
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            txDate >= calendar.timeInMillis
        }
        TimeFilter.THIS_YEAR -> {
            calendar.timeInMillis = now
            calendar.set(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            txDate >= calendar.timeInMillis
        }
        TimeFilter.ALL_TIME -> true
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    viewModel: PfmsViewModel,
    onNavigateBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isSearchFocused by remember { mutableStateOf(false) }

    // Filter states
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterTimeRange by remember { mutableStateOf(TimeFilter.THIS_MONTH) }
    var filterAccountId by remember { mutableStateOf<String?>(null) } // null = All
    var filterCurrency by remember { mutableStateOf<String?>(null) } // null = All
    var filterCategory by remember { mutableStateOf<String?>(null) } // null = All
    var filterSortOrder by remember { mutableStateOf(TransactionSortOrder.NEWEST) }

    // Main 4-Tab Horizontal Pager for Full Gesture Sync: 0 = All, 1 = Income, 2 = Expense, 3 = Transfer
    val mainPagerState = rememberPagerState(pageCount = { 4 })

    // Currencies list for multi-currency header carousel
    val supportedCurrencies = remember(accounts, transactions) {
        val fromAccounts = accounts.map { it.currency }
        val fromTxs = transactions.map { it.currency }
        (fromAccounts + fromTxs + listOf("SAR", "USD", "YER")).filter { it.isNotBlank() }.distinct()
    }
    
    val currencyPagerState = rememberPagerState(pageCount = { supportedCurrencies.size.coerceAtLeast(1) })
    val activeCurrency = supportedCurrencies.getOrNull(currencyPagerState.currentPage) ?: "SAR"

    LaunchedEffect(filterCurrency) {
        if (filterCurrency != null) {
            val targetIdx = supportedCurrencies.indexOfFirst { it.equals(filterCurrency, ignoreCase = true) }
            if (targetIdx >= 0 && targetIdx != currencyPagerState.currentPage) {
                currencyPagerState.animateScrollToPage(targetIdx)
            }
        }
    }

    // Action sheet modal states
    var activeActionSheet by remember { mutableStateOf<String?>(null) } // "INCOME", "EXPENSE", "TRANSFER", "FAB_MENU"
    var selectedDetailTransaction by remember { mutableStateOf<Transaction?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Dynamic colors & gradients based on current active tab index
    val (activeGradient, activeAccentColor, headerTitleText) = remember(mainPagerState.currentPage, isArabic) {
        when (mainPagerState.currentPage) {
            1 -> Triple(
                Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF10B981))),
                Color(0xFF10B981),
                if (isArabic) "إجمالي الدخل" else "Total Income"
            )
            2 -> Triple(
                Brush.linearGradient(listOf(Color(0xFF991B1B), Color(0xFFEF4444))),
                Color(0xFFEF4444),
                if (isArabic) "إجمالي المصروفات" else "Total Expenses"
            )
            3 -> Triple(
                Brush.linearGradient(listOf(Color(0xFF1E40AF), Color(0xFF3B82F6))),
                Color(0xFF3B82F6),
                if (isArabic) "إجمالي التحويلات" else "Total Transfers"
            )
            else -> Triple(
                Brush.linearGradient(listOf(Color(0xFF6B46C1), Color(0xFF805AD5))),
                Color(0xFF6B46C1),
                if (isArabic) "إجمالي التدفقات المالية" else "Total Financial Operations"
            )
        }
    }

    // Active filters count indicator
    val activeFiltersCount = remember(filterTimeRange, filterAccountId, filterCurrency, filterCategory) {
        var count = 0
        if (filterTimeRange != TimeFilter.THIS_MONTH) count++
        if (filterAccountId != null) count++
        if (filterCurrency != null) count++
        if (filterCategory != null) count++
        count
    }

    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isArabic) "مركز العمليات النقدية" else "Transactions Center",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (isArabic) "سجل العمليات والتحويلات اليومية" else "Daily Cash Operations & Transfers",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButtonPosition = if (isArabic) FabPosition.Start else FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (mainPagerState.currentPage) {
                        1 -> activeActionSheet = "INCOME"
                        2 -> activeActionSheet = "EXPENSE"
                        3 -> activeActionSheet = "TRANSFER"
                        else -> activeActionSheet = "FAB_MENU"
                    }
                },
                containerColor = activeAccentColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 💳 1. DYNAMIC GRADIENT HEADER CARD (Glassmorphic Multi-Currency Carousel)
            val currentTabType = when (mainPagerState.currentPage) {
                1 -> TransactionType.INCOME
                2 -> TransactionType.EXPENSE
                3 -> TransactionType.TRANSFER
                else -> null
            }

            HorizontalPager(
                state = currencyPagerState,
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                val cardCurrency = supportedCurrencies.getOrNull(pageIndex) ?: "SAR"

                val matchingHeaderTxs = remember(transactions, currentTabType, cardCurrency, filterTimeRange, filterAccountId, filterCategory) {
                    transactions.filter { tx ->
                        if (!tx.currency.equals(cardCurrency, ignoreCase = true)) return@filter false
                        val isDailyCashTx = tx.type == TransactionType.INCOME ||
                                tx.type == TransactionType.EXPENSE ||
                                tx.type == TransactionType.TRANSFER ||
                                tx.type == TransactionType.BILL_PAYMENT
                        if (!isDailyCashTx) return@filter false

                        val matchesTab = when (currentTabType) {
                            null -> true
                            TransactionType.INCOME -> tx.type == TransactionType.INCOME
                            TransactionType.EXPENSE -> tx.type == TransactionType.EXPENSE || tx.type == TransactionType.BILL_PAYMENT
                            TransactionType.TRANSFER -> tx.type == TransactionType.TRANSFER
                            else -> tx.type == currentTabType
                        }
                        val matchesAccount = filterAccountId == null || tx.sourceAccountId == filterAccountId || tx.destinationAccountId == filterAccountId
                        val matchesCategory = filterCategory == null || tx.category.equals(filterCategory, ignoreCase = true)
                        val matchesTime = checkTimeFilter(tx.date, filterTimeRange, now, calendar)

                        matchesTab && matchesAccount && matchesCategory && matchesTime
                    }
                }

                val totalHeaderAmount = remember(matchingHeaderTxs, currentTabType) {
                    matchingHeaderTxs.sumOf { tx ->
                        if (currentTabType == null && (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.BILL_PAYMENT)) {
                            -tx.amount
                        } else {
                            tx.amount
                        }
                    }
                }

                val txHeaderCount = matchingHeaderTxs.size

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(26.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(brush = activeGradient)
                            .padding(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 25.dp, y = (-25).dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = headerTitleText,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.95f)
                                    )
                                )

                                // Glassmorphic Currency Selector Pill
                                Surface(
                                    onClick = {
                                        if (supportedCurrencies.size > 1) {
                                            val nextIndex = (pageIndex + 1) % supportedCurrencies.size
                                            coroutineScope.launch { currencyPagerState.animateScrollToPage(nextIndex) }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$cardCurrency ▾",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            Text(
                                text = CurrencyFormatter.format(Math.abs(totalHeaderAmount), cardCurrency),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 32.sp
                                )
                            )

                            Text(
                                text = "$txHeaderCount ${if (isArabic) "عملية" else "Transactions"} • ${if (isArabic) filterTimeRange.labelAr else filterTimeRange.labelEn}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            // 🔘 2. INTERACTIVE CURRENCY DOTS INDICATOR (Directly below summary card)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(supportedCurrencies.size) { index ->
                    val isSelected = currencyPagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 22.dp else 7.dp,
                        label = "dotWidth"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isSelected) activeAccentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        label = "dotColor"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(7.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                            .clickable {
                                coroutineScope.launch { currencyPagerState.animateScrollToPage(index) }
                            }
                    )
                }
            }

            // 🔍 3. iOS FINTECH SEARCH BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSearchFocused) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(
                        width = if (isSearchFocused) 1.5.dp else 0.5.dp,
                        color = if (isSearchFocused) activeAccentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    shadowElevation = if (isSearchFocused) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (isSearchFocused) activeAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = if (isArabic) "البحث في العمليات أو المبالغ..." else "Search transactions...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isSearchFocused = it.isFocused }
                            )
                        }

                        // Clear Button (×)
                        AnimatedVisibility(
                            visible = searchQuery.isNotEmpty(),
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Integrated Filter Button with Badge
                        Box(contentAlignment = Alignment.Center) {
                            IconButton(
                                onClick = { showFilterSheet = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Filter",
                                    tint = if (activeFiltersCount > 0) activeAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (activeFiltersCount > 0) {
                                Badge(
                                    containerColor = activeAccentColor,
                                    contentColor = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 2.dp, end = 2.dp)
                                ) {
                                    Text(
                                        text = "$activeFiltersCount",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isSearchFocused || searchQuery.isNotEmpty(),
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text(
                            text = if (isArabic) "إلغاء" else "Cancel",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = activeAccentColor
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 🎨 4. FILTER BAR / CATEGORY TABS (Active Tab Highlight)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabLabels = listOf(
                    if (isArabic) "الكل" else "All",
                    if (isArabic) "دخل" else "Income",
                    if (isArabic) "مصروف" else "Expense",
                    if (isArabic) "تحويل" else "Transfer"
                )

                tabLabels.forEachIndexed { index, label ->
                    val isSelected = mainPagerState.currentPage == index

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) activeAccentColor else Color.Transparent)
                            .clickable {
                                coroutineScope.launch { mainPagerState.animateScrollToPage(index) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 🔄 5. FULL HORIZONTAL GESTURE SWIPE PAGER FOR TRANSACTIONS
            HorizontalPager(
                state = mainPagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageTabType = when (page) {
                    1 -> TransactionType.INCOME
                    2 -> TransactionType.EXPENSE
                    3 -> TransactionType.TRANSFER
                    else -> null
                }

                val pageFilteredList = remember(
                    transactions,
                    accounts,
                    pageTabType,
                    searchQuery,
                    filterTimeRange,
                    filterAccountId,
                    filterCurrency,
                    activeCurrency,
                    filterCategory,
                    filterSortOrder
                ) {
                    val targetCurr = filterCurrency ?: activeCurrency
                    val baseList = transactions.filter { tx ->
                        val isDailyCashTx = tx.type == TransactionType.INCOME ||
                                tx.type == TransactionType.EXPENSE ||
                                tx.type == TransactionType.TRANSFER ||
                                tx.type == TransactionType.BILL_PAYMENT

                        if (!isDailyCashTx) return@filter false

                        val matchesTab = when (pageTabType) {
                            null -> true
                            TransactionType.INCOME -> tx.type == TransactionType.INCOME
                            TransactionType.EXPENSE -> tx.type == TransactionType.EXPENSE || tx.type == TransactionType.BILL_PAYMENT
                            TransactionType.TRANSFER -> tx.type == TransactionType.TRANSFER
                            else -> tx.type == pageTabType
                        }

                        val matchesAccount = filterAccountId == null ||
                                tx.sourceAccountId == filterAccountId ||
                                tx.destinationAccountId == filterAccountId

                        val matchesCurrency = tx.currency.equals(targetCurr, ignoreCase = true)
                        val matchesCategory = filterCategory == null || tx.category.equals(filterCategory, ignoreCase = true)
                        val matchesTime = checkTimeFilter(tx.date, filterTimeRange, now, calendar)

                        val sourceAccName = accounts.find { it.id == tx.sourceAccountId }?.name ?: ""
                        val destAccName = accounts.find { it.id == tx.destinationAccountId }?.name ?: ""

                        val matchesQuery = searchQuery.isBlank() ||
                                tx.category.contains(searchQuery, ignoreCase = true) ||
                                (tx.party?.contains(searchQuery, ignoreCase = true) ?: false) ||
                                tx.note.contains(searchQuery, ignoreCase = true) ||
                                sourceAccName.contains(searchQuery, ignoreCase = true) ||
                                destAccName.contains(searchQuery, ignoreCase = true) ||
                                tx.amount.toString().contains(searchQuery)

                        matchesTab && matchesAccount && matchesCurrency && matchesCategory && matchesTime && matchesQuery
                    }

                    when (filterSortOrder) {
                        TransactionSortOrder.NEWEST -> baseList.sortedByDescending { it.date }
                        TransactionSortOrder.OLDEST -> baseList.sortedBy { it.date }
                        TransactionSortOrder.HIGHEST_AMOUNT -> baseList.sortedByDescending { it.amount }
                        TransactionSortOrder.LOWEST_AMOUNT -> baseList.sortedBy { it.amount }
                    }
                }

                if (pageFilteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(activeAccentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = activeAccentColor,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Text(
                                text = if (isArabic) "لا يوجد عمليات ماليّة" else "No Transactions Found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isArabic) "لا توجد أي عمليات نقديّة تطابق خيارات الفلترة المحددة." else "There are no cash operations matching your current filter criteria.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp, top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pageFilteredList, key = { it.id }) { tx ->
                            val sourceAccount = accounts.find { it.id == tx.sourceAccountId }
                            val destAccount = accounts.find { it.id == tx.destinationAccountId }

                            TransactionModernCard(
                                transaction = tx,
                                sourceAccountName = sourceAccount?.name ?: (if (isArabic) "حساب نقد" else "Cash Account"),
                                destAccountName = destAccount?.name,
                                onClick = { selectedDetailTransaction = tx }
                            )
                        }
                    }
                }
            }
        }
    }

    // ------------------- BOTTOM SHEETS & MODALS -------------------

    // 1. COMPREHENSIVE FILTER BOTTOM SHEET
    if (showFilterSheet) {
        TransactionsFilterBottomSheet(
            accounts = accounts,
            currentTimeFilter = filterTimeRange,
            currentAccountId = filterAccountId,
            currentCurrency = filterCurrency,
            currentCategory = filterCategory,
            currentSortOrder = filterSortOrder,
            isArabic = isArabic,
            onDismiss = { showFilterSheet = false },
            onApply = { time, accId, curr, cat, sort ->
                filterTimeRange = time
                filterAccountId = accId
                filterCurrency = curr
                filterCategory = cat
                filterSortOrder = sort
                showFilterSheet = false
            },
            onReset = {
                filterTimeRange = TimeFilter.THIS_MONTH
                filterAccountId = null
                filterCurrency = null
                filterCategory = null
                filterSortOrder = TransactionSortOrder.NEWEST
                showFilterSheet = false
            }
        )
    }

    // 2. SPEED DIAL / FAB MENU SHEET (When clicking FAB inside 'All' tab)
    if (activeActionSheet == "FAB_MENU") {
        ModalBottomSheet(
            onDismissRequest = { activeActionSheet = null },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isArabic) "عملية نقدية جديدة" else "New Cash Transaction",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isArabic) "اختر نوع العملية التي تريد تسجيلها:" else "Choose the type of operation you wish to perform:",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Income Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeActionSheet = "INCOME" },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenIncome.copy(alpha = 0.12f)),
                    border = BorderStroke(0.5.dp, GreenIncome.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GreenIncome),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(if (isArabic) "الدخل (Income)" else "Income", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = GreenIncome))
                            Text(if (isArabic) "تسجيل راتب، مكافأة، إيداع نقدي أو أرباح" else "Record salary, gifts, returns, or cash deposit", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }

                // Expense Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeActionSheet = "EXPENSE" },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = RedExpense.copy(alpha = 0.12f)),
                    border = BorderStroke(0.5.dp, RedExpense.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(RedExpense),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(if (isArabic) "المصروفات (Expense)" else "Expense", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = RedExpense))
                            Text(if (isArabic) "تسجيل المشتريات، وقود، مطاعم، فواتير أو سحب نقدي" else "Record shopping, fuel, dining, bills, or cash withdrawal", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }

                // Transfer Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeActionSheet = "TRANSFER" },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(if (isArabic) "التحويلات (Transfer)" else "Transfer", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                            Text(if (isArabic) "تحويل الأموال بين الحسابات الداخلية أو المستفيدين" else "Transfer funds between internal accounts or beneficiaries", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }
            }
        }
    }

    // 3. ACTION SHEETS FOR ADDING TRANSACTIONS
    if (activeActionSheet == "INCOME") {
        AddIncomeBottomSheet(
            accounts = accounts,
            initialAccountId = filterAccountId,
            onDismiss = { activeActionSheet = null },
            onConfirm = { amount, accountId, category, party, note, currency ->
                viewModel.addIncome(amount, accountId, category, party, note, currency)
            }
        )
    }

    if (activeActionSheet == "EXPENSE") {
        AddExpenseBottomSheet(
            accounts = accounts,
            initialAccountId = filterAccountId,
            onDismiss = { activeActionSheet = null },
            onConfirm = { amount, accountId, category, party, note, currency ->
                viewModel.addExpense(amount, accountId, category, party, note, currency)
            }
        )
    }

    if (activeActionSheet == "TRANSFER") {
        AddTransferBottomSheet(
            accounts = accounts,
            initialAccountId = filterAccountId,
            onDismiss = { activeActionSheet = null },
            onConfirm = { amount, sourceAccountId, destAccountId, note, currency ->
                viewModel.addTransfer(amount, sourceAccountId, destAccountId, note, currency)
            }
        )
    }

    // 4. TRANSACTION DETAIL SHEET
    selectedDetailTransaction?.let { tx ->
        val sourceAcc = accounts.find { it.id == tx.sourceAccountId }
        val destAcc = accounts.find { it.id == tx.destinationAccountId }

        TransactionDetailBottomSheet(
            transaction = tx,
            accountName = sourceAcc?.name ?: (if (isArabic) "الحساب" else "Account"),
            destAccountName = destAcc?.name,
            onDelete = { txId -> viewModel.deleteTransaction(txId) },
            onDismiss = { selectedDetailTransaction = null }
        )
    }
}

// ------------------- TOP SUMMARY CARD PER CURRENCY -------------------
@Composable
fun SummaryTabTotalsCard(
    currency: String,
    selectedTab: TransactionType?,
    timeFilter: TimeFilter,
    transactions: List<Transaction>,
    isArabic: Boolean
) {
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()

    // Calculate sum and count for this currency matching tab and time filter
    val matchingTxs = transactions.filter { tx ->
        if (!tx.currency.equals(currency, ignoreCase = true)) return@filter false

        val isDailyCashTx = tx.type == TransactionType.INCOME ||
                tx.type == TransactionType.EXPENSE ||
                tx.type == TransactionType.TRANSFER ||
                tx.type == TransactionType.BILL_PAYMENT

        if (!isDailyCashTx) return@filter false

        val matchesTab = when (selectedTab) {
            null -> true
            TransactionType.INCOME -> tx.type == TransactionType.INCOME
            TransactionType.EXPENSE -> tx.type == TransactionType.EXPENSE || tx.type == TransactionType.BILL_PAYMENT
            TransactionType.TRANSFER -> tx.type == TransactionType.TRANSFER
            else -> tx.type == selectedTab
        }

        val matchesTime = when (timeFilter) {
            TimeFilter.TODAY -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                tx.date >= calendar.timeInMillis
            }
            TimeFilter.YESTERDAY -> {
                calendar.timeInMillis = now
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfYesterday = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val endOfYesterday = calendar.timeInMillis
                tx.date in startOfYesterday until endOfYesterday
            }
            TimeFilter.LAST_7_DAYS -> tx.date >= (now - 7L * 24 * 3600 * 1000)
            TimeFilter.LAST_30_DAYS -> tx.date >= (now - 30L * 24 * 3600 * 1000)
            TimeFilter.THIS_MONTH -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                tx.date >= calendar.timeInMillis
            }
            TimeFilter.THIS_YEAR -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                tx.date >= calendar.timeInMillis
            }
            TimeFilter.ALL_TIME -> true
        }

        matchesTab && matchesTime
    }

    val totalAmount = matchingTxs.sumOf { tx ->
        if (selectedTab == null && (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.BILL_PAYMENT)) {
            -tx.amount
        } else {
            tx.amount
        }
    }

    val txCount = matchingTxs.size

    val headerTitle = when (selectedTab) {
        TransactionType.INCOME -> if (isArabic) "إجمالي الدخل (TOTAL INCOME)" else "TOTAL INCOME"
        TransactionType.EXPENSE -> if (isArabic) "إجمالي المصروفات (TOTAL EXPENSE)" else "TOTAL EXPENSE"
        TransactionType.TRANSFER -> if (isArabic) "إجمالي التحويلات (TOTAL TRANSFERS)" else "TOTAL TRANSFERS"
        else -> if (isArabic) "إجمالي التدفقات (TOTAL TRANSACTIONS)" else "TOTAL TRANSACTIONS"
    }

    val headerColor = when (selectedTab) {
        TransactionType.INCOME -> GreenIncome
        TransactionType.EXPENSE -> RedExpense
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = headerColor
                    )
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = headerColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = currency,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = headerColor
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${String.format("%.2f", Math.abs(totalAmount))} $currency",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Text(
                    text = "$txCount ${if (isArabic) "عملية" else "Transactions"} • ${if (isArabic) timeFilter.labelAr else timeFilter.labelEn}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

// ------------------- COMPREHENSIVE FILTER BOTTOM SHEET -------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsFilterBottomSheet(
    accounts: List<Account>,
    currentTimeFilter: TimeFilter,
    currentAccountId: String?,
    currentCurrency: String?,
    currentCategory: String?,
    currentSortOrder: TransactionSortOrder,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onApply: (TimeFilter, String?, String?, String?, TransactionSortOrder) -> Unit,
    onReset: () -> Unit
) {
    var selectedTime by remember { mutableStateOf(currentTimeFilter) }
    var selectedAccountId by remember { mutableStateOf(currentAccountId) }
    var selectedCurrency by remember { mutableStateOf(currentCurrency) }
    var selectedCategory by remember { mutableStateOf(currentCategory) }
    var selectedSort by remember { mutableStateOf(currentSortOrder) }

    val currencyOptions = listOf("SAR", "USD", "YER")
    val categoryOptions = listOf("Salary", "Food", "Shopping", "Fuel", "Bills", "Gifts", "Transfers", "Other")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "تصفية العمليات (Filter ▼)" else "Filter Transactions",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onReset) {
                    Text(if (isArabic) "إعادة ضبط" else "Reset", color = MaterialTheme.colorScheme.error)
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 1. TIME PERIOD
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isArabic) "الفترة الزمنية (Time Period)" else "Time Period",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TimeFilter.values()) { time ->
                        FilterChip(
                            selected = selectedTime == time,
                            onClick = { selectedTime = time },
                            label = { Text(if (isArabic) time.labelAr else time.labelEn) },
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                }
            }

            // 2. ACCOUNT
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isArabic) "الحساب المالي (Account)" else "Account",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedAccountId == null,
                            onClick = { selectedAccountId = null },
                            label = { Text(if (isArabic) "جميع الحسابات" else "All Accounts") },
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                    items(accounts) { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name) },
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                }
            }

            // 3. CURRENCY
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isArabic) "العملة (Currency)" else "Currency",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedCurrency == null,
                        onClick = { selectedCurrency = null },
                        label = { Text(if (isArabic) "الكل" else "All") },
                        shape = RoundedCornerShape(18.dp)
                    )
                    currencyOptions.forEach { curr ->
                        FilterChip(
                            selected = selectedCurrency.equals(curr, ignoreCase = true),
                            onClick = { selectedCurrency = curr },
                            label = { Text(curr) },
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                }
            }

            // 4. CATEGORY
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isArabic) "الفئة (Category)" else "Category",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(if (isArabic) "جميع الفئات" else "All Categories") },
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                    items(categoryOptions) { cat ->
                        FilterChip(
                            selected = selectedCategory.equals(cat, ignoreCase = true),
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                }
            }

            // 5. SORT ORDER
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isArabic) "ترتيب النتائج (Sort By)" else "Sort By",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TransactionSortOrder.values()) { sort ->
                        FilterChip(
                            selected = selectedSort == sort,
                            onClick = { selectedSort = sort },
                            label = { Text(if (isArabic) sort.labelAr else sort.labelEn) },
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // APPLY BUTTON
            Button(
                onClick = {
                    onApply(selectedTime, selectedAccountId, selectedCurrency, selectedCategory, selectedSort)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = if (isArabic) "تطبيق الفلاتر (Apply Filters)" else "Apply Filters",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

// ------------------- MODERN TRANSACTION ITEM CARD -------------------
@Composable
fun TransactionModernCard(
    transaction: Transaction,
    sourceAccountName: String,
    destAccountName: String?,
    onClick: () -> Unit
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val isExpense = transaction.type == TransactionType.EXPENSE || transaction.type == TransactionType.BILL_PAYMENT || transaction.type == TransactionType.ASSET_PURCHASE
    val isTransfer = transaction.type == TransactionType.TRANSFER

    val amountColor = when {
        isIncome -> GreenIncome
        isExpense -> RedExpense
        else -> MaterialTheme.colorScheme.primary
    }

    val iconBg = when {
        isIncome -> GreenIncome.copy(alpha = 0.15f)
        isExpense -> RedExpense.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val iconTint = when {
        isIncome -> GreenIncome
        isExpense -> RedExpense
        else -> MaterialTheme.colorScheme.primary
    }

    val icon = when (transaction.type) {
        TransactionType.INCOME -> Icons.Default.ArrowDownward
        TransactionType.EXPENSE -> Icons.Default.ArrowUpward
        TransactionType.TRANSFER -> Icons.Default.SwapHoriz
        TransactionType.BILL_PAYMENT -> Icons.Default.ReceiptLong
        else -> Icons.Default.AttachMoney
    }

    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(transaction.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val subtitleText = when {
                        isTransfer -> "$sourceAccountName ➔ ${destAccountName ?: "Account"}"
                        !transaction.party.isNullOrBlank() -> "${transaction.party} • $sourceAccountName"
                        else -> sourceAccountName
                    }

                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                val prefix = if (isIncome) "+" else if (isExpense) "-" else ""
                Text(
                    text = "$prefix${String.format("%.2f", transaction.amount)} ${transaction.currency}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = amountColor
                    )
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = iconBg,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = transaction.type.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = iconTint
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
