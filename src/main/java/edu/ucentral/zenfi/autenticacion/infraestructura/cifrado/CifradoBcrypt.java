package edu.ucentral.zenfi.autenticacion.infraestructura.cifrado;

import edu.ucentral.zenfi.autenticacion.dominio.puerto.CifradoContrasenas;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.util.ModularCrypt;

/**
 * Adaptador de salida: cifra con BCrypt.
 *
 * BCrypt es lento a proposito y agrega una sal aleatoria a cada
 * contrasena: dos usuarios con la misma contrasena tienen hashes
 * distintos, y probar millones de contrasenas contra un hash robado se
 * vuelve muy costoso.
 */
@ApplicationScoped
public class CifradoBcrypt implements CifradoContrasenas {

    @Override
    public String cifrar(String contrasena) {
        return BcryptUtil.bcryptHash(contrasena);
    }

    @Override
    public boolean coincide(String contrasena, String hash) {
        try {
            Password guardada = ModularCrypt.decode(hash);
            PasswordFactory fabrica = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT);
            BCryptPassword bcrypt = (BCryptPassword) fabrica.translate(guardada);
            return fabrica.verify(bcrypt, contrasena.toCharArray());
        } catch (Exception e) {
            // Un hash danado o en otro formato nunca debe dejar entrar
            return false;
        }
    }
}
