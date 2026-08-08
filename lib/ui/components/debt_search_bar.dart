import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';

class DebtSearchBar extends StatelessWidget {
  final String query;
  final ValueChanged<String> onChanged;
  final VoidCallback onOpenFilter;
  final bool isArabic;

  const DebtSearchBar({
    super.key,
    required this.query,
    required this.onChanged,
    required this.onOpenFilter,
    required this.isArabic,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: DesignTokens.spaceMedium),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              onChanged: onChanged,
              style: const TextStyle(color: AppColors.textPrimaryDark),
              decoration: InputDecoration(
                hintText: isArabic ? "بحث في الديون أو الأشخاص..." : "Search debts or people...",
                hintStyle: const TextStyle(color: AppColors.textMutedDark, fontSize: 13),
                prefixIcon: const Icon(Icons.search, color: AppColors.textSecondaryDark),
                filled: true,
                fillColor: AppColors.slateDarkCard,
                contentPadding: const EdgeInsets.symmetric(vertical: 0),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(DesignTokens.radiusMedium),
                  borderSide: const BorderSide(color: AppColors.slateBorder),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(DesignTokens.radiusMedium),
                  borderSide: const BorderSide(color: AppColors.slateBorder),
                ),
              ),
            ),
          ),
          const SizedBox(width: DesignTokens.spaceSmall),
          IconButton(
            icon: const Icon(Icons.filter_list, color: AppColors.tealAccent),
            onPressed: onOpenFilter,
          ),
        ],
      ),
    );
  }
}
