package edu.ucentral.zenfi.metasahorro;

import edu.ucentral.zenfi.metasahorro.dominio.modelo.MetaAhorro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pruebas unitarias del estado de una meta (HU004). Sin Quarkus ni base de datos. */
class MetaAhorroTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);

    private MetaAhorro meta(String acumulado, LocalDate fechaLimite) {
        MetaAhorro meta = new MetaAhorro("Viaje", new BigDecimal("1000"), fechaLimite, BigDecimal.ZERO);
        meta.setMontoAcumulado(new BigDecimal(acumulado));
        return meta;
    }

    @Test
    @DisplayName("Una meta nueva empieza ACTIVA")
    void nuevaActiva() {
        assertEquals(MetaAhorro.ACTIVA, meta("0", HOY.plusMonths(1)).getEstado());
    }

    @Test
    @DisplayName("Al alcanzar el objetivo exacto queda COMPLETADA")
    void completada() {
        MetaAhorro meta = meta("1000", HOY.plusMonths(1));
        meta.recalcularEstado(HOY);
        assertEquals(MetaAhorro.COMPLETADA, meta.getEstado());
    }

    @Test
    @DisplayName("Si ya paso la fecha limite sin alcanzar el objetivo queda VENCIDA")
    void vencida() {
        MetaAhorro meta = meta("999.99", HOY.minusDays(1));
        meta.recalcularEstado(HOY);
        assertEquals(MetaAhorro.VENCIDA, meta.getEstado());
    }

    @Test
    @DisplayName("El mismo dia limite la meta sigue ACTIVA")
    void diaLimiteActiva() {
        MetaAhorro meta = meta("500", HOY);
        meta.recalcularEstado(HOY);
        assertEquals(MetaAhorro.ACTIVA, meta.getEstado());
    }

    @Test
    @DisplayName("Una meta cumplida no se vence aunque pase la fecha limite")
    void cumplidaNoSeVence() {
        MetaAhorro meta = meta("1000", HOY.minusDays(10));
        meta.recalcularEstado(HOY);
        assertEquals(MetaAhorro.COMPLETADA, meta.getEstado());
    }

    @Test
    @DisplayName("Una meta completada vuelve a ACTIVA si el acumulado baja antes de la fecha limite")
    void vuelveActiva() {
        MetaAhorro meta = meta("1000", HOY.plusDays(5));
        meta.recalcularEstado(HOY);
        meta.setMontoAcumulado(new BigDecimal("300"));
        meta.recalcularEstado(HOY);
        assertEquals(MetaAhorro.ACTIVA, meta.getEstado());
    }
}
