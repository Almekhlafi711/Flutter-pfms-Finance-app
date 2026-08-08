package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.DesignTokens
import com.example.domain.model.*
import com.example.ui.viewmodel.QuickActionSheetType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionBottomSheet(
    sheetType: QuickActionSheetType,
    accounts: List<Account>,
    selectedAccountId: String?,
    onDismiss: () -> Unit,
    onAddIncome: (Double, String, String, String, String) -> Unit,
    onAddExpense: (Double, String, String, String, String) -> Unit,
    onAddTransfer: (Double, String, String, String) -> Unit,
    onAddAsset: (String, AssetType, Double, Double, String) -> Unit,
    onAddDebt: (String, String, DebtType, Double, String, String) -> Unit,
    onAddGoal: (String, Double, Double) -> Unit,
    onAddBudget: (String, Double) -> Unit,
    onAddBill: (String, Double, String, String) -> Unit,
    onExportPdf: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            when (sheetType) {
                QuickActionSheetType.QUICK_ADD, QuickActionSheetType.DEPOSIT -> {}
                QuickActionSheetType.INCOME -> IncomeForm(accounts, selectedAccountId, onAddIncome, onDismiss)
                QuickActionSheetType.EXPENSE -> ExpenseForm(accounts, selectedAccountId, onAddExpense, onDismiss)
                QuickActionSheetType.TRANSFER -> TransferForm(accounts, selectedAccountId, onAddTransfer, onDismiss)
                QuickActionSheetType.ASSET -> AssetForm(accounts, selectedAccountId, onAddAsset, onDismiss)
                QuickActionSheetType.DEBT -> DebtForm(accounts, selectedAccountId, onAddDebt, onDismiss)
                QuickActionSheetType.GOAL -> GoalForm(onAddGoal, onDismiss)
                QuickActionSheetType.BUDGET -> BudgetForm(onAddBudget, onDismiss)
                QuickActionSheetType.BILL -> BillForm(accounts, selectedAccountId, onAddBill, onDismiss)
                QuickActionSheetType.REPORT -> StatementExportForm(onExportPdf, onDismiss)
            }
        }
    }
}

@Composable
fun IncomeForm(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSubmit: (Double, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Salary") }
    var party by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var targetAccId by remember { mutableStateOf(selectedAccountId ?: accounts.firstOrNull()?.id ?: "") }

    Text("Add Income", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Amount (SAR)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = category,
        onValueChange = { category = it },
        label = { Text("Category (e.g., Salary, Dividend, Gift)") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = party,
        onValueChange = { party = it },
        label = { Text("Payer / Company (Optional)") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("Notes") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = {
            val amt = amountText.toDoubleOrNull() ?: 0.0
            if (amt > 0 && targetAccId.isNotEmpty()) {
                onSubmit(amt, targetAccId, category, party, note)
            }
        },
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text("Save Income Transaction")
    }
}

@Composable
fun ExpenseForm(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSubmit: (Double, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Dining") }
    var party by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var sourceAccId by remember { mutableStateOf(selectedAccountId ?: accounts.firstOrNull()?.id ?: "") }

    Text("Add Expense", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Amount (SAR)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = category,
        onValueChange = { category = it },
        label = { Text("Category (e.g. Dining, Housing, Utilities, Fun)") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = party,
        onValueChange = { party = it },
        label = { Text("Merchant / Person (Optional)") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("Notes") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = {
            val amt = amountText.toDoubleOrNull() ?: 0.0
            if (amt > 0 && sourceAccId.isNotEmpty()) {
                onSubmit(amt, sourceAccId, category, party, note)
            }
        },
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text("Save Expense Transaction")
    }
}

@Composable
fun TransferForm(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSubmit: (Double, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var sourceAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var destAccId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: "") }

    Text("Internal Transfer", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Transfer Amount (SAR)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("Notes / Purpose") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = {
            val amt = amountText.toDoubleOrNull() ?: 0.0
            if (amt > 0 && sourceAccId.isNotEmpty() && destAccId.isNotEmpty() && sourceAccId != destAccId) {
                onSubmit(amt, sourceAccId, destAccId, note)
            }
        },
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text("Perform Transfer")
    }
}

@Composable
fun AssetForm(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSubmit: (String, AssetType, Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var purchaseValText by remember { mutableStateOf("") }
    var currentValText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AssetType.REAL_ESTATE) }
    var sourceAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }

    Text("New Asset", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Asset Name (e.g., Land, Gold, Villa)") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = purchaseValText,
        onValueChange = { purchaseValText = it },
        label = { Text("Purchase Price (SAR)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = currentValText,
        onValueChange = { currentValText = it },
        label = { Text("Current Market Value (SAR)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = {
            val pVal = purchaseValText.toDoubleOrNull() ?: 0.0
            val cVal = currentValText.toDoubleOrNull() ?: pVal
            if (name.isNotEmpty() && pVal > 0) {
                onSubmit(name, selectedType, pVal, cVal, sourceAccId)
            }
        },
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text("Save Asset")
    }
}

@Composable
fun DebtForm(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSubmit: (String, String, DebtType, Double, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var partyName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var debtType by remember { mutableStateOf(DebtType.RECEIVABLE) }
    var note by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }

    Text("New Debt Record", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
    Spacer(modifier = Modifier.height(16.dp))

    Row(modifier = Modifier.fillMaxWidth()) {
        FilterChip(
            selected = debtType == DebtType.RECEIVABLE,
            onClick = { debtType = DebtType.RECEIVABLE },
            label = { Text("Receivable (Owed to You)") },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilterChip(
            selected = debtType == DebtType.PAYABLE,
            onClick = { debtType = DebtType.PAYABLE },
            label = { Text("Payable (You Owe)") },
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = partyName,
        onValueChange = { partyName = it },
        label = { Text("Person / Counterparty Name") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Total Amount (SAR)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("Notes / Purpose") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = {
            val amt = amountText.toDoubleOrNull() ?: 0.0
            if (partyName.isNotEmpty() && amt > 0) {
                onSubmit(partyName, phone, debtType, amt, accountId, note)
            }
        },
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text("Save Debt Record")
    }
}

@Composable
fun GoalForm(
    onSubmit: (String, Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var initialText by remember { mutableStateOf("") }

    Text("New Savings Goal", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Goal Title (e.g. New Home Deposit)") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = targetText,
        onValueChange = { targetText = it },
        label = { Text("Target Goal Amount (SAR)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = initialText,
        onValueChange = { initialText = it },
        label = { Text("Initial Deposit (Optional)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = {
            val target = targetText.toDoubleOrNull() ?: 0.0
            val initAmt = initialText.toDoubleOrNull() ?: 0.0
            if (title.isNotEmpty() && target > 0) {
                onSubmit(title, target, initAmt)
            }
        },
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text("Create Goal")
    }
}

@Composable
fun BudgetForm(
    onSubmit: (String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }

    Text("New Budget Limit", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = category,
        onValueChange = { category = it },
        label = { Text("Category (e.g. Dining, Shopping)") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = limitText,
        onValueChange = { limitText = it },
        label = { Text("Monthly Limit (SAR)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = {
            val limit = limitText.toDoubleOrNull() ?: 0.0
            if (category.isNotEmpty() && limit > 0) {
                onSubmit(category, limit)
            }
        },
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text("Set Budget Limit")
    }
}

@Composable
fun BillForm(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSubmit: (String, Double, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Utilities") }
    var accountId by remember { mutableStateOf(selectedAccountId ?: accounts.firstOrNull()?.id ?: "") }

    Text("New Bill Subscription", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Bill Title (e.g., STC Fiber Internet)") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = { Text("Amount (SAR)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = {
            val amt = amountText.toDoubleOrNull() ?: 0.0
            if (title.isNotEmpty() && amt > 0) {
                onSubmit(title, amt, category, accountId)
            }
        },
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text("Add Bill")
    }
}

@Composable
fun StatementExportForm(
    onExportPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    Text("Export Statement Report", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
    Spacer(modifier = Modifier.height(12.dp))
    Text("Generate an official printable PDF account statement with net worth calculations.", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = {
            onExportPdf()
            onDismiss()
        },
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text("Generate PDF Report")
    }
}
