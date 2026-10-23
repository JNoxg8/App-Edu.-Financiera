package edu.ucentral.zenfi.movimientos.dominio.modelo;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * HU009: un ingreso o un gasto del usuario. Java puro: sin JPA ni Quarkus.
 *
 * Cruce con deudas: cada abono a una deuda es tambien un gasto en la
 * categoria "Deudas". Esos movimientos guardan deudaId y abonoId, y
 * sus datos (monto, fecha, categoria) los manda la deuda: aqui solo se
 * puede cambiar la descripcion.
 */
public class Movimiento {

    public static final int LONGITUD_MAXIMA_DESCRIPCION = 255;
    private static final int ESCALA = 2;

    private Long id;
    private TipoMovimiento tipo;
    private BigDecimal monto;
    private LocalDate fecha;
    private String descripcion;
    private Categoria categoria;
    private Long usuarioId;

    /** Deuda a la que corresponde este gasto, si es un abono. */
    private Long deudaId;

    /** Abono de la deuda que origino este gasto. */
    private Long abonoId;

    /** Reconstruye un movimiento ya guardado (lo usa el adaptador de persistencia). */
    public Movimiento(Long id, TipoMovimiento tipo, BigDecimal monto, LocalDate fecha,
                      String descripcion, Categoria categoria, Long usuarioId,
                      Long deudaId, Long abonoId) {
        this.id = id;
        this.tipo = tipo;
        this.monto = monto;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.usuarioId = usuarioId;
        this.deudaId = deudaId;
        this.abonoId = abonoId;
    }

    // ------------------------------------------------------------------
    // Creacion
    // ------------------------------------------------------------------

    /**
     * Registra un ingreso o gasto hecho a mano, validando HU009:
     * tipo, monto > 0, fecha y categoria obligatorios; descripcion opcional.
     *
     * Los DTO ya validan casi todo con Bean Validation; aqui se repite
     * para que la regla se cumpla aunque el movimiento se cree desde
     * otro punto (igual que en Perfil).
     */
    public static Movimiento registrar(TipoMovimiento tipo, BigDecimal monto, LocalDate fecha,
                                       String descripcion, Categoria categoria, Long usuarioId) {
        validar(tipo, monto, fecha, categoria);
        if (categoria.esDeDeudas()) {
            throw new ReglaNegocioException(
                    "Un gasto en la categoria Deudas debe indicar la deuda a la que se abona");
        }
        return new Movimiento(null, tipo, escalar(monto), fecha, limpiarDescripcion(descripcion),
                categoria, usuarioId, null, null);
    }

    /**
     * Crea el gasto que refleja un abono a una deuda. Lo dispara el
     * evento AbonoRegistrado del modulo de deudas.
     */
    public static Movimiento desdeAbono(Long usuarioId, Long deudaId, Long abonoId,
                                        String acreedor, BigDecimal monto, LocalDate fecha,
                                        Categoria categoriaDeudas) {
        if (categoriaDeudas == null || !categoriaDeudas.esDeDeudas()) {
            throw new IllegalArgumentException("El abono debe registrarse en la categoria Deudas");
        }
        validar(TipoMovimiento.GASTO, monto, fecha, categoriaDeudas);
        return new Movimiento(null, TipoMovimiento.GASTO, escalar(monto), fecha,
                limpiarDescripcion("Abono a deuda: " + acreedor),
                categoriaDeudas, usuarioId, deudaId, abonoId);
    }

    // ------------------------------------------------------------------
    // Edicion
    // ------------------------------------------------------------------

    /**
     * Edita el movimiento.
     *
     * Si es un abono a una deuda solo se acepta cambiar la descripcion:
     * cambiar el monto aqui dejaria el saldo de la deuda desincronizado.
     */
    public void actualizar(TipoMovimiento nuevoTipo, BigDecimal nuevoMonto, LocalDate nuevaFecha,
                           String nuevaDescripcion, Categoria nuevaCategoria) {
        if (esAbonoDeDeuda()) {
            boolean cambiaDatosDeLaDeuda = nuevoTipo != tipo
                    || nuevoMonto == null || nuevoMonto.compareTo(monto) != 0
                    || !fecha.equals(nuevaFecha)
                    || !categoria.esMismaQue(nuevaCategoria);
            if (cambiaDatosDeLaDeuda) {
                throw new ReglaNegocioException(
                        "Este gasto es un abono a una deuda: solo se puede cambiar la descripcion. "
                                + "Para corregir el monto o la fecha, elimine el movimiento y "
                                + "registre el abono de nuevo");
            }
            cambiarDescripcion(nuevaDescripcion);
            return;
        }

        validar(nuevoTipo, nuevoMonto, nuevaFecha, nuevaCategoria);
        if (nuevaCategoria.esDeDeudas() && !nuevaCategoria.esMismaQue(categoria)) {
            throw new ReglaNegocioException(
                    "No se puede pasar un movimiento a la categoria Deudas; "
                            + "registre el abono desde la deuda correspondiente");
        }

        this.tipo = nuevoTipo;
        this.monto = escalar(nuevoMonto);
        this.fecha = nuevaFecha;
        this.descripcion = limpiarDescripcion(nuevaDescripcion);
        this.categoria = nuevaCategoria;
    }

    public void cambiarDescripcion(String nuevaDescripcion) {
        this.descripcion = limpiarDescripcion(nuevaDescripcion);
    }

    /**
     * Se llama cuando la deuda se elimina: el gasto se conserva (el
     * dinero si salio), pero ya no apunta a una deuda que no existe.
     */
    public void desvincularDeuda() {
        this.deudaId = null;
        this.abonoId = null;
    }

    public boolean esAbonoDeDeuda() {
        return abonoId != null;
    }

    public boolean esIngreso() {
        return tipo == TipoMovimiento.INGRESO;
    }

    // ------------------------------------------------------------------
    // Validaciones
    // ------------------------------------------------------------------

    private static void validar(TipoMovimiento tipo, BigDecimal monto,
                                LocalDate fecha, Categoria categoria) {
        if (tipo == null) {
            throw new ReglaNegocioException("Debe seleccionar el tipo de movimiento: ingreso o gasto");
        }
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaNegocioException("El monto debe ser mayor a cero");
        }
        if (fecha == null) {
            throw new ReglaNegocioException("La fecha es obligatoria");
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha del movimiento no puede ser futura");
        }
        if (categoria == null) {
            throw new ReglaNegocioException("Debe seleccionar una categoria");
        }
        if (categoria.getTipo() != tipo) {
            throw new ReglaNegocioException(
                    "La categoria '" + categoria.getNombre() + "' es de tipo "
                            + categoria.getTipo().getNombre().toLowerCase()
                            + " y no se puede usar en un " + tipo.getNombre().toLowerCase());
        }
    }

    /** La descripcion es opcional: vacia o solo espacios se guarda como null. */
    private static String limpiarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        String limpia = descripcion.trim();
        if (limpia.length() > LONGITUD_MAXIMA_DESCRIPCION) {
            throw new ReglaNegocioException(
                    "La descripcion no puede superar " + LONGITUD_MAXIMA_DESCRIPCION + " caracteres");
        }
        return limpia;
    }

    private static BigDecimal escalar(BigDecimal monto) {
        return monto.setScale(ESCALA, RoundingMode.HALF_UP);
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

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getDeudaId() {
        return deudaId;
    }

    public Long getAbonoId() {
        return abonoId;
    }
}
