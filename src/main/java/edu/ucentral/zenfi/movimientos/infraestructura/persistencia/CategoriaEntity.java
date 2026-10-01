package edu.ucentral.zenfi.movimientos.infraestructura.persistencia;

import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Tabla "categoria_movimiento".
 *
 * Las predefinidas tienen usuario_id = null; las personalizadas, el id
 * del usuario que las creo.
 */
@Entity
@Table(name = "categoria_movimiento")
public class CategoriaEntity extends PanacheEntity {

    @Column(nullable = false, length = 40)
    public String nombre;

    /** STRING guarda "GASTO" y no un numero: reordenar el enum no altera los datos. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    public TipoMovimiento tipo;

    @Column(nullable = false)
    public boolean predefinida;

    @Column(name = "usuario_id")
    public Long usuarioId;
}
