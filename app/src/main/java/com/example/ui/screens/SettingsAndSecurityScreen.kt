package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.DesignTokens
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.PfmsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndSecurityScreen(
    viewModel: PfmsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val secManager = viewModel.securityManager
    var biometricsEnabled by remember { mutableStateOf(secManager.isBiometricsEnabled) }
    var pinEnabled by remember { mutableStateOf(secManager.isPinEnabled) }
    var cloudBackupEnabled by remember { mutableStateOf(secManager.isCloudBackupEnabled) }
    val isArabic by viewModel.isArabic.collectAsState()

    var userName by remember { mutableStateOf("Mohammed Al-Mkhlafi") }
    var userEmail by remember { mutableStateOf("mohammed@example.com") }
    var selectedCurrency by remember { mutableStateOf("USD ($)") }
    var selectedTheme by remember { mutableStateOf(if (isArabic) "تلقائي" else "System Default") }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "الإعدادات والأمان" else "Settings & Security",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Profile Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.RadiusLarge),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = { showProfileDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isArabic) "تعديل" else "Edit",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // 2. Premium Pro Subscription Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.RadiusMedium),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "اشتراك PFMS Pro المميز" else "PFMS Pro Subscription",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isArabic) "حسابات غير محدودة، نسخ سحابي، وتقارير AI" else "Unlimited accounts, cloud sync & AI insights",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Button(
                        onClick = {
                            Toast.makeText(
                                context,
                                if (isArabic) "أنت مشترك في الباقة الاحترافية!" else "You are already on the Pro plan!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Text(
                            text = if (isArabic) "نشط Pro" else "Active Pro",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Application Preferences Section
            SettingsSectionCard(
                title = if (isArabic) "إعدادات التطبيق" else "APP PREFERENCES"
            ) {
                // Language
                SettingsRowItem(
                    icon = Icons.Default.Language,
                    title = if (isArabic) "لغة التطبيق" else "App Language",
                    value = if (isArabic) "العربية" else "English",
                    onClick = { viewModel.toggleLanguage() }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Default Currency
                SettingsRowItem(
                    icon = Icons.Default.AttachMoney,
                    title = if (isArabic) "العملة الافتراضية" else "Default Currency",
                    value = selectedCurrency,
                    onClick = { showCurrencyDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Theme Mode
                SettingsRowItem(
                    icon = Icons.Default.DarkMode,
                    title = if (isArabic) "مظهر التطبيق (الثيم)" else "App Theme",
                    value = selectedTheme,
                    onClick = { showThemeDialog = true }
                )
            }

            // 4. Security & Access Section
            SettingsSectionCard(
                title = if (isArabic) "الأمان والوصول" else "SECURITY & ACCESS"
            ) {
                // Biometrics
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = if (isArabic) "بصمة الأصبع / Face ID" else "Biometrics / Face ID",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = if (isArabic) "قفل التطبيق بالبصمة للحماية" else "Protect app access with biometrics",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                    Switch(
                        checked = biometricsEnabled,
                        onCheckedChange = {
                            biometricsEnabled = it
                            secManager.isBiometricsEnabled = it
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // PIN Code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = if (isArabic) "رمز المرور (PIN)" else "Passcode (PIN)",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = if (isArabic) "تفعيل قفل الرمز السرّي" else "Enable 4-digit passcode lock",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                    Switch(
                        checked = pinEnabled,
                        onCheckedChange = {
                            pinEnabled = it
                            secManager.isPinEnabled = it
                        }
                    )
                }
            }

            // 5. Data & Cloud Backup
            SettingsSectionCard(
                title = if (isArabic) "البيانات والنسخ الاحتياطي" else "DATA & CLOUD BACKUP"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = if (isArabic) "النسخ السحابي والتزامن" else "Cloud Sync & Backup",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = if (isArabic) "تزامن البيانات تلقائياً مع السحابة" else "Auto-sync data safely to cloud",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                    Switch(
                        checked = cloudBackupEnabled,
                        onCheckedChange = {
                            cloudBackupEnabled = it
                            secManager.isCloudBackupEnabled = it
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Export Data
                SettingsRowItem(
                    icon = Icons.Default.Download,
                    title = if (isArabic) "تصدير البيانات (PDF / CSV)" else "Export Data (PDF / CSV)",
                    value = "",
                    onClick = {
                        viewModel.exportAccountStatementPdf()
                        Toast.makeText(
                            context,
                            if (isArabic) "تم تصدير كشف الحساب بنجاح!" else "Statement exported successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Import Data
                SettingsRowItem(
                    icon = Icons.Default.Upload,
                    title = if (isArabic) "استيراد واستعادة البيانات" else "Import & Restore Data",
                    value = "",
                    onClick = {
                        Toast.makeText(
                            context,
                            if (isArabic) "اختر ملف النسخة الاحتياطية الاستعادة" else "Select backup file to restore",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            // 6. Support & Legal Section
            SettingsSectionCard(
                title = if (isArabic) "الدعم والمعلومات" else "SUPPORT & LEGAL"
            ) {
                // Support
                SettingsRowItem(
                    icon = Icons.Default.SupportAgent,
                    title = if (isArabic) "التواصل مع الدعم الفني" else "Contact Support",
                    value = if (isArabic) "واتساب / بريد" else "WhatsApp / Email",
                    onClick = { showSupportDialog = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Privacy Policy
                SettingsRowItem(
                    icon = Icons.Default.Shield,
                    title = if (isArabic) "سياسة الخصوصية" else "Privacy Policy",
                    value = "",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.google.com/privacy"))
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Terms of Use
                SettingsRowItem(
                    icon = Icons.Default.Description,
                    title = if (isArabic) "شروط الاستخدام" else "Terms of Service",
                    value = "",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.google.com/terms"))
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // About & Version
                SettingsRowItem(
                    icon = Icons.Default.Info,
                    title = if (isArabic) "حول التطبيق والإصدار" else "About App & Version",
                    value = "v2.4.0 (FinTech)",
                    onClick = {
                        Toast.makeText(
                            context,
                            "PFMS FinTech Edition v2.4.0 - Built with Jetpack Compose",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- Dialogs ---

    // Profile Edit Dialog
    if (showProfileDialog) {
        var tempName by remember { mutableStateOf(userName) }
        var tempEmail by remember { mutableStateOf(userEmail) }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Text(
                    text = if (isArabic) "تعديل الملف الشخصي" else "Edit Profile",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text(if (isArabic) "الاسم الكامل" else "Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempEmail,
                        onValueChange = { tempEmail = it },
                        label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userName = tempName
                        userEmail = tempEmail
                        showProfileDialog = false
                    }
                ) {
                    Text(if (isArabic) "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Currency Picker Dialog
    if (showCurrencyDialog) {
        val currencies = listOf("USD ($)", "YER (ر.ي)", "SAR (ر.س)", "EUR (€)", "AED (د.إ)")
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text(if (isArabic) "اختر العملة الافتراضية" else "Select Default Currency", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    currencies.forEach { curr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCurrency = curr
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(curr, style = MaterialTheme.typography.bodyLarge)
                            if (selectedCurrency == curr) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Theme Picker Dialog
    if (showThemeDialog) {
        val themes = if (isArabic) listOf("تلقائي (حسب النظام)", "فاتح", "داكن") else listOf("System Default", "Light", "Dark")
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(if (isArabic) "اختر الثيم" else "Select Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    themes.forEach { th ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTheme = th
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(th, style = MaterialTheme.typography.bodyLarge)
                            if (selectedTheme == th) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Support Channels Dialog
    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text(if (isArabic) "الدعم الفني والخدمة" else "Customer Support", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (isArabic) "اختر وسيلة التواصل المناسبة لك:" else "Choose your preferred channel:")
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/967770000000"))
                            context.startActivity(intent)
                            showSupportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("💬 WhatsApp Support")
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@pfms.com"))
                            context.startActivity(intent)
                            showSupportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("✉️ Email Support")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text(if (isArabic) "إغلاق" else "Close")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.RadiusMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
