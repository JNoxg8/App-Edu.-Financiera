package edu.ucentral.zenfi.panel.infraestructura.presupuesto;

import edu.ucentral.zenfi.panel.dominio.puerto.PresupuestoPanelPuerto;
import edu.ucentral.zenfi.presupuesto.aplicacion.PresupuestoServicio;
import edu.ucentral.zenfi.presupuesto.dominio.modelo.Presupuesto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.YearMonth;
import java.util.Optional;

/** Adaptador de salida: busca el presupuesto del mes con el servicio de HU011. */
@ApplicationScoped
public class PresupuestoPanelAdaptador implements PresupuestoPanelPuerto {

    @Inject
    PresupuestoServicio presupuestos;

    @Override
    public Optional<Presupuesto> buscar(Long usuarioId, YearMonth mes) {
        return presupuestos.buscar(usuarioId, mes);
    }
}
