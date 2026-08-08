package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.theme.DesignTokens
import com.example.core.util.CurrencyFormatter
import com.example.ui.theme.GreenIncome
import com.example.ui.theme.RedExpense
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.PfmsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsAndReportsScreen(
    viewModel: PfmsViewModel,
    onNavigateBack: () -> Unit
) {
    val netWorthSummary by viewModel.netWorthSummary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    val totalIncome = transactions.filter { it.type == com.example.domain.model.TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == com.example.domain.model.TransactionType.EXPENSE }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "التقارير والتحليلات المالية" else "Analytics & Financial Reports",
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
                    IconButton(onClick = { viewModel.exportAccountStatementPdf() }) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = if (isArabic) "تصدير PDF" else "Export PDF"
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Statement Export Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(DesignTokens.RadiusMedium),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export Financial Statement", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Download official PDF report of all accounts and net worth calculations.", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = { viewModel.exportAccountStatementPdf() }) {
                        Text("Export PDF")
                    }
                }
            }

            // Cash Flow Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(DesignTokens.RadiusMedium),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("CASH FLOW BREAKDOWN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Income", style = MaterialTheme.typography.labelSmall)
                            Text(CurrencyFormatter.format(totalIncome, netWorthSummary.currency), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = GreenIncome))
                        }
                        Column {
                            Text("Total Expenses", style = MaterialTheme.typography.labelSmall)
                            Text(CurrencyFormatter.format(totalExpense, netWorthSummary.currency), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = RedExpense))
                        }
                    }
                }
            }

            // Net Worth Chart
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(DesignTokens.RadiusMedium),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("NET WORTH GROWTH TREND", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Spacer(modifier = Modifier.height(16.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val points = listOf(
                            Offset(0f, size.height * 0.8f),
                            Offset(size.width * 0.25f, size.height * 0.65f),
                            Offset(size.width * 0.5f, size.height * 0.5f),
                            Offset(size.width * 0.75f, size.height * 0.35f),
                            Offset(size.width, size.height * 0.15f)
                        )

                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = TealAccent,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
