import 'package:flutter_test/flutter_test.dart';

import 'package:synergy_mobile/utils/app_config.dart';

void main() {
  group('AppConfig.normalizar', () {
    test('completa http, puerto y /api a partir de una IP', () {
      expect(AppConfig.normalizar('192.168.1.10'), 'http://192.168.1.10:8080/api');
    });

    test('respeta el puerto y la ruta que ya vienen', () {
      expect(AppConfig.normalizar('http://192.168.1.10:9090/api/'), 'http://192.168.1.10:9090/api');
    });

    test('un valor vacío vuelve a la dirección por defecto', () {
      expect(AppConfig.normalizar('  '), AppConfig.defaultApiBaseUrl);
    });
  });
}
