import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../core/util/currency_formatter.dart';
import '../../domain/models/domain_models.dart';
import '../viewmodel/pfms_provider.dart';

class BillsAndSubscriptionsScreen extends ConsumerWidget {
  final VoidCallback onNavigateBack;

  const BillsAndSubscriptionsScreen({super.key, required this.onNavigateBack});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final controller = PfmsController(ref);
    final bills = ref.watch(billsStreamProvider).value ?? [];
    final accounts = ref.watch(accountsStreamProvider).value ?? [];
    final isArabic = ref.watch(isArabicProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(isArabic ? "الفواتير والاشتراكات" : "Bills & Subscriptions"),
        leading: IconButton(icon: const Icon(Icons.arrow_back), onPressed: onNavigateBack),
      ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: AppColors.tealAccentLight,
        onPressed: () => controller.openBottomSheet(QuickActionSheetType.BILL),
        child: const Icon(Icons.add, color: Colors.black),
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(DesignTokens.spaceMedium),
        itemCount: bills.length,
        itemBuilder: (context, index) {
          final bill = bills[index];
          final isPaid = bill.status == BillStatus.PAID;

          return Card(
            color: AppColors.slateDarkCard,
            margin: const EdgeInsets.only(bottom: DesignTokens.spaceSmall),
            child: ListTile(
              leading: CircleAvatar(
                backgroundColor: isPaid ? AppColors.greenIncome.withOpacity(0.2) : AppColors.goldAccent.withOpacity(0.2),
                child: Icon(Icons.receipt_long, color: isPaid ? AppColors.greenIncome : AppColors.goldAccent),
              ),
              title: Text(bill.title, style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark)),
              subtitle: Text("${bill.frequency} • ${CurrencyFormatter.format(bill.amount, bill.currency)}", style: const TextStyle(color: AppColors.textMutedDark)),
              trailing: isPaid
                  ? Text(isArabic ? "مدفوعة" : "Paid", style: const TextStyle(color: AppColors.greenIncome, fontWeight: FontWeight.bold))
                  : ElevatedButton(
                      style: ElevatedButton.styleFrom(backgroundColor: AppColors.tealAccent),
                      onPressed: () {
                        if (accounts.isNotEmpty) {
                          controller.payBill(bill, accounts.first.id);
                        }
                      },
                      child: Text(isArabic ? "دفع" : "Pay", style: const TextStyle(color: Colors.black)),
                    ),
            ),
          );
        },
      ),
    );
  }
}
