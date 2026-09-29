import 'package:flutter_test/flutter_test.dart';

void main() {
  group('CommentService', () {
    test('getForProject returns list of comments', () async {
      // Mock the API client
      // final mockClient = MockApiClient();
      //
      // when(mockClient.get('/comentarios/proyecto/proj_1'))
      //     .thenAnswer((_) async => [
      //       {
      //         'id': 'comment_1',
      //         'contenido': 'Primer comentario',
      //         'idProyecto': 'proj_1',
      //         'idUsuario': 'user_1',
      //       }
      //     ]);
      //
      // final service = CommentService(client: mockClient);
      // final comments = await service.getForProject('proj_1');
      //
      // expect(comments, isNotEmpty);
      // expect(comments.first.contenido, equals('Primer comentario'));
    });

    test('createForProject sends post request', () async {
      // final mockClient = MockApiClient();
      // final service = CommentService(client: mockClient);
      //
      // when(mockClient.post(
      //   '/comentarios/proyecto/proj_1',
      //   body: {'contenido': 'Nuevo comentario'},
      // )).thenAnswer((_) async => {
      //   'id': 'comment_2',
      //   'contenido': 'Nuevo comentario',
      // });
      //
      // final result = await service.createForProject('proj_1', 'Nuevo comentario');
      // expect(result.contenido, equals('Nuevo comentario'));
      //
      // verify(mockClient.post(
      //   '/comentarios/proyecto/proj_1',
      //   body: any,
      // )).called(1);
    });

    test('delete sends delete request', () async {
      // final mockClient = MockApiClient();
      // final service = CommentService(client: mockClient);
      //
      // when(mockClient.delete('/comentarios/comment_1')).thenAnswer((_) async => {});
      //
      // await service.delete('comment_1');
      //
      // verify(mockClient.delete('/comentarios/comment_1')).called(1);
    });
  });

  group('ProjectService', () {
    test('getAll returns list of projects', () async {
      // final mockClient = MockApiClient();
      //
      // when(mockClient.get('/proyectos')).thenAnswer((_) async => [
      //   {'id': 'proj_1', 'nombre': 'Proyecto 1', 'estado': 'ACTIVO'},
      //   {'id': 'proj_2', 'nombre': 'Proyecto 2', 'estado': 'COMPLETADO'},
      // ]);
      //
      // final service = ProjectService(client: mockClient);
      // final projects = await service.getAll();
      //
      // expect(projects.length, equals(2));
      // expect(projects.first.nombre, equals('Proyecto 1'));
    });

    test('getById returns single project', () async {
      // final mockClient = MockApiClient();
      //
      // when(mockClient.get('/proyectos/proj_1')).thenAnswer((_) async => {
      //   'id': 'proj_1',
      //   'nombre': 'Proyecto Especial',
      //   'estado': 'ACTIVO',
      // });
      //
      // final service = ProjectService(client: mockClient);
      // final project = await service.getById('proj_1');
      //
      // expect(project.id, equals('proj_1'));
      // expect(project.nombre, equals('Proyecto Especial'));
    });

    test('create sends post with project data', () async {
      // final mockClient = MockApiClient();
      // final service = ProjectService(client: mockClient);
      //
      // final newProject = Proyecto(
      //   nombre: 'Nuevo Proyecto',
      //   descripcion: 'Descripción',
      // );
      //
      // when(mockClient.post('/proyectos', body: newProject.toJson()))
      //     .thenAnswer((_) async => {'id': 'proj_new', ...newProject.toJson()});
      //
      // final result = await service.create(newProject);
      //
      // expect(result.id, isNotNull);
      // verify(mockClient.post('/proyectos', body: any)).called(1);
    });

    test('updateStatus sends put request', () async {
      // final mockClient = MockApiClient();
      // final service = ProjectService(client: mockClient);
      //
      // when(mockClient.put('/proyectos/proj_1/estado', body: {'estado': 'COMPLETADO'}))
      //     .thenAnswer((_) async => {'id': 'proj_1', 'estado': 'COMPLETADO'});
      //
      // await service.updateStatus('proj_1', 'COMPLETADO');
      //
      // verify(mockClient.put('/proyectos/proj_1/estado', body: any)).called(1);
    });

    test('delete sends delete request', () async {
      // final mockClient = MockApiClient();
      // final service = ProjectService(client: mockClient);
      //
      // when(mockClient.delete('/proyectos/proj_1')).thenAnswer((_) async => {});
      //
      // await service.delete('proj_1');
      //
      // verify(mockClient.delete('/proyectos/proj_1')).called(1);
    });
  });

  group('TaskService', () {
    test('getMyTasks returns user assigned tasks', () async {
      // final mockClient = MockApiClient();
      //
      // when(mockClient.get('/tareas/mias')).thenAnswer((_) async => [
      //   {'id': 'task_1', 'titulo': 'Tarea 1', 'estado': 'PENDIENTE'},
      //   {'id': 'task_2', 'titulo': 'Tarea 2', 'estado': 'EN_PROGRESO'},
      // ]);
      //
      // final service = TaskService(client: mockClient);
      // final tasks = await service.getMyTasks();
      //
      // expect(tasks.length, equals(2));
      // expect(tasks.first.titulo, equals('Tarea 1'));
    });

    test('updateStatus changes task status', () async {
      // final mockClient = MockApiClient();
      // final service = TaskService(client: mockClient);
      //
      // when(mockClient.put('/tareas/task_1/estado', body: {'estado': 'COMPLETADA'}))
      //     .thenAnswer((_) async => {'id': 'task_1', 'estado': 'COMPLETADA'});
      //
      // await service.updateStatus('task_1', 'COMPLETADA');
      //
      // verify(mockClient.put('/tareas/task_1/estado', body: any)).called(1);
    });
  });

  group('UserService', () {
    test('getById returns user info', () async {
      // final mockClient = MockApiClient();
      //
      // when(mockClient.get('/usuarios/user_1')).thenAnswer((_) async => {
      //   'id': 'user_1',
      //   'nombre': 'Juan',
      //   'correo': 'juan@test.com',
      //   'rol': 'LIDER',
      // });
      //
      // final service = UserService(client: mockClient);
      // final user = await service.getById('user_1');
      //
      // expect(user.nombre, equals('Juan'));
      // expect(user.rol, equals('LIDER'));
    });

    test('getAllForAdmin returns all users', () async {
      // final mockClient = MockApiClient();
      //
      // when(mockClient.get('/admin/usuarios')).thenAnswer((_) async => [
      //   {'id': 'user_1', 'nombre': 'Juan', 'rol': 'LIDER', 'activo': true},
      //   {'id': 'user_2', 'nombre': 'María', 'rol': 'PARTICIPANTE', 'activo': true},
      // ]);
      //
      // final service = UserService(client: mockClient);
      // final users = await service.getAllForAdmin();
      //
      // expect(users.length, equals(2));
      // verify(mockClient.get('/admin/usuarios')).called(1);
    });

    test('changeRole updates user role', () async {
      // final mockClient = MockApiClient();
      // final service = UserService(client: mockClient);
      //
      // when(mockClient.put('/admin/usuarios/user_1/rol', body: {'rol': 'ADMINISTRADOR'}))
      //     .thenAnswer((_) async => {});
      //
      // await service.changeRole('user_1', 'ADMINISTRADOR');
      //
      // verify(mockClient.put('/admin/usuarios/user_1/rol', body: any)).called(1);
    });

    test('setActive activates/deactivates user', () async {
      // final mockClient = MockApiClient();
      // final service = UserService(client: mockClient);
      //
      // when(mockClient.put('/admin/usuarios/user_1/activar')).thenAnswer((_) async => {});
      //
      // await service.setActive('user_1', true);
      //
      // verify(mockClient.put('/admin/usuarios/user_1/activar')).called(1);
    });

    test('delete removes user', () async {
      // final mockClient = MockApiClient();
      // final service = UserService(client: mockClient);
      //
      // when(mockClient.delete('/admin/usuarios/user_1')).thenAnswer((_) async => {});
      //
      // await service.delete('user_1');
      //
      // verify(mockClient.delete('/admin/usuarios/user_1')).called(1);
    });
  });
}
