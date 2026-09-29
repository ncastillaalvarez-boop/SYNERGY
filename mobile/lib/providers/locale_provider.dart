import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class LocaleProvider extends ChangeNotifier {
  static const _localeKey = 'locale';

  Locale _locale = const Locale('es', 'ES');

  Locale get locale => _locale;
  String get languageName => _locale.languageCode == 'en' ? 'English' : 'Español';

  Future<void> loadLocale() async {
    final prefs = await SharedPreferences.getInstance();
    final code = prefs.getString(_localeKey) ?? 'es';
    _locale = _stringToLocale(code);
    notifyListeners();
  }

  Future<void> setLocale(Locale locale) async {
    _locale = locale;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_localeKey, locale.languageCode);
    notifyListeners();
  }

  Locale _stringToLocale(String code) {
    return switch (code) {
      'en' => const Locale('en', 'US'),
      'es' => const Locale('es', 'ES'),
      _ => const Locale('es', 'ES'),
    };
  }
}
