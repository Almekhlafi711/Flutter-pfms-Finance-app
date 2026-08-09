import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../core/util/currency_formatter.dart';
import '../../domain/models/domain_models.dart';

class FinancialOverviewCard extends StatelessWidget {
  final NetWorthSummary netWorth;
  final bool isArabic;

  const FinancialOverviewCard({
    super.key,
    required this.netWorth,
    required this.isArabic,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.all(DesignTokens.spaceMedium),
      color: AppColors.slateDarkCard,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(DesignTokens.radiusLarge),
        side: const BorderSide(color: AppColors.slateBorder, width: DesignTokens.borderWidth),
      ),
      child: Padding(
        padding: const EdgeInsets.all(DesignTokens.spaceLarge),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  isArabic ? "صافي الثروة" : "Net Worth",
                  style: const TextStyle(fontSize: 14, color: AppColors.textSecondaryDark),
                ),
                const Icon(Icons.shield_outlined, color: AppColors.tealAccent, size: 20),
              ],
            ),
            const SizedBox(height: DesignTokens.spaceSmall),
            Text(
              CurrencyFormatter.format(netWorth.netWorth, "SAR"),
              style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold, color: AppColors.goldAccent),
            ),
            const SizedBox(height: DesignTokens.spaceLarge),
            const Divider(color: AppColors.slateBorder),
            const SizedBox(height: DesignTokens.spaceMedium),
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(isArabic ? "النقد والحسابات" : "Cash & Accounts", style: const TextStyle(fontSize: 11, color: AppColors.textMutedDark)),
                      Text(CurrencyFormatter.formatCompact(netWorth.totalCashAndAccounts, "SAR"), style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark)),
                    ],
                  ),
                ),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(isArabic ? "قيمة الأصول" : "Assets Value", style: const TextStyle(fontSize: 11, color: AppColors.textMutedDark)),
                      Text(CurrencyFormatter.formatCompact(netWorth.totalAssetsValue, "SAR"), style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.purpleAsset)),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: DesignTokens.spaceMedium),
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(isArabic ? "ديون لك (مستحقات)" : "Receivables", style: const TextStyle(fontSize: 11, color: AppColors.textMutedDark)),
                      Text(CurrencyFormatter.formatCompact(netWorth.totalReceivables, "SAR"), style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.greenIncome)),
                    ],
                  ),
                ),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(isArabic ? "ديون عليك (التزامات)" : "Liabilities", style: const TextStyle(fontSize: 11, color: AppColors.textMutedDark)),
                      Text(CurrencyFormatter.formatCompact(netWorth.totalLiabilitiesAndPayables, "SAR"), style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.redExpense)),
                    ],
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
