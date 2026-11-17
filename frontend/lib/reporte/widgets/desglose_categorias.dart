import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../comun/graficos.dart';
import '../../presupuesto/modelo/mes.dart';
import '../modelo/reporte_mensual.dart';

/// HU025: gastos del mes por categoria, con el monto y el porcentaje que
/// representa cada una sobre el total gastado.
///
/// Dona con las 5 categorias de mas gasto (el resto se agrupa en "Otras")
/// y, debajo, la leyenda con TODAS las categorias: la dona da la idea de un
/// vistazo y la lista da las cifras exactas. Se muestra debajo del
/// presupuesto (HU011), en la misma pestana.
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
    final partes = agruparPartes([
      for (final c in reporte.gastosPorCategoria)
        (nombre: c.categoria, valor: c.total, porcentaje: c.porcentaje),
    ]);

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Gastos por categoría',
                style: tema.textTheme.titleMedium
                    ?.copyWith(fontWeight: FontWeight.w600)),
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
            else ...[
              Center(
                child: GraficoDona(
                  partes: partes,
                  tamano: 170,
                  grosor: 26,
                  centro: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text('Total',
                          style: tema.textTheme.labelSmall
                              ?.copyWith(color: tema.colorScheme.outline)),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 32),
                        child: FittedBox(
                          child: Text(formato.format(reporte.totalGastado),
                              style: tema.textTheme.titleMedium
                                  ?.copyWith(fontWeight: FontWeight.bold)),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 14),
              // Leyenda: las primeras categorias con su color de la dona; si
              // hay "Otras", tambien se listan una por una en gris
              for (var i = 0; i < reporte.gastosPorCategoria.length; i++)
                FilaLeyenda(
                  color: i < PaletaCategorias.colores.length
                      ? PaletaCategorias.colores[i]
                      : PaletaCategorias.otras,
                  nombre: reporte.gastosPorCategoria[i].categoria,
                  monto: formato.format(reporte.gastosPorCategoria[i].total),
                  detalle: '${reporte.gastosPorCategoria[i].porcentaje.toStringAsFixed(1)} % · '
                      '${reporte.gastosPorCategoria[i].cantidadMovimientos} '
                      '${reporte.gastosPorCategoria[i].cantidadMovimientos == 1 ? 'gasto' : 'gastos'}',
                ),
            ],
          ],
        ),
      ),
    );
  }
}
