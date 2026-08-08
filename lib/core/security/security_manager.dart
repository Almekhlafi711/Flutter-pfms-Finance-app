import 'package:shared_preferences/shared_preferences.dart';

class SecurityManager {
  SharedPreferences? _prefs;

  Future<void> init() async {
    _prefs = await SharedPreferences.getInstance();
  }

  bool get isBiometricsEnabled => _prefs?.getBool('biometrics_enabled') ?? false;
  set isBiometricsEnabled(bool value) => _prefs?.setBool('biometrics_enabled', value);

  bool get isPinEnabled => _prefs?.getBool('pin_enabled') ?? false;
  set isPinEnabled(bool value) => _prefs?.setBool('pin_enabled', value);

  String get userPin => _prefs?.getString('user_pin') ?? "";
  set userPin(String value) => _prefs?.setString('user_pin', value);

  bool get isCloudBackupEnabled => _prefs?.getBool('cloud_backup_enabled') ?? false;
  set isCloudBackupEnabled(bool value) => _prefs?.setBool('cloud_backup_enabled', value);

  String get baseCurrency => _prefs?.getString('base_currency') ?? "SAR";
  set baseCurrency(String value) => _prefs?.setString('base_currency', value);

  String get selectedLanguage => _prefs?.getString('selected_language') ?? "en";
  set selectedLanguage(String value) => _prefs?.setString('selected_language', value);

  bool verifyPin(String inputPin) => userPin == inputPin;
}
