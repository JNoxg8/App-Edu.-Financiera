package edu.ucentral.zenfi.autenticacion.infraestructura.persistencia;

import edu.ucentral.zenfi.autenticacion.dominio.modelo.Cuenta;
import edu.ucentral.zenfi.autenticacion.dominio.repositorio.CuentaRepositorio;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;

/** Adaptador de salida: cuentas con Panache. */
@ApplicationScoped
public class CuentaPanache implements CuentaRepositorio, PanacheRepository<CuentaEntity> {

    @Override
    @Transactional
    public Cuenta crear(Cuenta cuenta) {
        CuentaEntity entidad = new CuentaEntity();
        entidad.correo = cuenta.getCorreo();
        entidad.hashContrasena = cuenta.getHashContrasena();
        entidad.fechaCreacion = cuenta.getFechaCreacion();
        persist(entidad);
        return aDominio(entidad);
    }

    @Override
    public Optional<Cuenta> buscarPorCorreo(String correo) {
        return find("correo", correo).firstResultOptional().map(this::aDominio);
    }

    @Override
    public Optional<Cuenta> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::aDominio);
    }

    private Cuenta aDominio(CuentaEntity e) {
        return new Cuenta(e.id, e.correo, e.hashContrasena, e.fechaCreacion);
    }
}
