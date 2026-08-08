import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../core/util/currency_formatter.dart';
import '../../domain/models/domain_models.dart';
import '../viewmodel/pfms_provider.dart';

class TransactionsScreen extends ConsumerWidget {
  final VoidCallback onNavigateBack;

  const TransactionsScreen({super.key, required this.onNavigateBack});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final transactions = ref.watch(filteredTransactionsProvider);
    final isArabic = ref.watch(isArabicProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(isArabic ? "سجل العمليات" : "Transactions Log"),
        leading: IconButton(icon: const Icon(Icons.arrow_back), onPressed: onNavigateBack),
      ),
      body: transactions.isEmpty
          ? Center(
              child: Text(
                isArabic ? "لا توجد عمليات مسجلة" : "No transactions recorded",
                style: const TextStyle(color: AppColors.textMutedDark),
              ),
            )
          : ListView.builder(
              padding: const EdgeInsets.all(DesignTokens.spaceMedium),
              itemCount: transactions.length,
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
                      child: Icon(isIncome ? Icons.arrow_downward : Icons.arrow_upward, color: color),
                    ),
                    title: Text(tx.category, style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark)),
                    subtitle: Text(tx.note.isNotEmpty ? tx.note : tx.type.name, style: const TextStyle(color: AppColors.textMutedDark)),
                    trailing: Text(
                      CurrencyFormatter.format(tx.amount, tx.currency),
                      style: TextStyle(fontWeight: FontWeight.bold, color: color),
                    ),
                  ),
                );
              },
            ),
    );
  }
}
