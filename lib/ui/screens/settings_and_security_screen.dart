import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/design_tokens.dart';
import '../viewmodel/pfms_provider.dart';

class SettingsAndSecurityScreen extends ConsumerWidget {
  final VoidCallback onNavigateBack;

  const SettingsAndSecurityScreen({super.key, required this.onNavigateBack});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final controller = PfmsController(ref);
    final isArabic = ref.watch(isArabicProvider);
    final security = ref.watch(securityManagerProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(isArabic ? "الإعدادات والأمان" : "Settings & Security"),
        leading: IconButton(icon: const Icon(Icons.arrow_back), onPressed: onNavigateBack),
      ),
      body: ListView(
        padding: const EdgeInsets.all(DesignTokens.spaceMedium),
        children: [
          Card(
            color: AppColors.slateDarkCard,
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.language, color: AppColors.tealAccent),
                  title: Text(isArabic ? "اللغة (Language)" : "Language / اللغة"),
                  subtitle: Text(isArabic ? "العربية" : "English"),
                  trailing: Switch(
                    value: isArabic,
                    activeThumbColor: AppColors.tealAccent,
                    onChanged: (_) => controller.toggleLanguage(),
                  ),
                ),
                const Divider(color: AppColors.slateBorder),
                ListTile(
                  leading: const Icon(Icons.fingerprint, color: AppColors.goldAccent),
                  title: Text(isArabic ? "القفل بالبصمة" : "Biometric Lock"),
                  subtitle: Text(isArabic ? "حماية التطبيق بالبصمة" : "Protect app with fingerprint"),
                  trailing: Switch(
                    value: security.isBiometricsEnabled,
                    activeThumbColor: AppColors.goldAccent,
                    onChanged: (val) {
                      security.isBiometricsEnabled = val;
                      ref.read(toastMessageProvider.notifier).state = isArabic ? "تم تحديث إعدادات الأمان" : "Security settings updated";
                    },
                  ),
                ),
                const Divider(color: AppColors.slateBorder),
                ListTile(
                  leading: const Icon(Icons.cloud_sync, color: AppColors.tealAccentLight),
                  title: Text(isArabic ? "المزامنة السحابية" : "Cloud Sync"),
                  subtitle: Text(isArabic ? "نسخ احتياطي للبيانات" : "Backup data safely"),
                  trailing: Switch(
                    value: security.isCloudBackupEnabled,
                    activeThumbColor: AppColors.tealAccentLight,
                    onChanged: (val) {
                      security.isCloudBackupEnabled = val;
                      ref.read(toastMessageProvider.notifier).state = isArabic ? "تم تحديث إعدادات النسخ الاحتياطي" : "Cloud backup settings updated";
                    },
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
