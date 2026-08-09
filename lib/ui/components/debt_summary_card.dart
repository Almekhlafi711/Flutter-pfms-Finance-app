import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../core/util/currency_formatter.dart';

class DebtSummaryCard extends StatelessWidget {
  final double totalReceivables;
  final double totalPayables;
  final bool isArabic;

  const DebtSummaryCard({
    super.key,
    required this.totalReceivables,
    required this.totalPayables,
    required this.isArabic,
  });

  @override
  Widget build(BuildContext context) {
    final netDebt = totalReceivables - totalPayables;

    return Card(
      margin: const EdgeInsets.all(DesignTokens.spaceMedium),
      color: AppColors.slateDarkCard,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(DesignTokens.radiusLarge),
        side: const BorderSide(color: AppColors.slateBorder),
      ),
      child: Padding(
        padding: const EdgeInsets.all(DesignTokens.spaceMediumLarge),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          const Icon(Icons.arrow_downward, color: AppColors.greenIncome, size: 16),
                          const SizedBox(width: 4),
                          Text(isArabic ? "ديون لك (مستحقات)" : "Receivables", style: const TextStyle(fontSize: 12, color: AppColors.textSecondaryDark)),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        CurrencyFormatter.format(totalReceivables, "SAR"),
                        style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.greenIncome),
                      ),
                    ],
                  ),
                ),
                Container(height: 40, width: 1, color: AppColors.slateBorder),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.only(left: DesignTokens.spaceMedium),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Icon(Icons.arrow_upward, color: AppColors.redExpense, size: 16),
                            const SizedBox(width: 4),
                            Text(isArabic ? "ديون عليك (التزامات)" : "Payables", style: const TextStyle(fontSize: 12, color: AppColors.textSecondaryDark)),
                          ],
                        ),
                        const SizedBox(height: 4),
                        Text(
                          CurrencyFormatter.format(totalPayables, "SAR"),
                          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.redExpense),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: DesignTokens.spaceMedium),
            Container(
              padding: const EdgeInsets.all(DesignTokens.spaceSmall),
              decoration: BoxDecoration(
                color: AppColors.slateDarkBackground,
                borderRadius: BorderRadius.circular(DesignTokens.radiusSmall),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(isArabic ? "صافي الديون" : "Net Debt Position", style: const TextStyle(fontSize: 12, color: AppColors.textSecondaryDark)),
                  Text(
                    CurrencyFormatter.format(netDebt, "SAR"),
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      color: netDebt >= 0 ? AppColors.greenIncome : AppColors.redExpense,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
