import 'package:finanzas_app/comun/menu_lateral.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// Pruebas de widget del menu lateral: se dibuja la interfaz en memoria
/// (sin emulador) y se simulan los toques.
void main() {
  const modulos = [
    ModuloMenu(
        titulo: 'Inicio',
        icono: Icons.home_outlined,
        iconoSeleccionado: Icons.home,
        grupo: 'Resumen'),
    ModuloMenu(
        titulo: 'Movimientos',
        icono: Icons.receipt_long_outlined,
        iconoSeleccionado: Icons.receipt_long,
        grupo: 'Mis finanzas'),
    ModuloMenu(
        titulo: 'Deudas',
        icono: Icons.account_balance_wallet_outlined,
        iconoSeleccionado: Icons.account_balance_wallet,
        grupo: 'Mis finanzas'),
  ];

  /// Igual que en la app: un Scaffold externo con el menu y, adentro, el
  /// Scaffold del modulo con su propia AppBar y el boton ☰.
  Widget app(ValueChanged<int> onSeleccionar) => MaterialApp(
        home: Scaffold(
          drawer: MenuLateral(
            modulos: modulos,
            seleccionado: 0,
            nombreUsuario: 'Camila',
            onSeleccionar: onSeleccionar,
          ),
          body: Scaffold(
            appBar: AppBar(
              leading: const BotonMenu(),
              title: const Text('Modulo'),
            ),
          ),
        ),
      );

  testWidgets('el boton ☰ del modulo abre el menu del Scaffold externo',
      (tester) async {
    await tester.pumpWidget(app((_) {}));
    expect(find.text('Hola, Camila'), findsNothing);

    await tester.tap(find.byTooltip('Menú'));
    await tester.pumpAndSettle();

    expect(find.text('Hola, Camila'), findsOneWidget);
    expect(find.text('Movimientos'), findsOneWidget);
    expect(find.text('Deudas'), findsOneWidget);
    // Un titulo por seccion, no uno por modulo
    expect(find.text('MIS FINANZAS'), findsOneWidget);
  });

  testWidgets('elegir un modulo avisa su posicion y cierra el menu',
      (tester) async {
    int? elegido;
    await tester.pumpWidget(app((i) => elegido = i));

    await tester.tap(find.byTooltip('Menú'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Deudas'));
    await tester.pumpAndSettle();

    expect(elegido, 2);
    expect(find.text('Hola, Camila'), findsNothing);
  });
}
