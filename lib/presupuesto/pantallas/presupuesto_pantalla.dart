import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../comun/cambios_financieros.dart';
import '../../comun/moneda.dart';
import '../../reporte/modelo/reporte_mensual.dart';
import '../../reporte/servicio/reporte_servicio.dart';
import '../../reporte/widgets/desglose_categorias.dart';
import '../modelo/mes.dart';
import '../modelo/presupuesto.dart';
import 'formulario_presupuesto_pantalla.dart';
import '../../comun/menu_lateral.dart';
import '../../comun/tema.dart';

/// HU011: presupuesto mensual con lo gastado y el porcentaje utilizado.
/// HU025: debajo, el reporte del mes con los gastos por categoria.
///
/// Pide el reporte mensual (una sola peticion): trae el presupuesto, si
/// existe, y el desglose, calculados con el mismo total gastado.
///
/// Escucha CambiosFinancieros: cuando se registra, modifica o elimina un
/// movimiento (o un abono) en otra pestana, el porcentaje se recalcula
/// solo, como pide el criterio de aceptacion.
class PresupuestoPantalla extends StatefulWidget {
  final Moneda moneda;

  const PresupuestoPantalla({super.key, this.moneda = Moneda.cop});

  @override
  State<PresupuestoPantalla> createState() => _PresupuestoPantallaState();
}

class _PresupuestoPantallaState extends State<PresupuestoPantalla> {
  final _servicio = ReporteServicio();
  DateTime _mes = mesActual();
  late Future<ReporteMensual> _futuro;

  NumberFormat get _formato => widget.moneda.formato;

  @override
  void initState() {
    super.initState();
    _futuro = _servicio.consultar(_mes);
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
      _futuro = _servicio.consultar(_mes);
    });
  }

  void _cambiarMes(int meses) {
    setState(() {
      _mes = DateTime(_mes.year, _mes.month + meses);
      _futuro = _servicio.consultar(_mes);
    });
  }

  Future<void> _abrirFormulario() async {
    final guardado = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (_) => FormularioPresupuestoPantalla(
          mesInicial: _mes,
          moneda: widget.moneda,
        ),
      ),
    );
    if (guardado == true && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: const Text('Presupuesto guardado'),
        backgroundColor: ColoresZenfi.positivo,
      ));
      _recargar();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: const BotonMenu(),
        title: const Text('Presupuesto mensual'),
        actions: [
          IconButton(
            onPressed: _recargar,
            icon: const Icon(Icons.refresh),
            tooltip: 'Recargar',
          ),
        ],
      ),
      body: Column(
        children: [
          _SelectorMes(
            mes: _mes,
            onAnterior: () => _cambiarMes(-1),
            onSiguiente: () => _cambiarMes(1),
          ),
          Expanded(
            child: FutureBuilder<ReporteMensual>(
              future: _futuro,
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const Center(child: CircularProgressIndicator());
                }

                if (snapshot.hasError) {
                  return _EstadoVacio(
                    icono: Icons.cloud_off,
                    titulo: 'No se pudo conectar',
                    mensaje: 'Verifica que Quarkus este corriendo.\n\n'
                        '${snapshot.error}',
                    accion: FilledButton.icon(
                      onPressed: _recargar,
                      icon: const Icon(Icons.refresh),
                      label: const Text('Reintentar'),
                    ),
                  );
                }

                final reporte = snapshot.data!;
                final presupuesto = reporte.presupuesto;

                return RefreshIndicator(
                  onRefresh: () async => _recargar(),
                  child: ListView(
                    padding: const EdgeInsets.only(top: 4, bottom: 24),
                    children: [
                      if (presupuesto != null)
                        _TarjetaPresupuesto(
                          presupuesto: presupuesto,
                          formato: _formato,
                        )
                      else
                        _SinPresupuesto(
                          mes: _mes,
                          onDefinir: _abrirFormulario,
                        ),
                      DesgloseCategorias(
                        reporte: reporte,
                        mes: _mes,
                        formato: _formato,
                      ),
                    ],
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _SelectorMes extends StatelessWidget {
  final DateTime mes;
  final VoidCallback onAnterior;
  final VoidCallback onSiguiente;

  const _SelectorMes({
    required this.mes,
    required this.onAnterior,
    required this.onSiguiente,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            onPressed: onAnterior,
            icon: const Icon(Icons.chevron_left),
            tooltip: 'Mes anterior',
          ),
          Expanded(
            child: Text(
              nombreMes(mes),
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ),
          IconButton(
            onPressed: onSiguiente,
            icon: const Icon(Icons.chevron_right),
            tooltip: 'Mes siguiente',
          ),
        ],
      ),
    );
  }
}

class _TarjetaPresupuesto extends StatelessWidget {
  final Presupuesto presupuesto;
  final NumberFormat formato;

  const _TarjetaPresupuesto({required this.presupuesto, required this.formato});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final excedido = presupuesto.excedido;
    // HU011: si el gasto supera el limite, el porcentaje se muestra como advertencia
    final color = excedido ? ColoresZenfi.negativo : tema.colorScheme.primary;

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Gastado en el mes',
              style: tema.textTheme.labelMedium
                  ?.copyWith(color: tema.colorScheme.outline),
            ),
            Text(
              formato.format(presupuesto.totalGastado),
              style: tema.textTheme.headlineSmall
                  ?.copyWith(fontWeight: FontWeight.bold, color: color),
            ),
            const SizedBox(height: 12),

            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: LinearProgressIndicator(
                value: presupuesto.progreso,
                minHeight: 12,
                color: color,
                backgroundColor: tema.colorScheme.surfaceContainerHighest,
              ),
            ),
            const SizedBox(height: 6),
            Row(
              children: [
                if (excedido) ...[
                  Icon(Icons.warning_amber_rounded, size: 18, color: color),
                  const SizedBox(width: 4),
                ],
                Text(
                  '${presupuesto.porcentajeUtilizado.toStringAsFixed(1)}% utilizado',
                  style: tema.textTheme.bodyMedium?.copyWith(
                    color: color,
                    fontWeight: excedido ? FontWeight.bold : null,
                  ),
                ),
              ],
            ),

            if (excedido) ...[
              const SizedBox(height: 12),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: ColoresZenfi.negativoFondo,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  'Superaste tu presupuesto por '
                  '${formato.format(-presupuesto.disponible)}.',
                  style: TextStyle(color: ColoresZenfi.negativoTexto),
                ),
              ),
            ],

            const Divider(height: 28),

            Wrap(
              spacing: 24,
              runSpacing: 10,
              children: [
                _Dato('Limite', formato.format(presupuesto.montoLimite)),
                _Dato(
                  excedido ? 'Excedido por' : 'Disponible',
                  formato.format(presupuesto.disponible.abs()),
                  color: excedido ? ColoresZenfi.negativo : ColoresZenfi.positivo,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// HU025: el mes no tiene presupuesto, pero el desglose por categoria se
/// muestra igual debajo. Solo se invita a definirlo si el mes no ha pasado.
class _SinPresupuesto extends StatelessWidget {
  final DateTime mes;
  final VoidCallback onDefinir;

  const _SinPresupuesto({required this.mes, required this.onDefinir});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final pasado = esMesPasado(mes);

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.pie_chart_outline, color: tema.colorScheme.outline),
                const SizedBox(width: 8),
                Expanded(
                  child: Text('Sin presupuesto para ${nombreMes(mes)}',
                      style: tema.textTheme.titleMedium),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              pasado
                  ? 'No hay presupuesto para comparar. Solo se puede definir '
                      'para el mes actual o meses futuros.'
                  : 'No hay presupuesto para comparar. Define cuanto quieres '
                      'gastar como maximo y Zenfi te mostrara que porcentaje '
                      'llevas.',
            ),
            if (!pasado) ...[
              const SizedBox(height: 12),
              FilledButton.icon(
                onPressed: onDefinir,
                icon: const Icon(Icons.add),
                label: const Text('Definir presupuesto'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _Dato extends StatelessWidget {
  final String etiqueta;
  final String valor;
  final Color? color;

  const _Dato(this.etiqueta, this.valor, {this.color});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          etiqueta,
          style: tema.textTheme.labelSmall
              ?.copyWith(color: tema.colorScheme.outline),
        ),
        Text(
          valor,
          style: tema.textTheme.bodyLarge
              ?.copyWith(fontWeight: FontWeight.w600, color: color),
        ),
      ],
    );
  }
}

class _EstadoVacio extends StatelessWidget {
  final IconData icono;
  final String titulo;
  final String mensaje;
  final Widget? accion;

  const _EstadoVacio({
    required this.icono,
    required this.titulo,
    required this.mensaje,
    this.accion,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icono, size: 64, color: Theme.of(context).colorScheme.outline),
            const SizedBox(height: 16),
            Text(titulo,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Text(mensaje, textAlign: TextAlign.center),
            if (accion != null) ...[const SizedBox(height: 24), accion!],
          ],
        ),
      ),
    );
  }
}
