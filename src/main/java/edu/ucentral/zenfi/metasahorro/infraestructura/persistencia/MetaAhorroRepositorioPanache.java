package edu.ucentral.zenfi.metasahorro.infraestructura.persistencia;

import edu.ucentral.zenfi.metasahorro.dominio.modelo.MetaAhorro;
import edu.ucentral.zenfi.metasahorro.dominio.repositorio.MetaAhorroRepositorio;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class MetaAhorroRepositorioPanache
        implements MetaAhorroRepositorio, PanacheRepository<MetaAhorro> {

    @Override
    public MetaAhorro guardar(MetaAhorro metaAhorro) {
        persist(metaAhorro);
        return metaAhorro;
    }

    @Override
    public Optional<MetaAhorro> buscarPorId(Long id) {
        return findByIdOptional(id);
    }

    @Override
    public List<MetaAhorro> listar() {
        return listAll();
    }
}
