package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.DesignTokens
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.AccountType
import com.example.domain.model.GroupedAccount

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountCardCarousel(
    groupedAccounts: List<GroupedAccount>,
    isArabic: Boolean = false,
    onNavigateToAccounts: () -> Unit,
    onOpenDeposit: () -> Unit
) {
    val activeGroups = groupedAccounts.filter { !it.isArchived }
    if (activeGroups.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { activeGroups.size })

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "الحسابات المالية" else "FINANCIAL ACCOUNTS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            TextButton(onClick = onNavigateToAccounts) {
                Text(
                    text = if (isArabic) "عرض الكل" else "Show All",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val group = activeGroups[page]
            AccountCardItem(
                group = group,
                isArabic = isArabic,
                onClick = onNavigateToAccounts
            )
        }

        // Carousel Indicator Dots (for Institutions)
        if (activeGroups.size > 1) {
            Row(
                Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(activeGroups.size) { iteration ->
                    val color by animateColorAsState(
                        targetValue = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        label = "indicatorColor"
                    )
                    Box(
                        modifier = Modifier
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(if (pagerState.currentPage == iteration) 10.dp else 6.dp)
                    )
                }
            }
        }

        // Deposit Pill Button (below cards and dots indicator, aligned to left)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onOpenDeposit,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.height(40.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isArabic) "إيداع" else "Deposit",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountCardItem(
    group: GroupedAccount,
    isArabic: Boolean = false,
    onClick: () -> Unit
) {
    val gradientBrush = when (group.type) {
        AccountType.BANK -> Brush.linearGradient(listOf(Color(0xFF6750A4), Color(0xFF4F378B)))
        AccountType.CASH -> Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)))
        AccountType.SAVINGS -> Brush.linearGradient(listOf(Color(0xFF7D5260), Color(0xFF492532)))
        AccountType.CRYPTO -> Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFF92400E)))
        AccountType.CREDIT_CARD -> Brush.linearGradient(listOf(Color(0xFFB3261E), Color(0xFF8C1D18)))
        AccountType.INVESTMENT -> Brush.linearGradient(listOf(Color(0xFF625B71), Color(0xFF3B3549)))
        AccountType.WALLET -> Brush.linearGradient(listOf(Color(0xFF4F378B), Color(0xFF31111D)))
    }

    val typeIcon = when (group.type) {
        AccountType.BANK -> Icons.Default.AccountBalance
        AccountType.CASH -> Icons.Default.Payments
        AccountType.SAVINGS -> Icons.Default.Savings
        AccountType.CRYPTO -> Icons.Default.CurrencyBitcoin
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
        AccountType.INVESTMENT -> Icons.Default.ShowChart
        AccountType.WALLET -> Icons.Default.AccountBalanceWallet
    }

    val innerPagerState = rememberPagerState(pageCount = { group.accounts.size })

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(DesignTokens.RadiusLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Card Top Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = group.name,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = if (isArabic) getAccountTypeNameAr(group.type) else group.type.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }

                    val currentAccount = group.accounts.getOrNull(innerPagerState.currentPage) ?: group.accounts.first()
                    Surface(
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = currentAccount.currency,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Balance Display & Inner Pager
                if (group.accounts.size == 1) {
                    val acc = group.accounts.first()
                    Column {
                        Text(
                            text = if (isArabic) "الرصيد الحالي" else "Current Balance",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        )
                        Text(
                            text = CurrencyFormatter.format(acc.balance, acc.currency),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        HorizontalPager(
                            state = innerPagerState,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            val acc = group.accounts[page]
                            Column {
                                Text(
                                    text = if (isArabic) "الرصيد الحالي (${acc.currency})" else "Current Balance (${acc.currency})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                )
                                Text(
                                    text = CurrencyFormatter.format(acc.balance, acc.currency),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        letterSpacing = (-0.5).sp
                                    )
                                )
                            }
                        }

                        // Inner Dots Indicator
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(group.accounts.size) { iteration ->
                                val color by animateColorAsState(
                                    targetValue = if (innerPagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.4f),
                                    label = "innerDotColor"
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(if (innerPagerState.currentPage == iteration) 6.dp else 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getAccountTypeNameAr(type: AccountType): String {
    return when (type) {
        AccountType.BANK -> "حساب بنكي"
        AccountType.WALLET -> "محفظة"
        AccountType.CASH -> "نقد"
        AccountType.SAVINGS -> "توفير"
        AccountType.CREDIT_CARD -> "بطاقة ائتمانية"
        AccountType.INVESTMENT -> "استثمار"
        AccountType.CRYPTO -> "عملات رقمية"
    }
}
