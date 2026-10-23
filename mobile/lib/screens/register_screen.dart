import 'package:flutter/material.dart';

import '../models/auth_session.dart';
import '../models/usuario.dart';
import '../services/auth_service.dart';
import '../utils/api_exception.dart';
import '../widgets/gradient_button.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _service = AuthService();
  var _isLeader = false;
  var _submitting = false;

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _register() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _submitting = true);
    try {
      final session = await _service.register(Usuario(
        nombre: _name.text.trim(),
        correo: _email.text.trim(),
        contrasena: _password.text,
        rol: _isLeader ? 'LIDER' : 'PARTICIPANTE',
      ));
      if (mounted) Navigator.of(context).pop<AuthSession>(session);
    } on ApiException catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message), duration: const Duration(seconds: 6)));
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Ocurrió un error inesperado. Intenta de nuevo.')));
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Crear cuenta')),
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 440),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text('Únete a la comunidad', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
                      const SizedBox(height: 8),
                      const Text('Crea tu perfil para descubrir o liderar proyectos comunitarios.'),
                      const SizedBox(height: 24),
                      TextFormField(
                        controller: _name,
                        decoration: const InputDecoration(labelText: 'Nombre completo', prefixIcon: Icon(Icons.person_outline)),
                        validator: (value) => value == null || value.trim().length < 3 ? 'Ingresa tu nombre' : null,
                      ),
                      const SizedBox(height: 14),
                      TextFormField(
                        controller: _email,
                        keyboardType: TextInputType.emailAddress,
                        decoration: const InputDecoration(labelText: 'Correo electrónico', prefixIcon: Icon(Icons.mail_outline)),
                        validator: (value) => value == null || !value.contains('@') ? 'Ingresa un correo válido' : null,
                      ),
                      const SizedBox(height: 14),
                      TextFormField(
                        controller: _password,
                        obscureText: true,
                        decoration: const InputDecoration(labelText: 'Contraseña', prefixIcon: Icon(Icons.lock_outline)),
                        validator: (value) => value == null || value.length < 6 ? 'Mínimo 6 caracteres' : null,
                      ),
                      const SizedBox(height: 14),
                      SwitchListTile.adaptive(
                        value: _isLeader,
                        onChanged: (value) => setState(() => _isLeader = value),
                        secondary: const Icon(Icons.volunteer_activism_outlined),
                        title: const Text('Quiero ser líder de proyectos'),
                        subtitle: const Text('Podrás crear proyectos y asignar tareas.'),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                      ),
                      const SizedBox(height: 24),
                      GradientButton(label: 'Crear cuenta', icon: Icons.person_add_alt_1, loading: _submitting, onPressed: _register),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      );
}
