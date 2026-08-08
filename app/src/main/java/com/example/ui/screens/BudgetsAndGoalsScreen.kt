package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.theme.DesignTokens
import com.example.core.util.CurrencyFormatter
import com.example.domain.model.Goal
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GreenIncome
import com.example.ui.theme.RedExpense
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.PfmsViewModel
import com.example.ui.viewmodel.QuickActionSheetType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsAndGoalsScreen(
    viewModel: PfmsViewModel,
    onNavigateBack: () -> Unit
) {
    val budgets by viewModel.budgets.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Budgets, 1 = Goals
    var goalToDeposit by remember { mutableStateOf<Goal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "الميزانيات والأهداف" else "Budgets & Goals",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (selectedTab == 0) viewModel.openBottomSheet(QuickActionSheetType.BUDGET)
                        else viewModel.openBottomSheet(QuickActionSheetType.GOAL)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (isArabic) "إضافة" else "Add Item"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Budgets", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Savings Goals", fontWeight = FontWeight.Bold) })
            }

            if (selectedTab == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(budgets) { b ->
                        val usage = b.usagePercentage
                        val progressColor = when {
                            usage >= 1.0f -> RedExpense
                            usage >= 0.8f -> GoldAccent
                            else -> GreenIncome
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(DesignTokens.RadiusMedium),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(b.category, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        "${CurrencyFormatter.format(b.spentAmount, b.currency)} / ${CurrencyFormatter.format(b.monthlyLimit, b.currency)}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = progressColor)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { usage.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = progressColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (usage >= 1.0f) "⚠️ Budget Exceeded!" else if (usage >= 0.8f) "⚡ 80% Limit Warning" else "On Track",
                                    style = MaterialTheme.typography.labelSmall.copy(color = progressColor)
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(goals) { g ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(DesignTokens.RadiusMedium),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(g.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("${(g.progressPercentage * 100).toInt()}%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TealAccent))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "${CurrencyFormatter.format(g.currentAmount, g.currency)} saved of ${CurrencyFormatter.format(g.targetAmount, g.currency)}",
                                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { g.progressPercentage },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = TealAccent
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { goalToDeposit = g },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Contribute Funds")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    goalToDeposit?.let { goal ->
        GoalDepositDialog(
            goal = goal,
            accounts = accounts,
            onDismiss = { goalToDeposit = null },
            onConfirm = { amt, accId ->
                viewModel.contributeToGoal(goal, amt, accId)
                goalToDeposit = null
            }
        )
    }
}

@Composable
fun GoalDepositDialog(
    goal: Goal,
    accounts: List<com.example.domain.model.Account>,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contribute to ${goal.title}") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Deposit Amount (SAR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                if (amt > 0 && accountId.isNotEmpty()) {
                    onConfirm(amt, accountId)
                }
            }) { Text("Deposit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
