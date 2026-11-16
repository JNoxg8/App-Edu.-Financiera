import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../comun/api_exception.dart';
import '../modelo/meta_ahorro.dart';
import '../servicio/meta_ahorro_servicio.dart';
import 'formulario_meta_ahorro_pantalla.dart';
import 'detalle_meta_ahorro_pantalla.dart';
import '../../comun/menu_lateral.dart';

class ListaMetasAhorroPantalla extends StatefulWidget {
  final MetaAhorroServicio? servicio;

  const ListaMetasAhorroPantalla({super.key, this.servicio});

  @override
  State<ListaMetasAhorroPantalla> createState() =>
      _ListaMetasAhorroPantallaState();
}

class _ListaMetasAhorroPantallaState extends State<ListaMetasAhorroPantalla> {
  late final MetaAhorroServicio _servicio =
      widget.servicio ?? MetaAhorroServicio();

  final _moneda = NumberFormat.currency(
    locale: 'es_CO',
    symbol: r'$ ',
    decimalDigits: 0,
  );

  List<MetaAhorro> _metas = [];
  bool _cargando = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _cargarMetas();
  }

  Future<void> _cargarMetas() async {
    setState(() {
      _cargando = true;
      _error = null;
    });

    try {
      final metas = await _servicio.listar();

      if (!mounted) return;

      setState(() {
        _metas = metas;
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
        _error = 'No se pudo conectar con el servidor.';
        _cargando = false;
      });
    }
  }

  Future<void> _abrirFormulario() async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const FormularioMetaAhorroPantalla()),
    );

    if (mounted) {
      _cargarMetas();
    }
  }

  Future<void> _abrirDetalle(MetaAhorro meta) async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => DetalleMetaAhorroPantalla(meta: meta)),
    );

    if (mounted) {
      _cargarMetas();
    }
  }

  Future<void> _eliminarMeta(MetaAhorro meta) async {
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

    try {
      await _servicio.eliminar(meta.id);

      if (!mounted) return;

      _mostrarMensaje('Meta eliminada correctamente.');
      await _cargarMetas();
    } on ApiException catch (e) {
      if (!mounted) return;
      _mostrarMensaje(e.mensaje);
    } catch (_) {
      if (!mounted) return;
      _mostrarMensaje('No se pudo eliminar la meta.');
    }
  }

  void _mostrarMensaje(String mensaje) {
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(mensaje)));
  }

  Widget _tarjetaMeta(MetaAhorro meta) {
    final tema = Theme.of(context);

    final progreso = meta.montoObjetivo > 0
        ? (meta.montoAcumulado / meta.montoObjetivo).clamp(0.0, 1.0)
        : 0.0;

    final porcentaje = (progreso * 100).toStringAsFixed(0);

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  Icons.savings_outlined,
                  color: tema.colorScheme.primary,
                  size: 30,
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    meta.nombre,
                    style: tema.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                Chip(label: Text(meta.estado)),
              ],
            ),
            const SizedBox(height: 12),
            Text('Acumulado: ${_moneda.format(meta.montoAcumulado)}'),
            Text('Objetivo: ${_moneda.format(meta.montoObjetivo)}'),
            const SizedBox(height: 10),
            LinearProgressIndicator(value: progreso),
            const SizedBox(height: 5),
            Text('$porcentaje % completado'),
            const SizedBox(height: 8),
            Text(
              'Fecha límite: ${DateFormat('dd/MM/yyyy').format(meta.fechaLimite)}',
              style: tema.textTheme.bodySmall,
            ),
            const SizedBox(height: 8),
            Align(
              alignment: Alignment.centerRight,
              child: Wrap(
                spacing: 4,
                children: [
                  TextButton.icon(
                    onPressed: () => _eliminarMeta(meta),
                    style: TextButton.styleFrom(foregroundColor: Colors.red),
                    icon: const Icon(Icons.delete_outline),
                    label: const Text('Eliminar'),
                  ),
                  TextButton.icon(
                    onPressed: () => _abrirDetalle(meta),
                    icon: const Icon(Icons.arrow_forward),
                    label: const Text('Ver meta'),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: const BotonMenu(),
        title: const Text('Mis metas de ahorro'),
        actions: [
          IconButton(
            onPressed: _cargando ? null : _cargarMetas,
            icon: const Icon(Icons.refresh),
            tooltip: 'Actualizar metas',
          ),
        ],
      ),
      body: SafeArea(
        child: _cargando
            ? const Center(child: CircularProgressIndicator())
            : _error != null
            ? Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.error_outline, size: 42),
                      const SizedBox(height: 12),
                      Text(_error!, textAlign: TextAlign.center),
                      const SizedBox(height: 12),
                      FilledButton.icon(
                        onPressed: _cargarMetas,
                        icon: const Icon(Icons.refresh),
                        label: const Text('Reintentar'),
                      ),
                    ],
                  ),
                ),
              )
            : _metas.isEmpty
            ? const Center(
                child: Padding(
                  padding: EdgeInsets.all(24),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.savings_outlined, size: 52),
                      SizedBox(height: 12),
                      Text(
                        'Todavía no tienes metas de ahorro.',
                        textAlign: TextAlign.center,
                      ),
                      SizedBox(height: 6),
                      Text(
                        'Crea una meta para comenzar a ahorrar.',
                        textAlign: TextAlign.center,
                      ),
                    ],
                  ),
                ),
              )
            : RefreshIndicator(
                onRefresh: _cargarMetas,
                child: ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: _metas.length,
                  itemBuilder: (context, index) => _tarjetaMeta(_metas[index]),
                ),
              ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _abrirFormulario,
        icon: const Icon(Icons.add),
        label: const Text('Crear meta'),
      ),
    );
  }
}
