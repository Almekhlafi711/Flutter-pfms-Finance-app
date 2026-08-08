package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.DesignTokens
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.DebtLedgerEntry
import com.example.domain.model.DebtType
import com.example.ui.theme.GreenIncome
import com.example.ui.theme.RedExpense
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DebtTimeline(
    entries: List<DebtLedgerEntry>,
    originalAmount: Double,
    currency: String = "SAR",
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(DesignTokens.RadiusLarge),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recorded transactions in ledger yet.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
        return
    }

    val sortedEntries = entries.sortedByDescending { it.date }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    // Calculate running balances in ascending order, then reverse
    var currentBalance = originalAmount
    val entriesWithRunningBalance = entries.sortedBy { it.date }.map { entry ->
        if (entry.isPayment) {
            currentBalance = (currentBalance - entry.amount).coerceAtLeast(0.0)
        }
        entry to currentBalance
    }.reversed()

    Column(modifier = modifier.fillMaxWidth()) {
        entriesWithRunningBalance.forEachIndexed { index, (entry, runningBal) ->
            TimelineItem(
                entry = entry,
                runningBalance = runningBal,
                currency = currency,
                dateFormat = dateFormat,
                isLast = index == entriesWithRunningBalance.size - 1
            )
        }
    }
}

@Composable
private fun TimelineItem(
    entry: DebtLedgerEntry,
    runningBalance: Double,
    currency: String,
    dateFormat: SimpleDateFormat,
    isLast: Boolean
) {
    val isPositive = entry.isPayment || entry.type == DebtType.RECEIVABLE
    val iconBg = if (entry.isPayment) GreenIncome.copy(alpha = 0.15f)
    else if (entry.type == DebtType.RECEIVABLE) GreenIncome.copy(alpha = 0.15f)
    else RedExpense.copy(alpha = 0.15f)

    val iconTint = if (entry.isPayment) GreenIncome
    else if (entry.type == DebtType.RECEIVABLE) GreenIncome
    else RedExpense

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: Timeline Line & Node Icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (entry.isPayment) Icons.Default.Check
                    else if (entry.type == DebtType.RECEIVABLE) Icons.Default.SouthWest
                    else Icons.Default.NorthEast,
                    contentDescription = entry.description,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(54.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Content: Card Item
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(DesignTokens.RadiusMedium),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (entry.isPayment) "Payment Received/Made" else entry.description.ifEmpty { entry.category },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = dateFormat.format(Date(entry.date)),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${if (entry.isPayment) "-" else "+"}${CurrencyFormatter.format(entry.amount, currency)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = iconTint
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Bal: ${CurrencyFormatter.format(runningBalance, currency)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Method: ${entry.paymentMethod}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = GreenIncome.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = entry.status,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GreenIncome,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
