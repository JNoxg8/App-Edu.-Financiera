package edu.ucentral.zenfi.deudas.infraestructura.dto;

import edu.ucentral.zenfi.deudas.dominio.modelo.Deuda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que devuelve la API.
 *
 * Los campos de abajo (cuotaMensual, saldoPendiente, porcentajePagado...)
 * NO existen en la tabla: se calculan al armar la respuesta.
 * Eso es lo que RQ-07 pide con "de forma automatica".
 */
public record DeudaRespuestaDto(
        Long id,
        String acreedor,
        BigDecimal montoTotal,
        BigDecimal tasaInteresAnual,
        int numeroCuotas,
        LocalDate fechaVencimiento,

        // --- calculados ---
        BigDecimal cuotaMensual,
        BigDecimal totalAPagar,
        BigDecimal costoIntereses,
        BigDecimal totalAbonado,
        BigDecimal saldoPendiente,
        BigDecimal porcentajePagado,
        boolean pagada,

        List<AbonoRespuestaDto> abonos
) {

    public static DeudaRespuestaDto desde(Deuda d) {
        List<AbonoRespuestaDto> abonos = d.getAbonos().stream()
                .map(a -> new AbonoRespuestaDto(a.getId(), a.getMonto(), a.getFecha()))
                .toList();

        return new DeudaRespuestaDto(
                d.getId(),
                d.getAcreedor(),
                d.getMontoTotal(),
                d.getTasaInteresAnual(),
                d.getNumeroCuotas(),
                d.getFechaVencimiento(),
                d.cuotaMensual(),
                d.totalAPagar(),
                d.costoIntereses(),
                d.totalAbonado(),
                d.saldoPendiente(),
                d.porcentajePagado(),
                d.estaPagada(),
                abonos
        );
    }
}
