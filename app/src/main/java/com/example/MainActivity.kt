package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.model.DebtType
import com.example.ui.components.DebtOperationFlow
import com.example.ui.components.DepositBottomSheet
import com.example.ui.components.QuickActionBottomSheet
import com.example.ui.components.QuickAddMasterBottomSheet
import com.example.ui.screens.*
import com.example.ui.theme.PfmsTheme
import com.example.ui.viewmodel.PfmsViewModel
import com.example.ui.viewmodel.QuickActionSheetType

enum class NavigationScreen(val titleEn: String, val titleAr: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", "الرئيسية", Icons.Default.Home),
    ACCOUNTS("Accounts", "الحسابات", Icons.Default.AccountBalance),
    TRANSACTIONS("Transactions", "العمليات", Icons.Default.ReceiptLong),
    DEBTS("Debts", "الديون", Icons.Default.Handshake),
    ASSETS("Assets", "الأصول", Icons.Default.HomeWork),
    BUDGETS("Goals", "الأهداف", Icons.Default.Flag),
    BILLS("Bills", "الفواتير", Icons.Default.Receipt),
    ANALYTICS("Analytics", "التقارير", Icons.Default.BarChart),
    SETTINGS("Settings", "الإعدادات", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: PfmsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PfmsTheme {
                MainAppStructure(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppStructure(viewModel: PfmsViewModel) {
    var currentScreen by remember { mutableStateOf(NavigationScreen.DASHBOARD) }

    val activeBottomSheet by viewModel.activeBottomSheet.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val groupedAccounts by viewModel.groupedAccounts.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    val toastMsg by viewModel.toastMessage.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Strictly primary financial modules (Settings is moved to Dashboard top bar)
                listOf(
                    NavigationScreen.DASHBOARD,
                    NavigationScreen.ACCOUNTS,
                    NavigationScreen.TRANSACTIONS,
                    NavigationScreen.DEBTS,
                    NavigationScreen.ASSETS,
                    NavigationScreen.ANALYTICS
                ).forEach { screen ->
                    val label = if (isArabic) screen.titleAr else screen.titleEn
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = { Icon(screen.icon, contentDescription = label) },
                        label = { Text(label, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                NavigationScreen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAccounts = { currentScreen = NavigationScreen.ACCOUNTS },
                    onNavigateToTransactions = { currentScreen = NavigationScreen.TRANSACTIONS },
                    onNavigateToDebts = { currentScreen = NavigationScreen.DEBTS },
                    onNavigateToAssets = { currentScreen = NavigationScreen.ASSETS },
                    onNavigateToGoals = { currentScreen = NavigationScreen.BUDGETS },
                    onNavigateToBills = { currentScreen = NavigationScreen.BILLS },
                    onNavigateToAnalytics = { currentScreen = NavigationScreen.ANALYTICS },
                    onNavigateToSettings = { currentScreen = NavigationScreen.SETTINGS }
                )
                NavigationScreen.ACCOUNTS -> AccountsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = NavigationScreen.DASHBOARD }
                )
                NavigationScreen.TRANSACTIONS -> TransactionsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = NavigationScreen.DASHBOARD }
                )
                NavigationScreen.DEBTS -> DebtCenterScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = NavigationScreen.DASHBOARD }
                )
                NavigationScreen.ASSETS -> AssetsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = NavigationScreen.DASHBOARD }
                )
                NavigationScreen.BUDGETS -> BudgetsAndGoalsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = NavigationScreen.DASHBOARD }
                )
                NavigationScreen.BILLS -> BillsAndSubscriptionsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = NavigationScreen.DASHBOARD }
                )
                NavigationScreen.ANALYTICS -> AnalyticsAndReportsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = NavigationScreen.DASHBOARD }
                )
                NavigationScreen.SETTINGS -> SettingsAndSecurityScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = NavigationScreen.DASHBOARD }
                )
            }
        }
    }

    // Render Quick Action Bottom Sheet when active
    activeBottomSheet?.let { sheetType ->
        when (sheetType) {
            QuickActionSheetType.DEPOSIT -> {
                DepositBottomSheet(
                    accounts = accounts,
                    groupedAccounts = groupedAccounts,
                    isArabic = isArabic,
                    onDismiss = { viewModel.closeBottomSheet() },
                    onAddDeposit = { amt: Double, acc: String, cat: String, nte: String, curr: String ->
                        viewModel.addDeposit(amt, acc, cat, nte, curr)
                    }
                )
            }
            QuickActionSheetType.QUICK_ADD, QuickActionSheetType.INCOME, QuickActionSheetType.EXPENSE, QuickActionSheetType.TRANSFER -> {
                QuickAddMasterBottomSheet(
                    accounts = accounts,
                    groupedAccounts = groupedAccounts,
                    isArabic = isArabic,
                    initialType = when (sheetType) {
                        QuickActionSheetType.INCOME -> 0
                        QuickActionSheetType.EXPENSE -> 1
                        QuickActionSheetType.TRANSFER -> 2
                        else -> 1
                    },
                    onDismiss = { viewModel.closeBottomSheet() },
                    onAddIncome = { amt, acc, cat, pty, nte, curr -> viewModel.addIncome(amt, acc, cat, pty, nte, curr) },
                    onAddExpense = { amt, acc, cat, pty, nte, curr -> viewModel.addExpense(amt, acc, cat, pty, nte, curr) },
                    onAddTransfer = { amt, src, dst, nte, curr -> viewModel.addTransfer(amt, src, dst, nte, curr) }
                )
            }
            QuickActionSheetType.DEBT -> {
                val persons by viewModel.persons.collectAsState()
                val personAccounts by viewModel.personDebtAccounts.collectAsState()

                DebtOperationFlow(
                    persons = persons,
                    accounts = accounts,
                    personDebtAccounts = personAccounts,
                    isArabic = isArabic,
                    onDismiss = { viewModel.closeBottomSheet() },
                    onExecuteOperation = { person, curr, opType, dir, amt, accId, notes ->
                        viewModel.closeBottomSheet()
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
            QuickActionSheetType.ASSET -> {
                AddAssetBottomSheet(
                    viewModel = viewModel,
                    accounts = accounts,
                    isArabic = isArabic,
                    onDismiss = { viewModel.closeBottomSheet() }
                )
            }
            else -> {
                QuickActionBottomSheet(
                    sheetType = sheetType,
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    onDismiss = { viewModel.closeBottomSheet() },
                    onAddIncome = { amt, acc, cat, pty, nte -> viewModel.addIncome(amt, acc, cat, pty, nte) },
                    onAddExpense = { amt, acc, cat, pty, nte -> viewModel.addExpense(amt, acc, cat, pty, nte) },
                    onAddTransfer = { amt, src, dst, nte -> viewModel.addTransfer(amt, src, dst, nte) },
                    onAddAsset = { nm, typ, pval, cval, acc -> viewModel.addAsset(nm, typ, pval, cval, acc) },
                    onAddDebt = { pty, ph, typ, amt, acc, nte -> viewModel.addDebt(pty, ph, typ, amt, acc, nte) },
                    onAddGoal = { ttl, tgt, init -> viewModel.addGoal(ttl, tgt, init) },
                    onAddBudget = { cat, lmt -> viewModel.addBudget(cat, lmt) },
                    onAddBill = { ttl, amt, cat, acc -> viewModel.addBill(ttl, amt, cat, acc) },
                    onExportPdf = { viewModel.exportAccountStatementPdf() }
                )
            }
        }
    }
}
}
