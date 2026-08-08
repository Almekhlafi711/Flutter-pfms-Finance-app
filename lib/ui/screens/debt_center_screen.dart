import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../domain/models/debt_models.dart';
import '../../domain/models/domain_models.dart';
import '../components/debt_card.dart';
import '../components/debt_filter_tabs.dart';
import '../components/debt_search_bar.dart';
import '../components/debt_summary_card.dart';
import '../viewmodel/pfms_provider.dart';

class DebtCenterScreen extends ConsumerStatefulWidget {
  final VoidCallback onNavigateBack;

  const DebtCenterScreen({super.key, required this.onNavigateBack});

  @override
  ConsumerState<DebtCenterScreen> createState() => _DebtCenterScreenState();
}

class _DebtCenterScreenState extends ConsumerState<DebtCenterScreen> {
  String _searchQuery = "";
  int _selectedTabIndex = 0; // 0 = All, 1 = Receivables, 2 = Payables

  @override
  Widget build(BuildContext context) {
    final controller = PfmsController(ref);
    final debtAccounts = ref.watch(personDebtAccountsProvider);
    final netWorth = ref.watch(netWorthSummaryProvider);
    final isArabic = ref.watch(isArabicProvider);

    final filteredDebts = debtAccounts.where((dac) {
      final matchesSearch = dac.person.name.toLowerCase().contains(_searchQuery.toLowerCase());
      if (!matchesSearch) return false;
      if (_selectedTabIndex == 1) return dac.mainDebt.type == DebtType.RECEIVABLE;
      if (_selectedTabIndex == 2) return dac.mainDebt.type == DebtType.PAYABLE;
      return true;
    }).toList();

    return Scaffold(
      appBar: AppBar(
        title: Text(isArabic ? "مركز الديون والمستحقات" : "Debt & Settlement Center"),
        leading: IconButton(icon: const Icon(Icons.arrow_back), onPressed: widget.onNavigateBack),
      ),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: AppColors.orangeDebt,
        onPressed: () => controller.openBottomSheet(QuickActionSheetType.DEBT),
        icon: const Icon(Icons.add, color: Colors.white),
        label: Text(isArabic ? "تسجيل دين جديد" : "Record Debt", style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            DebtSummaryCard(
              totalReceivables: netWorth.totalReceivables,
              totalPayables: netWorth.totalLiabilitiesAndPayables,
              isArabic: isArabic,
            ),
            DebtSearchBar(
              query: _searchQuery,
              onChanged: (q) => setState(() => _searchQuery = q),
              onOpenFilter: () {},
              isArabic: isArabic,
            ),
            const SizedBox(height: DesignTokens.spaceSmall),
            DebtFilterTabs(
              selectedIndex: _selectedTabIndex,
              onTabSelected: (idx) => setState(() => _selectedTabIndex = idx),
              isArabic: isArabic,
            ),
            const SizedBox(height: DesignTokens.spaceMedium),
            ListView.builder(
              shrinkWrap: true,
              physics: const NeverScrollablePhysics(),
              padding: const EdgeInsets.symmetric(horizontal: DesignTokens.spaceMedium),
              itemCount: filteredDebts.length,
              itemBuilder: (context, index) {
                final dac = filteredDebts[index];
                return DebtCard(
                  debtAccount: dac,
                  onTap: () {
                    _showDebtDetailsSheet(context, dac, controller, isArabic);
                  },
                  isArabic: isArabic,
                );
              },
            ),
            const SizedBox(height: DesignTokens.spaceHuge),
          ],
        ),
      ),
    );
  }

  void _showDebtDetailsSheet(BuildContext context, PersonDebtAccount dac, PfmsController controller, bool isArabic) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.slateDarkSurface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(DesignTokens.radiusLarge))),
      builder: (context) {
        return Container(
          padding: const EdgeInsets.all(DesignTokens.spaceMedium),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(dac.person.name, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark)),
              const SizedBox(height: 10),
              Text("${isArabic ? 'المبلغ الأصلي' : 'Original'}: ${dac.totalOriginalAmount} ${dac.currency}"),
              Text("${isArabic ? 'المبلغ المتبقي' : 'Remaining'}: ${dac.totalRemainingAmount} ${dac.currency}"),
              const SizedBox(height: 20),
              ElevatedButton(
                style: ElevatedButton.styleFrom(backgroundColor: AppColors.tealAccent),
                onPressed: () {
                  Navigator.pop(context);
                  controller.openBottomSheet(QuickActionSheetType.DEBT);
                },
                child: Text(isArabic ? "تسجيل سداد" : "Record Payment", style: const TextStyle(color: Colors.black)),
              ),
            ],
          ),
        );
      },
    );
  }
}
