import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../core/util/currency_formatter.dart';
import '../viewmodel/pfms_provider.dart';

class BudgetsAndGoalsScreen extends ConsumerWidget {
  final VoidCallback onNavigateBack;

  const BudgetsAndGoalsScreen({super.key, required this.onNavigateBack});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final controller = PfmsController(ref);
    final goals = ref.watch(goalsStreamProvider).value ?? [];
    final budgets = ref.watch(budgetsStreamProvider).value ?? [];
    final isArabic = ref.watch(isArabicProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(isArabic ? "الأهداف والميزانيات" : "Goals & Budgets"),
        leading: IconButton(icon: const Icon(Icons.arrow_back), onPressed: onNavigateBack),
      ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: AppColors.goldAccent,
        onPressed: () => controller.openBottomSheet(QuickActionSheetType.GOAL),
        child: const Icon(Icons.add, color: Colors.black),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(DesignTokens.spaceMedium),
        child: Column(
          cross: CrossAxisAlignment.start,
          children: [
            Text(
              isArabic ? "الأهداف الادخارية" : "Savings Goals",
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark),
            ),
            const SizedBox(height: DesignTokens.spaceSmall),
            ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollablePhysics(),
              itemCount: goals.length,
              itemBuilder: (context, index) {
                final goal = goals[index];
                return Card(
                  color: AppColors.slateDarkCard,
                  margin: const EdgeInsets.only(bottom: DesignTokens.spaceSmall),
                  child: Padding(
                    padding: const EdgeInsets.all(DesignTokens.spaceMedium),
                    child: Column(
                      cross: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(goal.title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: AppColors.textPrimaryDark)),
                            Text(CurrencyFormatter.format(goal.currentAmount, goal.currency), style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.goldAccent)),
                          ],
                        ),
                        const SizedBox(height: DesignTokens.spaceSmall),
                        ClipRRect(
                          borderRadius: BorderRadius.circular(4),
                          child: LinearProgressIndicator(
                            value: goal.progress,
                            backgroundColor: AppColors.slateBorder,
                            color: AppColors.goldAccent,
                            minHeight: 6,
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: DesignTokens.spaceLarge),
            Text(
              isArabic ? "الميزانيات الشهرية" : "Monthly Budgets",
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark),
            ),
            const SizedBox(height: DesignTokens.spaceSmall),
            ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollablePhysics(),
              itemCount: budgets.length,
              itemBuilder: (context, index) {
                final budget = budgets[index];
                return Card(
                  color: AppColors.slateDarkCard,
                  margin: const EdgeInsets.only(bottom: DesignTokens.spaceSmall),
                  child: ListTile(
                    title: Text(budget.category, style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark)),
                    subtitle: Text("${isArabic ? 'المصروف' : 'Spent'}: ${CurrencyFormatter.format(budget.spentAmount, budget.currency)} / ${CurrencyFormatter.format(budget.monthlyLimit, budget.currency)}"),
                    trailing: CircularProgressIndicator(
                      value: budget.usageRatio,
                      backgroundColor: AppColors.slateBorder,
                      color: budget.usageRatio > 0.9 ? AppColors.redExpense : AppColors.tealAccent,
                    ),
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}
