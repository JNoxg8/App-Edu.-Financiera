import 'package:flutter/material.dart';

import '../comun/menu_lateral.dart';
import '../deudas/pantallas/lista_deudas_pantalla.dart';
import '../movimientos/pantallas/lista_movimientos_pantalla.dart';
import '../metas_ahorro/pantallas/lista_metas_ahorro_pantalla.dart';
import '../notificaciones/pantallas/notificaciones_pantalla.dart';
import '../panel/pantallas/panel_pantalla.dart';
import '../perfil/modelo/perfil.dart';
import '../perfil/pantallas/perfil_pantalla.dart';
import '../presupuesto/pantallas/presupuesto_pantalla.dart';

/// Contenedor de los modulos. Se navega con el menu lateral (☰).
///
/// IndexedStack mantiene vivos todos los modulos: al cambiar de uno a
/// otro no se pierde lo que estaba en pantalla, y todos siguen escuchando
/// CambiosFinancieros para actualizarse solos.
class InicioPantalla extends StatefulWidget {
  final Perfil perfil;

  const InicioPantalla({super.key, required this.perfil});

  @override
  State<InicioPantalla> createState() => _InicioPantallaState();
}

class _InicioPantallaState extends State<InicioPantalla> {
  /// El orden de esta lista es el mismo de los modulos en el IndexedStack.
  static const _modulos = [
    ModuloMenu(
      titulo: 'Inicio',
      icono: Icons.home_outlined,
      iconoSeleccionado: Icons.home,
      grupo: 'Resumen',
    ),
    ModuloMenu(
      titulo: 'Movimientos',
      icono: Icons.receipt_long_outlined,
      iconoSeleccionado: Icons.receipt_long,
      grupo: 'Mis finanzas',
    ),
    ModuloMenu(
      titulo: 'Presupuesto',
      icono: Icons.pie_chart_outline,
      iconoSeleccionado: Icons.pie_chart,
      grupo: 'Mis finanzas',
    ),
    ModuloMenu(
      titulo: 'Metas de ahorro',
      icono: Icons.savings_outlined,
      iconoSeleccionado: Icons.savings,
      grupo: 'Mis finanzas',
    ),
    ModuloMenu(
      titulo: 'Deudas',
      icono: Icons.account_balance_wallet_outlined,
      iconoSeleccionado: Icons.account_balance_wallet,
      grupo: 'Mis finanzas',
    ),
    ModuloMenu(
      titulo: 'Avisos',
      icono: Icons.notifications_none,
      iconoSeleccionado: Icons.notifications,
      grupo: 'Mi cuenta',
    ),
    ModuloMenu(
      titulo: 'Perfil',
      icono: Icons.person_outline,
      iconoSeleccionado: Icons.person,
      grupo: 'Mi cuenta',
    ),
  ];

  int _indice = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      drawer: MenuLateral(
        modulos: _modulos,
        seleccionado: _indice,
        nombreUsuario: widget.perfil.nombre,
        onSeleccionar: (i) => setState(() => _indice = i),
      ),
      body: IndexedStack(
        index: _indice,
        children: [
          // HU015: el panel principal es lo primero que se ve al entrar
          PanelPantalla(moneda: widget.perfil.moneda),
          ListaMovimientosPantalla(moneda: widget.perfil.moneda),
          PresupuestoPantalla(moneda: widget.perfil.moneda),
          const ListaMetasAhorroPantalla(),
          const ListaDeudasPantalla(),
          const NotificacionesPantalla(),
          PerfilPantalla(perfil: widget.perfil),
        ],
      ),
    );
  }
}
