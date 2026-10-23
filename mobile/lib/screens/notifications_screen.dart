import 'package:flutter/material.dart';

import '../models/notificacion.dart';
import '../services/notification_api.dart';
import '../utils/api_exception.dart';
import '../utils/formatters.dart';
import '../widgets/app_loading.dart';
import '../widgets/empty_state.dart';

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  final _api = NotificationApi();
  late Future<({int noLeidas, List<Notificacion> lista})> _future;

  @override
  void initState() {
    super.initState();
    _future = _api.listar();
  }

  Future<void> _reload() async {
    setState(() {
      _future = _api.listar();
    });
    try {
      await _future;
    } catch (_) {
      // el error se muestra en pantalla
    }
  }

  Future<void> _marcarLeidas() async {
    try {
      await _api.marcarLeidas();
      await _reload();
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  static IconData _icono(String? tipo) => switch (tipo) {
        'TAREA' => Icons.assignment_outlined,
        'ENTREGA' => Icons.upload_file_outlined,
        'PROYECTO' => Icons.eco_outlined,
        'RIESGO' => Icons.warning_amber_rounded,
        'MENSAJE' => Icons.campaign_outlined,
        _ => Icons.notifications_none,
      };

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(
          title: const Text('Notificaciones'),
          actions: [
            IconButton(tooltip: 'Marcar todas como leídas', icon: const Icon(Icons.done_all), onPressed: _marcarLeidas),
          ],
        ),
        body: FutureBuilder<({int noLeidas, List<Notificacion> lista})>(
          future: _future,
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done) return const AppLoading();
            if (snapshot.hasError) {
              final message = snapshot.error is ApiException ? (snapshot.error as ApiException).message : 'No se pudieron cargar.';
              return EmptyState(icon: Icons.cloud_off_outlined, title: 'Sin conexión', message: message,
                  action: FilledButton.icon(onPressed: _reload, icon: const Icon(Icons.refresh), label: const Text('Reintentar')));
            }
            final lista = snapshot.data?.lista ?? [];
            return RefreshIndicator(
              onRefresh: _reload,
              child: lista.isEmpty
                  ? ListView(children: const [
                      SizedBox(height: 380, child: EmptyState(icon: Icons.notifications_none, title: 'Estás al día',
                          message: 'Aquí verás tus tareas nuevas, las revisiones de tus entregas y los mensajes del líder.')),
                    ])
                  : ListView.separated(
                      padding: const EdgeInsets.all(12),
                      itemCount: lista.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 8),
                      itemBuilder: (context, i) {
                        final n = lista[i];
                        return Card(
                          color: n.leida ? null : Theme.of(context).colorScheme.primaryContainer.withValues(alpha: .35),
                          child: ListTile(
                            leading: Icon(_icono(n.tipo), color: n.tipo == 'RIESGO' ? Colors.redAccent : null),
                            title: Text(n.titulo, style: TextStyle(fontWeight: n.leida ? FontWeight.w500 : FontWeight.w800)),
                            subtitle: Text([if (n.mensaje != null) n.mensaje!, if (n.fecha != null) Formatters.shortDate(n.fecha)].join('\n')),
                            isThreeLine: n.mensaje != null && n.fecha != null,
                          ),
                        );
                      },
                    ),
            );
          },
        ),
      );
}
