package edu.ucentral.zenfi.presupuesto.dominio.modelo;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

/**
 * Presupuesto mensual del usuario (HU011). Java puro: sin JPA ni Quarkus.
 *
 * Solo guarda el plan: el mes y el monto limite. Lo gastado NO se guarda,
 * se calcula en cada consulta con estado(). Asi el porcentaje utilizado
 * siempre refleja los movimientos actuales, aunque se registren, modifiquen
 * o eliminen despues de crear el presupuesto.
 */
public class Presupuesto {

    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final Long id;
    private final Long usuarioId;
    private final YearMonth mes;
    private final BigDecimal montoLimite;

    /** Reconstruye un presupuesto ya guardado (lo usa el adaptador de persistencia). */
    public Presupuesto(Long id, Long usuarioId, YearMonth mes, BigDecimal montoLimite) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.mes = mes;
        this.montoLimite = montoLimite;
    }

    /**
     * Crea un presupuesto nuevo validando HU011: mes obligatorio y monto
     * limite mayor a cero. Solo se acepta el mes actual o uno futuro: un
     * presupuesto es un plan, no tiene sentido definirlo para un mes que
     * ya termino.
     *
     * El mes actual llega como parametro (en vez de llamar aqui a
     * YearMonth.now()) para que el dominio no dependa del reloj del
     * servidor y se pueda probar con cualquier fecha.
     */
    public static Presupuesto crear(Long usuarioId, YearMonth mes, BigDecimal montoLimite,
                                    YearMonth mesActual) {
        if (mes == null) {
            throw new ReglaNegocioException("Debe indicar el mes del presupuesto");
        }
        if (montoLimite == null || montoLimite.signum() <= 0) {
            throw new ReglaNegocioException("El monto limite debe ser mayor a cero");
        }
        if (mes.isBefore(mesActual)) {
            throw new ReglaNegocioException(
                    "No se puede crear un presupuesto para " + mes
                            + ": el mes debe ser el actual (" + mesActual + ") o uno futuro");
        }
        return new Presupuesto(null, usuarioId, mes, montoLimite.setScale(ESCALA, REDONDEO));
    }

    /** Cruza el presupuesto con lo gastado en su mes (HU011). */
    public EstadoPresupuesto estado(BigDecimal totalGastado) {
        return EstadoPresupuesto.calcular(this, totalGastado);
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public YearMonth getMes() {
        return mes;
    }

    public BigDecimal getMontoLimite() {
        return montoLimite;
    }
}
