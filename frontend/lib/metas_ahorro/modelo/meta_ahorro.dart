/// Modelo Dart correspondiente a la respuesta de la HU003 en Quarkus.
class MetaAhorro {
  final int id;
  final String nombre;
  final double montoObjetivo;
  final DateTime fechaLimite;
  final double montoInicial;
  final double montoAcumulado;
  final String estado;

  const MetaAhorro({
    required this.id,
    required this.nombre,
    required this.montoObjetivo,
    required this.fechaLimite,
    required this.montoInicial,
    required this.montoAcumulado,
    required this.estado,
  });

  factory MetaAhorro.desdeJson(Map<String, dynamic> json) => MetaAhorro(
        id: (json['id'] as num).toInt(),
        nombre: json['nombre'] as String,
        montoObjetivo: (json['montoObjetivo'] as num).toDouble(),
        fechaLimite: DateTime.parse(json['fechaLimite'] as String),
        montoInicial: (json['montoInicial'] as num).toDouble(),
        montoAcumulado: (json['montoAcumulado'] as num).toDouble(),
        estado: json['estado'] as String,
      );
}
