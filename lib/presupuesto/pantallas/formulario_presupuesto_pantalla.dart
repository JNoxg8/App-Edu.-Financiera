import 'package:flutter/material.dart';

import '../../comun/moneda.dart';
import '../modelo/mes.dart';
import '../servicio/presupuesto_servicio.dart';

/// RQ-11: formulario para definir el presupuesto de un mes.
///
/// Solo ofrece el mes actual y los siguientes: el backend rechaza los
/// meses pasados, y es mejor no dejar elegirlos que mostrar un error.
class FormularioPresupuestoPantalla extends StatefulWidget {
  final Moneda moneda;

  /// Mes que aparece seleccionado al abrir el formulario.
  final DateTime mesInicial;

  const FormularioPresupuestoPantalla({
    super.key,
    required this.mesInicial,
    this.moneda = Moneda.cop,
  });

  @override
  State<FormularioPresupuestoPantalla> createState() =>
      _FormularioPresupuestoPantallaState();
}

class _FormularioPresupuestoPantallaState
    extends State<FormularioPresupuestoPantalla> {
  static const int _mesesAOfrecer = 12;

  final _formKey = GlobalKey<FormState>();
  final _servicio = PresupuestoServicio();
  final _monto = TextEditingController();

  late final List<DateTime> _meses;
  DateTime? _mes;
  bool _guardando = false;

  @override
  void initState() {
    super.initState();
    final actual = mesActual();
    _meses = List.generate(
        _mesesAOfrecer, (i) => DateTime(actual.year, actual.month + i));

    // Si se navego a un mes mas lejano que los 12 ofrecidos, tambien se agrega
    final inicial = inicioDeMes(widget.mesInicial);
    if (!esMesPasado(inicial) && !_meses.contains(inicial)) {
      _meses.add(inicial);
    }
    _mes = _meses.contains(inicial) ? inicial : _meses.first;
  }

  @override
  void dispose() {
    _monto.dispose();
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

  Future<void> _guardar() async {
    // HU011: si falta un dato o no es valido, se notifica y NO se guarda.
    // Los mensajes aparecen debajo de cada campo sin borrar lo escrito.
    if (!_formKey.currentState!.validate()) {
      _mostrarError('Completa los campos obligatorios');
      return;
    }

    setState(() => _guardando = true);
    try {
      await _servicio.crear(mes: _mes!, montoLimite: _aDouble(_monto.text)!);
      if (mounted) Navigator.pop(context, true);
    } on ApiException catch (e) {
      // 409: el mes ya tiene presupuesto (o ya paso)
      _mostrarError(e.toString());
    } catch (_) {
      _mostrarError('No se pudo conectar con el servidor');
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Nuevo presupuesto')),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text(
              'Define cuanto quieres gastar como maximo en el mes. Zenfi '
              'sumara tus gastos y te mostrara que porcentaje llevas.',
              style: tema.textTheme.bodyMedium
                  ?.copyWith(color: tema.colorScheme.outline),
            ),
            const SizedBox(height: 20),

            // ---------- Mes ----------
            DropdownButtonFormField<DateTime>(
              initialValue: _mes,
              decoration: const InputDecoration(
                labelText: 'Mes *',
                prefixIcon: Icon(Icons.calendar_month_outlined),
                border: OutlineInputBorder(),
              ),
              items: _meses
                  .map((m) =>
                      DropdownMenuItem(value: m, child: Text(nombreMes(m))))
                  .toList(),
              onChanged: (m) => setState(() => _mes = m),
              validator: (m) => m == null ? 'El mes es requerido' : null,
            ),
            const SizedBox(height: 16),

            // ---------- Monto limite ----------
            TextFormField(
              controller: _monto,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              decoration: InputDecoration(
                labelText: 'Monto limite *',
                helperText: 'Lo maximo que quieres gastar en el mes',
                prefixText: '${widget.moneda.simbolo} ',
                prefixIcon: const Icon(Icons.savings_outlined),
                border: const OutlineInputBorder(),
              ),
              validator: (v) {
                final n = _aDouble(v ?? '');
                if (n == null) return 'El monto limite es requerido';
                if (n <= 0) return 'El monto limite debe ser mayor a cero';
                return null;
              },
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
                  : const Icon(Icons.check),
              label: const Text('Guardar presupuesto'),
            ),
          ],
        ),
      ),
    );
  }
}
