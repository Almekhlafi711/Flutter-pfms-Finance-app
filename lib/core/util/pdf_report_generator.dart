import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';
import '../../domain/models/domain_models.dart';
import 'currency_formatter.dart';

class PdfReportGenerator {
  static Future<void> exportAccountStatementPdf({
    required Account account,
    required List<Transaction> transactions,
    required bool isArabic,
  }) async {
    final pdf = pw.Document();

    pdf.addPage(
      pw.Page(
        pageFormat: PdfPageFormat.a4,
        build: (pw.Context context) {
          return pw.Column(
            cross: pw.CrossAxisAlignment.start,
            children: [
              pw.Header(
                level: 0,
                child: pw.Row(
                  main: pw.MainAxisAlignment.spaceBetween,
                  children: [
                    pw.Text(
                      isArabic ? "كشف حساب مالـي" : "Financial Account Statement",
                      style: pw.TextStyle(fontSize: 22, fontWeight: pw.FontWeight.bold),
                    ),
                    pw.Text(
                      account.name,
                      style: pw.TextStyle(fontSize: 18, color: PdfColors.blue700),
                    ),
                  ],
                ),
              ),
              pw.SizedBox(height: 10),
              pw.Text("${isArabic ? "نوع الحساب" : "Account Type"}: ${account.type.name}"),
              pw.Text("${isArabic ? "الرصيد الحالي" : "Current Balance"}: ${CurrencyFormatter.format(account.balance, account.currency)}"),
              pw.SizedBox(height: 20),
              pw.Table.fromTextArray(
                headers: isArabic ? ["التاريخ", "النوع", "المبلغ", "التصنيف", "ملاحظات"] : ["Date", "Type", "Amount", "Category", "Note"],
                data: transactions.map((tx) {
                  final dateStr = DateTime.fromMillisecondsSinceEpoch(tx.date).toString().split(" ")[0];
                  return [
                    dateStr,
                    tx.type.name,
                    CurrencyFormatter.format(tx.amount, tx.currency),
                    tx.category,
                    tx.note,
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
