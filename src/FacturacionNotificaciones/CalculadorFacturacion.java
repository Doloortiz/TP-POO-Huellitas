package FacturacionNotificaciones;

import java.util.List;
import java.io.Serializable;
import Excepciones.FacturacionException;
import ObraSocial.PlanObraSocial;

// Servicio de cálculo contable que liquida subtotales, descuentos y recargos según la estrategia de pago.

public class CalculadorFacturacion implements Serializable {
    private static final long serialVersionUID = 1L;

    // Valor bruto de los items
    public double calcularSubtotal(List<LineaFactura> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            return 0.0;
        }
        double acumulador = 0.0;
        for (LineaFactura l : lineas) {
            acumulador += l.calcularSubtotal();
        }
        return EstrategiaPago.redondear(acumulador);
    }

    // Calcular cuánto dinero descuenta la Obra Social según el plan
    public double calcularDescuentoObraSocial(List<LineaFactura> lineas, PlanObraSocial plan) {
        if (lineas == null || plan == null) {
            return 0.0;
        }
        double descuentoTotal = 0.0;
        for (LineaFactura l : lineas) {
            double precioLinea = l.calcularSubtotal();
            if (l.esConsulta()) {
                descuentoTotal += plan.calcularDescuentoConsulta(precioLinea); // Consulta 100%
            } else {
                descuentoTotal += plan.calcularDescuentoMedicamentoCirugia(precioLinea); // Medicamentos y cirugias
            }
        }
        return EstrategiaPago.redondear(descuentoTotal);
    }

    // Calcular total final (aplicando medio de pago)
    public double calcularImporteFinal(double copago, EstrategiaPago estrategiaPago) throws FacturacionException {
        if (copago <= 0.0) {
            return 0.0; // Cobertura total
        }
        if (estrategiaPago == null) {
            throw new FacturacionException("Debe seleccionarse un medio de pago.");
        }
        return estrategiaPago.calcularTotal(copago);
    }
}
