package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.DesignTokens
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PfmsViewModel
import com.example.ui.viewmodel.QuickActionSheetType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: PfmsViewModel,
    onNavigateToAccounts: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToAssets: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val groupedAccounts by viewModel.groupedAccounts.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val netWorthSummary by viewModel.netWorthSummary.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    Scaffold(
        topBar = {
            DashboardHeader(
                userName = "Mohammed Al-Mkhlafi",
                isArabic = isArabic,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Account Carousel
            item {
                AccountCardCarousel(
                    groupedAccounts = groupedAccounts,
                    isArabic = isArabic,
                    onNavigateToAccounts = onNavigateToAccounts,
                    onOpenDeposit = { viewModel.openBottomSheet(QuickActionSheetType.DEPOSIT) }
                )
            }

            // 2. Quick Action Grid
            item {
                QuickActionGrid(
                    isArabic = isArabic,
                    onActionSelected = { viewModel.openBottomSheet(it) }
                )
            }

            // 3. Financial Overview Card
            item {
                FinancialOverviewCard(
                    summary = netWorthSummary,
                    isArabic = isArabic
                )
            }

            // 4. Smart Widgets Section
            item {
                SmartWidgetsSection(
                    debts = debts,
                    goals = goals,
                    budgets = budgets,
                    bills = bills,
                    isArabic = isArabic,
                    onNavigateToDebts = onNavigateToDebts,
                    onNavigateToGoals = onNavigateToGoals,
                    onNavigateToBills = onNavigateToBills
                )
            }

            // 5. Recent Transactions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedAccount != null) {
                            if (isArabic) "الأنشطة الأخيرة (${selectedAccount?.name})" else "RECENT ACTIVITIES (${selectedAccount?.name})"
                        } else {
                            if (isArabic) "جميع الأنشطة الأخيرة" else "ALL RECENT ACTIVITIES"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    TextButton(onClick = onNavigateToTransactions) {
                        Text(if (isArabic) "عرض الكل" else "View All", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            items(filteredTransactions.take(6)) { tx ->
                TransactionListItem(transaction = tx)
            }
        }
    }
}

@Composable
fun DashboardHeader(
    userName: String,
    isArabic: Boolean,
    onNavigateToSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right side: Avatar + Greeting + Name (clickable to Settings)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onNavigateToSettings)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isArabic) "مرحباً بعودتك 👋" else "Welcome back 👋",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                // Left side: Notifications icon only
                IconButton(
                    onClick = { /* Handle notifications */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = if (isArabic) "الإشعارات" else "Notifications",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun SmartWidgetsSection(
    debts: List<Debt>,
    goals: List<Goal>,
    budgets: List<Budget>,
    bills: List<Bill>,
    isArabic: Boolean = false,
    onNavigateToDebts: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToBills: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Debt Summary Widget
        if (debts.isNotEmpty()) {
            val activeDebts = debts.filter { it.status != DebtStatus.COMPLETED }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable(onClick = onNavigateToDebts),
                shape = RoundedCornerShape(DesignTokens.RadiusSmall),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(OrangeDebt.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Handshake, contentDescription = if (isArabic) "الديون" else "Debts", tint = OrangeDebt)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                if (isArabic) "حالة مركز الديون" else "Debt Center Status",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                if (isArabic) "${activeDebts.size} ديون نشطة مسجلة" else "${activeDebts.size} Active Debts Recorded",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Goals Widget
        if (goals.isNotEmpty()) {
            val topGoal = goals.firstOrNull()
            topGoal?.let { goal ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(onClick = onNavigateToGoals),
                    shape = RoundedCornerShape(DesignTokens.RadiusSmall),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Flag, contentDescription = if (isArabic) "هدف" else "Goal", tint = TealAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isArabic) "الهدف: ${goal.title}" else "Goal: ${goal.title}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text("${(goal.progressPercentage * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TealAccent))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { goal.progressPercentage },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = TealAccent,
                            trackColor = TealAccent.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionListItem(transaction: Transaction) {
    val txColor = when (transaction.type) {
        TransactionType.INCOME -> GreenIncome
        TransactionType.EXPENSE -> RedExpense
        TransactionType.TRANSFER -> BlueTransfer
        TransactionType.ASSET_PURCHASE -> PurpleAsset
        else -> OrangeDebt
    }

    val icon = when (transaction.type) {
        TransactionType.INCOME -> Icons.Default.ArrowDownward
        TransactionType.EXPENSE -> Icons.Default.ArrowUpward
        TransactionType.TRANSFER -> Icons.Default.SwapHoriz
        else -> Icons.Default.Receipt
    }

    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.US)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(DesignTokens.RadiusSmall),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(txColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = transaction.type.name, tint = txColor)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.category + if (!transaction.party.isNull_or_empty()) " - ${transaction.party}" else "",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = dateFormat.format(Date(transaction.date)) + if (transaction.note.isNotEmpty()) " • ${transaction.note}" else "",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Text(
                text = (if (transaction.type == TransactionType.INCOME) "+" else "-") + CurrencyFormatter.format(transaction.amount, transaction.currency),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == TransactionType.INCOME) GreenIncome else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (String, AccountType, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Financial Account") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name (e.g. Al Inma Bank)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Opening Balance (SAR)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val bal = balanceText.toDoubleOrNull() ?: 0.0
                if (name.isNotEmpty()) {
                    onSave(name, selectedType, bal, "SAR")
                }
            }) {
                Text("Save Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
