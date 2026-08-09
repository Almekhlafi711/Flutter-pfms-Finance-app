import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../core/util/currency_formatter.dart';
import '../../domain/models/domain_models.dart';
import '../components/account_card_carousel.dart';
import '../components/financial_overview_card.dart';
import '../components/quick_action_grid.dart';
import '../viewmodel/pfms_provider.dart';

class DashboardScreen extends ConsumerWidget {
  final VoidCallback onNavigateToAccounts;
  final VoidCallback onNavigateToTransactions;
  final VoidCallback onNavigateToDebts;
  final VoidCallback onNavigateToAssets;
  final VoidCallback onNavigateToGoals;
  final VoidCallback onNavigateToBills;
  final VoidCallback onNavigateToAnalytics;
  final VoidCallback onNavigateToSettings;

  const DashboardScreen({
    super.key,
    required this.onNavigateToAccounts,
    required this.onNavigateToTransactions,
    required this.onNavigateToDebts,
    required this.onNavigateToAssets,
    required this.onNavigateToGoals,
    required this.onNavigateToBills,
    required this.onNavigateToAnalytics,
    required this.onNavigateToSettings,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final controller = PfmsController(ref);
    final netWorth = ref.watch(netWorthSummaryProvider);
    final accounts = ref.watch(accountsStreamProvider).value ?? [];
    final selectedAccountId = ref.watch(selectedAccountIdProvider);
    final transactions = ref.watch(filteredTransactionsProvider);
    final isArabic = ref.watch(isArabicProvider);

    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            const Icon(Icons.account_balance_wallet, color: AppColors.tealAccent),
            const SizedBox(width: DesignTokens.spaceSmall),
            Text(
              isArabic ? "النظام المالي الشخصي" : "PFMS Finance",
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: Icon(isArabic ? Icons.language : Icons.translate, color: AppColors.tealAccent),
            onPressed: () => controller.toggleLanguage(),
          ),
          IconButton(
            icon: const Icon(Icons.settings, color: AppColors.textSecondaryDark),
            onPressed: onNavigateToSettings,
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            FinancialOverviewCard(netWorth: netWorth, isArabic: isArabic),
            AccountCardCarousel(
              accounts: accounts,
              selectedAccountId: selectedAccountId,
              onAccountSelected: (id) => controller.selectAccount(id),
              onAddAccount: onNavigateToAccounts,
              isArabic: isArabic,
            ),
            const SizedBox(height: DesignTokens.spaceLarge),
            QuickActionGrid(
              onActionSelected: (type) => controller.openBottomSheet(type),
              isArabic: isArabic,
            ),
            const SizedBox(height: DesignTokens.spaceLarge),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: DesignTokens.spaceMedium),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    isArabic ? "آخر العمليات" : "Recent Activities",
                    style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark),
                  ),
                  TextButton(
                    onPressed: onNavigateToTransactions,
                    child: Text(isArabic ? "عرض الكل" : "View All", style: const TextStyle(color: AppColors.tealAccent)),
                  ),
                ],
              ),
            ),
            ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              padding: const EdgeInsets.symmetric(horizontal: DesignTokens.spaceMedium),
              itemCount: transactions.length > 5 ? 5 : transactions.length,
              itemBuilder: (context, index) {
                final tx = transactions[index];
                final isIncome = tx.type == TransactionType.INCOME;
                final color = isIncome ? AppColors.greenIncome : AppColors.redExpense;

                return Card(
                  color: AppColors.slateDarkCard,
                  margin: const EdgeInsets.only(bottom: DesignTokens.spaceSmall),
                  child: ListTile(
                    leading: CircleAvatar(
                      backgroundColor: color.withOpacity(0.2),
                      child: Icon(
                        isIncome ? Icons.arrow_downward : Icons.arrow_upward,
                        color: color,
                        size: 20,
                      ),
                    ),
                    title: Text(tx.category, style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark)),
                    subtitle: Text(tx.note, style: const TextStyle(color: AppColors.textMutedDark, fontSize: 12)),
                    trailing: Text(
                      CurrencyFormatter.format(tx.amount, tx.currency),
                      style: TextStyle(fontWeight: FontWeight.bold, color: color),
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: DesignTokens.spaceHuge),
          ],
        ),
      ),
    );
  }
}
