import 'dart:async';

import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';

import '../models/proyecto.dart';
import '../models/tarea.dart';

class CacheDatabase {
  static const String dbName = 'synergy_cache.db';
  static const int version = 1;

  static final CacheDatabase _instance = CacheDatabase._internal();

  factory CacheDatabase() => _instance;

  CacheDatabase._internal();

  Database? _database;

  Future<Database> get database async {
    _database ??= await _initDatabase();
    return _database!;
  }

  Future<Database> _initDatabase() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, dbName);
    return openDatabase(
      path,
      version: version,
      onCreate: _createTables,
      onUpgrade: _upgradeTables,
    );
  }

  Future<void> _createTables(Database db, int version) async {
    // Tabla de proyectos cacheados
    await db.execute('''
      CREATE TABLE proyectos (
        id TEXT PRIMARY KEY,
        nombre TEXT NOT NULL,
        descripcion TEXT,
        fechaInicio TEXT,
        fechaFin TEXT,
        idLider TEXT,
        participantes TEXT,
        estado TEXT,
        comentarios TEXT,
        tareas TEXT,
        fechaCreacion TEXT,
        fechaActualizacion TEXT,
        cached_at INTEGER
      )
    ''');

    // Tabla de tareas cacheadas
    await db.execute('''
      CREATE TABLE tareas (
        id TEXT PRIMARY KEY,
        titulo TEXT NOT NULL,
        descripcion TEXT,
        estado TEXT,
        fechaEntrega TEXT,
        idProyecto TEXT,
        usuariosAsignados TEXT,
        entregas TEXT,
        comentarios TEXT,
        fechaCreacion TEXT,
        fechaActualizacion TEXT,
        cached_at INTEGER
      )
    ''');

    // Tabla de sincronización
    await db.execute('''
      CREATE TABLE sync_log (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        entity_type TEXT NOT NULL,
        entity_id TEXT NOT NULL,
        action TEXT NOT NULL,
        status TEXT,
        created_at INTEGER,
        synced_at INTEGER
      )
    ''');
  }

  Future<void> _upgradeTables(Database db, int oldVersion, int newVersion) async {
    // Aquí manejar migraciones futuras si es necesario
  }

  // ==================== PROYECTOS ====================

  Future<void> cacheProyectos(List<Proyecto> proyectos) async {
    final db = await database;
    final batch = db.batch();
    for (final proyecto in proyectos) {
      batch.insert(
        'proyectos',
        _proyectoToMap(proyecto),
        conflictAlgorithm: ConflictAlgorithm.replace,
      );
    }
    await batch.commit(noResult: true);
  }

  Future<void> cacheProyecto(Proyecto proyecto) async {
    final db = await database;
    await db.insert(
      'proyectos',
      _proyectoToMap(proyecto),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<List<Proyecto>> getCachedProyectos() async {
    final db = await database;
    final maps = await db.query('proyectos', orderBy: 'cached_at DESC');
    return maps.map((m) => _mapToProyecto(m)).toList();
  }

  Future<Proyecto?> getCachedProyecto(String id) async {
    final db = await database;
    final maps = await db.query('proyectos', where: 'id = ?', whereArgs: [id]);
    if (maps.isEmpty) return null;
    return _mapToProyecto(maps.first);
  }

  Future<void> deleteProyectoCache(String id) async {
    final db = await database;
    await db.delete('proyectos', where: 'id = ?', whereArgs: [id]);
  }

  // ==================== TAREAS ====================

  Future<void> cacheTareas(List<Tarea> tareas) async {
    final db = await database;
    final batch = db.batch();
    for (final tarea in tareas) {
      batch.insert(
        'tareas',
        _tareaToMap(tarea),
        conflictAlgorithm: ConflictAlgorithm.replace,
      );
    }
    await batch.commit(noResult: true);
  }

  Future<void> cacheTarea(Tarea tarea) async {
    final db = await database;
    await db.insert(
      'tareas',
      _tareaToMap(tarea),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<List<Tarea>> getCachedTareas() async {
    final db = await database;
    final maps = await db.query('tareas', orderBy: 'cached_at DESC');
    return maps.map((m) => _mapToTarea(m)).toList();
  }

  Future<Tarea?> getCachedTarea(String id) async {
    final db = await database;
    final maps = await db.query('tareas', where: 'id = ?', whereArgs: [id]);
    if (maps.isEmpty) return null;
    return _mapToTarea(maps.first);
  }

  Future<void> deleteTareaCache(String id) async {
    final db = await database;
    await db.delete('tareas', where: 'id = ?', whereArgs: [id]);
  }

  // ==================== SYNC LOG ====================

  Future<void> logSync(String entityType, String entityId, String action) async {
    final db = await database;
    await db.insert('sync_log', {
      'entity_type': entityType,
      'entity_id': entityId,
      'action': action,
      'status': 'pending',
      'created_at': DateTime.now().millisecondsSinceEpoch,
    });
  }

  Future<List<Map<String, dynamic>>> getPendingSyncs() async {
    final db = await database;
    return db.query('sync_log', where: 'status = ?', whereArgs: ['pending']);
  }

  Future<void> markSyncAsComplete(int logId) async {
    final db = await database;
    await db.update(
      'sync_log',
      {'status': 'synced', 'synced_at': DateTime.now().millisecondsSinceEpoch},
      where: 'id = ?',
      whereArgs: [logId],
    );
  }

  Future<void> clearCache() async {
    final db = await database;
    await db.delete('proyectos');
    await db.delete('tareas');
  }

  // ==================== HELPERS ====================

  Map<String, dynamic> _proyectoToMap(Proyecto p) => {
        'id': p.id,
        'nombre': p.nombre,
        'descripcion': p.descripcion,
        'fechaInicio': p.fechaInicio?.toString(),
        'fechaFin': p.fechaFin?.toString(),
        'idLider': p.idLider,
        'participantes': p.participantes.join(','),
        'estado': p.estado,
        'comentarios': p.comentarios.join(','),
        'tareas': p.tareas.join(','),
        'fechaCreacion': p.fechaCreacion?.toIso8601String(),
        'fechaActualizacion': p.fechaActualizacion?.toIso8601String(),
        'cached_at': DateTime.now().millisecondsSinceEpoch,
      };

  Proyecto _mapToProyecto(Map<String, dynamic> map) => Proyecto(
        id: map['id'] as String?,
        nombre: map['nombre'] as String? ?? '',
        descripcion: map['descripcion'] as String? ?? '',
        fechaInicio: map['fechaInicio'] != null ? DateTime.tryParse(map['fechaInicio'] as String) : null,
        fechaFin: map['fechaFin'] != null ? DateTime.tryParse(map['fechaFin'] as String) : null,
        idLider: map['idLider'] as String?,
        participantes: (map['participantes'] as String?)?.split(',').where((e) => e.isNotEmpty).toList() ?? [],
        estado: map['estado'] as String? ?? 'ACTIVO',
        comentarios: (map['comentarios'] as String?)?.split(',').where((e) => e.isNotEmpty).toList() ?? [],
        tareas: (map['tareas'] as String?)?.split(',').where((e) => e.isNotEmpty).toList() ?? [],
        fechaCreacion: map['fechaCreacion'] != null ? DateTime.tryParse(map['fechaCreacion'] as String) : null,
        fechaActualizacion: map['fechaActualizacion'] != null ? DateTime.tryParse(map['fechaActualizacion'] as String) : null,
      );

  Map<String, dynamic> _tareaToMap(Tarea t) => {
        'id': t.id,
        'titulo': t.titulo,
        'descripcion': t.descripcion,
        'estado': t.estado,
        'fechaEntrega': t.fechaEntrega?.toString(),
        'idProyecto': t.idProyecto,
        'usuariosAsignados': t.usuariosAsignados.join(','),
        'entregas': t.entregas.join(','),
        'comentarios': t.comentarios.join(','),
        'fechaCreacion': t.fechaCreacion?.toIso8601String(),
        'fechaActualizacion': t.fechaActualizacion?.toIso8601String(),
        'cached_at': DateTime.now().millisecondsSinceEpoch,
      };

  Tarea _mapToTarea(Map<String, dynamic> map) => Tarea(
        id: map['id'] as String?,
        titulo: map['titulo'] as String? ?? '',
        descripcion: map['descripcion'] as String? ?? '',
        estado: map['estado'] as String? ?? 'PENDIENTE',
        fechaEntrega: map['fechaEntrega'] != null ? DateTime.tryParse(map['fechaEntrega'] as String) : null,
        idProyecto: map['idProyecto'] as String? ?? '',
        usuariosAsignados: (map['usuariosAsignados'] as String?)?.split(',').where((e) => e.isNotEmpty).toList() ?? [],
        entregas: (map['entregas'] as String?)?.split(',').where((e) => e.isNotEmpty).toList() ?? [],
        comentarios: (map['comentarios'] as String?)?.split(',').where((e) => e.isNotEmpty).toList() ?? [],
        fechaCreacion: map['fechaCreacion'] != null ? DateTime.tryParse(map['fechaCreacion'] as String) : null,
        fechaActualizacion: map['fechaActualizacion'] != null ? DateTime.tryParse(map['fechaActualizacion'] as String) : null,
      );
}
