
package edu.ucentral.zenfi.metasahorro.aplicacion;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.metasahorro.dominio.evento.AporteEliminado;
import edu.ucentral.zenfi.metasahorro.dominio.evento.AporteRegistrado;
import edu.ucentral.zenfi.metasahorro.dominio.modelo.AporteMetaAhorro;
import edu.ucentral.zenfi.metasahorro.dominio.modelo.MetaAhorro;
import edu.ucentral.zenfi.metasahorro.dominio.repositorio.AporteMetaAhorroRepositorio;
import edu.ucentral.zenfi.metasahorro.dominio.repositorio.MetaAhorroRepositorio;
import edu.ucentral.zenfi.metasahorro.infraestructura.dto.AporteMetaAhorroResponse;
import edu.ucentral.zenfi.metasahorro.infraestructura.dto.CrearAporteMetaAhorroRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Aportes a metas de ahorro (HU004).
 *
 * Cruce con movimientos: igual que un abono a una deuda, cada aporte se
 * publica como evento (AporteRegistrado / AporteEliminado) y movimientos
 * lo anota como un gasto en la categoria "Metas de ahorro". Asi el aporte
 * descuenta del presupuesto del mes (HU011), aparece en el reporte
 * (HU025) y en el panel (HU015) sin que esos modulos conozcan las metas.
 */
@ApplicationScoped
public class AporteMetaAhorroServicio {

    /** Sin login (RQ-01/RQ-02 fuera de alcance): mismo usuario fijo que el resto. */
    static final Long USUARIO_ACTUAL = 1L;

    @Inject
    AporteMetaAhorroRepositorio aporteRepositorio;

    @Inject
    MetaAhorroRepositorio metaRepositorio;

    @Inject
    Event<AporteRegistrado> aporteRegistrado;

    @Inject
    Event<AporteEliminado> aporteEliminado;

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

        // HU004: la fecha del aporte no puede quedar vacia
        if (request.fecha() == null) {
            throw new ReglaNegocioException(
                    "La fecha del aporte es obligatoria");
        }

        LocalDate hoy = LocalDate.now();
        meta.recalcularEstado(hoy);

        if (MetaAhorro.COMPLETADA.equals(meta.getEstado())) {
            throw new ReglaNegocioException(
                    "No se pueden realizar aportes a una meta completada");
        }

        if (MetaAhorro.VENCIDA.equals(meta.getEstado())) {
            throw new ReglaNegocioException(
                    "No se pueden realizar aportes a una meta vencida");
        }

        LocalDate fecha = request.fecha();

        if (fecha.isAfter(hoy)) {
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
        meta.recalcularEstado(hoy);

        metaRepositorio.actualizar(meta);

        // Sincrono y en la misma transaccion: si no se puede crear el gasto,
        // el aporte tampoco queda guardado
        aporteRegistrado.fire(new AporteRegistrado(metaAhorroId, guardado.getId(),
                USUARIO_ACTUAL, meta.getNombre(), guardado.getMonto(), guardado.getFecha()));

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
        // Una meta vencida sigue vencida aunque se borre un aporte
        meta.recalcularEstado(LocalDate.now());

        aporteRepositorio.eliminar(aporteId);
        metaRepositorio.actualizar(meta);

        // Movimientos borra el gasto del aporte: vuelve a estar disponible
        aporteEliminado.fire(new AporteEliminado(metaAhorroId, aporteId));
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
