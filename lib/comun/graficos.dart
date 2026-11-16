import 'dart:math' as math;

import 'package:flutter/material.dart';

import 'tema.dart';

// ---------------------------------------------------------------------------
// Graficos circulares de Zenfi, dibujados con CustomPainter (sin librerias).
//
// - AnilloProgreso: UNA proporcion frente a un limite (presupuesto usado,
//   avance de una meta, porcentaje pagado de una deuda).
// - GraficoDona: como se reparte un total entre varias partes (gastos por
//   categoria). Maximo 5 partes + "Otras"; siempre va con su leyenda.
// ---------------------------------------------------------------------------

/// Colores de las categorias en la dona, en ORDEN FIJO.
///
/// Se validaron con un verificador de paletas: se distinguen entre si con
/// vision normal y con los tres tipos de daltonismo, incluso el ultimo con el
/// primero (en una dona tambien quedan juntos), y todos tienen contraste
/// >= 3:1 sobre blanco. No se usan el verde ni el rojo de la app para que no
/// se confundan con "positivo" o "negativo".
class PaletaCategorias {
  PaletaCategorias._();

  static const colores = [
    Color(0xFF256ABF), // azul
    Color(0xFFD0692F), // naranja tostado
    Color(0xFF17977A), // verde azulado
    Color(0xFF8E5BB8), // violeta
    Color(0xFFB8860B), // dorado oscuro
  ];

  /// Para la parte que agrupa el resto ("Otras"): gris, no compite.
  static const otras = Color(0xFF8C97A6);
}

/// Una parte de la dona ya lista para dibujar y para la leyenda.
class ParteGrafico {
  final String nombre;
  final double valor;

  /// 0 a 100, sobre el total de la dona.
  final double porcentaje;
  final Color color;

  const ParteGrafico({
    required this.nombre,
    required this.valor,
    required this.porcentaje,
    required this.color,
  });
}

/// Toma partes ordenadas de mayor a menor y deja las [maximo] primeras con
/// su color; el resto se suma en una sola parte "Otras" (gris).
///
/// Mas de 6 colores ya no se distinguen bien: por eso se agrupa en vez de
/// inventar colores nuevos.
List<ParteGrafico> agruparPartes(
  List<({String nombre, double valor, double porcentaje})> partes, {
  int maximo = 5,
}) {
  final resultado = <ParteGrafico>[];
  for (var i = 0; i < partes.length && i < maximo; i++) {
    resultado.add(ParteGrafico(
      nombre: partes[i].nombre,
      valor: partes[i].valor,
      porcentaje: partes[i].porcentaje,
      color: PaletaCategorias.colores[i % PaletaCategorias.colores.length],
    ));
  }
  if (partes.length > maximo) {
    final resto = partes.skip(maximo);
    resultado.add(ParteGrafico(
      nombre: 'Otras (${resto.length})',
      valor: resto.fold(0.0, (s, p) => s + p.valor),
      porcentaje: resto.fold(0.0, (s, p) => s + p.porcentaje),
      color: PaletaCategorias.otras,
    ));
  }
  return resultado;
}

/// Colores de un anillo segun que tan cerca esta del limite:
/// normal (petroleo) -> desde el 80 % advertencia (ambar) -> pasado el
/// 100 % peligro (rojo). La pista es un tono claro del mismo color.
({Color relleno, Color pista}) coloresSeveridad(double fraccion) {
  if (fraccion > 1) {
    return (relleno: ColoresZenfi.negativo, pista: ColoresZenfi.negativoFondo);
  }
  if (fraccion >= 0.8) {
    return (relleno: ColoresZenfi.advertencia, pista: const Color(0xFFF5E6CC));
  }
  return (relleno: ColoresZenfi.petroleo, pista: ColoresZenfi.petroleoClaro);
}

/// Color del avance de una meta segun su estado (HU004): activa en el
/// acento, cumplida en verde y vencida en gris (ya no avanza).
Color colorAvanceMeta(String estado) => switch (estado) {
      'COMPLETADA' => ColoresZenfi.positivo,
      'VENCIDA' => ColoresZenfi.textoSecundario,
      _ => ColoresZenfi.petroleo,
    };

/// Anillo que se llena segun [fraccion] (0 = vacio, 1 = completo). Si la
/// fraccion pasa de 1 el anillo se ve lleno: el exceso lo dice el color y
/// el texto del centro.
class AnilloProgreso extends StatelessWidget {
  final double fraccion;
  final double tamano;
  final double grosor;
  final Color color;
  final Color colorPista;

  /// Lo que va en el centro (normalmente el porcentaje).
  final Widget? centro;

  const AnilloProgreso({
    super.key,
    required this.fraccion,
    required this.color,
    required this.colorPista,
    this.tamano = 56,
    this.grosor = 6,
    this.centro,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: tamano,
      height: tamano,
      child: CustomPaint(
        painter: _PintorAnillo(
          fraccion: fraccion.clamp(0.0, 1.0),
          grosor: grosor,
          color: color,
          colorPista: colorPista,
        ),
        child: Center(child: centro),
      ),
    );
  }
}

class _PintorAnillo extends CustomPainter {
  final double fraccion;
  final double grosor;
  final Color color;
  final Color colorPista;

  _PintorAnillo({
    required this.fraccion,
    required this.grosor,
    required this.color,
    required this.colorPista,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final rect = (Offset.zero & size).deflate(grosor / 2);
    final pincel = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = grosor;

    canvas.drawArc(rect, 0, 2 * math.pi, false, pincel..color = colorPista);
    if (fraccion > 0) {
      // Empieza arriba (-90 grados) y avanza en el sentido del reloj
      canvas.drawArc(rect, -math.pi / 2, 2 * math.pi * fraccion, false,
          pincel
            ..color = color
            ..strokeCap = StrokeCap.round);
    }
  }

  @override
  bool shouldRepaint(_PintorAnillo viejo) =>
      viejo.fraccion != fraccion ||
      viejo.color != color ||
      viejo.colorPista != colorPista ||
      viejo.grosor != grosor;
}

/// Dona: cada parte ocupa un arco proporcional a su valor, con un pequeno
/// espacio blanco entre partes para que se separen bien.
class GraficoDona extends StatelessWidget {
  final List<ParteGrafico> partes;
  final double tamano;
  final double grosor;
  final Widget? centro;

  const GraficoDona({
    super.key,
    required this.partes,
    this.tamano = 140,
    this.grosor = 22,
    this.centro,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: tamano,
      height: tamano,
      child: CustomPaint(
        painter: _PintorDona(
          partes: partes,
          grosor: grosor,
          separacion: Theme.of(context).cardTheme.color ?? Colors.white,
        ),
        child: Center(child: centro),
      ),
    );
  }
}

class _PintorDona extends CustomPainter {
  final List<ParteGrafico> partes;
  final double grosor;
  final Color separacion;

  _PintorDona({
    required this.partes,
    required this.grosor,
    required this.separacion,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final total = partes.fold(0.0, (s, p) => s + p.valor);
    if (total <= 0) return;

    final rect = (Offset.zero & size).deflate(grosor / 2);
    final pincel = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = grosor;

    var inicio = -math.pi / 2;
    final angulos = <double>[];
    for (final parte in partes) {
      final barrido = 2 * math.pi * parte.valor / total;
      canvas.drawArc(rect, inicio, barrido, false, pincel..color = parte.color);
      angulos.add(inicio);
      inicio += barrido;
    }

    // Separadores de 2 px del color de la tarjeta entre parte y parte
    if (partes.length > 1) {
      final centro = size.center(Offset.zero);
      final radioInterno = rect.width / 2 - grosor / 2 - 1;
      final radioExterno = rect.width / 2 + grosor / 2 + 1;
      final linea = Paint()
        ..color = separacion
        ..strokeWidth = 2;
      for (final a in angulos) {
        final dir = Offset(math.cos(a), math.sin(a));
        canvas.drawLine(centro + dir * radioInterno, centro + dir * radioExterno, linea);
      }
    }
  }

  @override
  bool shouldRepaint(_PintorDona viejo) =>
      viejo.partes != partes || viejo.grosor != grosor;
}

/// Fila de leyenda: punto de color + nombre + monto + porcentaje.
/// El texto va en los colores de texto; el color solo esta en el punto.
class FilaLeyenda extends StatelessWidget {
  final Color color;
  final String nombre;
  final String monto;
  final String? detalle;

  const FilaLeyenda({
    super.key,
    required this.color,
    required this.nombre,
    required this.monto,
    this.detalle,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Container(
            width: 12,
            height: 12,
            decoration: BoxDecoration(color: color, shape: BoxShape.circle),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(nombre,
                overflow: TextOverflow.ellipsis,
                style: tema.textTheme.bodyMedium),
          ),
          const SizedBox(width: 8),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(monto,
                  style: tema.textTheme.bodyMedium
                      ?.copyWith(fontWeight: FontWeight.w600)),
              if (detalle != null)
                Text(detalle!,
                    style: tema.textTheme.bodySmall
                        ?.copyWith(color: tema.colorScheme.outline)),
            ],
          ),
        ],
      ),
    );
  }
}
