import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

class NotificationService {
  static final NotificationService _instance = NotificationService._internal();

  factory NotificationService() => _instance;

  NotificationService._internal();

  final FirebaseMessaging _firebaseMessaging = FirebaseMessaging.instance;

  // Callbacks para las notificaciones
  Function(Map<String, dynamic>)? onMessageCallback;
  Function(RemoteMessage)? onMessageOpenedAppCallback;
  Function(RemoteMessage)? onBackgroundMessageCallback;

  Future<void> initialize() async {
    // Solicitar permisos de notificación
    final settings = await _firebaseMessaging.requestPermission(
      alert: true,
      announcement: false,
      badge: true,
      criticalAlert: false,
      provisional: false,
      sound: true,
    );

    if (settings.authorizationStatus == AuthorizationStatus.authorized) {
      // Usuario permitió notificaciones
      _setupMessageHandlers();
      await _getDeviceToken();
    }
  }

  void _setupMessageHandlers() {
    // Mensaje recibido mientras la app está en foreground
    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      if (message.notification != null) {
        onMessageCallback?.call({
          'title': message.notification!.title,
          'body': message.notification!.body,
          'data': message.data,
        });
      }
    });

    // Cuando el usuario toca la notificación (app en background o closed)
    FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      onMessageOpenedAppCallback?.call(message);
    });

    // Manejador para mensajes en background
    FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);
  }

  Future<void> _getDeviceToken() async {
    try {
      final token = await _firebaseMessaging.getToken();
      // TODO: Este token debe ser enviado al backend para asociarlo con el usuario
      // await UserService().updateDeviceToken(token);
      // ignore: unused_local_variable
    } catch (e) {
      // Error al obtener token
    }
  }

  // Refrescar token cuando cambia
  void listenForTokenRefresh() {
    FirebaseMessaging.instance.onTokenRefresh.listen((newToken) {
      // Actualizar token en el backend
      // await UserService().updateDeviceToken(newToken);
    });
  }

  // Suscribirse a tópico (opcional)
  Future<void> subscribeToTopic(String topic) async {
    await _firebaseMessaging.subscribeToTopic(topic);
  }

  // Desuscribirse de tópico
  Future<void> unsubscribeFromTopic(String topic) async {
    await _firebaseMessaging.unsubscribeFromTopic(topic);
  }

  // Obtener token del dispositivo
  Future<String?> getDeviceToken() async {
    return _firebaseMessaging.getToken();
  }
}

// Esta función debe ser de nivel superior (top-level)
@pragma('vm:entry-point')
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  // Inicializar Firebase si aún no está inicializado
  await Firebase.initializeApp();

  // Procesar el mensaje en background
  if (message.notification != null) {
    // Aquí puedes guardar el mensaje, actualizar UI, etc.
  }
}
