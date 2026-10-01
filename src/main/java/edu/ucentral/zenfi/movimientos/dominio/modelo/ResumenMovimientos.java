package edu.ucentral.zenfi.movimientos.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HU009 "para saber en que uso mi dinero": totales de un periodo y como
 * se reparte cada tipo entre categorias.
 *
 * Los abonos a deudas entran aqui como un gasto mas (categoria Deudas),
 * asi que el balance ya descuenta lo que se paga en deudas.
 *
 * Todo se calcula en memoria a partir de la lista de movimientos: nada
 * de esto se guarda en la base de datos.
 */
public class ResumenMovimientos {

    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /** Total de una categoria dentro del periodo. */
    public record TotalCategoria(Long categoriaId, String categoria, BigDecimal total,
                                 BigDecimal porcentaje, int cantidadMovimientos) {
    }

    private final LocalDate desde;
    private final LocalDate hasta;
    private final BigDecimal totalIngresos;
    private final BigDecimal totalGastos;
    private final List<TotalCategoria> ingresosPorCategoria;
    private final List<TotalCategoria> gastosPorCategoria;

    private ResumenMovimientos(LocalDate desde, LocalDate hasta, List<Movimiento> movimientos) {
        this.desde = desde;
        this.hasta = hasta;

        List<Movimiento> ingresos = movimientos.stream().filter(Movimiento::esIngreso).toList();
        List<Movimiento> gastos = movimientos.stream().filter(m -> !m.esIngreso()).toList();

        this.totalIngresos = sumar(ingresos);
        this.totalGastos = sumar(gastos);
        this.ingresosPorCategoria = agruparPorCategoria(ingresos, totalIngresos);
        this.gastosPorCategoria = agruparPorCategoria(gastos, totalGastos);
    }

    public static ResumenMovimientos calcular(LocalDate desde, LocalDate hasta,
                                              List<Movimiento> movimientos) {
        return new ResumenMovimientos(desde, hasta, movimientos == null ? List.of() : movimientos);
    }

    /** Ingresos - gastos. Negativo significa que se gasto mas de lo que entro. */
    public BigDecimal balance() {
        return totalIngresos.subtract(totalGastos).setScale(ESCALA, REDONDEO);
    }

    /** Que porcentaje de los ingresos se fue en gastos. 0 si no hubo ingresos. */
    public BigDecimal porcentajeGastado() {
        if (totalIngresos.signum() == 0) {
            return cero();
        }
        return totalGastos.multiply(CIEN).divide(totalIngresos, ESCALA, REDONDEO);
    }

    // ------------------------------------------------------------------

    private static List<TotalCategoria> agruparPorCategoria(List<Movimiento> movimientos,
                                                            BigDecimal totalTipo) {
        // LinkedHashMap: agrupa conservando el orden de llegada
        Map<Long, List<Movimiento>> grupos = new LinkedHashMap<>();
        for (Movimiento m : movimientos) {
            grupos.computeIfAbsent(m.getCategoria().getId(), k -> new ArrayList<>()).add(m);
        }

        return grupos.values().stream()
                .map(grupo -> {
                    BigDecimal total = sumar(grupo);
                    BigDecimal porcentaje = totalTipo.signum() == 0
                            ? cero()
                            : total.multiply(CIEN).divide(totalTipo, ESCALA, REDONDEO);
                    Categoria c = grupo.get(0).getCategoria();
                    return new TotalCategoria(c.getId(), c.getNombre(), total, porcentaje, grupo.size());
                })
                // La categoria donde mas se mueve dinero va primero
                .sorted(Comparator.comparing(TotalCategoria::total).reversed()
                        .thenComparing(TotalCategoria::categoria))
                .toList();
    }

    private static BigDecimal sumar(List<Movimiento> movimientos) {
        return movimientos.stream()
                .map(Movimiento::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(ESCALA, REDONDEO);
    }

    private static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(ESCALA, REDONDEO);
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public BigDecimal getTotalIngresos() {
        return totalIngresos;
    }

    public BigDecimal getTotalGastos() {
        return totalGastos;
    }

    public List<TotalCategoria> getIngresosPorCategoria() {
        return ingresosPorCategoria;
    }

    public List<TotalCategoria> getGastosPorCategoria() {
        return gastosPorCategoria;
    }
}
