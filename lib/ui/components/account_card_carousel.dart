import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../core/util/currency_formatter.dart';
import '../../domain/models/domain_models.dart';

class AccountCardCarousel extends StatelessWidget {
  final List<Account> accounts;
  final String? selectedAccountId;
  final ValueChanged<String?> onAccountSelected;
  final VoidCallback onAddAccount;
  final bool isArabic;

  const AccountCardCarousel({
    super.key,
    required this.accounts,
    required this.selectedAccountId,
    required this.onAccountSelected,
    required this.onAddAccount,
    required this.isArabic,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: DesignTokens.spaceMedium),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                isArabic ? "الحسابات المحفظية" : "Portfolio Accounts",
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimaryDark,
                ),
              ),
              IconButton(
                icon: const Icon(Icons.add_circle_outline, color: AppColors.tealAccent),
                onPressed: onAddAccount,
              ),
            ],
          ),
        ),
        const SizedBox(height: DesignTokens.spaceSmall),
        SizedBox(
          height: 140,
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: DesignTokens.spaceMedium),
            itemCount: accounts.length + 1,
            itemBuilder: (context, index) {
              if (index == 0) {
                final isSelected = selectedAccountId == null;
                final totalBal = accounts.fold(0.0, (sum, a) => sum + a.balance);
                return GestureDetector(
                  onTap: () => onAccountSelected(null),
                  child: Container(
                    width: 200,
                    margin: const EdgeInsets.only(right: DesignTokens.spaceSmall),
                    padding: const EdgeInsets.all(DesignTokens.spaceMedium),
                    decoration: BoxDecoration(
                      color: isSelected ? AppColors.tealAccent.withValues(alpha: 0.2) : AppColors.slateDarkCard,
                      borderRadius: BorderRadius.circular(DesignTokens.radiusMedium),
                      border: Border.all(
                        color: isSelected ? AppColors.tealAccent : AppColors.slateBorder,
                        width: isSelected ? 2.0 : 1.0,
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Row(
                          children: [
                            const Icon(Icons.dashboard_outlined, color: AppColors.tealAccent, size: 24),
                            const SizedBox(width: DesignTokens.spaceSmall),
                            Text(
                              isArabic ? "جميع الحسابات" : "All Accounts",
                              style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark),
                            ),
                          ],
                        ),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              isArabic ? "إجمالي الرصيد" : "Total Balance",
                              style: const TextStyle(fontSize: 12, color: AppColors.textSecondaryDark),
                            ),
                            Text(
                              CurrencyFormatter.format(totalBal, "SAR"),
                              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.goldAccent),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                );
              }

              final account = accounts[index - 1];
              final isSelected = selectedAccountId == account.id;

              return GestureDetector(
                onTap: () => onAccountSelected(account.id),
                child: Container(
                  width: 200,
                  margin: const EdgeInsets.only(right: DesignTokens.spaceSmall),
                  padding: const EdgeInsets.all(DesignTokens.spaceMedium),
                  decoration: BoxDecoration(
                    color: isSelected ? AppColors.tealAccent.withValues(alpha: 0.2) : AppColors.slateDarkCard,
                    borderRadius: BorderRadius.circular(DesignTokens.radiusMedium),
                    border: Border.all(
                      color: isSelected ? AppColors.tealAccent : AppColors.slateBorder,
                      width: isSelected ? 2.0 : 1.0,
                    ),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Icon(
                            account.type == AccountType.CASH
                                ? Icons.account_balance_wallet
                                : account.type == AccountType.SAVINGS
                                    ? Icons.savings
                                    : account.type == AccountType.CRYPTO
                                        ? Icons.currency_bitcoin
                                        : Icons.account_balance,
                            color: AppColors.tealAccent,
                            size: 24,
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(
                              color: AppColors.slateBorder,
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: Text(
                              account.currency,
                              style: const TextStyle(fontSize: 10, color: AppColors.textSecondaryDark),
                            ),
                          ),
                        ],
                      ),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            account.name,
                            style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                          Text(
                            CurrencyFormatter.format(account.balance, account.currency),
                            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.greenIncome),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}
