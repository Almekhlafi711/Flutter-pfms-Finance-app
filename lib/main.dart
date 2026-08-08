import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'core/theme/app_colors.dart';
import 'core/theme/app_theme.dart';
import 'domain/models/debt_models.dart';
import 'domain/models/domain_models.dart';
import 'ui/components/debt_operation_flow.dart';
import 'ui/components/deposit_bottom_sheet.dart';
import 'ui/components/quick_action_bottom_sheets.dart';
import 'ui/components/quick_add_master_bottom_sheet.dart';
import 'ui/screens/add_asset_bottom_sheet.dart';
import 'ui/screens/analytics_and_reports_screen.dart';
import 'ui/screens/assets_screen.dart';
import 'ui/screens/bills_and_subscriptions_screen.dart';
import 'ui/screens/budgets_and_goals_screen.dart';
import 'ui/screens/accounts_screen.dart';
import 'ui/screens/dashboard_screen.dart';
import 'ui/screens/debt_center_screen.dart';
import 'ui/screens/settings_and_security_screen.dart';
import 'ui/screens/transactions_screen.dart';
import 'ui/viewmodel/pfms_provider.dart';

void main() {
  runApp(
    const ProviderScope(
      child: PfmsApp(),
    ),
  );
}

class PfmsApp extends ConsumerWidget {
  const PfmsApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isArabic = ref.watch(isArabicProvider);

    return MaterialApp(
      title: 'PFMS Finance',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: ThemeMode.dark,
      locale: isArabic ? const Locale('ar') : const Locale('en'),
      supportedLocales: const [
        Locale('en', ''),
        Locale('ar', ''),
      ],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: const MainAppStructure(),
    );
  }
}

class MainAppStructure extends ConsumerStatefulWidget {
  const MainAppStructure({super.key});

  @override
  ConsumerState<MainAppStructure> createState() => _MainAppStructureState();
}

class _MainAppStructureState extends ConsumerState<MainAppStructure> {
  int _currentTabIndex = 0;

  @override
  Widget build(BuildContext context) {
    final controller = PfmsController(ref);
    final activeSheet = ref.watch(activeBottomSheetProvider);
    final isArabic = ref.watch(isArabicProvider);
    final accounts = ref.watch(accountsStreamProvider).value ?? [];
    final groupedAccounts = ref.watch(groupedAccountsProvider);
    final toastMsg = ref.watch(toastMessageProvider);

    if (toastMsg != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(toastMsg),
            backgroundColor: AppColors.slateDarkCard,
            duration: const Duration(seconds: 2),
          ),
        );
        controller.clearToast();
      });
    }

    final screens = [
      DashboardScreen(
        onNavigateToAccounts: () => setState(() => _currentTabIndex = 1),
        onNavigateToTransactions: () => setState(() => _currentTabIndex = 2),
        onNavigateToDebts: () => setState(() => _currentTabIndex = 3),
        onNavigateToAssets: () => setState(() => _currentTabIndex = 4),
        onNavigateToGoals: () => Navigator.push(context, MaterialPageRoute(builder: (_) => BudgetsAndGoalsScreen(onNavigateBack: () => Navigator.pop(context)))),
        onNavigateToBills: () => Navigator.push(context, MaterialPageRoute(builder: (_) => BillsAndSubscriptionsScreen(onNavigateBack: () => Navigator.pop(context)))),
        onNavigateToAnalytics: () => setState(() => _currentTabIndex = 5),
        onNavigateToSettings: () => Navigator.push(context, MaterialPageRoute(builder: (_) => SettingsAndSecurityScreen(onNavigateBack: () => Navigator.pop(context)))),
      ),
      AccountsScreen(onNavigateBack: () => setState(() => _currentTabIndex = 0)),
      TransactionsScreen(onNavigateBack: () => setState(() => _currentTabIndex = 0)),
      DebtCenterScreen(onNavigateBack: () => setState(() => _currentTabIndex = 0)),
      AssetsScreen(onNavigateBack: () => setState(() => _currentTabIndex = 0)),
      AnalyticsAndReportsScreen(onNavigateBack: () => setState(() => _currentTabIndex = 0)),
    ];

    return Scaffold(
      body: Stack(
        children: [
          IndexedStack(
            index: _currentTabIndex,
            children: screens,
          ),
          if (activeSheet != null) _buildBottomSheetOverlay(context, activeSheet, controller, accounts, groupedAccounts, isArabic),
        ],
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentTabIndex,
        type: BottomNavigationBarType.fixed,
        backgroundColor: AppColors.slateDarkSurface,
        selectedItemColor: AppColors.tealAccent,
        unselectedItemColor: AppColors.textMutedDark,
        onTap: (index) => setState(() => _currentTabIndex = index),
        items: [
          BottomNavigationBarItem(
            icon: const Icon(Icons.home),
            label: isArabic ? "الرئيسية" : "Dashboard",
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.account_balance),
            label: isArabic ? "الحسابات" : "Accounts",
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.receipt_long),
            label: isArabic ? "العمليات" : "Transactions",
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.handshake),
            label: isArabic ? "الديون" : "Debts",
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.home_work),
            label: isArabic ? "الأصول" : "Assets",
          ),
          BottomNavigationBarItem(
            icon: const Icon(Icons.bar_chart),
            label: isArabic ? "التقارير" : "Analytics",
          ),
        ],
      ),
    );
  }

  Widget _buildBottomSheetOverlay(
    BuildContext context,
    QuickActionSheetType sheetType,
    PfmsController controller,
    List<dynamic> accounts,
    List<dynamic> groupedAccounts,
    bool isArabic,
  ) {
    Widget child;
    switch (sheetType) {
      case QuickActionSheetType.DEPOSIT:
        child = DepositBottomSheet(
          accounts: accounts.cast(),
          groupedAccounts: groupedAccounts.cast(),
          isArabic: isArabic,
          onDismiss: () => controller.closeBottomSheet(),
          onAddDeposit: (amt, acc, cat, nte, curr) => controller.addDeposit(amt, acc, cat, nte, curr),
        );
        break;
      case QuickActionSheetType.QUICK_ADD:
      case QuickActionSheetType.INCOME:
      case QuickActionSheetType.EXPENSE:
      case QuickActionSheetType.TRANSFER:
        child = QuickAddMasterBottomSheet(
          accounts: accounts.cast(),
          groupedAccounts: groupedAccounts.cast(),
          isArabic: isArabic,
          initialType: sheetType == QuickActionSheetType.INCOME ? 0 : sheetType == QuickActionSheetType.EXPENSE ? 1 : 2,
          onDismiss: () => controller.closeBottomSheet(),
          onAddIncome: (amt, acc, cat, pty, nte, curr) => controller.addIncome(amt, acc, cat, pty, nte, curr),
          onAddExpense: (amt, acc, cat, pty, nte, curr) => controller.addExpense(amt, acc, cat, pty, nte, curr),
          onAddTransfer: (amt, src, dst, nte, curr) => controller.addTransfer(amt, src, dst, nte, curr),
        );
        break;
      case QuickActionSheetType.DEBT:
        final persons = ref.watch(personsStreamProvider).value ?? [];
        final personAccounts = ref.watch(personDebtAccountsProvider);
        child = DebtOperationFlow(
          persons: persons,
          accounts: accounts.cast(),
          personDebtAccounts: personAccounts,
          isArabic: isArabic,
          onDismiss: () => controller.closeBottomSheet(),
          onExecuteOperation: (person, curr, opType, dir, amt, accId, notes) {
            controller.closeBottomSheet();
            if (opType == LedgerOperationType.ADD_DEBT) {
              controller.addDebtForPerson(person, dir ?? DebtType.RECEIVABLE, amt, accId, "General", curr, notes);
            } else if (opType == LedgerOperationType.RECEIVE_PAYMENT) {
              controller.addPaymentForPerson(person, amt, accId, curr, notes, true);
            } else {
              controller.addPaymentForPerson(person, amt, accId, curr, notes, false);
            }
          },
          onCreatePerson: (prs) => controller.addPerson(prs),
        );
        break;
      case QuickActionSheetType.ASSET:
        child = AddAssetBottomSheet(
          viewModel: controller,
          accounts: accounts.cast(),
          isArabic: isArabic,
          onDismiss: () => controller.closeBottomSheet(),
        );
        break;
      default:
        child = QuickActionBottomSheet(
          sheetType: sheetType,
          accounts: accounts.cast(),
          onDismiss: () => controller.closeBottomSheet(),
          onAddIncome: (amt, acc, cat, pty, nte) => controller.addIncome(amt, acc, cat, pty, nte),
          onAddExpense: (amt, acc, cat, pty, nte) => controller.addExpense(amt, acc, cat, pty, nte),
          onAddTransfer: (amt, src, dst, nte) => controller.addTransfer(amt, src, dst, nte),
          onAddAsset: (nm, typ, pval, cval, acc) => controller.addAsset(nm, typ, pval, cval, acc),
          onAddDebt: (pty, ph, typ, amt, acc, nte) => controller.addDebt(pty, ph, typ, amt, acc, nte),
          onAddGoal: (ttl, tgt, init) => controller.addGoal(ttl, tgt, init),
          onAddBudget: (cat, lmt) => controller.addBudget(cat, lmt),
          onAddBill: (ttl, amt, cat, acc) => controller.addBill(ttl, amt, cat, acc),
          onExportPdf: () {},
        );
        break;
    }

    return GestureDetector(
      onTap: () => controller.closeBottomSheet(),
      child: Container(
        color: Colors.black54,
        alignment: Alignment.bottomCenter,
        child: GestureDetector(
          onTap: () {},
          child: child,
        ),
      ),
    );
  }
}
