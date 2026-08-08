import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../core/util/currency_formatter.dart';
import '../viewmodel/pfms_provider.dart';

class AnalyticsAndReportsScreen extends ConsumerWidget {
  final VoidCallback onNavigateBack;

  const AnalyticsAndReportsScreen({super.key, required this.onNavigateBack});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final netWorth = ref.watch(netWorthSummaryProvider);
    final isArabic = ref.watch(isArabicProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(isArabic ? "التقارير والتحليلات" : "Analytics & Reports"),
        leading: IconButton(icon: const Icon(Icons.arrow_back), onPressed: onNavigateBack),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(DesignTokens.spaceMedium),
        child: Column(
          children: [
            Card(
              color: AppColors.slateDarkCard,
              child: Padding(
                padding: const EdgeInsets.all(DesignTokens.spaceLarge),
                child: Column(
                  children: [
                    Text(isArabic ? "ملخص الهيكل المالي" : "Financial Structure Summary", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: AppColors.textPrimaryDark)),
                    const SizedBox(height: DesignTokens.spaceMedium),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(isArabic ? "إجمالي الأصول والمستحقات" : "Total Assets & Receivables", style: const TextStyle(color: AppColors.textSecondaryDark)),
                        Text(CurrencyFormatter.format(netWorth.totalAssets, "SAR"), style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.greenIncome)),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(isArabic ? "إجمالي الالتزامات" : "Total Liabilities", style: const TextStyle(color: AppColors.textSecondaryDark)),
                        Text(CurrencyFormatter.format(netWorth.totalLiabilitiesAndPayables, "SAR"), style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.redExpense)),
                      ],
                    ),
                    const Divider(color: AppColors.slateBorder, height: 30),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(isArabic ? "صافي الثروة النهائي" : "Net Wealth Position", style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark)),
                        Text(CurrencyFormatter.format(netWorth.netWorth, "SAR"), style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: AppColors.goldAccent)),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
