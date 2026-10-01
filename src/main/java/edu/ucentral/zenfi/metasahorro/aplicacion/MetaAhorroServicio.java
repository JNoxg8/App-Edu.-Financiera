package edu.ucentral.zenfi.metasahorro.aplicacion;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.metasahorro.dominio.modelo.MetaAhorro;
import edu.ucentral.zenfi.metasahorro.dominio.repositorio.MetaAhorroRepositorio;
import edu.ucentral.zenfi.metasahorro.infraestructura.dto.CrearMetaAhorroRequest;
import edu.ucentral.zenfi.metasahorro.infraestructura.dto.MetaAhorroResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;

@ApplicationScoped
public class MetaAhorroServicio {

    @Inject
    MetaAhorroRepositorio repositorio;

    @Transactional
    public MetaAhorroResponse crear(CrearMetaAhorroRequest request) {

        if (!request.fechaLimite().isAfter(LocalDate.now())) {
            throw new ReglaNegocioException(
                    "La fecha límite debe ser posterior a la fecha actual");
        }

        if (request.montoInicial().compareTo(request.montoObjetivo()) >= 0) {
            throw new ReglaNegocioException(
                    "El monto inicial debe ser menor que el monto objetivo");
        }

        MetaAhorro metaAhorro = new MetaAhorro(
                request.nombre(),
                request.montoObjetivo(),
                request.fechaLimite(),
                request.montoInicial()
        );

        MetaAhorro guardada = repositorio.guardar(metaAhorro);

        return new MetaAhorroResponse(
                guardada.getId(),
                guardada.getNombre(),
                guardada.getMontoObjetivo(),
                guardada.getFechaLimite(),
                guardada.getMontoInicial(),
                guardada.getMontoAcumulado(),
                guardada.getEstado()
        );
    }
}
