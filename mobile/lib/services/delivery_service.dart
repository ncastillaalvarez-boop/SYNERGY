import 'package:file_picker/file_picker.dart';

import '../models/entrega_tarea.dart';
import 'api_client.dart';

class DeliveryService {
  DeliveryService({ApiClient? client}) : _client = client ?? ApiClient();
  final ApiClient _client;

  Future<List<EntregaTarea>> getForTask(String taskId) async =>
      (await _client.get('/entregas/tarea/$taskId') as List)
          .map((item) => EntregaTarea.fromJson(Map<String, dynamic>.from(item as Map)))
          .toList();

  Future<void> submit(String taskId, PlatformFile file) async {
    await _client.upload(
      '/entregas/tarea/$taskId',
      field: 'archivo',
      filename: file.name,
      bytes: file.bytes,
      filePath: file.path,
    );
  }
}
