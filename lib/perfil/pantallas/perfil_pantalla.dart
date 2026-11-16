import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../modelo/perfil.dart';

/// Pestana "Perfil": muestra los datos registrados en HU001.
/// La HU solo contempla crear el perfil, por eso aqui no se edita.
class PerfilPantalla extends StatelessWidget {
  final Perfil perfil;

  const PerfilPantalla({super.key, required this.perfil});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final inicial =
        perfil.nombre.isNotEmpty ? perfil.nombre[0].toUpperCase() : '?';

    return Scaffold(
      appBar: AppBar(title: const Text('Mi perfil')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Center(
            child: CircleAvatar(
              radius: 40,
              backgroundColor: tema.colorScheme.primaryContainer,
              child: Text(inicial,
                  style: tema.textTheme.headlineMedium
                      ?.copyWith(color: tema.colorScheme.onPrimaryContainer)),
            ),
          ),
          const SizedBox(height: 12),
          Text(perfil.nombre,
              textAlign: TextAlign.center,
              style: tema.textTheme.titleLarge
                  ?.copyWith(fontWeight: FontWeight.bold)),
          Text(
            'En Zenfi desde el '
            '${DateFormat('dd/MM/yyyy').format(perfil.fechaCreacion)}',
            textAlign: TextAlign.center,
            style: tema.textTheme.bodySmall
                ?.copyWith(color: tema.colorScheme.outline),
          ),
          const SizedBox(height: 24),
          Card(
            child: ListTile(
              leading: const Icon(Icons.payments_outlined),
              title: const Text('Moneda de preferencia'),
              subtitle: Text(
                  '${perfil.moneda.nombre} (${perfil.moneda.codigo})'),
            ),
          ),
          Card(
            child: ListTile(
              leading: const Icon(Icons.school_outlined),
              title: const Text('Nivel de conocimiento'),
              subtitle: Text(perfil.nivel.nombre),
            ),
          ),
          // Criterio HU001 "se habilitan los modulos educativos del nivel":
          // el nivel ya esta guardado; el contenido llega con el modulo educativo.
          Card(
            color: tema.colorScheme.surfaceContainerHighest,
            child: ListTile(
              leading: const Icon(Icons.menu_book_outlined),
              title: Text('Módulos educativos · ${perfil.nivel.nombre}'),
              subtitle: const Text('Disponibles próximamente'),
            ),
          ),
        ],
      ),
    );
  }
}
