package edu.ucentral.zenfi.autenticacion.dominio.puerto;

/**
 * Puerto de salida: como se cifran y se comprueban las contrasenas. El
 * dominio no sabe que se usa BCrypt; lo implementa CifradoBcrypt.
 */
public interface CifradoContrasenas {

    String cifrar(String contrasena);

    boolean coincide(String contrasena, String hash);
}
