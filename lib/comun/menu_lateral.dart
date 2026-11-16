import 'package:flutter/material.dart';

/// Un modulo de la app tal como aparece en el menu lateral.
class ModuloMenu {
  final String titulo;
  final IconData icono;
  final IconData iconoSeleccionado;

  /// Seccion del menu ("Mis finanzas", "Mi cuenta"...). Los modulos de una
  /// misma seccion deben ir seguidos en la lista.
  final String grupo;

  const ModuloMenu({
    required this.titulo,
    required this.icono,
    required this.iconoSeleccionado,
    required this.grupo,
  });
}

/// Menu lateral (navigation drawer de Material 3) con todos los modulos,
/// agrupados por seccion. Se despliega desde la izquierda con BotonMenu.
class MenuLateral extends StatelessWidget {
  final List<ModuloMenu> modulos;
  final int seleccionado;
  final ValueChanged<int> onSeleccionar;
  final String nombreUsuario;

  const MenuLateral({
    super.key,
    required this.modulos,
    required this.seleccionado,
    required this.onSeleccionar,
    required this.nombreUsuario,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return NavigationDrawer(
      selectedIndex: seleccionado,
      onDestinationSelected: (i) {
        // Cierra el menu y luego cambia de modulo
        Navigator.of(context).pop();
        onSeleccionar(i);
      },
      children: [
        // Encabezado: nombre de la app y saludo al usuario
        Padding(
          padding: const EdgeInsets.fromLTRB(28, 24, 16, 16),
          child: Row(
            children: [
              CircleAvatar(
                radius: 24,
                backgroundColor: tema.colorScheme.primaryContainer,
                child: Icon(Icons.savings,
                    color: tema.colorScheme.onPrimaryContainer),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Zenfi',
                        style: tema.textTheme.titleLarge
                            ?.copyWith(fontWeight: FontWeight.bold)),
                    Text('Hola, $nombreUsuario',
                        overflow: TextOverflow.ellipsis,
                        style: tema.textTheme.bodyMedium
                            ?.copyWith(color: tema.colorScheme.outline)),
                  ],
                ),
              ),
            ],
          ),
        ),
        const Divider(indent: 28, endIndent: 28),
        for (var i = 0; i < modulos.length; i++) ...[
          // Titulo de seccion cuando empieza un grupo nuevo
          if (i == 0 || modulos[i].grupo != modulos[i - 1].grupo)
            Padding(
              padding: const EdgeInsets.fromLTRB(28, 16, 16, 8),
              child: Text(modulos[i].grupo, style: tema.textTheme.titleSmall),
            ),
          NavigationDrawerDestination(
            icon: Icon(modulos[i].icono),
            selectedIcon: Icon(modulos[i].iconoSeleccionado),
            label: Text(modulos[i].titulo),
          ),
        ],
      ],
    );
  }
}

/// Boton ☰ para la AppBar de cada modulo: abre el menu lateral.
///
/// Cada modulo tiene su propio Scaffold, pero el menu pertenece al
/// Scaffold de InicioPantalla, que los envuelve. Scaffold.of(context)
/// devolveria el del modulo (que no tiene menu), asi que se busca hacia
/// arriba el primer Scaffold que si tenga uno.
class BotonMenu extends StatelessWidget {
  const BotonMenu({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: const Icon(Icons.menu),
      tooltip: 'Menú',
      onPressed: () => abrirMenuLateral(context),
    );
  }
}

/// Abre el menu lateral del Scaffold mas cercano que tenga uno.
void abrirMenuLateral(BuildContext context) {
  ScaffoldState? conMenu;
  context.visitAncestorElements((elemento) {
    if (elemento is StatefulElement && elemento.state is ScaffoldState) {
      final scaffold = elemento.state as ScaffoldState;
      if (scaffold.hasDrawer) {
        conMenu = scaffold;
        return false; // encontrado: deja de subir
      }
    }
    return true;
  });
  conMenu?.openDrawer();
}
