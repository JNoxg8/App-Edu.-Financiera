package edu.ucentral.zenfi.movimientos.infraestructura.persistencia;

import edu.ucentral.zenfi.movimientos.dominio.modelo.CategoriaPredefinida;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Inserta las categorias predefinidas al arrancar la aplicacion.
 *
 * Se hace en codigo y no con import.sql porque import.sql solo corre con
 * drop-and-create; con DB_ESQUEMA=update las categorias no se crearian.
 * Es idempotente: solo inserta las que falten, asi que se puede ejecutar
 * en cada arranque sin duplicar nada.
 */
@ApplicationScoped
public class CategoriaSemilla {

    @Inject
    CategoriaPanache categorias;

    @Transactional
    void alIniciar(@Observes StartupEvent evento) {
        int creadas = 0;
        for (CategoriaPredefinida predefinida : CategoriaPredefinida.values()) {
            boolean existe = categorias.count("predefinida = true and tipo = ?1 and nombre = ?2",
                    predefinida.getTipo(), predefinida.getNombre()) > 0;
            if (!existe) {
                CategoriaEntity entidad = new CategoriaEntity();
                entidad.nombre = predefinida.getNombre();
                entidad.tipo = predefinida.getTipo();
                entidad.predefinida = true;
                entidad.usuarioId = null;
                categorias.persist(entidad);
                creadas++;
            }
        }
        Log.infof("Categorias predefinidas listas (%d nuevas)", creadas);
    }
}
