import 'package:flutter/material.dart';

/// Paleta de Zenfi: tonos sobrios de banca.
///
/// Regla de uso: el azul marino identifica la app (barras y menu), el
/// petroleo marca lo que se puede tocar o el avance (botones flotantes,
/// barras de progreso), y el verde y el rojo se reservan para el
/// significado del dinero (positivo / negativo). El fondo gris azulado
/// hace que las tarjetas blancas, donde esta la informacion, resalten.
class ColoresZenfi {
  ColoresZenfi._();

  static const azulMarino = Color(0xFF1B365D);
  static const azulProfundo = Color(0xFF0F2540);
  static const petroleo = Color(0xFF1F6F78);
  static const petroleoClaro = Color(0xFFD3ECEE);

  /// Fondo de las pantallas (detras de las tarjetas).
  static const fondo = Color(0xFFE6ECF2);
  static const texto = Color(0xFF1C2833);
  static const textoSecundario = Color(0xFF5B6B7C);
  static const borde = Color(0xFFD3DCE6);

  /// Ingresos, disponible, saldo a favor.
  static const positivo = Color(0xFF2E7D4F);

  /// Gastos, excesos, saldos negativos, acciones de eliminar.
  static const negativo = Color(0xFFB3261E);
  static const negativoFondo = Color(0xFFFBEAE8);
  static const negativoTexto = Color(0xFF7A1C17);

  /// Avisos que no son error (por ejemplo, una deuda proxima a vencer).
  static const advertencia = Color(0xFFA86A12);

  /// Versiones claras para usar sobre fondos azul marino.
  static const positivoSobreOscuro = Color(0xFF9FDCB4);
  static const negativoSobreOscuro = Color(0xFFF5AFA9);
}

ThemeData temaZenfi() {
  final esquema = ColorScheme.fromSeed(
    seedColor: ColoresZenfi.azulMarino,
    primary: ColoresZenfi.azulMarino,
    onPrimary: Colors.white,
    primaryContainer: const Color(0xFFD6E3F4),
    onPrimaryContainer: ColoresZenfi.azulProfundo,
    secondary: ColoresZenfi.petroleo,
    onSecondary: Colors.white,
    secondaryContainer: ColoresZenfi.petroleoClaro,
    onSecondaryContainer: const Color(0xFF0B3C41),
    error: ColoresZenfi.negativo,
    surface: Colors.white,
    onSurface: ColoresZenfi.texto,
    surfaceContainerHighest: const Color(0xFFDCE3EA),
    outline: ColoresZenfi.textoSecundario,
    outlineVariant: ColoresZenfi.borde,
  );

  final redondeo = BorderRadius.circular(14);

  return ThemeData(
    useMaterial3: true,
    colorScheme: esquema,
    scaffoldBackgroundColor: ColoresZenfi.fondo,
    appBarTheme: const AppBarTheme(
      backgroundColor: ColoresZenfi.azulMarino,
      foregroundColor: Colors.white,
      elevation: 0,
      scrolledUnderElevation: 3,
      titleTextStyle: TextStyle(
        color: Colors.white,
        fontSize: 20,
        fontWeight: FontWeight.w600,
      ),
    ),
    cardTheme: CardThemeData(
      color: Colors.white,
      surfaceTintColor: Colors.transparent,
      elevation: 1,
      shadowColor: ColoresZenfi.azulMarino.withValues(alpha: 0.18),
      shape: RoundedRectangleBorder(
        borderRadius: redondeo,
        side: const BorderSide(color: ColoresZenfi.borde),
      ),
    ),
    navigationDrawerTheme: NavigationDrawerThemeData(
      backgroundColor: Colors.white,
      indicatorColor: ColoresZenfi.petroleoClaro,
      iconTheme: WidgetStateProperty.resolveWith((estados) => IconThemeData(
            color: estados.contains(WidgetState.selected)
                ? ColoresZenfi.petroleo
                : ColoresZenfi.textoSecundario,
          )),
      labelTextStyle: WidgetStateProperty.resolveWith((estados) => TextStyle(
            fontSize: 15,
            color: estados.contains(WidgetState.selected)
                ? ColoresZenfi.azulProfundo
                : ColoresZenfi.texto,
            fontWeight: estados.contains(WidgetState.selected)
                ? FontWeight.w600
                : FontWeight.normal,
          )),
    ),
    floatingActionButtonTheme: const FloatingActionButtonThemeData(
      backgroundColor: ColoresZenfi.petroleo,
      foregroundColor: Colors.white,
    ),
    progressIndicatorTheme: const ProgressIndicatorThemeData(
      color: ColoresZenfi.petroleo,
      linearTrackColor: Color(0xFFDCE3EA),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: Colors.white,
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
    ),
    chipTheme: const ChipThemeData(
      backgroundColor: Color(0xFFD6E3F4),
      labelStyle: TextStyle(
        color: ColoresZenfi.azulProfundo,
        fontWeight: FontWeight.w600,
      ),
      side: BorderSide.none,
    ),
    dialogTheme: DialogThemeData(
      backgroundColor: Colors.white,
      surfaceTintColor: Colors.transparent,
      shape: RoundedRectangleBorder(borderRadius: redondeo),
    ),
    snackBarTheme: const SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: ColoresZenfi.azulProfundo,
    ),
    dividerTheme: const DividerThemeData(color: ColoresZenfi.borde),
    listTileTheme: const ListTileThemeData(iconColor: ColoresZenfi.azulMarino),
  );
}
