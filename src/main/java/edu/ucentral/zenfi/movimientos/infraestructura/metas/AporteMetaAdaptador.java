package edu.ucentral.zenfi.movimientos.infraestructura.metas;

import edu.ucentral.zenfi.metasahorro.aplicacion.AporteMetaAhorroServicio;
import edu.ucentral.zenfi.movimientos.dominio.puerto.AporteMetaPuerto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Adaptador de salida: implementa AporteMetaPuerto con el servicio del
 * modulo de metas. Es el UNICO punto de movimientos que conoce clases de
 * metas (como PagoDeudaAdaptador con deudas).
 */
@ApplicationScoped
public class AporteMetaAdaptador implements AporteMetaPuerto {

    @Inject
    AporteMetaAhorroServicio aportes;

    @Override
    public void revertirAporte(Long usuarioId, Long metaAhorroId, Long aporteId) {
        aportes.eliminar(usuarioId, metaAhorroId, aporteId);
    }
}
