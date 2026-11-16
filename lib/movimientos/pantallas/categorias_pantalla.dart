import 'package:flutter/material.dart';

import '../../comun/cambios_financieros.dart';
import '../modelo/movimiento.dart';
import '../servicio/movimiento_servicio.dart';

/// HU009: categorias de ingresos y gastos.
///
/// Las predefinidas se muestran con candado (no se editan ni borran).
/// Las personalizadas se pueden crear, renombrar y eliminar; el backend
/// rechaza nombres repetidos y borrar categorias con movimientos.
class CategoriasPantalla extends StatefulWidget {
  const CategoriasPantalla({super.key});

  @override
  State<CategoriasPantalla> createState() => _CategoriasPantallaState();
}

class _CategoriasPantallaState extends State<CategoriasPantalla> {
  final _servicio = MovimientoServicio();
  TipoMovimiento _tipo = TipoMovimiento.gasto;
  late Future<List<Categoria>> _futuro;

  @override
  void initState() {
    super.initState();
    _futuro = _servicio.listarCategorias(tipo: _tipo);
  }

  void _recargar() {
    if (!mounted) return;
    setState(() {
      _futuro = _servicio.listarCategorias(tipo: _tipo);
    });
  }

  void _mostrarMensaje(String texto, {bool error = false}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(texto),
      backgroundColor: error ? Colors.red.shade700 : Colors.green.shade700,
    ));
  }

  /// Dialogo con un campo de nombre. Devuelve null si se cancela.
  Future<String?> _pedirNombre({required String titulo, String inicial = ''}) {
    final controlador = TextEditingController(text: inicial);
    final clave = GlobalKey<FormState>();

    return showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(titulo),
        content: Form(
          key: clave,
          child: TextFormField(
            controller: controlador,
            autofocus: true,
            maxLength: 40,
            textCapitalization: TextCapitalization.sentences,
            decoration: const InputDecoration(
              labelText: 'Nombre de la categoría',
              border: OutlineInputBorder(),
            ),
            validator: (v) => (v == null || v.trim().isEmpty)
                ? 'El nombre es obligatorio'
                : null,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () {
              if (clave.currentState!.validate()) {
                Navigator.pop(ctx, controlador.text.trim());
              }
            },
            child: const Text('Guardar'),
          ),
        ],
      ),
    );
  }

  Future<void> _crear() async {
    final nombre = await _pedirNombre(
        titulo: 'Nueva categoría de ${_tipo.nombre.toLowerCase()}s');
    if (nombre == null) return;

    try {
      final creada = await _servicio.crearCategoria(nombre: nombre, tipo: _tipo);
      _mostrarMensaje('Categoría "${creada.nombre}" creada');
      _recargar();
    } on ApiException catch (e) {
      _mostrarMensaje(e.toString(), error: true);
    } catch (_) {
      _mostrarMensaje('No se pudo conectar con el servidor', error: true);
    }
  }

  Future<void> _renombrar(Categoria c) async {
    final nombre =
        await _pedirNombre(titulo: 'Renombrar categoría', inicial: c.nombre);
    if (nombre == null || nombre == c.nombre) return;

    try {
      await _servicio.renombrarCategoria(id: c.id, nombre: nombre);
      _mostrarMensaje('Categoría actualizada');
      _recargar();
    } on ApiException catch (e) {
      _mostrarMensaje(e.toString(), error: true);
    } catch (_) {
      _mostrarMensaje('No se pudo conectar con el servidor', error: true);
    }
  }

  Future<void> _eliminar(Categoria c) async {
    final confirma = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Eliminar categoría'),
        // HU019: los movimientos de la categoria pasan a "Otros"
        content: Text(
          'Se eliminará la categoría "${c.nombre}".\n\n'
          'Los movimientos que tenga asociados pasarán a la categoría "Otros".',
        ),
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

    try {
      await _servicio.eliminarCategoria(c.id);
      _mostrarMensaje('Categoría eliminada');
      _recargar();
      // Sus movimientos cambiaron de categoria: la lista y el reporte se recargan
      CambiosFinancieros.notificar();
    } on ApiException catch (e) {
      // 409 si es predefinida
      _mostrarMensaje(e.mensaje, error: true);
    } catch (_) {
      _mostrarMensaje('No se pudo eliminar la categoría', error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Categorías')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _crear,
        icon: const Icon(Icons.add),
        label: const Text('Nueva categoría'),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: SizedBox(
              width: double.infinity,
              child: SegmentedButton<TipoMovimiento>(
                segments: const [
                  ButtonSegment(
                    value: TipoMovimiento.gasto,
                    label: Text('Gastos'),
                  ),
                  ButtonSegment(
                    value: TipoMovimiento.ingreso,
                    label: Text('Ingresos'),
                  ),
                ],
                selected: {_tipo},
                onSelectionChanged: (s) {
                  _tipo = s.first;
                  _recargar();
                },
              ),
            ),
          ),
          Expanded(
            child: FutureBuilder<List<Categoria>>(
              future: _futuro,
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const Center(child: CircularProgressIndicator());
                }
                if (snapshot.hasError) {
                  return Center(
                    child: FilledButton.icon(
                      onPressed: _recargar,
                      icon: const Icon(Icons.refresh),
                      label: const Text('Reintentar'),
                    ),
                  );
                }

                final categorias = snapshot.data ?? [];
                return ListView(
                  padding: const EdgeInsets.only(bottom: 88),
                  children: [
                    for (final c in categorias)
                      Card(
                        margin: const EdgeInsets.symmetric(
                            horizontal: 12, vertical: 4),
                        child: ListTile(
                          leading: Icon(
                            c.requiereDeuda
                                ? Icons.account_balance_outlined
                                : Icons.label_outline,
                            color: tema.colorScheme.primary,
                          ),
                          title: Text(c.nombre),
                          subtitle: Text(c.predefinida
                              ? (c.requiereDeuda
                                  ? 'Predefinida · Se enlaza con tus deudas'
                                  : 'Predefinida')
                              : 'Personalizada'),
                          trailing: c.predefinida
                              ? Icon(Icons.lock_outline,
                                  color: tema.colorScheme.outline)
                              : Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    IconButton(
                                      onPressed: () => _renombrar(c),
                                      icon: const Icon(Icons.edit_outlined),
                                      tooltip: 'Renombrar',
                                    ),
                                    IconButton(
                                      onPressed: () => _eliminar(c),
                                      icon: const Icon(Icons.delete_outline),
                                      tooltip: 'Eliminar',
                                    ),
                                  ],
                                ),
                        ),
                      ),
                  ],
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
