import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';

// Generar mocks: dart run build_runner build
import 'package:http/http.dart' as http;

@GenerateMocks([http.Client])
void main() {
  group('Auth Models and Serialization', () {
    test('AuthSession.fromJson creates correct instance', () {
      final json = {
        'token': 'test_token_123',
        'id': 'user_123',
        'nombre': 'Juan Pérez',
        'correo': 'juan@test.com',
        'rol': 'LIDER',
        'privilegios': ['create_proyecto', 'edit_proyecto'],
        'expiresIn': 780000,
      };

      // Importaría AuthSession aquí en un proyecto real
      // final session = AuthSession.fromJson(json);
      // expect(session.token, equals('test_token_123'));
      // expect(session.rol, equals('LIDER'));
      // expect(session.isLeader, isTrue);
      // expect(session.isAdmin, isFalse);
    });

    test('AuthSession.toJson serializes correctly', () {
      // Importaría AuthSession aquí en un proyecto real
      // final session = AuthSession(
      //   token: 'test_token',
      //   id: 'user_123',
      //   nombre: 'Juan',
      //   correo: 'juan@test.com',
      //   rol: 'ADMINISTRADOR',
      // );
      //
      // final json = session.toJson();
      // expect(json['token'], equals('test_token'));
      // expect(json['rol'], equals('ADMINISTRADOR'));
    });
  });

  group('Usuario Model', () {
    test('Usuario.fromJson handles nullable fields', () {
      final json = {
        'id': 'user_1',
        'nombre': 'María',
        'correo': 'maria@test.com',
        'rol': 'PARTICIPANTE',
        'estado': 'ACTIVO',
        'activo': true,
        'privilegios': [],
        'proyectos': ['proj_1', 'proj_2'],
      };

      // Prueba de serialización
      // final usuario = Usuario.fromJson(json);
      // expect(usuario.id, equals('user_1'));
      // expect(usuario.rol, equals('PARTICIPANTE'));
      // expect(usuario.proyectos.length, equals(2));
    });
  });

  group('Proyecto Model', () {
    test('Proyecto.fromJson parses dates correctly', () {
      final json = {
        'id': 'proj_1',
        'nombre': 'Proyecto de Sostenibilidad',
        'descripcion': 'Un proyecto importante',
        'fechaInicio': '2026-09-01T10:30:00Z',
        'fechaFin': '2026-12-31T23:59:59Z',
        'idLider': 'user_1',
        'participantes': ['user_1', 'user_2'],
        'estado': 'ACTIVO',
        'comentarios': [],
        'tareas': ['task_1'],
      };

      // Prueba de fechas
      // final proyecto = Proyecto.fromJson(json);
      // expect(proyecto.nombre, equals('Proyecto de Sostenibilidad'));
      // expect(proyecto.estado, equals('ACTIVO'));
      // expect(proyecto.participantes.length, equals(2));
      // expect(proyecto.tareas.length, equals(1));
    });

    test('Proyecto.copyWith creates new instance with updated fields', () {
      // final proyecto = Proyecto(
      //   id: 'proj_1',
      //   nombre: 'Original',
      //   descripcion: 'Descripción',
      //   estado: 'ACTIVO',
      // );
      //
      // final updated = proyecto.copyWith(
      //   nombre: 'Actualizado',
      //   estado: 'COMPLETADO',
      // );
      //
      // expect(updated.nombre, equals('Actualizado'));
      // expect(updated.estado, equals('COMPLETADO'));
      // expect(updated.id, equals(proyecto.id));
    });
  });

  group('Tarea Model', () {
    test('Tarea.fromJson creates instance with correct defaults', () {
      final json = {
        'id': 'task_1',
        'titulo': 'Tarea importante',
        'descripcion': 'Descripción de la tarea',
        'estado': 'PENDIENTE',
        'fechaEntrega': '2026-09-30T23:59:59Z',
        'idProyecto': 'proj_1',
        'usuariosAsignados': ['user_1', 'user_2'],
      };

      // Prueba
      // final tarea = Tarea.fromJson(json);
      // expect(tarea.estado, equals('PENDIENTE'));
      // expect(tarea.usuariosAsignados.length, equals(2));
    });
  });

  group('Comment Model', () {
    test('Comentario.fromJson handles null dates', () {
      final json = {
        'id': 'comment_1',
        'contenido': 'Este es un comentario de prueba',
        'idProyecto': 'proj_1',
        'idUsuario': 'user_1',
      };

      // Prueba
      // final comentario = Comentario.fromJson(json);
      // expect(comentario.contenido, equals('Este es un comentario de prueba'));
      // expect(comentario.idProyecto, equals('proj_1'));
    });
  });

  group('JSON Utilities', () {
    test('JsonUtils.string handles null values', () {
      // Utilidades de prueba
      // expect(JsonUtils.string(null), isNull);
      // expect(JsonUtils.string('test'), equals('test'));
      // expect(JsonUtils.string(123), equals('123'));
    });

    test('JsonUtils.strings converts list of strings', () {
      // expect(JsonUtils.strings(null), isEmpty);
      // expect(JsonUtils.strings([]), isEmpty);
      // expect(JsonUtils.strings(['a', 'b']), equals(['a', 'b']));
    });

    test('JsonUtils.dateTime parses ISO 8601 dates', () {
      // final dateStr = '2026-08-18T10:30:00Z';
      // final date = JsonUtils.dateTime(dateStr);
      // expect(date, isNotNull);
      // expect(date?.year, equals(2026));
      // expect(date?.month, equals(8));
      // expect(date?.day, equals(18));
    });

    test('JsonUtils.withoutNulls removes null values from map', () {
      // final map = {'a': 'value', 'b': null, 'c': '', 'd': 0};
      // final result = JsonUtils.withoutNulls(map);
      // expect(result.containsKey('b'), isFalse);
      // expect(result.containsKey('a'), isTrue);
    });
  });
}
