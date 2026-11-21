package FacturacionNotificaciones;
import java.io.Serializable;

// Interfaz para calcular importes finales con recargos o descuentos.

public interface EstrategiaPago extends Serializable {
    // Aplicar recargo o descuento del medio de pago
    double calcularTotal(double importe);

    MedioPago getMedioPago();

    double getPorcentajeDescuento();

    double getPorcentajeRecargo();

    static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    static void validarSubtotal(double subtotal) {
        if (subtotal < 0) {
            throw new IllegalArgumentException("El importe no puede ser negativo.");
        }
    }
}
