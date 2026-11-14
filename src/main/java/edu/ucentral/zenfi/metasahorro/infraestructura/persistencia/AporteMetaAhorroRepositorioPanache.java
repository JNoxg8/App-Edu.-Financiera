
package edu.ucentral.zenfi.metasahorro.infraestructura.persistencia;

import edu.ucentral.zenfi.metasahorro.dominio.modelo.AporteMetaAhorro;
import edu.ucentral.zenfi.metasahorro.dominio.modelo.MetaAhorro;
import edu.ucentral.zenfi.metasahorro.dominio.repositorio.AporteMetaAhorroRepositorio;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AporteMetaAhorroRepositorioPanache
        implements AporteMetaAhorroRepositorio,
        PanacheRepository<AporteMetaAhorroEntity> {

    @Inject
    EntityManager entityManager;

    @Override
    @Transactional
    public AporteMetaAhorro guardar(AporteMetaAhorro aporte) {

        AporteMetaAhorroEntity entidad = new AporteMetaAhorroEntity();

        entidad.monto = aporte.getMonto();
        entidad.fecha = aporte.getFecha();
        entidad.metaAhorro = entityManager.getReference(
                MetaAhorro.class,
                aporte.getMetaAhorroId()
        );

        persist(entidad);

        aporte.setId(entidad.id);

        return aporte;
    }

    @Override
    @Transactional
    public List<AporteMetaAhorro> listarPorMetaAhorroId(Long metaAhorroId) {

        return find(
                "metaAhorro.id = ?1 order by fecha desc, id desc",
                metaAhorroId
        ).list()
                .stream()
                .map(this::aDominio)
                .toList();
    }

    @Override
    @Transactional
    public Optional<AporteMetaAhorro> buscarPorId(Long id) {

        return findByIdOptional(id).map(this::aDominio);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        deleteById(id);
    }

   private AporteMetaAhorro aDominio(AporteMetaAhorroEntity e) {
    return new AporteMetaAhorro(
            e.id,
            e.monto,
            e.fecha,
            e.metaAhorro.getId()
    );
}
}
