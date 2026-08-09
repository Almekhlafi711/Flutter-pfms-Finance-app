import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../viewmodel/pfms_provider.dart';

class QuickActionGrid extends StatelessWidget {
  final ValueChanged<QuickActionSheetType> onActionSelected;
  final bool isArabic;

  const QuickActionGrid({
    super.key,
    required this.onActionSelected,
    required this.isArabic,
  });

  @override
  Widget build(BuildContext context) {
    final actions = [
      _ActionItem(QuickActionSheetType.INCOME, isArabic ? "دخل جديد" : "Add Income", Icons.arrow_downward, AppColors.greenIncome),
      _ActionItem(QuickActionSheetType.EXPENSE, isArabic ? "مصروف جديد" : "Add Expense", Icons.arrow_upward, AppColors.redExpense),
      _ActionItem(QuickActionSheetType.TRANSFER, isArabic ? "تحويل" : "Transfer", Icons.swap_horiz, AppColors.blueTransfer),
      _ActionItem(QuickActionSheetType.DEPOSIT, isArabic ? "إيداع نقدي" : "Cash Deposit", Icons.savings_outlined, AppColors.tealAccent),
      _ActionItem(QuickActionSheetType.DEBT, isArabic ? "عملية دين" : "Debt Op", Icons.handshake_outlined, AppColors.orangeDebt),
      _ActionItem(QuickActionSheetType.ASSET, isArabic ? "أصل جديد" : "New Asset", Icons.home_work_outlined, AppColors.purpleAsset),
      _ActionItem(QuickActionSheetType.GOAL, isArabic ? "هدف جديد" : "New Goal", Icons.flag_outlined, AppColors.goldAccent),
      _ActionItem(QuickActionSheetType.BILL, isArabic ? "فاتورة" : "Pay Bill", Icons.receipt_long_outlined, AppColors.tealAccentLight),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: DesignTokens.spaceMedium),
          child: Text(
            isArabic ? "الإجراءات السريعة" : "Quick Actions",
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark),
          ),
        ),
        const SizedBox(height: DesignTokens.spaceSmall),
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          padding: const EdgeInsets.symmetric(horizontal: DesignTokens.spaceMedium),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 4,
            childAspectRatio: 0.95,
            crossAxisSpacing: DesignTokens.spaceSmall,
            mainAxisSpacing: DesignTokens.spaceSmall,
          ),
          itemCount: actions.length,
          itemBuilder: (context, index) {
            final item = actions[index];
            return InkWell(
              onTap: () => onActionSelected(item.type),
              borderRadius: BorderRadius.circular(DesignTokens.radiusMedium),
              child: Container(
                decoration: BoxDecoration(
                  color: AppColors.slateDarkCard,
                  borderRadius: BorderRadius.circular(DesignTokens.radiusMedium),
                  border: Border.all(color: AppColors.slateBorder),
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    CircleAvatar(
                      radius: 18,
                      backgroundColor: item.color.withOpacity(0.2),
                      child: Icon(item.icon, color: item.color, size: 20),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      item.title,
                      style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: AppColors.textPrimaryDark),
                      textAlign: TextAlign.center,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
            );
          },
        ),
      ],
    );
  }
}

class _ActionItem {
  final QuickActionSheetType type;
  final String title;
  final IconData icon;
  final Color color;

  _ActionItem(this.type, this.title, this.icon, this.color);
}
