/// Ayudas para trabajar con meses (sin dia) en el presupuesto (HU011).
///
/// Un mes se representa con un DateTime en el dia 1. Los nombres de los
/// meses se escriben a mano para no depender de initializeDateFormatting,
/// que DateFormat necesita para mostrar textos en espanol.
const _nombresMeses = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
];

/// Primer dia del mes de la fecha dada: 2026-10-25 -> 2026-10-01.
DateTime inicioDeMes(DateTime fecha) => DateTime(fecha.year, fecha.month);

/// El mes actual, en su dia 1.
DateTime mesActual() => inicioDeMes(DateTime.now());

/// Formato que espera el backend: 2026-10.
String codigoMes(DateTime mes) =>
    '${mes.year.toString().padLeft(4, '0')}-${mes.month.toString().padLeft(2, '0')}';

/// Texto para la pantalla: Octubre 2026.
String nombreMes(DateTime mes) => '${_nombresMeses[mes.month - 1]} ${mes.year}';

/// HU011: solo se crea presupuesto para el mes actual o uno futuro.
bool esMesPasado(DateTime mes) => inicioDeMes(mes).isBefore(mesActual());
