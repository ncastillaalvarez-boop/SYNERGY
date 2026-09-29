import 'package:flutter/material.dart';

import '../models/auth_session.dart';
import '../models/tarea.dart';
import '../services/task_service.dart';
import '../utils/api_exception.dart';
import '../widgets/app_loading.dart';
import '../widgets/empty_state.dart';
import '../widgets/task_card.dart';
import 'task_detail_screen.dart';
import 'task_form_screen.dart';

class TasksScreen extends StatefulWidget {
  const TasksScreen({super.key, required this.session});
  final AuthSession session;

  @override
  State<TasksScreen> createState() => _TasksScreenState();
}

class _TasksScreenState extends State<TasksScreen> {
  final _service = TaskService();
  late Future<List<Tarea>> _future;

  bool get _canManage => widget.session.isLeader || widget.session.isAdmin;

  @override
  void initState() {
    super.initState();
    _future = _loadTasks();
  }

  Future<List<Tarea>> _loadTasks() => _canManage ? _service.getManaged() : _service.getMine();

  Future<void> _reload() async {
    setState(() => _future = _loadTasks());
    await _future;
  }

  Future<void> _openTask(Tarea task) async {
    final changed = await Navigator.of(context).push<bool>(MaterialPageRoute(builder: (_) => TaskDetailScreen(taskId: task.id!, session: widget.session)));
    if (changed == true) _reload();
  }

  Future<void> _newTask() async {
    final changed = await Navigator.of(context).push<bool>(MaterialPageRoute(builder: (_) => TaskFormScreen(session: widget.session)));
    if (changed == true) _reload();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text(_canManage ? 'Gestión de tareas' : 'Mis tareas')),
        floatingActionButton: _canManage ? FloatingActionButton.extended(onPressed: _newTask, icon: const Icon(Icons.add_task), label: const Text('Asignar tarea')) : null,
        body: FutureBuilder<List<Tarea>>(
          future: _future,
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done) return const AppLoading(label: 'Consultando tareas…');
            if (snapshot.hasError) {
              final message = snapshot.error is ApiException ? (snapshot.error as ApiException).message : 'No se pudieron cargar las tareas.';
              return EmptyState(icon: Icons.cloud_off_outlined, title: 'No se pudieron cargar', message: message, action: FilledButton.icon(onPressed: _reload, icon: const Icon(Icons.refresh), label: const Text('Reintentar')));
            }
            final tasks = snapshot.data ?? [];
            if (tasks.isEmpty) return EmptyState(icon: Icons.assignment_late_outlined, title: 'Todo está al día', message: _canManage ? 'Crea una tarea para comenzar a organizar el proyecto.' : 'No tienes tareas asignadas por ahora.');
            return RefreshIndicator(
              onRefresh: _reload,
              child: ListView.separated(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 96),
                itemCount: tasks.length,
                separatorBuilder: (_, __) => const SizedBox(height: 10),
                itemBuilder: (_, index) => TaskCard(task: tasks[index], onTap: () => _openTask(tasks[index])),
              ),
            );
          },
        ),
      );
}
