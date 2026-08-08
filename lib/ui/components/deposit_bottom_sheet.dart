import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../domain/models/domain_models.dart';

class DepositBottomSheet extends StatefulWidget {
  final List<Account> accounts;
  final List<GroupedAccount> groupedAccounts;
  final bool isArabic;
  final VoidCallback onDismiss;
  final Function(double amount, String accountId, String category, String note, String currency) onAddDeposit;

  const DepositBottomSheet({
    super.key,
    required this.accounts,
    required this.groupedAccounts,
    required this.isArabic,
    required this.onDismiss,
    required this.onAddDeposit,
  });

  @override
  State<DepositBottomSheet> createState() => _DepositBottomSheetState();
}

class _DepositBottomSheetState extends State<DepositBottomSheet> {
  final _amountController = TextEditingController();
  final _categoryController = TextEditingController(text: "Cash Deposit");
  final _noteController = TextEditingController();
  late String _selectedAccountId;
  String _selectedCurrency = "SAR";

  @override
  void initState() {
    super.initState();
    _selectedAccountId = widget.accounts.isNotEmpty ? widget.accounts.first.id : "";
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.only(
        top: DesignTokens.spaceMedium,
        left: DesignTokens.spaceMedium,
        right: DesignTokens.spaceMedium,
        bottom: MediaQuery.of(context).viewInsets.bottom + DesignTokens.spaceMedium,
      ),
      decoration: const BoxDecoration(
        color: AppColors.slateDarkSurface,
        borderRadius: BorderRadius.vertical(top: Radius.circular(DesignTokens.radiusLarge)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                widget.isArabic ? "إيداع نقدي جديد" : "New Cash Deposit",
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark),
              ),
              IconButton(icon: const Icon(Icons.close, color: AppColors.textSecondaryDark), onPressed: widget.onDismiss),
            ],
          ),
          const SizedBox(height: DesignTokens.spaceMedium),
          TextField(
            controller: _amountController,
            keyboardType: TextInputType.number,
            style: const TextStyle(color: AppColors.textPrimaryDark, fontSize: 18, fontWeight: FontWeight.bold),
            decoration: InputDecoration(
              labelText: widget.isArabic ? "المبلغ" : "Amount",
              labelStyle: const TextStyle(color: AppColors.textSecondaryDark),
              filled: true,
              fillColor: AppColors.slateDarkCard,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(DesignTokens.radiusMedium)),
            ),
          ),
          const SizedBox(height: DesignTokens.spaceMedium),
          DropdownButtonFormField<String>(
            value: _selectedAccountId,
            dropdownColor: AppColors.slateDarkCard,
            style: const TextStyle(color: AppColors.textPrimaryDark),
            decoration: InputDecoration(
              labelText: widget.isArabic ? "الحساب" : "Account",
              filled: true,
              fillColor: AppColors.slateDarkCard,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(DesignTokens.radiusMedium)),
            ),
            items: widget.accounts.map((acc) {
              return DropdownMenuItem(
                value: acc.id,
                child: Text("${acc.name} (${acc.currency})"),
              );
            }).toList(),
            onChanged: (val) {
              if (val != null) setState(() => _selectedAccountId = val);
            },
          ),
          const SizedBox(height: DesignTokens.spaceMedium),
          TextField(
            controller: _noteController,
            style: const TextStyle(color: AppColors.textPrimaryDark),
            decoration: InputDecoration(
              labelText: widget.isArabic ? "ملاحظات" : "Notes",
              filled: true,
              fillColor: AppColors.slateDarkCard,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(DesignTokens.radiusMedium)),
            ),
          ),
          const SizedBox(height: DesignTokens.spaceLarge),
          SizedBox(
            width: double.infinity,
            height: 48,
            child: ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.tealAccent,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(DesignTokens.radiusMedium)),
              ),
              onPressed: () {
                final amount = double.tryParse(_amountController.text) ?? 0.0;
                if (amount > 0 && _selectedAccountId.isNotEmpty) {
                  widget.onAddDeposit(amount, _selectedAccountId, _categoryController.text, _noteController.text, _selectedCurrency);
                }
              },
              child: Text(
                widget.isArabic ? "تأكيد الإيداع" : "Confirm Deposit",
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.black),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
