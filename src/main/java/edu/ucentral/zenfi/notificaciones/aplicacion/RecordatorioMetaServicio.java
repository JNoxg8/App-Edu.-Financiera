
package edu.ucentral.zenfi.notificaciones.aplicacion;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.metasahorro.dominio.repositorio.MetaAhorroRepositorio;
import edu.ucentral.zenfi.notificaciones.dominio.modelo.RecordatorioMeta;
import edu.ucentral.zenfi.notificaciones.dominio.repositorio.RecordatorioMetaRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;

@ApplicationScoped
public class RecordatorioMetaServicio {

    @Inject
    RecordatorioMetaRepositorio repositorio;

    @Inject
    MetaAhorroRepositorio metaRepositorio;

    @Transactional
    public RecordatorioMeta crearParaMeta(Long metaAhorroId) {

        validarMeta(metaAhorroId);

        if (repositorio.buscarPorMetaAhorroId(metaAhorroId).isPresent()) {
            throw new ReglaNegocioException(
                    "La meta ya tiene un recordatorio configurado");
        }

        return repositorio.guardar(new RecordatorioMeta(metaAhorroId));
    }

    public RecordatorioMeta consultar(Long metaAhorroId) {

        validarMeta(metaAhorroId);

        return repositorio.buscarPorMetaAhorroId(metaAhorroId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La meta no tiene un recordatorio configurado"));
    }

    /**
     * Cambia la frecuencia y el estado en una sola transaccion: si algo
     * falla, no queda guardada solo la mitad del cambio.
     *
     * La proxima fecha solo se recalcula si cambia la frecuencia o si la
     * que habia ya paso (por ejemplo, al reactivar un recordatorio que
     * estuvo apagado). Asi, encender o apagar el recordatorio no corre la
     * fecha que el usuario ya tenia.
     */
    @Transactional
    public RecordatorioMeta configurar(
            Long metaAhorroId,
            String frecuencia,
            boolean activo) {

        validarFrecuencia(frecuencia);

        RecordatorioMeta recordatorio = consultar(metaAhorroId);

        LocalDate hoy = LocalDate.now();
        LocalDate proximo = recordatorio.getProximoRecordatorio();

        if (!frecuencia.equals(recordatorio.getFrecuencia())
                || !proximo.isAfter(hoy)) {
            proximo = sumarPeriodo(hoy, frecuencia);
        }

        recordatorio.configurar(frecuencia, proximo);

        if (activo) {
            recordatorio.activar();
        } else {
            recordatorio.desactivar();
        }

        repositorio.actualizar(recordatorio);

        return recordatorio;
    }

    @Transactional
    public void avanzarProximoRecordatorio(RecordatorioMeta recordatorio) {

        LocalDate siguiente = recordatorio.getProximoRecordatorio();
        LocalDate hoy = LocalDate.now();

        while (!siguiente.isAfter(hoy)) {
            siguiente = sumarPeriodo(siguiente, recordatorio.getFrecuencia());
        }

        recordatorio.actualizarProximoRecordatorio(siguiente);
        repositorio.actualizar(recordatorio);
    }

    private void validarMeta(Long metaAhorroId) {
        if (metaRepositorio.buscarPorId(metaAhorroId).isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "No se encontró la meta de ahorro");
        }
    }

    private void validarFrecuencia(String frecuencia) {
        if (frecuencia == null ||
                !frecuencia.equals("DIARIA") &&
                        !frecuencia.equals("SEMANAL") &&
                        !frecuencia.equals("QUINCENAL") &&
                        !frecuencia.equals("MENSUAL")) {

            throw new ReglaNegocioException(
                    "Frecuencia no válida. Use DIARIA, SEMANAL, QUINCENAL o MENSUAL");
        }
    }

    private LocalDate sumarPeriodo(LocalDate fecha, String frecuencia) {
        return switch (frecuencia) {
            case "DIARIA" -> fecha.plusDays(1);
            case "SEMANAL" -> fecha.plusWeeks(1);
            case "QUINCENAL" -> fecha.plusDays(15);
            case "MENSUAL" -> fecha.plusMonths(1);
            default -> throw new ReglaNegocioException("Frecuencia no válida");
        };
    }
}
