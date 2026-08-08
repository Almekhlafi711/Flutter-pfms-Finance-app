import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';

class DebtFilterTabs extends StatelessWidget {
  final int selectedIndex;
  final ValueChanged<int> onTabSelected;
  final bool isArabic;

  const DebtFilterTabs({
    super.key,
    required this.selectedIndex,
    required this.onTabSelected,
    required this.isArabic,
  });

  @override
  Widget build(BuildContext context) {
    final tabs = isArabic
        ? ["الكل", "مستحقات (لك)", "التزامات (عليك)"]
        : ["All", "Receivables", "Payables"];

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: DesignTokens.spaceMedium),
      child: Row(
        children: List.generate(tabs.length, (index) {
          final isSelected = selectedIndex == index;
          return Padding(
            padding: const EdgeInsets.only(right: DesignTokens.spaceSmall),
            child: ChoiceChip(
              label: Text(tabs[index]),
              selected: isSelected,
              onSelected: (_) => onTabSelected(index),
              selectedColor: AppColors.tealAccent.withOpacity(0.2),
              backgroundColor: AppColors.slateDarkCard,
              labelStyle: TextStyle(
                color: isSelected ? AppColors.tealAccent : AppColors.textSecondaryDark,
                fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
              ),
              side: BorderSide(
                color: isSelected ? AppColors.tealAccent : AppColors.slateBorder,
              ),
            ),
          );
        }),
      ),
    );
  }
}
