package edu.ucentral.zenfi.presupuesto.dominio.puerto;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Puerto de salida hacia el modulo de movimientos.
 *
 * Presupuesto declara QUE necesita (el total gastado en un mes) sin
 * saber COMO lo calcula movimientos. El adaptador GastosMensualesAdaptador
 * lo implementa llamando a MovimientoServicio. En los tests se simula con
 * @InjectMock, sin tocar el modulo de movimientos.
 */
public interface GastosMensualesPuerto {

    /**
     * Suma de los movimientos de tipo gasto del usuario en el mes.
     * Incluye los abonos a deudas, que movimientos registra como gastos.
     */
    BigDecimal totalGastos(Long usuarioId, YearMonth mes);
}
