import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../domain/models/domain_models.dart';

class QuickAddMasterBottomSheet extends StatefulWidget {
  final List<Account> accounts;
  final List<GroupedAccount> groupedAccounts;
  final bool isArabic;
  final int initialType; // 0 = Income, 1 = Expense, 2 = Transfer
  final VoidCallback onDismiss;
  final Function(double amt, String acc, String cat, String pty, String nte, String curr) onAddIncome;
  final Function(double amt, String acc, String cat, String pty, String nte, String curr) onAddExpense;
  final Function(double amt, String src, String dst, String nte, String curr) onAddTransfer;

  const QuickAddMasterBottomSheet({
    super.key,
    required this.accounts,
    required this.groupedAccounts,
    required this.isArabic,
    this.initialType = 1,
    required this.onDismiss,
    required this.onAddIncome,
    required this.onAddExpense,
    required this.onAddTransfer,
  });

  @override
  State<QuickAddMasterBottomSheet> createState() => _QuickAddMasterBottomSheetState();
}

class _QuickAddMasterBottomSheetState extends State<QuickAddMasterBottomSheet> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  final _amountController = TextEditingController();
  final _categoryController = TextEditingController();
  final _partyController = TextEditingController();
  final _noteController = TextEditingController();

  late String _sourceAccountId;
  late String _destAccountId;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this, initialIndex: widget.initialType);
    _sourceAccountId = widget.accounts.isNotEmpty ? widget.accounts.first.id : "";
    _destAccountId = widget.accounts.length > 1 ? widget.accounts[1].id : _sourceAccountId;
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
        children: [
          TabBar(
            controller: _tabController,
            indicatorColor: AppColors.tealAccent,
            labelColor: AppColors.tealAccent,
            unselectedLabelColor: AppColors.textSecondaryDark,
            tabs: [
              Tab(text: widget.isArabic ? "دخل" : "Income"),
              Tab(text: widget.isArabic ? "مصروف" : "Expense"),
              Tab(text: widget.isArabic ? "تحويل" : "Transfer"),
            ],
          ),
          const SizedBox(height: DesignTokens.spaceMedium),
          TextField(
            controller: _amountController,
            keyboardType: TextInputType.number,
            style: const TextStyle(color: AppColors.textPrimaryDark, fontSize: 20, fontWeight: FontWeight.bold),
            decoration: InputDecoration(
              labelText: widget.isArabic ? "المبلغ" : "Amount",
              labelStyle: const TextStyle(color: AppColors.textSecondaryDark),
              filled: true,
              fillColor: AppColors.slateDarkCard,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(DesignTokens.radiusMedium)),
            ),
          ),
          const SizedBox(height: DesignTokens.spaceSmall),
          DropdownButtonFormField<String>(
            value: _sourceAccountId,
            dropdownColor: AppColors.slateDarkCard,
            style: const TextStyle(color: AppColors.textPrimaryDark),
            decoration: InputDecoration(
              labelText: widget.isArabic ? "الحساب" : "Account",
              filled: true,
              fillColor: AppColors.slateDarkCard,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(DesignTokens.radiusMedium)),
            ),
            items: widget.accounts.map((acc) {
              return DropdownMenuItem(value: acc.id, child: Text("${acc.name} (${acc.currency})"));
            }).toList(),
            onChanged: (val) {
              if (val != null) setState(() => _sourceAccountId = val);
            },
          ),
          const SizedBox(height: DesignTokens.spaceSmall),
          TextField(
            controller: _categoryController,
            style: const TextStyle(color: AppColors.textPrimaryDark),
            decoration: InputDecoration(
              labelText: widget.isArabic ? "التصنيف" : "Category",
              filled: true,
              fillColor: AppColors.slateDarkCard,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(DesignTokens.radiusMedium)),
            ),
          ),
          const SizedBox(height: DesignTokens.spaceSmall),
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
                if (amount <= 0 || _sourceAccountId.isEmpty) return;

                final index = _tabController.index;
                if (index == 0) {
                  widget.onAddIncome(amount, _sourceAccountId, _categoryController.text.isEmpty ? "General Income" : _categoryController.text, _partyController.text, _noteController.text, "SAR");
                } else if (index == 1) {
                  widget.onAddExpense(amount, _sourceAccountId, _categoryController.text.isEmpty ? "General Expense" : _categoryController.text, _partyController.text, _noteController.text, "SAR");
                } else {
                  widget.onAddTransfer(amount, _sourceAccountId, _destAccountId, _noteController.text, "SAR");
                }
              },
              child: Text(
                widget.isArabic ? "حفظ العملية" : "Save Transaction",
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.black),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
