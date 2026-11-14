

package edu.ucentral.zenfi.metasahorro.aplicacion;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.metasahorro.dominio.modelo.MetaAhorro;
import edu.ucentral.zenfi.metasahorro.dominio.modelo.AporteMetaAhorro;
import edu.ucentral.zenfi.metasahorro.dominio.repositorio.MetaAhorroRepositorio;
import edu.ucentral.zenfi.metasahorro.dominio.repositorio.AporteMetaAhorroRepositorio;
import edu.ucentral.zenfi.metasahorro.infraestructura.dto.CrearMetaAhorroRequest;
import edu.ucentral.zenfi.metasahorro.infraestructura.dto.MetaAhorroResponse;
import edu.ucentral.zenfi.notificaciones.aplicacion.RecordatorioMetaServicio;
import edu.ucentral.zenfi.notificaciones.dominio.repositorio.RecordatorioMetaRepositorio;
import edu.ucentral.zenfi.notificaciones.dominio.repositorio.NotificacionRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class MetaAhorroServicio {

    @Inject
    MetaAhorroRepositorio repositorio;

    @Inject
    AporteMetaAhorroRepositorio aporteRepositorio;

    @Inject
    RecordatorioMetaServicio recordatorioServicio;

    @Inject
    RecordatorioMetaRepositorio recordatorioRepositorio;

    @Inject
    NotificacionRepositorio notificacionRepositorio;

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

        if (request.montoInicial().compareTo(BigDecimal.ZERO) > 0) {

            AporteMetaAhorro aporteInicial = new AporteMetaAhorro(
                    null,
                    request.montoInicial(),
                    LocalDate.now(),
                    guardada.getId()
            );

            aporteRepositorio.guardar(aporteInicial);
        }

        recordatorioServicio.crearParaMeta(guardada.getId());

        return convertirRespuesta(guardada);
    }

    public List<MetaAhorroResponse> listar() {
        return repositorio.listar()
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    @Transactional
    public void eliminar(Long id) {

        MetaAhorro meta = repositorio.buscarPorId(id)
                .orElseThrow(() ->
                        new ReglaNegocioException("La meta de ahorro no existe"));

        List<AporteMetaAhorro> aportes =
                aporteRepositorio.listarPorMetaAhorroId(id);

        for (AporteMetaAhorro aporte : aportes) {
            aporteRepositorio.eliminar(aporte.getId());
        }

        recordatorioRepositorio.eliminarPorMetaAhorroId(id);

        notificacionRepositorio.desvincularMeta(id);

        repositorio.eliminar(meta.getId());
    }

    private MetaAhorroResponse convertirRespuesta(MetaAhorro meta) {
        return new MetaAhorroResponse(
                meta.getId(),
                meta.getNombre(),
                meta.getMontoObjetivo(),
                meta.getFechaLimite(),
                meta.getMontoInicial(),
                meta.getMontoAcumulado(),
                meta.getEstado()
        );
    }
}

