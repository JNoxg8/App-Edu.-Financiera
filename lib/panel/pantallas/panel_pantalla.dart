import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../comun/cambios_financieros.dart';
import '../../comun/graficos.dart';
import '../../comun/menu_lateral.dart';
import '../../comun/moneda.dart';
import '../../comun/tema.dart';
import '../../presupuesto/modelo/mes.dart';
import '../../presupuesto/modelo/presupuesto.dart';
import '../modelo/panel_principal.dart';
import '../servicio/panel_servicio.dart';

final _formatoFecha = DateFormat('dd/MM/yyyy');

/// HU015: panel principal. Es la primera pantalla que se ve al entrar.
///
/// Una sola peticion (GET /panel) trae todas las secciones. Escucha
/// CambiosFinancieros, asi que se actualiza solo al registrar, modificar o
/// eliminar movimientos, aportes, abonos, metas o deudas en otra pantalla.
class PanelPantalla extends StatefulWidget {
  final Moneda moneda;

  /// Se puede inyectar para las pruebas (con un MockClient adentro).
  final PanelServicio? servicio;

  const PanelPantalla({super.key, this.moneda = Moneda.cop, this.servicio});

  @override
  State<PanelPantalla> createState() => _PanelPantallaState();
}

class _PanelPantallaState extends State<PanelPantalla> {
  late final PanelServicio _servicio = widget.servicio ?? PanelServicio();
  late Future<PanelPrincipal> _futuro;

  NumberFormat get _formato => widget.moneda.formato;

  @override
  void initState() {
    super.initState();
    _futuro = _servicio.consultar();
    CambiosFinancieros.escuchar.addListener(_recargar);
  }

  @override
  void dispose() {
    CambiosFinancieros.escuchar.removeListener(_recargar);
    super.dispose();
  }

  void _recargar() {
    if (!mounted) return;
    setState(() {
      _futuro = _servicio.consultar();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: const BotonMenu(),
        title: const Text('Mi resumen'),
        actions: [
          IconButton(
            onPressed: _recargar,
            icon: const Icon(Icons.refresh),
            tooltip: 'Recargar',
          ),
        ],
      ),
      body: FutureBuilder<PanelPrincipal>(
        future: _futuro,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }

          if (snapshot.hasError) {
            return Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(32),
                child: Column(
                  children: [
                    Icon(Icons.cloud_off,
                        size: 64, color: Theme.of(context).colorScheme.outline),
                    const SizedBox(height: 16),
                    Text('No se pudo conectar',
                        style: Theme.of(context).textTheme.titleLarge),
                    const SizedBox(height: 8),
                    Text('${snapshot.error}', textAlign: TextAlign.center),
                    const SizedBox(height: 24),
                    FilledButton.icon(
                      onPressed: _recargar,
                      icon: const Icon(Icons.refresh),
                      label: const Text('Reintentar'),
                    ),
                  ],
                ),
              ),
            );
          }

          final panel = snapshot.data!;
          final mes = nombreMes(DateTime.parse('${panel.mes}-01'));
          return RefreshIndicator(
            onRefresh: () async => _recargar(),
            child: ListView(
              padding: const EdgeInsets.only(top: 4, bottom: 24),
              children: [
                _Balance(panel: panel, formato: _formato),
                _PresupuestoDelMes(
                    presupuesto: panel.presupuesto, mes: mes, formato: _formato),
                _GastosDelMes(panel: panel, mes: mes, formato: _formato),
                _Metas(metas: panel.metasActivas, formato: _formato),
                _Deudas(deudas: panel.deudasPendientes, formato: _formato),
              ],
            ),
          );
        },
      ),
    );
  }
}

/// Tarjeta con titulo e icono, comun a las secciones del panel.
class _Seccion extends StatelessWidget {
  final IconData icono;
  final String titulo;
  final Widget contenido;

  const _Seccion({
    required this.icono,
    required this.titulo,
    required this.contenido,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                // Icono en un circulo de color: identifica la seccion de un vistazo
                CircleAvatar(
                  radius: 16,
                  backgroundColor: tema.colorScheme.secondaryContainer,
                  child: Icon(icono,
                      size: 18, color: tema.colorScheme.secondary),
                ),
                const SizedBox(width: 10),
                // Expanded + ellipsis: en pantallas angostas el titulo se
                // recorta en vez de salirse de la tarjeta
                Expanded(
                  child: Text(titulo,
                      overflow: TextOverflow.ellipsis,
                      style: tema.textTheme.titleMedium
                          ?.copyWith(fontWeight: FontWeight.w600)),
                ),
              ],
            ),
            const SizedBox(height: 14),
            contenido,
          ],
        ),
      ),
    );
  }
}

/// Mensaje en gris para una seccion sin informacion.
class _SinDatos extends StatelessWidget {
  final String mensaje;

  const _SinDatos(this.mensaje);

  @override
  Widget build(BuildContext context) {
    return Text(
      mensaje,
      style: Theme.of(context)
          .textTheme
          .bodyMedium
          ?.copyWith(color: Theme.of(context).colorScheme.outline),
    );
  }
}

/// Porcentaje en el centro de un anillo.
class _Porcentaje extends StatelessWidget {
  final double porcentaje;
  final double tamanoLetra;
  final Color? color;
  final String? debajo;

  const _Porcentaje(this.porcentaje,
      {this.tamanoLetra = 13, this.color, this.debajo});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text('${porcentaje.toStringAsFixed(0)} %',
            style: TextStyle(
                fontSize: tamanoLetra,
                fontWeight: FontWeight.bold,
                color: color ?? tema.colorScheme.onSurface)),
        if (debajo != null)
          Text(debajo!,
              style: TextStyle(
                  fontSize: tamanoLetra * 0.55,
                  color: color ?? tema.colorScheme.outline)),
      ],
    );
  }
}

/// Tarjeta destacada en azul: el balance es el dato principal del panel.
/// El anillo muestra que parte de lo que entro ya se gasto.
class _Balance extends StatelessWidget {
  final PanelPrincipal panel;
  final NumberFormat formato;

  const _Balance({required this.panel, required this.formato});

  static const _textoClaro = Color(0xFFC9D6E6);

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    if (!panel.tieneMovimientos) {
      return const _Seccion(
        icono: Icons.account_balance,
        titulo: 'Balance general',
        contenido: _SinDatos(
            'Aún no registras ingresos ni gastos. Agrégalos en Movimientos.'),
      );
    }

    final negativo = panel.balanceGeneral < 0;
    // Sobre el fondo azul se usan las versiones claras del verde y el rojo
    final color = negativo
        ? ColoresZenfi.negativoSobreOscuro
        : ColoresZenfi.positivoSobreOscuro;

    // Que parte de los ingresos se fue en gastos (sin ingresos no hay anillo)
    final fraccionGastada = panel.totalIngresos > 0
        ? panel.totalGastos / panel.totalIngresos
        : null;

    return Container(
      margin: const EdgeInsets.fromLTRB(12, 8, 12, 6),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [ColoresZenfi.azulMarino, ColoresZenfi.azulProfundo],
        ),
        boxShadow: [
          BoxShadow(
            color: ColoresZenfi.azulProfundo.withValues(alpha: 0.25),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.account_balance,
                            size: 18, color: _textoClaro),
                        const SizedBox(width: 8),
                        Text('Balance general',
                            style: tema.textTheme.titleSmall
                                ?.copyWith(color: _textoClaro)),
                      ],
                    ),
                    const SizedBox(height: 8),
                    FittedBox(
                      fit: BoxFit.scaleDown,
                      alignment: Alignment.centerLeft,
                      child: Text(
                        formato.format(panel.balanceGeneral),
                        style: tema.textTheme.headlineMedium?.copyWith(
                            fontWeight: FontWeight.bold, color: color),
                      ),
                    ),
                    if (negativo)
                      Text('Has gastado más de lo que has recibido.',
                          style: TextStyle(color: color)),
                  ],
                ),
              ),
              if (fraccionGastada != null) ...[
                const SizedBox(width: 12),
                _AnilloSobreOscuro(fraccion: fraccionGastada),
              ],
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: _Dato('Ingresos', formato.format(panel.totalIngresos),
                    icono: Icons.arrow_downward,
                    color: ColoresZenfi.positivoSobreOscuro),
              ),
              Expanded(
                child: _Dato('Gastos', formato.format(panel.totalGastos),
                    icono: Icons.arrow_upward,
                    color: ColoresZenfi.negativoSobreOscuro),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

/// Anillo "gastado de lo que entró", con colores claros para el fondo azul.
class _AnilloSobreOscuro extends StatelessWidget {
  final double fraccion;

  const _AnilloSobreOscuro({required this.fraccion});

  @override
  Widget build(BuildContext context) {
    final Color relleno;
    if (fraccion > 1) {
      relleno = ColoresZenfi.negativoSobreOscuro;
    } else if (fraccion >= 0.8) {
      relleno = const Color(0xFFF2C46D); // ambar claro
    } else {
      relleno = const Color(0xFF7FD1D8); // petroleo claro
    }
    return Tooltip(
      message: 'Parte de tus ingresos que ya gastaste',
      child: AnilloProgreso(
        fraccion: fraccion,
        tamano: 78,
        grosor: 8,
        color: relleno,
        colorPista: Colors.white.withValues(alpha: 0.18),
        centro: _Porcentaje(fraccion * 100,
            tamanoLetra: 16, color: Colors.white, debajo: 'gastado'),
      ),
    );
  }
}

/// Ingresos o gastos dentro de la tarjeta azul del balance.
class _Dato extends StatelessWidget {
  final String etiqueta;
  final String valor;
  final IconData icono;
  final Color color;

  const _Dato(this.etiqueta, this.valor,
      {required this.icono, required this.color});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Row(
      children: [
        Icon(icono, size: 18, color: color),
        const SizedBox(width: 6),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(etiqueta,
                  style: tema.textTheme.labelSmall
                      ?.copyWith(color: _Balance._textoClaro)),
              Text(valor,
                  overflow: TextOverflow.ellipsis,
                  style: tema.textTheme.bodyLarge
                      ?.copyWith(fontWeight: FontWeight.w600, color: color)),
            ],
          ),
        ),
      ],
    );
  }
}

/// HU011 en el panel: anillo con el porcentaje del presupuesto utilizado.
class _PresupuestoDelMes extends StatelessWidget {
  final Presupuesto? presupuesto;
  final String mes;
  final NumberFormat formato;

  const _PresupuestoDelMes({
    required this.presupuesto,
    required this.mes,
    required this.formato,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final p = presupuesto;
    if (p == null) {
      return _Seccion(
        icono: Icons.pie_chart,
        titulo: 'Presupuesto de $mes',
        contenido: _SinDatos(
            'No has definido un presupuesto para $mes. Créalo en Presupuesto.'),
      );
    }

    final fraccion = p.porcentajeUtilizado / 100;
    final colores = coloresSeveridad(fraccion);

    return _Seccion(
      icono: Icons.pie_chart,
      titulo: 'Presupuesto de $mes',
      contenido: Row(
        children: [
          AnilloProgreso(
            fraccion: fraccion,
            tamano: 104,
            grosor: 11,
            color: colores.relleno,
            colorPista: colores.pista,
            centro: _Porcentaje(p.porcentajeUtilizado,
                tamanoLetra: 20, debajo: 'utilizado'),
          ),
          const SizedBox(width: 18),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _Linea('Límite', formato.format(p.montoLimite)),
                _Linea('Gastado', formato.format(p.totalGastado)),
                _Linea(
                  p.excedido ? 'Excedido por' : 'Disponible',
                  formato.format(p.disponible.abs()),
                  color: p.excedido
                      ? ColoresZenfi.negativo
                      : ColoresZenfi.positivo,
                ),
                if (p.excedido)
                  Padding(
                    padding: const EdgeInsets.only(top: 6),
                    child: Row(
                      children: [
                        const Icon(Icons.warning_amber_rounded,
                            size: 18, color: ColoresZenfi.negativo),
                        const SizedBox(width: 4),
                        Expanded(
                          child: Text('Superaste el límite',
                              style: tema.textTheme.bodySmall?.copyWith(
                                  color: ColoresZenfi.negativo,
                                  fontWeight: FontWeight.w600)),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Etiqueta a la izquierda y monto a la derecha.
class _Linea extends StatelessWidget {
  final String etiqueta;
  final String valor;
  final Color? color;

  const _Linea(this.etiqueta, this.valor, {this.color});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          Expanded(
            child: Text(etiqueta,
                style: tema.textTheme.bodyMedium
                    ?.copyWith(color: tema.colorScheme.outline)),
          ),
          Text(valor,
              style: tema.textTheme.bodyMedium
                  ?.copyWith(fontWeight: FontWeight.w600, color: color)),
        ],
      ),
    );
  }
}

/// Dona con los gastos del mes por categoria y su leyenda.
class _GastosDelMes extends StatelessWidget {
  final PanelPrincipal panel;
  final String mes;
  final NumberFormat formato;

  const _GastosDelMes({
    required this.panel,
    required this.mes,
    required this.formato,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    if (panel.gastosMesActual == 0 || panel.gastosMesPorCategoria.isEmpty) {
      return _Seccion(
        icono: Icons.donut_large,
        titulo: 'Gastos de $mes',
        contenido: _SinDatos('No hay gastos registrados en $mes.'),
      );
    }

    final partes = agruparPartes([
      for (final g in panel.gastosMesPorCategoria)
        (nombre: g.categoria, valor: g.total, porcentaje: g.porcentaje),
    ]);

    return _Seccion(
      icono: Icons.donut_large,
      titulo: 'Gastos de $mes',
      contenido: Column(
        children: [
          GraficoDona(
            partes: partes,
            tamano: 160,
            grosor: 24,
            centro: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('Total',
                    style: tema.textTheme.labelSmall
                        ?.copyWith(color: tema.colorScheme.outline)),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 30),
                  child: FittedBox(
                    child: Text(formato.format(panel.gastosMesActual),
                        style: tema.textTheme.titleMedium
                            ?.copyWith(fontWeight: FontWeight.bold)),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          for (final parte in partes)
            FilaLeyenda(
              color: parte.color,
              nombre: parte.nombre,
              monto: formato.format(parte.valor),
              detalle: '${parte.porcentaje.toStringAsFixed(1)} %',
            ),
        ],
      ),
    );
  }
}

class _Metas extends StatelessWidget {
  final List<ResumenMeta> metas;
  final NumberFormat formato;

  const _Metas({required this.metas, required this.formato});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return _Seccion(
      icono: Icons.savings,
      titulo: 'Metas de ahorro activas',
      contenido: metas.isEmpty
          ? const _SinDatos(
              'No tienes metas de ahorro activas. Crea una en Metas de ahorro.')
          : Column(
              children: [
                for (final m in metas)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 6),
                    child: Row(
                      children: [
                        // El avance de una meta es progreso, no un riesgo:
                        // siempre en el color de acento
                        AnilloProgreso(
                          fraccion: m.porcentaje / 100,
                          tamano: 54,
                          grosor: 6,
                          color: ColoresZenfi.petroleo,
                          colorPista: ColoresZenfi.petroleoClaro,
                          centro: _Porcentaje(m.porcentaje),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(m.nombre,
                                  overflow: TextOverflow.ellipsis,
                                  style: tema.textTheme.bodyLarge
                                      ?.copyWith(fontWeight: FontWeight.w600)),
                              Text(
                                '${formato.format(m.montoAhorrado)} de '
                                '${formato.format(m.montoObjetivo)}',
                                style: tema.textTheme.bodyMedium,
                              ),
                              Text(
                                'Límite: ${_formatoFecha.format(m.fechaLimite)}',
                                style: tema.textTheme.bodySmall
                                    ?.copyWith(color: tema.colorScheme.outline),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
    );
  }
}

class _Deudas extends StatelessWidget {
  final List<ResumenDeuda> deudas;
  final NumberFormat formato;

  const _Deudas({required this.deudas, required this.formato});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return _Seccion(
      icono: Icons.account_balance_wallet,
      titulo: 'Deudas pendientes',
      contenido: deudas.isEmpty
          ? const _SinDatos('No tienes deudas pendientes.')
          : Column(
              children: [
                for (final d in deudas)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 6),
                    child: Row(
                      children: [
                        // Cuanto se ha pagado: tambien es avance
                        AnilloProgreso(
                          fraccion: d.porcentajePagado / 100,
                          tamano: 54,
                          grosor: 6,
                          color: ColoresZenfi.petroleo,
                          colorPista: ColoresZenfi.petroleoClaro,
                          centro: _Porcentaje(d.porcentajePagado),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(d.acreedor,
                                  overflow: TextOverflow.ellipsis,
                                  style: tema.textTheme.bodyLarge
                                      ?.copyWith(fontWeight: FontWeight.w600)),
                              Text('Pagado · vence '
                                  '${_formatoFecha.format(d.fechaVencimiento)}',
                                  style: tema.textTheme.bodySmall?.copyWith(
                                      color: tema.colorScheme.outline)),
                            ],
                          ),
                        ),
                        const SizedBox(width: 8),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Text('Saldo',
                                style: tema.textTheme.labelSmall?.copyWith(
                                    color: tema.colorScheme.outline)),
                            Text(formato.format(d.saldoPendiente),
                                style: tema.textTheme.bodyLarge?.copyWith(
                                    fontWeight: FontWeight.w700,
                                    color: ColoresZenfi.negativo)),
                          ],
                        ),
                      ],
                    ),
                  ),
              ],
            ),
    );
  }
}
