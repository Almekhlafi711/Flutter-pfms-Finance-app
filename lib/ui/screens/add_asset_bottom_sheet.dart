import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../domain/models/domain_models.dart';
import '../viewmodel/pfms_provider.dart';

class AddAssetBottomSheet extends StatefulWidget {
  final PfmsController viewModel;
  final List<Account> accounts;
  final bool isArabic;
  final VoidCallback onDismiss;

  const AddAssetBottomSheet({
    super.key,
    required this.viewModel,
    required this.accounts,
    required this.isArabic,
    required this.onDismiss,
  });

  @override
  State<AddAssetBottomSheet> createState() => _AddAssetBottomSheetState();
}

class _AddAssetBottomSheetState extends State<AddAssetBottomSheet> {
  final _nameController = TextEditingController();
  final _purchaseValController = TextEditingController();
  final _currentValController = TextEditingController();
  AssetType _selectedType = AssetType.REAL_ESTATE;
  late String _selectedAccountId;

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
                widget.isArabic ? "إضافة أصل جديد" : "Add New Asset",
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark),
              ),
              IconButton(icon: const Icon(Icons.close, color: AppColors.textSecondaryDark), onPressed: widget.onDismiss),
            ],
          ),
          const SizedBox(height: DesignTokens.spaceMedium),
          TextField(
            controller: _nameController,
            style: const TextStyle(color: AppColors.textPrimaryDark),
            decoration: InputDecoration(
              labelText: widget.isArabic ? "اسم الأصل" : "Asset Name",
              filled: true,
              fillColor: AppColors.slateDarkCard,
            ),
          ),
          const SizedBox(height: DesignTokens.spaceSmall),
          DropdownButtonFormField<AssetType>(
            value: _selectedType,
            dropdownColor: AppColors.slateDarkCard,
            style: const TextStyle(color: AppColors.textPrimaryDark),
            decoration: InputDecoration(
              labelText: widget.isArabic ? "نوع الأصل" : "Asset Type",
              filled: true,
              fillColor: AppColors.slateDarkCard,
            ),
            items: AssetType.values.map((t) => DropdownMenuItem(value: t, child: Text(t.name))).toList(),
            onChanged: (val) {
              if (val != null) setState(() => _selectedType = val);
            },
          ),
          const SizedBox(height: DesignTokens.spaceSmall),
          TextField(
            controller: _purchaseValController,
            keyboardType: TextInputType.number,
            style: const TextStyle(color: AppColors.textPrimaryDark),
            decoration: InputDecoration(
              labelText: widget.isArabic ? "قيمة الشراء" : "Purchase Value",
              filled: true,
              fillColor: AppColors.slateDarkCard,
            ),
          ),
          const SizedBox(height: DesignTokens.spaceSmall),
          TextField(
            controller: _currentValController,
            keyboardType: TextInputType.number,
            style: const TextStyle(color: AppColors.textPrimaryDark),
            decoration: InputDecoration(
              labelText: widget.isArabic ? "القيمة الحالية" : "Current Value",
              filled: true,
              fillColor: AppColors.slateDarkCard,
            ),
          ),
          const SizedBox(height: DesignTokens.spaceLarge),
          SizedBox(
            width: double.infinity,
            height: 48,
            child: ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.purpleAsset,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(DesignTokens.radiusMedium)),
              ),
              onPressed: () {
                final name = _nameController.text.trim();
                final pVal = double.tryParse(_purchaseValController.text) ?? 0.0;
                final cVal = double.tryParse(_currentValController.text) ?? pVal;
                if (name.isNotEmpty && pVal > 0) {
                  widget.viewModel.addAsset(name, _selectedType, pVal, cVal, _selectedAccountId);
                }
              },
              child: Text(
                widget.isArabic ? "حفظ الأصل" : "Save Asset",
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.white),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
