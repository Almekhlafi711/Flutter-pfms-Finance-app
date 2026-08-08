package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.DesignTokens
import com.example.ui.theme.*
import com.example.ui.viewmodel.QuickActionSheetType

@Composable
fun QuickActionGrid(
    isArabic: Boolean = false,
    onActionSelected: (QuickActionSheetType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = if (isArabic) "الإجراءات المالية السريعة" else "FINANCIAL QUICK ACTIONS",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionButton(
                title = if (isArabic) "دخل" else "Income",
                icon = Icons.Default.ArrowDownward,
                color = GreenIncome,
                onClick = { onActionSelected(QuickActionSheetType.INCOME) }
            )
            QuickActionButton(
                title = if (isArabic) "مصروف" else "Expense",
                icon = Icons.Default.ArrowUpward,
                color = RedExpense,
                onClick = { onActionSelected(QuickActionSheetType.EXPENSE) }
            )
            QuickActionButton(
                title = if (isArabic) "تحويل" else "Transfer",
                icon = Icons.Default.SwapHoriz,
                color = BlueTransfer,
                onClick = { onActionSelected(QuickActionSheetType.TRANSFER) }
            )
            QuickActionButton(
                title = if (isArabic) "أصل" else "Asset",
                icon = Icons.Default.HomeWork,
                color = PurpleAsset,
                onClick = { onActionSelected(QuickActionSheetType.ASSET) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionButton(
                title = if (isArabic) "دين" else "Debt",
                icon = Icons.Default.Handshake,
                color = OrangeDebt,
                onClick = { onActionSelected(QuickActionSheetType.DEBT) }
            )
            QuickActionButton(
                title = if (isArabic) "هدف" else "Goal",
                icon = Icons.Default.Flag,
                color = TealAccent,
                onClick = { onActionSelected(QuickActionSheetType.GOAL) }
            )
            QuickActionButton(
                title = if (isArabic) "فاتورة" else "Bill",
                icon = Icons.Default.ReceiptLong,
                color = GoldAccent,
                onClick = { onActionSelected(QuickActionSheetType.BILL) }
            )
            QuickActionButton(
                title = if (isArabic) "كشف حساب" else "Statement",
                icon = Icons.Default.PictureAsPdf,
                color = MaterialTheme.colorScheme.primary,
                onClick = { onActionSelected(QuickActionSheetType.REPORT) }
            )
        }
    }
}

@Composable
fun RowScope.QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(DesignTokens.RadiusMedium))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
