import '../../comun/moneda.dart';

/// Nivel de conocimiento financiero (HU001). Espejo del enum del backend.
enum NivelConocimiento {
  basico('BASICO', 'Básico',
      'Estoy empezando: quiero aprender los conceptos esenciales.'),
  intermedio('INTERMEDIO', 'Intermedio',
      'Manejo un presupuesto y conozco productos como el crédito.'),
  avanzado('AVANZADO', 'Avanzado',
      'Entiendo tasas, inversión y planeación a largo plazo.');

  final String codigo;
  final String nombre;
  final String descripcion;

  const NivelConocimiento(this.codigo, this.nombre, this.descripcion);

  static NivelConocimiento desdeCodigo(String codigo) =>
      NivelConocimiento.values.firstWhere(
        (n) => n.codigo == codigo,
        orElse: () => throw ArgumentError('Nivel desconocido: $codigo'),
      );
}

/// Espejo en Dart del PerfilRespuestaDto del backend.
class Perfil {
  final int id;
  final String nombre;
  final Moneda moneda;
  final NivelConocimiento nivel;
  final DateTime fechaCreacion;

  const Perfil({
    required this.id,
    required this.nombre,
    required this.moneda,
    required this.nivel,
    required this.fechaCreacion,
  });

  factory Perfil.desdeJson(Map<String, dynamic> json) {
    return Perfil(
      id: json['id'] as int,
      nombre: json['nombre'] as String,
      moneda: Moneda.desdeCodigo(json['moneda'] as String),
      nivel: NivelConocimiento.desdeCodigo(json['nivel'] as String),
      fechaCreacion: DateTime.parse(json['fechaCreacion'] as String),
    );
  }
}
