package com.example.core.localization

object AppLocalizations {

    fun getString(key: String, isArabic: Boolean = false): String {
        val enMap = mapOf(
            "app_name" to "PFMS Finance",
            "net_worth" to "Net Worth",
            "accounts" to "Accounts",
            "transactions" to "Transactions",
            "assets" to "Assets",
            "debts" to "Debt Center",
            "budgets" to "Budgets",
            "goals" to "Goals",
            "bills" to "Bills & Subscriptions",
            "analytics" to "Analytics & Reports",
            "quick_actions" to "Quick Actions",
            "add_income" to "Add Income",
            "add_expense" to "Add Expense",
            "transfer" to "Transfer",
            "new_asset" to "New Asset",
            "new_debt" to "New Debt",
            "new_goal" to "New Goal",
            "pay_bill" to "Pay Bill",
            "reports" to "Reports",
            "recent_activities" to "Recent Activities",
            "receivables" to "Receivables (Owed to You)",
            "payables" to "Payables (You Owe)",
            "settings" to "Settings & Security",
            "security" to "Security & Lock",
            "cloud_backup" to "Cloud Sync & Backup",
            "currency" to "Base Currency",
            "language" to "Language / اللغة",
            "statement_pdf" to "Export PDF Statement"
        )

        val arMap = mapOf(
            "app_name" to "النظام المالي الشخصي",
            "net_worth" to "صافي الثروة",
            "accounts" to "الحسابات المالية",
            "transactions" to "المعاملات المالية",
            "assets" to "الأصول والممتلكات",
            "debts" to "مركز الديون",
            "budgets" to "الميزانيات",
            "goals" to "الأهداف المالية",
            "bills" to "الفواتير والاشتراكات",
            "analytics" to "التحليلات والتقارير",
            "quick_actions" to "الإجراءات السريعة",
            "add_income" to "إضافة دخل",
            "add_expense" to "إضافة مصروف",
            "transfer" to "تحويل مال",
            "new_asset" to "أصل جديد",
            "new_debt" to "دين جديد",
            "new_goal" to "هدف جديد",
            "pay_bill" to "دفع فاتورة",
            "reports" to "التقارير المالية",
            "recent_activities" to "آخر العمليات",
            "receivables" to "ديون لك (مستحقات)",
            "payables" to "ديون عليك (التزامات)",
            "settings" to "الإعدادات والأمان",
            "security" to "الأمان والحماية",
            "cloud_backup" to "المزامنة السحابية",
            "currency" to "العملة الأساسية",
            "language" to "اللغة / Language",
            "statement_pdf" to "تصدير كشف حساب PDF"
        )

        return if (isArabic) arMap[key] ?: key else enMap[key] ?: key
    }
}
