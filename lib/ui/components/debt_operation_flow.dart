import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../../domain/models/debt_models.dart';
import '../../domain/models/domain_models.dart';
import '../../domain/models/person.dart';

class DebtOperationFlow extends StatefulWidget {
  final List<Person> persons;
  final List<Account> accounts;
  final List<PersonDebtAccount> personDebtAccounts;
  final bool isArabic;
  final VoidCallback onDismiss;
  final Function(Person person, String currency, LedgerOperationType opType, DebtType? direction, double amount, String accountId, String notes) onExecuteOperation;
  final Function(Person person) onCreatePerson;

  const DebtOperationFlow({
    super.key,
    required this.persons,
    required this.accounts,
    required this.personDebtAccounts,
    required this.isArabic,
    required this.onDismiss,
    required this.onExecuteOperation,
    required this.onCreatePerson,
  });

  @override
  State<DebtOperationFlow> createState() => _DebtOperationFlowState();
}

class _DebtOperationFlowState extends State<DebtOperationFlow> {
  final _personNameController = TextEditingController();
  final _amountController = TextEditingController();
  final _notesController = TextEditingController();

  LedgerOperationType _opType = LedgerOperationType.ADD_DEBT;
  DebtType _debtDirection = DebtType.RECEIVABLE;
  String _selectedAccountId = "";
  String _selectedCurrency = "SAR";

  @override
  void initState() {
    super.initState();
    if (widget.accounts.isNotEmpty) {
      _selectedAccountId = widget.accounts.first.id;
    }
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
                widget.isArabic ? "عمليات الدين والتصفية" : "Debt Operation & Settlement",
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.textPrimaryDark),
              ),
              IconButton(icon: const Icon(Icons.close, color: AppColors.textSecondaryDark), onPressed: widget.onDismiss),
            ],
          ),
          const SizedBox(height: DesignTokens.spaceMedium),
          DropdownButtonFormField<LedgerOperationType>(
            value: _opType,
            dropdownColor: AppColors.slateDarkCard,
            style: const TextStyle(color: AppColors.textPrimaryDark),
            decoration: InputDecoration(
              labelText: widget.isArabic ? "نوع الإجراء" : "Operation Type",
              filled: true,
              fillColor: AppColors.slateDarkCard,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(DesignTokens.radiusMedium)),
            ),
            items: [
              DropdownMenuItem(value: LedgerOperationType.ADD_DEBT, child: Text(widget.isArabic ? "تسجيل دين جديد" : "Record New Debt")),
              DropdownMenuItem(value: LedgerOperationType.RECEIVE_PAYMENT, child: Text(widget.isArabic ? "استلام سداد (لك)" : "Receive Debt Payment")),
              DropdownMenuItem(value: LedgerOperationType.PAY_DEBT, child: Text(widget.isArabic ? "سداد دين (عليك)" : "Pay Debt")),
            ],
            onChanged: (val) {
              if (val != null) setState(() => _opType = val);
            },
          ),
          if (_opType == LedgerOperationType.ADD_DEBT) ...[
            const SizedBox(height: DesignTokens.spaceSmall),
            Row(
              children: [
                Expanded(
                  child: ChoiceChip(
                    label: Text(widget.isArabic ? "لك (دين مستحق)" : "Receivable"),
                    selected: _debtDirection == DebtType.RECEIVABLE,
                    onSelected: (_) => setState(() => _debtDirection = DebtType.RECEIVABLE),
                  ),
                ),
                const SizedBox(width: DesignTokens.spaceSmall),
                Expanded(
                  child: ChoiceChip(
                    label: Text(widget.isArabic ? "عليك (دين التزام)" : "Payable"),
                    selected: _debtDirection == DebtType.PAYABLE,
                    onSelected: (_) => setState(() => _debtDirection = DebtType.PAYABLE),
                  ),
                ),
              ],
            ),
          ],
          const SizedBox(height: DesignTokens.spaceSmall),
          TextField(
            controller: _personNameController,
            style: const TextStyle(color: AppColors.textPrimaryDark),
            decoration: InputDecoration(
              labelText: widget.isArabic ? "اسم الشخص / الجهة" : "Person / Party Name",
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
              labelText: widget.isArabic ? "المبلغ" : "Amount",
              filled: true,
              fillColor: AppColors.slateDarkCard,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(DesignTokens.radiusMedium)),
            ),
          ),
          const SizedBox(height: DesignTokens.spaceSmall),
          DropdownButtonFormField<String>(
            value: _selectedAccountId,
            dropdownColor: AppColors.slateDarkCard,
            style: const TextStyle(color: AppColors.textPrimaryDark),
            decoration: InputDecoration(
              labelText: widget.isArabic ? "الحساب المرتبط" : "Linked Account",
              filled: true,
              fillColor: AppColors.slateDarkCard,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(DesignTokens.radiusMedium)),
            ),
            items: widget.accounts.map((acc) => DropdownMenuItem(value: acc.id, child: Text(acc.name))).toList(),
            onChanged: (val) {
              if (val != null) setState(() => _selectedAccountId = val);
            },
          ),
          const SizedBox(height: DesignTokens.spaceSmall),
          TextField(
            controller: _notesController,
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
                final name = _personNameController.text.trim();
                final amount = double.tryParse(_amountController.text) ?? 0.0;
                if (name.isNotEmpty && amount > 0) {
                  final person = Person(id: "prs_${name.hashCode}", name: name);
                  widget.onExecuteOperation(
                    person,
                    _selectedCurrency,
                    _opType,
                    _opType == LedgerOperationType.ADD_DEBT ? _debtDirection : null,
                    amount,
                    _selectedAccountId,
                    _notesController.text,
                  );
                }
              },
              child: Text(
                widget.isArabic ? "تنفيذ الإجراء" : "Execute Operation",
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.black),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
