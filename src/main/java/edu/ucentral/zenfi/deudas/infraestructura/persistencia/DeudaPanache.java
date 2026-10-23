package edu.ucentral.zenfi.deudas.infraestructura.persistencia;

import edu.ucentral.zenfi.deudas.dominio.modelo.Abono;
import edu.ucentral.zenfi.deudas.dominio.modelo.Deuda;
import edu.ucentral.zenfi.deudas.dominio.repositorio.DeudaRepositorio;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida: implementa el puerto DeudaRepositorio usando Panache.
 *
 * Su otra responsabilidad es traducir entre los dos mundos:
 *   Deuda (dominio)  <-->  DeudaEntity (base de datos)
 *
 * Si manana cambiaran a MongoDB, solo se reescribe esta clase.
 */
@ApplicationScoped
public class DeudaPanache implements DeudaRepositorio, PanacheRepository<DeudaEntity> {

    @Override
    @Transactional
    public Deuda crear(Deuda deuda) {
        DeudaEntity entidad = new DeudaEntity();
        entidad.acreedor = deuda.getAcreedor();
        entidad.montoTotal = deuda.getMontoTotal();
        entidad.tasaInteresAnual = deuda.getTasaInteresAnual();
        entidad.numeroCuotas = deuda.getNumeroCuotas();
        entidad.fechaVencimiento = deuda.getFechaVencimiento();
        entidad.usuarioId = deuda.getUsuarioId();

        persist(entidad);
        return aDominio(entidad);
    }

    @Override
    @Transactional
    public Deuda actualizar(Deuda deuda) {
        DeudaEntity entidad = findById(deuda.getId());
        if (entidad == null) {
            return null;
        }

        entidad.acreedor = deuda.getAcreedor();
        entidad.tasaInteresAnual = deuda.getTasaInteresAnual();
        entidad.numeroCuotas = deuda.getNumeroCuotas();
        entidad.fechaVencimiento = deuda.getFechaVencimiento();
        // El montoTotal no se toca: RQ-22 no permite editarlo

        return aDominio(entidad);
    }

    @Override
    @Transactional
    public Optional<Deuda> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::aDominio);
    }

    @Override
    @Transactional
    public List<Deuda> listarPorUsuario(Long usuarioId) {
        return find("usuarioId = ?1 order by fechaVencimiento asc", usuarioId)
                .list()
                .stream()
                .map(this::aDominio)
                .toList();
    }

    @Override
    @Transactional
    public void agregarAbono(Long deudaId, Abono abono) {
        DeudaEntity deuda = findById(deudaId);
        if (deuda == null) {
            return;
        }

        AbonoEntity entidad = new AbonoEntity();
        entidad.monto = abono.getMonto();
        entidad.fecha = abono.getFecha();
        entidad.deuda = deuda;
        entidad.persist();

        deuda.abonos.add(entidad);
        abono.setId(entidad.id);
    }

    @Override
    @Transactional
    public void eliminarAbono(Long deudaId, Long abonoId) {
        DeudaEntity deuda = findById(deudaId);
        if (deuda == null) {
            return;
        }
        // orphanRemoval = true: sacarlo de la lista lo borra de la tabla
        deuda.abonos.removeIf(a -> a.id.equals(abonoId));
    }

    @Override
    @Transactional
    public boolean eliminar(Long id) {
        return deleteById(id);
    }

    /** Traduce la entidad JPA al modelo de dominio. */
    private Deuda aDominio(DeudaEntity e) {
        List<Abono> abonos = new ArrayList<>();
        if (e.abonos != null) {
            for (AbonoEntity a : e.abonos) {
                abonos.add(new Abono(a.id, a.monto, a.fecha));
            }
        }

        return new Deuda(
                e.id,
                e.acreedor,
                e.montoTotal,
                e.tasaInteresAnual,
                e.numeroCuotas,
                e.fechaVencimiento,
                e.usuarioId,
                abonos
        );
    }
}
