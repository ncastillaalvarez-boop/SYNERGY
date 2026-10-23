import 'package:flutter/material.dart';

/// Semáforo del modelo predictivo: Alto (rojo), Medio (ámbar), Bajo (verde), sin datos (gris).
class RiskChip extends StatelessWidget {
  const RiskChip({super.key, required this.level, this.probability});
  final String? level;
  final double? probability;

  static Color colorFor(String? level) => switch (level) {
        'Alto' => const Color(0xFFEF4444),
        'Medio' => const Color(0xFFF59E0B),
        'Bajo' => const Color(0xFF10B981),
        _ => const Color(0xFF94A3B8),
      };

  @override
  Widget build(BuildContext context) {
    final color = colorFor(level);
    final text = level == null
        ? 'Riesgo: sin datos'
        : 'Riesgo ${level!.toLowerCase()}${probability == null ? '' : ' · ${(probability! * 100).round()} %'}';
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(color: color.withValues(alpha: .12), borderRadius: BorderRadius.circular(99)),
      child: Row(mainAxisSize: MainAxisSize.min, children: [
        Container(width: 8, height: 8, decoration: BoxDecoration(color: color, shape: BoxShape.circle)),
        const SizedBox(width: 6),
        Text(text, style: TextStyle(color: color, fontWeight: FontWeight.w700, fontSize: 12)),
      ]),
    );
  }
}
