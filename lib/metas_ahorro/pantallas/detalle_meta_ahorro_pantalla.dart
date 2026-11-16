import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../comun/api_exception.dart';
import '../modelo/meta_ahorro.dart';
import '../modelo/aporte_meta_ahorro.dart';
import '../servicio/aporte_meta_ahorro_servicio.dart';
import '../servicio/meta_ahorro_servicio.dart';
import '../modelo/recordatorio_meta.dart';
import '../servicio/recordatorio_meta_servicio.dart';

class DetalleMetaAhorroPantalla extends StatefulWidget {
  final MetaAhorro meta;

  const DetalleMetaAhorroPantalla({super.key, required this.meta});

  @override
  State<DetalleMetaAhorroPantalla> createState() =>
      _DetalleMetaAhorroPantallaState();
}

class _DetalleMetaAhorroPantallaState extends State<DetalleMetaAhorroPantalla> {
  final AporteMetaAhorroServicio _aporteServicio = AporteMetaAhorroServicio();

  final MetaAhorroServicio _metaServicio = MetaAhorroServicio();

  final RecordatorioMetaServicio _recordatorioServicio =
      RecordatorioMetaServicio();

  RecordatorioMeta? _recordatorio;

  bool _cargandoRecordatorio = true;
  bool _guardandoRecordatorio = false;

  String? _errorRecordatorio;

  final NumberFormat _moneda = NumberFormat.currency(
    locale: 'es_CO',
    symbol: r'$ ',
    decimalDigits: 0,
  );

  MetaAhorro? _meta;
  List<AporteMetaAhorro> _aportes = [];

  bool _cargando = true;
  bool _procesando = false;
  String? _error;

  double get _progreso {
    final meta = _meta ?? widget.meta;

    if (meta.montoObjetivo <= 0) return 0;

    return (meta.montoAcumulado / meta.montoObjetivo).clamp(0.0, 1.0);
  }

  @override
  void initState() {
    super.initState();
    _meta = widget.meta;
    _cargarDatos();
    _cargarRecordatorio();
  }

  Future<void> _cargarRecordatorio() async {
    setState(() {
      _cargandoRecordatorio = true;
      _errorRecordatorio = null;
    });

    try {
      final recordatorio = await _recordatorioServicio.consultar(
        widget.meta.id,
      );

      if (!mounted) return;

      setState(() {
        _recordatorio = recordatorio;
        _cargandoRecordatorio = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;

      setState(() {
        _errorRecordatorio = e.toString();
        _cargandoRecordatorio = false;
      });
    } catch (_) {
      if (!mounted) return;

      setState(() {
        _errorRecordatorio = 'No se pudo consultar el recordatorio.';
        _cargandoRecordatorio = false;
      });
    }
  }

  Future<void> _guardarRecordatorio({
    required String frecuencia,
    required bool activo,
  }) async {
    setState(() {
      _guardandoRecordatorio = true;
      _errorRecordatorio = null;
    });

    try {
      final actualizado = await _recordatorioServicio.configurar(
        metaAhorroId: widget.meta.id,
        frecuencia: frecuencia,
        activo: activo,
      );

      if (!mounted) return;

      setState(() {
        _recordatorio = actualizado;
        _guardandoRecordatorio = false;
      });

      _mostrarMensaje('Configuración del recordatorio actualizada.');
    } on ApiException catch (e) {
      if (!mounted) return;

      setState(() {
        _errorRecordatorio = e.toString();
        _guardandoRecordatorio = false;
      });

      _mostrarMensaje(e.toString());
    } catch (_) {
      if (!mounted) return;

      setState(() {
        _guardandoRecordatorio = false;
      });

      _mostrarMensaje('No se pudo guardar la configuración.');
    }
  }

  Future<void> _cargarDatos() async {
    setState(() {
      _cargando = true;
      _error = null;
    });

    try {
      final metas = await _metaServicio.listar();

      final metaActual = metas.firstWhere((meta) => meta.id == widget.meta.id);

      final aportes = await _aporteServicio.listar(widget.meta.id);

      if (!mounted) return;

      setState(() {
        _meta = metaActual;
        _aportes = aportes;
        _cargando = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;

      setState(() {
        _error = e.toString();
        _cargando = false;
      });
    } catch (_) {
      if (!mounted) return;

      setState(() {
        _error = 'No se pudo consultar la información de la meta.';
        _cargando = false;
      });
    }
  }

  Future<void> _registrarAporte() async {
    final meta = _meta ?? widget.meta;

    final restante = meta.montoObjetivo - meta.montoAcumulado;

    if (restante <= 0) {
      _mostrarMensaje('Esta meta ya alcanzó su objetivo.');
      return;
    }

    final controlador = TextEditingController();
    DateTime? fechaSeleccionada;

    final confirmar = await showDialog<bool>(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, actualizarDialogo) {
            return AlertDialog(
              title: const Text('Registrar aporte'),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Dinero pendiente: ${_moneda.format(restante)}'),
                  const SizedBox(height: 16),
                  TextField(
                    controller: controlador,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    decoration: const InputDecoration(
                      labelText: 'Monto del aporte',
                      prefixText: r'$ ',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 16),
                  OutlinedButton.icon(
                    onPressed: () async {
                      final fecha = await showDatePicker(
                        context: dialogContext,
                        initialDate: DateTime.now(),
                        firstDate: DateTime(2000),
                        lastDate: DateTime.now(),
                        helpText: 'Fecha del aporte',
                      );

                      if (fecha != null) {
                        actualizarDialogo(() {
                          fechaSeleccionada = fecha;
                        });
                      }
                    },
                    icon: const Icon(Icons.calendar_today),
                    label: Text(
                      fechaSeleccionada == null
                          ? 'Fecha: hoy'
                          : DateFormat('dd/MM/yyyy').format(fechaSeleccionada!),
                    ),
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    'Si no seleccionas una fecha, se utilizará la fecha actual.',
                    style: TextStyle(fontSize: 12),
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(dialogContext, false),
                  child: const Text('Cancelar'),
                ),
                FilledButton(
                  onPressed: () {
                    final monto = double.tryParse(
                      controlador.text.trim().replaceAll(',', '.'),
                    );

                    if (monto == null || monto <= 0) {
                      _mostrarMensaje(
                        'Ingresa un monto válido mayor que cero.',
                      );
                      return;
                    }

                    if (monto > restante) {
                      _mostrarMensaje('El aporte supera el dinero pendiente.');
                      return;
                    }

                    Navigator.pop(dialogContext, true);
                  },
                  child: const Text('Guardar'),
                ),
              ],
            );
          },
        );
      },
    );

    if (confirmar != true) {
      controlador.dispose();
      return;
    }

    final monto = double.parse(controlador.text.trim().replaceAll(',', '.'));

    controlador.dispose();

    setState(() => _procesando = true);

    try {
      await _aporteServicio.registrar(
        metaAhorroId: meta.id,
        monto: monto,
        fecha: fechaSeleccionada,
      );

      await _cargarDatos();

      if (!mounted) return;

      _mostrarMensaje('Aporte registrado correctamente.');
    } on ApiException catch (e) {
      if (!mounted) return;
      _mostrarMensaje(e.toString());
    } catch (_) {
      if (!mounted) return;
      _mostrarMensaje('No se pudo registrar el aporte.');
    } finally {
      if (mounted) {
        setState(() => _procesando = false);
      }
    }
  }

  Future<void> _eliminarAporte(AporteMetaAhorro aporte) async {
    final confirmar = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Eliminar aporte'),
        content: Text(
          '¿Deseas eliminar el aporte de ${_moneda.format(aporte.monto)}?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Eliminar'),
          ),
        ],
      ),
    );

    if (confirmar != true) return;

    setState(() => _procesando = true);

    try {
      await _aporteServicio.eliminar(
        metaAhorroId: widget.meta.id,
        aporteId: aporte.id,
      );

      await _cargarDatos();

      if (!mounted) return;

      _mostrarMensaje('Aporte eliminado correctamente.');
    } on ApiException catch (e) {
      if (!mounted) return;
      _mostrarMensaje(e.toString());
    } catch (_) {
      if (!mounted) return;
      _mostrarMensaje('No se pudo eliminar el aporte.');
    } finally {
      if (mounted) {
        setState(() => _procesando = false);
      }
    }
  }

  Future<void> _eliminarMeta() async {
    final meta = _meta ?? widget.meta;

    final confirmar = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Eliminar meta'),
        content: Text(
          '¿Deseas eliminar la meta "${meta.nombre}"? '
          'También se eliminarán sus aportes y recordatorios. '
          'Esta acción no se puede deshacer.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Eliminar'),
          ),
        ],
      ),
    );

    if (confirmar != true) return;

    setState(() => _procesando = true);

    try {
      await _metaServicio.eliminar(meta.id);

      if (!mounted) return;

      _mostrarMensaje('Meta eliminada correctamente.');

      // Vuelve a la lista, que recarga sus datos al regresar.
      Navigator.pop(context, true);
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _procesando = false);
      _mostrarMensaje(e.mensaje);
    } catch (_) {
      if (!mounted) return;
      setState(() => _procesando = false);
      _mostrarMensaje('No se pudo eliminar la meta.');
    }
  }

  void _mostrarMensaje(String mensaje) {
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(mensaje)));
  }

  Widget _construirConfiguracionRecordatorio() {
    if (_cargandoRecordatorio) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(20),
          child: Center(child: CircularProgressIndicator()),
        ),
      );
    }

    if (_errorRecordatorio != null && _recordatorio == null) {
      return Card(
        child: ListTile(
          title: const Text('Recordatorios'),
          subtitle: Text(_errorRecordatorio!),
          trailing: IconButton(
            onPressed: _cargarRecordatorio,
            icon: const Icon(Icons.refresh),
            tooltip: 'Reintentar',
          ),
        ),
      );
    }

    final recordatorio = _recordatorio!;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Recordatorios',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 8),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Activar recordatorios'),
              subtitle: Text(
                recordatorio.activo
                    ? 'Recibirás avisos para aportar a esta meta.'
                    : 'Los recordatorios están desactivados.',
              ),
              value: recordatorio.activo,
              onChanged: _guardandoRecordatorio
                  ? null
                  : (activo) {
                      _guardarRecordatorio(
                        frecuencia: recordatorio.frecuencia,
                        activo: activo,
                      );
                    },
            ),
            const Divider(),
            const Text('Frecuencia'),
            const SizedBox(height: 8),
            DropdownButtonFormField<String>(
              initialValue: recordatorio.frecuencia,
              decoration: const InputDecoration(border: OutlineInputBorder()),
              items: const [
                DropdownMenuItem(value: 'DIARIA', child: Text('Diaria')),
                DropdownMenuItem(value: 'SEMANAL', child: Text('Semanal')),
                DropdownMenuItem(value: 'QUINCENAL', child: Text('Quincenal')),
                DropdownMenuItem(value: 'MENSUAL', child: Text('Mensual')),
              ],
              onChanged: _guardandoRecordatorio
                  ? null
                  : (frecuencia) {
                      if (frecuencia == null) return;

                      _guardarRecordatorio(
                        frecuencia: frecuencia,
                        activo: recordatorio.activo,
                      );
                    },
            ),
            const SizedBox(height: 12),
            Text(
              'Próximo recordatorio: '
              '${DateFormat('dd/MM/yyyy').format(recordatorio.proximoRecordatorio)}',
            ),
            if (_guardandoRecordatorio) ...[
              const SizedBox(height: 12),
              const LinearProgressIndicator(),
            ],
          ],
        ),
      ),
    );
  }

  Widget _construirResumen() {
    final meta = _meta ?? widget.meta;

    final restante = (meta.montoObjetivo - meta.montoAcumulado).clamp(
      0.0,
      double.infinity,
    );

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(meta.nombre, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            Text('Acumulado: ${_moneda.format(meta.montoAcumulado)}'),
            Text('Objetivo: ${_moneda.format(meta.montoObjetivo)}'),
            Text('Falta: ${_moneda.format(restante)}'),
            const SizedBox(height: 12),
            LinearProgressIndicator(value: _progreso),
            const SizedBox(height: 6),
            Text('${(_progreso * 100).toStringAsFixed(0)} % completado'),
            const SizedBox(height: 8),
            Chip(label: Text(meta.estado)),
            Text(
              'Fecha límite: ${DateFormat('dd/MM/yyyy').format(meta.fechaLimite)}',
            ),
          ],
        ),
      ),
    );
  }

  Widget _construirHistorial() {
    if (_cargando) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: CircularProgressIndicator(),
        ),
      );
    }

    if (_error != null) {
      return Column(
        children: [
          Text(_error!, textAlign: TextAlign.center),
          const SizedBox(height: 8),
          FilledButton(
            onPressed: _cargarDatos,
            child: const Text('Reintentar'),
          ),
        ],
      );
    }

    if (_aportes.isEmpty) {
      return const Padding(
        padding: EdgeInsets.all(20),
        child: Center(child: Text('Todavía no hay aportes registrados.')),
      );
    }

    return Column(
      children: _aportes.map((aporte) {
        return Card(
          child: ListTile(
            leading: const CircleAvatar(child: Icon(Icons.savings_outlined)),
            title: Text(_moneda.format(aporte.monto)),
            subtitle: Text(DateFormat('dd/MM/yyyy').format(aporte.fecha)),
            trailing: IconButton(
              onPressed: _procesando ? null : () => _eliminarAporte(aporte),
              icon: const Icon(Icons.delete_outline),
              tooltip: 'Eliminar aporte',
            ),
          ),
        );
      }).toList(),
    );
  }

  @override
  Widget build(BuildContext context) {
    final meta = _meta ?? widget.meta;

    final objetivoAlcanzado = meta.montoAcumulado >= meta.montoObjetivo;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Detalle de meta'),
        actions: [
          IconButton(
            onPressed: _cargando || _procesando ? null : _cargarDatos,
            icon: const Icon(Icons.refresh),
            tooltip: 'Actualizar',
          ),
          IconButton(
            onPressed: _procesando ? null : _eliminarMeta,
            icon: const Icon(Icons.delete_outline),
            tooltip: 'Eliminar meta',
          ),
        ],
      ),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: _cargarDatos,
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _construirResumen(),
              const SizedBox(height: 16),
              _construirConfiguracionRecordatorio(),
              const SizedBox(height: 20),
              Text(
                'Historial de aportes',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 8),
              _construirHistorial(),
            ],
          ),
        ),
      ),
      floatingActionButton: objetivoAlcanzado
          ? null
          : FloatingActionButton.extended(
              onPressed: _procesando || _cargando ? null : _registrarAporte,
              icon: const Icon(Icons.add),
              label: const Text('Registrar aporte'),
            ),
    );
  }
}
