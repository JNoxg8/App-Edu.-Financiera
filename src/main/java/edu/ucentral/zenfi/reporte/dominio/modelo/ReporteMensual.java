package edu.ucentral.zenfi.reporte.dominio.modelo;

import edu.ucentral.zenfi.presupuesto.dominio.modelo.EstadoPresupuesto;
import edu.ucentral.zenfi.presupuesto.dominio.modelo.Presupuesto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * HU025: reporte de un mes que compara los gastos con el presupuesto.
 *
 * No repite calculos de otros modulos: el desglose por categoria viene
 * de movimientos (HU009) y la comparacion con el limite la hace
 * EstadoPresupuesto (HU011). Aqui solo se juntan, usando UN solo total
 * gastado para las dos partes, asi nunca muestran cifras distintas.
 *
 * Se calcula en cada consulta; nada de esto se guarda.
 */
public class ReporteMensual {

    private static final int ESCALA = 2;

    private final YearMonth mes;
    private final List<GastoCategoria> gastosPorCategoria;
    private final BigDecimal totalGastado;
    /** null si el mes no tiene presupuesto definido. */
    private final EstadoPresupuesto presupuesto;

    private ReporteMensual(YearMonth mes, List<GastoCategoria> gastosPorCategoria,
                           Presupuesto presupuesto) {
        this.mes = mes;
        this.gastosPorCategoria = List.copyOf(gastosPorCategoria);
        this.totalGastado = gastosPorCategoria.stream()
                .map(GastoCategoria::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(ESCALA, RoundingMode.HALF_UP);
        this.presupuesto = presupuesto == null ? null : presupuesto.estado(totalGastado);
    }

    /**
     * @param gastosPorCategoria gastos del mes agrupados, de la categoria
     *                           con mas gasto a la de menos
     * @param presupuesto        presupuesto del mes, o null si no tiene
     */
    public static ReporteMensual generar(YearMonth mes, List<GastoCategoria> gastosPorCategoria,
                                         Presupuesto presupuesto) {
        if (mes == null) {
            throw new IllegalArgumentException("El mes del reporte es obligatorio");
        }
        return new ReporteMensual(mes,
                gastosPorCategoria == null ? List.of() : gastosPorCategoria, presupuesto);
    }

    public boolean tieneGastos() {
        return !gastosPorCategoria.isEmpty();
    }

    public Optional<EstadoPresupuesto> getPresupuesto() {
        return Optional.ofNullable(presupuesto);
    }

    public YearMonth getMes() {
        return mes;
    }

    public List<GastoCategoria> getGastosPorCategoria() {
        return gastosPorCategoria;
    }

    public BigDecimal getTotalGastado() {
        return totalGastado;
    }
}
