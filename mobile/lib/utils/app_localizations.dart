import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart';

class AppLocalizations {
  AppLocalizations(this.locale);

  static AppLocalizations? _instance;
  final Locale locale;

  static AppLocalizations of(BuildContext context) {
    _instance ??= AppLocalizations(Localizations.localeOf(context));
    return _instance!;
  }

  static const supportedLocales = [
    Locale('es', 'ES'),
    Locale('en', 'US'),
  ];

  static final List<LocalizationsDelegate<Object>> localizationsDelegates = [
    AppLocalizationsDelegate(),
    GlobalMaterialLocalizations.delegate,
    GlobalWidgetsLocalizations.delegate,
    GlobalCupertinoLocalizations.delegate,
  ];

  // Autenticación
  String get loginTitle => locale.languageCode == 'en' ? 'Welcome to Synergy' : 'Bienvenido a Synergy';
  String get loginSubtitle => locale.languageCode == 'en' 
    ? 'Manage initiatives and participate from anywhere.'
    : 'Gestiona iniciativas y participa desde cualquier lugar.';
  String get emailLabel => locale.languageCode == 'en' ? 'Email address' : 'Correo electrónico';
  String get passwordLabel => locale.languageCode == 'en' ? 'Password' : 'Contraseña';
  String get loginButton => locale.languageCode == 'en' ? 'Sign in' : 'Iniciar sesión';
  String get registerButton => locale.languageCode == 'en' ? 'Register' : 'Registrarse';
  String get noAccountText => locale.languageCode == 'en' 
    ? "Don't have an account? " 
    : '¿No tienes cuenta? ';
  String get invalidEmail => locale.languageCode == 'en' 
    ? 'Enter a valid email' 
    : 'Ingresa un correo válido';
  String get minPasswordLength => locale.languageCode == 'en' 
    ? 'Minimum 6 characters' 
    : 'Mínimo 6 caracteres';

  // Navegación
  String get projectsTab => locale.languageCode == 'en' ? 'Projects' : 'Proyectos';
  String get tasksTab => locale.languageCode == 'en' ? 'Tasks' : 'Tareas';
  String get usersTab => locale.languageCode == 'en' ? 'Users' : 'Usuarios';
  String get profileTab => locale.languageCode == 'en' ? 'Profile' : 'Perfil';

  // Proyectos
  String get projectsTitle => locale.languageCode == 'en' ? 'My Projects' : 'Mis Proyectos';
  String get projectDetailTitle => locale.languageCode == 'en' ? 'Project Details' : 'Detalle del Proyecto';
  String get newProjectButton => locale.languageCode == 'en' ? 'New Project' : 'Nuevo Proyecto';
  String get joinProjectButton => locale.languageCode == 'en' ? 'Join Now' : 'Participar Ahora';
  String get leaveProjectButton => locale.languageCode == 'en' ? 'Leave Project' : 'Salir del Proyecto';
  String get projectNameLabel => locale.languageCode == 'en' ? 'Project name' : 'Nombre del proyecto';
  String get projectDescriptionLabel => locale.languageCode == 'en' ? 'Description' : 'Descripción';
  String get projectStatus => locale.languageCode == 'en' ? 'Status' : 'Estado';
  String get members => locale.languageCode == 'en' ? 'members' : 'integrantes';
  String get tasks => locale.languageCode == 'en' ? 'tasks' : 'tareas';

  // Tareas
  String get tasksTitle => locale.languageCode == 'en' ? 'My Tasks' : 'Mis Tareas';
  String get taskDetailTitle => locale.languageCode == 'en' ? 'Task Details' : 'Detalle de la Tarea';
  String get newTaskButton => locale.languageCode == 'en' ? 'New Task' : 'Nueva Tarea';
  String get taskTitleLabel => locale.languageCode == 'en' ? 'Task title' : 'Título de la tarea';
  String get taskDescriptionLabel => locale.languageCode == 'en' ? 'Description' : 'Descripción';
  String get dueDate => locale.languageCode == 'en' ? 'Due date' : 'Fecha límite';
  String get uploadDelivery => locale.languageCode == 'en' ? 'Upload Delivery' : 'Entregar Archivo';
  String get submitted => locale.languageCode == 'en' ? 'Submitted' : 'Entregado';

  // Comentarios
  String get comments => locale.languageCode == 'en' ? 'Comments' : 'Comentarios';
  String get writeComment => locale.languageCode == 'en' ? 'Write a comment...' : 'Escribe un comentario...';
  String get publish => locale.languageCode == 'en' ? 'Publish' : 'Publicar';
  String get noComments => locale.languageCode == 'en' 
    ? 'No comments yet' 
    : 'No hay comentarios aún';
  String get beFirstComment => locale.languageCode == 'en' 
    ? 'Be the first to comment!' 
    : '¡Sé el primero en comentar!';

  // Perfil
  String get profileTitle => locale.languageCode == 'en' ? 'My Profile' : 'Mi Perfil';
  String get changePasswordButton => locale.languageCode == 'en' 
    ? 'Change Password' 
    : 'Cambiar Contraseña';
  String get changePasswordTitle => locale.languageCode == 'en' 
    ? 'Change Password' 
    : 'Cambiar Contraseña';
  String get currentPassword => locale.languageCode == 'en' 
    ? 'Current password' 
    : 'Contraseña Actual';
  String get newPassword => locale.languageCode == 'en' 
    ? 'New password' 
    : 'Nueva Contraseña';
  String get confirmPassword => locale.languageCode == 'en' 
    ? 'Confirm password' 
    : 'Confirmar Contraseña';
  String get updatePasswordButton => locale.languageCode == 'en' 
    ? 'Update Password' 
    : 'Actualizar Contraseña';
  String get logout => locale.languageCode == 'en' ? 'Logout' : 'Cerrar Sesión';

  // Gestión de Usuarios (Admin)
  String get usersTitle => locale.languageCode == 'en' ? 'User Management' : 'Gestión de Usuarios';
  String get editRole => locale.languageCode == 'en' ? 'Edit Role' : 'Cambiar Rol';
  String get deleteUser => locale.languageCode == 'en' ? 'Delete User' : 'Eliminar Usuario';
  String get confirmDelete => locale.languageCode == 'en' 
    ? 'Are you sure you want to delete this user?' 
    : '¿Estás seguro de que deseas eliminar este usuario?';

  // Estados
  String getStatusLabel(String status) {
    return switch (status.toUpperCase()) {
      'ACTIVO' || 'ACTIVE' => locale.languageCode == 'en' ? 'Active' : 'Activo',
      'INACTIVO' || 'INACTIVE' => locale.languageCode == 'en' ? 'Inactive' : 'Inactivo',
      'COMPLETADO' || 'COMPLETED' => locale.languageCode == 'en' ? 'Completed' : 'Completado',
      'CANCELADO' || 'CANCELLED' => locale.languageCode == 'en' ? 'Cancelled' : 'Cancelado',
      'PENDIENTE' || 'PENDING' => locale.languageCode == 'en' ? 'Pending' : 'Pendiente',
      'EN_PROGRESO' || 'IN_PROGRESS' => locale.languageCode == 'en' ? 'In Progress' : 'En Progreso',
      'ENTREGADO' || 'DELIVERED' => locale.languageCode == 'en' ? 'Delivered' : 'Entregado',
      _ => status,
    };
  }

  // Roles
  String getRoleLabel(String role) {
    return switch (role.toUpperCase()) {
      'ADMINISTRADOR' => locale.languageCode == 'en' ? 'Administrator' : 'Administrador',
      'LIDER' => locale.languageCode == 'en' ? 'Leader' : 'Líder',
      'PARTICIPANTE' => locale.languageCode == 'en' ? 'Participant' : 'Participante',
      _ => role,
    };
  }

  // Mensajes generales
  String get loading => locale.languageCode == 'en' ? 'Loading...' : 'Cargando...';
  String get error => locale.languageCode == 'en' ? 'Error' : 'Error';
  String get success => locale.languageCode == 'en' ? 'Success' : 'Éxito';
  String get cancel => locale.languageCode == 'en' ? 'Cancel' : 'Cancelar';
  String get delete => locale.languageCode == 'en' ? 'Delete' : 'Eliminar';
  String get edit => locale.languageCode == 'en' ? 'Edit' : 'Editar';
  String get save => locale.languageCode == 'en' ? 'Save' : 'Guardar';
  String get emptyState => locale.languageCode == 'en' 
    ? 'No items to display' 
    : 'No hay elementos para mostrar';
  String get tryAgain => locale.languageCode == 'en' ? 'Try again' : 'Intentar de nuevo';
  String get connectionError => locale.languageCode == 'en' 
    ? 'Connection error. Check your internet.' 
    : 'Error de conexión. Verifica tu internet.';
}

class AppLocalizationsDelegate extends LocalizationsDelegate<AppLocalizations> {
  const AppLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) => AppLocalizations.supportedLocales.contains(locale);

  @override
  Future<AppLocalizations> load(Locale locale) async {
    Intl.defaultLocale = '${locale.languageCode}_${locale.countryCode}';
    return AppLocalizations(locale);
  }

  @override
  bool shouldReload(AppLocalizationsDelegate old) => false;
}
