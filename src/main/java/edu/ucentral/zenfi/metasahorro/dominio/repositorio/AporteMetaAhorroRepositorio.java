
package edu.ucentral.zenfi.metasahorro.dominio.repositorio;

import edu.ucentral.zenfi.metasahorro.dominio.modelo.AporteMetaAhorro;

import java.util.List;
import java.util.Optional;

public interface AporteMetaAhorroRepositorio {

    AporteMetaAhorro guardar(AporteMetaAhorro aporte);

    List<AporteMetaAhorro> listarPorMetaAhorroId(Long metaAhorroId);

    Optional<AporteMetaAhorro> buscarPorId(Long id);

    void eliminar(Long id);
}
