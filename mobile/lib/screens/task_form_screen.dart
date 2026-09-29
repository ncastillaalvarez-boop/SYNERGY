import 'package:flutter/material.dart';

import '../models/auth_session.dart';
import '../models/participacion.dart';
import '../models/proyecto.dart';
import '../models/tarea.dart';
import '../services/participation_service.dart';
import '../services/project_service.dart';
import '../services/task_service.dart';
import '../utils/api_exception.dart';
import '../utils/formatters.dart';
import '../widgets/app_loading.dart';
import '../widgets/gradient_button.dart';

class TaskFormScreen extends StatefulWidget {
  const TaskFormScreen({super.key, required this.session});
  final AuthSession session;

  @override
  State<TaskFormScreen> createState() => _TaskFormScreenState();
}

class _TaskFormScreenState extends State<TaskFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _title = TextEditingController();
  final _description = TextEditingController();
  final _projectService = ProjectService();
  final _participationService = ParticipationService();
  final _taskService = TaskService();
  late Future<List<Proyecto>> _projectsFuture;
  List<Participacion> _participants = const [];
  final Set<String> _assignees = {};
  String? _projectId;
  DateTime? _dueDate;
  bool _saving = false;
  bool _loadingParticipants = false;

  @override
  void initState() {
    super.initState();
    _projectsFuture = widget.session.isAdmin ? _projectService.getAll() : _projectService.getMyLeadershipProjects();
  }

  @override
  void dispose() {
    _title.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _changeProject(String? id) async {
    setState(() {
      _projectId = id;
      _participants = const [];
      _assignees.clear();
      _loadingParticipants = id != null;
    });
    if (id == null) return;
    try {
      final participants = await _participationService.getParticipants(id);
      if (mounted) setState(() => _participants = participants);
    } on ApiException catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) setState(() => _loadingParticipants = false);
    }
  }

  Future<void> _pickDate() async {
    final value = await showDatePicker(context: context, initialDate: _dueDate ?? DateTime.now(), firstDate: DateTime.now().subtract(const Duration(days: 1)), lastDate: DateTime(2100));
    if (value != null) setState(() => _dueDate = value);
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate() || _projectId == null) {
      if (_projectId == null) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Selecciona un proyecto.')));
      return;
    }
    setState(() => _saving = true);
    try {
      await _taskService.create(Tarea(
        titulo: _title.text.trim(),
        descripcion: _description.text.trim(),
        idProyecto: _projectId!,
        fechaEntrega: _dueDate,
        usuariosAsignados: _assignees.toList(),
      ));
      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Asignar tarea')),
        body: FutureBuilder<List<Proyecto>>(
          future: _projectsFuture,
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done) return const AppLoading();
            final projects = snapshot.data ?? [];
            return Form(
              key: _formKey,
              child: ListView(
                padding: const EdgeInsets.all(20),
                children: [
                  DropdownButtonFormField<String>(
                    value: _projectId,
                    isExpanded: true,
                    decoration: const InputDecoration(labelText: 'Proyecto', prefixIcon: Icon(Icons.folder_outlined)),
                    items: projects.map((project) => DropdownMenuItem(value: project.id, child: Text(project.nombre, overflow: TextOverflow.ellipsis))).toList(),
                    onChanged: _changeProject,
                    validator: (value) => value == null ? 'Selecciona un proyecto' : null,
                  ),
                  const SizedBox(height: 14),
                  TextFormField(controller: _title, decoration: const InputDecoration(labelText: 'Título de la tarea', prefixIcon: Icon(Icons.assignment_outlined)), validator: (value) => value == null || value.trim().isEmpty ? 'El título es obligatorio' : null),
                  const SizedBox(height: 14),
                  TextFormField(controller: _description, maxLines: 4, decoration: const InputDecoration(labelText: 'Descripción', alignLabelWithHint: true, prefixIcon: Icon(Icons.notes_outlined)), validator: (value) => value == null || value.trim().isEmpty ? 'Describe la tarea' : null),
                  const SizedBox(height: 14),
                  ListTile(
                    tileColor: Colors.white,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    leading: const Icon(Icons.event_outlined),
                    title: const Text('Fecha de entrega'),
                    subtitle: Text(Formatters.shortDate(_dueDate)),
                    onTap: _pickDate,
                  ),
                  const SizedBox(height: 22),
                  Text('Asignar a', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800)),
                  const SizedBox(height: 6),
                  if (_loadingParticipants) const Padding(padding: EdgeInsets.all(12), child: LinearProgressIndicator())
                  else if (_projectId == null) const Text('Selecciona un proyecto para ver sus integrantes.')
                  else if (_participants.isEmpty) const Text('Este proyecto aún no tiene participantes registrados.')
                  else Wrap(
                    spacing: 8,
                    runSpacing: 4,
                    children: _participants.map((participant) {
                      final selected = _assignees.contains(participant.idUsuario);
                      return FilterChip(
                        label: Text(participant.idUsuario.length > 8 ? '${participant.idUsuario.substring(0, 8)}…' : participant.idUsuario),
                        selected: selected,
                        avatar: const Icon(Icons.person_outline, size: 18),
                        onSelected: (value) => setState(() => value ? _assignees.add(participant.idUsuario) : _assignees.remove(participant.idUsuario)),
                      );
                    }).toList(),
                  ),
                  const SizedBox(height: 28),
                  GradientButton(label: 'Crear y asignar', icon: Icons.add_task, loading: _saving, onPressed: _save),
                ],
              ),
            );
          },
        ),
      );
}
