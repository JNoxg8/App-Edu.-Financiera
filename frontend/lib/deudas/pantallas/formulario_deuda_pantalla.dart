import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../modelo/deuda.dart';
import '../servicio/deuda_servicio.dart';
import '../../comun/tema.dart';

/// RQ-07 (crear) y RQ-22 (editar).
///
/// Si recibe una deuda, entra en modo edicion: precarga los campos y
/// bloquea el monto total, que el RQ-22 no permite modificar.
class FormularioDeudaPantalla extends StatefulWidget {
  final Deuda? deuda;

  const FormularioDeudaPantalla({super.key, this.deuda});

  bool get esEdicion => deuda != null;

  @override
  State<FormularioDeudaPantalla> createState() =>
      _FormularioDeudaPantallaState();
}

class _FormularioDeudaPantallaState extends State<FormularioDeudaPantalla> {
  final _formKey = GlobalKey<FormState>();
  final _servicio = DeudaServicio();

  final _acreedor = TextEditingController();
  final _monto = TextEditingController();
  final _tasa = TextEditingController();
  final _cuotas = TextEditingController();

  DateTime? _vencimiento;
  bool _guardando = false;

  @override
  void initState() {
    super.initState();
    final d = widget.deuda;
    if (d != null) {
      _acreedor.text = d.acreedor;
      _monto.text = d.montoTotal.toStringAsFixed(2);
      _tasa.text = d.tasaInteresAnual.toStringAsFixed(2);
      _cuotas.text = d.numeroCuotas.toString();
      _vencimiento = d.fechaVencimiento;
    }
  }

  @override
  void dispose() {
    _acreedor.dispose();
    _monto.dispose();
    _tasa.dispose();
    _cuotas.dispose();
    super.dispose();
  }

  double? _aDouble(String s) => double.tryParse(s.trim().replaceAll(',', '.'));

  Future<void> _elegirFecha() async {
    final hoy = DateTime.now();
    var inicial = _vencimiento ?? hoy.add(const Duration(days: 365));
    if (inicial.isBefore(hoy)) inicial = hoy;

    final elegida = await showDatePicker(
      context: context,
      initialDate: inicial,
      firstDate: hoy,
      lastDate: DateTime(hoy.year + 50),
    );
    if (elegida != null) {
      setState(() {
        _vencimiento = elegida;
      });
    }
  }

  Future<void> _guardar() async {
    if (!_formKey.currentState!.validate()) return;

    if (_vencimiento == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Selecciona la fecha de vencimiento')),
      );
      return;
    }

    setState(() {
      _guardando = true;
    });

    try {
      if (widget.esEdicion) {
        await _servicio.actualizar(
          id: widget.deuda!.id,
          acreedor: _acreedor.text.trim(),
          tasaInteresAnual: _aDouble(_tasa.text)!,
          numeroCuotas: int.parse(_cuotas.text.trim()),
          fechaVencimiento: _vencimiento!,
        );
      } else {
        await _servicio.crear(
          acreedor: _acreedor.text.trim(),
          montoTotal: _aDouble(_monto.text)!,
          tasaInteresAnual: _aDouble(_tasa.text)!,
          numeroCuotas: int.parse(_cuotas.text.trim()),
          fechaVencimiento: _vencimiento!,
        );
      }
      if (mounted) Navigator.pop(context, true);
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: Text(e.toString()),
          backgroundColor: ColoresZenfi.negativo,
        ));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
          content: const Text('No se pudo conectar con el servidor'),
          backgroundColor: ColoresZenfi.negativo,
        ));
      }
    } finally {
      if (mounted) {
        setState(() {
          _guardando = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.esEdicion ? 'Editar deuda' : 'Nueva deuda'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            TextFormField(
              controller: _acreedor,
              textCapitalization: TextCapitalization.words,
              decoration: const InputDecoration(
                labelText: 'Acreedor *',
                hintText: 'Banco, persona o entidad',
                prefixIcon: Icon(Icons.account_balance_outlined),
                border: OutlineInputBorder(),
              ),
              validator: (v) {
                if (v == null || v.trim().isEmpty) {
                  return 'El nombre del acreedor es requerido';
                }
                if (v.trim().length > 120) return 'Maximo 120 caracteres';
                return null;
              },
            ),
            const SizedBox(height: 16),

            TextFormField(
              controller: _monto,
              readOnly: widget.esEdicion,
              enabled: !widget.esEdicion,
              keyboardType:
              const TextInputType.numberWithOptions(decimal: true),
              decoration: InputDecoration(
                labelText: 'Monto total *',
                helperText: widget.esEdicion
                    ? 'El capital prestado no se puede modificar'
                    : 'Capital prestado, sin intereses',
                prefixText: '\$ ',
                prefixIcon: const Icon(Icons.attach_money),
                border: const OutlineInputBorder(),
              ),
              validator: (v) {
                if (widget.esEdicion) return null;
                final n = _aDouble(v ?? '');
                if (n == null) return 'Ingresa un numero valido';
                if (n <= 0) return 'El monto debe ser mayor a cero';
                return null;
              },
            ),
            const SizedBox(height: 16),

            TextFormField(
              controller: _tasa,
              keyboardType:
              const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(
                labelText: 'Tasa de interes *',
                helperText: 'Efectiva anual. Ej: 24.5 para 24,5% E.A.',
                suffixText: '% E.A.',
                prefixIcon: Icon(Icons.percent),
                border: OutlineInputBorder(),
              ),
              validator: (v) {
                final n = _aDouble(v ?? '');
                if (n == null) return 'Ingresa un numero valido';
                if (n < 0) return 'La tasa no puede ser negativa';
                if (n > 300) return 'La tasa supera el maximo permitido';
                return null;
              },
            ),
            const SizedBox(height: 16),

            TextFormField(
              controller: _cuotas,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Numero de cuotas *',
                helperText: 'Cuotas mensuales',
                prefixIcon: Icon(Icons.format_list_numbered),
                border: OutlineInputBorder(),
              ),
              validator: (v) {
                final n = int.tryParse((v ?? '').trim());
                if (n == null) return 'Ingresa un numero entero';
                if (n < 1) return 'Debe haber al menos una cuota';
                if (n > 600) return 'El numero de cuotas es demasiado alto';
                return null;
              },
            ),
            const SizedBox(height: 16),

            InkWell(
              onTap: _elegirFecha,
              child: InputDecorator(
                decoration: const InputDecoration(
                  labelText: 'Fecha de vencimiento *',
                  prefixIcon: Icon(Icons.event_outlined),
                  border: OutlineInputBorder(),
                ),
                child: Text(
                  _vencimiento == null
                      ? 'Selecciona una fecha'
                      : DateFormat('dd/MM/yyyy').format(_vencimiento!),
                  style: TextStyle(
                    color: _vencimiento == null
                        ? Theme.of(context).colorScheme.outline
                        : null,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 32),

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
                  : (widget.esEdicion ? 'Guardar cambios' : 'Registrar deuda')),
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