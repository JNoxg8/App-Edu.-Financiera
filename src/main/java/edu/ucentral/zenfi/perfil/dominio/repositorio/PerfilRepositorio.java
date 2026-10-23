package edu.ucentral.zenfi.perfil.dominio.repositorio;

import edu.ucentral.zenfi.perfil.dominio.modelo.Perfil;

import java.util.Optional;

/** Puerto de persistencia del perfil. Lo implementa PerfilPanache. */
public interface PerfilRepositorio {

    Optional<Perfil> buscarPorId(Long id);

    boolean existe(Long id);

    Perfil guardar(Perfil perfil);
}