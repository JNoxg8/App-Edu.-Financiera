import 'package:flutter/material.dart';

import '../deudas/pantallas/lista_deudas_pantalla.dart';
import '../movimientos/pantallas/lista_movimientos_pantalla.dart';
import '../metas_ahorro/pantallas/lista_metas_ahorro_pantalla.dart';
import '../notificaciones/pantallas/notificaciones_pantalla.dart';
import '../perfil/modelo/perfil.dart';
import '../perfil/pantallas/perfil_pantalla.dart';
import '../presupuesto/pantallas/presupuesto_pantalla.dart';

class InicioPantalla extends StatefulWidget {
  final Perfil perfil;

  const InicioPantalla({super.key, required this.perfil});

  @override
  State<InicioPantalla> createState() => _InicioPantallaState();
}

class _InicioPantallaState extends State<InicioPantalla> {
  int _indice = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _indice,
        children: [
          const ListaDeudasPantalla(),
          ListaMovimientosPantalla(moneda: widget.perfil.moneda),
          PresupuestoPantalla(moneda: widget.perfil.moneda),
          const ListaMetasAhorroPantalla(),
          const NotificacionesPantalla(),
          PerfilPantalla(perfil: widget.perfil),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _indice,
        onDestinationSelected: (i) => setState(() => _indice = i),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.account_balance_wallet_outlined),
            selectedIcon: Icon(Icons.account_balance_wallet),
            label: 'Deudas',
          ),
          NavigationDestination(
            icon: Icon(Icons.receipt_long_outlined),
            selectedIcon: Icon(Icons.receipt_long),
            label: 'Movimientos',
          ),
          NavigationDestination(
            icon: Icon(Icons.pie_chart_outline),
            selectedIcon: Icon(Icons.pie_chart),
            label: 'Presupuesto',
          ),
          NavigationDestination(
            icon: Icon(Icons.savings_outlined),
            selectedIcon: Icon(Icons.savings),
            label: 'Metas',
          ),
          NavigationDestination(
            icon: Icon(Icons.notifications_none),
            selectedIcon: Icon(Icons.notifications),
            label: 'Avisos',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: 'Perfil',
          ),
        ],
      ),
    );
  }
}
