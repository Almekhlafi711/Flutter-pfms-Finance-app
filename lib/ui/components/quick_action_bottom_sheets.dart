import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../domain/models/domain_models.dart';
import '../viewmodel/pfms_provider.dart';

class QuickActionBottomSheet extends StatefulWidget {
  final QuickActionSheetType sheetType;
  final List<Account> accounts;
  final String? selectedAccountId;
  final VoidCallback onDismiss;
  final Function(double, String, String, String, String) onAddIncome;
  final Function(double, String, String, String, String) onAddExpense;
  final Function(double, String, String, String) onAddTransfer;
  final Function(String, AssetType, double, double, String) onAddAsset;
  final Function(String, String, DebtType, double, String, String) onAddDebt;
  final Function(String, double, double) onAddGoal;
  final Function(String, double) onAddBudget;
  final Function(String, double, String, String) onAddBill;
  final VoidCallback onExportPdf;

  const QuickActionBottomSheet({
    super.key,
    required this.sheetType,
    required this.accounts,
    this.selectedAccountId,
    required this.onDismiss,
    required this.onAddIncome,
    required this.onAddExpense,
    required this.onAddTransfer,
    required this.onAddAsset,
    required this.onAddDebt,
    required this.onAddGoal,
    required this.onAddBudget,
    required this.onAddBill,
    required this.onExportPdf,
  });

  @override
  State<QuickActionBottomSheet> createState() => _QuickActionBottomSheetState();
}

class _QuickActionBottomSheetState extends State<QuickActionBottomSheet> {
  final _titleController = TextEditingController();
  final _amountController = TextEditingController();
  final _targetController = TextEditingController();
  final _categoryController = TextEditingController();

  late String _selectedAccountId;

  @override
  void initState() {
    super.initState();
    _selectedAccountId = widget.accounts.isNotEmpty ? widget.accounts.first.id : "";
  }

  @override
  Widget build(BuildContext context) {
    if (widget.sheetType == QuickActionSheetType.REPORT) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        widget.onExportPdf();
        widget.onDismiss();
      });
      return const SizedBox.shrink();
    }

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
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                widget.sheetType.name,
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark),
              ),
              IconButton(icon: const Icon(Icons.close, color: AppColors.textSecondaryDark), onPressed: widget.onDismiss),
            ],
          ),
          const SizedBox(height: DesignTokens.spaceMedium),
          TextField(
            controller: _titleController,
            style: const TextStyle(color: AppColors.textPrimaryDark),
            decoration: InputDecoration(
              labelText: "Title / Name",
              filled: true,
              fillColor: AppColors.slateDarkCard,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(DesignTokens.radiusMedium)),
            ),
          ),
          const SizedBox(height: DesignTokens.spaceSmall),
          TextField(
            controller: _amountController,
            keyboardType: TextInputType.number,
            style: const TextStyle(color: AppColors.textPrimaryDark, fontSize: 18, fontWeight: FontWeight.bold),
            decoration: InputDecoration(
              labelText: "Amount",
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
                final title = _titleController.text.trim();
                final amount = double.tryParse(_amountController.text) ?? 0.0;
                if (title.isNotEmpty && amount > 0) {
                  if (widget.sheetType == QuickActionSheetType.GOAL) {
                    widget.onAddGoal(title, amount, 0.0);
                  } else if (widget.sheetType == QuickActionSheetType.BUDGET) {
                    widget.onAddBudget(title, amount);
                  } else if (widget.sheetType == QuickActionSheetType.BILL) {
                    widget.onAddBill(title, amount, "General", _selectedAccountId);
                  }
                }
              },
              child: const Text("Save", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.black)),
            ),
          ),
        ],
      ),
    );
  }
}
