package edu.ucentral.zenfi.panel.infraestructura.deudas;

import edu.ucentral.zenfi.deudas.aplicacion.DeudaServicio;
import edu.ucentral.zenfi.panel.dominio.modelo.ResumenDeuda;
import edu.ucentral.zenfi.panel.dominio.puerto.DeudasPanelPuerto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/** Adaptador de salida: las deudas de HU007 que aun no estan pagadas. */
@ApplicationScoped
public class DeudasPanelAdaptador implements DeudasPanelPuerto {

    @Inject
    DeudaServicio deudas;

    @Override
    public List<ResumenDeuda> deudasPendientes(Long usuarioId) {
        return deudas.listar(usuarioId).stream()
                .filter(d -> !d.estaPagada())
                .map(d -> new ResumenDeuda(d.getId(), d.getAcreedor(), d.saldoPendiente(),
                        d.porcentajePagado(), d.getFechaVencimiento()))
                .toList();
    }
}
