package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.theme.DesignTokens
import com.example.core.util.CurrencyFormatter
import com.example.core.util.DebtPdfReportGenerator
import com.example.domain.model.Account
import com.example.domain.model.DebtLedgerEntry
import com.example.domain.model.DebtType
import com.example.domain.model.Person
import com.example.domain.model.PersonDebtAccount
import com.example.ui.components.*
import com.example.ui.theme.GreenIncome
import com.example.ui.theme.RedExpense
import com.example.ui.viewmodel.PfmsViewModel
import java.text.SimpleDateFormat
import java.util.*

data class TransactionWithRunningBalance(
    val entry: DebtLedgerEntry,
    val runningBalance: Double,
    val dateGroupKey: String
)

enum class LedgerOperationType {
    ADD_DEBT,
    RECEIVE_PAYMENT,
    PAY_DEBT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonProfileScreen(
    initialPerson: Person,
    currency: String = "SAR",
    viewModel: PfmsViewModel,
    isArabic: Boolean = false,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val personAccounts by viewModel.personDebtAccounts.collectAsState()
    val persons by viewModel.persons.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    // Layout direction support
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Find all persons in the current currency ledger for horizontal swiping
    val personsInCurrencyLedger = remember(personAccounts, persons, currency) {
        val matchedPersons = personAccounts
            .filter { it.mainDebt.currency.equals(currency, ignoreCase = true) }
            .map { it.person }
            .distinctBy { it.id }

        if (matchedPersons.none { it.id == initialPerson.id }) {
            listOf(initialPerson) + matchedPersons.filter { it.id != initialPerson.id }
        } else {
            matchedPersons
        }
    }

    val initialPageIndex = remember(personsInCurrencyLedger, initialPerson) {
        personsInCurrencyLedger.indexOfFirst { it.id == initialPerson.id }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { personsInCurrencyLedger.size }
    )

    // Current active swiped person
    val activePersonState = personsInCurrencyLedger.getOrNull(pagerState.currentPage) ?: initialPerson
    val activePerson = persons.find { it.id == activePersonState.id } ?: activePersonState

    // Filter account for active person & currency
    val currentAccount = remember(personAccounts, activePerson, currency) {
        personAccounts.find { it.person.id == activePerson.id && it.mainDebt.currency.equals(currency, ignoreCase = true) }
            ?: personAccounts.find { it.person.id == activePerson.id }
    }

    var showTopMenu by remember { mutableStateOf(false) }

    // Bottom Sheet states
    var showEditPersonSheet by remember { mutableStateOf(false) }
    var showStatementSheet by remember { mutableStateOf(false) }
    var showTypeSelectionSheet by remember { mutableStateOf(false) }
    var activeOperationType by remember { mutableStateOf<LedgerOperationType?>(null) }
    var showSuccessOverlay by remember { mutableStateOf(false) }
    var selectedEntryForEdit by remember { mutableStateOf<DebtLedgerEntry?>(null) }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", if (isArabic) Locale("ar") else Locale.getDefault())
    val dateTimeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", if (isArabic) Locale("ar") else Locale.getDefault())

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val initials = activePerson.name.trim().split(" ")
                                .take(2)
                                .mapNotNull { it.firstOrNull()?.uppercase() }
                                .joinToString("")
                                .ifEmpty { "P" }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activePerson.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (personsInCurrencyLedger.size > 1) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = "${pagerState.currentPage + 1}/${personsInCurrencyLedger.size}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = buildString {
                                        append(if (isArabic) "حساب دفتر الأستاذ" else "Ledger Account")
                                        append(" • ")
                                        append(currency)
                                        if (!activePerson.phone.isNullOrBlank()) {
                                            append(" • ")
                                            append(activePerson.phone)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isArabic) "تعديل بيانات الشخص" else "Edit Person Details") },
                                onClick = {
                                    showTopMenu = false
                                    showEditPersonSheet = true
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (isArabic) "معاينة كشف الحساب" else "Preview Statement") },
                                onClick = {
                                    showTopMenu = false
                                    showStatementSheet = true
                                },
                                leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isArabic) "تصدير كشف PDF" else "Export PDF Statement") },
                                onClick = {
                                    showTopMenu = false
                                    currentAccount?.let { acc ->
                                        val pdfFile = DebtPdfReportGenerator.generatePersonDebtStatementPdf(context, acc)
                                        if (pdfFile != null) {
                                            Toast.makeText(context, if (isArabic) "تم تصدير كشف الحساب PDF: ${pdfFile.name}" else "PDF Statement Exported: ${pdfFile.name}", Toast.LENGTH_LONG).show()
                                        }
                                    } ?: Toast.makeText(context, if (isArabic) "لا توجد بيانات كشف حساب" else "No ledger account data", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            },
            bottomBar = {
                // FIXED ATTRACTIVE BOTTOM ACTION BUTTON FOR ADDING TRANSACTIONS
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding()
                    ) {
                        Button(
                            onClick = { showTypeSelectionSheet = true },
                            enabled = activePerson.isActive,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) "عملية جديدة" else "New Operation",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            // HorizontalPager for Swiping between persons in the same currency ledger
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { pageIndex ->
                val personForPage = personsInCurrencyLedger.getOrNull(pageIndex) ?: activePerson
                val accountForPage = remember(personAccounts, personForPage, currency) {
                    personAccounts.find { it.person.id == personForPage.id && it.mainDebt.currency.equals(currency, ignoreCase = true) }
                        ?: personAccounts.find { it.person.id == personForPage.id }
                }

                val rawEntries = accountForPage?.entries ?: emptyList()
                val sortedEntries = remember(rawEntries) { rawEntries.sortedBy { it.date } }

                var runningAcc = 0.0
                val entriesWithBalance = remember(sortedEntries, isArabic) {
                    sortedEntries.map { entry ->
                        if (entry.isPayment) {
                            runningAcc -= entry.amount
                        } else {
                            runningAcc += entry.amount
                        }
                        val groupKey = getDateGroupKey(entry.date, isArabic)
                        TransactionWithRunningBalance(entry, runningAcc, groupKey)
                    }.reversed()
                }

                val groupedEntries = remember(entriesWithBalance) {
                    entriesWithBalance.groupBy { it.dateGroupKey }
                }

                val currentBalance = accountForPage?.totalRemainingAmount ?: 0.0

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // SECTION 1: Light Lavender Banking Card (Matching Debt Center Image)
                    item {
                        val origAmount = accountForPage?.totalOriginalAmount ?: 0.0
                        val paidAmount = accountForPage?.totalPaidAmount ?: 0.0
                        val txCount = accountForPage?.transactionCount ?: 0
                        val lastTxDate = accountForPage?.lastTransactionDate ?: System.currentTimeMillis()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EFF7)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                ) {
                                    // Header Row: Top Badge Pill & Wallet Title
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50.dp),
                                            color = if (currentBalance >= 0) Color(0xFFE6F4EA) else Color(0xFFFCE8E6)
                                        ) {
                                            Text(
                                                text = if (currentBalance >= 0) {
                                                    if (isArabic) "(+) مستحق له" else "(+) Asset Receivable"
                                                } else {
                                                    if (isArabic) "(-) مستحق عليه" else "(-) Owed Debt"
                                                },
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (currentBalance >= 0) Color(0xFF15803D) else Color(0xFFB91C1C),
                                                    fontSize = 12.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isArabic) "بطاقة الحساب المصرفية" else "Net Debt Balance",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF64748B),
                                                    fontSize = 13.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE9D8FD)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountBalanceWallet,
                                                    contentDescription = null,
                                                    tint = Color(0xFF6B46C1),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Main Balance Section
                                    val formattedBal = CurrencyFormatter.format(kotlin.math.abs(currentBalance), currency) + (if (currentBalance < 0) "-" else "+")
                                    Text(
                                        text = formattedBal,
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 32.sp,
                                            color = if (currentBalance >= 0) Color(0xFF15803D) else Color(0xFFB91C1C)
                                        ),
                                        modifier = Modifier.align(Alignment.End)
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // 3 White Sub-cards Breakdown Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = if (isArabic) "الأصلي" else "Original",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF64748B),
                                                        fontSize = 10.sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = CurrencyFormatter.format(origAmount, currency),
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1E293B),
                                                        fontSize = 12.sp
                                                    )
                                                )
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = if (isArabic) "المسدد" else "Paid",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF64748B),
                                                        fontSize = 10.sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = CurrencyFormatter.format(paidAmount, currency),
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF15803D),
                                                        fontSize = 12.sp
                                                    )
                                                )
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = if (isArabic) "المتبقي" else "Remaining",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF64748B),
                                                        fontSize = 10.sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = CurrencyFormatter.format(currentBalance, currency),
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (currentBalance >= 0) Color(0xFF15803D) else Color(0xFFB91C1C),
                                                        fontSize = 12.sp
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = Color(0xFFE2E8F0))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Footer Metadata Inside Card
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isArabic) "عدد العمليات: $txCount" else "Operations: $txCount",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF64748B),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )

                                        Text(
                                            text = if (isArabic) "آخر عملية: ${dateFormat.format(Date(lastTxDate))}" else "Last Activity: ${dateFormat.format(Date(lastTxDate))}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF64748B),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 2: Transaction History Log Header (كشف الحساب المصرفي)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "سجل العمليات (كشف حساب بنكي)" else "BANKING STATEMENT LOG",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }

                            Text(
                                text = "${entriesWithBalance.size} ${if (isArabic) "حركة" else "entries"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // SECTION 3: Transaction Items Grouped by Date
                    if (groupedEntries.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(DesignTokens.RadiusMedium),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (isArabic) "لا توجد عمليات مسبقة في دفتر الحساب المالي." else "No transaction history recorded in this ledger yet.",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        groupedEntries.forEach { (dateGroup, itemsInGroup) ->
                            // Date Period Header
                            item(key = "group_header_${personForPage.id}_$dateGroup") {
                                Text(
                                    text = dateGroup,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                )
                            }

                            items(itemsInGroup, key = { it.entry.id }) { itemWithBal ->
                                val entry = itemWithBal.entry
                                val balanceAfter = itemWithBal.runningBalance

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clickable { selectedEntryForEdit = entry },
                                    shape = RoundedCornerShape(DesignTokens.RadiusMedium),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (entry.type == DebtType.RECEIVABLE) GreenIncome.copy(alpha = 0.15f)
                                                    else RedExpense.copy(alpha = 0.15f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (entry.type == DebtType.RECEIVABLE) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (entry.type == DebtType.RECEIVABLE) GreenIncome else RedExpense,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (entry.type == DebtType.RECEIVABLE) {
                                                        if (isArabic) "إضافة دين (له)" else "Receivable Entry"
                                                    } else {
                                                        if (isArabic) "تسجيل التزام (عليه)" else "Payable Entry"
                                                    },
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                )

                                                Spacer(modifier = Modifier.width(6.dp))

                                                // Badge له / عليه
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (entry.type == DebtType.RECEIVABLE) GreenIncome.copy(alpha = 0.15f) else RedExpense.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (entry.type == DebtType.RECEIVABLE) {
                                                            if (isArabic) "له" else "Asset"
                                                        } else {
                                                            if (isArabic) "عليه" else "Liability"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp,
                                                            color = if (entry.type == DebtType.RECEIVABLE) GreenIncome else RedExpense
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            if (entry.description.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = entry.description,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 12.sp
                                                    )
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = dateTimeFormat.format(Date(entry.date)),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.outline,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = (if (entry.type == DebtType.RECEIVABLE) "+" else "-") + CurrencyFormatter.format(entry.amount, currency),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    color = if (entry.type == DebtType.RECEIVABLE) GreenIncome else RedExpense
                                                )
                                            )

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                text = "${if (isArabic) "الرصيد بعدها: " else "Bal: "}${CurrencyFormatter.format(balanceAfter, currency)}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
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

        // 1. Edit Person Bottom Sheet
        if (showEditPersonSheet) {
            EditPersonBottomSheet(
                person = activePerson,
                isArabic = isArabic,
                onDismiss = { showEditPersonSheet = false },
                onSave = { updated ->
                    viewModel.updatePerson(updated)
                    showEditPersonSheet = false
                }
            )
        }

        // 2. Statement Options Bottom Sheet
        if (showStatementSheet && currentAccount != null) {
            DebtStatementBottomSheet(
                account = currentAccount,
                isArabic = isArabic,
                onDismiss = { showStatementSheet = false }
            )
        }

        // 3. Edit Transaction Bottom Sheet
        if (selectedEntryForEdit != null) {
            EditLedgerTransactionBottomSheet(
                entry = selectedEntryForEdit!!,
                personName = activePerson.name,
                currency = currency,
                isArabic = isArabic,
                onDismiss = { selectedEntryForEdit = null },
                onConfirmSave = { entryId, amt, desc, type ->
                    viewModel.updateDebtLedgerEntry(entryId, amt, desc, type)
                    selectedEntryForEdit = null
                }
            )
        }

        // 4. Unified Debt Operation Flow
        if (showTypeSelectionSheet) {
            DebtOperationFlow(
                persons = persons,
                accounts = accounts,
                personDebtAccounts = personAccounts,
                isArabic = isArabic,
                initialPersonId = activePerson.id,
                initialCurrency = currency,
                onDismiss = { showTypeSelectionSheet = false },
                onExecuteOperation = { person, curr, opType, dir, amt, accId, notes ->
                    showTypeSelectionSheet = false
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

        // 6. Success Message Overlay Dialog
        if (showSuccessOverlay) {
            SuccessOverlayDialog(
                isArabic = isArabic,
                onDismiss = { showSuccessOverlay = false }
            )
        }
    }
}

private fun getDateGroupKey(timestamp: Long, isArabic: Boolean): String {
    val cal = Calendar.getInstance()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterdayStart = todayStart - 24 * 3600 * 1000L
    val weekStart = todayStart - 6 * 24 * 3600 * 1000L

    return when {
        timestamp >= todayStart -> if (isArabic) "اليوم" else "Today"
        timestamp >= yesterdayStart -> if (isArabic) "أمس" else "Yesterday"
        timestamp >= weekStart -> if (isArabic) "هذا الأسبوع" else "This Week"
        else -> {
            val sdf = SimpleDateFormat("yyyy/MM/dd", if (isArabic) Locale("ar") else Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLedgerTransactionBottomSheet(
    entry: DebtLedgerEntry,
    personName: String,
    currency: String,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirmSave: (entryId: String, amount: Double, description: String, type: DebtType) -> Unit
) {
    var amountText by remember { mutableStateOf(entry.amount.toString()) }
    var description by remember { mutableStateOf(entry.description) }
    var selectedType by remember { mutableStateOf(entry.type) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = DesignTokens.RadiusLarge, topEnd = DesignTokens.RadiusLarge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "تعديل العملية المالية" else "Edit Ledger Transaction",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider()

            // Locked Person & Currency Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isArabic) "الشخص" else "Person",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = personName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isArabic) "العملة" else "Currency",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = currency,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Direction Choice: له vs عليه (World Class Design Selector)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Receivable Card (له)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedType = DebtType.RECEIVABLE },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedType == DebtType.RECEIVABLE) GreenIncome.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = if (selectedType == DebtType.RECEIVABLE) androidx.compose.foundation.BorderStroke(2.dp, GreenIncome) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GreenIncome),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isArabic) "له (مستحق)" else "Receivable",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = if (selectedType == DebtType.RECEIVABLE) GreenIncome else MaterialTheme.colorScheme.onSurface)
                            )
                            Text(
                                text = if (isArabic) "رصيد لك" else "Asset",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            )
                        }
                    }
                }

                // Payable Card (عليه)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedType = DebtType.PAYABLE },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedType == DebtType.PAYABLE) RedExpense.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = if (selectedType == DebtType.PAYABLE) androidx.compose.foundation.BorderStroke(2.dp, RedExpense) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RedExpense),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isArabic) "عليه (التزام)" else "Payable",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = if (selectedType == DebtType.PAYABLE) RedExpense else MaterialTheme.colorScheme.onSurface)
                            )
                            Text(
                                text = if (isArabic) "دين عليك" else "Liability",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            )
                        }
                    }
                }
            }

            // Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(if (isArabic) "المبلغ *" else "Amount *") },
                suffix = { Text(currency, fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.RadiusMedium),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (isArabic) "وصف العملية" else "Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.RadiusMedium),
                minLines = 2
            )

            Button(
                onClick = { showConfirmationDialog = true },
                enabled = parsedAmount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(DesignTokens.RadiusMedium)
            ) {
                Text(
                    text = if (isArabic) "حفظ التغييرات" else "Save Changes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // Confirmation Modal in Center of Screen for Edit
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isArabic) "تأكيد تعديل العملية" else "Confirm Transaction Edit",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "الشخص:" else "Person:", style = MaterialTheme.typography.bodySmall)
                            Text(personName, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "العملة:" else "Currency:", style = MaterialTheme.typography.bodySmall)
                            Text(currency, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "الحالة:" else "Direction:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = if (selectedType == DebtType.RECEIVABLE) (if (isArabic) "له (مستحق)" else "Receivable") else (if (isArabic) "عليه (التزام)" else "Payable"),
                                fontWeight = FontWeight.Bold,
                                color = if (selectedType == DebtType.RECEIVABLE) GreenIncome else RedExpense
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "المبلغ الجديد:" else "New Amount:", style = MaterialTheme.typography.bodySmall)
                            Text(CurrencyFormatter.format(parsedAmount, currency), fontWeight = FontWeight.ExtraBold)
                        }
                        if (description.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isArabic) "الوصف:" else "Notes:", style = MaterialTheme.typography.bodySmall)
                                Text(description, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        onConfirmSave(entry.id, parsedAmount, description, selectedType)
                    }
                ) {
                    Text(if (isArabic) "تأكيد التعديل" else "Confirm Edit")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmationDialog = false }) {
                    Text(if (isArabic) "إلغاء التعديل" else "Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSingleLedgerTransactionBottomSheet(
    person: Person,
    currency: String,
    accounts: List<Account>,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirmTransaction: (type: DebtType, amount: Double, accountId: String, notes: String) -> Unit
) {
    var operationType by remember { mutableStateOf(DebtType.RECEIVABLE) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val selectedAccount = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = DesignTokens.RadiusLarge, topEnd = DesignTokens.RadiusLarge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "إضافة دين / عملية جديدة" else "Add New Debt / Operation",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider()

            // Locked Person & Currency Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isArabic) "حساب الشخص" else "Person Ledger",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = person.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isArabic) "العملة الحالية" else "Active Currency",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = currency,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // Operation Type Selection (له vs عليه - Eye-catching Design with Arrows)
            Column {
                Text(
                    text = if (isArabic) "تحديد اتجاه العملية (له أم عليه) *" else "Select Operation Type *",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1: له (Receivable - Green)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { operationType = DebtType.RECEIVABLE },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (operationType == DebtType.RECEIVABLE) GreenIncome.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (operationType == DebtType.RECEIVABLE) androidx.compose.foundation.BorderStroke(2.dp, GreenIncome) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GreenIncome),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "له (مستحق)" else "Receivable",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (operationType == DebtType.RECEIVABLE) GreenIncome else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (isArabic) "دين لك" else "You lend",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                )
                            }
                        }
                    }

                    // Option 2: عليه (Payable - Red)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { operationType = DebtType.PAYABLE },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (operationType == DebtType.PAYABLE) RedExpense.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (operationType == DebtType.PAYABLE) androidx.compose.foundation.BorderStroke(2.dp, RedExpense) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RedExpense),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "عليه (التزام)" else "Payable",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (operationType == DebtType.PAYABLE) RedExpense else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (isArabic) "دين عليك" else "You borrow",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                )
                            }
                        }
                    }
                }
            }

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(if (isArabic) "المبلغ *" else "Amount *") },
                suffix = { Text(currency, fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.RadiusMedium),
                singleLine = true
            )

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (isArabic) "البيان / وصف العملية" else "Notes / Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.RadiusMedium),
                minLines = 2
            )

            // Account / Wallet Card Selector (خيار حساب البنك أو المحفظة)
            if (accounts.isNotEmpty()) {
                Column {
                    Text(
                        text = if (isArabic) "حساب البنك / المحفظة المرتبطة *" else "Bank Account / Wallet *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.take(3).forEach { acc ->
                            val isSelected = selectedAccountId == acc.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAccountId = acc.id },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(acc.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(
                                                text = "${if (isArabic) "الرصيد: " else "Bal: "}${CurrencyFormatter.format(acc.balance, acc.currency)}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { showConfirmationDialog = true },
                enabled = parsedAmount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isArabic) "تأكيد واستمرار" else "Confirm & Proceed",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // Confirmation Screen / Dialog in the CENTER of the screen (رسالة تأكيد بوسط الشاشة)
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (operationType == DebtType.RECEIVABLE) GreenIncome else RedExpense,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isArabic) "تأكيد إضافة العملية المالية" else "Confirm New Transaction",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "الشخص:" else "Person:", style = MaterialTheme.typography.bodySmall)
                            Text(person.name, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "العملة:" else "Currency:", style = MaterialTheme.typography.bodySmall)
                            Text(currency, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "نوع العملية:" else "Type:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = if (operationType == DebtType.RECEIVABLE) (if (isArabic) "إضافة دين (له)" else "Add Debt") else (if (isArabic) "تسجيل التزام (عليه)" else "Record Debt"),
                                fontWeight = FontWeight.Bold,
                                color = if (operationType == DebtType.RECEIVABLE) GreenIncome else RedExpense
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "المبلغ:" else "Amount:", style = MaterialTheme.typography.bodySmall)
                            Text(CurrencyFormatter.format(parsedAmount, currency), fontWeight = FontWeight.ExtraBold)
                        }
                        if (selectedAccount != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isArabic) "الحساب المرتبط:" else "Linked Vault:", style = MaterialTheme.typography.bodySmall)
                                Text(selectedAccount.name, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (description.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isArabic) "البيان:" else "Notes:", style = MaterialTheme.typography.bodySmall)
                                Text(description, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        onConfirmTransaction(operationType, parsedAmount, selectedAccountId, description)
                    }
                ) {
                    Text(if (isArabic) "تأكيد العملية" else "Confirm Transaction")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmationDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectOperationTypeBottomSheet(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSelectOperation: (LedgerOperationType) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isArabic) "اختر نوع العملية" else "Select Operation Type",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 1. إضافة دين
                OperationTypeCard(
                    title = if (isArabic) "إضافة دين" else "Add Debt",
                    description = if (isArabic) "إضافة مبلغ جديد إلى دفتر الأستاذ الحالي." else "Add a new amount to the current ledger.",
                    icon = Icons.Default.Add,
                    iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { onSelectOperation(LedgerOperationType.ADD_DEBT) }
                )
                
                // 2. استلام مبلغ
                OperationTypeCard(
                    title = if (isArabic) "استلام مبلغ" else "Receive Amount",
                    description = if (isArabic) "تسجيل مبلغ تم استلامه من هذا الشخص." else "Record an amount received from this person.",
                    icon = Icons.Default.ArrowDownward,
                    iconBgColor = GreenIncome.copy(alpha = 0.15f),
                    iconColor = GreenIncome,
                    onClick = { onSelectOperation(LedgerOperationType.RECEIVE_PAYMENT) }
                )
                
                // 3. سداد مبلغ
                OperationTypeCard(
                    title = if (isArabic) "سداد مبلغ" else "Pay Amount",
                    description = if (isArabic) "تسجيل مبلغ تم دفعه لهذا الشخص." else "Record an amount paid to this person.",
                    icon = Icons.Default.ArrowUpward,
                    iconBgColor = RedExpense.copy(alpha = 0.15f),
                    iconColor = RedExpense,
                    onClick = { onSelectOperation(LedgerOperationType.PAY_DEBT) }
                )
            }
        }
    }
}

@Composable
fun OperationTypeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerOperationBottomSheet(
    operationType: LedgerOperationType,
    person: Person,
    currency: String,
    accounts: List<Account>,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onExecuteOperation: (LedgerOperationType, DebtType?, Double, String, String) -> Unit
) {
    var selectedDirection by remember { mutableStateOf<DebtType?>(null) }
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var showDirectionError by remember { mutableStateOf(false) }

    // Filter bank accounts by currency of the current ledger!
    val filteredAccounts = remember(accounts, currency) {
        accounts.filter { it.currency.equals(currency, ignoreCase = true) }
    }

    var selectedAccountId by remember(filteredAccounts) {
        mutableStateOf(filteredAccounts.firstOrNull()?.id ?: "")
    }

    var showConfirmationDialog by remember { mutableStateOf(false) }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val selectedAccount = filteredAccounts.find { it.id == selectedAccountId }

    // Validation
    val isAmountAndAccountValid = parsedAmount > 0 && selectedAccountId.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (operationType) {
                            LedgerOperationType.ADD_DEBT -> if (isArabic) "إضافة دين" else "Add Debt"
                            LedgerOperationType.RECEIVE_PAYMENT -> if (isArabic) "استلام مبلغ" else "Receive Amount"
                            LedgerOperationType.PAY_DEBT -> if (isArabic) "سداد مبلغ" else "Pay Amount"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${person.name} • $currency",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Only for ADD_DEBT: direction (له vs عليه)
            if (operationType == LedgerOperationType.ADD_DEBT) {
                Column {
                    Text(
                        text = if (isArabic) "اتجاه العملية *" else "Operation Direction *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // له (RECEIVABLE - Green)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedDirection = DebtType.RECEIVABLE
                                    showDirectionError = false
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedDirection == DebtType.RECEIVABLE) GreenIncome.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = if (selectedDirection == DebtType.RECEIVABLE) androidx.compose.foundation.BorderStroke(2.dp, GreenIncome) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GreenIncome),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) "له (مستحق لك)" else "Lend",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedDirection == DebtType.RECEIVABLE) GreenIncome else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = if (isArabic) "إضافة مبلغ مستحق لك على الشخص." else "You lend to them",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }

                        // عليه (PAYABLE - Red)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedDirection = DebtType.PAYABLE
                                    showDirectionError = false
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedDirection == DebtType.PAYABLE) RedExpense.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = if (selectedDirection == DebtType.PAYABLE) androidx.compose.foundation.BorderStroke(2.dp, RedExpense) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(RedExpense),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) "عليه (التزام عليك)" else "Borrow",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedDirection == DebtType.PAYABLE) RedExpense else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = if (isArabic) "إضافة مبلغ مستحق للشخص عليك." else "You borrow from them",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (showDirectionError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isArabic) "حدد اتجاه العملية: له أو عليه" else "Select operation direction: Lend or Borrow",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // Amount input field
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(if (isArabic) "المبلغ *" else "Amount *") },
                suffix = { Text(currency, fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Notes / Description
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text(if (isArabic) "وصف العملية (اختياري)" else "Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Bank Account / Wallet Selector - Only display accounts of same currency
            if (filteredAccounts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) "الحساب البنكي أو المحفظة المرتبطة *" else "Linked Bank Account / Wallet *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var isDropdownExpanded by remember { mutableStateOf(false) }

                    if (!isDropdownExpanded) {
                        // Closed / collapsed state: shows only selected account
                        selectedAccount?.let { acc ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isDropdownExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = acc.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "${if (isArabic) "الرصيد: " else "Balance: "}${CurrencyFormatter.format(acc.balance, acc.currency)}",
                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = GreenIncome,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Expand",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Open / expanded state: list all filtered accounts
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                filteredAccounts.forEach { acc ->
                                    val isSelected = selectedAccountId == acc.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                selectedAccountId = acc.id
                                                isDropdownExpanded = false
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = acc.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                                Text(
                                                    text = "${if (isArabic) "الرصيد: " else "Balance: "}${CurrencyFormatter.format(acc.balance, acc.currency)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main CTA
            Button(
                onClick = {
                    if (operationType == LedgerOperationType.ADD_DEBT && selectedDirection == null) {
                        showDirectionError = true
                    } else {
                        showConfirmationDialog = true
                    }
                },
                enabled = isAmountAndAccountValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isArabic) "إضافة العملية" else "Add Operation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // Centered Confirmation Dialog
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isArabic) "تأكيد العملية" else "Confirm Operation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Person
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "الشخص:" else "Person:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(person.name, fontWeight = FontWeight.Bold)
                        }
                        // Currency
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "العملة:" else "Currency:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currency, fontWeight = FontWeight.Bold)
                        }
                        // Type of operation
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "نوع العملية:" else "Operation:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val typeStr = when (operationType) {
                                LedgerOperationType.ADD_DEBT -> if (isArabic) "إضافة دين" else "Add Debt"
                                LedgerOperationType.RECEIVE_PAYMENT -> if (isArabic) "استلام مبلغ" else "Receive Amount"
                                LedgerOperationType.PAY_DEBT -> if (isArabic) "سداد مبلغ" else "Pay Amount"
                            }
                            Text(typeStr, fontWeight = FontWeight.Bold)
                        }
                        // Direction (only for Add Debt)
                        if (operationType == LedgerOperationType.ADD_DEBT && selectedDirection != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isArabic) "الاتجاه:" else "Direction:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val dirStr = if (selectedDirection == DebtType.RECEIVABLE) (if (isArabic) "له (مستحق لك)" else "Lend (Receivable)") else (if (isArabic) "عليه (التزام عليك)" else "Borrow (Payable)")
                                Text(
                                    text = dirStr,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedDirection == DebtType.RECEIVABLE) GreenIncome else RedExpense
                                )
                            }
                        }
                        // Amount
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "المبلغ:" else "Amount:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyFormatter.format(parsedAmount, currency), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        // Account
                        if (selectedAccount != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isArabic) "الحساب المالي:" else "Linked Vault:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(selectedAccount.name, fontWeight = FontWeight.Bold)
                            }
                        }
                        // Description
                        if (notesText.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isArabic) "الوصف:" else "Description:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(notesText, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        onExecuteOperation(operationType, selectedDirection, parsedAmount, selectedAccountId, notesText)
                    }
                ) {
                    Text(if (isArabic) "تأكيد" else "Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmationDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun SuccessOverlayDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(800) // Auto dismiss after 0.8 seconds
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .width(260.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(GreenIncome.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = GreenIncome,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isArabic) "تم تسجيل العملية بنجاح" else "Operation recorded successfully",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
