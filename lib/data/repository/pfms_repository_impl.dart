import 'dart:async';
import 'package:uuid/uuid.dart';
import '../../domain/models/domain_models.dart';
import '../../domain/models/debt_models.dart';
import '../../domain/models/person.dart';
import '../../domain/repository/pfms_repository.dart';

class PfmsRepositoryImpl implements PfmsRepository {
  final _uuid = const Uuid();

  final List<Account> _accounts = [];
  final List<Transaction> _transactions = [];
  final List<Asset> _assets = [];
  final List<AssetLog> _assetLogs = [];
  final List<Debt> _debts = [];
  final List<Budget> _budgets = [];
  final List<Goal> _goals = [];
  final List<Bill> _bills = [];
  final List<Person> _persons = [];

  final _accountsController = StreamController<List<Account>>.broadcast();
  final _transactionsController = StreamController<List<Transaction>>.broadcast();
  final _assetsController = StreamController<List<Asset>>.broadcast();
  final _assetLogsController = StreamController<List<AssetLog>>.broadcast();
  final _debtsController = StreamController<List<Debt>>.broadcast();
  final _budgetsController = StreamController<List<Budget>>.broadcast();
  final _goalsController = StreamController<List<Goal>>.broadcast();
  final _billsController = StreamController<List<Bill>>.broadcast();
  final _personsController = StreamController<List<Person>>.broadcast();

  void _notifyAccounts() => _accountsController.add(List.unmodifiable(_accounts));
  void _notifyTransactions() => _transactionsController.add(List.unmodifiable(_transactions));
  void _notifyAssets() => _assetsController.add(List.unmodifiable(_assets));
  void _notifyAssetLogs() => _assetLogsController.add(List.unmodifiable(_assetLogs));
  void _notifyDebts() => _debtsController.add(List.unmodifiable(_debts));
  void _notifyBudgets() {
    // Dynamically calculate spent amount from transactions
    final updatedBudgets = _budgets.map((b) {
      final spent = _transactions
          .where((tx) => tx.type == TransactionType.EXPENSE && tx.category.toLowerCase() == b.category.toLowerCase())
          .fold(0.0, (sum, tx) => sum + tx.amount);
      return b.copyWith(spentAmount: spent);
    }).toList();
    _budgetsController.add(List.unmodifiable(updatedBudgets));
  }
  void _notifyGoals() => _goalsController.add(List.unmodifiable(_goals));
  void _notifyBills() => _billsController.add(List.unmodifiable(_bills));
  void _notifyPersons() => _personsController.add(List.unmodifiable(_persons));

  @override
  Stream<List<Account>> getAccounts() async* {
    yield List.unmodifiable(_accounts);
    yield* _accountsController.stream;
  }

  @override
  Stream<List<Transaction>> getTransactions() async* {
    yield List.unmodifiable(_transactions);
    yield* _transactionsController.stream;
  }

  @override
  Stream<List<Transaction>> getTransactionsForAccount(String accountId) async* {
    yield List.unmodifiable(_transactions.where((tx) => tx.sourceAccountId == accountId || tx.destinationAccountId == accountId).toList());
    yield* _transactionsController.stream.map((list) => list.where((tx) => tx.sourceAccountId == accountId || tx.destinationAccountId == accountId).toList());
  }

  @override
  Stream<List<Asset>> getAssets() async* {
    yield List.unmodifiable(_assets);
    yield* _assetsController.stream;
  }

  @override
  Stream<List<Debt>> getDebts() async* {
    yield List.unmodifiable(_debts);
    yield* _debtsController.stream;
  }

  @override
  Stream<List<Budget>> getBudgets() async* {
    final updatedBudgets = _budgets.map((b) {
      final spent = _transactions
          .where((tx) => tx.type == TransactionType.EXPENSE && tx.category.toLowerCase() == b.category.toLowerCase())
          .fold(0.0, (sum, tx) => sum + tx.amount);
      return b.copyWith(spentAmount: spent);
    }).toList();
    yield List.unmodifiable(updatedBudgets);
    yield* _budgetsController.stream;
  }

  @override
  Stream<List<Goal>> getGoals() async* {
    yield List.unmodifiable(_goals);
    yield* _goalsController.stream;
  }

  @override
  Stream<List<Bill>> getBills() async* {
    yield List.unmodifiable(_bills);
    yield* _billsController.stream;
  }

  @override
  Stream<List<Person>> getPersons() async* {
    yield List.unmodifiable(_persons);
    yield* _personsController.stream;
  }

  @override
  Stream<List<AssetLog>> getAssetLogs(String assetId) async* {
    yield List.unmodifiable(_assetLogs.where((log) => log.assetId == assetId).toList());
    yield* _assetLogsController.stream.map((list) => list.where((log) => log.assetId == assetId).toList());
  }

  @override
  Future<void> addAccount(Account account) async {
    if (account.name.trim().isEmpty) {
      throw ArgumentError("اسم الحساب لا يمكن أن يكون فارغاً / Account name cannot be empty");
    }
    if (account.balance < 0.0 && account.type != AccountType.CREDIT_CARD) {
      throw ArgumentError("لا يمكن بدء حساب عادي برصيد سالب / Standard account cannot start with a negative balance");
    }
    _accounts.add(account);
    _notifyAccounts();
  }

  @override
  Future<void> updateAccount(Account account) async {
    if (account.name.trim().isEmpty) {
      throw ArgumentError("اسم الحساب لا يمكن أن يكون فارغاً / Account name cannot be empty");
    }
    final index = _accounts.indexWhere((a) => a.id == account.id);
    if (index != -1) {
      _accounts[index] = account;
      _notifyAccounts();
    }
  }

  @override
  Future<void> archiveAccount(String accountId) async {
    final index = _accounts.indexWhere((a) => a.id == accountId);
    if (index != -1) {
      _accounts[index] = _accounts[index].copyWith(isArchived: true);
      _notifyAccounts();
    }
  }

  @override
  Future<bool> isAccountInUse(String accountId) async {
    final hasTx = _transactions.any((tx) => tx.sourceAccountId == accountId || tx.destinationAccountId == accountId);
    final hasBill = _bills.any((b) => b.accountId == accountId);
    return hasTx || hasBill;
  }

  @override
  Future<void> deleteAccount(String accountId) async {
    if (await isAccountInUse(accountId)) {
      throw StateError("AccountInUseException: Cannot delete account because it is referenced in transactions or records.");
    }
    _accounts.removeWhere((a) => a.id == accountId);
    _notifyAccounts();
  }

  @override
  Future<void> addTransaction(Transaction transaction) async {
    if (transaction.amount <= 0.0) {
      throw ArgumentError("مبلغ العملية يجب أن يكون أكبر من الصفر / Transaction amount must be greater than zero");
    }

    final sourceIdx = _accounts.indexWhere((a) => a.id == transaction.sourceAccountId);
    if (sourceIdx == -1) {
      throw ArgumentError("الحساب المصدر غير موجود / Source account does not exist");
    }
    final sourceAccount = _accounts[sourceIdx];

    if (transaction.currency != sourceAccount.currency) {
      throw ArgumentError("عملة العملية (${transaction.currency}) لا تطابق عملة الحساب (${sourceAccount.currency}) / Currency mismatch");
    }

    final isWithdraw = [
      TransactionType.EXPENSE,
      TransactionType.TRANSFER,
      TransactionType.GOAL_CONTRIBUTION,
      TransactionType.BILL_PAYMENT,
      TransactionType.ASSET_PURCHASE,
      TransactionType.DEBT_CREATION
    ].contains(transaction.type);

    if (isWithdraw && sourceAccount.balance < transaction.amount && sourceAccount.type != AccountType.CREDIT_CARD) {
      throw StateError("رصيد غير كافٍ في الحساب '${sourceAccount.name}' / Insufficient balance");
    }

    if (transaction.type == TransactionType.TRANSFER) {
      final destId = transaction.destinationAccountId;
      if (destId == null || destId.isEmpty) {
        throw ArgumentError("يجب تحديد الحساب المستلم للتحويل / Destination account must be specified for transfer");
      }
      if (destId == transaction.sourceAccountId) {
        throw ArgumentError("لا يمكن التحويل لنفس الحساب / Cannot transfer to the same account");
      }
      final destIdx = _accounts.indexWhere((a) => a.id == destId);
      if (destIdx == -1) {
        throw ArgumentError("الحساب المستلم غير موجود / Destination account does not exist");
      }
      final destAccount = _accounts[destIdx];
      if (sourceAccount.currency != destAccount.currency) {
        throw ArgumentError("لا يمكن التحويل بين عملات مختلفة مباشرة / Cannot transfer directly between different currencies");
      }
    }

    _transactions.insert(0, transaction);

    // Adjust balances
    final amount = (transaction.amount * 100.0).roundToDouble() / 100.0;
    switch (transaction.type) {
      case TransactionType.INCOME:
        _accounts[sourceIdx] = sourceAccount.copyWith(balance: sourceAccount.balance + amount);
        break;
      case TransactionType.EXPENSE:
      case TransactionType.GOAL_CONTRIBUTION:
      case TransactionType.BILL_PAYMENT:
      case TransactionType.ASSET_PURCHASE:
      case TransactionType.DEBT_CREATION:
        _accounts[sourceIdx] = sourceAccount.copyWith(balance: sourceAccount.balance - amount);
        break;
      case TransactionType.TRANSFER:
        _accounts[sourceIdx] = sourceAccount.copyWith(balance: sourceAccount.balance - amount);
        if (transaction.destinationAccountId != null) {
          final destIdx = _accounts.indexWhere((a) => a.id == transaction.destinationAccountId);
          if (destIdx != -1) {
            _accounts[destIdx] = _accounts[destIdx].copyWith(balance: _accounts[destIdx].balance + amount);
          }
        }
        break;
      case TransactionType.ASSET_SALE:
        _accounts[sourceIdx] = sourceAccount.copyWith(balance: sourceAccount.balance + amount);
        break;
      case TransactionType.DEBT_PAYMENT:
        final debtId = transaction.relatedEntityId;
        if (debtId == null) throw ArgumentError("معرّف الدين مطلوب لسداد الدين / Debt ID is required for debt payment");
        final debt = _debts.firstWhere((d) => d.id == debtId, orElse: () => throw ArgumentError("الدين غير موجود / Debt not found"));
        final delta = debt.type == DebtType.RECEIVABLE ? amount : -amount;
        _accounts[sourceIdx] = sourceAccount.copyWith(balance: sourceAccount.balance + delta);
        break;
    }

    _notifyAccounts();
    _notifyTransactions();
    _notifyBudgets();
  }

  @override
  Future<void> updateTransaction(Transaction transaction) async {
    final idx = _transactions.indexWhere((t) => t.id == transaction.id);
    if (idx != -1) {
      _transactions[idx] = transaction;
      _notifyTransactions();
      _notifyBudgets();
    }
  }

  @override
  Future<void> deleteTransaction(String id) async {
    _transactions.removeWhere((t) => t.id == id);
    _notifyTransactions();
    _notifyBudgets();
  }

  @override
  Future<void> addAsset(Asset asset) async {
    if (asset.name.trim().isEmpty) {
      throw ArgumentError("اسم الأصل مطلوب / Asset name cannot be empty");
    }
    if (asset.purchaseValue <= 0.0) {
      throw ArgumentError("قيمة الشراء للأصل يجب أن تكون إيجابية / Purchase value must be positive");
    }
    if (asset.quantity <= 0.0) {
      throw ArgumentError("كمية الأصل يجب أن تكون أكبر من الصفر / Quantity must be greater than zero");
    }
    _assets.add(asset);
    _notifyAssets();
  }

  @override
  Future<void> updateAsset(Asset asset) async {
    if (asset.name.trim().isEmpty) {
      throw ArgumentError("اسم الأصل مطلوب / Asset name cannot be empty");
    }
    final idx = _assets.indexWhere((a) => a.id == asset.id);
    if (idx != -1) {
      _assets[idx] = asset;
      _notifyAssets();
    }
  }

  @override
  Future<void> deleteAsset(String id) async {
    _assets.removeWhere((a) => a.id == id);
    _notifyAssets();
  }

  @override
  Future<void> addAssetLog(AssetLog log) async {
    _assetLogs.insert(0, log);
    _notifyAssetLogs();
  }

  @override
  Future<void> addPerson(Person person) async {
    if (person.name.trim().isEmpty) {
      throw ArgumentError("اسم الشخص مطلوب / Person name cannot be empty");
    }
    final idx = _persons.indexWhere((p) => p.id == person.id);
    if (idx != -1) {
      _persons[idx] = person;
    } else {
      _persons.add(person);
    }
    _notifyPersons();
  }

  @override
  Future<void> updatePerson(Person person) async {
    if (person.name.trim().isEmpty) {
      throw ArgumentError("اسم الشخص مطلوب / Person name cannot be empty");
    }
    final idx = _persons.indexWhere((p) => p.id == person.id);
    if (idx != -1) {
      _persons[idx] = person;
      _notifyPersons();
    }
  }

  @override
  Future<void> addDebt(Debt debt) async {
    if (debt.partyName.trim().isEmpty) {
      throw ArgumentError("اسم العميل أو الجهة مطلوب / Party name cannot be empty");
    }
    if (debt.originalAmount < 0.0 || debt.remainingAmount < 0.0) {
      throw ArgumentError("مبلغ الدين لا يمكن أن يكون سالباً / Debt amount cannot be negative");
    }
    if (debt.remainingAmount > debt.originalAmount) {
      throw ArgumentError("المبلغ المتبقي لا يمكن أن يتجاوز مبلغ الدين الأصلي / Remaining amount cannot exceed original amount");
    }
    final idx = _debts.indexWhere((d) => d.id == debt.id);
    if (idx != -1) {
      _debts[idx] = debt;
    } else {
      _debts.add(debt);
    }
    _notifyDebts();
  }

  @override
  Future<void> recordDebtPayment(String debtId, double paymentAmount, String accountId) async {
    if (paymentAmount <= 0.0) {
      throw ArgumentError("مبلغ السداد يجب أن يكون إيجابياً / Payment amount must be positive");
    }
    final debtIdx = _debts.indexWhere((d) => d.id == debtId);
    if (debtIdx == -1) {
      throw ArgumentError("الدين غير موجود / Debt not found");
    }
    final debt = _debts[debtIdx];
    if (paymentAmount > debt.remainingAmount) {
      throw ArgumentError("مبلغ السداد ($paymentAmount) يتجاوز المبلغ المتبقي من الدين (${debt.remainingAmount}) / Payment exceeds remaining debt");
    }

    final updatedRemaining = (debt.remainingAmount - paymentAmount).clamp(0.0, double.infinity);
    final updatedStatus = updatedRemaining == 0.0 ? DebtStatus.COMPLETED : DebtStatus.PARTIAL;
    final updatedDebt = debt.copyWith(remainingAmount: updatedRemaining, status: updatedStatus);
    _debts[debtIdx] = updatedDebt;
    _notifyDebts();

    final tx = Transaction(
      id: _uuid.v4(),
      type: TransactionType.DEBT_PAYMENT,
      amount: paymentAmount,
      currency: debt.currency,
      sourceAccountId: accountId,
      category: "Debt Settlement",
      party: debt.partyName,
      date: DateTime.now().millisecondsSinceEpoch,
      relatedEntityId: debtId,
      note: "سداد دفعة من الدين: ${debt.partyName} / Debt payment recorded",
    );
    await addTransaction(tx);
  }

  @override
  Future<void> deleteDebt(String id) async {
    _debts.removeWhere((d) => d.id == id);
    _notifyDebts();
  }

  @override
  Future<void> addBudget(Budget budget) async {
    if (budget.category.trim().isEmpty) {
      throw ArgumentError("تصنيف الميزانية مطلوب / Budget category cannot be empty");
    }
    if (budget.monthlyLimit <= 0.0) {
      throw ArgumentError("حد الميزانية الشهري يجب أن يكون أكبر من الصفر / Monthly limit must be positive");
    }
    _budgets.add(budget);
    _notifyBudgets();
  }

  @override
  Future<void> deleteBudget(String id) async {
    _budgets.removeWhere((b) => b.id == id);
    _notifyBudgets();
  }

  @override
  Future<void> addGoal(Goal goal) async {
    if (goal.title.trim().isEmpty) {
      throw ArgumentError("عنوان الهدف مطلوب / Goal title cannot be empty");
    }
    if (goal.targetAmount <= 0.0) {
      throw ArgumentError("المبلغ المستهدف يجب أن يكون أكبر من الصفر / Target amount must be positive");
    }
    if (goal.currentAmount < 0.0) {
      throw ArgumentError("المبلغ المجمع الحالي لا يمكن أن يكون سالباً / Current amount cannot be negative");
    }
    final idx = _goals.indexWhere((g) => g.id == goal.id);
    if (idx != -1) {
      _goals[idx] = goal;
    } else {
      _goals.add(goal);
    }
    _notifyGoals();
  }

  @override
  Future<void> contributeToGoal(String goalId, double amount, String accountId) async {
    if (amount <= 0.0) {
      throw ArgumentError("مبلغ المساهمة يجب أن يكون أكبر من الصفر / Contribution must be positive");
    }
    final idx = _goals.indexWhere((g) => g.id == goalId);
    if (idx == -1) {
      throw ArgumentError("الهدف الادخاري غير موجود / Goal not found");
    }
    final goal = _goals[idx];

    final newCurrent = goal.currentAmount + amount;
    final isCompleted = newCurrent >= goal.targetAmount;
    final updatedGoal = goal.copyWith(currentAmount: newCurrent, isCompleted: isCompleted);
    _goals[idx] = updatedGoal;
    _notifyGoals();

    final tx = Transaction(
      id: _uuid.v4(),
      type: TransactionType.GOAL_CONTRIBUTION,
      amount: amount,
      currency: goal.currency,
      sourceAccountId: accountId,
      category: "Savings & Goals",
      date: DateTime.now().millisecondsSinceEpoch,
      relatedEntityId: goalId,
      note: "مساهمة في هدف ادخاري: ${goal.title} / Contribution to savings goal",
    );
    await addTransaction(tx);
  }

  @override
  Future<void> deleteGoal(String id) async {
    _goals.removeWhere((g) => g.id == id);
    _notifyGoals();
  }

  @override
  Future<void> addBill(Bill bill) async {
    if (bill.title.trim().isEmpty) {
      throw ArgumentError("عنوان الفاتورة مطلوب / Bill title cannot be empty");
    }
    if (bill.amount <= 0.0) {
      throw ArgumentError("مبلغ الفاتورة يجب أن يكون أكبر من الصفر / Bill amount must be positive");
    }
    final idx = _bills.indexWhere((b) => b.id == bill.id);
    if (idx != -1) {
      _bills[idx] = bill;
    } else {
      _bills.add(bill);
    }
    _notifyBills();
  }

  @override
  Future<void> payBill(String billId, String accountId) async {
    final idx = _bills.indexWhere((b) => b.id == billId);
    if (idx == -1) {
      throw ArgumentError("الفاتورة غير موجودة / Bill not found");
    }
    final bill = _bills[idx];

    final updatedBill = bill.copyWith(
      status: BillStatus.PAID,
      nextDueDate: bill.nextDueDate + 30 * 24 * 3600 * 1000,
    );
    _bills[idx] = updatedBill;
    _notifyBills();

    final tx = Transaction(
      id: _uuid.v4(),
      type: TransactionType.BILL_PAYMENT,
      amount: bill.amount,
      currency: bill.currency,
      sourceAccountId: accountId,
      category: bill.category,
      date: DateTime.now().millisecondsSinceEpoch,
      relatedEntityId: billId,
      note: "سداد الفاتورة: ${bill.title} / Bill paid",
    );
    await addTransaction(tx);
  }

  @override
  Future<void> deleteBill(String id) async {
    _bills.removeWhere((b) => b.id == id);
    _notifyBills();
  }

  @override
  Future<void> seedInitialSampleDataIfEmpty() async {
    if (_accounts.isNotEmpty) return;

    final defaultAccounts = [
      Account(id: "acc_rajhi", name: "Al Rajhi Bank", type: AccountType.BANK, balance: 28450.00, currency: "SAR", accountNumber: "**** 8842", colorHex: "#0EA5E9", iconName: "bank"),
      Account(id: "acc_cash", name: "Wallet Cash", type: AccountType.CASH, balance: 1250.00, currency: "SAR", accountNumber: "", colorHex: "#10B981", iconName: "wallet"),
      Account(id: "acc_inma", name: "Al Inma Savings", type: AccountType.SAVINGS, balance: 45000.00, currency: "SAR", accountNumber: "**** 1209", colorHex: "#8B5CF6", iconName: "savings"),
      Account(id: "acc_crypto", name: "Binance Crypto", type: AccountType.CRYPTO, balance: 18200.00, currency: "SAR", accountNumber: "BTC/ETH", colorHex: "#F59E0B", iconName: "crypto")
    ];
    _accounts.addAll(defaultAccounts);

    final now = DateTime.now().millisecondsSinceEpoch;
    const day = 24 * 3600 * 1000;

    final sampleTransactions = [
      Transaction(id: "tx_1", type: TransactionType.INCOME, amount: 18500.00, currency: "SAR", sourceAccountId: "acc_rajhi", category: "Salary", party: "Aramco Tech", date: now - 2 * day, note: "Monthly Salary Deposit"),
      Transaction(id: "tx_2", type: TransactionType.EXPENSE, amount: 4200.00, currency: "SAR", sourceAccountId: "acc_rajhi", category: "Housing", party: "Emaar Property", date: now - 3 * day, note: "Apartment Rent"),
      Transaction(id: "tx_3", type: TransactionType.EXPENSE, amount: 320.00, currency: "SAR", sourceAccountId: "acc_cash", category: "Dining", party: "Al Baik Restaurant", date: now - 1 * day, note: "Dinner with family"),
      Transaction(id: "tx_4", type: TransactionType.TRANSFER, amount: 2000.00, currency: "SAR", sourceAccountId: "acc_rajhi", destinationAccountId: "acc_inma", category: "Transfer", party: "Internal Savings", date: now - 4 * day, note: "Monthly Savings Transfer"),
      Transaction(id: "tx_5", type: TransactionType.EXPENSE, amount: 180.00, currency: "SAR", sourceAccountId: "acc_rajhi", category: "Utilities", party: "STC Fiber", date: now - 5 * day, note: "Internet Bill")
    ];
    _transactions.addAll(sampleTransactions);

    final sampleAssets = [
      Asset(id: "ast_1", name: "Riyadh Villa Quarter", type: AssetType.REAL_ESTATE, purchaseValue: 450000.0, currentValue: 520000.0, quantity: 1.0, unit: "Property", currency: "SAR", notes: "Prime location real estate", purchaseDate: now - 365 * day, status: AssetStatus.ACTIVE, purchaseAccountId: "acc_1", purchaseAccountName: "Al Rajhi Bank"),
      Asset(id: "ast_2", name: "Toyota Camry 2024", type: AssetType.VEHICLE, purchaseValue: 95000.0, currentValue: 82000.0, quantity: 1.0, unit: "Car", currency: "SAR", notes: "Personal Transport", purchaseDate: now - 120 * day, status: AssetStatus.ACTIVE, purchaseAccountId: "acc_1", purchaseAccountName: "Al Rajhi Bank"),
      Asset(id: "ast_3", name: "Gold Bullion Bar", type: AssetType.GOLD, purchaseValue: 22000.0, currentValue: 26800.0, quantity: 100.0, unit: "Grams", currency: "SAR", notes: "24K Fine Gold", purchaseDate: now - 200 * day, status: AssetStatus.ACTIVE, purchaseAccountId: "acc_2", purchaseAccountName: "SNB AlAhli")
    ];
    _assets.addAll(sampleAssets);

    final sampleAssetLogs = [
      AssetLog(id: "log_1", assetId: "ast_1", type: AssetLogType.PURCHASE, title: "شراء الأصل", amount: 450000.0, previousValue: 0.0, newValue: 450000.0, accountName: "Al Rajhi Bank", date: now - 365 * day, notes: "شراء عقار حي الرياض"),
      AssetLog(id: "log_2", assetId: "ast_1", type: AssetLogType.VALUE_UPDATE, title: "تحديث القيمة", amount: 520000.0, previousValue: 450000.0, newValue: 520000.0, accountName: "Al Rajhi Bank", date: now - 30 * day, notes: "تحديث القيمة حسب التقييم العقاري"),
      AssetLog(id: "log_3", assetId: "ast_2", type: AssetLogType.PURCHASE, title: "شراء الأصل", amount: 95000.0, previousValue: 0.0, newValue: 95000.0, accountName: "Al Rajhi Bank", date: now - 120 * day, notes: "شراء مركبة جديدة"),
      AssetLog(id: "log_4", assetId: "ast_2", type: AssetLogType.VALUE_UPDATE, title: "تحديث القيمة", amount: 82000.0, previousValue: 95000.0, newValue: 82000.0, accountName: "Al Rajhi Bank", date: now - 10 * day, notes: "تخفيض القيمة مع الاستهلاك"),
      AssetLog(id: "log_5", assetId: "ast_3", type: AssetLogType.PURCHASE, title: "شراء الأصل", amount: 22000.0, previousValue: 0.0, newValue: 22000.0, accountName: "SNB AlAhli", date: now - 200 * day, notes: "شراء سبيكة ذهب")
    ];
    _assetLogs.addAll(sampleAssetLogs);

    final samplePersons = [
      Person(id: "prs_1", name: "Ahmed Al-Mansoor", phone: "+966 50 123 4567", category: "Personal", currency: "SAR", notes: "College friend", isActive: true, createdAt: now - 30 * day),
      Person(id: "prs_2", name: "Samba Auto Finance", phone: "+966 800 124 8000", category: "Institutional", currency: "SAR", notes: "Vehicle installment loan", isActive: true, createdAt: now - 40 * day),
      Person(id: "prs_3", name: "Mohammed Al-Amri", phone: "+967 77 123 4567", category: "Personal", currency: "USD", notes: "Software consulting client", isActive: true, createdAt: now - 20 * day),
      Person(id: "prs_4", name: "Tariq Yemen Import", phone: "+967 71 987 6543", category: "Institutional", currency: "YER", notes: "Goods supplier", isActive: true, createdAt: now - 50 * day)
    ];
    _persons.addAll(samplePersons);

    final sampleDebts = [
      Debt(id: "dbt_1", debtAccountId: "dac_1", personId: "prs_1", partyName: "Ahmed Al-Mansoor", partyPhone: "+966 50 123 4567", type: DebtType.RECEIVABLE, originalAmount: 5000.0, remainingAmount: 2000.0, currency: "SAR", dueDate: now + 15 * day, status: DebtStatus.PARTIAL, notes: "Personal loan for business equipment"),
      Debt(id: "dbt_2", debtAccountId: "dac_2", personId: "prs_2", partyName: "Samba Auto Finance", partyPhone: "+966 800 124 8000", type: DebtType.PAYABLE, originalAmount: 35000.0, remainingAmount: 18500.0, currency: "SAR", dueDate: now + 45 * day, status: DebtStatus.PARTIAL, notes: "Car installment loan"),
      Debt(id: "dbt_3", debtAccountId: "dac_3", personId: "prs_3", partyName: "Mohammed Al-Amri", partyPhone: "+967 77 123 4567", type: DebtType.RECEIVABLE, originalAmount: 1500.0, remainingAmount: 1500.0, currency: "USD", dueDate: now + 30 * day, status: DebtStatus.ACTIVE, notes: "Software consulting fee"),
      Debt(id: "dbt_4", debtAccountId: "dac_4", personId: "prs_4", partyName: "Tariq Yemen Import", partyPhone: "+967 71 987 6543", type: DebtType.PAYABLE, originalAmount: 250000.0, remainingAmount: 120000.0, currency: "YER", dueDate: now + 60 * day, status: DebtStatus.PARTIAL, notes: "Goods supplier invoice")
    ];
    _debts.addAll(sampleDebts);

    final sampleBudgets = [
      Budget(id: "bdg_1", category: "Dining", monthlyLimit: 1500.0, currency: "SAR", period: "MONTHLY"),
      Budget(id: "bdg_2", category: "Housing", monthlyLimit: 5000.0, currency: "SAR", period: "MONTHLY"),
      Budget(id: "bdg_3", category: "Utilities", monthlyLimit: 800.0, currency: "SAR", period: "MONTHLY"),
      Budget(id: "bdg_4", category: "Entertainment", monthlyLimit: 1200.0, currency: "SAR", period: "MONTHLY")
    ];
    _budgets.addAll(sampleBudgets);

    final sampleGoals = [
      Goal(id: "gol_1", title: "Emergency Fund", targetAmount: 60000.0, currentAmount: 42000.0, currency: "SAR", targetDate: now + 120 * day, iconName: "shield", colorHex: "#10B981"),
      Goal(id: "gol_2", title: "Summer Europe Trip", targetAmount: 25000.0, currentAmount: 11500.0, currency: "SAR", targetDate: now + 90 * day, iconName: "plane", colorHex: "#0EA5E9"),
      Goal(id: "gol_3", title: "New MacBook Pro", targetAmount: 12000.0, currentAmount: 8500.0, currency: "SAR", targetDate: now + 30 * day, iconName: "laptop", colorHex: "#F59E0B")
    ];
    _goals.addAll(sampleGoals);

    final sampleBills = [
      Bill(id: "bil_1", title: "STC Fiber Internet", amount: 287.50, currency: "SAR", frequency: "MONTHLY", nextDueDate: now + 5 * day, category: "Utilities", accountId: "acc_rajhi", status: BillStatus.UPCOMING, isAutoPay: true),
      Bill(id: "bil_2", title: "SEC Electricity", amount: 412.00, currency: "SAR", frequency: "MONTHLY", nextDueDate: now + 10 * day, category: "Utilities", accountId: "acc_rajhi", status: BillStatus.SCHEDULED, isAutoPay: false),
      Bill(id: "bil_3", title: "Fitness Time Gym", amount: 350.00, currency: "SAR", frequency: "MONTHLY", nextDueDate: now + 18 * day, category: "Health", accountId: "acc_rajhi", status: BillStatus.SCHEDULED, isAutoPay: true)
    ];
    _bills.addAll(sampleBills);

    _notifyAccounts();
    _notifyTransactions();
    _notifyAssets();
    _notifyAssetLogs();
    _notifyPersons();
    _notifyDebts();
    _notifyBudgets();
    _notifyGoals();
    _notifyBills();
  }
}
