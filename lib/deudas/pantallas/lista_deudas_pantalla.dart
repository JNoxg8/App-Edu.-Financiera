import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../comun/cambios_financieros.dart';
import '../modelo/deuda.dart';
import '../servicio/deuda_servicio.dart';
import 'formulario_deuda_pantalla.dart';

final formatoMoneda =
    NumberFormat.currency(locale: 'es_CO', symbol: '\$', decimalDigits: 0);
final formatoFecha = DateFormat('dd/MM/yyyy');

/// RQ-07: listado de deudas activas con su saldo pendiente actualizado.
class ListaDeudasPantalla extends StatefulWidget {
  const ListaDeudasPantalla({super.key});

  @override
  State<ListaDeudasPantalla> createState() => _ListaDeudasPantallaState();
}

class _ListaDeudasPantallaState extends State<ListaDeudasPantalla> {
  final _servicio = DeudaServicio();
  late Future<List<Deuda>> _futuro;

  @override
  void initState() {
    super.initState();
    _futuro = _servicio.listar();
    // HU009: un gasto en la categoria Deudas abona una deuda desde la
    // pestana Movimientos; asi esta lista muestra el saldo actualizado.
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
      _futuro = _servicio.listar();
    });
  }

  void _mostrarMensaje(String texto, {bool error = false}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(texto),
      backgroundColor: error ? Colors.red.shade700 : Colors.green.shade700,
    ));
  }

  Future<void> _abrirFormulario({Deuda? deuda}) async {
    final guardada = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (_) => FormularioDeudaPantalla(deuda: deuda),
      ),
    );
    if (guardada == true) {
      _mostrarMensaje(deuda == null ? 'Deuda registrada' : 'Deuda actualizada');
      _recargar();
    }
  }

  Future<void> _abrirDialogoAbono(Deuda deuda) async {
    final controlador = TextEditingController();

    final monto = await showDialog<double>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Abonar a ${deuda.acreedor}'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
                'Saldo pendiente: ${formatoMoneda.format(deuda.saldoPendiente)}'),
            const SizedBox(height: 12),
            TextField(
              controller: controlador,
              keyboardType:
              const TextInputType.numberWithOptions(decimal: true),
              autofocus: true,
              decoration: const InputDecoration(
                labelText: 'Monto del abono',
                prefixText: '\$ ',
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () {
              final valor =
              double.tryParse(controlador.text.trim().replaceAll(',', '.'));
              if (valor != null && valor > 0) Navigator.pop(ctx, valor);
            },
            child: const Text('Abonar'),
          ),
        ],
      ),
    );

    if (monto == null) return;

    bool exito = false;
    try {
      await _servicio.abonar(deudaId: deuda.id, monto: monto);
      exito = true;
    } on ApiException catch (e) {
      // toString() incluye los detalles de validacion (error.detalles)
      _mostrarMensaje(e.toString(), error: true);
    } catch (_) {
      _mostrarMensaje('No se pudo conectar con el servidor', error: true);
    }

    if (exito) {
      _mostrarMensaje('Abono registrado');
      // Recarga esta lista y avisa a Movimientos: el abono es un gasto
      CambiosFinancieros.notificar();
    }
  }



  Future<void> _eliminar(Deuda deuda) async {
    final confirma = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Eliminar deuda'),
        content: Text('Se eliminara la deuda con ${deuda.acreedor}.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Eliminar'),
          ),
        ],
      ),
    );

    if (confirma != true) return;

    bool exito = false;
    try {
      await _servicio.eliminar(deuda.id);
      exito = true;
    } on ApiException catch (e) {
      _mostrarMensaje(e.mensaje, error: true);
    } catch (_) {
      _mostrarMensaje('No se pudo eliminar la deuda', error: true);
    }

    if (exito) {
      _mostrarMensaje('Deuda eliminada');
      // Sus gastos en Movimientos quedan desvinculados de la deuda
      CambiosFinancieros.notificar();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mis deudas'),
        actions: [
          IconButton(
            onPressed: _recargar,
            icon: const Icon(Icons.refresh),
            tooltip: 'Recargar',
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _abrirFormulario,
        icon: const Icon(Icons.add),
        label: const Text('Nueva deuda'),
      ),
      body: FutureBuilder<List<Deuda>>(
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

          final deudas = snapshot.data ?? [];

          if (deudas.isEmpty) {
            return const _EstadoVacio(
              icono: Icons.check_circle_outline,
              titulo: 'Sin deudas registradas',
              mensaje: 'Presiona "Nueva deuda" para agregar la primera.',
            );
          }

          return RefreshIndicator(
            onRefresh: () async => _recargar(),
            child: ListView.builder(
              padding: const EdgeInsets.only(bottom: 88, top: 4),
              itemCount: deudas.length,
              itemBuilder: (_, i) => _TarjetaDeuda(
                deuda: deudas[i],
                onAbonar: () => _abrirDialogoAbono(deudas[i]),
                onEditar: () => _abrirFormulario(deuda: deudas[i]),
                onEliminar: () => _eliminar(deudas[i]),
              ),
            ),
          );
        },
      ),
    );
  }
}

class _TarjetaDeuda extends StatelessWidget {
  final Deuda deuda;
  final VoidCallback onAbonar;
  final VoidCallback onEditar;
  final VoidCallback onEliminar;

  const _TarjetaDeuda({
    required this.deuda,
    required this.onAbonar,
    required this.onEditar,
    required this.onEliminar,
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
                Expanded(
                  child: Text(
                    deuda.acreedor,
                    style: tema.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.bold),
                  ),
                ),
                if (deuda.pagada)
                  const Chip(
                    label: Text('Pagada'),
                    avatar: Icon(Icons.check, size: 16),
                    visualDensity: VisualDensity.compact,
                  ),
                IconButton(
                  onPressed: onEditar,
                  icon: const Icon(Icons.edit_outlined),
                  tooltip: 'Editar',
                ),
                IconButton(
                  onPressed: onEliminar,
                  icon: const Icon(Icons.delete_outline),
                  tooltip: 'Eliminar',
                ),
              ],
            ),

            const SizedBox(height: 8),

            // RQ-07: saldo pendiente actualizado automaticamente
            Text(
              'Saldo pendiente',
              style: tema.textTheme.labelMedium
                  ?.copyWith(color: tema.colorScheme.outline),
            ),
            Text(
              formatoMoneda.format(deuda.saldoPendiente),
              style: tema.textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.bold,
                color: deuda.pagada
                    ? Colors.green.shade700
                    : tema.colorScheme.primary,
              ),
            ),

            const SizedBox(height: 12),

            ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: LinearProgressIndicator(
                value: deuda.progreso,
                minHeight: 10,
                backgroundColor: tema.colorScheme.surfaceContainerHighest,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              '${deuda.porcentajePagado.toStringAsFixed(1)}% pagado  -  '
              'Abonado ${formatoMoneda.format(deuda.totalAbonado)}',
              style: tema.textTheme.bodySmall,
            ),

            const Divider(height: 24),

            Wrap(
              spacing: 20,
              runSpacing: 10,
              children: [
                _Dato('Cuota mensual',
                    formatoMoneda.format(deuda.cuotaMensual)),
                _Dato('Total a pagar',
                    formatoMoneda.format(deuda.totalAPagar)),
                _Dato('Intereses', formatoMoneda.format(deuda.costoIntereses),
                    resaltado: true),
                _Dato('Tasa',
                    '${deuda.tasaInteresAnual.toStringAsFixed(2)}% E.A.'),
                _Dato('Cuotas', '${deuda.numeroCuotas}'),
                _Dato('Vence', formatoFecha.format(deuda.fechaVencimiento)),
              ],
            ),

            const SizedBox(height: 16),

            SizedBox(
              width: double.infinity,
              child: FilledButton.tonalIcon(
                onPressed: deuda.pagada ? null : onAbonar,
                icon: const Icon(Icons.payments_outlined),
                label: Text(deuda.pagada ? 'Deuda saldada' : 'Registrar abono'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Dato extends StatelessWidget {
  final String etiqueta;
  final String valor;
  final bool resaltado;

  const _Dato(this.etiqueta, this.valor, {this.resaltado = false});

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
          style: tema.textTheme.bodyMedium?.copyWith(
            fontWeight: FontWeight.w600,
            color: resaltado ? Colors.orange.shade800 : null,
          ),
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
            Text(titulo, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Text(mensaje, textAlign: TextAlign.center),
            if (accion != null) ...[const SizedBox(height: 24), accion!],
          ],
        ),
      ),
    );
  }
}
