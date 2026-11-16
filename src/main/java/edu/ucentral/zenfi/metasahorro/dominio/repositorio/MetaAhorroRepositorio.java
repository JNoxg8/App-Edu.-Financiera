
package edu.ucentral.zenfi.metasahorro.dominio.repositorio;

import edu.ucentral.zenfi.metasahorro.dominio.modelo.MetaAhorro;

import java.util.List;
import java.util.Optional;

public interface MetaAhorroRepositorio {

    MetaAhorro guardar(MetaAhorro metaAhorro);

    MetaAhorro actualizar(MetaAhorro metaAhorro);

    Optional<MetaAhorro> buscarPorId(Long id);

    /** Todas las metas (las usan los procesos internos, como los recordatorios). */
    List<MetaAhorro> listar();

    List<MetaAhorro> listarPorUsuario(Long usuarioId);

    void eliminar(Long id);
}

