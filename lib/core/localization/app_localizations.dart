class AppLocalizations {
  static String getString(String key, {bool isArabic = false}) {
    const enMap = {
      "app_name": "PFMS Finance",
      "net_worth": "Net Worth",
      "accounts": "Accounts",
      "transactions": "Transactions",
      "assets": "Assets",
      "debts": "Debt Center",
      "budgets": "Budgets",
      "goals": "Goals",
      "bills": "Bills & Subscriptions",
      "analytics": "Analytics & Reports",
      "quick_actions": "Quick Actions",
      "add_income": "Add Income",
      "add_expense": "Add Expense",
      "transfer": "Transfer",
      "new_asset": "New Asset",
      "new_debt": "New Debt",
      "new_goal": "New Goal",
      "pay_bill": "Pay Bill",
      "reports": "Reports",
      "recent_activities": "Recent Activities",
      "receivables": "Receivables (Owed to You)",
      "payables": "Payables (You Owe)",
      "settings": "Settings & Security",
      "security": "Security & Lock",
      "cloud_backup": "Cloud Sync & Backup",
      "currency": "Base Currency",
      "language": "Language / اللغة",
      "statement_pdf": "Export PDF Statement"
    };

    const arMap = {
      "app_name": "النظام المالي الشخصي",
      "net_worth": "صافي الثروة",
      "accounts": "الحسابات المالية",
      "transactions": "المعاملات المالية",
      "assets": "الأصول والممتلكات",
      "debts": "مركز الديون",
      "budgets": "الميزانيات",
      "goals": "الأهداف المالية",
      "bills": "الفواتير والاشتراكات",
      "analytics": "التحليلات والتقارير",
      "quick_actions": "الإجراءات السريعة",
      "add_income": "إضافة دخل",
      "add_expense": "إضافة مصروف",
      "transfer": "تحويل مال",
      "new_asset": "أصل جديد",
      "new_debt": "دين جديد",
      "new_goal": "هدف جديد",
      "pay_bill": "دفع فاتورة",
      "reports": "التقارير المالية",
      "recent_activities": "آخر العمليات",
      "receivables": "ديون لك (مستحقات)",
      "payables": "ديون عليك (التزامات)",
      "settings": "الإعدادات والأمان",
      "security": "الأمان والحماية",
      "cloud_backup": "المزامنة السحابية",
      "currency": "العملة الأساسية",
      "language": "اللغة / Language",
      "statement_pdf": "تصدير كشف حساب PDF"
    };

    if (isArabic) {
      return arMap[key] ?? key;
    } else {
      return enMap[key] ?? key;
    }
  }
}
