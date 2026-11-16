package edu.ucentral.zenfi.autenticacion.infraestructura.persistencia;

import edu.ucentral.zenfi.autenticacion.dominio.repositorio.SesionRepositorio;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/** Adaptador de salida: sesiones con Panache. */
@ApplicationScoped
public class SesionPanache implements SesionRepositorio, PanacheRepository<SesionEntity> {

    @Override
    @Transactional
    public void crear(String hashToken, Long cuentaId, LocalDateTime expira) {
        SesionEntity entidad = new SesionEntity();
        entidad.hashToken = hashToken;
        entidad.cuentaId = cuentaId;
        entidad.expira = expira;
        persist(entidad);
    }

    @Override
    public Optional<Long> cuentaDeSesionVigente(String hashToken, LocalDateTime ahora) {
        return find("hashToken = ?1 and expira > ?2", hashToken, ahora)
                .firstResultOptional()
                .map(s -> s.cuentaId);
    }

    @Override
    @Transactional
    public void eliminar(String hashToken) {
        delete("hashToken", hashToken);
    }
}
