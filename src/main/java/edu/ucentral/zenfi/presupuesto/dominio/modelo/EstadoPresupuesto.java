package edu.ucentral.zenfi.presupuesto.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Como va el presupuesto de un mes (HU011). Se calcula en cada consulta,
 * nunca se guarda en la base de datos.
 *
 * @param porcentajeUtilizado gastado / limite * 100. Puede pasar de 100.
 * @param disponible          limite - gastado. Negativo si se excedio.
 * @param excedido            true solo si lo gastado SUPERA el limite;
 *                            gastar exactamente el limite no es exceso.
 */
public record EstadoPresupuesto(
        Presupuesto presupuesto,
        BigDecimal totalGastado,
        BigDecimal porcentajeUtilizado,
        BigDecimal disponible,
        boolean excedido
) {

    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    static EstadoPresupuesto calcular(Presupuesto presupuesto, BigDecimal totalGastado) {
        BigDecimal limite = presupuesto.getMontoLimite();
        BigDecimal gastado = (totalGastado == null ? BigDecimal.ZERO : totalGastado)
                .setScale(ESCALA, REDONDEO);

        // El limite siempre es > 0 (lo valida Presupuesto.crear): no hay division por cero
        BigDecimal porcentaje = gastado.multiply(CIEN).divide(limite, ESCALA, REDONDEO);
        BigDecimal disponible = limite.subtract(gastado).setScale(ESCALA, REDONDEO);

        return new EstadoPresupuesto(presupuesto, gastado, porcentaje, disponible,
                gastado.compareTo(limite) > 0);
    }
}
