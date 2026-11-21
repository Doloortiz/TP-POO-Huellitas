package FacturacionNotificaciones;

// Debito: sin recargo.

public class PagoDebito implements EstrategiaPago {
    private static final long serialVersionUID = 1L;

    @Override
    public double calcularTotal(double importe) {
        EstrategiaPago.validarSubtotal(importe);
        return EstrategiaPago.redondear(importe);
    }

    @Override
    public MedioPago getMedioPago() {
        return MedioPago.DEBITO;
    }

    @Override
    public double getPorcentajeDescuento(boolean tienePlanSalud) {
        return 0.0;
    }

    @Override
    public double getPorcentajeRecargo() {
        return 0.0;
    }
}
