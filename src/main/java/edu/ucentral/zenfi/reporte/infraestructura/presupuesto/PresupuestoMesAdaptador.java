package edu.ucentral.zenfi.reporte.infraestructura.presupuesto;

import edu.ucentral.zenfi.presupuesto.aplicacion.PresupuestoServicio;
import edu.ucentral.zenfi.presupuesto.dominio.modelo.Presupuesto;
import edu.ucentral.zenfi.reporte.dominio.puerto.PresupuestoMesPuerto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.YearMonth;
import java.util.Optional;

/**
 * Adaptador de salida: implementa PresupuestoMesPuerto con el servicio
 * del modulo de presupuesto (HU011).
 */
@ApplicationScoped
public class PresupuestoMesAdaptador implements PresupuestoMesPuerto {

    @Inject
    PresupuestoServicio presupuestos;

    @Override
    public Optional<Presupuesto> buscar(Long usuarioId, YearMonth mes) {
        return presupuestos.buscar(usuarioId, mes);
    }
}
