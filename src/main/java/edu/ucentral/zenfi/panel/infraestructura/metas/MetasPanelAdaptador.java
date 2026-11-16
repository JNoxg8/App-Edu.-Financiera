package edu.ucentral.zenfi.panel.infraestructura.metas;

import edu.ucentral.zenfi.metasahorro.aplicacion.MetaAhorroServicio;
import edu.ucentral.zenfi.metasahorro.dominio.modelo.MetaAhorro;
import edu.ucentral.zenfi.panel.dominio.modelo.ResumenMeta;
import edu.ucentral.zenfi.panel.dominio.puerto.MetasPanelPuerto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Adaptador de salida: lista las metas con el servicio de HU004, que
 * recalcula su estado (las vencidas o cumplidas no se muestran).
 */
@ApplicationScoped
public class MetasPanelAdaptador implements MetasPanelPuerto {

    @Inject
    MetaAhorroServicio metas;

    @Override
    public List<ResumenMeta> metasActivas() {
        return metas.listar().stream()
                .filter(m -> MetaAhorro.ACTIVA.equals(m.estado()))
                .map(m -> ResumenMeta.de(m.id(), m.nombre(), m.montoObjetivo(),
                        m.montoAcumulado(), m.fechaLimite()))
                .toList();
    }
}
