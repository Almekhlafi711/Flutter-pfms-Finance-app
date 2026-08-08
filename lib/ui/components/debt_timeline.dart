import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../core/util/currency_formatter.dart';
import '../../domain/models/debt_models.dart';

class DebtTimeline extends StatelessWidget {
  final List<DebtLedgerEntry> entries;
  final String currency;
  final bool isArabic;

  const DebtTimeline({
    super.key,
    required this.entries,
    required this.currency,
    required this.isArabic,
  });

  @override
  Widget build(BuildContext context) {
    if (entries.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(DesignTokens.spaceLarge),
          child: Text(
            isArabic ? "لا توجد حركات دين مسجلة" : "No debt entries recorded",
            style: const TextStyle(color: AppColors.textMutedDark),
          ),
        ),
      );
    }

    return ListView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: entries.length,
      itemBuilder: (context, index) {
        final entry = entries[index];
        final dateStr = DateTime.fromMillisecondsSinceEpoch(entry.date).toString().split(" ")[0];
        final isPayment = entry.isPayment;
        final color = isPayment ? AppColors.greenIncome : AppColors.orangeDebt;

        return Row(
          cross: CrossAxisAlignment.start,
          children: [
            Column(
              children: [
                CircleAvatar(
                  radius: 12,
                  backgroundColor: color.withOpacity(0.2),
                  child: Icon(isPayment ? Icons.check : Icons.add, size: 14, color: color),
                ),
                if (index < entries.length - 1)
                  Container(
                    width: 2,
                    height: 40,
                    color: AppColors.slateBorder,
                  ),
              ],
            ),
            const SizedBox(width: DesignTokens.spaceMedium),
            Expanded(
              child: Container(
                margin: const EdgeInsets.only(bottom: DesignTokens.spaceSmall),
                padding: const EdgeInsets.all(DesignTokens.spaceSmall),
                decoration: BoxDecoration(
                  color: AppColors.slateDarkCard,
                  borderRadius: BorderRadius.circular(DesignTokens.radiusSmall),
                  border: Border.all(color: AppColors.slateBorder),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Column(
                      cross: CrossAxisAlignment.start,
                      children: [
                        Text(
                          entry.description.isNotEmpty ? entry.description : (isPayment ? "Payment" : "Principal"),
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppColors.textPrimaryDark),
                        ),
                        Text(
                          "$dateStr • ${entry.paymentMethod}",
                          style: const TextStyle(fontSize: 11, color: AppColors.textMutedDark),
                        ),
                      ],
                    ),
                    Text(
                      CurrencyFormatter.format(entry.amount, currency),
                      style: TextStyle(fontWeight: FontWeight.bold, color: color),
                    ),
                  ],
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}
