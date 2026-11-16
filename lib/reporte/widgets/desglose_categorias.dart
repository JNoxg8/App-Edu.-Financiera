import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../presupuesto/modelo/mes.dart';
import '../modelo/reporte_mensual.dart';

/// HU025: gastos del mes por categoria, con el monto y el porcentaje que
/// representa cada una sobre el total gastado.
///
/// Se muestra debajo del presupuesto (HU011), en la misma pestana.
class DesgloseCategorias extends StatelessWidget {
  final ReporteMensual reporte;
  final DateTime mes;
  final NumberFormat formato;

  const DesgloseCategorias({
    super.key,
    required this.reporte,
    required this.mes,
    required this.formato,
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
                  child: Text('Gastos por categoría',
                      style: tema.textTheme.titleMedium),
                ),
                if (reporte.tieneGastos)
                  Text(
                    formato.format(reporte.totalGastado),
                    style: tema.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.bold),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            if (!reporte.tieneGastos)
              Row(
                children: [
                  Icon(Icons.receipt_long_outlined,
                      color: tema.colorScheme.outline),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'No hay gastos registrados en ${nombreMes(mes)}.',
                      style: tema.textTheme.bodyMedium
                          ?.copyWith(color: tema.colorScheme.outline),
                    ),
                  ),
                ],
              )
            else
              for (final c in reporte.gastosPorCategoria)
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              c.categoria,
                              style: tema.textTheme.bodyLarge,
                            ),
                          ),
                          Text(
                            formato.format(c.total),
                            style: tema.textTheme.bodyLarge
                                ?.copyWith(fontWeight: FontWeight.w600),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      ClipRRect(
                        borderRadius: BorderRadius.circular(8),
                        child: LinearProgressIndicator(
                          value: (c.porcentaje / 100).clamp(0.0, 1.0),
                          minHeight: 8,
                          backgroundColor:
                              tema.colorScheme.surfaceContainerHighest,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        '${c.porcentaje.toStringAsFixed(1)}% del total · '
                        '${c.cantidadMovimientos} '
                        '${c.cantidadMovimientos == 1 ? 'gasto' : 'gastos'}',
                        style: tema.textTheme.bodySmall
                            ?.copyWith(color: tema.colorScheme.outline),
                      ),
                    ],
                  ),
                ),
          ],
        ),
      ),
    );
  }
}
