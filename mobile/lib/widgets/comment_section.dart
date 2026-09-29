import 'package:flutter/material.dart';

import '../models/auth_session.dart';
import '../models/comentario.dart';
import '../services/comment_service.dart';
import '../utils/api_exception.dart';

class CommentSection extends StatefulWidget {
  const CommentSection({
    super.key,
    required this.projectId,
    required this.session,
    this.taskId,
  });

  final String projectId;
  final String? taskId;
  final AuthSession session;

  @override
  State<CommentSection> createState() => _CommentSectionState();
}

class _CommentSectionState extends State<CommentSection> {
  final _commentService = CommentService();
  final _textController = TextEditingController();
  List<Comentario> _comments = [];
  var _isLoadingComments = true;
  var _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    _loadComments();
  }

  @override
  void dispose() {
    _textController.dispose();
    super.dispose();
  }

  Future<void> _loadComments() async {
    setState(() => _isLoadingComments = true);
    try {
      final comments = await _commentService.getByProjectId(widget.projectId);
      setState(() => _comments = comments);
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error al cargar comentarios: $error')),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoadingComments = false);
    }
  }

  Future<void> _submitComment() async {
    if (_textController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('El comentario no puede estar vacío')),
      );
      return;
    }

    setState(() => _isSubmitting = true);
    try {
      final newComment = Comentario(
        contenido: _textController.text.trim(),
        idProyecto: widget.projectId,
        idUsuario: widget.session.id,
      );
      await _commentService.create(widget.projectId, newComment);
      _textController.clear();
      await _loadComments();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Comentario publicado')),
        );
      }
    } on ApiException catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
      }
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  Future<void> _deleteComment(Comentario comment) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Eliminar Comentario'),
        content: const Text('¿Deseas eliminar este comentario?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Eliminar'),
          ),
        ],
      ),
    );

    if (confirm == true) {
      try {
        await _commentService.delete(comment.id ?? '');
        await _loadComments();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Comentario eliminado')),
          );
        }
      } on ApiException catch (error) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) => Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Encabezado
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              children: [
                Icon(Icons.comment_outlined, color: Theme.of(context).colorScheme.primary),
                const SizedBox(width: 8),
                Text(
                  'Comentarios (${_comments.length})',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                ),
              ],
            ),
          ),

          // Campo de entrada
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    TextField(
                      controller: _textController,
                      maxLines: 3,
                      minLines: 2,
                      decoration: InputDecoration(
                        hintText: 'Escribe tu comentario...',
                        border: InputBorder.none,
                        contentPadding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        prefixIcon: Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: CircleAvatar(
                            radius: 16,
                            backgroundColor: Theme.of(context).colorScheme.primaryContainer,
                            child: Icon(
                              Icons.person,
                              size: 18,
                              color: Theme.of(context).colorScheme.onPrimaryContainer,
                            ),
                          ),
                        ),
                        prefixIconConstraints: const BoxConstraints(minWidth: 0, minHeight: 0),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Align(
                      alignment: Alignment.centerRight,
                      child: FilledButton.icon(
                        onPressed: _isSubmitting ? null : _submitComment,
                        icon: _isSubmitting ? const SizedBox.shrink() : const Icon(Icons.send, size: 18),
                        label: Text(_isSubmitting ? 'Publicando...' : 'Publicar'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),

          // Lista de comentarios
          Expanded(
            child: _isLoadingComments
                ? const Center(child: CircularProgressIndicator())
                : _comments.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.comment_outlined, size: 48, color: Theme.of(context).colorScheme.outline),
                            const SizedBox(height: 16),
                            Text('No hay comentarios aún',
                                style: Theme.of(context).textTheme.bodyMedium
                                    ?.copyWith(color: Theme.of(context).colorScheme.outline)),
                            const SizedBox(height: 12),
                            Text('¡Sé el primero en comentar!',
                                style: Theme.of(context).textTheme.bodySmall
                                    ?.copyWith(color: Theme.of(context).colorScheme.outline)),
                          ],
                        ),
                      )
                    : ListView.separated(
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                        itemCount: _comments.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 8),
                        itemBuilder: (context, index) {
                          final comment = _comments[index];
                          return _CommentCard(
                            comment: comment,
                            isAuthor: comment.idUsuario == widget.session.id,
                            isAdmin: widget.session.isAdmin,
                            onDelete: () => _deleteComment(comment),
                          );
                        },
                      ),
          ),
        ],
      );
}

class _CommentCard extends StatelessWidget {
  const _CommentCard({
    required this.comment,
    required this.isAuthor,
    required this.isAdmin,
    required this.onDelete,
  });

  final Comentario comment;
  final bool isAuthor;
  final bool isAdmin;
  final VoidCallback onDelete;

  String _formatDate(DateTime? date) {
    if (date == null) return '';
    final now = DateTime.now();
    final difference = now.difference(date);

    if (difference.inMinutes < 1) return 'Ahora';
    if (difference.inMinutes < 60) return 'hace ${difference.inMinutes}m';
    if (difference.inHours < 24) return 'hace ${difference.inHours}h';
    if (difference.inDays < 7) return 'hace ${difference.inDays}d';

    return '${date.day}/${date.month}/${date.year}';
  }

  @override
  Widget build(BuildContext context) => Card(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  CircleAvatar(
                    radius: 20,
                    backgroundColor: Theme.of(context).colorScheme.primaryContainer,
                    child: Icon(
                      Icons.person,
                      color: Theme.of(context).colorScheme.onPrimaryContainer,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                'Usuario',
                                style: Theme.of(context).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.bold),
                              ),
                            ),
                            if (isAuthor)
                              Chip(
                                label: const Text('Tú', style: TextStyle(fontSize: 11)),
                                padding: EdgeInsets.zero,
                                labelPadding: const EdgeInsets.symmetric(horizontal: 6),
                                backgroundColor: Theme.of(context).colorScheme.primaryContainer,
                                labelStyle:
                                    TextStyle(color: Theme.of(context).colorScheme.onPrimaryContainer, fontSize: 11),
                              ),
                          ],
                        ),
                        Text(
                          _formatDate(comment.fecha),
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                color: Theme.of(context).colorScheme.outline,
                              ),
                        ),
                      ],
                    ),
                  ),
                  if (isAuthor || isAdmin)
                    IconButton(
                      icon: const Icon(Icons.delete_outline, size: 18),
                      color: Colors.red,
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(),
                      onPressed: onDelete,
                    ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                comment.contenido,
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ],
          ),
        ),
      );
}
