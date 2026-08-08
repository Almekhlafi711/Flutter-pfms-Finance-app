enum AccountType { BANK, CASH, SAVINGS, INVESTMENT, CREDIT_CARD, CRYPTO }

class Account {
  final String id;
  final String name;
  final AccountType type;
  final double balance;
  final String currency;
  final String accountNumber;
  final String colorHex;
  final String iconName;
  final bool isArchived;

  Account({
    required this.id,
    required this.name,
    required this.type,
    required this.balance,
    this.currency = "SAR",
    this.accountNumber = "",
    this.colorHex = "#0EA5E9",
    this.iconName = "bank",
    this.isArchived = false,
  });

  Account copyWith({
    String? id,
    String? name,
    AccountType? type,
    double? balance,
    String? currency,
    String? accountNumber,
    String? colorHex,
    String? iconName,
    bool? isArchived,
  }) {
    return Account(
      id: id ?? this.id,
      name: name ?? this.name,
      type: type ?? this.type,
      balance: balance ?? this.balance,
      currency: currency ?? this.currency,
      accountNumber: accountNumber ?? this.accountNumber,
      colorHex: colorHex ?? this.colorHex,
      iconName: iconName ?? this.iconName,
      isArchived: isArchived ?? this.isArchived,
    );
  }
}

class GroupedAccount {
  final String name;
  final AccountType type;
  final String colorHex;
  final String iconName;
  final bool isArchived;
  final List<Account> accounts;

  GroupedAccount({
    required this.name,
    required this.type,
    required this.colorHex,
    required this.iconName,
    required this.isArchived,
    required this.accounts,
  });

  double get totalBalance => accounts.fold(0.0, (sum, acc) => sum + acc.balance);
}

enum TransactionType {
  INCOME,
  EXPENSE,
  TRANSFER,
  ASSET_PURCHASE,
  ASSET_SALE,
  DEBT_PAYMENT,
  DEBT_CREATION,
  GOAL_CONTRIBUTION,
  BILL_PAYMENT
}

class Transaction {
  final String id;
  final TransactionType type;
  final double amount;
  final String currency;
  final String sourceAccountId;
  final String? destinationAccountId;
  final String category;
  final String? party;
  final int date;
  final String note;
  final String? relatedEntityId;
  final String status;

  Transaction({
    required this.id,
    required this.type,
    required this.amount,
    this.currency = "SAR",
    required this.sourceAccountId,
    this.destinationAccountId,
    required this.category,
    this.party,
    int? date,
    this.note = "",
    this.relatedEntityId,
    this.status = "COMPLETED",
  }) : date = date ?? DateTime.now().millisecondsSinceEpoch;

  Transaction copyWith({
    String? id,
    TransactionType? type,
    double? amount,
    String? currency,
    String? sourceAccountId,
    String? destinationAccountId,
    String? category,
    String? party,
    int? date,
    String? note,
    String? relatedEntityId,
    String? status,
  }) {
    return Transaction(
      id: id ?? this.id,
      type: type ?? this.type,
      amount: amount ?? this.amount,
      currency: currency ?? this.currency,
      sourceAccountId: sourceAccountId ?? this.sourceAccountId,
      destinationAccountId: destinationAccountId ?? this.destinationAccountId,
      category: category ?? this.category,
      party: party ?? this.party,
      date: date ?? this.date,
      note: note ?? this.note,
      relatedEntityId: relatedEntityId ?? this.relatedEntityId,
      status: status ?? this.status,
    );
  }
}

enum AssetType { REAL_ESTATE, VEHICLE, STOCKS, GOLD, CRYPTO, EQUIPMENT, OTHER }
enum AssetStatus { ACTIVE, SOLD }

class Asset {
  final String id;
  final String name;
  final AssetType type;
  final double purchaseValue;
  final double currentValue;
  final double quantity;
  final String unit;
  final String currency;
  final String notes;
  final int purchaseDate;
  final AssetStatus status;
  final String purchaseAccountId;
  final String purchaseAccountName;
  final double? soldPrice;
  final int? soldDate;
  final String? soldAccountId;
  final String? soldAccountName;

  Asset({
    required this.id,
    required this.name,
    required this.type,
    required this.purchaseValue,
    required this.currentValue,
    this.quantity = 1.0,
    this.unit = "Unit",
    this.currency = "SAR",
    this.notes = "",
    int? purchaseDate,
    this.status = AssetStatus.ACTIVE,
    this.purchaseAccountId = "",
    this.purchaseAccountName = "",
    this.soldPrice,
    this.soldDate,
    this.soldAccountId,
    this.soldAccountName,
  }) : purchaseDate = purchaseDate ?? DateTime.now().millisecondsSinceEpoch;

  double get totalPurchaseValue => purchaseValue * quantity;
  double get totalCurrentValue => currentValue * quantity;
  double get gainLossAmount => totalCurrentValue - totalPurchaseValue;
  double get gainLossPercentage => totalPurchaseValue > 0 ? (gainLossAmount / totalPurchaseValue) * 100.0 : 0.0;

  Asset copyWith({
    String? id,
    String? name,
    AssetType? type,
    double? purchaseValue,
    double? currentValue,
    double? quantity,
    String? unit,
    String? currency,
    String? notes,
    int? purchaseDate,
    AssetStatus? status,
    String? purchaseAccountId,
    String? purchaseAccountName,
    double? soldPrice,
    int? soldDate,
    String? soldAccountId,
    String? soldAccountName,
  }) {
    return Asset(
      id: id ?? this.id,
      name: name ?? this.name,
      type: type ?? this.type,
      purchaseValue: purchaseValue ?? this.purchaseValue,
      currentValue: currentValue ?? this.currentValue,
      quantity: quantity ?? this.quantity,
      unit: unit ?? this.unit,
      currency: currency ?? this.currency,
      notes: notes ?? this.notes,
      purchaseDate: purchaseDate ?? this.purchaseDate,
      status: status ?? this.status,
      purchaseAccountId: purchaseAccountId ?? this.purchaseAccountId,
      purchaseAccountName: purchaseAccountName ?? this.purchaseAccountName,
      soldPrice: soldPrice ?? this.soldPrice,
      soldDate: soldDate ?? this.soldDate,
      soldAccountId: soldAccountId ?? this.soldAccountId,
      soldAccountName: soldAccountName ?? this.soldAccountName,
    );
  }
}

enum AssetLogType { PURCHASE, VALUE_UPDATE, SALE, EXPENSE, INCOME }

class AssetLog {
  final String id;
  final String assetId;
  final AssetLogType type;
  final String title;
  final double amount;
  final double previousValue;
  final double newValue;
  final String accountName;
  final int date;
  final String notes;

  AssetLog({
    required this.id,
    required this.assetId,
    required this.type,
    required this.title,
    required this.amount,
    this.previousValue = 0.0,
    this.newValue = 0.0,
    this.accountName = "",
    int? date,
    this.notes = "",
  }) : date = date ?? DateTime.now().millisecondsSinceEpoch;
}

enum DebtType { RECEIVABLE, PAYABLE }
enum DebtStatus { ACTIVE, PARTIAL, COMPLETED, OVERDUE }

class Debt {
  final String id;
  final String debtAccountId;
  final String personId;
  final String partyName;
  final String? partyPhone;
  final DebtType type;
  final double originalAmount;
  final double remainingAmount;
  final String currency;
  final int dueDate;
  final DebtStatus status;
  final String notes;
  final int createdAt;

  Debt({
    required this.id,
    this.debtAccountId = "",
    this.personId = "",
    required this.partyName,
    this.partyPhone,
    required this.type,
    required this.originalAmount,
    required this.remainingAmount,
    this.currency = "SAR",
    int? dueDate,
    this.status = DebtStatus.ACTIVE,
    this.notes = "",
    int? createdAt,
  })  : dueDate = dueDate ?? (DateTime.now().millisecondsSinceEpoch + 30 * 24 * 3600 * 1000),
        createdAt = createdAt ?? DateTime.now().millisecondsSinceEpoch;

  double get paidAmount => originalAmount - remainingAmount;
  double get progress => originalAmount > 0 ? (paidAmount / originalAmount).clamp(0.0, 1.0) : 0.0;

  Debt copyWith({
    String? id,
    String? debtAccountId,
    String? personId,
    String? partyName,
    String? partyPhone,
    DebtType? type,
    double? originalAmount,
    double? remainingAmount,
    String? currency,
    int? dueDate,
    DebtStatus? status,
    String? notes,
    int? createdAt,
  }) {
    return Debt(
      id: id ?? this.id,
      debtAccountId: debtAccountId ?? this.debtAccountId,
      personId: personId ?? this.personId,
      partyName: partyName ?? this.partyName,
      partyPhone: partyPhone ?? this.partyPhone,
      type: type ?? this.type,
      originalAmount: originalAmount ?? this.originalAmount,
      remainingAmount: remainingAmount ?? this.remainingAmount,
      currency: currency ?? this.currency,
      dueDate: dueDate ?? this.dueDate,
      status: status ?? this.status,
      notes: notes ?? this.notes,
      createdAt: createdAt ?? this.createdAt,
    );
  }
}

class Budget {
  final String id;
  final String category;
  final double monthlyLimit;
  final double spentAmount;
  final String currency;
  final String period;
  final String? accountId;

  Budget({
    required this.id,
    required this.category,
    required this.monthlyLimit,
    this.spentAmount = 0.0,
    this.currency = "SAR",
    this.period = "MONTHLY",
    this.accountId,
  });

  double get remaining => monthlyLimit - spentAmount;
  double get usageRatio => monthlyLimit > 0 ? (spentAmount / monthlyLimit).clamp(0.0, 1.0) : 0.0;

  Budget copyWith({
    String? id,
    String? category,
    double? monthlyLimit,
    double? spentAmount,
    String? currency,
    String? period,
    String? accountId,
  }) {
    return Budget(
      id: id ?? this.id,
      category: category ?? this.category,
      monthlyLimit: monthlyLimit ?? this.monthlyLimit,
      spentAmount: spentAmount ?? this.spentAmount,
      currency: currency ?? this.currency,
      period: period ?? this.period,
      accountId: accountId ?? this.accountId,
    );
  }
}

class Goal {
  final String id;
  final String title;
  final double targetAmount;
  final double currentAmount;
  final String currency;
  final int targetDate;
  final String iconName;
  final String colorHex;
  final bool isCompleted;

  Goal({
    required this.id,
    required this.title,
    required this.targetAmount,
    this.currentAmount = 0.0,
    this.currency = "SAR",
    int? targetDate,
    this.iconName = "flag",
    this.colorHex = "#10B981",
    this.isCompleted = false,
  }) : targetDate = targetDate ?? (DateTime.now().millisecondsSinceEpoch + 180 * 24 * 3600 * 1000);

  double get progress => targetAmount > 0 ? (currentAmount / targetAmount).clamp(0.0, 1.0) : 0.0;

  Goal copyWith({
    String? id,
    String? title,
    double? targetAmount,
    double? currentAmount,
    String? currency,
    int? targetDate,
    String? iconName,
    String? colorHex,
    bool? isCompleted,
  }) {
    return Goal(
      id: id ?? this.id,
      title: title ?? this.title,
      targetAmount: targetAmount ?? this.targetAmount,
      currentAmount: currentAmount ?? this.currentAmount,
      currency: currency ?? this.currency,
      targetDate: targetDate ?? this.targetDate,
      iconName: iconName ?? this.iconName,
      colorHex: colorHex ?? this.colorHex,
      isCompleted: isCompleted ?? this.isCompleted,
    );
  }
}

enum BillStatus { UPCOMING, SCHEDULED, PAID, OVERDUE }

class Bill {
  final String id;
  final String title;
  final double amount;
  final String currency;
  final String frequency;
  final int nextDueDate;
  final String category;
  final String accountId;
  final BillStatus status;
  final bool isAutoPay;

  Bill({
    required this.id,
    required this.title,
    required this.amount,
    this.currency = "SAR",
    this.frequency = "MONTHLY",
    int? nextDueDate,
    required this.category,
    this.accountId = "",
    this.status = BillStatus.UPCOMING,
    this.isAutoPay = false,
  }) : nextDueDate = nextDueDate ?? (DateTime.now().millisecondsSinceEpoch + 14 * 24 * 3600 * 1000);

  Bill copyWith({
    String? id,
    String? title,
    double? amount,
    String? currency,
    String? frequency,
    int? nextDueDate,
    String? category,
    String? accountId,
    BillStatus? status,
    bool? isAutoPay,
  }) {
    return Bill(
      id: id ?? this.id,
      title: title ?? this.title,
      amount: amount ?? this.amount,
      currency: currency ?? this.currency,
      frequency: frequency ?? this.frequency,
      nextDueDate: nextDueDate ?? this.nextDueDate,
      category: category ?? this.category,
      accountId: accountId ?? this.accountId,
      status: status ?? this.status,
      isAutoPay: isAutoPay ?? this.isAutoPay,
    );
  }
}

class NetWorthSummary {
  final double totalCashAndAccounts;
  final double totalAssetsValue;
  final double totalReceivables;
  final double totalLiabilitiesAndPayables;

  NetWorthSummary({
    required this.totalCashAndAccounts,
    required this.totalAssetsValue,
    required this.totalReceivables,
    required this.totalLiabilitiesAndPayables,
  });

  double get totalAssets => totalCashAndAccounts + totalAssetsValue + totalReceivables;
  double get netWorth => totalAssets - totalLiabilitiesAndPayables;
}
