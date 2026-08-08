import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../core/util/currency_formatter.dart';
import '../../domain/models/domain_models.dart';
import '../viewmodel/pfms_provider.dart';

class AccountsScreen extends ConsumerWidget {
  final VoidCallback onNavigateBack;

  const AccountsScreen({super.key, required this.onNavigateBack});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final controller = PfmsController(ref);
    final accounts = ref.watch(accountsStreamProvider).value ?? [];
    final isArabic = ref.watch(isArabicProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(isArabic ? "إدارة الحسابات" : "Accounts Management"),
        leading: IconButton(icon: const Icon(Icons.arrow_back), onPressed: onNavigateBack),
      ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: AppColors.tealAccent,
        onPressed: () {
          _showAddAccountDialog(context, controller, isArabic);
        },
        child: const Icon(Icons.add, color: Colors.black),
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(DesignTokens.spaceMedium),
        itemCount: accounts.length,
        itemBuilder: (context, index) {
          final account = accounts[index];
          return Card(
            color: AppColors.slateDarkCard,
            margin: const EdgeInsets.only(bottom: DesignTokens.spaceSmall),
            child: ListTile(
              leading: CircleAvatar(
                backgroundColor: AppColors.tealAccent.withOpacity(0.2),
                child: const Icon(Icons.account_balance, color: AppColors.tealAccent),
              ),
              title: Text(account.name, style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark)),
              subtitle: Text("${account.type.name} • ${account.currency}", style: const TextStyle(color: AppColors.textMutedDark)),
              trailing: Text(
                CurrencyFormatter.format(account.balance, account.currency),
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: AppColors.greenIncome),
              ),
            ),
          );
        },
      ),
    );
  }

  void _showAddAccountDialog(BuildContext context, PfmsController controller, bool isArabic) {
    final nameController = TextEditingController();
    final balanceController = TextEditingController();
    AccountType selectedType = AccountType.BANK;

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          backgroundColor: AppColors.slateDarkSurface,
          title: Text(isArabic ? "إضافة حساب جديد" : "Add New Account", style: const TextStyle(color: AppColors.textPrimaryDark)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameController,
                style: const TextStyle(color: AppColors.textPrimaryDark),
                decoration: InputDecoration(
                  labelText: isArabic ? "اسم الحساب" : "Account Name",
                  filled: true,
                  fillColor: AppColors.slateDarkCard,
                ),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: balanceController,
                keyboardType: TextInputType.number,
                style: const TextStyle(color: AppColors.textPrimaryDark),
                decoration: InputDecoration(
                  labelText: isArabic ? "الرصيد الأولي" : "Initial Balance",
                  filled: true,
                  fillColor: AppColors.slateDarkCard,
                ),
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context), child: Text(isArabic ? "إلغاء" : "Cancel")),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: AppColors.tealAccent),
              onPressed: () {
                final name = nameController.text.trim();
                final bal = double.tryParse(balanceController.text) ?? 0.0;
                if (name.isNotEmpty) {
                  controller.addAccount(name, selectedType, bal, "SAR");
                  Navigator.pop(context);
                }
              },
              child: Text(isArabic ? "حفظ" : "Save", style: const TextStyle(color: Colors.black)),
            ),
          ],
        );
      },
    );
  }
}
