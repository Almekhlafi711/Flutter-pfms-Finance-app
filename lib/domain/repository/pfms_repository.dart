import '../models/domain_models.dart';
import '../models/debt_models.dart';
import '../models/person.dart';

abstract class PfmsRepository {
  Stream<List<Account>> getAccounts();
  Stream<List<Transaction>> getTransactions();
  Stream<List<Transaction>> getTransactionsForAccount(String accountId);
  Stream<List<Asset>> getAssets();
  Stream<List<Debt>> getDebts();
  Stream<List<Budget>> getBudgets();
  Stream<List<Goal>> getGoals();
  Stream<List<Bill>> getBills();
  Stream<List<Person>> getPersons();
  Stream<List<AssetLog>> getAssetLogs(String assetId);

  Future<void> addAccount(Account account);
  Future<void> updateAccount(Account account);
  Future<void> archiveAccount(String accountId);
  Future<void> deleteAccount(String accountId);
  Future<bool> isAccountInUse(String accountId);

  Future<void> addTransaction(Transaction transaction);
  Future<void> updateTransaction(Transaction transaction);
  Future<void> deleteTransaction(String id);

  Future<void> addAsset(Asset asset);
  Future<void> updateAsset(Asset asset);
  Future<void> deleteAsset(String id);
  Future<void> addAssetLog(AssetLog log);

  Future<void> addPerson(Person person);
  Future<void> updatePerson(Person person);

  Future<void> addDebt(Debt debt);
  Future<void> recordDebtPayment(String debtId, double paymentAmount, String accountId);
  Future<void> deleteDebt(String id);

  Future<void> addBudget(Budget budget);
  Future<void> deleteBudget(String id);

  Future<void> addGoal(Goal goal);
  Future<void> contributeToGoal(String goalId, double amount, String accountId);
  Future<void> deleteGoal(String id);

  Future<void> addBill(Bill bill);
  Future<void> payBill(String billId, String accountId);
  Future<void> deleteBill(String id);

  Future<void> seedInitialSampleDataIfEmpty();
}
