import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';

import 'models/auth_session.dart';
import 'providers/locale_provider.dart';
import 'providers/theme_provider.dart';
import 'screens/home_shell.dart';
import 'screens/login_screen.dart';
import 'services/auth_service.dart';
import 'services/notification_service.dart';
import 'utils/app_config.dart';
import 'utils/app_localizations.dart';
import 'utils/app_theme.dart';
import 'widgets/app_loading.dart';

/// true solo si Firebase quedó configurado (requiere android/app/google-services.json).
/// Sin ese archivo la app funciona igual, solo que sin notificaciones push.
bool firebaseDisponible = false;

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Dirección del servidor guardada desde la pantalla de login
  await AppConfig.load();

  try {
    await Firebase.initializeApp();
    firebaseDisponible = true;
  } catch (_) {
    firebaseDisponible = false;
  }

  runApp(const SynergyApp());
}

class SynergyApp extends StatefulWidget {
  const SynergyApp({super.key});

  @override
  State<SynergyApp> createState() => _SynergyAppState();
}

class _SynergyAppState extends State<SynergyApp> {
  final _authService = AuthService();
  final _themeProvider = ThemeProvider();
  final _localeProvider = LocaleProvider();
  
  AuthSession? _session;
  var _loadingSession = true;

  @override
  void initState() {
    super.initState();
    _initializeApp();
  }

  Future<void> _initializeApp() async {
    await Future.wait([
      _restoreSession(),
      _themeProvider.loadThemeMode(),
      _localeProvider.loadLocale(),
    ]);

    // Notificaciones push solo si Firebase está configurado
    if (firebaseDisponible) {
      try {
        await NotificationService().initialize();
      } catch (_) {
        // Sin permisos o sin servicios de Google: la app sigue funcionando
      }
    }
  }

  Future<void> _restoreSession() async {
    final session = await _authService.restoreSession();
    if (mounted) {
      setState(() {
        _session = session;
        _loadingSession = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
        listenable: Listenable.merge([_themeProvider, _localeProvider]),
        builder: (context, child) => MaterialApp(
          title: AppConfig.appName,
          debugShowCheckedModeBanner: false,
          locale: _localeProvider.locale,
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          themeMode: _themeProvider.themeMode,
          theme: AppTheme.light(),
          darkTheme: AppTheme.dark(),
          home: _loadingSession
              ? const Scaffold(body: AppLoading(label: 'Preparando tu espacio de trabajo…'))
              : _session == null
                  ? LoginScreen(onAuthenticated: (session) => setState(() => _session = session))
                  : HomeShell(
                      session: _session!,
                      themeProvider: _themeProvider,
                      localeProvider: _localeProvider,
                      onLogout: () => setState(() => _session = null),
                    ),
        ),
      );
}
