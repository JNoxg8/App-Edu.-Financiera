package edu.ucentral.zenfi.panel.aplicacion;

import edu.ucentral.zenfi.panel.dominio.modelo.PanelPrincipal;
import edu.ucentral.zenfi.panel.dominio.puerto.DeudasPanelPuerto;
import edu.ucentral.zenfi.panel.dominio.puerto.MetasPanelPuerto;
import edu.ucentral.zenfi.panel.dominio.puerto.MovimientosPanelPuerto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.YearMonth;

/** Caso de uso de HU015: armar el panel principal. */
@ApplicationScoped
public class PanelServicio {

    /** Fecha minima para el balance "de siempre": antes no hay movimientos. */
    static final LocalDate INICIO = LocalDate.of(1900, 1, 1);

    @Inject
    MovimientosPanelPuerto movimientos;

    @Inject
    MetasPanelPuerto metas;

    @Inject
    DeudasPanelPuerto deudas;

    public PanelPrincipal generar(Long usuarioId, LocalDate hoy) {
        YearMonth mes = YearMonth.from(hoy);
        return PanelPrincipal.generar(
                mes,
                movimientos.totales(usuarioId, INICIO, hoy),
                movimientos.totales(usuarioId, mes.atDay(1), mes.atEndOfMonth()),
                metas.metasActivas(),
                deudas.deudasPendientes(usuarioId));
    }
}
