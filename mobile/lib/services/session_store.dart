import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../models/auth_session.dart';

class SessionStore {
  static const _sessionKey = 'synergy_session';

  Future<AuthSession?> read() async {
    final raw = (await SharedPreferences.getInstance()).getString(_sessionKey);
    if (raw == null) return null;
    try {
      return AuthSession.fromJson(Map<String, dynamic>.from(jsonDecode(raw) as Map));
    } catch (_) {
      await clear();
      return null;
    }
  }

  Future<void> save(AuthSession session) async {
    await (await SharedPreferences.getInstance()).setString(_sessionKey, jsonEncode(session.toJson()));
  }

  Future<void> clear() async {
    await (await SharedPreferences.getInstance()).remove(_sessionKey);
  }
}
