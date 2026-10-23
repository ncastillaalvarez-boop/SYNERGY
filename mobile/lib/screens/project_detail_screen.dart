import 'package:flutter/material.dart';

import '../models/auth_session.dart';
import '../models/comentario.dart';
import '../models/proyecto.dart';
import '../services/comment_service.dart';
import '../services/participation_service.dart';
import '../services/project_service.dart';
import '../utils/api_exception.dart';
import '../utils/formatters.dart';
import '../widgets/app_loading.dart';
import '../widgets/gradient_button.dart';
import '../widgets/status_chip.dart';
import 'project_form_screen.dart';

class ProjectDetailScreen extends StatefulWidget {
  const ProjectDetailScreen({super.key, required this.projectId, required this.session});
  final String projectId;
  final AuthSession session;

  @override
  State<ProjectDetailScreen> createState() => _ProjectDetailScreenState();
}

class _ProjectDetailScreenState extends State<ProjectDetailScreen> {
  final _projectService = ProjectService();
  final _participationService = ParticipationService();
  final _commentService = CommentService();
  final _comment = TextEditingController();
  Proyecto? _project;
  Map<String, dynamic>? _risk;
  List<Comentario> _comments = const [];
  bool _enrolled = false;
  bool _loading = true;
  bool _savingComment = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _comment.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final values = await Future.wait([
        _projectService.getById(widget.projectId),
        _commentService.getForProject(widget.projectId),
        _participationService.isEnrolled(widget.projectId),
      ]);
      if (!mounted) return;
      setState(() {
        _project = values[0] as Proyecto;
        _comments = values[1] as List<Comentario>;
        _enrolled = values[2] as bool;
      });
      if (_canManage) _loadRisk();
    } on ApiException catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  /// El riesgo solo lo ven el líder del proyecto y el administrador; si falla no se muestra.
  Future<void> _loadRisk() async {
    try {
      final risk = await _projectService.getRisk(widget.projectId);
      if (mounted) setState(() => _risk = risk);
    } catch (_) {
      if (mounted) setState(() => _risk = null);
    }
  }

  bool get _canManage => _project != null && (widget.session.isAdmin || _project!.idLider == widget.session.id);

  Future<void> _toggleParticipation() async {
    try {
      if (_enrolled) {
        await _participationService.leave(widget.projectId);
      } else {
        await _participationService.join(widget.projectId);
      }
      await _load();
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(_enrolled ? 'Te uniste al proyecto.' : 'Ya no participas en este proyecto.')));
    } on ApiException catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
    }
  }

  Future<void> _saveComment() async {
    final content = _comment.text.trim();
    if (content.isEmpty) return;
    setState(() => _savingComment = true);
    try {
      await _commentService.createForProject(widget.projectId, content);
      _comment.clear();
      await _load();
    } on ApiException catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
    } finally {
      if (mounted) setState(() => _savingComment = false);
    }
  }

  Future<void> _edit() async {
    final changed = await Navigator.of(context).push<bool>(MaterialPageRoute(builder: (_) => ProjectFormScreen(project: _project)));
    if (changed == true) {
      await _load();
      if (mounted) Navigator.of(context).pop(true);
    }
  }

  Future<void> _deleteProject() async {
    final accepted = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        icon: const Icon(Icons.delete_forever_outlined),
        title: const Text('¿Eliminar proyecto?'),
        content: const Text('Esta acción no se puede deshacer.'),
        actions: [TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')), FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Eliminar'))],
      ),
    );
    if (accepted != true) return;
    try {
      await _projectService.delete(widget.projectId);
      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final project = _project;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Detalle del proyecto'),
        actions: [
          if (_canManage) IconButton(tooltip: 'Editar', icon: const Icon(Icons.edit_outlined), onPressed: _edit),
          if (_canManage) IconButton(tooltip: 'Eliminar', icon: const Icon(Icons.delete_outline), onPressed: _deleteProject),
        ],
      ),
      body: _loading || project == null
          ? const AppLoading()
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
                children: [
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(20),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Row(children: [Expanded(child: Text(project.nombre, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800))), StatusChip(status: project.estado)]),
                        const SizedBox(height: 14),
                        Text(project.descripcion),
                        const SizedBox(height: 18),
                        Wrap(spacing: 20, runSpacing: 10, children: [
                          _DetailItem(icon: Icons.group_outlined, label: '${project.participantes.length} integrantes'),
                          _DetailItem(icon: Icons.event_outlined, label: 'Finaliza ${Formatters.shortDate(project.fechaFin)}'),
                          _DetailItem(icon: Icons.task_alt_outlined, label: '${project.tareas.length} tareas'),
                        ]),
                      ]),
                    ),
                  ),
                  if (_risk != null && project.estado == 'ACTIVO') ...[
                    const SizedBox(height: 12),
                    _RiskCard(risk: _risk!),
                  ],
                  const SizedBox(height: 16),
                  if (project.idLider != widget.session.id)
                    GradientButton(label: _enrolled ? 'Salir del proyecto' : 'Participar ahora', icon: _enrolled ? Icons.exit_to_app : Icons.handshake_outlined, onPressed: _toggleParticipation),
                  const SizedBox(height: 28),
                  Text('Conversación', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)),
                  const SizedBox(height: 10),
                  TextField(
                    controller: _comment,
                    minLines: 1,
                    maxLines: 3,
                    decoration: InputDecoration(
                      hintText: 'Escribe un comentario…',
                      prefixIcon: const Icon(Icons.chat_bubble_outline),
                      suffixIcon: IconButton(tooltip: 'Enviar comentario', icon: _savingComment ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.send_outlined), onPressed: _savingComment ? null : _saveComment),
                    ),
                  ),
                  const SizedBox(height: 12),
                  if (_comments.isEmpty)
                    const Padding(padding: EdgeInsets.all(20), child: Text('Sé la primera persona en comentar.'))
                  else
                    ..._comments.map((comment) => _CommentTile(
                          comment: comment,
                          canDelete: widget.session.isAdmin || comment.idUsuario == widget.session.id,
                          onDelete: () async {
                            await _commentService.delete(comment.id!);
                            await _load();
                          },
                        )),
                ],
              ),
            ),
    );
  }
}

class _DetailItem extends StatelessWidget {
  const _DetailItem({required this.icon, required this.label});
  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) => Row(mainAxisSize: MainAxisSize.min, children: [Icon(icon, size: 18), const SizedBox(width: 6), Text(label)]);
}

class _CommentTile extends StatelessWidget {
  const _CommentTile({required this.comment, required this.canDelete, required this.onDelete});
  final Comentario comment;
  final bool canDelete;
  final Future<void> Function() onDelete;

  @override
  Widget build(BuildContext context) => Card(
        margin: const EdgeInsets.only(bottom: 8),
        child: ListTile(
          leading: const CircleAvatar(child: Icon(Icons.person_outline)),
          title: Text(comment.contenido),
          subtitle: Text(Formatters.shortDate(comment.fecha)),
          trailing: canDelete ? IconButton(tooltip: 'Eliminar comentario', icon: const Icon(Icons.delete_outline), onPressed: () => onDelete()) : null,
        ),
      );
}

class _RiskCard extends StatelessWidget {
  const _RiskCard({required this.risk});
  final Map<String, dynamic> risk;

  Color _color(String? nivel) => switch (nivel) {
        'Alto' => const Color(0xFFDC2626),
        'Medio' => const Color(0xFFD97706),
        'Bajo' => const Color(0xFF059669),
        _ => Colors.blueGrey,
      };

  @override
  Widget build(BuildContext context) {
    final nivel = risk['nivel']?.toString();
    final motivos = (risk['motivos'] as List? ?? const []).map((m) => m.toString()).toList();
    final probabilidad = risk['probabilidad'] is num ? ((risk['probabilidad'] as num) * 100).round() : null;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Icon(Icons.traffic_outlined, color: _color(nivel)),
            const SizedBox(width: 8),
            Expanded(child: Text('Riesgo: ${nivel ?? 'sin datos'}${probabilidad != null ? ' ($probabilidad %)' : ''}',
                style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800, color: _color(nivel)))),
          ]),
          const SizedBox(height: 8),
          ...motivos.map((m) => Padding(padding: const EdgeInsets.only(bottom: 4), child: Text('• $m'))),
          if (risk['sugerencia'] != null) ...[
            const SizedBox(height: 6),
            Text('Sugerencia: ${risk['sugerencia']}', style: const TextStyle(fontStyle: FontStyle.italic)),
          ],
        ]),
      ),
    );
  }
}
