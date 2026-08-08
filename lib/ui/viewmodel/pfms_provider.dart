import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';
import '../../core/security/security_manager.dart';
import '../../domain/models/domain_models.dart';
import '../../domain/models/debt_models.dart';
import '../../domain/models/person.dart';
import '../../domain/repository/pfms_repository.dart';
import '../../data/repository/pfms_repository_impl.dart';

enum QuickActionSheetType {
  INCOME, EXPENSE, TRANSFER, ASSET, DEBT, GOAL, BUDGET, BILL, REPORT, QUICK_ADD, DEPOSIT
}

final securityManagerProvider = Provider<SecurityManager>((ref) {
  return SecurityManager();
});

final repositoryProvider = Provider<PfmsRepository>((ref) {
  final repo = PfmsRepositoryImpl();
  repo.seedInitialSampleDataIfEmpty();
  return repo;
});

final accountsStreamProvider = StreamProvider<List<Account>>((ref) {
  return ref.watch(repositoryProvider).getAccounts();
});

final groupedAccountsProvider = Provider<List<GroupedAccount>>((ref) {
  final accounts = ref.watch(accountsStreamProvider).value ?? [];
  final Map<String, List<Account>> groups = {};
  for (var acc in accounts) {
    final key = "${acc.name}_${acc.type.name}";
    groups.putIfAbsent(key, () => []).add(acc);
  }
  return groups.entries.map((entry) {
    final list = entry.value;
    final first = list.first;
    return GroupedAccount(
      name: first.name,
      type: first.type,
      colorHex: first.colorHex,
      iconName: first.iconName,
      isArchived: list.every((a) => a.isArchived),
      accounts: list,
    );
  }).toList();
});

final selectedAccountIdProvider = StateProvider<String?>((ref) => null);

final selectedAccountProvider = Provider<Account?>((ref) {
  final accounts = ref.watch(accountsStreamProvider).value ?? [];
  final selId = ref.watch(selectedAccountIdProvider);
  if (selId == null) return accounts.isNotEmpty ? accounts.first : null;
  return accounts.firstWhere((a) => a.id == selId, orElse: () => accounts.first);
});

final transactionsStreamProvider = StreamProvider<List<Transaction>>((ref) {
  return ref.watch(repositoryProvider).getTransactions();
});

final filteredTransactionsProvider = Provider<List<Transaction>>((ref) {
  final txs = ref.watch(transactionsStreamProvider).value ?? [];
  final selId = ref.watch(selectedAccountIdProvider);
  if (selId == null) return txs;
  return txs.where((tx) => tx.sourceAccountId == selId || tx.destinationAccountId == selId).toList();
});

final assetsStreamProvider = StreamProvider<List<Asset>>((ref) {
  return ref.watch(repositoryProvider).getAssets();
});

final debtsStreamProvider = StreamProvider<List<Debt>>((ref) {
  return ref.watch(repositoryProvider).getDebts();
});

final personsStreamProvider = StreamProvider<List<Person>>((ref) {
  return ref.watch(repositoryProvider).getPersons();
});

final budgetsStreamProvider = StreamProvider<List<Budget>>((ref) {
  return ref.watch(repositoryProvider).getBudgets();
});

final goalsStreamProvider = StreamProvider<List<Goal>>((ref) {
  return ref.watch(repositoryProvider).getGoals();
});

final billsStreamProvider = StreamProvider<List<Bill>>((ref) {
  return ref.watch(repositoryProvider).getBills();
});

final debtLedgerEntriesStateProvider = StateProvider<List<DebtLedgerEntry>>((ref) => []);

final personDebtAccountsProvider = Provider<List<PersonDebtAccount>>((ref) {
  final debts = ref.watch(debtsStreamProvider).value ?? [];
  final persons = ref.watch(personsStreamProvider).value ?? [];
  final customEntries = ref.watch(debtLedgerEntriesStateProvider);

  return debts.map((debt) {
    final matchingPerson = persons.firstWhere(
      (p) => p.id == debt.personId,
      orElse: () => Person(
        id: debt.personId,
        name: debt.partyName.isNotEmpty ? debt.partyName : "Unknown Person",
        phone: debt.partyPhone,
        category: "Personal",
      ),
    );

    final matchingEntries = customEntries.where((e) => e.debtId == debt.id || e.personId == matchingPerson.id).toList();
    List<DebtLedgerEntry> defaultEntries;
    if (matchingEntries.isEmpty) {
      defaultEntries = [
        DebtLedgerEntry(
          id: "entry_init_${debt.id}",
          debtId: debt.id,
          personId: matchingPerson.id,
          type: debt.type,
          isPayment: false,
          amount: debt.originalAmount,
          date: debt.createdAt,
          description: debt.notes.isNotEmpty ? debt.notes : "Initial Debt Creation",
          category: "Debt Principal",
          paymentMethod: "Bank / Cash",
          status: "CLEARED",
        ),
      ];
      if (debt.originalAmount > debt.remainingAmount) {
        final paid = debt.originalAmount - debt.remainingAmount;
        defaultEntries.add(
          DebtLedgerEntry(
            id: "entry_pay_${debt.id}",
            debtId: debt.id,
            personId: matchingPerson.id,
            type: debt.type,
            isPayment: true,
            amount: paid,
            date: DateTime.now().millisecondsSinceEpoch - 2 * 24 * 3600 * 1000,
            description: "Partial Debt Settlement",
            category: "Settlement",
            paymentMethod: "Bank Transfer",
            status: "CLEARED",
          ),
        );
      }
    } else {
      defaultEntries = matchingEntries;
    }

    return PersonDebtAccount(
      id: debt.debtAccountId.isNotEmpty ? debt.debtAccountId : "dac_${debt.id}",
      personId: matchingPerson.id,
      person: matchingPerson,
      currency: debt.currency,
      notes: debt.notes,
      isActive: matchingPerson.isActive,
      createdAt: debt.createdAt,
      mainDebt: debt,
      entries: defaultEntries,
    );
  }).toList();
});

final netWorthSummaryProvider = Provider<NetWorthSummary>((ref) {
  final accounts = ref.watch(accountsStreamProvider).value ?? [];
  final assets = ref.watch(assetsStreamProvider).value ?? [];
  final debts = ref.watch(debtsStreamProvider).value ?? [];

  final totalAccounts = accounts.fold(0.0, (sum, acc) => sum + acc.balance);
  final totalAssets = assets.fold(0.0, (sum, ast) => sum + ast.totalCurrentValue);
  final totalReceivables = debts.where((d) => d.type == DebtType.RECEIVABLE).fold(0.0, (sum, d) => sum + d.remainingAmount);
  final totalPayables = debts.where((d) => d.type == DebtType.PAYABLE).fold(0.0, (sum, d) => sum + d.remainingAmount);

  return NetWorthSummary(
    totalCashAndAccounts: totalAccounts,
    totalAssetsValue: totalAssets,
    totalReceivables: totalReceivables,
    totalLiabilitiesAndPayables: totalPayables,
  );
});

final activeBottomSheetProvider = StateProvider<QuickActionSheetType?>((ref) => null);
final isArabicProvider = StateProvider<bool>((ref) => false);
final toastMessageProvider = StateProvider<String?>((ref) => null);

class PfmsController {
  final WidgetRef ref;
  final _uuid = const Uuid();

  PfmsController(this.ref);

  PfmsRepository get _repo => ref.read(repositoryProvider);

  void selectAccount(String? id) => ref.read(selectedAccountIdProvider.notifier).state = id;
  void openBottomSheet(QuickActionSheetType type) => ref.read(activeBottomSheetProvider.notifier).state = type;
  void closeBottomSheet() => ref.read(activeBottomSheetProvider.notifier).state = null;
  void toggleLanguage() {
    final curr = ref.read(isArabicProvider);
    ref.read(isArabicProvider.notifier).state = !curr;
  }

  void setToast(String msg) => ref.read(toastMessageProvider.notifier).state = msg;
  void clearToast() => ref.read(toastMessageProvider.notifier).state = null;

  Future<void> addDeposit(double amount, String accountId, String category, String note, [String currency = "SAR"]) async {
    try {
      final accounts = ref.read(accountsStreamProvider).value ?? [];
      final accCurrency = accounts.firstWhere((a) => a.id == accountId, orElse: () => accounts.first).currency;
      final tx = Transaction(
        id: _uuid.v4(),
        type: TransactionType.INCOME,
        amount: amount,
        currency: currency.isNotEmpty ? currency : accCurrency,
        sourceAccountId: accountId,
        category: category.isNotEmpty ? category : "Cash Deposit",
        note: note,
      );
      await _repo.addTransaction(tx);
      closeBottomSheet();
    } catch (e) {
      setToast(e.toString());
    }
  }

  Future<void> addIncome(double amount, String accountId, String category, String party, String note, [String currency = "SAR"]) async {
    try {
      final accounts = ref.read(accountsStreamProvider).value ?? [];
      final accCurrency = accounts.firstWhere((a) => a.id == accountId, orElse: () => accounts.first).currency;
      final tx = Transaction(
        id: _uuid.v4(),
        type: TransactionType.INCOME,
        amount: amount,
        currency: currency.isNotEmpty ? currency : accCurrency,
        sourceAccountId: accountId,
        category: category,
        party: party.isNotEmpty ? party : null,
        note: note,
      );
      await _repo.addTransaction(tx);
      closeBottomSheet();
    } catch (e) {
      setToast(e.toString());
    }
  }

  Future<void> addExpense(double amount, String accountId, String category, String party, String note, [String currency = "SAR"]) async {
    try {
      final accounts = ref.read(accountsStreamProvider).value ?? [];
      final accCurrency = accounts.firstWhere((a) => a.id == accountId, orElse: () => accounts.first).currency;
      final tx = Transaction(
        id: _uuid.v4(),
        type: TransactionType.EXPENSE,
        amount: amount,
        currency: currency.isNotEmpty ? currency : accCurrency,
        sourceAccountId: accountId,
        category: category,
        party: party.isNotEmpty ? party : null,
        note: note,
      );
      await _repo.addTransaction(tx);
      closeBottomSheet();
    } catch (e) {
      setToast(e.toString());
    }
  }

  Future<void> addTransfer(double amount, String sourceAccountId, String destAccountId, String note, [String currency = "SAR"]) async {
    try {
      final accounts = ref.read(accountsStreamProvider).value ?? [];
      final accCurrency = accounts.firstWhere((a) => a.id == sourceAccountId, orElse: () => accounts.first).currency;
      final tx = Transaction(
        id: _uuid.v4(),
        type: TransactionType.TRANSFER,
        amount: amount,
        currency: currency.isNotEmpty ? currency : accCurrency,
        sourceAccountId: sourceAccountId,
        destinationAccountId: destAccountId,
        category: "Internal Transfer",
        note: note,
      );
      await _repo.addTransaction(tx);
      closeBottomSheet();
    } catch (e) {
      setToast(e.toString());
    }
  }

  Future<void> deleteTransaction(String id) => _repo.deleteTransaction(id);
  Future<void> updateTransaction(Transaction tx) => _repo.updateTransaction(tx);

  Future<void> deleteAsset(String id) => _repo.deleteAsset(id);
  Future<void> updateAsset(Asset asset) => _repo.updateAsset(asset);

  Future<void> updateAssetValue(Asset asset, double newCurrentValue, [String notes = ""]) async {
    final previousValue = asset.currentValue;
    final updatedAsset = asset.copyWith(currentValue: newCurrentValue);
    await _repo.updateAsset(updatedAsset);

    final isAr = ref.read(isArabicProvider);
    final log = AssetLog(
      id: _uuid.v4(),
      assetId: asset.id,
      type: AssetLogType.VALUE_UPDATE,
      title: isAr ? "تحديث القيمة" : "Value Update",
      amount: newCurrentValue,
      previousValue: previousValue,
      newValue: newCurrentValue,
      date: DateTime.now().millisecondsSinceEpoch,
      notes: notes,
    );
    await _repo.addAssetLog(log);
  }

  Future<void> sellAsset(Asset asset, double salePrice, String destinationAccountId, [String notes = ""]) async {
    final accounts = ref.read(accountsStreamProvider).value ?? [];
    final account = accounts.firstWhere((a) => a.id == destinationAccountId, orElse: () => accounts.first);
    final now = DateTime.now().millisecondsSinceEpoch;

    final updatedAsset = asset.copyWith(
      status: AssetStatus.SOLD,
      soldPrice: salePrice,
      soldDate: now,
      soldAccountId: destinationAccountId,
      soldAccountName: account.name,
    );
    await _repo.updateAsset(updatedAsset);

    final isAr = ref.read(isArabicProvider);
    final tx = Transaction(
      id: _uuid.v4(),
      type: TransactionType.ASSET_SALE,
      amount: salePrice,
      currency: "SAR",
      sourceAccountId: destinationAccountId,
      category: "Asset Liquidation",
      party: asset.name,
      relatedEntityId: asset.id,
      note: isAr ? "بيع أصل: ${asset.name}" : "Sold asset: ${asset.name}",
      date: now,
    );
    await _repo.addTransaction(tx);

    final log = AssetLog(
      id: _uuid.v4(),
      assetId: asset.id,
      type: AssetLogType.SALE,
      title: isAr ? "بيع الأصل" : "Asset Sale",
      amount: salePrice,
      accountName: account.name,
      date: now,
      notes: notes,
    );
    await _repo.addAssetLog(log);
  }

  Future<void> addDetailedAsset(String name, AssetType type, double purchaseVal, double currentVal, String accountId, {String currency = "SAR", int? purchaseDate, String notes = ""}) async {
    final accounts = ref.read(accountsStreamProvider).value ?? [];
    final account = accounts.firstWhere((a) => a.id == accountId, orElse: () => accounts.first);
    final accountName = account.name;
    final finalCurrentValue = currentVal <= 0.0 ? purchaseVal : currentVal;
    final assetId = _uuid.v4();
    final pDate = purchaseDate ?? DateTime.now().millisecondsSinceEpoch;

    final asset = Asset(
      id: assetId,
      name: name,
      type: type,
      purchaseValue: purchaseVal,
      currentValue: finalCurrentValue,
      currency: "SAR",
      purchaseDate: pDate,
      notes: notes,
      status: AssetStatus.ACTIVE,
      purchaseAccountId: accountId,
      purchaseAccountName: accountName,
    );
    await _repo.addAsset(asset);

    final isAr = ref.read(isArabicProvider);
    if (purchaseVal > 0 && accountId.isNotEmpty) {
      final tx = Transaction(
        id: _uuid.v4(),
        type: TransactionType.ASSET_PURCHASE,
        amount: purchaseVal,
        currency: "SAR",
        sourceAccountId: accountId,
        category: "Asset Acquisition",
        party: name,
        relatedEntityId: assetId,
        note: notes.isNotEmpty ? notes : (isAr ? "شراء أصل $name" : "Purchased asset $name"),
        date: pDate,
      );
      await _repo.addTransaction(tx);
    }

    final log = AssetLog(
      id: _uuid.v4(),
      assetId: assetId,
      type: AssetLogType.PURCHASE,
      title: isAr ? "شراء الأصل" : "Asset Purchase",
      amount: purchaseVal,
      previousValue: 0.0,
      newValue: purchaseVal,
      accountName: accountName,
      date: pDate,
      notes: notes,
    );
    await _repo.addAssetLog(log);
  }

  Future<void> addAsset(String name, AssetType type, double purchaseVal, double currentVal, String accountId) async {
    await addDetailedAsset(name, type, purchaseVal, currentVal, accountId);
    closeBottomSheet();
  }

  Future<void> addPerson(Person person) => _repo.addPerson(person);
  Future<void> updatePerson(Person person) => _repo.updatePerson(person);
  Future<void> updatePersonStatus(String personId, bool isActive) async {
    final persons = ref.read(personsStreamProvider).value ?? [];
    final target = persons.firstWhere((p) => p.id == personId, orElse: () => persons.first);
    await updatePerson(target.copyWith(isActive: isActive));
  }

  Future<void> addDebtForPerson(Person person, DebtType type, double amount, String accountId, String category, String currency, String notes) async {
    await addPerson(person);
    final dacId = "dac_${_uuid.v4().substring(0, 8)}";
    final debtId = "dbt_${_uuid.v4().substring(0, 8)}";

    final debt = Debt(
      id: debtId,
      debtAccountId: dacId,
      personId: person.id,
      partyName: person.name,
      partyPhone: person.phone,
      type: type,
      originalAmount: amount,
      remainingAmount: amount,
      currency: currency,
      dueDate: DateTime.now().millisecondsSinceEpoch + 30 * 24 * 3600 * 1000,
      status: DebtStatus.ACTIVE,
      notes: notes,
    );
    await _repo.addDebt(debt);

    final entry = DebtLedgerEntry(
      id: "entry_${_uuid.v4().substring(0, 8)}",
      debtAccountId: dacId,
      debtId: debtId,
      personId: person.id,
      type: type,
      isPayment: false,
      amount: amount,
      date: DateTime.now().millisecondsSinceEpoch,
      description: notes.isNotEmpty ? notes : "New Debt Ledger Creation",
      category: category,
      paymentMethod: accountId.isNotEmpty ? "Account Transfer" : "Cash",
      accountId: accountId,
      status: "CLEARED",
    );

    final currentEntries = ref.read(debtLedgerEntriesStateProvider);
    ref.read(debtLedgerEntriesStateProvider.notifier).state = [...currentEntries, entry];

    if (accountId.isNotEmpty) {
      final txType = type == DebtType.RECEIVABLE ? TransactionType.DEBT_CREATION : TransactionType.INCOME;
      final tx = Transaction(
        id: _uuid.v4(),
        type: txType,
        amount: amount,
        currency: currency,
        sourceAccountId: accountId,
        category: "Debt Center",
        party: person.name,
        relatedEntityId: debtId,
        note: notes.isNotEmpty ? notes : "Created debt with ${person.name}",
      );
      await _repo.addTransaction(tx);
    }
    closeBottomSheet();
  }

  Future<void> addPaymentForPerson(Person person, double amount, String accountId, String currency, String notes, bool isReceive) async {
    await addPerson(person);
    final debts = ref.read(debtsStreamProvider).value ?? [];
    Debt? matchingDebt = debts.firstWhere((d) => d.personId == person.id && d.currency.toLowerCase() == currency.toLowerCase(), orElse: () => Debt(id: "", partyName: "", type: DebtType.RECEIVABLE, originalAmount: 0, remainingAmount: 0));

    if (matchingDebt.id.isEmpty) {
      final dacId = "dac_${_uuid.v4().substring(0, 8)}";
      final debtId = "dbt_${_uuid.v4().substring(0, 8)}";
      matchingDebt = Debt(
        id: debtId,
        debtAccountId: dacId,
        personId: person.id,
        partyName: person.name,
        partyPhone: person.phone,
        type: isReceive ? DebtType.RECEIVABLE : DebtType.PAYABLE,
        originalAmount: 0.0,
        remainingAmount: 0.0,
        currency: currency,
        dueDate: DateTime.now().millisecondsSinceEpoch + 30 * 24 * 3600 * 1000,
        status: DebtStatus.ACTIVE,
        notes: notes,
      );
      await _repo.addDebt(matchingDebt);
    } else {
      final updatedRemaining = (matchingDebt.remainingAmount - amount).clamp(0.0, double.infinity);
      final updatedStatus = updatedRemaining == 0.0 ? DebtStatus.COMPLETED : DebtStatus.PARTIAL;
      await _repo.addDebt(matchingDebt.copyWith(remainingAmount: updatedRemaining, status: updatedStatus));
    }

    final entry = DebtLedgerEntry(
      id: "pay_entry_${_uuid.v4().substring(0, 8)}",
      debtAccountId: matchingDebt.debtAccountId,
      debtId: matchingDebt.id,
      personId: person.id,
      type: matchingDebt.type,
      isPayment: true,
      amount: amount,
      date: DateTime.now().millisecondsSinceEpoch,
      description: notes.isNotEmpty ? notes : (isReceive ? "استلام مبلغ" : "سداد مبلغ"),
      category: isReceive ? "Receipt" : "Settlement",
      paymentMethod: accountId.isNotEmpty ? "Account Settlement" : "Cash",
      accountId: accountId,
      status: "CLEARED",
    );

    final currentEntries = ref.read(debtLedgerEntriesStateProvider);
    ref.read(debtLedgerEntriesStateProvider.notifier).state = [...currentEntries, entry];

    if (accountId.isNotEmpty) {
      final txType = isReceive ? TransactionType.INCOME : TransactionType.EXPENSE;
      final tx = Transaction(
        id: _uuid.v4(),
        type: txType,
        amount: amount,
        currency: currency,
        sourceAccountId: accountId,
        category: "Debt Settlement",
        party: person.name,
        relatedEntityId: matchingDebt.id,
        note: notes.isNotEmpty ? notes : (isReceive ? "Payment received from ${person.name}" : "Payment paid to ${person.name}"),
      );
      await _repo.addTransaction(tx);
    }
    closeBottomSheet();
  }

  Future<void> addDebt(String partyName, String phone, DebtType type, double amount, String accountId, String note) async {
    final persons = ref.read(personsStreamProvider).value ?? [];
    final person = persons.firstWhere(
      (p) => p.name.toLowerCase() == partyName.toLowerCase(),
      orElse: () => Person(
        id: "prs_${_uuid.v4().substring(0, 8)}",
        name: partyName,
        phone: phone.isNotEmpty ? phone : null,
        category: "General",
      ),
    );
    await addDebtForPerson(person, type, amount, accountId, "General", "SAR", note);
  }

  Future<void> addGoal(String title, double targetAmount, double initialAmount) async {
    final goal = Goal(
      id: _uuid.v4(),
      title: title,
      targetAmount: targetAmount,
      currentAmount: initialAmount,
      targetDate: DateTime.now().millisecondsSinceEpoch + 180 * 24 * 3600 * 1000,
    );
    await _repo.addGoal(goal);
    closeBottomSheet();
  }

  Future<void> addBudget(String category, double limit) async {
    final budget = Budget(
      id: _uuid.v4(),
      category: category,
      monthlyLimit: limit,
    );
    await _repo.addBudget(budget);
    closeBottomSheet();
  }

  Future<void> addBill(String title, double amount, String category, String accountId) async {
    final bill = Bill(
      id: _uuid.v4(),
      title: title,
      amount: amount,
      category: category,
      accountId: accountId,
      nextDueDate: DateTime.now().millisecondsSinceEpoch + 14 * 24 * 3600 * 1000,
    );
    await _repo.addBill(bill);
    closeBottomSheet();
  }

  Future<void> recordDebtPayment(Debt debt, double paymentAmount, String accountId) async {
    final updatedRemaining = (debt.remainingAmount - paymentAmount).clamp(0.0, double.infinity);
    final updatedStatus = updatedRemaining == 0.0 ? DebtStatus.COMPLETED : DebtStatus.PARTIAL;
    await _repo.addDebt(debt.copyWith(remainingAmount: updatedRemaining, status: updatedStatus));

    final persons = ref.read(personsStreamProvider).value ?? [];
    final person = persons.firstWhere((p) => p.name.toLowerCase() == debt.partyName.toLowerCase(), orElse: () => Person(id: "prs_${debt.partyName.hashCode}", name: debt.partyName));

    final paymentEntry = DebtLedgerEntry(
      id: "pay_entry_${_uuid.v4().substring(0, 8)}",
      debtId: debt.id,
      personId: person.id,
      type: debt.type,
      isPayment: true,
      amount: paymentAmount,
      date: DateTime.now().millisecondsSinceEpoch,
      description: "Settlement Payment Received/Paid",
      category: "Settlement",
      paymentMethod: accountId.isNotEmpty ? "Account Settlement" : "Cash",
      accountId: accountId,
      status: "CLEARED",
    );

    final currentEntries = ref.read(debtLedgerEntriesStateProvider);
    ref.read(debtLedgerEntriesStateProvider.notifier).state = [...currentEntries, paymentEntry];

    final txType = debt.type == DebtType.RECEIVABLE ? TransactionType.INCOME : TransactionType.EXPENSE;
    final tx = Transaction(
      id: _uuid.v4(),
      type: txType,
      amount: paymentAmount,
      sourceAccountId: accountId,
      category: "Debt Settlement",
      party: debt.partyName,
      relatedEntityId: debt.id,
      note: "Payment recorded for debt",
    );
    await _repo.addTransaction(tx);
  }

  Future<void> contributeToGoal(Goal goal, double amount, String accountId) async {
    await _repo.contributeToGoal(goal.id, amount, accountId);
  }

  Future<void> payBill(Bill bill, String accountId) async {
    await _repo.payBill(bill.id, accountId);
  }

  Future<void> addAccount(String name, AccountType type, double balance, String currency) async {
    final acc = Account(
      id: "acc_${_uuid.v4().substring(0, 8)}",
      name: name,
      type: type,
      balance: balance,
      currency: currency,
    );
    await _repo.addAccount(acc);
  }

  Future<void> addDetailedAccount(String name, AccountType type, double balance, String currency, String accountNumber, String colorHex, String iconName) async {
    final acc = Account(
      id: "acc_${_uuid.v4().substring(0, 8)}",
      name: name,
      type: type,
      balance: balance,
      currency: currency,
      accountNumber: accountNumber,
      colorHex: colorHex,
      iconName: iconName,
    );
    await _repo.addAccount(acc);
  }

  Future<void> updateAccount(Account account) => _repo.updateAccount(account);

  Future<void> archiveAccount(String accountId) async {
    await _repo.archiveAccount(accountId);
    if (ref.read(selectedAccountIdProvider) == accountId) {
      selectAccount(null);
    }
  }

  Future<void> addMultiCurrencyAccount(String name, AccountType type, Map<String, double> currencyBalances, String colorHex, String iconName, String notes) async {
    for (var entry in currencyBalances.entries) {
      final acc = Account(
        id: "acc_${_uuid.v4().substring(0, 8)}",
        name: name,
        type: type,
        balance: entry.value,
        currency: entry.key,
        colorHex: colorHex,
        iconName: iconName,
      );
      await _repo.addAccount(acc);
    }
  }

  Future<void> deleteAccount(String accountId, {required VoidCallback onSuccess, required Function(String) onError}) async {
    try {
      await _repo.deleteAccount(accountId);
      if (ref.read(selectedAccountIdProvider) == accountId) {
        selectAccount(null);
      }
      onSuccess();
    } catch (e) {
      onError(e.toString());
    }
  }
}
