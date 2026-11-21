package FacturacionNotificaciones;

// Efectivo: 10 % de descuento.

public class PagoEfectivo implements EstrategiaPago {
    private static final long serialVersionUID = 1L;

    @Override
    public double calcularTotal(double importe) {
        EstrategiaPago.validarSubtotal(importe);
        double total = importe * 0.90; // 10% de descuento
        return EstrategiaPago.redondear(total);
    }

    @Override
    public MedioPago getMedioPago() {
        return MedioPago.EFECTIVO;
    }

    @Override
    public double getPorcentajeDescuento() {
        return 0.10;
    }

    @Override
    public double getPorcentajeRecargo() {
        return 0.0;
    }
}
