import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

import '../models/auth_session.dart';
import '../models/entrega_tarea.dart';
import '../models/tarea.dart';
import '../services/delivery_service.dart';
import '../services/task_service.dart';
import '../utils/api_exception.dart';
import '../utils/formatters.dart';
import '../widgets/app_loading.dart';
import '../widgets/gradient_button.dart';
import '../widgets/status_chip.dart';

class TaskDetailScreen extends StatefulWidget {
  const TaskDetailScreen({super.key, required this.taskId, required this.session});
  final String taskId;
  final AuthSession session;

  @override
  State<TaskDetailScreen> createState() => _TaskDetailScreenState();
}

class _TaskDetailScreenState extends State<TaskDetailScreen> {
  final _taskService = TaskService();
  final _deliveryService = DeliveryService();
  Tarea? _task;
  List<EntregaTarea> _deliveries = const [];
  bool _loading = true;
  bool _uploading = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final task = await _taskService.getById(widget.taskId);
      List<EntregaTarea> deliveries = const [];
      try {
        deliveries = await _deliveryService.getForTask(widget.taskId);
      } on ApiException {
        // La tarea puede ser visible aunque el usuario no tenga permiso de ver entregas ajenas.
      }
      if (mounted) setState(() {
        _task = task;
        _deliveries = deliveries;
      });
    } on ApiException catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _changeStatus() async {
    final task = _task;
    if (task == null) return;
    final value = await showModalBottomSheet<String>(
      context: context,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          const ListTile(title: Text('Actualizar estado')),
          for (final status in ['PENDIENTE', 'EN_PROGRESO', 'COMPLETADA', 'CANCELADA'])
            ListTile(leading: StatusChip(status: status), title: Text(Formatters.statusLabel(status)), onTap: () => Navigator.pop(context, status)),
        ]),
      ),
    );
    if (value == null) return;
    try {
      await _taskService.updateStatus(task.id!, value);
      await _load();
      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
    }
  }

  Future<void> _submitDelivery() async {
    final selected = await FilePicker.platform.pickFiles(withData: true);
    final PlatformFile? file = selected?.files.single;
    if (file == null) return;
    setState(() => _uploading = true);
    try {
      await _deliveryService.submit(widget.taskId, file);
      await _load();
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Entrega enviada correctamente.')));
    } on ApiException catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) setState(() => _uploading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final task = _task;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Detalle de tarea'),
        actions: [IconButton(tooltip: 'Cambiar estado', icon: const Icon(Icons.sync_outlined), onPressed: _changeStatus)],
      ),
      body: _loading || task == null
          ? const AppLoading()
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Row(children: [Expanded(child: Text(task.titulo, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800))), StatusChip(status: task.estado)]),
                      const SizedBox(height: 14),
                      Text(task.descripcion),
                      const SizedBox(height: 18),
                      Row(children: [const Icon(Icons.event_outlined, size: 18), const SizedBox(width: 8), Text('Fecha límite: ${Formatters.shortDate(task.fechaEntrega)}')]),
                      const SizedBox(height: 10),
                      Row(children: [const Icon(Icons.people_outline, size: 18), const SizedBox(width: 8), Text('${task.usuariosAsignados.length} personas asignadas')]),
                    ]),
                  ),
                ),
                const SizedBox(height: 16),
                GradientButton(label: 'Adjuntar entrega', icon: Icons.upload_file_outlined, loading: _uploading, onPressed: _submitDelivery),
                const SizedBox(height: 28),
                Text('Entregas', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)),
                const SizedBox(height: 10),
                if (_deliveries.isEmpty)
                  const Card(child: Padding(padding: EdgeInsets.all(18), child: Text('No hay entregas visibles todavía.')))
                else
                  ..._deliveries.map((delivery) => Card(
                        margin: const EdgeInsets.only(bottom: 8),
                        child: ListTile(
                          leading: const CircleAvatar(child: Icon(Icons.insert_drive_file_outlined)),
                          title: Text(delivery.nombreArchivo ?? 'Archivo adjunto'),
                          subtitle: Text(Formatters.shortDate(delivery.fechaEntrega)),
                          trailing: StatusChip(status: delivery.estado),
                        ),
                      )),
              ],
            ),
    );
  }
}
