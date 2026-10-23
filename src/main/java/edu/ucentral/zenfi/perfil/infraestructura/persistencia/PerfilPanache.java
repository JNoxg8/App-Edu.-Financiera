package edu.ucentral.zenfi.perfil.infraestructura.persistencia;

import edu.ucentral.zenfi.perfil.dominio.modelo.Perfil;
import edu.ucentral.zenfi.perfil.dominio.repositorio.PerfilRepositorio;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;

/** Adaptador: implementa el puerto PerfilRepositorio con Panache. */
@ApplicationScoped
public class PerfilPanache implements PerfilRepositorio,
        PanacheRepositoryBase<PerfilEntity, Long> {

    @Override
    public Optional<Perfil> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::aDominio);
    }

    @Override
    public boolean existe(Long id) {
        return count("id", id) > 0;
    }

    @Override
    @Transactional
    public Perfil guardar(Perfil perfil) {
        PerfilEntity entidad = new PerfilEntity();
        entidad.id = perfil.getId();
        entidad.nombre = perfil.getNombre();
        entidad.moneda = perfil.getMoneda();
        entidad.nivel = perfil.getNivel();
        entidad.fechaCreacion = perfil.getFechaCreacion();
        persist(entidad);
        return aDominio(entidad);
    }

    private Perfil aDominio(PerfilEntity e) {
        return new Perfil(e.id, e.nombre, e.moneda, e.nivel, e.fechaCreacion);
    }
}