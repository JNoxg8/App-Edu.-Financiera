import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../comun/api_exception.dart';
import '../../comun/cambios_financieros.dart';
import '../modelo/meta_ahorro.dart';
import '../servicio/meta_ahorro_servicio.dart';
import '../../comun/tema.dart';

/// Pantalla de creación de metas de ahorro (HU003).
class FormularioMetaAhorroPantalla extends StatefulWidget {
  final MetaAhorroServicio? servicio;

  const FormularioMetaAhorroPantalla({super.key, this.servicio});

  @override
  State<FormularioMetaAhorroPantalla> createState() =>
      _FormularioMetaAhorroPantallaState();
}

class _FormularioMetaAhorroPantallaState
    extends State<FormularioMetaAhorroPantalla> {
  final _formKey = GlobalKey<FormState>();
  final _nombre = TextEditingController();
  final _objetivo = TextEditingController();
  final _inicial = TextEditingController(text: '0');
  late final MetaAhorroServicio _servicio =
      widget.servicio ?? MetaAhorroServicio();

  DateTime? _fechaLimite;
  bool _guardando = false;
  MetaAhorro? _creada;

  final _moneda = NumberFormat.currency(
    locale: 'es_CO',
    symbol: r'$ ',
    decimalDigits: 0,
  );

  @override
  void dispose() {
    _nombre.dispose();
    _objetivo.dispose();
    _inicial.dispose();
    super.dispose();
  }

  double? _numero(String texto) =>
      double.tryParse(texto.trim().replaceAll(' ', '').replaceAll(',', '.'));

  Future<void> _elegirFecha() async {
    final hoy = DateUtils.dateOnly(DateTime.now());
    final fecha = await showDatePicker(
      context: context,
      initialDate: _fechaLimite ?? hoy.add(const Duration(days: 30)),
      firstDate: hoy.add(const Duration(days: 1)),
      lastDate: DateTime(hoy.year + 30, hoy.month, hoy.day),
      helpText: 'Selecciona la fecha límite',
      cancelText: 'Cancelar',
      confirmText: 'Aceptar',
    );
    if (fecha != null && mounted) setState(() => _fechaLimite = fecha);
  }

  void _mensaje(String texto, {bool error = false}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(texto),
        backgroundColor: error ? ColoresZenfi.negativo : ColoresZenfi.positivo,
      ),
    );
  }

  Future<void> _guardar() async {
    FocusScope.of(context).unfocus();
    if (!_formKey.currentState!.validate()) return;
    if (_fechaLimite == null ||
        !DateUtils.dateOnly(_fechaLimite!)
            .isAfter(DateUtils.dateOnly(DateTime.now()))) {
      _mensaje('Selecciona una fecha límite posterior a hoy.', error: true);
      return;
    }

    final objetivo = _numero(_objetivo.text)!;
    final inicial = _numero(_inicial.text)!;
    if (inicial >= objetivo) {
      _mensaje(
        'El monto inicial debe ser menor que el monto objetivo.',
        error: true,
      );
      return;
    }

    setState(() => _guardando = true);
    try {
      final meta = await _servicio.crear(
        nombre: _nombre.text,
        montoObjetivo: objetivo,
        fechaLimite: _fechaLimite!,
        montoInicial: inicial,
      );
      CambiosFinancieros.notificar();
      if (!mounted) return;
      setState(() {
        _creada = meta;
        _nombre.clear();
        _objetivo.clear();
        _inicial.text = '0';
        _fechaLimite = null;
      });
      _mensaje('Meta de ahorro creada correctamente.');
    } on ApiException catch (e) {
      _mensaje(e.toString(), error: true);
    } catch (_) {
      _mensaje(
        'No se pudo conectar con el servidor. Verifica que Quarkus esté activo.',
        error: true,
      );
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Metas de ahorro'),
        actions: [
          IconButton(
            onPressed: _guardando
                ? null
                : () => setState(() {
                    _nombre.clear();
                    _objetivo.clear();
                    _inicial.text = '0';
                    _fechaLimite = null;
                    _creada = null;
                  }),
            icon: const Icon(Icons.refresh),
            tooltip: 'Limpiar formulario',
          ),
        ],
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(18),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(
                      Icons.savings_outlined,
                      size: 42,
                      color: tema.colorScheme.primary,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Define tu próxima meta',
                      style: tema.textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Establece cuánto quieres ahorrar y para cuándo.',
                      style: tema.textTheme.bodyMedium,
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
            Form(
              key: _formKey,
              child: Column(
                children: [
                  TextFormField(
                    controller: _nombre,
                    textCapitalization: TextCapitalization.sentences,
                    maxLength: 100,
                    decoration: const InputDecoration(
                      labelText: 'Nombre de la meta',
                      hintText: 'Ej. Viaje a Cartagena',
                      prefixIcon: Icon(Icons.flag_outlined),
                      border: OutlineInputBorder(),
                    ),
                    validator: (v) {
                      if (v == null || v.trim().isEmpty) {
                        return 'El nombre es obligatorio';
                      }
                      if (v.trim().length > 100) {
                        return 'Máximo 100 caracteres';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _objetivo,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    decoration: const InputDecoration(
                      labelText: 'Monto objetivo',
                      hintText: 'Ej. 2000000',
                      prefixText: r'$ ',
                      prefixIcon: Icon(Icons.account_balance_wallet_outlined),
                      border: OutlineInputBorder(),
                    ),
                    validator: (v) {
                      final n = v == null ? null : _numero(v);
                      if (v == null || v.trim().isEmpty) {
                        return 'El monto objetivo es obligatorio';
                      }
                      if (n == null || n <= 0) {
                        return 'El monto debe ser mayor que cero';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _inicial,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    decoration: const InputDecoration(
                      labelText: 'Monto inicial',
                      helperText: 'Puedes comenzar con \$0',
                      prefixText: r'$ ',
                      prefixIcon: Icon(Icons.savings_outlined),
                      border: OutlineInputBorder(),
                    ),
                    validator: (v) {
                      if (v == null || v.trim().isEmpty) {
                        return 'El monto inicial es obligatorio';
                      }
                      final n = _numero(v);
                      if (n == null || n < 0) {
                        return 'El monto inicial no puede ser negativo';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 12),
                  InkWell(
                    onTap: _guardando ? null : _elegirFecha,
                    borderRadius: BorderRadius.circular(4),
                    child: InputDecorator(
                      decoration: const InputDecoration(
                        labelText: 'Fecha límite',
                        prefixIcon: Icon(Icons.calendar_month_outlined),
                        border: OutlineInputBorder(),
                      ),
                      child: Text(
                        _fechaLimite == null
                            ? 'Seleccionar fecha'
                            : DateFormat('dd/MM/yyyy').format(_fechaLimite!),
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      onPressed: _guardando ? null : _guardar,
                      icon: _guardando
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.save_outlined),
                      label: Text(_guardando ? 'Guardando...' : 'Crear meta'),
                    ),
                  ),
                ],
              ),
            ),
            if (_creada != null) ...[
              const SizedBox(height: 20),
              Card(
                color: tema.colorScheme.primaryContainer,
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Row(
                        children: [
                          Icon(Icons.check_circle_outline),
                          SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              'Meta creada correctamente',
                              style: TextStyle(fontWeight: FontWeight.bold),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Text(_creada!.nombre, style: tema.textTheme.titleMedium),
                      Text(
                        'Objetivo: ${_moneda.format(_creada!.montoObjetivo)}',
                      ),
                      Text(
                        'Acumulado: ${_moneda.format(_creada!.montoAcumulado)}',
                      ),
                      Text(
                        'Fecha límite: ${DateFormat('dd/MM/yyyy').format(_creada!.fechaLimite)}',
                      ),
                      Text('Estado: ${_creada!.estado} · ID: ${_creada!.id}'),
                    ],
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
