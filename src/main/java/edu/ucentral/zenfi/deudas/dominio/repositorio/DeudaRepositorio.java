package edu.ucentral.zenfi.deudas.dominio.repositorio;

import edu.ucentral.zenfi.deudas.dominio.modelo.Abono;
import edu.ucentral.zenfi.deudas.dominio.modelo.Deuda;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: el dominio declara QUE necesita de la persistencia,
 * sin decir COMO se hace. La implementacion con Panache vive en
 * infraestructura/persistencia.
 *
 * Gracias a esta interfaz el servicio se puede probar con @InjectMock,
 * sin base de datos.
 */
public interface DeudaRepositorio {

    Deuda crear(Deuda deuda);

    Deuda actualizar(Deuda deuda);

    Optional<Deuda> buscarPorId(Long id);

    List<Deuda> listarPorUsuario(Long usuarioId);

    void agregarAbono(Long deudaId, Abono abono);

    boolean eliminar(Long id);
}
