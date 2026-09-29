import 'package:flutter/material.dart';

import '../models/proyecto.dart';
import '../utils/formatters.dart';
import 'status_chip.dart';

class ProjectCard extends StatelessWidget {
  const ProjectCard({super.key, required this.project, required this.onTap});
  final Proyecto project;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Card(
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(child: Text(project.nombre, style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800))),
                    StatusChip(status: project.estado),
                  ],
                ),
                const SizedBox(height: 8),
                Text(project.descripcion, maxLines: 2, overflow: TextOverflow.ellipsis),
                const SizedBox(height: 15),
                Row(
                  children: [
                    const Icon(Icons.group_outlined, size: 18),
                    const SizedBox(width: 5),
                    Text('${project.participantes.length} integrantes'),
                    const Spacer(),
                    const Icon(Icons.calendar_today_outlined, size: 16),
                    const SizedBox(width: 5),
                    Text(Formatters.shortDate(project.fechaFin)),
                  ],
                ),
              ],
            ),
          ),
        ),
      );
}
