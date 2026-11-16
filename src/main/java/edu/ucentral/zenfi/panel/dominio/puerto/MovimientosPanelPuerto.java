package edu.ucentral.zenfi.panel.dominio.puerto;

import edu.ucentral.zenfi.panel.dominio.modelo.TotalesMovimientos;

import java.time.LocalDate;

/** Puerto de salida: lo que el panel necesita del modulo de movimientos. */
public interface MovimientosPanelPuerto {

    TotalesMovimientos totales(Long usuarioId, LocalDate desde, LocalDate hasta);
}
