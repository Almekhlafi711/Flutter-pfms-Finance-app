package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.*

enum class DebtStatusFilter { ALL, ACTIVE, INACTIVE }
enum class DebtCategoryFilter { ALL, PERSONAL, CORPORATE }
enum class DebtTypeFilter { ALL, RECEIVABLE, PAYABLE }
enum class DebtDateRangeFilter { ALL, TODAY, LAST_7_DAYS, LAST_30_DAYS, THIS_MONTH, THIS_YEAR, CUSTOM }

data class DebtFilterState(
    val status: DebtStatusFilter = DebtStatusFilter.ALL,
    val category: DebtCategoryFilter = DebtCategoryFilter.ALL,
    val type: DebtTypeFilter = DebtTypeFilter.ALL,
    val dateRange: DebtDateRangeFilter = DebtDateRangeFilter.ALL,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null
) {
    val isFiltered: Boolean
        get() = status != DebtStatusFilter.ALL ||
                category != DebtCategoryFilter.ALL ||
                type != DebtTypeFilter.ALL ||
                dateRange != DebtDateRangeFilter.ALL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFilterBottomSheet(
    initialFilterState: DebtFilterState,
    isArabic: Boolean = false,
    onDismiss: () -> Unit,
    onApplyFilter: (DebtFilterState) -> Unit
) {
    var tempState by remember { mutableStateOf(initialFilterState) }
    var showDatePickerForStart by remember { mutableStateOf(false) }
    var showDatePickerForEnd by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = DesignTokens.RadiusLarge, topEnd = DesignTokens.RadiusLarge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "خيارات الفلترة" else "Filter Options",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { tempState = DebtFilterState() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "إعادة ضبط" else "Reset",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Status Filter (نشط / غير نشط / الكل)
                FilterSectionTitle(title = if (isArabic) "حالة الحساب" else "Account Status")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipOption(
                        selected = tempState.status == DebtStatusFilter.ALL,
                        label = if (isArabic) "الكل" else "All",
                        onClick = { tempState = tempState.copy(status = DebtStatusFilter.ALL) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChipOption(
                        selected = tempState.status == DebtStatusFilter.ACTIVE,
                        label = if (isArabic) "نشط" else "Active",
                        onClick = { tempState = tempState.copy(status = DebtStatusFilter.ACTIVE) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChipOption(
                        selected = tempState.status == DebtStatusFilter.INACTIVE,
                        label = if (isArabic) "غير نشط" else "Inactive",
                        onClick = { tempState = tempState.copy(status = DebtStatusFilter.INACTIVE) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 2. Category Filter (شخصي / مؤسسي / الكل)
                FilterSectionTitle(title = if (isArabic) "التصنيف" else "Category")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipOption(
                        selected = tempState.category == DebtCategoryFilter.ALL,
                        label = if (isArabic) "الكل" else "All",
                        onClick = { tempState = tempState.copy(category = DebtCategoryFilter.ALL) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChipOption(
                        selected = tempState.category == DebtCategoryFilter.PERSONAL,
                        label = if (isArabic) "شخصي" else "Personal",
                        onClick = { tempState = tempState.copy(category = DebtCategoryFilter.PERSONAL) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChipOption(
                        selected = tempState.category == DebtCategoryFilter.CORPORATE,
                        label = if (isArabic) "مؤسسي" else "Corporate",
                        onClick = { tempState = tempState.copy(category = DebtCategoryFilter.CORPORATE) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Debt Type Filter (له / عليه / الكل)
                FilterSectionTitle(title = if (isArabic) "نوع الحساب" else "Account Type")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipOption(
                        selected = tempState.type == DebtTypeFilter.ALL,
                        label = if (isArabic) "الكل" else "All",
                        onClick = { tempState = tempState.copy(type = DebtTypeFilter.ALL) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChipOption(
                        selected = tempState.type == DebtTypeFilter.RECEIVABLE,
                        label = if (isArabic) "له (مستحقات)" else "Receivable",
                        onClick = { tempState = tempState.copy(type = DebtTypeFilter.RECEIVABLE) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChipOption(
                        selected = tempState.type == DebtTypeFilter.PAYABLE,
                        label = if (isArabic) "عليه (التزامات)" else "Payable",
                        onClick = { tempState = tempState.copy(type = DebtTypeFilter.PAYABLE) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 4. Time Period Filter
                FilterSectionTitle(title = if (isArabic) "الفترة الزمنية" else "Time Period")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val periods = listOf(
                        DebtDateRangeFilter.ALL to if (isArabic) "الكل" else "All Time",
                        DebtDateRangeFilter.TODAY to if (isArabic) "اليوم" else "Today",
                        DebtDateRangeFilter.LAST_7_DAYS to if (isArabic) "آخر 7 أيام" else "Last 7 Days",
                        DebtDateRangeFilter.LAST_30_DAYS to if (isArabic) "آخر 30 يوماً" else "Last 30 Days",
                        DebtDateRangeFilter.THIS_MONTH to if (isArabic) "هذا الشهر" else "This Month",
                        DebtDateRangeFilter.THIS_YEAR to if (isArabic) "هذه السنة" else "This Year",
                        DebtDateRangeFilter.CUSTOM to if (isArabic) "فترة مخصصة" else "Custom Period"
                    )

                    // Display period options in rows of 3
                    periods.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (filterOption, label) ->
                                FilterChipOption(
                                    selected = tempState.dateRange == filterOption,
                                    label = label,
                                    onClick = { tempState = tempState.copy(dateRange = filterOption) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill remaining space if row has fewer than 3 items
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (tempState.dateRange == DebtDateRangeFilter.CUSTOM) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDatePickerForStart = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = tempState.customStartDate?.let { dateFormat.format(Date(it)) }
                                        ?: if (isArabic) "من تاريخ" else "From Date",
                                    fontSize = 12.sp
                                )
                            }

                            OutlinedButton(
                                onClick = { showDatePickerForEnd = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = tempState.customEndDate?.let { dateFormat.format(Date(it)) }
                                        ?: if (isArabic) "إلى تاريخ" else "To Date",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Apply Button
            Button(
                onClick = {
                    onApplyFilter(tempState)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(DesignTokens.RadiusMedium)
            ) {
                Text(
                    text = if (isArabic) "تطبيق الفلاتر" else "Apply Filters",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    if (showDatePickerForStart) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = tempState.customStartDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerForStart = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        tempState = tempState.copy(customStartDate = it)
                    }
                    showDatePickerForStart = false
                }) {
                    Text(if (isArabic) "موافق" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerForStart = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDatePickerForEnd) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = tempState.customEndDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerForEnd = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        tempState = tempState.copy(customEndDate = it)
                    }
                    showDatePickerForEnd = false
                }) {
                    Text(if (isArabic) "موافق" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerForEnd = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp
        )
    )
}

@Composable
private fun FilterChipOption(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(DesignTokens.RadiusSmall),
        modifier = modifier
    )
}
