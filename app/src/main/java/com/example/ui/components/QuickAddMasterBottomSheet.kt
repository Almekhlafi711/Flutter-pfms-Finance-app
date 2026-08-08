package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.GreenIncome
import com.example.core.theme.RedExpense
import com.example.core.theme.BlueTransfer
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.Account
import com.example.domain.model.AccountType
import com.example.domain.model.GroupedAccount
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddMasterBottomSheet(
    accounts: List<Account>,
    groupedAccounts: List<GroupedAccount>,
    isArabic: Boolean = false,
    initialType: Int = 1, // 0: Income, 1: Expense, 2: Transfer (default to expense as most common)
    onDismiss: () -> Unit,
    onAddIncome: (Double, String, String, String, String, String) -> Unit,
    onAddExpense: (Double, String, String, String, String, String) -> Unit,
    onAddTransfer: (Double, String, String, String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialType) } // 0: Income, 1: Expense, 2: Transfer

    val defaultAcc = accounts.firstOrNull()
    var selectedAccountId by remember { mutableStateOf(defaultAcc?.id ?: "") }
    var targetAccountId by remember { mutableStateOf(accounts.getOrNull(1)?.id ?: defaultAcc?.id ?: "") }

    val currentAccount = accounts.find { it.id == selectedAccountId } ?: defaultAcc
    val selectedCurrency = currentAccount?.currency ?: "SAR"

    // Find all currencies for the current account's institution if grouped
    val currentInstitutionGroup = groupedAccounts.find { group ->
        group.accounts.any { it.id == selectedAccountId }
    }
    val availableCurrencies = currentInstitutionGroup?.accounts?.map { it.currency } ?: listOf(selectedCurrency)

    // Categories
    val incomeCategories = if (isArabic) {
        listOf("راتب" to "Salary", "مبيعات" to "Sales", "أرباح" to "Profits", "هبة" to "Gift", "دخل آخر" to "Other Income")
    } else {
        listOf("Salary" to "Salary", "Sales" to "Sales", "Profits" to "Profits", "Gift" to "Gift", "Other Income" to "Other Income")
    }

    val expenseCategories = if (isArabic) {
        listOf("طعام" to "Dining", "وقود" to "Fuel", "سكن" to "Housing", "صحة" to "Health", "تعليم" to "Education", "مصروف آخر" to "Other Expense")
    } else {
        listOf("Dining" to "Dining", "Fuel" to "Fuel", "Housing" to "Housing", "Health" to "Health", "Education" to "Education", "Other Expense" to "Other Expense")
    }

    var selectedCategory by remember(selectedTab) {
        mutableStateOf(if (selectedTab == 0) incomeCategories.first().second else expenseCategories.first().second)
    }

    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    var showAccountPicker by remember { mutableStateOf(false) }
    var showTargetAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val currentDateStr = dateFormat.format(Date())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Segmented Control for Transaction Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "تسجيل حركة سريعة" else "Quick Transaction",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Type Segmented Control (Income / Expense / Transfer)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = if (isArabic) listOf("دخل", "مصروف", "تحويل") else listOf("Income", "Expense", "Transfer")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        val activeColor = when (index) {
                            0 -> GreenIncome
                            1 -> RedExpense
                            else -> BlueTransfer
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable { selectedTab = index },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) activeColor else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedTab == 2) {
                // TRANSFER FORM
                // 1. Source Account Card
                Text(
                    text = if (isArabic) "الحساب المصدر" else "Source Account",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                AccountSelectionCard(
                    account = currentAccount,
                    isArabic = isArabic,
                    onClick = { showAccountPicker = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Target Account Card
                Text(
                    text = if (isArabic) "الحساب المستهدف" else "Target Account",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                val targetAcc = accounts.find { it.id == targetAccountId } ?: accounts.firstOrNull()
                AccountSelectionCard(
                    account = targetAcc,
                    isArabic = isArabic,
                    onClick = { showTargetAccountPicker = true }
                )
            } else {
                // INCOME / EXPENSE FORM
                // 1. Account Card
                Text(
                    text = if (isArabic) "الحساب" else "Account",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                AccountSelectionCard(
                    account = currentAccount,
                    isArabic = isArabic,
                    currencies = availableCurrencies,
                    selectedCurrency = selectedCurrency,
                    onCurrencySelected = { newCurr ->
                        // Switch account matching this currency in the same institution if available
                        currentInstitutionGroup?.accounts?.find { it.currency == newCurr }?.let {
                            selectedAccountId = it.id
                        }
                    },
                    onClick = { showAccountPicker = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Category Dropdown Card
                Text(
                    text = if (isArabic) "الفئة" else "Category",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryPicker = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isArabic) {
                                        if (selectedTab == 0) incomeCategories.find { it.second == selectedCategory }?.first ?: selectedCategory
                                        else expenseCategories.find { it.second == selectedCategory }?.first ?: selectedCategory
                                    } else selectedCategory,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (isArabic) "اضغط للتغيير" else "Tap to change category",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Date Display Card
            Text(
                text = if (isArabic) "التاريخ" else "Date",
                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = currentDateStr, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    Text(text = if (isArabic) "اليوم" else "Today", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Amount Input Field
            Text(
                text = if (isArabic) "المبلغ" else "Amount",
                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                placeholder = { Text("0.00") },
                trailingIcon = {
                    Text(
                        text = selectedCurrency,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Description / Note
            Text(
                text = if (isArabic) "الوصف (اختياري)" else "Description (Optional)",
                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text(if (isArabic) "أدخل ملاحظة أو وصف..." else "Add note or description...") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            val buttonColor = when (selectedTab) {
                0 -> GreenIncome
                1 -> RedExpense
                else -> BlueTransfer
            }
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        when (selectedTab) {
                            0 -> {
                                if (selectedAccountId.isNotEmpty()) {
                                    onAddIncome(amt, selectedAccountId, selectedCategory, "", noteText, selectedCurrency)
                                }
                            }
                            1 -> {
                                if (selectedAccountId.isNotEmpty()) {
                                    onAddExpense(amt, selectedAccountId, selectedCategory, "", noteText, selectedCurrency)
                                }
                            }
                            2 -> {
                                if (selectedAccountId.isNotEmpty() && targetAccountId.isNotEmpty() && selectedAccountId != targetAccountId) {
                                    onAddTransfer(amt, selectedAccountId, targetAccountId, noteText, selectedCurrency)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Text(
                    text = when (selectedTab) {
                        0 -> if (isArabic) "حفظ دخل جديد" else "Save Income"
                        1 -> if (isArabic) "حفظ مصروف جديد" else "Save Expense"
                        else -> if (isArabic) "تنفيذ التحويل" else "Execute Transfer"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            }
        }
    }

    // Account Picker Bottom Sheet (Selection only, no creation)
    if (showAccountPicker) {
        AccountPickerOnlyBottomSheet(
            groupedAccounts = groupedAccounts,
            isArabic = isArabic,
            onDismiss = { showAccountPicker = false },
            onAccountSelected = { acc ->
                selectedAccountId = acc.id
                showAccountPicker = false
            }
        )
    }

    if (showTargetAccountPicker) {
        AccountPickerOnlyBottomSheet(
            groupedAccounts = groupedAccounts,
            isArabic = isArabic,
            onDismiss = { showTargetAccountPicker = false },
            onAccountSelected = { acc ->
                targetAccountId = acc.id
                showTargetAccountPicker = false
            }
        )
    }

    // Category Picker Bottom Sheet
    if (showCategoryPicker) {
        CategoryPickerBottomSheet(
            isArabic = isArabic,
            isIncome = selectedTab == 0,
            incomeCategories = incomeCategories,
            expenseCategories = expenseCategories,
            onDismiss = { showCategoryPicker = false },
            onCategorySelected = { cat ->
                selectedCategory = cat
                showCategoryPicker = false
            }
        )
    }
}

@Composable
fun AccountSelectionCard(
    account: Account?,
    isArabic: Boolean,
    currencies: List<String> = emptyList(),
    selectedCurrency: String = "SAR",
    onCurrencySelected: (String) -> Unit = {},
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = account?.name ?: (if (isArabic) "اختر الحساب" else "Select Account"),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = if (account != null) CurrencyFormatter.format(account.balance, account.currency) else "---",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Icon(imageVector = Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // If multiple currencies available in this institution group, show mini segmented control
            if (currencies.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currencies.forEach { curr ->
                        val isCurrSelected = curr == selectedCurrency
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clickable { onCurrencySelected(curr) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = curr,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerOnlyBottomSheet(
    groupedAccounts: List<GroupedAccount>,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onAccountSelected: (Account) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .heightIn(max = 500.dp)
        ) {
            Text(
                text = if (isArabic) "اختر المؤسسة / الحساب" else "Select Account / Institution",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val activeGroups = groupedAccounts.filter { !it.isArchived }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                activeGroups.forEach { group ->
                    group.accounts.forEach { acc ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAccountSelected(acc) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "${group.name} (${acc.currency})",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (isArabic) getAccountTypeNameAr(group.type) else group.type.name,
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                                Text(
                                    text = CurrencyFormatter.format(acc.balance, acc.currency),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerBottomSheet(
    isArabic: Boolean,
    isIncome: Boolean,
    incomeCategories: List<Pair<String, String>>,
    expenseCategories: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = if (isArabic) "اختر الفئة" else "Select Category",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val categories = if (isIncome) incomeCategories else expenseCategories
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { (ar, en) ->
                    val label = if (isArabic) ar else en
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(en) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Label, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }
        }
    }
}
