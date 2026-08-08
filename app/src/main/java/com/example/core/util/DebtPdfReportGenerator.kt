package com.example.core.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.domain.model.DebtType
import com.example.domain.model.PersonDebtAccount
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object DebtPdfReportGenerator {

    fun generatePersonDebtStatementPdf(
        context: Context,
        account: PersonDebtAccount
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint()

        // 1. Header & Branding
        paint.color = Color.parseColor("#6750A4") // M3 Primary
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        titlePaint.color = Color.WHITE
        titlePaint.textSize = 20f
        titlePaint.isFakeBoldText = true
        canvas.drawText("FINANCIAL DEBT STATEMENT", 40f, 45f, titlePaint)

        paint.color = Color.parseColor("#EADDFF")
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Official Ledger Record & Account Breakdown", 40f, 68f, paint)

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
        paint.color = Color.WHITE
        paint.textSize = 10f
        canvas.drawText("Date: ${dateFormat.format(Date())}", 430f, 45f, paint)

        // 2. Person Information Box
        paint.color = Color.parseColor("#F3EDF7")
        canvas.drawRoundRect(40f, 110f, 555f, 190f, 12f, 12f, paint)

        paint.color = Color.parseColor("#1D1B20")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("DEBT ACCOUNT OWNER", 60f, 135f, paint)

        paint.textSize = 12f
        canvas.drawText("Name: ${account.person.name}", 60f, 158f, paint)
        paint.isFakeBoldText = false
        paint.textSize = 10f
        canvas.drawText("Phone: ${account.person.phone ?: "N/A"} | Category: ${account.person.category}", 60f, 175f, paint)

        // Account Type Badge inside box
        paint.color = if (account.mainDebt.type == DebtType.RECEIVABLE) Color.parseColor("#2E7D32") else Color.parseColor("#B3261E")
        canvas.drawRoundRect(420f, 130f, 535f, 160f, 8f, 8f, paint)
        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.isFakeBoldText = true
        val typeLabel = if (account.mainDebt.type == DebtType.RECEIVABLE) "RECEIVABLE" else "PAYABLE"
        canvas.drawText(typeLabel, 435f, 150f, paint)

        // 3. Financial Summary Grid
        val summaryY = 205f
        paint.color = Color.parseColor("#E7E0EC")
        canvas.drawRoundRect(40f, summaryY, 200f, summaryY + 55f, 8f, 8f, paint)
        canvas.drawRoundRect(215f, summaryY, 375f, summaryY + 55f, 8f, 8f, paint)
        canvas.drawRoundRect(390f, summaryY, 555f, summaryY + 55f, 8f, 8f, paint)

        paint.color = Color.parseColor("#49454F")
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("ORIGINAL AMOUNT", 55f, summaryY + 20f, paint)
        canvas.drawText("PAID AMOUNT", 230f, summaryY + 20f, paint)
        canvas.drawText("REMAINING BALANCE", 405f, summaryY + 20f, paint)

        paint.textSize = 13f
        paint.isFakeBoldText = true
        paint.color = Color.parseColor("#1D1B20")
        canvas.drawText(CurrencyFormatter.format(account.totalOriginalAmount, account.mainDebt.currency), 55f, summaryY + 42f, paint)
        paint.color = Color.parseColor("#2E7D32")
        canvas.drawText(CurrencyFormatter.format(account.totalPaidAmount, account.mainDebt.currency), 230f, summaryY + 42f, paint)
        paint.color = if (account.totalRemainingAmount > 0) Color.parseColor("#B3261E") else Color.parseColor("#2E7D32")
        canvas.drawText(CurrencyFormatter.format(account.totalRemainingAmount, account.mainDebt.currency), 405f, summaryY + 42f, paint)

        // 4. Ledger Table Header
        var y = 285f
        paint.color = Color.parseColor("#6750A4")
        canvas.drawRect(40f, y, 555f, y + 25f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("DATE", 50f, y + 17f, paint)
        canvas.drawText("DESCRIPTION & METHOD", 140f, y + 17f, paint)
        canvas.drawText("TYPE", 330f, y + 17f, paint)
        canvas.drawText("AMOUNT", 410f, y + 17f, paint)
        canvas.drawText("RUNNING BAL", 485f, y + 17f, paint)

        // 5. Ledger Entries
        y += 35f
        paint.isFakeBoldText = false
        val rowDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        var runningBal = account.totalOriginalAmount

        if (account.entries.isEmpty()) {
            // Display initial principal debt entry
            paint.color = Color.parseColor("#1D1B20")
            canvas.drawText(rowDateFormat.format(Date(account.mainDebt.createdAt)), 50f, y, paint)
            canvas.drawText(account.mainDebt.notes.ifEmpty { "Initial Debt Principal" }.take(25), 140f, y, paint)
            canvas.drawText(account.mainDebt.type.name, 330f, y, paint)
            paint.color = if (account.mainDebt.type == DebtType.RECEIVABLE) Color.parseColor("#2E7D32") else Color.parseColor("#B3261E")
            canvas.drawText(CurrencyFormatter.format(account.mainDebt.originalAmount, account.mainDebt.currency), 410f, y, paint)
            paint.color = Color.parseColor("#1D1B20")
            canvas.drawText(CurrencyFormatter.format(account.mainDebt.remainingAmount, account.mainDebt.currency), 485f, y, paint)
            y += 24f
        } else {
            account.entries.forEach { entry ->
                if (entry.isPayment) {
                    runningBal = (runningBal - entry.amount).coerceAtLeast(0.0)
                }
                paint.color = Color.parseColor("#1D1B20")
                canvas.drawText(rowDateFormat.format(Date(entry.date)), 50f, y, paint)
                val desc = "${entry.description.ifEmpty { entry.category }} (${entry.paymentMethod})"
                canvas.drawText(desc.take(24), 140f, y, paint)
                val typeTxt = if (entry.isPayment) "PAYMENT" else entry.type.name
                canvas.drawText(typeTxt, 330f, y, paint)

                paint.color = if (entry.isPayment) Color.parseColor("#2E7D32") else Color.parseColor("#B3261E")
                val sign = if (entry.isPayment) "-" else "+"
                canvas.drawText("$sign${CurrencyFormatter.format(entry.amount, account.mainDebt.currency)}", 410f, y, paint)

                paint.color = Color.parseColor("#1D1B20")
                canvas.drawText(CurrencyFormatter.format(runningBal, account.mainDebt.currency), 485f, y, paint)

                y += 22f
                if (y > 700f) return@forEach
            }
        }

        // 6. Signatures Area & Terms
        val sigY = 730f
        paint.color = Color.parseColor("#CAC4D0")
        canvas.drawLine(50f, sigY, 220f, sigY, paint)
        canvas.drawLine(375f, sigY, 545f, sigY, paint)

        paint.color = Color.parseColor("#49454F")
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText("ACCOUNT HOLDER SIGNATURE", 60f, sigY + 15f, paint)
        canvas.drawText("AUTHORIZED STAMP / SIGN", 385f, sigY + 15f, paint)

        // Footer
        paint.color = Color.GRAY
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText("Generated by Premium FinTech Debt Center | Confidential Record | Page 1 of 1", 40f, 820f, paint)

        pdfDocument.finishPage(page)

        return try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Debt_Statement_${account.person.name.replace(" ", "_")}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}
