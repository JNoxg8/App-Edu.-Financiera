package edu.ucentral.zenfi.panel.dominio.puerto;

import edu.ucentral.zenfi.presupuesto.dominio.modelo.Presupuesto;

import java.time.YearMonth;
import java.util.Optional;

/** Puerto de salida: el presupuesto del mes (HU011), si el usuario lo definio. */
public interface PresupuestoPanelPuerto {

    Optional<Presupuesto> buscar(Long usuarioId, YearMonth mes);
}
