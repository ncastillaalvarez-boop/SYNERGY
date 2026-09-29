import 'package:flutter/material.dart';

import '../models/proyecto.dart';
import '../services/project_service.dart';
import '../utils/api_exception.dart';
import '../utils/formatters.dart';
import '../widgets/gradient_button.dart';

class ProjectFormScreen extends StatefulWidget {
  const ProjectFormScreen({super.key, this.project});
  final Proyecto? project;

  @override
  State<ProjectFormScreen> createState() => _ProjectFormScreenState();
}

class _ProjectFormScreenState extends State<ProjectFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _description = TextEditingController();
  final _service = ProjectService();
  DateTime? _start;
  DateTime? _end;
  var _saving = false;

  @override
  void initState() {
    super.initState();
    final project = widget.project;
    _name.text = project?.nombre ?? '';
    _description.text = project?.descripcion ?? '';
    _start = project?.fechaInicio;
    _end = project?.fechaFin;
  }

  @override
  void dispose() {
    _name.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _selectDate(bool isStart) async {
    final selected = await showDatePicker(
      context: context,
      initialDate: (isStart ? _start : _end) ?? DateTime.now(),
      firstDate: DateTime(2020),
      lastDate: DateTime(2100),
    );
    if (selected != null) {
      setState(() {
        if (isStart) {
          _start = selected;
        } else {
          _end = selected;
        }
      });
    }
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    if (_start != null && _end != null && _end!.isBefore(_start!)) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('La fecha final debe ser posterior a la fecha inicial.')));
      return;
    }
    setState(() => _saving = true);
    final base = widget.project;
    final project = Proyecto(
      id: base?.id,
      nombre: _name.text.trim(),
      descripcion: _description.text.trim(),
      fechaInicio: _start,
      fechaFin: _end,
      idLider: base?.idLider,
      participantes: base?.participantes ?? const [],
      estado: base?.estado ?? 'ACTIVO',
      comentarios: base?.comentarios ?? const [],
      tareas: base?.tareas ?? const [],
      fechaCreacion: base?.fechaCreacion,
    );
    try {
      if (base == null) {
        await _service.create(project);
      } else {
        await _service.update(project);
      }
      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text(widget.project == null ? 'Nuevo proyecto' : 'Editar proyecto')),
        body: SafeArea(
          child: Form(
            key: _formKey,
            child: ListView(
              padding: const EdgeInsets.all(20),
              children: [
                TextFormField(controller: _name, decoration: const InputDecoration(labelText: 'Nombre', prefixIcon: Icon(Icons.folder_outlined)), validator: (value) => value == null || value.trim().isEmpty ? 'El nombre es obligatorio' : null),
                const SizedBox(height: 14),
                TextFormField(controller: _description, maxLines: 4, decoration: const InputDecoration(labelText: 'Descripción', alignLabelWithHint: true, prefixIcon: Icon(Icons.notes_outlined)), validator: (value) => value == null || value.trim().isEmpty ? 'Describe el proyecto' : null),
                const SizedBox(height: 14),
                ListTile(
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                  tileColor: Colors.white,
                  leading: const Icon(Icons.event_available_outlined),
                  title: const Text('Fecha de inicio'),
                  subtitle: Text(Formatters.shortDate(_start)),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => _selectDate(true),
                ),
                const SizedBox(height: 10),
                ListTile(
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                  tileColor: Colors.white,
                  leading: const Icon(Icons.event_outlined),
                  title: const Text('Fecha de finalización'),
                  subtitle: Text(Formatters.shortDate(_end)),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => _selectDate(false),
                ),
                const SizedBox(height: 28),
                GradientButton(label: 'Guardar proyecto', icon: Icons.save_outlined, loading: _saving, onPressed: _save),
              ],
            ),
          ),
        ),
      );
}
