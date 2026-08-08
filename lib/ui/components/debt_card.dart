import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../core/util/currency_formatter.dart';
import '../../domain/models/debt_models.dart';
import '../../domain/models/domain_models.dart';

class DebtCard extends StatelessWidget {
  final PersonDebtAccount debtAccount;
  final VoidCallback onTap;
  final bool isArabic;

  const DebtCard({
    super.key,
    required this.debtAccount,
    required this.onTap,
    required this.isArabic,
  });

  @override
  Widget build(BuildContext context) {
    final debt = debtAccount.mainDebt;
    final isReceivable = debt.type == DebtType.RECEIVABLE;
    final statusColor = debt.status == DebtStatus.COMPLETED
        ? AppColors.greenIncome
        : isReceivable ? AppColors.greenIncome : AppColors.redExpense;

    return Card(
      margin: const EdgeInsets.only(bottom: DesignTokens.spaceMedium),
      color: AppColors.slateDarkCard,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(DesignTokens.radiusMedium),
        side: const BorderSide(color: AppColors.slateBorder),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(DesignTokens.radiusMedium),
        child: Padding(
          padding: const EdgeInsets.all(DesignTokens.spaceMedium),
          child: Column(
            cross: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      CircleAvatar(
                        radius: 20,
                        backgroundColor: statusColor.withOpacity(0.2),
                        child: Text(
                          debtAccount.person.name.isNotEmpty ? debtAccount.person.name[0].toUpperCase() : "?",
                          style: TextStyle(fontWeight: FontWeight.bold, color: statusColor),
                        ),
                      ),
                      const SizedBox(width: DesignTokens.spaceSmall),
                      Column(
                        cross: CrossAxisAlignment.start,
                        children: [
                          Text(
                            debtAccount.person.name,
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: AppColors.textPrimaryDark),
                          ),
                          Text(
                            isReceivable
                                ? (isArabic ? "لك (مستحق)" : "Receivable")
                                : (isArabic ? "عليك (التزام)" : "Payable"),
                            style: TextStyle(fontSize: 12, color: statusColor, fontWeight: FontWeight.w600),
                          ),
                        ],
                      ),
                    ],
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: statusColor.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      debt.status.name,
                      style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: statusColor),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: DesignTokens.spaceMedium),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Column(
                    cross: CrossAxisAlignment.start,
                    children: [
                      Text(isArabic ? "المبلغ الأصلي" : "Original", style: const TextStyle(fontSize: 11, color: AppColors.textMutedDark)),
                      Text(CurrencyFormatter.format(debt.originalAmount, debt.currency), style: const TextStyle(fontWeight: FontWeight.w600, color: AppColors.textSecondaryDark)),
                    ],
                  ),
                  Column(
                    cross: CrossAxisAlignment.end,
                    children: [
                      Text(isArabic ? "المطبقي" : "Remaining", style: const TextStyle(fontSize: 11, color: AppColors.textMutedDark)),
                      Text(CurrencyFormatter.format(debt.remainingAmount, debt.currency), style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: statusColor)),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: DesignTokens.spaceSmall),
              ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: LinearProgressIndicator(
                  value: debt.progress,
                  backgroundColor: AppColors.slateBorder,
                  color: statusColor,
                  minHeight: 6,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
