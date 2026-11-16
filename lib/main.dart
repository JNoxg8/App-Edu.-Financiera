import 'package:flutter/material.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'comun/tema.dart';
import 'inicio/arranque_pantalla.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await initializeDateFormatting('es_CO');

  runApp(const FinanzasApp());
}

class FinanzasApp extends StatelessWidget {
  const FinanzasApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Zenfi',
      debugShowCheckedModeBanner: false,
      theme: temaZenfi(),
      home: const ArranquePantalla(),
    );
  }
}
