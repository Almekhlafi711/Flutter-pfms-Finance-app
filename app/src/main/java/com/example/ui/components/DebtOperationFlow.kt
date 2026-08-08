package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.provider.ContactsContract
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import coil.compose.AsyncImage
import kotlin.math.absoluteValue
import com.example.core.theme.DesignTokens
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.Account
import com.example.domain.model.DebtType
import com.example.domain.model.Person
import com.example.domain.model.PersonDebtAccount
import com.example.ui.screens.LedgerOperationType
import com.example.ui.theme.GreenIncome
import com.example.ui.theme.OrangeDebt
import com.example.ui.theme.RedExpense
import java.util.UUID
import kotlinx.coroutines.delay

enum class FlowStep {
    CHOOSE_OR_CREATE_PERSON,
    SELECT_EXISTING_PERSON,
    CREATE_NEW_PERSON,
    CHOOSE_OPERATION_TYPE,
    ENTER_DETAILS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtOperationFlow(
    persons: List<Person>,
    accounts: List<Account>,
    personDebtAccounts: List<PersonDebtAccount>,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onExecuteOperation: (
        person: Person,
        currency: String,
        operationType: LedgerOperationType,
        direction: DebtType?,
        amount: Double,
        accountId: String,
        notes: String
    ) -> Unit,
    onCreatePerson: (Person) -> Unit,
    initialPersonId: String? = null,
    initialCurrency: String? = null,
    initialOperationType: LedgerOperationType? = null
) {
    // Current Step State
    var currentStep by remember {
        mutableStateOf(
            if (initialPersonId != null) {
                if (initialOperationType != null) {
                    FlowStep.ENTER_DETAILS
                } else {
                    FlowStep.CHOOSE_OPERATION_TYPE
                }
            } else {
                FlowStep.CHOOSE_OR_CREATE_PERSON
            }
        )
    }

    // Context / Process States
    var selectedPerson by remember {
        mutableStateOf(persons.find { it.id == initialPersonId })
    }
    var selectedCurrency by remember {
        mutableStateOf(initialCurrency ?: "SAR")
    }
    var selectedOperationType by remember {
        mutableStateOf(initialOperationType)
    }

    // Form inputs
    var personName by remember { mutableStateOf("") }
    var personPhone by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var selectedDirection by remember { mutableStateOf<DebtType?>(null) }
    var showDirectionError by remember { mutableStateOf(false) }

    // Navigation Stack for going back
    val stepHistory = remember { mutableStateListOf<FlowStep>() }

    fun navigateTo(nextStep: FlowStep) {
        stepHistory.add(currentStep)
        currentStep = nextStep
    }

    fun navigateBack() {
        if (stepHistory.isNotEmpty()) {
            currentStep = stepHistory.removeAt(stepHistory.size - 1)
        } else {
            onDismiss()
        }
    }

    // Filter accounts by current currency
    val filteredAccounts = remember(accounts, selectedCurrency) {
        accounts.filter { it.currency.equals(selectedCurrency, ignoreCase = true) }
    }
    var selectedAccountId by remember(filteredAccounts) {
        mutableStateOf(filteredAccounts.firstOrNull()?.id ?: "")
    }
    val selectedAccount = filteredAccounts.find { it.id == selectedAccountId }

    // Dialog & overlay triggers
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showSuccessOverlay by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Drag handle standard space
                Spacer(modifier = Modifier.height(4.dp))

                // Standardized Header with back action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val canGoBack = stepHistory.isNotEmpty() || initialPersonId == null
                    if (canGoBack) {
                        IconButton(onClick = { navigateBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isArabic) "رجوع" else "Back"
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Text(
                        text = when (currentStep) {
                            FlowStep.CHOOSE_OR_CREATE_PERSON -> if (isArabic) "نوع الحساب" else "Account Type"
                            FlowStep.SELECT_EXISTING_PERSON -> if (isArabic) "اختر الشخص من الحسابات" else "Choose Person from Accounts"
                            FlowStep.CREATE_NEW_PERSON -> if (isArabic) "إضافة شخص جديد" else "Add New Person"
                            FlowStep.CHOOSE_OPERATION_TYPE -> if (isArabic) "اختر نوع العملية" else "Select Operation Type"
                            FlowStep.ENTER_DETAILS -> when (selectedOperationType) {
                                LedgerOperationType.ADD_DEBT -> if (isArabic) "إضافة دين" else "Add Debt"
                                LedgerOperationType.RECEIVE_PAYMENT -> if (isArabic) "استلام مبلغ" else "Receive Amount"
                                LedgerOperationType.PAY_DEBT -> if (isArabic) "سداد مبلغ" else "Pay Amount"
                                null -> ""
                            }
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // STEP CONTENT SWITCHER
                when (currentStep) {
                    FlowStep.CHOOSE_OR_CREATE_PERSON -> {
                        ChooseOrCreatePersonView(
                            isArabic = isArabic,
                            onChooseExisting = { navigateTo(FlowStep.SELECT_EXISTING_PERSON) },
                            onAddNewPerson = { navigateTo(FlowStep.CREATE_NEW_PERSON) }
                        )
                    }
                    FlowStep.SELECT_EXISTING_PERSON -> {
                        SelectExistingPersonView(
                            persons = persons,
                            personDebtAccounts = personDebtAccounts,
                            isArabic = isArabic,
                            fixedCurrency = initialCurrency,
                            onSelect = { person, currency ->
                                selectedPerson = person
                                selectedCurrency = currency
                                navigateTo(FlowStep.CHOOSE_OPERATION_TYPE)
                            }
                        )
                    }
                    FlowStep.CREATE_NEW_PERSON -> {
                        CreateNewPersonView(
                            isArabic = isArabic,
                            fixedCurrency = initialCurrency,
                            persons = persons,
                            accounts = accounts,
                            onSelectDuplicate = { duplicate ->
                                selectedPerson = duplicate
                                selectedCurrency = initialCurrency ?: "SAR"
                                navigateTo(FlowStep.CHOOSE_OPERATION_TYPE)
                            },
                            onSaveAtomic = { newPrs, currency, opType, direction, amount, accountId, notes ->
                                keyboardController?.hide()
                                onCreatePerson(newPrs)
                                selectedPerson = newPrs
                                selectedCurrency = currency
                                selectedOperationType = opType
                                selectedDirection = direction
                                amountText = if (amount > 0) amount.toString() else ""
                                selectedAccountId = accountId
                                notesText = notes
                                showConfirmationDialog = true
                            }
                        )
                    }
                    FlowStep.CHOOSE_OPERATION_TYPE -> {
                        ChooseOperationTypeView(
                            isArabic = isArabic,
                            personName = selectedPerson?.name ?: "",
                            currency = selectedCurrency,
                            onSelect = { opType ->
                                selectedOperationType = opType
                                navigateTo(FlowStep.ENTER_DETAILS)
                            }
                        )
                    }
                    FlowStep.ENTER_DETAILS -> {
                        EnterDetailsView(
                            operationType = selectedOperationType ?: LedgerOperationType.ADD_DEBT,
                            personName = selectedPerson?.name ?: "",
                            currency = selectedCurrency,
                            filteredAccounts = filteredAccounts,
                            selectedAccountId = selectedAccountId,
                            onAccountIdChange = { selectedAccountId = it },
                            amountText = amountText,
                            onAmountChange = { amountText = it },
                            notesText = notesText,
                            onNotesChange = { notesText = it },
                            selectedDirection = selectedDirection,
                            onDirectionChange = { selectedDirection = it },
                            showDirectionError = showDirectionError,
                            onShowDirectionErrorChange = { showDirectionError = it },
                            isArabic = isArabic,
                            onSubmit = {
                                keyboardController?.hide()
                                showConfirmationDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // STEP 6: Centered Confirmation Dialog
    if (showConfirmationDialog) {
        Dialog(onDismissRequest = { showConfirmationDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Icon & Header
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isArabic) "تأكيد تسجيل العملية" else "Confirm Transaction Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Properties List
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Person Name
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "اسم الشخص:" else "Person Name:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedPerson?.name ?: "", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        // Currency
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "العملة المالية:" else "Currency Context:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedCurrency, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        // Operation Type
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "نوع العملية:" else "Operation Type:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val typeText = when (selectedOperationType) {
                                LedgerOperationType.ADD_DEBT -> if (isArabic) "إضافة دين" else "Add Debt"
                                LedgerOperationType.RECEIVE_PAYMENT -> if (isArabic) "استلام مبلغ" else "Receive Amount"
                                LedgerOperationType.PAY_DEBT -> if (isArabic) "سداد مبلغ" else "Pay Amount"
                                null -> ""
                            }
                            Text(typeText, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        // Direction (Add debt only)
                        if (selectedOperationType == LedgerOperationType.ADD_DEBT && selectedDirection != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isArabic) "الاتجاه الفعلي:" else "Actual Direction:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val dirText = if (selectedDirection == DebtType.RECEIVABLE) (if (isArabic) "له (مستحق لك)" else "Lend (Receivable)") else (if (isArabic) "عليه (التزام عليك)" else "Borrow (Payable)")
                                Text(
                                    text = dirText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedDirection == DebtType.RECEIVABLE) GreenIncome else RedExpense
                                    )
                                )
                            }
                        }
                        // Amount
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isArabic) "المبلغ المالي:" else "Financial Amount:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val parsedVal = amountText.toDoubleOrNull() ?: 0.0
                            Text(
                                text = CurrencyFormatter.format(parsedVal, selectedCurrency),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            )
                        }
                        // Bank Account
                        if (selectedAccount != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isArabic) "حساب الخزينة:" else "Funding Source:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(selectedAccount.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        // Description
                        if (notesText.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isArabic) "الوصف والبيان:" else "Notes Description:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(notesText, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    // Confirm/Cancel Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showConfirmationDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isArabic) "إلغاء" else "Cancel")
                        }

                        Button(
                            onClick = {
                                showConfirmationDialog = false
                                showSuccessOverlay = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isArabic) "تأكيد" else "Confirm")
                        }
                    }
                }
            }
        }
    }

    // SUCCESS DIALOG WITH AUTOMATIC 0.7s EXPIRY
    if (showSuccessOverlay) {
        Dialog(onDismissRequest = {}) {
            LaunchedEffect(Unit) {
                // Execute actual database operations
                selectedPerson?.let { person ->
                    selectedOperationType?.let { opType ->
                        val parsedVal = amountText.toDoubleOrNull() ?: 0.0
                        onExecuteOperation(
                            person,
                            selectedCurrency,
                            opType,
                            selectedDirection,
                            parsedVal,
                            selectedAccountId,
                            notesText
                        )
                    }
                }
                delay(750) // Wait ~0.75 seconds then exit
                showSuccessOverlay = false
                onDismiss()
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .wrapContentSize()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(GreenIncome),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isArabic) "تم تسجيل العملية بنجاح" else "Transaction Saved Successfully",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GreenIncome,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ChooseOrCreatePersonView(
    isArabic: Boolean,
    onChooseExisting: () -> Unit,
    onAddNewPerson: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isArabic) "حدد خيار المستفيد من العملية المالية الحالية:" else "Choose recipient option for transaction:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onChooseExisting),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "شخص موجود" else "Existing Person",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isArabic) "اختر من جهات الاتصال أو العملاء الحاليين المسجلين مسبقًا." else "Select from previously registered contacts or clients.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAddNewPerson),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "إضافة شخص جديد" else "Add New Person",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isArabic) "إنشاء مستفيد جديد وتسجيل بياناته الأساسية في النظام." else "Create a new contact and register basic details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@Composable
fun SelectExistingPersonView(
    persons: List<Person>,
    personDebtAccounts: List<PersonDebtAccount>,
    isArabic: Boolean,
    fixedCurrency: String?,
    onSelect: (Person, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeCurrencyTab by remember { mutableStateOf(fixedCurrency ?: "SAR") }

    val activePersonsOnly = remember(persons) {
        persons.filter { it.isActive }
    }

    // Filter persons who have a debt ledger in activeCurrencyTab
    val personsInActiveCurrency = remember(activePersonsOnly, personDebtAccounts, activeCurrencyTab) {
        activePersonsOnly.filter { p ->
            personDebtAccounts.any { account ->
                account.personId == p.id && account.currency.equals(activeCurrencyTab, ignoreCase = true)
            }
        }
    }

    val filteredList = remember(personsInActiveCurrency, searchQuery) {
        personsInActiveCurrency.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    (it.phone?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (isArabic) "البحث بالاسم أو برقم الهاتف..." else "Search by name or phone...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Currency Tabs - Always show
        val tabs = listOf("SAR", "USD", "YER")
        TabRow(
            selectedTabIndex = tabs.indexOf(activeCurrencyTab).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEach { t ->
                val label = when (t) {
                    "SAR" -> if (isArabic) "ريال سعودي" else "SAR"
                    "USD" -> if (isArabic) "دولار" else "USD"
                    "YER" -> if (isArabic) "ريال يمني" else "YER"
                    else -> t
                }
                Tab(
                    selected = activeCurrencyTab == t,
                    onClick = { activeCurrencyTab = t },
                    text = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }
        }

        // List of People
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 280.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) "لا توجد حسابات ديون في هذه العملة" else "No ledger accounts found for this currency",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredList) { person ->
                    // Find the balance for this person in active currency
                    val matchingAccount = personDebtAccounts.find {
                        it.personId == person.id && it.currency.equals(activeCurrencyTab, ignoreCase = true)
                    }
                    val balanceAmount = matchingAccount?.totalRemainingAmount ?: 0.0
                    val isReceivable = matchingAccount?.mainDebt?.type == DebtType.RECEIVABLE

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(person, activeCurrencyTab) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar with initials
                            val initials = person.name.trim().split(" ")
                                .take(2)
                                .mapNotNull { it.firstOrNull()?.uppercase() }
                                .joinToString("")
                                .ifEmpty { "P" }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    initials,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = person.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = person.phone ?: (if (isArabic) "لا يوجد هاتف" else "No phone"),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Balance
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyFormatter.format(balanceAmount, activeCurrencyTab),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (balanceAmount > 0) {
                                            if (isReceivable) GreenIncome else RedExpense
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                )
                                if (balanceAmount > 0) {
                                    Text(
                                        text = if (isReceivable) {
                                            if (isArabic) "🟢 له (+)" else "🟢 Lend (+)"
                                        } else {
                                            if (isArabic) "🔴 عليه (-)" else "🔴 Borrow (-)"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isReceivable) GreenIncome else RedExpense,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateNewPersonView(
    isArabic: Boolean,
    fixedCurrency: String?,
    persons: List<Person>,
    accounts: List<Account>,
    onSelectDuplicate: (Person) -> Unit,
    onSaveAtomic: (
        person: Person,
        currency: String,
        opType: LedgerOperationType,
        direction: DebtType?,
        amount: Double,
        accountId: String,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var personCategory by remember { mutableStateOf("Personal") } // Personal or Institution
    var currencySelected by remember { mutableStateOf(fixedCurrency ?: "SAR") }
    
    var opTypeSelected by remember { mutableStateOf(LedgerOperationType.ADD_DEBT) }
    var debtDirectionSelected by remember { mutableStateOf<DebtType?>(DebtType.RECEIVABLE) }
    var amountText by remember { mutableStateOf("") }
    
    var isAccordionExpanded by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Filter accounts matching selected currency
    val matchedAccounts = remember(accounts, currencySelected) {
        accounts.filter { it.currency.equals(currencySelected, ignoreCase = true) }
    }
    var selectedAccountId by remember(matchedAccounts) {
        mutableStateOf(matchedAccounts.firstOrNull()?.id ?: "")
    }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Contact Picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let { contactUri ->
            try {
                val cr = context.contentResolver
                var contactName = ""
                var contactPhone = ""
                
                cr.query(contactUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            contactName = cursor.getString(nameIndex) ?: ""
                        }
                        
                        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                        val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        val hasPhone = if (hasPhoneIndex >= 0) cursor.getInt(hasPhoneIndex) > 0 else false
                        
                        if (hasPhone && idIndex >= 0) {
                            val contactId = cursor.getString(idIndex)
                            cr.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf(contactId),
                                null
                            )?.use { phoneCursor ->
                                if (phoneCursor.moveToFirst()) {
                                    val numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numberIndex >= 0) {
                                        contactPhone = phoneCursor.getString(numberIndex) ?: ""
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (contactName.isNotBlank() && name.isBlank()) {
                    name = contactName
                }
                if (contactPhone.isNotBlank()) {
                    phone = contactPhone.replace(Regex("[\\s\\-\\(\\)]"), "")
                }
            } catch (e: Exception) {
                Log.e("ContactPicker", "Error querying contact details", e)
            }
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
    }

    // Duplicate detection check
    val duplicatePerson = remember(phone, persons) {
        val cleanPhone = phone.trim().replace(Regex("[\\s\\-\\(\\)]"), "")
        if (cleanPhone.length >= 6) {
            persons.find { p ->
                val pPhone = p.phone?.trim()?.replace(Regex("[\\s\\-\\(\\)]"), "") ?: ""
                pPhone.isNotEmpty() && (pPhone.endsWith(cleanPhone) || cleanPhone.endsWith(pPhone))
            }
        } else {
            null
        }
    }

    // Gradient helper for avatar
    val nameGradient = remember(name) {
        if (name.isBlank()) {
            Pair(Color(0xFF6366F1), Color(0xFF8B5CF6))
        } else {
            val code = name.hashCode().absoluteValue
            val gradients = listOf(
                Pair(Color(0xFF6366F1), Color(0xFF8B5CF6)), // Indigo -> Violet
                Pair(Color(0xFF3B82F6), Color(0xFF06B6D4)), // Blue -> Cyan
                Pair(Color(0xFF10B981), Color(0xFF059669)), // Emerald -> Green
                Pair(Color(0xFFF59E0B), Color(0xFFD97706)), // Amber -> Orange
                Pair(Color(0xFFEF4444), Color(0xFFDC2626)), // Red -> Dark Red
                Pair(Color(0xFFEC4899), Color(0xFFF43F5E))  // Pink -> Rose
            )
            gradients[code % gradients.size]
        }
    }

    val initials = remember(name) {
        val clean = name.trim()
        if (clean.isEmpty()) "👤" else {
            val words = clean.split("\\s+".toRegex())
            if (words.size == 1) words[0].take(2).uppercase() else {
                val first = words[0].firstOrNull()?.toString() ?: ""
                val last = words.lastOrNull()?.firstOrNull()?.toString() ?: ""
                (first + last).uppercase()
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. IDENTITY CARD (بطاقة هوية الشخص) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Profile Picture Box
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(nameGradient.first, nameGradient.second)
                            )
                        )
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = if (name.isBlank()) 28.sp else 22.sp
                            )
                        )
                    }
                    
                    // Small Camera Icon indicator overlay
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Fields Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isArabic) "الاسم بالكامل *" else "Full Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isArabic) "رقم الهاتف (اختياري)" else "Phone (Optional)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingIcon = {
                            IconButton(onClick = { contactPickerLauncher.launch(null) }) {
                                Icon(
                                    imageVector = Icons.Default.ContactPhone,
                                    contentDescription = if (isArabic) "اختر من جهات الاتصال" else "Contacts Picker",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // --- DUPLICATE WARNING DIALOG / CARD ---
        if (duplicatePerson != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) {
                                "هذا الشخص مسجل مسبقاً باسم: \"${duplicatePerson.name}\""
                            } else {
                                "This number is already registered to: \"${duplicatePerson.name}\""
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) {
                                "انقر أدناه لإكمال العملية في حسابه بدلاً من التكرار."
                            } else {
                                "Click below to complete transaction on their existing account instead of duplicating."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onSelectDuplicate(duplicatePerson) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = if (isArabic) "نعم، اختر الشخص الحالي" else "Yes, Select Existing",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // --- 2. COLLAPSIBLE ACCORDION (بيانات إضافية) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAccordionExpanded = !isAccordionExpanded }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isArabic) "بيانات إضافية (اختياري)" else "Additional Data (Optional)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Icon(
                        imageVector = if (isAccordionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Accordion"
                    )
                }

                if (isAccordionExpanded) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Address field
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text(if (isArabic) "العنوان / اللقب" else "Address / Title") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Classification Type: Personal / Institution
                        Text(
                            text = if (isArabic) "تصنيف العميل:" else "Client Type:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Personal", "Institution").forEach { cat ->
                                val label = when (cat) {
                                    "Personal" -> if (isArabic) "👤 شخصي" else "👤 Personal"
                                    "Institution" -> if (isArabic) "🏢 مؤسسي / تجاري" else "🏢 Institutional / Business"
                                    else -> cat
                                }
                                val isSelected = personCategory == cat
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { personCategory = cat },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Notes field
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(if (isArabic) "ملاحظات وتفاصيل" else "Notes & Remarks") },
                            leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // --- 3. CURRENCY SELECTOR (تحديد العملة) ---
        if (fixedCurrency == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isArabic) "تحديد عملة دفتر الحسابات الأول *" else "Choose currency context for first debt ledger *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SAR", "USD", "YER").forEach { c ->
                            val nameStr = when (c) {
                                "SAR" -> if (isArabic) "SAR (ريال سعودي)" else "SAR"
                                "USD" -> if (isArabic) "USD (دولار أمريكي)" else "USD"
                                "YER" -> if (isArabic) "YER (ريال يمني)" else "YER"
                                else -> c
                            }
                            val isSelected = currencySelected == c
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { currencySelected = c },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                ),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = nameStr,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. TRANSACTION TYPE & DIRECTION (نوع العملية والاتجاه) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "تحديد الإجراء والعملية الأولى *" else "First Financial Action Type *",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                // Select Operation Type Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple(LedgerOperationType.ADD_DEBT, if (isArabic) "إضافة دين" else "Add Debt", Color(0xFF8B5CF6)),
                        Triple(LedgerOperationType.RECEIVE_PAYMENT, if (isArabic) "استلام مبلغ" else "Receive Pay", GreenIncome),
                        Triple(LedgerOperationType.PAY_DEBT, if (isArabic) "سداد مبلغ" else "Pay Debt", RedExpense)
                    ).forEach { (type, label, colorVal) ->
                        val isSelected = opTypeSelected == type
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { opTypeSelected = type },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) colorVal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            border = if (isSelected) BorderStroke(1.5.dp, colorVal) else null,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colorVal else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                // If adding debt, show Direction selectors "له" vs "عليه"
                if (opTypeSelected == LedgerOperationType.ADD_DEBT) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    Text(
                        text = if (isArabic) "اتجاه الدين الحالي (إلزامي):" else "Current Debt Direction (Required):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Lend/Receivable (له)
                        val isLend = debtDirectionSelected == DebtType.RECEIVABLE
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { debtDirectionSelected = DebtType.RECEIVABLE },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLend) GreenIncome.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                            ),
                            border = if (isLend) BorderStroke(2.dp, GreenIncome) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isLend) GreenIncome else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isArabic) "له (مستحق لك)" else "Lend (Owed to me)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isLend) GreenIncome else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isArabic) "مبلغ أقرضته له" else "You loaned to them",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Borrow/Payable (عليه)
                        val isBorrow = debtDirectionSelected == DebtType.PAYABLE
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { debtDirectionSelected = DebtType.PAYABLE },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBorrow) RedExpense.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                            ),
                            border = if (isBorrow) BorderStroke(2.dp, RedExpense) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isBorrow) RedExpense else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isArabic) "عليه (التزام عليك)" else "Borrow (Owed by me)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isBorrow) RedExpense else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isArabic) "مبلغ اقترضته منه" else "You borrowed from them",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 5. AMOUNT & ACCOUNT SELECTION (المبلغ والحساب المالي) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isArabic) "المبلغ المالي ومصدر التمويل *" else "Amount & Payment Source *",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                // Large Banking-style Amount Input field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = { Text("0.00", style = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.outline)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    ),
                    suffix = {
                        Text(
                            text = currencySelected,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Filtered Bank Accounts Dropdown Selector
                if (matchedAccounts.isNotEmpty()) {
                    val currentSelAccount = matchedAccounts.find { it.id == selectedAccountId } ?: matchedAccounts.firstOrNull()
                    var isAccountDropdownExpanded by remember { mutableStateOf(false) }
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isArabic) "حساب الدفع والخزينة:" else "Funding Source Account:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Styled trigger box mimicking high-grade bank card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable { isAccountDropdownExpanded = true }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Color(
                                                    android.graphics.Color.parseColor(
                                                        currentSelAccount?.colorHex ?: "#0EA5E9"
                                                    )
                                                )
                                            )
                                    )
                                    Text(
                                        text = currentSelAccount?.name ?: "",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "(${CurrencyFormatter.format(currentSelAccount?.balance ?: 0.0, currentSelAccount?.currency ?: "")})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = isAccountDropdownExpanded,
                            onDismissRequest = { isAccountDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            matchedAccounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Color(
                                                            android.graphics.Color.parseColor(acc.colorHex)
                                                        )
                                                    )
                                            )
                                            Text(acc.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                "(${CurrencyFormatter.format(acc.balance, acc.currency)})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedAccountId = acc.id
                                        isAccountDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 6. ATOMIC SAVE ACTION BUTTON ---
        val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
        val isInputValid = name.isNotBlank() && parsedAmount > 0.0 && selectedAccountId.isNotEmpty()

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (isInputValid) {
                    val finalPrsId = "prs_" + UUID.randomUUID().toString().take(8)
                    val newPrs = Person(
                        id = finalPrsId,
                        name = name.trim(),
                        phone = phone.ifBlank { null },
                        category = personCategory,
                        notes = notes.trim(),
                        isActive = true,
                        currency = currencySelected
                    )
                    onSaveAtomic(
                        newPrs,
                        currencySelected,
                        opTypeSelected,
                        if (opTypeSelected == LedgerOperationType.ADD_DEBT) debtDirectionSelected else null,
                        parsedAmount,
                        selectedAccountId,
                        notes.ifBlank { if (isArabic) "الرصيد الافتتاحي عند الإنشاء" else "Opening initial balance" }
                    )
                }
            },
            enabled = isInputValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = if (isArabic) "💾 حفظ الحساب وتسجيل العملية" else "💾 Save Account & Save Debt",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ChooseOperationTypeView(
    isArabic: Boolean,
    personName: String,
    currency: String,
    onSelect: (LedgerOperationType) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = if (isArabic) "تحديد الإجراء المالي" else "Define Financial Action",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "$personName • $currency",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        // 3 Options Cards
        listOf(
            Triple(LedgerOperationType.ADD_DEBT, if (isArabic) "🟣 إضافة دين" else "🟣 Add Debt", if (isArabic) "تسجيل التزام مالي جديد (له أو عليه)" else "Record new debt principal (Lend/Borrow)"),
            Triple(LedgerOperationType.RECEIVE_PAYMENT, if (isArabic) "🟢 استلام مبلغ" else "🟢 Receive Amount", if (isArabic) "تسجيل استلام دفعة مالية من الشخص" else "Receive payment cash/transfer from contact"),
            Triple(LedgerOperationType.PAY_DEBT, if (isArabic) "🔴 سداد مبلغ" else "🔴 Pay Amount", if (isArabic) "تسجيل تسديد دفعة مالية إلى الشخص" else "Pay off / settle payment to contact")
        ).forEach { (type, title, desc) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(type) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun EnterDetailsView(
    operationType: LedgerOperationType,
    personName: String,
    currency: String,
    filteredAccounts: List<Account>,
    selectedAccountId: String,
    onAccountIdChange: (String) -> Unit,
    amountText: String,
    onAmountChange: (String) -> Unit,
    notesText: String,
    onNotesChange: (String) -> Unit,
    selectedDirection: DebtType?,
    onDirectionChange: (DebtType?) -> Unit,
    showDirectionError: Boolean,
    onShowDirectionErrorChange: (Boolean) -> Unit,
    isArabic: Boolean,
    onSubmit: () -> Unit
) {
    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val selectedAccount = filteredAccounts.find { it.id == selectedAccountId }

    // Validation check
    val isValid = parsedAmount > 0 && selectedAccountId.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "$personName • $currency",
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        )

        // 1. ADD_DEBT direction choice (له / عليه) - MUST NOT BE SELECTED BY DEFAULT
        if (operationType == LedgerOperationType.ADD_DEBT) {
            Column {
                Text(
                    text = if (isArabic) "حدد اتجاه الديون *" else "Choose Debt Direction *",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // RECEIVABLE (له)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onDirectionChange(DebtType.RECEIVABLE)
                                onShowDirectionErrorChange(false)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedDirection == DebtType.RECEIVABLE) GreenIncome.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = if (selectedDirection == DebtType.RECEIVABLE) BorderStroke(2.dp, GreenIncome) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(GreenIncome),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "له (مستحق لك)" else "Lend",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    // PAYABLE (عليه)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onDirectionChange(DebtType.PAYABLE)
                                onShowDirectionErrorChange(false)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedDirection == DebtType.PAYABLE) RedExpense.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = if (selectedDirection == DebtType.PAYABLE) BorderStroke(2.dp, RedExpense) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(RedExpense),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "عليه (التزام)" else "Borrow",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                if (showDirectionError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic) "يجب اختيار الاتجاه للمتابعة" else "Please select the direction of debt",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // 2. Amount Input
        OutlinedTextField(
            value = amountText,
            onValueChange = { onAmountChange(it) },
            label = { Text(if (isArabic) "المبلغ المالي *" else "Financial Amount *") },
            suffix = { Text(currency, fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // 3. Notes
        OutlinedTextField(
            value = notesText,
            onValueChange = { onNotesChange(it) },
            label = { Text(if (isArabic) "الوصف / الملاحظات (اختياري)" else "Description / Notes (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = 1,
            maxLines = 3
        )

        // 4. Associated Account Selection (filtered by currency, collapsed dropdown view)
        if (filteredAccounts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isArabic) "الحساب المالي المرتبط *" else "Linked Wallet / Account *",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                var isExpanded by remember { mutableStateOf(false) }

                if (!isExpanded) {
                    selectedAccount?.let { acc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(acc.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(
                                            "${if (isArabic) "الرصيد: " else "Balance: "}${CurrencyFormatter.format(acc.balance, acc.currency)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Expand")
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(
                            modifier = Modifier.padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredAccounts.forEach { acc ->
                                val isSelected = acc.id == selectedAccountId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable {
                                            onAccountIdChange(acc.id)
                                            isExpanded = false
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(acc.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal))
                                            Text(
                                                "${if (isArabic) "الرصيد: " else "Balance: "}${CurrencyFormatter.format(acc.balance, acc.currency)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (operationType == LedgerOperationType.ADD_DEBT && selectedDirection == null) {
                    onShowDirectionErrorChange(true)
                } else {
                    onSubmit()
                }
            },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (isArabic) "إضافة العملية" else "Add Operation", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}
