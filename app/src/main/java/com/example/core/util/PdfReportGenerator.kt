package com.example.core.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.domain.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    fun generateAccountStatementPdf(
        context: Context,
        account: Account,
        transactions: List<Transaction>,
        netWorth: NetWorthSummary
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint()

        // Header Style
        titlePaint.color = Color.parseColor("#0F172A")
        titlePaint.textSize = 22f
        titlePaint.isFakeBoldText = true

        canvas.drawText("PERSONAL FINANCE MANAGEMENT SYSTEM", 40f, 50f, titlePaint)

        paint.color = Color.parseColor("#0EA5E9")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("ACCOUNT STATEMENT - ${account.name.uppercase(Locale.ROOT)}", 40f, 80f, paint)

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
        paint.color = Color.GRAY
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Generated on: ${dateFormat.format(Date())}", 40f, 100f, paint)

        // Account Summary Card
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(40f, 120f, 555f, 190f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("Current Balance: ${CurrencyFormatter.format(account.balance, account.currency)}", 60f, 150f, paint)
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Account Type: ${account.type} | Number: ${account.accountNumber.ifEmpty { "N/A" }}", 60f, 170f, paint)

        // Transactions Table Header
        var y = 220f
        paint.color = Color.parseColor("#1E293B")
        canvas.drawRect(40f, y, 555f, y + 25f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("DATE", 50f, y + 17f, paint)
        canvas.drawText("CATEGORY & PARTY", 140f, y + 17f, paint)
        canvas.drawText("TYPE", 340f, y + 17f, paint)
        canvas.drawText("AMOUNT", 450f, y + 17f, paint)

        y += 35f
        paint.isFakeBoldText = false
        val rowDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

        transactions.take(20).forEach { tx ->
            paint.color = Color.parseColor("#334155")
            canvas.drawText(rowDateFormat.format(Date(tx.date)), 50f, y, paint)
            val desc = "${tx.category} ${if (!tx.party.isNull_or_empty()) "(${tx.party})" else ""}"
            canvas.drawText(desc.take(28), 140f, y, paint)
            canvas.drawText(tx.type.name, 340f, y, paint)

            if (tx.type == TransactionType.INCOME) {
                paint.color = Color.parseColor("#10B981")
            } else {
                paint.color = Color.parseColor("#EF4444")
            }
            canvas.drawText(CurrencyFormatter.format(tx.amount, tx.currency), 450f, y, paint)

            y += 22f
            if (y > 780f) return@forEach
        }

        // Footer
        paint.color = Color.GRAY
        paint.textSize = 9f
        canvas.drawText("Official Confidential Financial Record | Page 1 of 1", 40f, 820f, paint)

        pdfDocument.finishPage(page)

        return try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Account_Statement_${account.id}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.trim().isEmpty()
}
