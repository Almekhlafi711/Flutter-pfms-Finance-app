import 'package:flutter/material.dart';
import 'app_colors.dart';
import 'design_tokens.dart';

class AppTheme {
  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: AppColors.slateDarkBackground,
      colorScheme: const ColorScheme.dark(
        primary: AppColors.tealAccent,
        surface: AppColors.slateDarkSurface,
        background: AppColors.slateDarkBackground,
        secondary: AppColors.goldAccent,
        error: AppColors.redExpense,
      ),
      cardTheme: CardThemeData(
        color: AppColors.slateDarkCard,
        elevation: DesignTokens.cardElevation,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(DesignTokens.radiusMedium),
          side: const BorderSide(color: AppColors.slateBorder, width: DesignTokens.borderWidth),
        ),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.slateDarkBackground,
        foregroundColor: AppColors.textPrimaryDark,
        elevation: 0,
      ),
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        backgroundColor: AppColors.slateDarkSurface,
        selectedItemColor: AppColors.tealAccent,
        unselectedItemColor: AppColors.textSecondaryDark,
        type: BottomNavigationBarType.fixed,
      ),
    );
  }

  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      scaffoldBackgroundColor: AppColors.lightBackground,
      colorScheme: const ColorScheme.light(
        primary: AppColors.tealAccent,
        surface: AppColors.lightSurface,
        background: AppColors.lightBackground,
        secondary: AppColors.goldAccent,
        error: AppColors.redExpense,
      ),
      cardTheme: CardThemeData(
        color: AppColors.lightCard,
        elevation: DesignTokens.cardElevation,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(DesignTokens.radiusMedium),
          side: const BorderSide(color: AppColors.lightBorder, width: DesignTokens.borderWidth),
        ),
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.lightBackground,
        foregroundColor: AppColors.textPrimaryLight,
        elevation: 0,
      ),
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        backgroundColor: AppColors.lightSurface,
        selectedItemColor: AppColors.tealAccent,
        unselectedItemColor: AppColors.textSecondaryLight,
        type: BottomNavigationBarType.fixed,
      ),
    );
  }
}
