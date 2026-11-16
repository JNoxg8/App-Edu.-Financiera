package edu.ucentral.zenfi.metasahorro.dominio.evento;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento de dominio: se registro un aporte a una meta de ahorro.
 *
 * Igual que AbonoRegistrado en deudas: el modulo de metas lo publica sin
 * saber quien lo escucha (patron Observer con los eventos CDI).
 * Movimientos lo escucha para anotar el aporte como un gasto en la
 * categoria "Metas de ahorro": el dinero que se aparta para ahorrar ya no
 * esta disponible para gastar, asi que cuenta en el presupuesto del mes.
 */
public record AporteRegistrado(
        Long metaAhorroId,
        Long aporteId,
        Long usuarioId,
        String nombreMeta,
        BigDecimal monto,
        LocalDate fecha
) {
}
