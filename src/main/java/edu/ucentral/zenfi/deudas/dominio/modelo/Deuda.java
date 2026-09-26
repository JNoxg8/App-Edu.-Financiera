package edu.ucentral.zenfi.deudas.dominio.modelo;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * RQ-07: deuda activa del usuario.
 *
 * Esta clase NO conoce JPA, ni HTTP, ni Quarkus. Es Java puro.
 * Por eso se puede probar con un test unitario normal, sin levantar
 * la aplicacion ni la base de datos.
 *
 * Toda la logica financiera vive aqui, no en el servicio.
 */
public class Deuda {

    /** Dos decimales: los centavos. */
    private static final int ESCALA = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;
    private static final MathContext PRECISION = new MathContext(20, REDONDEO);
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private Long id;
    private String acreedor;

    /** Capital prestado, sin intereses. */
    private BigDecimal montoTotal;

    /** Tasa efectiva anual expresada en porcentaje. Ej: 24.50 significa 24.5% E.A. */
    private BigDecimal tasaInteresAnual;

    private int numeroCuotas;
    private LocalDate fechaVencimiento;
    private Long usuarioId;

    private List<Abono> abonos = new ArrayList<>();

    public Deuda() {
    }

    public Deuda(Long id, String acreedor, BigDecimal montoTotal,
                 BigDecimal tasaInteresAnual, int numeroCuotas,
                 LocalDate fechaVencimiento, Long usuarioId, List<Abono> abonos) {
        this.id = id;
        this.acreedor = acreedor;
        this.montoTotal = montoTotal;
        this.tasaInteresAnual = tasaInteresAnual;
        this.numeroCuotas = numeroCuotas;
        this.fechaVencimiento = fechaVencimiento;
        this.usuarioId = usuarioId;
        this.abonos = (abonos == null) ? new ArrayList<>() : new ArrayList<>(abonos);
    }

    // ------------------------------------------------------------------
    // Calculos financieros (sistema de amortizacion frances: cuota fija)
    // ------------------------------------------------------------------

    /**
     * Convierte la tasa efectiva anual a efectiva mensual.
     *
     *     i_mensual = (1 + i_anual)^(1/12) - 1
     *
     * No se divide entre 12: esa seria la tasa nominal, no la efectiva.
     */
    private double tasaMensual() {
        if (tasaInteresAnual == null || tasaInteresAnual.signum() <= 0) {
            return 0d;
        }
        double anual = tasaInteresAnual.doubleValue() / 100d;
        return Math.pow(1d + anual, 1d / 12d) - 1d;
    }

    /**
     * Cuota fija mensual:
     *
     *     C = P * i / (1 - (1 + i)^-n)
     *
     * Si la tasa es cero, simplemente se reparte el capital entre las cuotas.
     */
    public BigDecimal cuotaMensual() {
        if (montoTotal == null || numeroCuotas <= 0) {
            return cero();
        }

        double i = tasaMensual();

        if (i == 0d) {
            return montoTotal.divide(BigDecimal.valueOf(numeroCuotas), ESCALA, REDONDEO);
        }

        double factor = i / (1d - Math.pow(1d + i, -numeroCuotas));
        return montoTotal.multiply(BigDecimal.valueOf(factor), PRECISION)
                .setScale(ESCALA, REDONDEO);
    }

    /**
     * Capital + intereses: lo que realmente se termina pagando.
     *
     * Sin intereses el total es exactamente el capital. No se calcula
     * como cuota * n porque el redondeo de la cuota puede perder
     * centavos: 100.000 en 3 cuotas da 33.333,33 * 3 = 99.999,99,
     * lo que dejaria un "costo de intereses" negativo de -0,01.
     */
    public BigDecimal totalAPagar() {
        if (montoTotal == null || numeroCuotas <= 0) {
            return cero();
        }
        if (tasaMensual() == 0d) {
            return montoTotal.setScale(ESCALA, REDONDEO);
        }
        return cuotaMensual()
                .multiply(BigDecimal.valueOf(numeroCuotas))
                .setScale(ESCALA, REDONDEO);
    }

    /** Cuanto se paga de mas solo por concepto de intereses. */
    public BigDecimal costoIntereses() {
        if (montoTotal == null) {
            return cero();
        }
        return totalAPagar().subtract(montoTotal).setScale(ESCALA, REDONDEO);
    }

    /** Suma de todos los abonos registrados. */
    public BigDecimal totalAbonado() {
        return abonos.stream()
                .map(Abono::getMonto)
                .filter(m -> m != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(ESCALA, REDONDEO);
    }

    /**
     * Saldo pendiente = total a pagar - abonos.
     * Nunca se guarda en la base de datos: se calcula siempre.
     * Asi no puede quedar desincronizado.
     */
    public BigDecimal saldoPendiente() {
        BigDecimal saldo = totalAPagar().subtract(totalAbonado());
        return saldo.signum() < 0 ? cero() : saldo.setScale(ESCALA, REDONDEO);
    }

    /** Porcentaje de la deuda ya cubierto, entre 0 y 100. */
    public BigDecimal porcentajePagado() {
        BigDecimal total = totalAPagar();
        if (total.signum() == 0) {
            return cero();
        }
        BigDecimal porcentaje = totalAbonado()
                .multiply(CIEN)
                .divide(total, ESCALA, REDONDEO);

        return porcentaje.compareTo(CIEN) > 0 ? CIEN.setScale(ESCALA) : porcentaje;
    }

    public boolean estaPagada() {
        return saldoPendiente().signum() == 0;
    }

    // ------------------------------------------------------------------
    // Reglas de negocio
    // ------------------------------------------------------------------

    /**
     * Agrega un abono validando las reglas del negocio.
     * Si algo no cuadra lanza ReglaNegocioException, que el mapper
     * convierte en un HTTP 409 con mensaje claro.
     */
    public void agregarAbono(Abono abono) {
        if (abono == null || abono.getMonto() == null) {
            throw new ReglaNegocioException("El abono es obligatorio");
        }
        if (abono.getMonto().signum() <= 0) {
            throw new ReglaNegocioException("El monto del abono debe ser mayor a cero");
        }
        if (estaPagada()) {
            throw new ReglaNegocioException("La deuda ya esta pagada en su totalidad");
        }
        if (abono.getMonto().compareTo(saldoPendiente()) > 0) {
            throw new ReglaNegocioException(
                    "El abono excede el saldo pendiente, que es " + saldoPendiente());
        }
        abonos.add(abono);
    }

    /**
     * RQ-22: actualiza los datos editables de la deuda.
     *
     * Antes de aplicar los cambios simula como quedaria la deuda. Si el
     * nuevo total a pagar quedara por debajo de lo ya abonado, el estado
     * seria incoherente (alguien habria pagado mas de lo que debe), asi
     * que se rechaza.
     */
    public void actualizarDatos(String nuevoAcreedor,
                                BigDecimal nuevaTasa,
                                int nuevasCuotas,
                                LocalDate nuevaFecha) {

        Deuda simulada = new Deuda(this.id, nuevoAcreedor, this.montoTotal,
                nuevaTasa, nuevasCuotas, nuevaFecha, this.usuarioId, this.abonos);

        if (simulada.totalAPagar().compareTo(totalAbonado()) < 0) {
            throw new ReglaNegocioException(
                    "Con esos valores el total a pagar seria "
                            + simulada.totalAPagar()
                            + ", menor a lo ya abonado (" + totalAbonado() + ")");
        }

        this.acreedor = nuevoAcreedor;
        this.tasaInteresAnual = nuevaTasa;
        this.numeroCuotas = nuevasCuotas;
        this.fechaVencimiento = nuevaFecha;
    }

    private BigDecimal cero() {
        return BigDecimal.ZERO.setScale(ESCALA, REDONDEO);
    }

    // ------------------------------------------------------------------
    // Getters y setters
    // ------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAcreedor() {
        return acreedor;
    }

    public void setAcreedor(String acreedor) {
        this.acreedor = acreedor;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public BigDecimal getTasaInteresAnual() {
        return tasaInteresAnual;
    }

    public void setTasaInteresAnual(BigDecimal tasaInteresAnual) {
        this.tasaInteresAnual = tasaInteresAnual;
    }

    public int getNumeroCuotas() {
        return numeroCuotas;
    }

    public void setNumeroCuotas(int numeroCuotas) {
        this.numeroCuotas = numeroCuotas;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public List<Abono> getAbonos() {
        return abonos;
    }

    public void setAbonos(List<Abono> abonos) {
        this.abonos = (abonos == null) ? new ArrayList<>() : new ArrayList<>(abonos);
    }
}
