package edu.ucentral.zenfi.movimientos.dominio.modelo;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Categoria de un movimiento (HU009). Java puro: sin JPA ni Quarkus.
 *
 * Hay dos clases de categorias:
 *   - Predefinidas: vienen con la app, usuarioId = null, no se editan.
 *   - Personalizadas: las crea el usuario; son solo suyas.
 *
 * Cada categoria pertenece a un tipo: una categoria de gastos no se
 * puede usar para registrar un ingreso, y al reves.
 */
public class Categoria {

    public static final int LONGITUD_MAXIMA_NOMBRE = 40;

    private Long id;
    private String nombre;
    private final TipoMovimiento tipo;
    private final boolean predefinida;
    private final Long usuarioId;

    /** Reconstruye una categoria ya guardada (lo usa el adaptador de persistencia). */
    public Categoria(Long id, String nombre, TipoMovimiento tipo,
                     boolean predefinida, Long usuarioId) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.predefinida = predefinida;
        this.usuarioId = usuarioId;
    }

    /**
     * Crea una categoria personalizada validando HU009:
     * "el nombre no debe estar vacio". Que no este repetido lo valida
     * el servicio, porque para eso hay que consultar las demas.
     */
    public static Categoria personalizada(String nombre, TipoMovimiento tipo, Long usuarioId) {
        if (tipo == null) {
            throw new ReglaNegocioException("Debe indicar si la categoria es de ingresos o de gastos");
        }
        return new Categoria(null, validarNombre(nombre), tipo, false, usuarioId);
    }

    /** Cambia el nombre. Las categorias predefinidas no se pueden renombrar. */
    public void renombrar(String nuevoNombre) {
        verificarEditable();
        this.nombre = validarNombre(nuevoNombre);
    }

    /** Lanza 409 si la categoria es de fabrica. */
    public void verificarEditable() {
        if (predefinida) {
            throw new ReglaNegocioException(
                    "La categoria '" + nombre + "' es predefinida y no se puede modificar ni eliminar");
        }
    }

    /** La categoria "Deudas" conecta este modulo con el de deudas. */
    public boolean esDeDeudas() {
        return predefinida
                && tipo == CategoriaPredefinida.DEUDAS.getTipo()
                && normalizar(nombre).equals(normalizar(CategoriaPredefinida.DEUDAS.getNombre()));
    }

    /** Un usuario puede usar las predefinidas y las suyas, nunca las de otro. */
    public boolean disponiblePara(Long usuario) {
        return predefinida || (usuarioId != null && usuarioId.equals(usuario));
    }

    /**
     * Compara nombres sin importar mayusculas, tildes ni espacios de mas:
     * "Mascotas", "mascotas" y " MASCOTAS " son la misma categoria, y
     * "Alimentacion" choca con la predefinida "Alimentación".
     */
    public boolean tieneMismoNombre(String otroNombre) {
        return otroNombre != null && normalizar(nombre).equals(normalizar(otroNombre));
    }

    public boolean esMismaQue(Categoria otra) {
        return otra != null && id != null && id.equals(otra.getId());
    }

    // ------------------------------------------------------------------

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("El nombre de la categoria es obligatorio");
        }
        // Quita espacios al inicio y al final, y deja uno solo entre palabras
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() > LONGITUD_MAXIMA_NOMBRE) {
            throw new ReglaNegocioException(
                    "El nombre de la categoria no puede superar " + LONGITUD_MAXIMA_NOMBRE + " caracteres");
        }
        return limpio;
    }

    static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public boolean isPredefinida() {
        return predefinida;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }
}
