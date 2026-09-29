import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

import '../models/auth_session.dart';
import '../models/entrega_tarea.dart';
import '../services/delivery_service.dart';

class DeliverySection extends StatefulWidget {
  const DeliverySection({
    super.key,
    required this.taskId,
    required this.session,
    this.isAssigned = false,
    this.taskStatus = 'PENDIENTE',
  });

  final String taskId;
  final AuthSession session;
  final bool isAssigned;
  final String taskStatus;

  @override
  State<DeliverySection> createState() => _DeliverySectionState();
}

class _DeliverySectionState extends State<DeliverySection> {
  final _deliveryService = DeliveryService();
  List<EntregaTarea> _deliveries = [];
  var _isLoadingDeliveries = true;
  var _isUploading = false;

  @override
  void initState() {
    super.initState();
    _loadDeliveries();
  }

  Future<void> _loadDeliveries() async {
    setState(() => _isLoadingDeliveries = true);
    try {
      final deliveries = await _deliveryService.getForTask(widget.taskId);
      setState(() => _deliveries = deliveries);
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error al cargar entregas: $error')),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoadingDeliveries = false);
    }
  }

  Future<void> _pickAndUploadFile() async {
    try {
      final result = await FilePicker.platform.pickFiles();
      if (result == null || result.files.isEmpty) return;

      final file = result.files.first;
      final maxSize = 10 * 1024 * 1024; // 10 MB

      if (file.size > maxSize) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('El archivo excede el tamaño máximo de 10 MB')),
          );
        }
        return;
      }

      setState(() => _isUploading = true);
      await _deliveryService.submit(widget.taskId, file);
      await _loadDeliveries();

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Archivo entregado exitosamente')),
        );
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error al subir archivo: $error')),
        );
      }
    } finally {
      if (mounted) setState(() => _isUploading = false);
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
                Icon(Icons.file_upload_outlined, color: Theme.of(context).colorScheme.primary),
                const SizedBox(width: 8),
                Text(
                  'Entregas (${_deliveries.length})',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                ),
              ],
            ),
          ),

          // Botón de subida
          if (widget.isAssigned && widget.taskStatus != 'COMPLETADA' && widget.taskStatus != 'CANCELADA')
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: FilledButton.icon(
                onPressed: _isUploading ? null : _pickAndUploadFile,
                icon: _isUploading ? const SizedBox.shrink() : const Icon(Icons.upload_file),
                label: Text(_isUploading ? 'Subiendo...' : 'Entregar Archivo'),
              ),
            ),

          // Lista de entregas
          Expanded(
            child: _isLoadingDeliveries
                ? const Center(child: CircularProgressIndicator())
                : _deliveries.isEmpty
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.file_upload_outlined,
                                  size: 48, color: Theme.of(context).colorScheme.outline),
                              const SizedBox(height: 16),
                              Text('Sin entregas aún',
                                  style: Theme.of(context).textTheme.bodyMedium
                                      ?.copyWith(color: Theme.of(context).colorScheme.outline)),
                              if (widget.isAssigned) ...[
                                const SizedBox(height: 8),
                                Text('Sube un archivo para completar la tarea',
                                    style: Theme.of(context).textTheme.bodySmall
                                        ?.copyWith(color: Theme.of(context).colorScheme.outline)),
                              ],
                            ],
                          ),
                        ),
                      )
                    : ListView.separated(
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                        itemCount: _deliveries.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 8),
                        itemBuilder: (context, index) {
                          final delivery = _deliveries[index];
                          return _DeliveryCard(delivery: delivery);
                        },
                      ),
          ),
        ],
      );
}

class _DeliveryCard extends StatelessWidget {
  const _DeliveryCard({required this.delivery});

  final EntregaTarea delivery;

  String _formatDate(DateTime? date) {
    if (date == null) return '';
    return '${date.day}/${date.month}/${date.year} ${date.hour}:${date.minute.toString().padLeft(2, '0')}';
  }

  String _getFileIcon(String? fileType) {
    if (fileType == null) return '📄';
    if (fileType.contains('pdf')) return '📕';
    if (fileType.contains('image')) return '🖼️';
    if (fileType.contains('word') || fileType.contains('document')) return '📝';
    if (fileType.contains('sheet') || fileType.contains('excel')) return '📊';
    if (fileType.contains('zip') || fileType.contains('rar')) return '📦';
    return '📄';
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
                  Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.primaryContainer,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Center(
                      child: Text(_getFileIcon(delivery.tipoArchivo), style: const TextStyle(fontSize: 20)),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          delivery.nombreArchivo ?? 'Sin nombre',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w600),
                        ),
                        Text(
                          _formatDate(delivery.fechaEntrega),
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                color: Theme.of(context).colorScheme.outline,
                              ),
                        ),
                      ],
                    ),
                  ),
                  if (delivery.estado == 'ENTREGADO')
                    Chip(
                      label: const Text('Entregado', style: TextStyle(fontSize: 11, color: Colors.white)),
                      backgroundColor: Colors.green,
                    ),
                ],
              ),
              if (delivery.comentariosRevisor != null) ...[
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.primaryContainer.withValues(alpha: 0.3),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Comentarios del revisor',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        delivery.comentariosRevisor!,
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                  ),
                ),
              ],
              if (delivery.calificacion != null) ...[
                const SizedBox(height: 8),
                Row(
                  children: [
                    Icon(Icons.grade, size: 16, color: Theme.of(context).colorScheme.primary),
                    const SizedBox(width: 6),
                    Text(
                      'Calificación: ${delivery.calificacion}',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(fontWeight: FontWeight.w600),
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      );
}
