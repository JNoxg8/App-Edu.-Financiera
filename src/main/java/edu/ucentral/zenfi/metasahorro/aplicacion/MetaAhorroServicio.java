

package edu.ucentral.zenfi.metasahorro.aplicacion;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.metasahorro.dominio.evento.AporteEliminado;
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
import jakarta.enterprise.event.Event;
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

    @Inject
    Event<AporteEliminado> aporteEliminado;

    @Transactional
    public MetaAhorroResponse crear(Long usuarioId, CrearMetaAhorroRequest request) {

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
        metaAhorro.setUsuarioId(usuarioId);

        MetaAhorro guardada = repositorio.guardar(metaAhorro);

        // El monto inicial es dinero que el usuario YA tenia ahorrado: queda
        // en el historial de aportes, pero no publica AporteRegistrado, asi
        // que no cuenta como gasto del mes (no sale de su presupuesto).
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

    /**
     * Antes de responder se recalcula el estado de cada meta: las que ya
     * pasaron su fecha limite quedan VENCIDA. Las entidades estan
     * administradas por Hibernate, asi que el cambio se guarda solo al
     * terminar la transaccion.
     */
    @Transactional
    public List<MetaAhorroResponse> listar(Long usuarioId) {
        LocalDate hoy = LocalDate.now();
        List<MetaAhorro> metas = repositorio.listarPorUsuario(usuarioId);
        metas.forEach(meta -> meta.recalcularEstado(hoy));
        return metas.stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    @Transactional
    public void eliminar(Long usuarioId, Long id) {

        MetaAhorro meta = obtenerDe(usuarioId, id);

        List<AporteMetaAhorro> aportes =
                aporteRepositorio.listarPorMetaAhorroId(id);

        // HU021: se borran todos los aportes. Cada uno avisa a movimientos
        // para que borre su gasto: el dinero de una meta eliminada vuelve
        // a estar disponible (a diferencia de un abono, que si se pago).
        for (AporteMetaAhorro aporte : aportes) {
            aporteRepositorio.eliminar(aporte.getId());
            aporteEliminado.fire(new AporteEliminado(id, aporte.getId()));
        }

        recordatorioRepositorio.eliminarPorMetaAhorroId(id);

        notificacionRepositorio.desvincularMeta(id);

        repositorio.eliminar(meta.getId());
    }

    /**
     * Meta DEL USUARIO. Una meta de otro usuario responde 404, igual que si
     * no existiera: asi no se revela que ese id esta ocupado.
     */
    public MetaAhorro obtenerDe(Long usuarioId, Long id) {
        return repositorio.buscarPorId(id)
                .filter(meta -> meta.perteneceA(usuarioId))
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("No se encontró la meta de ahorro"));
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

