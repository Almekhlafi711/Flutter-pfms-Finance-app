import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../core/util/currency_formatter.dart';
import '../../domain/models/domain_models.dart';
import '../viewmodel/pfms_provider.dart';

class AssetsScreen extends ConsumerWidget {
  final VoidCallback onNavigateBack;

  const AssetsScreen({super.key, required this.onNavigateBack});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final controller = PfmsController(ref);
    final assets = ref.watch(assetsStreamProvider).value ?? [];
    final isArabic = ref.watch(isArabicProvider);

    final totalAssetsVal = assets.fold(0.0, (sum, a) => sum + a.totalCurrentValue);

    return Scaffold(
      appBar: AppBar(
        title: Text(isArabic ? "الأصول والممتلكات" : "Assets Portfolio"),
        leading: IconButton(icon: const Icon(Icons.arrow_back), onPressed: onNavigateBack),
      ),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: AppColors.purpleAsset,
        onPressed: () => controller.openBottomSheet(QuickActionSheetType.ASSET),
        icon: const Icon(Icons.add, color: Colors.white),
        label: Text(isArabic ? "أصل جديد" : "New Asset", style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            Card(
              margin: const EdgeInsets.all(DesignTokens.spaceMedium),
              color: AppColors.slateDarkCard,
              child: Padding(
                padding: const EdgeInsets.all(DesignTokens.spaceLarge),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(isArabic ? "إجمالي قيمة الأصول" : "Total Assets Value", style: const TextStyle(color: AppColors.textSecondaryDark)),
                        const SizedBox(height: 4),
                        Text(
                          CurrencyFormatter.format(totalAssetsVal, "SAR"),
                          style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: AppColors.purpleAsset),
                        ),
                      ],
                    ),
                    const Icon(Icons.home_work_outlined, color: AppColors.purpleAsset, size: 36),
                  ],
                ),
              ),
            ),
            ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              padding: const EdgeInsets.symmetric(horizontal: DesignTokens.spaceMedium),
              itemCount: assets.length,
              itemBuilder: (context, index) {
                final asset = assets[index];
                return Card(
                  color: AppColors.slateDarkCard,
                  margin: const EdgeInsets.only(bottom: DesignTokens.spaceSmall),
                  child: ListTile(
                    leading: CircleAvatar(
                      backgroundColor: AppColors.purpleAsset.withOpacity(0.2),
                      child: const Icon(Icons.home_work, color: AppColors.purpleAsset),
                    ),
                    title: Text(asset.name, style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark)),
                    subtitle: Text("${asset.type.name} • ${asset.quantity} ${asset.unit}", style: const TextStyle(color: AppColors.textMutedDark)),
                    trailing: Text(
                      CurrencyFormatter.format(asset.totalCurrentValue, asset.currency),
                      style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark),
                    ),
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}
