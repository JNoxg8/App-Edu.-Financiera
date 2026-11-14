
package edu.ucentral.zenfi.metasahorro.aplicacion;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.metasahorro.dominio.modelo.AporteMetaAhorro;
import edu.ucentral.zenfi.metasahorro.dominio.modelo.MetaAhorro;
import edu.ucentral.zenfi.metasahorro.dominio.repositorio.AporteMetaAhorroRepositorio;
import edu.ucentral.zenfi.metasahorro.dominio.repositorio.MetaAhorroRepositorio;
import edu.ucentral.zenfi.metasahorro.infraestructura.dto.AporteMetaAhorroResponse;
import edu.ucentral.zenfi.metasahorro.infraestructura.dto.CrearAporteMetaAhorroRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AporteMetaAhorroServicio {

    @Inject
    AporteMetaAhorroRepositorio aporteRepositorio;

    @Inject
    MetaAhorroRepositorio metaRepositorio;

    @Transactional
    public AporteMetaAhorroResponse registrar(
            Long metaAhorroId,
            CrearAporteMetaAhorroRequest request) {

        MetaAhorro meta = metaRepositorio.buscarPorId(metaAhorroId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la meta de ahorro"));

        if (request == null || request.monto() == null) {
            throw new ReglaNegocioException(
                    "El monto del aporte es obligatorio");
        }

        if (request.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException(
                    "El aporte debe ser mayor que cero");
        }

        if ("COMPLETADA".equals(meta.getEstado())) {
            throw new ReglaNegocioException(
                    "No se pueden realizar aportes a una meta completada");
        }

        LocalDate fecha = request.fecha() == null
                ? LocalDate.now()
                : request.fecha();

        if (fecha.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException(
                    "La fecha del aporte no puede ser futura");
        }

        BigDecimal pendiente = meta.getMontoObjetivo()
                .subtract(meta.getMontoAcumulado());

        if (request.monto().compareTo(pendiente) > 0) {
            throw new ReglaNegocioException(
                    "El aporte supera el monto pendiente de la meta");
        }

        AporteMetaAhorro aporte = new AporteMetaAhorro(
                null,
                request.monto(),
                fecha,
                metaAhorroId
        );

        AporteMetaAhorro guardado = aporteRepositorio.guardar(aporte);

        BigDecimal nuevoAcumulado = meta.getMontoAcumulado()
                .add(request.monto());

        meta.setMontoAcumulado(nuevoAcumulado);

        if (nuevoAcumulado.compareTo(meta.getMontoObjetivo()) >= 0) {
            meta.setEstado("COMPLETADA");
        }

        metaRepositorio.actualizar(meta);

        return aRespuesta(guardado);
    }

    @Transactional
    public List<AporteMetaAhorroResponse> listar(Long metaAhorroId) {

        metaRepositorio.buscarPorId(metaAhorroId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la meta de ahorro"));

        return aporteRepositorio.listarPorMetaAhorroId(metaAhorroId)
                .stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional
    public void eliminar(Long metaAhorroId, Long aporteId) {

        MetaAhorro meta = metaRepositorio.buscarPorId(metaAhorroId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la meta de ahorro"));

        AporteMetaAhorro aporte = aporteRepositorio.buscarPorId(aporteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el aporte"));

        if (!aporte.getMetaAhorroId().equals(metaAhorroId)) {
            throw new RecursoNoEncontradoException(
                    "El aporte no pertenece a esta meta");
        }

        BigDecimal nuevoAcumulado = meta.getMontoAcumulado()
                .subtract(aporte.getMonto());

        meta.setMontoAcumulado(nuevoAcumulado);

        if (nuevoAcumulado.compareTo(meta.getMontoObjetivo()) >= 0) {
            meta.setEstado("COMPLETADA");
        } else {
            meta.setEstado("ACTIVA");
        }

        aporteRepositorio.eliminar(aporteId);
        metaRepositorio.actualizar(meta);
    }

    private AporteMetaAhorroResponse aRespuesta(AporteMetaAhorro aporte) {
        return new AporteMetaAhorroResponse(
                aporte.getId(),
                aporte.getMetaAhorroId(),
                aporte.getMonto(),
                aporte.getFecha()
        );
    }
}
