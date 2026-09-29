import 'package:flutter/material.dart';

class Formatters {
  Formatters._();

  static String shortDate(DateTime? value) {
    if (value == null) return 'Sin fecha';
    final day = value.day.toString().padLeft(2, '0');
    final month = value.month.toString().padLeft(2, '0');
    return '$day/$month/${value.year}';
  }

  static Color statusColor(String? value) {
    switch (value?.toUpperCase()) {
      case 'COMPLETADO':
      case 'COMPLETADA':
        return const Color(0xFF2E7D32);
      case 'CANCELADO':
      case 'CANCELADA':
      case 'RECHAZADO':
        return const Color(0xFFC62828);
      case 'EN_PROGRESO':
        return const Color(0xFF1565C0);
      default:
        return const Color(0xFFB26A00);
    }
  }

  static String statusLabel(String? value) => (value ?? 'PENDIENTE').replaceAll('_', ' ');
}
