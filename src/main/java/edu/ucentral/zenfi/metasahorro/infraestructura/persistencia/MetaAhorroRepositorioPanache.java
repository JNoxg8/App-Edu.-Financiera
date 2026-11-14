

package edu.ucentral.zenfi.metasahorro.infraestructura.persistencia;

import edu.ucentral.zenfi.metasahorro.dominio.modelo.MetaAhorro;
import edu.ucentral.zenfi.metasahorro.dominio.repositorio.MetaAhorroRepositorio;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MetaAhorroRepositorioPanache
        implements MetaAhorroRepositorio, PanacheRepository<MetaAhorro> {

    @Override
    @Transactional
    public MetaAhorro guardar(MetaAhorro metaAhorro) {
        persist(metaAhorro);
        return metaAhorro;
    }

    @Override
    @Transactional
    public MetaAhorro actualizar(MetaAhorro metaAhorro) {

        MetaAhorro existente = findById(metaAhorro.getId());

        if (existente == null) {
            return null;
        }

        existente.setNombre(metaAhorro.getNombre());
        existente.setMontoObjetivo(metaAhorro.getMontoObjetivo());
        existente.setFechaLimite(metaAhorro.getFechaLimite());
        existente.setMontoInicial(metaAhorro.getMontoInicial());
        existente.setMontoAcumulado(metaAhorro.getMontoAcumulado());
        existente.setEstado(metaAhorro.getEstado());

        return existente;
    }

    @Override
    @Transactional
    public Optional<MetaAhorro> buscarPorId(Long id) {
        return findByIdOptional(id);
    }

    @Override
    @Transactional
    public List<MetaAhorro> listar() {
        return listAll();
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        deleteById(id);
    }
}

