import 'package:flutter/material.dart';

import '../models/auth_session.dart';
import '../providers/locale_provider.dart';
import '../providers/theme_provider.dart';
import '../services/auth_service.dart';
import '../utils/app_localizations.dart';
import 'admin_users_screen.dart';
import 'profile_screen.dart';
import 'projects_screen.dart';
import 'tasks_screen.dart';

class HomeShell extends StatefulWidget {
  const HomeShell({
    super.key,
    required this.session,
    required this.themeProvider,
    required this.localeProvider,
    required this.onLogout,
  });

  final AuthSession session;
  final ThemeProvider themeProvider;
  final LocaleProvider localeProvider;
  final VoidCallback onLogout;

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  var _index = 0;

  Future<void> _logout() async {
    await AuthService().logout();
    widget.onLogout();
  }

  void _showSettingsMenu(BuildContext context, AppLocalizations loc) {
    showModalBottomSheet(
      context: context,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(
                'Configuración',
                style: Theme.of(context).textTheme.titleMedium,
              ),
            ),
            const Divider(),
            // Tema
            ListTile(
              leading: const Icon(Icons.brightness_4_outlined),
              title: const Text('Tema'),
              subtitle: Text(_getThemeModeLabel(widget.themeProvider.themeMode)),
              trailing: const Icon(Icons.arrow_forward_ios, size: 16),
              onTap: () => _showThemeSelector(context),
            ),
            // Idioma
            ListTile(
              leading: const Icon(Icons.language_outlined),
              title: const Text('Idioma'),
              subtitle: Text(widget.localeProvider.languageName),
              trailing: const Icon(Icons.arrow_forward_ios, size: 16),
              onTap: () => _showLanguageSelector(context),
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }

  void _showThemeSelector(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Seleccionar Tema'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            RadioListTile<ThemeMode>(
              title: const Text('Claro'),
              value: ThemeMode.light,
              groupValue: widget.themeProvider.themeMode,
              onChanged: (value) {
                if (value != null) {
                  widget.themeProvider.setThemeMode(value);
                  Navigator.pop(context);
                  Navigator.pop(context);
                }
              },
            ),
            RadioListTile<ThemeMode>(
              title: const Text('Oscuro'),
              value: ThemeMode.dark,
              groupValue: widget.themeProvider.themeMode,
              onChanged: (value) {
                if (value != null) {
                  widget.themeProvider.setThemeMode(value);
                  Navigator.pop(context);
                  Navigator.pop(context);
                }
              },
            ),
            RadioListTile<ThemeMode>(
              title: const Text('Sistema'),
              value: ThemeMode.system,
              groupValue: widget.themeProvider.themeMode,
              onChanged: (value) {
                if (value != null) {
                  widget.themeProvider.setThemeMode(value);
                  Navigator.pop(context);
                  Navigator.pop(context);
                }
              },
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cerrar')),
        ],
      ),
    );
  }

  void _showLanguageSelector(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Seleccionar Idioma'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            RadioListTile<String>(
              title: const Text('Español'),
              value: 'es',
              groupValue: widget.localeProvider.locale.languageCode,
              onChanged: (value) {
                if (value != null) {
                  widget.localeProvider.setLocale(const Locale('es', 'ES'));
                  Navigator.pop(context);
                  Navigator.pop(context);
                }
              },
            ),
            RadioListTile<String>(
              title: const Text('English'),
              value: 'en',
              groupValue: widget.localeProvider.locale.languageCode,
              onChanged: (value) {
                if (value != null) {
                  widget.localeProvider.setLocale(const Locale('en', 'US'));
                  Navigator.pop(context);
                  Navigator.pop(context);
                }
              },
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cerrar')),
        ],
      ),
    );
  }

  String _getThemeModeLabel(ThemeMode mode) {
    return switch (mode) {
      ThemeMode.light => 'Claro',
      ThemeMode.dark => 'Oscuro',
      ThemeMode.system => 'Sistema',
    };
  }

  @override
  Widget build(BuildContext context) {
    final loc = AppLocalizations.of(context);
    final destinations = <NavigationDestination>[
      NavigationDestination(
        icon: const Icon(Icons.dashboard_outlined),
        selectedIcon: const Icon(Icons.dashboard),
        label: loc.projectsTab,
      ),
      NavigationDestination(
        icon: const Icon(Icons.assignment_outlined),
        selectedIcon: const Icon(Icons.assignment),
        label: loc.tasksTab,
      ),
      if (widget.session.isAdmin)
        NavigationDestination(
          icon: const Icon(Icons.manage_accounts_outlined),
          selectedIcon: const Icon(Icons.manage_accounts),
          label: loc.usersTab,
        ),
      NavigationDestination(
        icon: const Icon(Icons.person_outline),
        selectedIcon: const Icon(Icons.person),
        label: loc.profileTab,
      ),
    ];

    final pages = <Widget>[
      ProjectsScreen(session: widget.session),
      TasksScreen(session: widget.session),
      if (widget.session.isAdmin) AdminUsersScreen(session: widget.session),
      ProfileScreen(
        session: widget.session,
        themeProvider: widget.themeProvider,
        localeProvider: widget.localeProvider,
        onLogout: _logout,
      ),
    ];

    return Scaffold(
      appBar: AppBar(
        title: Text('Synergy'),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings_outlined),
            tooltip: 'Configuración',
            onPressed: () => _showSettingsMenu(context, loc),
          ),
        ],
      ),
      body: IndexedStack(index: _index, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        destinations: destinations,
        onDestinationSelected: (value) => setState(() => _index = value),
      ),
    );
  }
}
