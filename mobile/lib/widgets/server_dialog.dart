import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import '../utils/app_config.dart';

/// Diálogo para cambiar la dirección del servidor sin recompilar la app.
/// Se abre desde el inicio de sesión y desde el engranaje de Configuración.
/// Devuelve la nueva URL guardada, o null si se canceló.
Future<String?> mostrarDialogoServidor(BuildContext context) async {
  final controller = TextEditingController(text: AppConfig.apiBaseUrl);
  final nueva = await showDialog<String>(
    context: context,
    builder: (context) => _ServidorDialog(controller: controller),
  );
  controller.dispose();
  if (nueva == null) return null;
  return AppConfig.setApiBaseUrl(nueva);
}

/// Llama a GET /api/public/ping. Devuelve null si responde, o el motivo del fallo.
Future<String?> probarServidor(String direccion) async {
  final base = AppConfig.normalizar(direccion);
  try {
    final r = await http.get(Uri.parse('$base/public/ping')).timeout(const Duration(seconds: 5));
    return r.statusCode == 200 ? null : 'El servidor respondió con el código ${r.statusCode}.';
  } catch (_) {
    return 'No hay respuesta en $base.';
  }
}

class _ServidorDialog extends StatefulWidget {
  const _ServidorDialog({required this.controller});
  final TextEditingController controller;

  @override
  State<_ServidorDialog> createState() => _ServidorDialogState();
}

class _ServidorDialogState extends State<_ServidorDialog> {
  var _probando = false;
  String? _resultado;
  var _ok = false;

  Future<void> _probar() async {
    setState(() {
      _probando = true;
      _resultado = null;
    });
    final error = await probarServidor(widget.controller.text);
    if (!mounted) return;
    setState(() {
      _probando = false;
      _ok = error == null;
      _resultado = error ?? 'Conexión correcta.';
    });
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
        title: const Text('Servidor de Synergy'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(
              controller: widget.controller,
              keyboardType: TextInputType.url,
              decoration: const InputDecoration(labelText: 'Dirección', hintText: '192.168.1.10:8080'),
            ),
            const SizedBox(height: 12),
            const Text('Emulador de Android: 10.0.2.2:8080\n'
                'Navegador (Edge/Chrome): localhost:8080\n'
                'Teléfono real: la IP de tu PC (en Windows: ipconfig → Dirección IPv4), '
                'con el teléfono en la misma red Wi-Fi.\n'
                'El servidor Spring Boot debe estar encendido.'),
            const SizedBox(height: 12),
            Row(children: [
              OutlinedButton.icon(
                onPressed: _probando ? null : _probar,
                icon: _probando
                    ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Icon(Icons.wifi_tethering),
                label: const Text('Probar conexión'),
              ),
            ]),
            if (_resultado != null)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(_resultado!, style: TextStyle(color: _ok ? Colors.green : Theme.of(context).colorScheme.error)),
              ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, AppConfig.defaultApiBaseUrl), child: const Text('Restablecer')),
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(context, widget.controller.text), child: const Text('Guardar')),
        ],
      );
}
