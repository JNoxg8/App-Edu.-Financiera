import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../comun/cambios_financieros.dart';
import '../../comun/moneda.dart';
import '../../presupuesto/modelo/mes.dart';
import '../modelo/panel_principal.dart';
import '../servicio/panel_servicio.dart';
import '../../comun/menu_lateral.dart';
import '../../comun/tema.dart';

final _formatoFecha = DateFormat('dd/MM/yyyy');

/// HU015: panel principal. Es la primera pestana que se ve al entrar.
///
/// Una sola peticion (GET /panel) trae las cuatro secciones. Escucha
/// CambiosFinancieros, asi que se actualiza solo al registrar, modificar o
/// eliminar movimientos, aportes, abonos, metas o deudas en otra pestana.
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
          return RefreshIndicator(
            onRefresh: () async => _recargar(),
            child: ListView(
              padding: const EdgeInsets.only(top: 4, bottom: 24),
              children: [
                _Balance(panel: panel, formato: _formato),
                _GastosDelMes(panel: panel, formato: _formato),
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

/// Tarjeta con titulo e icono, comun a las cuatro secciones.
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
            const SizedBox(height: 12),
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

class _Balance extends StatelessWidget {
  final PanelPrincipal panel;
  final NumberFormat formato;

  const _Balance({required this.panel, required this.formato});

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
    const textoClaro = Color(0xFFC9D6E6);

    // Tarjeta destacada: el balance es el dato principal del panel
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
            children: [
              const Icon(Icons.account_balance, size: 18, color: textoClaro),
              const SizedBox(width: 8),
              Text('Balance general',
                  style: tema.textTheme.titleSmall?.copyWith(color: textoClaro)),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            formato.format(panel.balanceGeneral),
            style: tema.textTheme.headlineMedium
                ?.copyWith(fontWeight: FontWeight.bold, color: color),
          ),
          if (negativo)
            Text('Has gastado más de lo que has recibido.',
                style: TextStyle(color: color)),
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

class _GastosDelMes extends StatelessWidget {
  final PanelPrincipal panel;
  final NumberFormat formato;

  const _GastosDelMes({required this.panel, required this.formato});

  @override
  Widget build(BuildContext context) {
    final mes = nombreMes(DateTime.parse('${panel.mes}-01'));
    return _Seccion(
      icono: Icons.calendar_month,
      titulo: 'Gastos de $mes',
      contenido: panel.gastosMesActual == 0
          ? _SinDatos('No hay gastos registrados en $mes.')
          : Text(
              formato.format(panel.gastosMesActual),
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.bold, color: ColoresZenfi.negativo),
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
              'No tienes metas de ahorro activas. Crea una en Metas.')
          : Column(
              children: [
                for (final m in metas)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(
                                child: Text(m.nombre,
                                    style: tema.textTheme.bodyLarge)),
                            Text('${m.porcentaje.toStringAsFixed(0)} %',
                                style: tema.textTheme.bodyLarge?.copyWith(
                                    fontWeight: FontWeight.w700,
                                    color: tema.colorScheme.secondary)),
                          ],
                        ),
                        const SizedBox(height: 4),
                        ClipRRect(
                          borderRadius: BorderRadius.circular(8),
                          child: LinearProgressIndicator(
                            value: (m.porcentaje / 100).clamp(0.0, 1.0),
                            minHeight: 8,
                            backgroundColor:
                                tema.colorScheme.surfaceContainerHighest,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          '${formato.format(m.montoAhorrado)} de '
                          '${formato.format(m.montoObjetivo)} · '
                          'límite ${_formatoFecha.format(m.fechaLimite)}',
                          style: tema.textTheme.bodySmall
                              ?.copyWith(color: tema.colorScheme.outline),
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
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text(d.acreedor),
                    subtitle: Text(
                      '${d.porcentajePagado.toStringAsFixed(0)} % pagado · '
                      'vence ${_formatoFecha.format(d.fechaVencimiento)}',
                    ),
                    trailing: Text(
                      formato.format(d.saldoPendiente),
                      style: tema.textTheme.bodyLarge?.copyWith(
                          fontWeight: FontWeight.w700,
                          color: ColoresZenfi.negativo),
                    ),
                  ),
              ],
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
                      ?.copyWith(color: const Color(0xFFC9D6E6))),
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
