
package edu.ucentral.zenfi.notificaciones.aplicacion;

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

        if (metaRepositorio.buscarPorId(metaAhorroId).isEmpty()) {
            throw new ReglaNegocioException("La meta de ahorro no existe");
        }

        if (repositorio.buscarPorMetaAhorroId(metaAhorroId).isPresent()) {
            throw new ReglaNegocioException(
                    "La meta ya tiene un recordatorio configurado");
        }

        return repositorio.guardar(new RecordatorioMeta(metaAhorroId));
    }

    public RecordatorioMeta consultar(Long metaAhorroId) {
        return repositorio.buscarPorMetaAhorroId(metaAhorroId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La meta no tiene un recordatorio configurado"));
    }

    @Transactional
    public RecordatorioMeta configurar(
            Long metaAhorroId,
            String frecuencia) {

        RecordatorioMeta recordatorio = consultar(metaAhorroId);

        validarFrecuencia(frecuencia);

        recordatorio.configurar(
                frecuencia,
                calcularProximaFecha(frecuencia)
        );

        repositorio.actualizar(recordatorio);

        return recordatorio;
    }

    @Transactional
    public RecordatorioMeta cambiarEstado(
            Long metaAhorroId,
            boolean activo) {

        RecordatorioMeta recordatorio = consultar(metaAhorroId);

        if (activo) {
            recordatorio.activar();
        } else {
            recordatorio.desactivar();
        }

        repositorio.actualizar(recordatorio);

        return recordatorio;
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

    private LocalDate calcularProximaFecha(String frecuencia) {
        LocalDate hoy = LocalDate.now();

        return switch (frecuencia) {
            case "DIARIA" -> hoy.plusDays(1);
            case "SEMANAL" -> hoy.plusWeeks(1);
            case "QUINCENAL" -> hoy.plusDays(15);
            case "MENSUAL" -> hoy.plusMonths(1);
            default -> throw new ReglaNegocioException(
                    "Frecuencia no válida");
        };
    }



    @Transactional
    public void avanzarProximoRecordatorio(RecordatorioMeta recordatorio) {

        LocalDate siguiente = recordatorio.getProximoRecordatorio();
        LocalDate hoy = LocalDate.now();

        while (!siguiente.isAfter(hoy)) {
            siguiente = switch (recordatorio.getFrecuencia()) {
                case "DIARIA" -> siguiente.plusDays(1);
                case "SEMANAL" -> siguiente.plusWeeks(1);
                case "QUINCENAL" -> siguiente.plusDays(15);
                case "MENSUAL" -> siguiente.plusMonths(1);
                default -> throw new ReglaNegocioException("Frecuencia no válida");
            };
        }

        recordatorio.actualizarProximoRecordatorio(siguiente);
        repositorio.actualizar(recordatorio);
    }


}

