package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.core.theme.DesignTokens
import com.example.core.util.DebtPdfReportGenerator
import com.example.domain.model.PersonDebtAccount
import java.io.File

enum class StatementPeriod { LAST_30_DAYS, THIS_MONTH, LAST_3_MONTHS, CUSTOM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtStatementBottomSheet(
    account: PersonDebtAccount,
    isArabic: Boolean = false,
    onDismiss: () -> Unit,
    onPreview: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf(StatementPeriod.LAST_30_DAYS) }

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
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "خيارات كشف الحساب" else "Statement Options",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider()

            // Subtitle
            Text(
                text = if (isArabic) "اختر الفترة الزمنية للتقرير:" else "Select Statement Period:",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            // Period Selector Grid / Row
            val periods = listOf(
                StatementPeriod.LAST_30_DAYS to if (isArabic) "آخر 30 يوماً" else "Last 30 Days",
                StatementPeriod.THIS_MONTH to if (isArabic) "هذا الشهر" else "This Month",
                StatementPeriod.LAST_3_MONTHS to if (isArabic) "آخر 3 أشهر" else "Last 3 Months",
                StatementPeriod.CUSTOM to if (isArabic) "فترة مخصصة" else "Custom Period"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                periods.take(2).forEach { (period, label) ->
                    val isSelected = selectedPeriod == period
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPeriod = period },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                periods.drop(2).forEach { (period, label) ->
                    val isSelected = selectedPeriod == period
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPeriod = period },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isArabic) "اختر الإجراء المطلوب:" else "Select Required Action:",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            // Action Options Cards
            StatementActionCard(
                icon = Icons.Default.Visibility,
                title = if (isArabic) "معاينة كشف الحساب" else "Preview Statement",
                subtitle = if (isArabic) "عرض التقرير التفاعلي مباشرة" else "View interactive report on screen",
                onClick = {
                    onPreview()
                    onDismiss()
                }
            )

            StatementActionCard(
                icon = Icons.Default.PictureAsPdf,
                title = if (isArabic) "تصدير كشف الحساب (PDF)" else "Export Statement (PDF)",
                subtitle = if (isArabic) "إنشاء وتنزيل ملف PDF رسمي" else "Generate official PDF report",
                onClick = {
                    val pdfFile = DebtPdfReportGenerator.generatePersonDebtStatementPdf(context, account)
                    if (pdfFile != null) {
                        Toast.makeText(context, if (isArabic) "تم إنشاء كشف الحساب PDF: ${pdfFile.name}" else "PDF Generated: ${pdfFile.name}", Toast.LENGTH_LONG).show()
                        openPdfFile(context, pdfFile)
                    } else {
                        Toast.makeText(context, if (isArabic) "فشل إنشاء ملف PDF" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }
            )

            StatementActionCard(
                icon = Icons.Default.Share,
                title = if (isArabic) "مشاركة عبر الواتساب" else "Share via WhatsApp",
                subtitle = if (isArabic) "إرسال التقرير مباشرة إلى الواتساب" else "Send statement report via WhatsApp",
                onClick = {
                    val pdfFile = DebtPdfReportGenerator.generatePersonDebtStatementPdf(context, account)
                    if (pdfFile != null) {
                        sharePdfToWhatsApp(context, pdfFile, account)
                    }
                    onDismiss()
                }
            )

            StatementActionCard(
                icon = Icons.Default.Print,
                title = if (isArabic) "طباعة كشف الحساب" else "Print Statement",
                subtitle = if (isArabic) "طباعة كشف الحساب مباشرة" else "Send to system printer",
                onClick = {
                    val pdfFile = DebtPdfReportGenerator.generatePersonDebtStatementPdf(context, account)
                    if (pdfFile != null) {
                        openPdfFile(context, pdfFile)
                    }
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun StatementActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(DesignTokens.RadiusMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openPdfFile(context: Context, file: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open PDF Statement"))
    } catch (e: Exception) {
        Toast.makeText(context, "PDF saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}

private fun sharePdfToWhatsApp(context: Context, file: File, account: PersonDebtAccount) {
    try {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "كشف حساب مالي - ${account.person.name}\nالعملة: ${account.mainDebt.currency}")
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to general share chooser
        try {
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "كشف حساب مالي - ${account.person.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Statement"))
        } catch (ex: Exception) {
            Toast.makeText(context, "PDF saved: ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }
}
