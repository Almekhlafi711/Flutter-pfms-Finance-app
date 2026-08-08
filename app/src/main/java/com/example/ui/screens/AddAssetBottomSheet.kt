package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Account
import com.example.domain.model.AssetType
import com.example.ui.viewmodel.PfmsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssetBottomSheet(
    viewModel: PfmsViewModel,
    accounts: List<Account>,
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // MASTER SPEC: Asset categories mapping
    val categories = remember {
        listOf(
            AssetCategoryInfo(AssetType.REAL_ESTATE, if (isArabic) "عقار" else "Real Estate", Icons.Default.Home),
            AssetCategoryInfo(AssetType.GOLD, if (isArabic) "ذهب" else "Gold", Icons.Default.MonetizationOn),
            AssetCategoryInfo(AssetType.STOCKS, if (isArabic) "استثمار" else "Investment", Icons.Default.TrendingUp),
            AssetCategoryInfo(AssetType.VEHICLE, if (isArabic) "مركبة" else "Vehicle", Icons.Default.DirectionsCar),
            AssetCategoryInfo(AssetType.COLLECTIBLE, if (isArabic) "مجوهرات" else "Jewelry", Icons.Default.Diamond),
            AssetCategoryInfo(AssetType.ELECTRONICS, if (isArabic) "معدات" else "Equipment", Icons.Default.Build),
            AssetCategoryInfo(AssetType.OTHER, if (isArabic) "أخرى" else "Other", Icons.Default.Category)
        )
    }

    var selectedCategory by remember { mutableStateOf(AssetType.REAL_ESTATE) }
    var pendingCategoryToSwitch by remember { mutableStateOf<AssetType?>(null) }
    var showTypeChangeConfirmation by remember { mutableStateOf(false) }

    // Account list filtered STRICTLY to SAR accounts
    val sarAccounts = remember(accounts) {
        accounts.filter { it.currency.equals("SAR", ignoreCase = true) }
    }
    var selectedAccount by remember { mutableStateOf<Account?>(sarAccounts.firstOrNull()) }

    // Common financial fields
    var name by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var currentValue by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }

    // Real Estate specific fields
    var propertyType by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Gold specific fields
    var goldType by remember { mutableStateOf("") }
    var goldWeight by remember { mutableStateOf("") }
    var weightUnit by remember { mutableStateOf(if (isArabic) "جرام" else "Grams") }
    var goldKarat by remember { mutableStateOf("") }

    // Investment specific fields
    var investmentType by remember { mutableStateOf("") }
    var entityCompany by remember { mutableStateOf("") }
    var portfolioNumber by remember { mutableStateOf("") }
    var unitsCount by remember { mutableStateOf("") }
    var unitPurchasePrice by remember { mutableStateOf("") }

    // Vehicle specific fields
    var vehicleType by remember { mutableStateOf("") }
    var manufacturer by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var manufactureYear by remember { mutableStateOf("") }
    var plateNumber by remember { mutableStateOf("") }
    var vinNumber by remember { mutableStateOf("") }

    // Jewelry specific fields
    var jewelryType by remember { mutableStateOf("") }
    var metalType by remember { mutableStateOf("") }
    var jewelryWeight by remember { mutableStateOf("") }
    var jewelryKarat by remember { mutableStateOf("") }
    var gemstonesDetails by remember { mutableStateOf("") }

    // Equipment specific fields
    var equipmentType by remember { mutableStateOf("") }
    var equipmentManufacturer by remember { mutableStateOf("") }
    var equipmentModel by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }

    // UI sheet toggles & collapsible tile
    var showAccountPickerSheet by remember { mutableStateOf(false) }
    var isNotesExpanded by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(purchaseDate))

    // Calculate whether user has entered any data to trigger switch warning guard
    val hasEnteredData = remember(
        name, purchasePrice, currentValue, notes, propertyType, location, description,
        goldType, goldWeight, goldKarat, investmentType, entityCompany, portfolioNumber,
        unitsCount, unitPurchasePrice, vehicleType, manufacturer, model, manufactureYear,
        plateNumber, vinNumber, jewelryType, metalType, jewelryWeight, jewelryKarat,
        gemstonesDetails, equipmentType, equipmentManufacturer, equipmentModel, serialNumber
    ) {
        name.isNotBlank() || purchasePrice.isNotBlank() || currentValue.isNotBlank() || notes.isNotBlank() ||
                propertyType.isNotBlank() || location.isNotBlank() || description.isNotBlank() ||
                goldType.isNotBlank() || goldWeight.isNotBlank() || goldKarat.isNotBlank() ||
                investmentType.isNotBlank() || entityCompany.isNotBlank() || portfolioNumber.isNotBlank() ||
                unitsCount.isNotBlank() || unitPurchasePrice.isNotBlank() || vehicleType.isNotBlank() ||
                manufacturer.isNotBlank() || model.isNotBlank() || manufactureYear.isNotBlank() ||
                plateNumber.isNotBlank() || vinNumber.isNotBlank() || jewelryType.isNotBlank() ||
                metalType.isNotBlank() || jewelryWeight.isNotBlank() || jewelryKarat.isNotBlank() ||
                gemstonesDetails.isNotBlank() || equipmentType.isNotBlank() || equipmentManufacturer.isNotBlank() ||
                equipmentModel.isNotBlank() || serialNumber.isNotBlank()
    }

    // Function to reset fields when switching category after confirmation
    fun resetSpecificFields() {
        name = ""
        propertyType = ""
        location = ""
        description = ""
        goldType = ""
        goldWeight = ""
        goldKarat = ""
        investmentType = ""
        entityCompany = ""
        portfolioNumber = ""
        unitsCount = ""
        unitPurchasePrice = ""
        vehicleType = ""
        manufacturer = ""
        model = ""
        manufactureYear = ""
        plateNumber = ""
        vinNumber = ""
        jewelryType = ""
        metalType = ""
        jewelryWeight = ""
        jewelryKarat = ""
        gemstonesDetails = ""
        equipmentType = ""
        equipmentManufacturer = ""
        equipmentModel = ""
        serialNumber = ""
        purchasePrice = ""
        currentValue = ""
        notes = ""
    }

    // Handle Category Click
    val onSelectCategoryClick: (AssetType) -> Unit = { targetCategory ->
        if (targetCategory != selectedCategory) {
            if (hasEnteredData) {
                pendingCategoryToSwitch = targetCategory
                showTypeChangeConfirmation = true
            } else {
                selectedCategory = targetCategory
            }
        }
    }

    // Validation Logic per Category
    val isFormValid = remember(
        selectedCategory, name, purchasePrice, selectedAccount, propertyType,
        goldType, goldWeight, goldKarat, investmentType, vehicleType, jewelryType, equipmentType
    ) {
        val hasPrice = purchasePrice.isNotBlank() && (purchasePrice.toDoubleOrNull() ?: 0.0) > 0
        val hasAccount = selectedAccount != null
        val hasName = name.isNotBlank()

        if (!hasPrice || !hasAccount || !hasName) false
        else when (selectedCategory) {
            AssetType.REAL_ESTATE -> propertyType.isNotBlank()
            AssetType.GOLD -> goldType.isNotBlank() && goldWeight.isNotBlank() && goldKarat.isNotBlank()
            AssetType.STOCKS, AssetType.INVESTMENTS -> investmentType.isNotBlank()
            AssetType.VEHICLE -> vehicleType.isNotBlank()
            AssetType.COLLECTIBLE -> jewelryType.isNotBlank()
            AssetType.ELECTRONICS -> equipmentType.isNotBlank()
            else -> true
        }
    }

    // Financial Calculation
    val pPriceVal = purchasePrice.toDoubleOrNull() ?: 0.0
    val cPriceVal = currentValue.toDoubleOrNull() ?: pPriceVal
    val calculatedProfit = cPriceVal - pPriceVal

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .imePadding()
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

            Spacer(modifier = Modifier.height(14.dp))

            // Sheet Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6B46C1).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFF6B46C1),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "إضافة أصل" else "Add Asset",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                    Text(
                        text = if (isArabic) "اختر نوع الأصل لتعبئة النموذج المخصص (SAR)" else "Select asset type to populate tailored form (SAR)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Category Selector Carousel / Horizontal Strip
            Text(
                text = if (isArabic) "نوع الأصل *" else "Asset Type *",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat.type == selectedCategory || (selectedCategory == AssetType.INVESTMENTS && cat.type == AssetType.STOCKS)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCategoryClick(cat.type) },
                        label = {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6B46C1),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Form Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated Switcher between dynamic forms
                AnimatedContent(
                    targetState = selectedCategory,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "DynamicAssetForm"
                ) { currentCategory ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            when (currentCategory) {
                                AssetType.REAL_ESTATE -> RealEstateForm(
                                    isArabic = isArabic,
                                    name = name,
                                    onNameChange = { name = it },
                                    propertyType = propertyType,
                                    onPropertyTypeChange = { propertyType = it },
                                    location = location,
                                    onLocationChange = { location = it },
                                    description = description,
                                    onDescriptionChange = { description = it }
                                )

                                AssetType.GOLD -> GoldForm(
                                    isArabic = isArabic,
                                    name = name,
                                    onNameChange = { name = it },
                                    goldType = goldType,
                                    onGoldTypeChange = { goldType = it },
                                    goldWeight = goldWeight,
                                    onGoldWeightChange = { goldWeight = it },
                                    weightUnit = weightUnit,
                                    onWeightUnitChange = { weightUnit = it },
                                    goldKarat = goldKarat,
                                    onGoldKaratChange = { goldKarat = it }
                                )

                                AssetType.STOCKS, AssetType.INVESTMENTS -> InvestmentForm(
                                    isArabic = isArabic,
                                    name = name,
                                    onNameChange = { name = it },
                                    investmentType = investmentType,
                                    onInvestmentTypeChange = { investmentType = it },
                                    entityCompany = entityCompany,
                                    onEntityCompanyChange = { entityCompany = it },
                                    portfolioNumber = portfolioNumber,
                                    onPortfolioNumberChange = { portfolioNumber = it },
                                    unitsCount = unitsCount,
                                    onUnitsCountChange = {
                                        unitsCount = it
                                        val u = it.toDoubleOrNull() ?: 0.0
                                        val p = unitPurchasePrice.toDoubleOrNull() ?: 0.0
                                        if (u > 0 && p > 0 && purchasePrice.isBlank()) {
                                            purchasePrice = (u * p).toString()
                                        }
                                    },
                                    unitPurchasePrice = unitPurchasePrice,
                                    onUnitPurchasePriceChange = {
                                        unitPurchasePrice = it
                                        val u = unitsCount.toDoubleOrNull() ?: 0.0
                                        val p = it.toDoubleOrNull() ?: 0.0
                                        if (u > 0 && p > 0) {
                                            purchasePrice = (u * p).toString()
                                        }
                                    }
                                )

                                AssetType.VEHICLE -> VehicleForm(
                                    isArabic = isArabic,
                                    name = name,
                                    onNameChange = { name = it },
                                    vehicleType = vehicleType,
                                    onVehicleTypeChange = { vehicleType = it },
                                    manufacturer = manufacturer,
                                    onManufacturerChange = { manufacturer = it },
                                    model = model,
                                    onModelChange = { model = it },
                                    manufactureYear = manufactureYear,
                                    onManufactureYearChange = { manufactureYear = it },
                                    plateNumber = plateNumber,
                                    onPlateNumberChange = { plateNumber = it },
                                    vinNumber = vinNumber,
                                    onVinNumberChange = { vinNumber = it }
                                )

                                AssetType.COLLECTIBLE -> JewelryForm(
                                    isArabic = isArabic,
                                    name = name,
                                    onNameChange = { name = it },
                                    jewelryType = jewelryType,
                                    onJewelryTypeChange = { jewelryType = it },
                                    metalType = metalType,
                                    onMetalTypeChange = { metalType = it },
                                    jewelryWeight = jewelryWeight,
                                    onJewelryWeightChange = { jewelryWeight = it },
                                    jewelryKarat = jewelryKarat,
                                    onJewelryKaratChange = { jewelryKarat = it },
                                    gemstonesDetails = gemstonesDetails,
                                    onGemstonesDetailsChange = { gemstonesDetails = it }
                                )

                                AssetType.ELECTRONICS -> EquipmentForm(
                                    isArabic = isArabic,
                                    name = name,
                                    onNameChange = { name = it },
                                    equipmentType = equipmentType,
                                    onEquipmentTypeChange = { equipmentType = it },
                                    equipmentManufacturer = equipmentManufacturer,
                                    onEquipmentManufacturerChange = { equipmentManufacturer = it },
                                    equipmentModel = equipmentModel,
                                    onEquipmentModelChange = { equipmentModel = it },
                                    serialNumber = serialNumber,
                                    onSerialNumberChange = { serialNumber = it }
                                )

                                else -> OtherAssetForm(
                                    isArabic = isArabic,
                                    name = name,
                                    onNameChange = { name = it },
                                    description = description,
                                    onDescriptionChange = { description = it }
                                )
                            }
                        }
                    }
                }

                // 2. Financial Values Card (Purchase & Current Prices - SAR ONLY)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (isArabic) "القيم والأسعار (بالريال السعودي SAR)" else "VALUES & PRICES (SAR ONLY)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = purchasePrice,
                                onValueChange = { purchasePrice = it },
                                label = { Text(if (isArabic) "سعر الشراء *" else "Purchase Price *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "SAR",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = currentValue,
                                onValueChange = { currentValue = it },
                                label = { Text(if (isArabic) "القيمة الحالية (افتراضيًا سعر الشراء)" else "Current Value (Defaults to purchase price)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "SAR",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }

                        // Auto-Calculated Profit / Loss Indicator
                        if (pPriceVal > 0.0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (calculatedProfit >= 0) Color(0xFF15803D).copy(alpha = 0.12f) else Color(0xFFB91C1C).copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isArabic) "الأرباح / الخسائر المحسوبة:" else "Calculated Profit/Loss:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${if (calculatedProfit >= 0) "+" else ""}SAR ${String.format(Locale.US, "%,.2f", calculatedProfit)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (calculatedProfit >= 0) Color(0xFF15803D) else Color(0xFFB91C1C)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Purchase Account Card (SAR ACCOUNTS ONLY)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAccountPickerSheet = true },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    text = if (isArabic) "حساب الشراء *" else "Purchase Account *",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = selectedAccount?.name ?: if (isArabic) "اختر حساب بالريال" else "Select SAR Account",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                if (selectedAccount != null) {
                                    Text(
                                        text = if (isArabic) "الرصيد: ${String.format(Locale.US, "%,.2f", selectedAccount!!.balance)} SAR" else "Balance: ${String.format(Locale.US, "%,.2f", selectedAccount!!.balance)} SAR",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = "Select Account",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4. Purchase Date Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val calendar = Calendar.getInstance().apply { timeInMillis = purchaseDate }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCalendar = Calendar.getInstance()
                                    newCalendar.set(year, month, dayOfMonth)
                                    purchaseDate = newCalendar.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "تاريخ الشراء / الاستثمار" else "Purchase / Investment Date",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }

                // 5. Collapsible Notes & Attachments Tile
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isNotesExpanded = !isNotesExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) "الملاحظات والمرفقات" else "Notes & Attachments",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Icon(
                                imageVector = if (isNotesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isNotesExpanded) {
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text(if (isArabic) "الملاحظات" else "Notes") },
                                placeholder = { Text(if (isArabic) "أي تفاصيل إضافية أو ملاحظات..." else "Any additional details or notes...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp),
                                shape = RoundedCornerShape(14.dp),
                                maxLines = 3
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Fixed at Bottom
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (selectedAccount == null) return@Button
                        
                        val extraDetails = buildString {
                            when (selectedCategory) {
                                AssetType.REAL_ESTATE -> {
                                    if (propertyType.isNotBlank()) append(if (isArabic) "نوع العقار: $propertyType\n" else "Property Type: $propertyType\n")
                                    if (location.isNotBlank()) append(if (isArabic) "الموقع: $location\n" else "Location: $location\n")
                                    if (description.isNotBlank()) append(if (isArabic) "الوصف: $description\n" else "Description: $description\n")
                                }
                                AssetType.GOLD -> {
                                    if (goldType.isNotBlank()) append(if (isArabic) "نوع الذهب: $goldType\n" else "Gold Type: $goldType\n")
                                    if (goldWeight.isNotBlank()) append(if (isArabic) "الوزن: $goldWeight $weightUnit\n" else "Weight: $goldWeight $weightUnit\n")
                                    if (goldKarat.isNotBlank()) append(if (isArabic) "العيار: $goldKarat\n" else "Karat: $goldKarat\n")
                                }
                                AssetType.STOCKS, AssetType.INVESTMENTS -> {
                                    if (investmentType.isNotBlank()) append(if (isArabic) "نوع الاستثمار: $investmentType\n" else "Investment Type: $investmentType\n")
                                    if (entityCompany.isNotBlank()) append(if (isArabic) "الجهة/الشركة: $entityCompany\n" else "Company/Entity: $entityCompany\n")
                                    if (portfolioNumber.isNotBlank()) append(if (isArabic) "رقم الحساب/المحفظة: $portfolioNumber\n" else "Portfolio #: $portfolioNumber\n")
                                    if (unitsCount.isNotBlank()) append(if (isArabic) "عدد الوحدات: $unitsCount\n" else "Units/Shares: $unitsCount\n")
                                    if (unitPurchasePrice.isNotBlank()) append(if (isArabic) "سعر الوحدة: $unitPurchasePrice SAR\n" else "Price/Unit: $unitPurchasePrice SAR\n")
                                }
                                AssetType.VEHICLE -> {
                                    if (vehicleType.isNotBlank()) append(if (isArabic) "نوع المركبة: $vehicleType\n" else "Vehicle Type: $vehicleType\n")
                                    if (manufacturer.isNotBlank()) append(if (isArabic) "المصنع: $manufacturer\n" else "Manufacturer: $manufacturer\n")
                                    if (model.isNotBlank()) append(if (isArabic) "الموديل: $model\n" else "Model: $model\n")
                                    if (manufactureYear.isNotBlank()) append(if (isArabic) "سنة الصنع: $manufactureYear\n" else "Year: $manufactureYear\n")
                                    if (plateNumber.isNotBlank()) append(if (isArabic) "رقم اللوحة: $plateNumber\n" else "Plate #: $plateNumber\n")
                                    if (vinNumber.isNotBlank()) append(if (isArabic) "رقم الهيكل VIN: $vinNumber\n" else "VIN: $vinNumber\n")
                                }
                                AssetType.COLLECTIBLE -> {
                                    if (jewelryType.isNotBlank()) append(if (isArabic) "نوع المجوهرات: $jewelryType\n" else "Jewelry Type: $jewelryType\n")
                                    if (metalType.isNotBlank()) append(if (isArabic) "المعدن: $metalType\n" else "Metal: $metalType\n")
                                    if (jewelryWeight.isNotBlank()) append(if (isArabic) "الوزن: $jewelryWeight g\n" else "Weight: $jewelryWeight g\n")
                                    if (jewelryKarat.isNotBlank()) append(if (isArabic) "العيار: $jewelryKarat\n" else "Karat: $jewelryKarat\n")
                                    if (gemstonesDetails.isNotBlank()) append(if (isArabic) "الأحجار/التفاصيل: $gemstonesDetails\n" else "Gemstones: $gemstonesDetails\n")
                                }
                                AssetType.ELECTRONICS -> {
                                    if (equipmentType.isNotBlank()) append(if (isArabic) "نوع المعدة: $equipmentType\n" else "Equipment Type: $equipmentType\n")
                                    if (equipmentManufacturer.isNotBlank()) append(if (isArabic) "المصنع: $equipmentManufacturer\n" else "Manufacturer: $equipmentManufacturer\n")
                                    if (equipmentModel.isNotBlank()) append(if (isArabic) "الموديل: $equipmentModel\n" else "Model: $equipmentModel\n")
                                    if (serialNumber.isNotBlank()) append(if (isArabic) "الرقم التسلسلي: $serialNumber\n" else "S/N: $serialNumber\n")
                                }
                                else -> {
                                    if (description.isNotBlank()) append(if (isArabic) "الوصف: $description\n" else "Description: $description\n")
                                }
                            }
                            if (notes.isNotBlank()) append(if (isArabic) "ملاحظات: $notes\n" else "Notes: $notes\n")
                        }.trim()

                        val pVal = purchasePrice.toDoubleOrNull() ?: 0.0
                        val cVal = currentValue.toDoubleOrNull() ?: pVal

                        viewModel.addDetailedAsset(
                            name = name,
                            type = selectedCategory,
                            purchaseVal = pVal,
                            currentVal = cVal,
                            accountId = selectedAccount!!.id,
                            currency = "SAR",
                            purchaseDate = purchaseDate,
                            notes = extraDetails
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B46C1),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE2E8F0),
                        disabledContentColor = Color(0xFF94A3B8)
                    )
                ) {
                    Text(
                        text = if (isArabic) "إضافة الأصل (SAR)" else "Add Asset (SAR)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isArabic) "إلغاء" else "Cancel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    // Type Change Confirmation Guard Dialog
    if (showTypeChangeConfirmation && pendingCategoryToSwitch != null) {
        AlertDialog(
            onDismissRequest = {
                showTypeChangeConfirmation = false
                pendingCategoryToSwitch = null
            },
            title = {
                Text(
                    text = if (isArabic) "تغيير نوع الأصل" else "Change Asset Type",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isArabic)
                        "تغيير نوع الأصل سيؤدي إلى تغيير الحقول الخاصة بهذا الأصل. هل تريد المتابعة؟"
                    else
                        "Changing the asset type will modify the fields for this asset. Do you want to proceed?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedCategory = pendingCategoryToSwitch!!
                        resetSpecificFields()
                        showTypeChangeConfirmation = false
                        pendingCategoryToSwitch = null
                    }
                ) {
                    Text(
                        text = if (isArabic) "متابعة" else "Continue",
                        color = Color(0xFF6B46C1),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTypeChangeConfirmation = false
                        pendingCategoryToSwitch = null
                    }
                ) {
                    Text(
                        text = if (isArabic) "إلغاء" else "Cancel",
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Account Picker Modal Bottom Sheet (Filtered strictly to SAR Accounts)
    if (showAccountPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountPickerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isArabic) "اختر حساب الشراء بالريال السعودي" else "Select Purchase Account (SAR)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (sarAccounts.isEmpty()) {
                    Text(
                        text = if (isArabic) "لا تتوفر حسابات بالريال السعودي. يرجى إضافة حساب SAR أولاً." else "No SAR accounts available. Please add a SAR account first.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    sarAccounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAccount = account
                                    showAccountPickerSheet = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${String.format(Locale.US, "%,.2f", account.balance)} SAR", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (selectedAccount?.id == account.id) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF6B46C1))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private data class AssetCategoryInfo(
    val type: AssetType,
    val label: String,
    val icon: ImageVector
)

// ==========================================
// DYNAMIC FORMS PER ASSET CATEGORY
// ==========================================

@Composable
private fun RealEstateForm(
    isArabic: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    propertyType: String,
    onPropertyTypeChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    val propertyTypes = remember(isArabic) {
        if (isArabic) listOf("منزل", "فيلا", "شقة", "أرض", "عمارة", "مكتب", "محل تجاري", "مستودع", "مزرعة", "عقار استثماري", "أخرى")
        else listOf("House", "Villa", "Apartment", "Land", "Building", "Office", "Retail Store", "Warehouse", "Farm", "Investment Property", "Other")
    }

    Text(
        text = if (isArabic) "بيانات العقار" else "Real Estate Details",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    )

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(if (isArabic) "اسم العقار *" else "Property Name *") },
        placeholder = { Text(if (isArabic) "مثال: فيلا النرجس / أرض الملقا" else "e.g. Al Narjis Villa") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )

    DropdownSelector(
        label = if (isArabic) "نوع العقار *" else "Property Type *",
        selectedOption = propertyType,
        options = propertyTypes,
        onOptionSelected = onPropertyTypeChange
    )

    OutlinedTextField(
        value = location,
        onValueChange = onLocationChange,
        label = { Text(if (isArabic) "الموقع" else "Location") },
        placeholder = { Text(if (isArabic) "المدينة / الحي" else "City / District") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )

    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = { Text(if (isArabic) "الوصف" else "Description") },
        placeholder = { Text(if (isArabic) "المساحة، المميزات..." else "Area, features...") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}

@Composable
private fun GoldForm(
    isArabic: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    goldType: String,
    onGoldTypeChange: (String) -> Unit,
    goldWeight: String,
    onGoldWeightChange: (String) -> Unit,
    weightUnit: String,
    onWeightUnitChange: (String) -> Unit,
    goldKarat: String,
    onGoldKaratChange: (String) -> Unit
) {
    val goldTypes = remember(isArabic) {
        if (isArabic) listOf("سبائك", "عملات ذهبية", "ليرات", "مجوهرات ذهبية", "أخرى")
        else listOf("Bullion Bars", "Gold Coins", "Liras", "Gold Jewelry", "Other")
    }

    val karats = remember(isArabic) {
        if (isArabic) listOf("24 عيار", "22 عيار", "21 عيار", "18 عيار")
        else listOf("24K", "22K", "21K", "18K")
    }

    val units = remember(isArabic) {
        if (isArabic) listOf("جرام", "أونصة", "كيلو") else listOf("Grams", "Ounces", "Kilos")
    }

    Text(
        text = if (isArabic) "بيانات الذهب" else "Gold Details",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    )

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(if (isArabic) "اسم الأصل *" else "Item Name *") },
        placeholder = { Text(if (isArabic) "مثال: سبيكة ذهب 100 جرام" else "e.g. 100g Gold Bar") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )

    DropdownSelector(
        label = if (isArabic) "نوع الذهب *" else "Gold Type *",
        selectedOption = goldType,
        options = goldTypes,
        onOptionSelected = onGoldTypeChange
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = goldWeight,
            onValueChange = onGoldWeightChange,
            label = { Text(if (isArabic) "الوزن *" else "Weight *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        DropdownSelector(
            label = if (isArabic) "الوحدة" else "Unit",
            selectedOption = weightUnit,
            options = units,
            onOptionSelected = onWeightUnitChange,
            modifier = Modifier.width(120.dp)
        )
    }

    DropdownSelector(
        label = if (isArabic) "العيار *" else "Karat / Purity *",
        selectedOption = goldKarat,
        options = karats,
        onOptionSelected = onGoldKaratChange
    )
}

@Composable
private fun InvestmentForm(
    isArabic: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    investmentType: String,
    onInvestmentTypeChange: (String) -> Unit,
    entityCompany: String,
    onEntityCompanyChange: (String) -> Unit,
    portfolioNumber: String,
    onPortfolioNumberChange: (String) -> Unit,
    unitsCount: String,
    onUnitsCountChange: (String) -> Unit,
    unitPurchasePrice: String,
    onUnitPurchasePriceChange: (String) -> Unit
) {
    val investmentTypes = remember(isArabic) {
        if (isArabic) listOf("أسهم", "صناديق استثمارية", "صكوك", "سندات", "عملات رقمية", "رأس مال جريء", "أخرى")
        else listOf("Stocks", "Mutual Funds", "Sukuk", "Bonds", "Crypto", "Venture Capital", "Other")
    }

    Text(
        text = if (isArabic) "بيانات الاستثمار" else "Investment Details",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    )

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(if (isArabic) "اسم الاستثمار *" else "Investment Name *") },
        placeholder = { Text(if (isArabic) "مثال: أسهم أرامكو / صندوق الإنماء" else "e.g. Aramco Shares / Alinma Fund") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )

    DropdownSelector(
        label = if (isArabic) "نوع الاستثمار *" else "Investment Type *",
        selectedOption = investmentType,
        options = investmentTypes,
        onOptionSelected = onInvestmentTypeChange
    )

    OutlinedTextField(
        value = entityCompany,
        onValueChange = onEntityCompanyChange,
        label = { Text(if (isArabic) "الجهة / الشركة" else "Company / Entity") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )

    OutlinedTextField(
        value = portfolioNumber,
        onValueChange = onPortfolioNumberChange,
        label = { Text(if (isArabic) "رقم الحساب أو المحفظة" else "Account / Portfolio No.") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = unitsCount,
            onValueChange = onUnitsCountChange,
            label = { Text(if (isArabic) "عدد الوحدات/الأسهم" else "No. of Units/Shares") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = unitPurchasePrice,
            onValueChange = onUnitPurchasePriceChange,
            label = { Text(if (isArabic) "سعر الوحدة" else "Price / Unit") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )
    }
}

@Composable
private fun VehicleForm(
    isArabic: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    vehicleType: String,
    onVehicleTypeChange: (String) -> Unit,
    manufacturer: String,
    onManufacturerChange: (String) -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    manufactureYear: String,
    onManufactureYearChange: (String) -> Unit,
    plateNumber: String,
    onPlateNumberChange: (String) -> Unit,
    vinNumber: String,
    onVinNumberChange: (String) -> Unit
) {
    val vehicleTypes = remember(isArabic) {
        if (isArabic) listOf("سيارة سيدان", "SUV (دفع رباعي)", "شاحنة", "حافلة", "دراجة نارية", "معدة ثقيلة", "أخرى")
        else listOf("Sedan", "SUV", "Truck", "Bus", "Motorcycle", "Heavy Equipment", "Other")
    }

    Text(
        text = if (isArabic) "بيانات المركبة" else "Vehicle Details",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    )

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(if (isArabic) "اسم المركبة *" else "Vehicle Name *") },
        placeholder = { Text(if (isArabic) "مثال: تويوتا كامري 2024" else "e.g. Toyota Camry 2024") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )

    DropdownSelector(
        label = if (isArabic) "نوع المركبة *" else "Vehicle Type *",
        selectedOption = vehicleType,
        options = vehicleTypes,
        onOptionSelected = onVehicleTypeChange
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = manufacturer,
            onValueChange = onManufacturerChange,
            label = { Text(if (isArabic) "المصنع" else "Manufacturer") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            label = { Text(if (isArabic) "الموديل" else "Model") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = manufactureYear,
            onValueChange = onManufactureYearChange,
            label = { Text(if (isArabic) "سنة الصنع" else "Year") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = plateNumber,
            onValueChange = onPlateNumberChange,
            label = { Text(if (isArabic) "رقم اللوحة" else "Plate No.") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )
    }

    OutlinedTextField(
        value = vinNumber,
        onValueChange = onVinNumberChange,
        label = { Text(if (isArabic) "رقم الهيكل VIN" else "Chassis VIN") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}

@Composable
private fun JewelryForm(
    isArabic: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    jewelryType: String,
    onJewelryTypeChange: (String) -> Unit,
    metalType: String,
    onMetalTypeChange: (String) -> Unit,
    jewelryWeight: String,
    onJewelryWeightChange: (String) -> Unit,
    jewelryKarat: String,
    onJewelryKaratChange: (String) -> Unit,
    gemstonesDetails: String,
    onGemstonesDetailsChange: (String) -> Unit
) {
    val jewelryTypes = remember(isArabic) {
        if (isArabic) listOf("طقم", "خاتم", "قلادة", "سوار", "أقراط", "أخرى")
        else listOf("Set", "Ring", "Necklace", "Bracelet", "Earrings", "Other")
    }

    val metals = remember(isArabic) {
        if (isArabic) listOf("ذهب أبيض", "ذهب أصفر", "ألماس", "بلاتين", "فضة", "أخرى")
        else listOf("White Gold", "Yellow Gold", "Diamond", "Platinum", "Silver", "Other")
    }

    Text(
        text = if (isArabic) "بيانات المجوهرات" else "Jewelry Details",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    )

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(if (isArabic) "اسم القطعة *" else "Piece Name *") },
        placeholder = { Text(if (isArabic) "مثال: طقم ألماس ملكي" else "e.g. Diamond Set") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )

    DropdownSelector(
        label = if (isArabic) "نوع المجوهرات *" else "Jewelry Type *",
        selectedOption = jewelryType,
        options = jewelryTypes,
        onOptionSelected = onJewelryTypeChange
    )

    DropdownSelector(
        label = if (isArabic) "نوع المعدن" else "Metal Type",
        selectedOption = metalType,
        options = metals,
        onOptionSelected = onMetalTypeChange
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = jewelryWeight,
            onValueChange = onJewelryWeightChange,
            label = { Text(if (isArabic) "الوزن (جرام)" else "Weight (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = jewelryKarat,
            onValueChange = onJewelryKaratChange,
            label = { Text(if (isArabic) "العيار" else "Karat") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )
    }

    OutlinedTextField(
        value = gemstonesDetails,
        onValueChange = onGemstonesDetailsChange,
        label = { Text(if (isArabic) "الأحجار / التفاصيل" else "Gemstones / Details") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}

@Composable
private fun EquipmentForm(
    isArabic: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    equipmentType: String,
    onEquipmentTypeChange: (String) -> Unit,
    equipmentManufacturer: String,
    onEquipmentManufacturerChange: (String) -> Unit,
    equipmentModel: String,
    onEquipmentModelChange: (String) -> Unit,
    serialNumber: String,
    onSerialNumberChange: (String) -> Unit
) {
    val equipmentTypes = remember(isArabic) {
        if (isArabic) listOf("إلكترونيات", "أجهزة مكتبية", "آلات مصنع", "معدات طبية", "أجهزة تصوير", "أخرى")
        else listOf("Electronics", "Office Equipment", "Industrial Machinery", "Medical Devices", "Camera/Media", "Other")
    }

    Text(
        text = if (isArabic) "بيانات المعدات" else "Equipment Details",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    )

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(if (isArabic) "اسم المعدة *" else "Equipment Name *") },
        placeholder = { Text(if (isArabic) "مثال: جهاز تصوير سينمائي" else "e.g. Cinema Camera") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )

    DropdownSelector(
        label = if (isArabic) "نوع المعدة *" else "Equipment Type *",
        selectedOption = equipmentType,
        options = equipmentTypes,
        onOptionSelected = onEquipmentTypeChange
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = equipmentManufacturer,
            onValueChange = onEquipmentManufacturerChange,
            label = { Text(if (isArabic) "الشركة المصنعة" else "Manufacturer") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = equipmentModel,
            onValueChange = onEquipmentModelChange,
            label = { Text(if (isArabic) "الموديل" else "Model") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )
    }

    OutlinedTextField(
        value = serialNumber,
        onValueChange = onSerialNumberChange,
        label = { Text(if (isArabic) "الرقم التسلسلي S/N" else "Serial Number") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}

@Composable
private fun OtherAssetForm(
    isArabic: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Text(
        text = if (isArabic) "بيانات الأصل" else "Asset Details",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    )

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(if (isArabic) "اسم الأصل *" else "Asset Name *") },
        placeholder = { Text(if (isArabic) "مثال: لوحة فنية / ساعات نادرة" else "e.g. Rare Watch / Artwork") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )

    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = { Text(if (isArabic) "الوصف" else "Description") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}

// ==========================================
// CUSTOM DROPDOWN SELECTOR COMPOSABLE
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
