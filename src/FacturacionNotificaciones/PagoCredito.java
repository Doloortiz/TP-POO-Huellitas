package FacturacionNotificaciones;

//Credito: 8 % de recargo.
 
public class PagoCredito implements EstrategiaPago {
    private static final long serialVersionUID = 1L;

    @Override
    public double calcularTotal(double importe) {
        EstrategiaPago.validarSubtotal(importe);
        double total = importe * 1.08; // 8% de recargo
        return EstrategiaPago.redondear(total);
    }

    @Override
    public MedioPago getMedioPago() {
        return MedioPago.CREDITO;
    }

    @Override
    public double getPorcentajeDescuento() {
        return 0.0;
    }

    @Override
    public double getPorcentajeRecargo() {
        return 0.08;
    }
}
