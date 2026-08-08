import 'domain_models.dart';
import 'person.dart';

class DebtLedgerEntry {
  final String id;
  final String debtAccountId;
  final String debtId;
  final String personId;
  final DebtType type;
  final bool isPayment;
  final double amount;
  final int date;
  final String description;
  final String category;
  final String paymentMethod;
  final String accountId;
  final String status;

  DebtLedgerEntry({
    required this.id,
    this.debtAccountId = "",
    this.debtId = "",
    this.personId = "",
    required this.type,
    this.isPayment = false,
    required this.amount,
    int? date,
    this.description = "",
    this.category = "General",
    this.paymentMethod = "Cash / Bank",
    this.accountId = "",
    this.status = "CLEARED",
  }) : date = date ?? DateTime.now().millisecondsSinceEpoch;

  DebtLedgerEntry copyWith({
    String? id,
    String? debtAccountId,
    String? debtId,
    String? personId,
    DebtType? type,
    bool? isPayment,
    double? amount,
    int? date,
    String? description,
    String? category,
    String? paymentMethod,
    String? accountId,
    String? status,
  }) {
    return DebtLedgerEntry(
      id: id ?? this.id,
      debtAccountId: debtAccountId ?? this.debtAccountId,
      debtId: debtId ?? this.debtId,
      personId: personId ?? this.personId,
      type: type ?? this.type,
      isPayment: isPayment ?? this.isPayment,
      amount: amount ?? this.amount,
      date: date ?? this.date,
      description: description ?? this.description,
      category: category ?? this.category,
      paymentMethod: paymentMethod ?? this.paymentMethod,
      accountId: accountId ?? this.accountId,
      status: status ?? this.status,
    );
  }
}

class PersonDebtAccount {
  final String id;
  final String personId;
  final Person person;
  final String currency;
  final String notes;
  final bool isActive;
  final int createdAt;
  final Debt mainDebt;
  final List<DebtLedgerEntry> entries;

  PersonDebtAccount({
    required this.id,
    required this.personId,
    required this.person,
    this.currency = "SAR",
    this.notes = "",
    this.isActive = true,
    int? createdAt,
    required this.mainDebt,
    this.entries = const [],
  }) : createdAt = createdAt ?? DateTime.now().millisecondsSinceEpoch;

  double get totalOriginalAmount => mainDebt.originalAmount;
  double get totalRemainingAmount => mainDebt.remainingAmount;
  double get totalPaidAmount => totalOriginalAmount - totalRemainingAmount;

  PersonDebtAccount copyWith({
    String? id,
    String? personId,
    Person? person,
    String? currency,
    String? notes,
    bool? isActive,
    int? createdAt,
    Debt? mainDebt,
    List<DebtLedgerEntry>? entries,
  }) {
    return PersonDebtAccount(
      id: id ?? this.id,
      personId: personId ?? this.personId,
      person: person ?? this.person,
      currency: currency ?? this.currency,
      notes: notes ?? this.notes,
      isActive: isActive ?? this.isActive,
      createdAt: createdAt ?? this.createdAt,
      mainDebt: mainDebt ?? this.mainDebt,
      entries: entries ?? this.entries,
    );
  }
}

enum LedgerOperationType { ADD_DEBT, RECEIVE_PAYMENT, PAY_DEBT }
