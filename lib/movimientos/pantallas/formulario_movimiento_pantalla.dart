import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../comun/moneda.dart';
import '../../deudas/modelo/deuda.dart';
import '../../deudas/servicio/deuda_servicio.dart';
import '../modelo/movimiento.dart';
import '../servicio/movimiento_servicio.dart';

/// HU009: registrar (o editar) un ingreso o gasto.
///
/// Si la categoria elegida es "Deudas" aparece el selector de la deuda:
/// el gasto se registra como un abono y baja el saldo de esa deuda.
///
/// Si recibe un movimiento que es abono a una deuda, entra en modo
/// edicion restringida: solo se puede cambiar la descripcion.
class FormularioMovimientoPantalla extends StatefulWidget {
  final Movimiento? movimiento;
  final Moneda moneda;

  /// Tipo con el que abre el formulario al crear.
  final TipoMovimiento tipoInicial;

  const FormularioMovimientoPantalla({
    super.key,
    this.movimiento,
    this.moneda = Moneda.cop,
    this.tipoInicial = TipoMovimiento.gasto,
  });

  bool get esEdicion => movimiento != null;

  /// Abonos a deudas: monto, fecha, tipo y categoria los manda la deuda.
  bool get soloDescripcion => movimiento?.abonoDeuda ?? false;

  @override
  State<FormularioMovimientoPantalla> createState() =>
      _FormularioMovimientoPantallaState();
}

class _FormularioMovimientoPantallaState
    extends State<FormularioMovimientoPantalla> {
  final _formKey = GlobalKey<FormState>();
  final _servicio = MovimientoServicio();
  final _deudaServicio = DeudaServicio();

  final _monto = TextEditingController();
  final _descripcion = TextEditingController();

  late TipoMovimiento _tipo;
  DateTime _fecha = DateTime.now();
  Categoria? _categoria;
  Deuda? _deuda;

  List<Categoria> _categorias = [];
  List<Deuda> _deudas = [];
  bool _cargandoCategorias = true;
  bool _cargandoDeudas = false;
  bool _guardando = false;

  @override
  void initState() {
    super.initState();
    final m = widget.movimiento;
    _tipo = m?.tipo ?? widget.tipoInicial;
    if (m != null) {
      _monto.text = m.monto.toStringAsFixed(2);
      _descripcion.text = m.descripcion ?? '';
      _fecha = m.fecha;
      _categoria = m.categoria;
    }
    _cargarCategorias();
  }

  @override
  void dispose() {
    _monto.dispose();
    _descripcion.dispose();
    super.dispose();
  }

  double? _aDouble(String s) => double.tryParse(s.trim().replaceAll(',', '.'));

  void _mostrarError(String texto) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(texto),
      backgroundColor: Colors.red.shade700,
    ));
  }

  // ------------------------------------------------------------------
  // Carga de datos
  // ------------------------------------------------------------------

  Future<void> _cargarCategorias() async {
    setState(() => _cargandoCategorias = true);
    try {
      final lista = await _servicio.listarCategorias(tipo: _tipo);
      if (!mounted) return;
      setState(() {
        _categorias = lista;
        // Si la categoria elegida no es del tipo actual, se descarta
        if (_categoria != null && !lista.contains(_categoria)) {
          _categoria = null;
        }
        _cargandoCategorias = false;
      });
      if (_categoria?.requiereDeuda == true && !widget.esEdicion) {
        _cargarDeudas();
      }
    } catch (_) {
      if (!mounted) return;
      setState(() => _cargandoCategorias = false);
      _mostrarError('No se pudieron cargar las categorias');
    }
  }

  /// Solo las deudas que aun tienen saldo se pueden abonar.
  Future<void> _cargarDeudas() async {
    setState(() => _cargandoDeudas = true);
    try {
      final lista = await _deudaServicio.listar();
      if (!mounted) return;
      setState(() {
        _deudas = lista.where((d) => !d.pagada).toList();
        _cargandoDeudas = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _cargandoDeudas = false);
      _mostrarError('No se pudieron cargar las deudas');
    }
  }

  void _cambiarTipo(TipoMovimiento tipo) {
    if (tipo == _tipo) return;
    setState(() {
      _tipo = tipo;
      _categoria = null;
      _deuda = null;
    });
    _cargarCategorias();
  }

  void _cambiarCategoria(Categoria? categoria) {
    setState(() {
      _categoria = categoria;
      _deuda = null;
    });
    if (categoria?.requiereDeuda == true && _deudas.isEmpty) {
      _cargarDeudas();
    }
  }

  // ------------------------------------------------------------------
  // Categoria personalizada (HU009)
  // ------------------------------------------------------------------

  Future<void> _nuevaCategoria() async {
    final controlador = TextEditingController();
    final claveDialogo = GlobalKey<FormState>();

    final nombre = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Nueva categoría de ${_tipo.nombre.toLowerCase()}s'),
        content: Form(
          key: claveDialogo,
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
              if (claveDialogo.currentState!.validate()) {
                Navigator.pop(ctx, controlador.text.trim());
              }
            },
            child: const Text('Crear'),
          ),
        ],
      ),
    );

    if (nombre == null) return;

    try {
      final creada =
          await _servicio.crearCategoria(nombre: nombre, tipo: _tipo);
      if (!mounted) return;
      setState(() {
        _categorias = [..._categorias, creada];
        _categoria = creada;
        _deuda = null;
      });
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text('Categoría "${creada.nombre}" creada'),
        backgroundColor: Colors.green.shade700,
      ));
    } on ApiException catch (e) {
      // 409 si el nombre ya existe
      _mostrarError(e.toString());
    } catch (_) {
      _mostrarError('No se pudo conectar con el servidor');
    }
  }

  // ------------------------------------------------------------------
  // Guardar
  // ------------------------------------------------------------------

  Future<void> _elegirFecha() async {
    final hoy = DateTime.now();
    final elegida = await showDatePicker(
      context: context,
      initialDate: _fecha.isAfter(hoy) ? hoy : _fecha,
      firstDate: DateTime(hoy.year - 20),
      // HU009: el movimiento ya ocurrio, no se aceptan fechas futuras
      lastDate: hoy,
    );
    if (elegida != null) setState(() => _fecha = elegida);
  }

  Future<void> _guardar() async {
    // Criterio HU009: si falta un campo obligatorio se notifica y NO se registra
    if (!_formKey.currentState!.validate()) {
      _mostrarError('Completa los campos obligatorios');
      return;
    }

    setState(() => _guardando = true);

    final descripcion = _descripcion.text.trim();
    try {
      if (widget.esEdicion) {
        final m = widget.movimiento!;
        await _servicio.actualizar(
          id: m.id,
          tipo: widget.soloDescripcion ? m.tipo : _tipo,
          monto: widget.soloDescripcion ? m.monto : _aDouble(_monto.text)!,
          fecha: widget.soloDescripcion ? m.fecha : _fecha,
          descripcion: descripcion.isEmpty ? null : descripcion,
          categoriaId: widget.soloDescripcion ? m.categoria.id : _categoria!.id,
        );
      } else {
        await _servicio.crear(
          tipo: _tipo,
          monto: _aDouble(_monto.text)!,
          fecha: _fecha,
          descripcion: descripcion.isEmpty ? null : descripcion,
          categoriaId: _categoria!.id,
          deudaId: _categoria!.requiereDeuda ? _deuda?.id : null,
        );
      }
      if (mounted) Navigator.pop(context, true);
    } on ApiException catch (e) {
      _mostrarError(e.toString());
    } catch (_) {
      _mostrarError('No se pudo conectar con el servidor');
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  // ------------------------------------------------------------------
  // UI
  // ------------------------------------------------------------------

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final bloqueado = widget.soloDescripcion;
    final formato = widget.moneda.formato;

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.esEdicion ? 'Editar movimiento' : 'Nuevo movimiento'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            if (bloqueado)
              Card(
                color: tema.colorScheme.surfaceContainerHighest,
                child: const ListTile(
                  leading: Icon(Icons.info_outline),
                  title: Text('Abono a una deuda'),
                  subtitle: Text(
                      'Solo puedes cambiar la descripción. Para corregir el '
                      'monto o la fecha, elimina el movimiento y registra el '
                      'abono de nuevo.'),
                ),
              ),
            if (bloqueado) const SizedBox(height: 16),

            // ---------- Tipo ----------
            Text('Tipo de movimiento *', style: tema.textTheme.titleSmall),
            const SizedBox(height: 8),
            SegmentedButton<TipoMovimiento>(
              segments: const [
                ButtonSegment(
                  value: TipoMovimiento.ingreso,
                  label: Text('Ingreso'),
                  icon: Icon(Icons.arrow_downward),
                ),
                ButtonSegment(
                  value: TipoMovimiento.gasto,
                  label: Text('Gasto'),
                  icon: Icon(Icons.arrow_upward),
                ),
              ],
              selected: {_tipo},
              onSelectionChanged:
                  bloqueado ? null : (s) => _cambiarTipo(s.first),
            ),
            const SizedBox(height: 16),

            // ---------- Monto ----------
            TextFormField(
              controller: _monto,
              enabled: !bloqueado,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              decoration: InputDecoration(
                labelText: 'Monto *',
                prefixText: '${widget.moneda.simbolo} ',
                prefixIcon: const Icon(Icons.attach_money),
                border: const OutlineInputBorder(),
              ),
              validator: (v) {
                if (bloqueado) return null;
                final n = _aDouble(v ?? '');
                if (n == null) return 'El monto es requerido';
                if (n <= 0) return 'El monto debe ser mayor a cero';
                if (_deuda != null && n > _deuda!.saldoPendiente) {
                  return 'Supera el saldo pendiente '
                      '(${formato.format(_deuda!.saldoPendiente)})';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),

            // ---------- Fecha ----------
            InkWell(
              onTap: bloqueado ? null : _elegirFecha,
              child: InputDecorator(
                decoration: InputDecoration(
                  labelText: 'Fecha *',
                  prefixIcon: const Icon(Icons.event_outlined),
                  border: const OutlineInputBorder(),
                  enabled: !bloqueado,
                ),
                child: Text(DateFormat('dd/MM/yyyy').format(_fecha)),
              ),
            ),
            const SizedBox(height: 16),

            // ---------- Categoria ----------
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: DropdownButtonFormField<Categoria>(
                    // initialValue solo se lee al construir el campo: la
                    // clave cambia cuando llegan categorias o se crea una,
                    // para que el campo muestre la seleccion actual.
                    key: ValueKey('categoria_${_tipo.codigo}_'
                        '${_categoria?.id}_${_categorias.length}'),
                    initialValue: _categoria,
                    isExpanded: true,
                    decoration: InputDecoration(
                      labelText: 'Categoría *',
                      prefixIcon: const Icon(Icons.category_outlined),
                      border: const OutlineInputBorder(),
                      helperText: _cargandoCategorias ? 'Cargando...' : null,
                    ),
                    items: (bloqueado ? [_categoria!] : _categorias)
                        .map((c) => DropdownMenuItem(
                              value: c,
                              child: Text(c.nombre),
                            ))
                        .toList(),
                    onChanged: bloqueado ? null : _cambiarCategoria,
                    validator: (c) =>
                        c == null ? 'Debe seleccionar una categoría' : null,
                  ),
                ),
                if (!bloqueado)
                  Padding(
                    padding: const EdgeInsets.only(left: 8, top: 4),
                    child: IconButton.filledTonal(
                      onPressed: _nuevaCategoria,
                      icon: const Icon(Icons.add),
                      tooltip: 'Nueva categoría',
                    ),
                  ),
              ],
            ),

            // ---------- Deuda (solo categoria Deudas) ----------
            if (!widget.esEdicion && _categoria?.requiereDeuda == true) ...[
              const SizedBox(height: 16),
              if (_cargandoDeudas)
                const LinearProgressIndicator()
              else if (_deudas.isEmpty)
                Card(
                  color: tema.colorScheme.surfaceContainerHighest,
                  child: const ListTile(
                    leading: Icon(Icons.check_circle_outline),
                    title: Text('No tienes deudas con saldo pendiente'),
                    subtitle: Text(
                        'Registra la deuda en la pestaña Deudas para poder abonarle.'),
                  ),
                )
              else
                DropdownButtonFormField<Deuda>(
                  initialValue: _deuda,
                  isExpanded: true,
                  decoration: const InputDecoration(
                    labelText: 'Deuda a la que abonas *',
                    helperText: 'El saldo de la deuda bajará en este monto',
                    prefixIcon: Icon(Icons.account_balance_outlined),
                    border: OutlineInputBorder(),
                  ),
                  items: _deudas
                      .map((d) => DropdownMenuItem(
                            value: d,
                            child: Text(
                              '${d.acreedor} · Saldo ${formato.format(d.saldoPendiente)}',
                              overflow: TextOverflow.ellipsis,
                            ),
                          ))
                      .toList(),
                  onChanged: (d) => setState(() => _deuda = d),
                  validator: (d) =>
                      d == null ? 'Selecciona la deuda a la que abonas' : null,
                ),
            ],
            const SizedBox(height: 16),

            // ---------- Descripcion (opcional) ----------
            TextFormField(
              controller: _descripcion,
              maxLength: 255,
              maxLines: 2,
              textCapitalization: TextCapitalization.sentences,
              decoration: const InputDecoration(
                labelText: 'Descripción',
                hintText: 'Opcional',
                prefixIcon: Icon(Icons.notes_outlined),
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 24),

            FilledButton.icon(
              onPressed: _guardando ? null : _guardar,
              icon: _guardando
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.save_outlined),
              label: Text(_guardando
                  ? 'Guardando...'
                  : (widget.esEdicion
                      ? 'Guardar cambios'
                      : 'Registrar movimiento')),
              style: FilledButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
