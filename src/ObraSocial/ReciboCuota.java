package ObraSocial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import FacturacionNotificaciones.MedioPago;

// Comprobante emitido al registrar el cobro de una cuota mensual de obra social.

public class ReciboCuota implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int numeroRecibo;
    private final String numeroCredencial;
    private final String nombreMascota;
    private final String nombreDueño;
    private final String nombrePlan;
    private final double montoAbonado;
    private final MedioPago medioPago;
    private final LocalDateTime fechaPago;

    public ReciboCuota(int numeroRecibo, String numeroCredencial, String nombreMascota,
                       String nombreDueño, String nombrePlan, double montoAbonado, MedioPago medioPago) {
        this.numeroRecibo = numeroRecibo;
        this.numeroCredencial = numeroCredencial;
        this.nombreMascota = nombreMascota;
        this.nombreDueño = nombreDueño;
        this.nombrePlan = nombrePlan;
        this.montoAbonado = montoAbonado;
        this.medioPago = medioPago;
        this.fechaPago = LocalDateTime.now();
    }

    public int getNumeroRecibo() {
        return numeroRecibo;
    }

    public String getNumeroCredencial() {
        return numeroCredencial;
    }

    public String getNombreMascota() {
        return nombreMascota;
    }

    public String getNombreDueño() {
        return nombreDueño;
    }

    public String getNombrePlan() {
        return nombrePlan;
    }

    public double getMontoAbonado() {
        return montoAbonado;
    }

    public MedioPago getMedioPago() {
        return medioPago;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public String obtenerDetalle() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return String.format("RECIBO DE CUOTA N° %04d%n"
                        + "Fecha: %s%n"
                        + "Credencial: %s | Plan: %s%n"
                        + "Mascota: %s | Titular: %s%n"
                        + "Medio de Pago: %s%n"
                        + "TOTAL ABONADO: $%.2f",
                numeroRecibo, fechaPago.format(fmt), numeroCredencial, nombrePlan,
                nombreMascota, nombreDueño, medioPago.getDescripcion(), montoAbonado);
    }
}