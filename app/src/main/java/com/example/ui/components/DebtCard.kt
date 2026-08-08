package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.DesignTokens
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.DebtStatus
import com.example.domain.model.DebtType
import com.example.domain.model.PersonDebtAccount
import com.example.ui.theme.GreenIncome
import com.example.ui.theme.RedExpense
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DebtCard(
    account: PersonDebtAccount,
    onClick: () -> Unit,
    isArabic: Boolean = false,
    modifier: Modifier = Modifier
) {
    val debt = account.mainDebt
    val person = account.person
    val isReceivable = debt.type == DebtType.RECEIVABLE

    val accentColor = if (isReceivable) GreenIncome else RedExpense
    val statusText = when (debt.status) {
        DebtStatus.ACTIVE -> if (isArabic) "نشط" else "Active"
        DebtStatus.PARTIAL -> if (isArabic) "قيد السداد" else "In Progress"
        DebtStatus.COMPLETED -> if (isArabic) "مكتمل" else "Settled"
        DebtStatus.ARCHIVED -> if (isArabic) "مؤرشف" else "Archived"
    }

    val statusBg = when (debt.status) {
        DebtStatus.COMPLETED -> GreenIncome.copy(alpha = 0.15f)
        DebtStatus.PARTIAL -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val statusFg = when (debt.status) {
        DebtStatus.COMPLETED -> GreenIncome
        DebtStatus.PARTIAL -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", if (isArabic) Locale("ar") else Locale.getDefault())
    val formattedDate = dateFormat.format(Date(account.lastTransactionDate))

    // Initials for avatar
    val initials = person.name.trim().split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifEmpty { "P" }

    val avatarBrush = if (isReceivable) {
        Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFF66BB6A)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFB3261E), Color(0xFFE57373)))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(DesignTokens.RadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top Row: Avatar + Name & Phone + Status Badge & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(avatarBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = person.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusBg
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = statusFg
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val typeLabel = if (isReceivable) {
                            if (isArabic) "مستحقات (له)" else "Receivable"
                        } else {
                            if (isArabic) "التزامات (عليه)" else "Payable"
                        }
                        val entriesLabel = if (isArabic) "عملية" else "entries"
                        Text(
                            text = "$typeLabel • ${account.transactionCount} $entriesLabel",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount & Chevron Arrow
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyFormatter.format(account.totalRemainingAmount, debt.currency),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        )
                        Text(
                            text = "${if (isArabic) "الأصل: " else "Orig: "}${CurrencyFormatter.format(account.totalOriginalAmount, debt.currency)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) {
                            "نسبة السداد: ${(account.progressPercentage * 100).toInt()}%"
                        } else {
                            "Settlement Progress: ${(account.progressPercentage * 100).toInt()}%"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "${if (isArabic) "آخر عملية: " else "Last: "}$formattedDate",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { account.progressPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.15f)
                )
            }
        }
    }
}
