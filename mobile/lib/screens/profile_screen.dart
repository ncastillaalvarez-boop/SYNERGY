import 'package:flutter/material.dart';
import '../utils/password_policy.dart';

import '../models/auth_session.dart';
import '../providers/locale_provider.dart';
import '../providers/theme_provider.dart';
import '../services/auth_service.dart';
import '../utils/api_exception.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({
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
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final _authService = AuthService();
  var _showChangePassword = false;
  var _isUpdating = false;

  final _currentPassword = TextEditingController();
  final _newPassword = TextEditingController();
  final _confirmPassword = TextEditingController();
  final _formKey = GlobalKey<FormState>();

  var _obscureCurrentPassword = true;
  var _obscureNewPassword = true;
  var _obscureConfirmPassword = true;

  @override
  void dispose() {
    _currentPassword.dispose();
    _newPassword.dispose();
    _confirmPassword.dispose();
    super.dispose();
  }

  Future<void> _changePassword() async {
    if (!_formKey.currentState!.validate()) return;
    if (_newPassword.text != _confirmPassword.text) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Las contraseñas nuevas no coinciden')),
      );
      return;
    }

    setState(() => _isUpdating = true);
    try {
      await _authService.changePassword(
        usuarioId: widget.session.id,
        actual: _currentPassword.text,
        nueva: _newPassword.text,
      );
      _currentPassword.clear();
      _newPassword.clear();
      _confirmPassword.clear();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Contraseña actualizada exitosamente')),
        );
        setState(() => _showChangePassword = false);
      }
    } on ApiException catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
      }
    } finally {
      if (mounted) setState(() => _isUpdating = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(
          title: const Text('Mi Perfil'),
          elevation: 0,
        ),
        body: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Avatar y nombre
              Center(
                child: Column(
                  children: [
                    Container(
                      width: 100,
                      height: 100,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: Theme.of(context).colorScheme.primary.withValues(alpha: 0.2),
                        border: Border.all(
                          color: Theme.of(context).colorScheme.primary,
                          width: 2,
                        ),
                      ),
                      child: Icon(
                        Icons.person,
                        size: 50,
                        color: Theme.of(context).colorScheme.primary,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      widget.session.nombre,
                      style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      widget.session.correo,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: Theme.of(context).colorScheme.outline,
                          ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 32),

              // Información de rol y estado
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(Icons.badge, color: Theme.of(context).colorScheme.primary),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text('Rol', style: Theme.of(context).textTheme.bodySmall),
                                Text(
                                  widget.session.rol,
                                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(fontWeight: FontWeight.bold),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                      if (widget.session.privilegios.isNotEmpty) ...[
                        const SizedBox(height: 16),
                        Divider(color: Theme.of(context).colorScheme.outlineVariant),
                        const SizedBox(height: 16),
                        Row(
                          children: [
                            Icon(Icons.security, color: Theme.of(context).colorScheme.primary),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text('Privilegios', style: Theme.of(context).textTheme.bodySmall),
                                  Wrap(
                                    spacing: 8,
                                    runSpacing: 8,
                                    children: widget.session.privilegios
                                        .map((p) => Chip(
                                              label: Text(p),

                                              backgroundColor: Theme.of(context).colorScheme.primaryContainer,
                                              labelStyle: TextStyle(
                                                color: Theme.of(context).colorScheme.onPrimaryContainer,
                                              ),
                                            ))
                                        .toList(),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ],
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 24),

              // Configuración de Tema
              Card(
                child: ListTile(
                  leading: Icon(Icons.brightness_4_outlined, color: Theme.of(context).colorScheme.primary),
                  title: const Text('Tema'),
                  subtitle: Text(_getThemeModeLabel(widget.themeProvider.themeMode)),
                  trailing: const Icon(Icons.arrow_forward_ios, size: 16),
                  onTap: _showThemeSelector,
                ),
              ),
              const SizedBox(height: 12),

              // Configuración de Idioma
              Card(
                child: ListTile(
                  leading: Icon(Icons.language_outlined, color: Theme.of(context).colorScheme.primary),
                  title: const Text('Idioma'),
                  subtitle: Text(widget.localeProvider.languageName),
                  trailing: const Icon(Icons.arrow_forward_ios, size: 16),
                  onTap: _showLanguageSelector,
                ),
              ),
              const SizedBox(height: 24),

              // Cambiar contraseña
              if (!_showChangePassword)
                OutlinedButton.icon(
                  onPressed: () => setState(() => _showChangePassword = true),
                  icon: const Icon(Icons.lock_outline),
                  label: const Text('Cambiar Contraseña'),
                )
              else
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Text('Cambiar Contraseña', style: Theme.of(context).textTheme.titleMedium),
                          const SizedBox(height: 16),
                          TextFormField(
                            controller: _currentPassword,
                            obscureText: _obscureCurrentPassword,
                            decoration: InputDecoration(
                              labelText: 'Contraseña Actual',
                              prefixIcon: const Icon(Icons.lock_outline),
                              suffixIcon: IconButton(
                                icon: Icon(_obscureCurrentPassword
                                    ? Icons.visibility_outlined
                                    : Icons.visibility_off_outlined),
                                onPressed: () => setState(() => _obscureCurrentPassword = !_obscureCurrentPassword),
                              ),
                            ),
                            validator: (value) => value == null || value.isEmpty ? 'Ingresa tu contraseña actual' : null,
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _newPassword,
                            obscureText: _obscureNewPassword,
                            decoration: InputDecoration(
                              labelText: 'Nueva Contraseña',
                              prefixIcon: const Icon(Icons.lock_outline),
                              suffixIcon: IconButton(
                                icon: Icon(_obscureNewPassword
                                    ? Icons.visibility_outlined
                                    : Icons.visibility_off_outlined),
                                onPressed: () => setState(() => _obscureNewPassword = !_obscureNewPassword),
                              ),
                            ),
                            validator: (value) =>
                                PasswordPolicy.validar(value),
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: _confirmPassword,
                            obscureText: _obscureConfirmPassword,
                            decoration: InputDecoration(
                              labelText: 'Confirmar Contraseña',
                              prefixIcon: const Icon(Icons.lock_outline),
                              suffixIcon: IconButton(
                                icon: Icon(_obscureConfirmPassword
                                    ? Icons.visibility_outlined
                                    : Icons.visibility_off_outlined),
                                onPressed: () => setState(() => _obscureConfirmPassword = !_obscureConfirmPassword),
                              ),
                            ),
                            validator: (value) =>
                                PasswordPolicy.validar(value),
                          ),
                          const SizedBox(height: 16),
                          FilledButton.icon(
                            onPressed: _isUpdating ? null : _changePassword,
                            icon: _isUpdating ? const SizedBox.shrink() : const Icon(Icons.check),
                            label: const Text('Actualizar Contraseña'),
                          ),
                          const SizedBox(height: 8),
                          OutlinedButton(
                            onPressed: () => setState(() => _showChangePassword = false),
                            child: const Text('Cancelar'),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              const SizedBox(height: 32),

              // Cerrar sesión
              FilledButton.tonalIcon(
                onPressed: widget.onLogout,
                icon: const Icon(Icons.logout),
                label: const Text('Cerrar Sesión'),
                style: FilledButton.styleFrom(
                  backgroundColor: Colors.red.withValues(alpha: 0.1),
                  foregroundColor: Colors.red,
                ),
              ),
            ],
          ),
        ),
      );

  void _showThemeSelector() {
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
                }
              },
            ),
          ],
        ),
      ),
    );
  }

  void _showLanguageSelector() {
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
                }
              },
            ),
          ],
        ),
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
}
