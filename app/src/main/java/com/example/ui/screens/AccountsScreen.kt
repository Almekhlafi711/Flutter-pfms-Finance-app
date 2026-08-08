package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.Account
import com.example.domain.model.AccountType
import com.example.domain.model.GroupedAccount
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.ui.viewmodel.PfmsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountsScreen(
    viewModel: PfmsViewModel,
    onNavigateBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showAddAccountSheet by remember { mutableStateOf(false) }
    var accountGroupToEdit by remember { mutableStateOf<GroupedAccount?>(null) }
    var accountGroupToViewDetails by remember { mutableStateOf<GroupedAccount?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<AccountType?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val activeAccounts = accounts.filter { !it.isArchived }

    // Filter accounts by Account Type first
    val typeAccounts = remember(activeAccounts, selectedTypeFilter) {
        if (selectedTypeFilter == null) activeAccounts
        else activeAccounts.filter { it.type == selectedTypeFilter }
    }

    // Dynamic unique currencies present in current account type selection
    val availableCurrencies = remember(typeAccounts) {
        typeAccounts.map { it.currency.uppercase() }.distinct()
    }

    val pagerState = rememberPagerState(
        pageCount = { availableCurrencies.size.coerceAtLeast(1) }
    )

    // Ensure pager resets to valid page if availableCurrencies change
    LaunchedEffect(availableCurrencies, selectedTypeFilter) {
        if (availableCurrencies.isNotEmpty()) {
            if (pagerState.currentPage >= availableCurrencies.size) {
                pagerState.scrollToPage(0)
            }
        }
    }

    // Selected Currency based on Pager page
    val currentCurrency = availableCurrencies.getOrNull(pagerState.currentPage) ?: ""

    // Accounts matching current Account Type, Currency, and Search Query
    val pageAccounts = remember(typeAccounts, currentCurrency, searchQuery) {
        if (currentCurrency.isBlank()) emptyList()
        else {
            typeAccounts.filter { acc ->
                val matchesCurrency = acc.currency.equals(currentCurrency, ignoreCase = true)
                val matchesSearch = searchQuery.isBlank() ||
                        acc.name.contains(searchQuery, ignoreCase = true) ||
                        acc.currency.contains(searchQuery, ignoreCase = true) ||
                        acc.accountNumber.contains(searchQuery, ignoreCase = true)
                matchesCurrency && matchesSearch
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isArabic) "مركز الحسابات" else "Accounts Center",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isArabic) "${activeAccounts.size} حساب نشط" else "${activeAccounts.size} Active Accounts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportAccountStatementPdf() }) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = if (isArabic) "تصدير كشف حساب PDF" else "Export PDF Statement",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButtonPosition = if (isArabic) FabPosition.Start else FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddAccountSheet = true },
                containerColor = Color(0xFF6B46C1),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isArabic) "إضافة حساب" else "Add Account"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ----------------------------------------------------
            // 🏛️ 1. HEADER CAROUSEL CARD DIRECTLY AT TOP
            // ----------------------------------------------------
            if (availableCurrencies.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) { pageIndex ->
                    val pageCurr = availableCurrencies.getOrNull(pageIndex) ?: ""
                    val currAccounts = typeAccounts.filter { it.currency.equals(pageCurr, ignoreCase = true) }
                    val currTotalBalance = currAccounts.sumOf { it.balance }

                    val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).absoluteValue
                    val cardScale = lerp(
                        start = 0.94f,
                        stop = 1.0f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                    val cardAlpha = lerp(
                        start = 0.82f,
                        stop = 1.0f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )

                    val typeTitle = if (selectedTypeFilter == null) {
                        if (isArabic) "إجمالي الحسابات" else "TOTAL ACCOUNTS"
                    } else {
                        val name = getAccountTypeName(selectedTypeFilter!!, isArabic)
                        if (isArabic) "إجمالي $name" else "TOTAL $name"
                    }

                    // Luxury Dynamic Gradient based on selected account type
                    val gradientBrush = getAccountTypeGradient(selectedTypeFilter)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = cardScale
                                scaleY = cardScale
                                alpha = cardAlpha
                            }
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(26.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(brush = gradientBrush)
                                .padding(22.dp)
                        ) {
                            // Faint subtle ambient background circles for premium finish
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 30.dp, y = (-30).dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getAccountTypeIcon(selectedTypeFilter),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "$typeTitle — $pageCurr",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.95f),
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            text = if (isArabic) "${currAccounts.size} حسابات" else "${currAccounts.size} Accounts",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = CurrencyFormatter.format(currTotalBalance, pageCurr),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        fontSize = 32.sp
                                    )
                                )

                                Text(
                                    text = if (isArabic) "مجمع الأرصدة الفعلية المطابقة للعملة والتصنيف" else "Aggregated balance for selected type & currency",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }

                // ----------------------------------------------------
                // 🔘 2. DOTS INDICATOR DIRECTLY BELOW SUMMARY CARD
                // ----------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(availableCurrencies.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 22.dp else 7.dp,
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(7.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color(0xFF6B46C1) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                                .clickable {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                }
                        )
                    }
                }
            } else {
                // Empty summary card placeholder if no accounts
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isArabic) "لا توجد حسابات نشطة في هذا التصنيف" else "No active accounts in this category",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ----------------------------------------------------
            // 🏷️ 3. CATEGORY FILTER BAR (TAB CHIPS BELOW CAROUSEL)
            // ----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAllSelected = selectedTypeFilter == null
                AccountsCategoryChip(
                    selected = isAllSelected,
                    onClick = {
                        selectedTypeFilter = null
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    },
                    label = if (isArabic) "الكل" else "All"
                )

                AccountType.values().forEach { type ->
                    val isSelected = selectedTypeFilter == type
                    AccountsCategoryChip(
                        selected = isSelected,
                        onClick = {
                            selectedTypeFilter = type
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        label = getAccountTypeName(type, isArabic),
                        icon = getAccountTypeIcon(type)
                    )
                }
            }

            // ----------------------------------------------------
            // 🔍 4. SEARCH BAR
            // ----------------------------------------------------
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isArabic) "بحث في الحسابات..." else "Search accounts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                )
            }

            // ----------------------------------------------------
            // 📋 5. FILTERED ACCOUNTS LIST OR EMPTY STATE
            // ----------------------------------------------------
            if (availableCurrencies.isEmpty() || pageAccounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val typeLabel = if (selectedTypeFilter != null) getAccountTypeName(selectedTypeFilter!!, isArabic) else (if (isArabic) "حسابات" else "Accounts")
                        Text(
                            text = if (availableCurrencies.isEmpty()) {
                                if (isArabic) "لا توجد $typeLabel حالياً" else "No $typeLabel available"
                            } else {
                                if (isArabic) "لا توجد نتائج مطابقة للبحث" else "No accounts found"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddAccountSheet = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isArabic) "إضافة حساب" else "Add Account")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pageAccounts, key = { it.id }) { acc ->
                        val matchingGroup = remember(acc, activeAccounts) {
                            val accs = activeAccounts.filter { it.name == acc.name && it.type == acc.type }
                            GroupedAccount(
                                name = acc.name,
                                type = acc.type,
                                colorHex = acc.colorHex,
                                iconName = acc.iconName,
                                isArchived = acc.isArchived,
                                accounts = accs.ifEmpty { listOf(acc) }
                            )
                        }

                        CleanAccountListItemCard(
                            account = acc,
                            isArabic = isArabic,
                            onClick = {
                                viewModel.selectAccount(acc.id)
                                accountGroupToViewDetails = matchingGroup
                            },
                            onOptionsClick = {
                                viewModel.selectAccount(acc.id)
                                accountGroupToViewDetails = matchingGroup
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                }
            }
        }
    }

    // Account Deletion Confirmation Dialog
    accountToDelete?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text(if (isArabic) "تأكيد حذف الحساب" else "Confirm Account Deletion") },
            text = { Text(if (isArabic) "هل أنت متأكد من حذف الحساب \"${acc.name}\" (${acc.currency})؟ لا يمكن التراجع عن هذا الإجراء." else "Are you sure you want to delete account \"${acc.name}\" (${acc.currency})? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = acc.id
                        accountToDelete = null
                        viewModel.deleteAccount(
                            accountId = targetId,
                            onSuccess = { },
                            onError = { err -> errorMessage = err }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isArabic) "حذف" else "Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { accountToDelete = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Add Account Sheet
    if (showAddAccountSheet) {
        AddAccountBottomSheet(
            isArabic = isArabic,
            onDismiss = { showAddAccountSheet = false },
            onSave = { name, type, currencyBalances, colorHex, iconName, notes ->
                viewModel.addMultiCurrencyAccount(name, type, currencyBalances, colorHex, iconName, notes)
                showAddAccountSheet = false
            }
        )
    }

    // Edit Account Sheet
    accountGroupToEdit?.let { group ->
        EditAccountBottomSheet(
            group = group,
            transactions = transactions,
            isArabic = isArabic,
            onDismiss = { accountGroupToEdit = null },
            onSave = { updatedAccount ->
                viewModel.updateAccount(updatedAccount)
                accountGroupToEdit = null
            }
        )
    }

    // Account Details & Statement Sheet
    accountGroupToViewDetails?.let { group ->
        AccountDetailsStatementSheet(
            group = group,
            transactions = transactions,
            isArabic = isArabic,
            viewModel = viewModel,
            onDismiss = {
                accountGroupToViewDetails = null
                viewModel.selectAccount(null)
            },
            onEdit = {
                accountGroupToViewDetails = null
                accountGroupToEdit = group
            },
            onArchive = {
                group.accounts.forEach { acc ->
                    viewModel.archiveAccount(acc.id)
                }
                accountGroupToViewDetails = null
                viewModel.selectAccount(null)
            }
        )
    }

    errorMessage?.let { err ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(if (isArabic) "تعذر حذف الحساب" else "Cannot Delete Account") },
            text = { Text(if (isArabic) "هذا الحساب مرتبط بمعاملات أو سجلات مالية نشطة. يرجى استخدام خيار (أرشفة الحساب) بدلاً من الحذف لضمان سلامة البيانات." else "This account is linked to active transactions or records. Please use the 'Archive Account' option instead to preserve financial data integrity.") },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text(if (isArabic) "حسناً" else "OK")
                }
            }
        )
    }
}

/**
 * Custom Styled Category Tab Chip with solid primary purple highlight when selected
 */
@Composable
fun AccountsCategoryChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFF6B46C1) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            )
        }
    }
}

/**
 * Clean, uncluttered Account Item Card (removes direct edit/delete icons from list, uses options menu/tap)
 */
@Composable
fun CleanAccountListItemCard(
    account: Account,
    isArabic: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAccountIcon(account.iconName),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = getAccountTypeName(account.type, isArabic) + " • ${account.currency}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.format(account.balance, account.currency),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                IconButton(onClick = onOptionsClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountBottomSheet(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, AccountType, Map<String, Double>, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }
    var notes by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("bank") }

    val availableCurrencies = listOf("SAR", "USD", "YER", "EUR", "GBP", "AED", "KWD")
    val selectedCurrencies = remember { mutableStateMapOf<String, Boolean>("SAR" to true) }
    val openingBalances = remember { mutableStateMapOf<String, String>() }

    var showTypePicker by remember { mutableStateOf(false) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isArabic) "إضافة حساب مالي جديد" else "Add New Financial Account",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (isArabic) "اسم الحساب *" else "Account Name *") },
                placeholder = { Text(if (isArabic) "مثل: بنك الراجحي" else "e.g. Al Rajhi Bank") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            OutlinedCard(
                onClick = { showTypePicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(if (isArabic) "نوع الحساب" else "Account Type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(getAccountTypeName(selectedType, isArabic), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                }
            }

            Text(
                text = if (isArabic) "العملات والأرصدة الافتتاحية" else "Currencies & Opening Balances",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableCurrencies.forEach { curr ->
                    val isChecked = selectedCurrencies[curr] == true
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            selectedCurrencies[curr] = !isChecked
                        },
                        label = { Text(curr, fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6B46C1),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            selectedCurrencies.filter { it.value }.forEach { (curr, _) ->
                var balText by remember(curr) { mutableStateOf(openingBalances[curr] ?: "") }
                OutlinedTextField(
                    value = balText,
                    onValueChange = {
                        balText = it
                        openingBalances[curr] = it
                    },
                    label = { Text(if (isArabic) "الرصيد الافتتاحي ($curr)" else "Opening Balance ($curr)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(if (isArabic) "ملاحظات" else "Notes") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    showSaveConfirmDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B46C1),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF6B46C1).copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                ),
                enabled = name.isNotBlank() && selectedCurrencies.any { it.value }
            ) {
                Text(
                    text = if (isArabic) "حفظ الحساب" else "Save Account",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showTypePicker) {
        ModalBottomSheet(onDismissRequest = { showTypePicker = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (isArabic) "اختر نوع الحساب" else "Select Account Type", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                AccountType.values().forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedType = type
                                showTypePicker = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(getAccountTypeName(type, isArabic), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (selectedType == type) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showSaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmDialog = false },
            title = {
                Text(
                    text = if (isArabic) "تأكيد إنشاء الحساب المالي" else "Confirm Financial Account Creation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                val accName = name.ifBlank { if (isArabic) "الحساب الجديد" else "New Account" }
                Text(
                    text = if (isArabic)
                        "هل أنت متأكد من حفظ الحساب ($accName) بالرصيد الافتتاحي المحدد؟\n\nتنبيه: لا يمكن تغيير أو حذف الرصيد الافتتاحي والعملة بعد الحفظ نهائياً للحفاظ على سلامة السجل المحاسبي."
                    else
                        "Are you sure you want to save account \"$accName\" with the specified opening balance(s)?\n\nWarning: Opening balances and currencies cannot be changed or deleted after creation to maintain financial audit integrity.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val balancesMap = mutableMapOf<String, Double>()
                        selectedCurrencies.filter { it.value }.forEach { (curr, _) ->
                            val bal = openingBalances[curr]?.toDoubleOrNull() ?: 0.0
                            balancesMap[curr] = bal
                        }
                        showSaveConfirmDialog = false
                        if (name.isNotBlank() && balancesMap.isNotEmpty()) {
                            onSave(name, selectedType, balancesMap, "#6B46C1", selectedIcon, notes)
                            Toast.makeText(
                                context,
                                if (isArabic) "تم إنشاء الحساب بنجاح" else "Account created successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B46C1), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isArabic) "تأكيد الحفظ" else "Confirm Save",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSaveConfirmDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isArabic) "تعديل / إلغاء" else "Edit / Cancel",
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountBottomSheet(
    group: GroupedAccount,
    transactions: List<Transaction>,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (Account) -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    val hasTransactions = group.accounts.any { acc -> transactions.any { it.sourceAccountId == acc.id || it.destinationAccountId == acc.id } }

    var selectedAccountIndex by remember { mutableStateOf(0) }
    val currentAccount = group.accounts.getOrNull(selectedAccountIndex) ?: group.accounts.first()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isArabic) "تعديل الحساب" else "Edit Account",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (isArabic) "اسم الحساب" else "Account Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            if (group.accounts.size > 1) {
                Text(
                    text = if (isArabic) "العملة المختارة للتعديل: ${currentAccount.currency}" else "Selected currency to edit: ${currentAccount.currency}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (hasTransactions) {
                Text(
                    text = if (isArabic) "ملاحظة: نوع الحساب والعملة مقفلان لسلامة العمليات المالية لوجود معاملات سابقة." else "Note: Account type and currency are permanently locked for financial integrity because transactions exist.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(currentAccount.copy(name = name))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isArabic) "حفظ التعديلات" else "Save Changes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountDetailsStatementSheet(
    group: GroupedAccount,
    transactions: List<Transaction>,
    isArabic: Boolean,
    viewModel: PfmsViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showExportSuccessDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        pageCount = { group.accounts.size.coerceAtLeast(1) }
    )

    val currentAccount = group.accounts.getOrNull(pagerState.currentPage) ?: group.accounts.first()

    val accountTransactions = remember(transactions, currentAccount) {
        transactions.filter { it.sourceAccountId == currentAccount.id || it.destinationAccountId == currentAccount.id }
    }

    val totalIncome = remember(accountTransactions, currentAccount) {
        accountTransactions
            .filter { it.type == TransactionType.INCOME || (it.type == TransactionType.TRANSFER && it.destinationAccountId == currentAccount.id) }
            .sumOf { it.amount }
    }

    val totalExpense = remember(accountTransactions, currentAccount) {
        accountTransactions
            .filter { it.type == TransactionType.EXPENSE || (it.type == TransactionType.TRANSFER && it.sourceAccountId == currentAccount.id) }
            .sumOf { it.amount }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 🛠️ 1. Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getAccountIcon(group.iconName),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = getAccountTypeName(group.type, isArabic) + " • ${currentAccount.currency}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Export/Print Report Icon
                    IconButton(
                        onClick = {
                            viewModel.exportAccountStatementPdf()
                            showExportSuccessDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = if (isArabic) "طباعة التقرير" else "Print Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Edit Account Icon
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = if (isArabic) "تعديل" else "Edit",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Archive/Deactivate Account Icon
                    IconButton(onClick = onArchive) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = if (isArabic) "تجميد الحساب" else "Archive Account",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // 💳 2. Multi-Currency Dashboard Gradient Header Summary Carousel
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 12.dp
            ) { pageIndex ->
                val acc = group.accounts.getOrNull(pageIndex) ?: group.accounts.first()
                val pageTxs = transactions.filter { it.sourceAccountId == acc.id || it.destinationAccountId == acc.id }
                val pageIncome = pageTxs
                    .filter { it.type == TransactionType.INCOME || (it.type == TransactionType.TRANSFER && it.destinationAccountId == acc.id) }
                    .sumOf { it.amount }
                val pageExpense = pageTxs
                    .filter { it.type == TransactionType.EXPENSE || (it.type == TransactionType.TRANSFER && it.sourceAccountId == acc.id) }
                    .sumOf { it.amount }

                val gradientBrush = getAccountTypeGradient(group.type)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(brush = gradientBrush)
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
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${acc.currency} • ${getAccountTypeName(group.type, isArabic)}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }

                                if (acc.isArchived) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.Red.copy(alpha = 0.35f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isArabic) "غير نشطة" else "Inactive",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = CurrencyFormatter.format(acc.balance, acc.currency),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 30.sp
                                )
                            )

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color.Black.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (isArabic) "إجمالي الوارد (Inflow)" else "Total Inflow",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "+" + CurrencyFormatter.format(pageIncome, acc.currency),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF34D399)
                                            )
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isArabic) "إجمالي الصادر (Outflow)" else "Total Outflow",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "-" + CurrencyFormatter.format(pageExpense, acc.currency),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF87171)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dots Indicator
            if (group.accounts.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(group.accounts.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 22.dp else 7.dp,
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(7.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color(0xFF6B46C1) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                                .clickable {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                }
                        )
                    }
                }
            }

            // 🚫 3. Sub-Currency Freeze / Reactivate Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentAccount.isArchived) {
                                if (isArabic) "عملة ${currentAccount.currency} (غير نشطة)" else "${currentAccount.currency} Currency (Frozen)"
                            } else {
                                if (isArabic) "عملة ${currentAccount.currency} (نشطة)" else "${currentAccount.currency} Currency (Active)"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (currentAccount.isArchived) {
                                if (isArabic) "انقر لإلغاء التجميد وإتاحة العملة للتحويلات" else "Tap to unfreeze and enable for new transactions"
                            } else {
                                if (isArabic) "إيقاف تنشيط هذه العملة فقط في الحساب" else "Freeze this specific currency sub-account"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = {
                            val updated = currentAccount.copy(isArchived = !currentAccount.isArchived)
                            viewModel.updateAccount(updated)
                        }
                    ) {
                        Text(
                            text = if (currentAccount.isArchived) {
                                if (isArabic) "تفعيل العملة" else "Unfreeze"
                            } else {
                                if (isArabic) "تجميد العملة" else "Freeze Currency"
                            },
                            color = if (currentAccount.isArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 📑 4. Account Ledger & Transaction History
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "كشف الحساب (العمليات)" else "Account Statement Ledger",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (isArabic) "${accountTransactions.size} عملية" else "${accountTransactions.size} Transactions",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (accountTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isArabic) "لا توجد معاملات مسجلة لعملة ${currentAccount.currency}" else "No transactions recorded for ${currentAccount.currency}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accountTransactions, key = { it.id }) { tx ->
                        val isPositive = tx.type == TransactionType.INCOME || tx.type == TransactionType.ASSET_SALE || (tx.type == TransactionType.TRANSFER && tx.destinationAccountId == currentAccount.id)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                            border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isPositive) Color(0xFF10B981).copy(alpha = 0.15f)
                                                else Color(0xFFEF4444).copy(alpha = 0.15f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = tx.category.ifEmpty { tx.type.name },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = dateFormat.format(Date(tx.date)) + if (tx.note.isNotEmpty()) " • ${tx.note}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = (if (isPositive) "+" else "-") + CurrencyFormatter.format(tx.amount, tx.currency),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showExportSuccessDialog = false },
            title = { Text(if (isArabic) "تم تصدير كشف الحساب" else "Statement Exported") },
            text = { Text(if (isArabic) "تم إنشاء كشف الحساب بنجاح بصيغة PDF وتجهيزه للطباعة." else "Account statement PDF has been successfully generated.") },
            confirmButton = {
                TextButton(onClick = { showExportSuccessDialog = false }) {
                    Text(if (isArabic) "حسناً" else "OK")
                }
            }
        )
    }
}

fun getAccountTypeName(type: AccountType, isArabic: Boolean): String {
    return when (type) {
        AccountType.BANK -> if (isArabic) "حساب بنكي" else "Bank"
        AccountType.WALLET -> if (isArabic) "محفظة" else "Wallet"
        AccountType.CASH -> if (isArabic) "نقد" else "Cash"
        AccountType.SAVINGS -> if (isArabic) "توفير" else "Savings"
        AccountType.CREDIT_CARD -> if (isArabic) "بطاقة ائتمانية" else "Credit Card"
        AccountType.INVESTMENT -> if (isArabic) "استثمار" else "Investment"
        AccountType.CRYPTO -> if (isArabic) "عملات رقمية" else "Crypto"
    }
}

fun getAccountTypeIcon(type: AccountType?): androidx.compose.ui.graphics.vector.ImageVector {
    if (type == null) return Icons.Default.AccountBalance
    return when (type) {
        AccountType.BANK -> Icons.Default.AccountBalance
        AccountType.WALLET -> Icons.Default.AccountBalanceWallet
        AccountType.CASH -> Icons.Default.Payments
        AccountType.SAVINGS -> Icons.Default.Savings
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
        AccountType.INVESTMENT -> Icons.Default.TrendingUp
        AccountType.CRYPTO -> Icons.Default.CurrencyBitcoin
    }
}

fun getAccountIcon(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName.lowercase()) {
        "wallet" -> Icons.Default.AccountBalanceWallet
        "cash" -> Icons.Default.Payments
        "savings" -> Icons.Default.Savings
        "credit_card" -> Icons.Default.CreditCard
        "investment" -> Icons.Default.TrendingUp
        "crypto" -> Icons.Default.CurrencyBitcoin
        else -> Icons.Default.AccountBalance
    }
}

fun getAccountTypeGradient(type: AccountType?): Brush {
    return when (type) {
        AccountType.BANK -> Brush.linearGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF1E293B))
        )
        AccountType.WALLET -> Brush.linearGradient(
            colors = listOf(Color(0xFF064E3B), Color(0xFF0D9488), Color(0xFF115E59))
        )
        AccountType.SAVINGS -> Brush.linearGradient(
            colors = listOf(Color(0xFF78350F), Color(0xFFD97706), Color(0xFF92400E))
        )
        AccountType.CASH -> Brush.linearGradient(
            colors = listOf(Color(0xFF14532D), Color(0xFF16A34A), Color(0xFF15803D))
        )
        AccountType.CREDIT_CARD -> Brush.linearGradient(
            colors = listOf(Color(0xFF881337), Color(0xFFE11D48), Color(0xFF9F1239))
        )
        AccountType.INVESTMENT -> Brush.linearGradient(
            colors = listOf(Color(0xFF3B0764), Color(0xFF7E22CE), Color(0xFF581C87))
        )
        AccountType.CRYPTO -> Brush.linearGradient(
            colors = listOf(Color(0xFF431407), Color(0xFFEA580C), Color(0xFF9A3412))
        )
        null -> Brush.linearGradient(
            colors = listOf(Color(0xFF311042), Color(0xFF5B21B6), Color(0xFF1E1B4B))
        )
    }
}
