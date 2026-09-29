import 'package:flutter/material.dart';

import '../models/auth_session.dart';
import '../models/usuario.dart';
import '../services/user_service.dart';
import '../utils/api_exception.dart';

class AdminUsersScreen extends StatefulWidget {
  const AdminUsersScreen({super.key, required this.session});
  final AuthSession session;

  @override
  State<AdminUsersScreen> createState() => _AdminUsersScreenState();
}

class _AdminUsersScreenState extends State<AdminUsersScreen> {
  final _userService = UserService();
  List<Usuario> _users = [];
  var _isLoading = true;
  var _selectedRole = 'TODOS';
  final _roles = ['TODOS', 'ADMINISTRADOR', 'LIDER', 'PARTICIPANTE'];

  @override
  void initState() {
    super.initState();
    _loadUsers();
  }

  Future<void> _loadUsers() async {
    setState(() => _isLoading = true);
    try {
      final users = await _userService.getAllForAdmin();
      setState(() => _users = users);
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error al cargar usuarios: $error')),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  List<Usuario> get _filteredUsers {
    if (_selectedRole == 'TODOS') return _users;
    return _users.where((u) => u.rol.toUpperCase() == _selectedRole).toList();
  }

  Future<void> _showEditRoleDialog(Usuario user) async {
    String selectedRole = user.rol;
    final newRole = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Cambiar Rol'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('Usuario: ${user.nombre}', style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            DropdownButton<String>(
              value: selectedRole,
              isExpanded: true,
              items: const [
                DropdownMenuItem(value: 'ADMINISTRADOR', child: Text('Administrador')),
                DropdownMenuItem(value: 'LIDER', child: Text('Líder')),
                DropdownMenuItem(value: 'PARTICIPANTE', child: Text('Participante')),
              ],
              onChanged: (value) => selectedRole = value ?? selectedRole,
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          FilledButton(
            onPressed: () => Navigator.pop(context, selectedRole),
            child: const Text('Actualizar'),
          ),
        ],
      ),
    );

    if (newRole != null && newRole != user.rol) {
      try {
        await _userService.changeRole(user.id ?? '', newRole);
        await _loadUsers();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Rol actualizado exitosamente')),
          );
        }
      } on ApiException catch (error) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
        }
      }
    }
  }

  Future<void> _toggleUserStatus(Usuario user) async {
    final newStatus = !user.activo;
    try {
      await _userService.setActive(user.id ?? '', newStatus);
      await _loadUsers();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Usuario ${newStatus ? 'activado' : 'desactivado'} exitosamente')),
        );
      }
    } on ApiException catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
      }
    }
  }

  Future<void> _deleteUser(Usuario user) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Eliminar Usuario'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('¿Estás seguro de que deseas eliminar este usuario?'),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.red.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Nombre: ${user.nombre}', style: const TextStyle(fontWeight: FontWeight.bold)),
                  Text('Correo: ${user.correo}'),
                ],
              ),
            ),
            const SizedBox(height: 12),
            Text('Esta acción es irreversible', style: TextStyle(color: Colors.red[400], fontSize: 12)),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Eliminar'),
          ),
        ],
      ),
    );

    if (confirm == true) {
      try {
        await _userService.delete(user.id ?? '');
        await _loadUsers();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Usuario eliminado exitosamente')),
          );
        }
      } on ApiException catch (error) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(
          title: const Text('Gestión de Usuarios'),
          elevation: 0,
          actions: [
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 12),
              child: Center(
                child: Badge.count(
                  count: _users.length,
                  child: const Icon(Icons.people_outline),
                ),
              ),
            ),
          ],
        ),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : Column(
                children: [
                  // Filtro por rol
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
                    child: Row(
                      children: _roles
                          .map((role) => Padding(
                                padding: const EdgeInsets.symmetric(horizontal: 4),
                                child: FilterChip(
                                  label: Text(role),
                                  selected: _selectedRole == role,
                                  onSelected: (_) => setState(() => _selectedRole = role),
                                ),
                              ))
                          .toList(),
                    ),
                  ),
                  // Lista de usuarios
                  Expanded(
                    child: _filteredUsers.isEmpty
                        ? Center(
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(Icons.people_outline, size: 64, color: Theme.of(context).colorScheme.outline),
                                const SizedBox(height: 16),
                                Text('No hay usuarios con este filtro',
                                    style: Theme.of(context).textTheme.bodyLarge),
                              ],
                            ),
                          )
                        : RefreshIndicator(
                            onRefresh: _loadUsers,
                            child: ListView.separated(
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                              itemCount: _filteredUsers.length,
                              separatorBuilder: (_, __) => const SizedBox(height: 8),
                              itemBuilder: (context, index) {
                                final user = _filteredUsers[index];
                                return _UserCard(
                                  user: user,
                                  onEditRole: () => _showEditRoleDialog(user),
                                  onToggleStatus: () => _toggleUserStatus(user),
                                  onDelete: () => _deleteUser(user),
                                );
                              },
                            ),
                          ),
                  ),
                ],
              ),
      );
}

class _UserCard extends StatelessWidget {
  const _UserCard({
    required this.user,
    required this.onEditRole,
    required this.onToggleStatus,
    required this.onDelete,
  });

  final Usuario user;
  final VoidCallback onEditRole;
  final VoidCallback onToggleStatus;
  final VoidCallback onDelete;

  IconData _getRoleIcon(String role) {
    switch (role.toUpperCase()) {
      case 'ADMINISTRADOR':
        return Icons.admin_panel_settings;
      case 'LIDER':
        return Icons.people_alt;
      default:
        return Icons.person;
    }
  }

  @override
  Widget build(BuildContext context) => Card(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: Theme.of(context).colorScheme.primaryContainer,
                    ),
                    child: Icon(
                      Icons.person,
                      color: Theme.of(context).colorScheme.onPrimaryContainer,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(user.nombre ?? 'Sin nombre',
                            style: Theme.of(context).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.bold)),
                        Text(user.correo ?? 'sin@email.com',
                            style: Theme.of(context).textTheme.bodySmall
                                ?.copyWith(color: Theme.of(context).colorScheme.outline)),
                      ],
                    ),
                  ),
                  if (!user.activo)
                    Chip(
                      label: const Text('Inactivo', style: TextStyle(fontSize: 12, color: Colors.white)),
                      backgroundColor: Colors.red,
                    ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Icon(_getRoleIcon(user.rol), size: 16, color: Theme.of(context).colorScheme.primary),
                  const SizedBox(width: 6),
                  Text(user.rol, style: Theme.of(context).textTheme.bodySmall?.copyWith(fontWeight: FontWeight.w600)),
                  const Spacer(),
                  Wrap(
                    spacing: 4,
                    children: [
                      SizedBox(
                        height: 32,
                        width: 32,
                        child: Tooltip(
                          message: 'Editar rol',
                          child: IconButton(
                            icon: const Icon(Icons.edit, size: 18),
                            padding: EdgeInsets.zero,
                            onPressed: onEditRole,
                          ),
                        ),
                      ),
                      SizedBox(
                        height: 32,
                        width: 32,
                        child: Tooltip(
                          message: user.activo ? 'Desactivar' : 'Activar',
                          child: IconButton(
                            icon: Icon(user.activo ? Icons.check_circle : Icons.cancel, size: 18),
                            padding: EdgeInsets.zero,
                            color: user.activo ? Colors.green : Colors.orange,
                            onPressed: onToggleStatus,
                          ),
                        ),
                      ),
                      SizedBox(
                        height: 32,
                        width: 32,
                        child: Tooltip(
                          message: 'Eliminar',
                          child: IconButton(
                            icon: const Icon(Icons.delete, size: 18),
                            padding: EdgeInsets.zero,
                            color: Colors.red,
                            onPressed: onDelete,
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ],
          ),
        ),
      );
}
