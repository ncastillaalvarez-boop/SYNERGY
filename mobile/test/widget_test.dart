import 'package:flutter_test/flutter_test.dart';

import 'package:synergy_mobile/utils/app_config.dart';
import 'package:synergy_mobile/utils/password_policy.dart';

void main() {
  group('AppConfig.normalizar', () {
    test('completa http, puerto y /api a partir de una IP', () {
      expect(AppConfig.normalizar('192.168.1.10'), 'http://192.168.1.10:8080/api');
    });

    test('respeta el puerto y la ruta que ya vienen', () {
      expect(AppConfig.normalizar('http://192.168.1.10:9090/api/'), 'http://192.168.1.10:9090/api');
    });

    test('un dominio sin protocolo usa https (servidor en Render)', () {
      expect(AppConfig.normalizar('synergy-expoferia.onrender.com'), 'https://synergy-expoferia.onrender.com/api');
    });

    test('un valor vacío vuelve a la dirección por defecto', () {
      expect(AppConfig.normalizar('  '), AppConfig.defaultApiBaseUrl);
    });
  });

  group('PasswordPolicy (mismas reglas que el servidor)', () {
    test('acepta las contraseñas de las cuentas de prueba', () {
      expect(PasswordPolicy.validar('Synergy123*'), isNull);
      expect(PasswordPolicy.validar('Voluntario123*'), isNull);
    });

    test('rechaza contraseñas cortas o sin mayúscula, minúscula o número', () {
      expect(PasswordPolicy.validar('Ab1'), isNotNull);
      expect(PasswordPolicy.validar('sinmayuscula1'), isNotNull);
      expect(PasswordPolicy.validar('SINMINUSCULA1'), isNotNull);
      expect(PasswordPolicy.validar('SinNumeros'), isNotNull);
    });
  });
}
