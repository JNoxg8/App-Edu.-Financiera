package edu.ucentral.zenfi.reporte.dominio.puerto;

import edu.ucentral.zenfi.presupuesto.dominio.modelo.Presupuesto;

import java.time.YearMonth;
import java.util.Optional;

/**
 * Puerto de salida: el presupuesto definido para un mes (HU011).
 * Lo implementa un adaptador que lee el modulo de presupuesto.
 */
public interface PresupuestoMesPuerto {

    /** Vacio si el mes no tiene presupuesto: para el reporte no es un error. */
    Optional<Presupuesto> buscar(Long usuarioId, YearMonth mes);
}
