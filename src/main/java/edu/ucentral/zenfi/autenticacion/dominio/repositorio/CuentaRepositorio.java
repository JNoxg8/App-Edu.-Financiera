package edu.ucentral.zenfi.autenticacion.dominio.repositorio;

import edu.ucentral.zenfi.autenticacion.dominio.modelo.Cuenta;

import java.util.Optional;

public interface CuentaRepositorio {

    Cuenta crear(Cuenta cuenta);

    Optional<Cuenta> buscarPorCorreo(String correo);

    Optional<Cuenta> buscarPorId(Long id);
}
