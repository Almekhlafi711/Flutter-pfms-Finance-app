package com.example.core.security

import android.content.Context

class SecurityManager(context: Context) {
    private val prefs = context.getSharedPreferences("pfms_security_prefs", Context.MODE_PRIVATE)

    var isBiometricsEnabled: Boolean
        get() = prefs.getBoolean("biometrics_enabled", false)
        set(value) = prefs.edit().putBoolean("biometrics_enabled", value).apply()

    var isPinEnabled: Boolean
        get() = prefs.getBoolean("pin_enabled", false)
        set(value) = prefs.edit().putBoolean("pin_enabled", value).apply()

    var userPin: String
        get() = prefs.getString("user_pin", "") ?: ""
        set(value) = prefs.edit().putString("user_pin", value).apply()

    var isCloudBackupEnabled: Boolean
        get() = prefs.getBoolean("cloud_backup_enabled", false)
        set(value) = prefs.edit().putBoolean("cloud_backup_enabled", value).apply()

    var baseCurrency: String
        get() = prefs.getString("base_currency", "SAR") ?: "SAR"
        set(value) = prefs.edit().putString("base_currency", value).apply()

    var selectedLanguage: String
        get() = prefs.getString("selected_language", "en") ?: "en"
        set(value) = prefs.edit().putString("selected_language", value).apply()

    fun verifyPin(inputPin: String): Boolean {
        return userPin == inputPin
    }
}
