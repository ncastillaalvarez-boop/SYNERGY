import 'package:flutter/material.dart';

import '../models/auth_session.dart';
import '../models/proyecto.dart';
import '../services/project_service.dart';
import '../utils/api_exception.dart';
import '../widgets/app_loading.dart';
import '../widgets/empty_state.dart';
import '../widgets/project_card.dart';
import 'project_detail_screen.dart';
import 'project_form_screen.dart';

class ProjectsScreen extends StatefulWidget {
  const ProjectsScreen({super.key, required this.session});
  final AuthSession session;

  @override
  State<ProjectsScreen> createState() => _ProjectsScreenState();
}

class _ProjectsScreenState extends State<ProjectsScreen> {
  final _service = ProjectService();
  late Future<List<Proyecto>> _future;
  var _query = '';

  @override
  void initState() {
    super.initState();
    _future = _service.getAll();
  }

  Future<void> _reload() async {
    // setState no puede recibir una función que devuelva un Future
    setState(() {
      _future = _service.getAll();
    });
    try {
      await _future;
    } catch (_) {
      // el FutureBuilder ya muestra el error ("Sin conexión" + Reintentar)
    }
  }

  Future<void> _openForm([Proyecto? project]) async {
    final changed = await Navigator.of(context).push<bool>(MaterialPageRoute(
      builder: (_) => ProjectFormScreen(project: project),
    ));
    if (changed == true) _reload();
  }

  Future<void> _openDetail(Proyecto project) async {
    final changed = await Navigator.of(context).push<bool>(MaterialPageRoute(
      builder: (_) => ProjectDetailScreen(projectId: project.id!, session: widget.session),
    ));
    if (changed == true) _reload();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(
          title: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('Proyectos'),
            Text('Hola, ${widget.session.nombre}', style: Theme.of(context).textTheme.labelMedium),
          ]),
        ),
        floatingActionButton: (widget.session.isLeader || widget.session.isAdmin)
            ? FloatingActionButton.extended(
                onPressed: _openForm,
                icon: const Icon(Icons.add_circle_outline),
                label: const Text('Nuevo proyecto'),
              )
            : null,
        body: FutureBuilder<List<Proyecto>>(
          future: _future,
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done) return const AppLoading();
            if (snapshot.hasError) {
              final message = snapshot.error is ApiException ? (snapshot.error as ApiException).message : 'No se pudieron cargar los proyectos.';
              return EmptyState(icon: Icons.cloud_off_outlined, title: 'Sin conexión', message: message, action: FilledButton.icon(onPressed: _reload, icon: const Icon(Icons.refresh), label: const Text('Reintentar')));
            }
            final projects = snapshot.data ?? [];
            final visible = projects.where((project) {
              final normalized = _query.toLowerCase();
              return project.nombre.toLowerCase().contains(normalized) || project.descripcion.toLowerCase().contains(normalized);
            }).toList();
            return RefreshIndicator(
              onRefresh: _reload,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 96),
                children: [
                  TextField(
                    onChanged: (value) => setState(() => _query = value),
                    decoration: const InputDecoration(prefixIcon: Icon(Icons.search), hintText: 'Buscar por nombre o tema'),
                  ),
                  const SizedBox(height: 18),
                  if (visible.isEmpty)
                    SizedBox(height: 360, child: EmptyState(icon: Icons.folder_open_outlined, title: 'No hay proyectos', message: _query.isEmpty ? 'Cuando haya iniciativas aparecerán aquí.' : 'Prueba con otro término de búsqueda.'))
                  else
                    ...visible.map((project) => Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: ProjectCard(project: project, onTap: () => _openDetail(project)),
                        )),
                ],
              ),
            );
          },
        ),
      );
}
