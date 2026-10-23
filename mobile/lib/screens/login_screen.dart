import 'package:flutter/material.dart';

import '../models/auth_session.dart';
import '../services/auth_service.dart';
import '../utils/api_exception.dart';
import '../utils/app_config.dart';
import '../widgets/gradient_button.dart';
import '../widgets/server_dialog.dart';
import 'register_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key, required this.onAuthenticated});
  final ValueChanged<AuthSession> onAuthenticated;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _service = AuthService();
  var _obscurePassword = true;
  var _submitting = false;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    try {
      final session = await _service.login(_email.text, _password.text);
      widget.onAuthenticated(session);
    } on ApiException catch (error) {
      _mostrar(error.message);
    } catch (_) {
      _mostrar('Ocurrió un error inesperado. Intenta de nuevo.');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  void _mostrar(String mensaje) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(mensaje), duration: const Duration(seconds: 6)));
  }

  /// Permite cambiar la dirección del servidor (IP del PC) sin recompilar la app.
  Future<void> _configurarServidor() async {
    final url = await mostrarDialogoServidor(context);
    if (url == null) return;
    if (mounted) setState(() {});
    _mostrar('Servidor: $url');
  }

  Future<void> _openRegister() async {
    final session = await Navigator.of(context).push<AuthSession>(MaterialPageRoute(builder: (_) => const RegisterScreen()));
    if (session != null && mounted) widget.onAuthenticated(session);
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        body: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 440),
                child: Card(
                  child: Padding(
                    padding: const EdgeInsets.all(28),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Center(child: Image.asset('assets/logo-mark.png', width: 92, height: 92)),
                          const SizedBox(height: 22),
                          Text('Bienvenido a Synergy', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
                          const SizedBox(height: 8),
                          Text('Gestiona iniciativas y participa desde cualquier lugar.', style: Theme.of(context).textTheme.bodyMedium),
                          const SizedBox(height: 26),
                          TextFormField(
                            controller: _email,
                            keyboardType: TextInputType.emailAddress,
                            decoration: const InputDecoration(labelText: 'Correo electrónico', prefixIcon: Icon(Icons.mail_outline)),
                            validator: (value) => value == null || !value.contains('@') ? 'Ingresa un correo válido' : null,
                          ),
                          const SizedBox(height: 14),
                          TextFormField(
                            controller: _password,
                            obscureText: _obscurePassword,
                            decoration: InputDecoration(
                              labelText: 'Contraseña',
                              prefixIcon: const Icon(Icons.lock_outline),
                              suffixIcon: IconButton(
                                tooltip: _obscurePassword ? 'Mostrar contraseña' : 'Ocultar contraseña',
                                icon: Icon(_obscurePassword ? Icons.visibility_outlined : Icons.visibility_off_outlined),
                                onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                              ),
                            ),
                            validator: (value) => value == null || value.length < 6 ? 'Mínimo 6 caracteres' : null,
                            onFieldSubmitted: (_) => _login(),
                          ),
                          const SizedBox(height: 22),
                          GradientButton(label: 'Iniciar sesión', icon: Icons.login, loading: _submitting, onPressed: _login),
                          const SizedBox(height: 14),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              const Text('¿Aún no tienes cuenta?'),
                              TextButton(onPressed: _submitting ? null : _openRegister, child: const Text('Regístrate')),
                            ],
                          ),
                          const Divider(height: 28),
                          TextButton.icon(
                            onPressed: _submitting ? null : _configurarServidor,
                            icon: const Icon(Icons.dns_outlined, size: 18),
                            label: Text('Servidor: ${AppConfig.apiBaseUrl}', overflow: TextOverflow.ellipsis),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      );
}
