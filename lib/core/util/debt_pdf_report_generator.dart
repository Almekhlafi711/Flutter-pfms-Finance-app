import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';
import '../../domain/models/debt_models.dart';
import 'currency_formatter.dart';

class DebtPdfReportGenerator {
  static Future<void> exportDebtStatementPdf({
    required PersonDebtAccount debtAccount,
    required bool isArabic,
  }) async {
    final pdf = pw.Document();

    pdf.addPage(
      pw.Page(
        pageFormat: PdfPageFormat.a4,
        build: (pw.Context context) {
          final person = debtAccount.person;
          return pw.Column(
            crossAxisAlignment: pw.CrossAxisAlignment.start,
            children: [
              pw.Header(
                level: 0,
                child: pw.Row(
                  mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                  children: [
                    pw.Text(
                      isArabic ? "كشف حساب الدين والتصفية" : "Debt & Settlement Statement",
                      style: pw.TextStyle(fontSize: 20, fontWeight: pw.FontWeight.bold),
                    ),
                    pw.Text(
                      person.name,
                      style: pw.TextStyle(fontSize: 18, color: PdfColors.orange700),
                    ),
                  ],
                ),
              ),
              pw.SizedBox(height: 10),
              pw.Text("${isArabic ? "الجهة/الطرف" : "Party Name"}: ${person.name} (${person.phone ?? 'N/A'})"),
              pw.Text("${isArabic ? "المبلغ الأصلي" : "Original Debt"}: ${CurrencyFormatter.format(debtAccount.totalOriginalAmount, debtAccount.currency)}"),
              pw.Text("${isArabic ? "المبلغ المتبقي" : "Remaining Debt"}: ${CurrencyFormatter.format(debtAccount.totalRemainingAmount, debtAccount.currency)}"),
              pw.SizedBox(height: 20),
              pw.TableHelper.fromTextArray(
                headers: isArabic ? ["التاريخ", "نوع العملية", "المبلغ", "الوسيلة", "الوصف"] : ["Date", "Type", "Amount", "Method", "Description"],
                data: debtAccount.entries.map((entry) {
                  final dateStr = DateTime.fromMillisecondsSinceEpoch(entry.date).toString().split(" ")[0];
                  final typeStr = entry.isPayment ? (isArabic ? "سداد/استلام" : "Payment") : (isArabic ? "نشأة دين" : "Principal");
                  return [
                    dateStr,
                    typeStr,
                    CurrencyFormatter.format(entry.amount, debtAccount.currency),
                    entry.paymentMethod,
                    entry.description,
                  ];
                }).toList(),
              ),
            ],
          );
        },
      ),
    );

    await Printing.layoutPdf(
      onLayout: (PdfPageFormat format) async => pdf.save(),
    );
  }
}
