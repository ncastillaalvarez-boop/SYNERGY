class AppConfig {
  AppConfig._();

  /// Se puede sobrescribir al arrancar: --dart-define=API_BASE_URL=http://192.168.1.10:8080/api
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080/api',
  );

  static const String appName = 'Synergy';
}
