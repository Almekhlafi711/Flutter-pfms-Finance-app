import 'package:intl/intl.dart';

class CurrencyFormatter {
  static String format(double amount, [String currency = "SAR"]) {
    final formatter = NumberFormat("#,##0.00", "en_US");
    return "${formatter.format(amount)} $currency";
  }

  static String formatCompact(double amount, [String currency = "SAR"]) {
    if (amount >= 1000000) {
      final val = (amount / 1000000).toStringAsFixed(2);
      return "$val M $currency";
    } else if (amount >= 10000) {
      final val = (amount / 1000).toStringAsFixed(1);
      return "$val K $currency";
    } else {
      return format(amount, currency);
    }
  }
}
