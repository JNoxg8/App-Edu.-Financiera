package edu.ucentral.zenfi.metasahorro.dominio.repositorio;

import edu.ucentral.zenfi.metasahorro.dominio.modelo.MetaAhorro;

import java.util.List;
import java.util.Optional;

public interface MetaAhorroRepositorio {

    MetaAhorro guardar(MetaAhorro metaAhorro);

    Optional<MetaAhorro> buscarPorId(Long id);

    List<MetaAhorro> listar();
}
