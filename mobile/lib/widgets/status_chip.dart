import 'package:flutter/material.dart';

import '../utils/formatters.dart';

class StatusChip extends StatelessWidget {
  const StatusChip({super.key, required this.status});
  final String? status;

  @override
  Widget build(BuildContext context) {
    final color = Formatters.statusColor(status);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(color: color.withValues(alpha: .12), borderRadius: BorderRadius.circular(99)),
      child: Text(
        Formatters.statusLabel(status),
        style: TextStyle(color: color, fontWeight: FontWeight.w800, fontSize: 11, letterSpacing: .3),
      ),
    );
  }
}
