package ObraSocial;
import java.io.Serializable;

// Clase abstracta base para los planes de cobertura médica.

public abstract class PlanObraSocial implements Serializable {
    private static final long serialVersionUID = 1L;

    protected final String nombrePlan;
    protected final double cuotaMensual;

    public PlanObraSocial(String nombrePlan, double cuotaMensual) {
        if (nombrePlan == null || nombrePlan.isBlank()) {
            throw new IllegalArgumentException("El nombre del plan no puede ser vacío.");
        }
        if (cuotaMensual < 0) {
            throw new IllegalArgumentException("La cuota mensual no puede ser negativa.");
        }
        this.nombrePlan = nombrePlan.trim();
        this.cuotaMensual = cuotaMensual;
    }

    public String getNombrePlan() {
        return nombrePlan;
    }

    public double getCuotaMensual() {
        return cuotaMensual;
    }

    // Las consultas médicas están 100% cubiertas en todos los planes.
    public double calcularDescuentoConsulta(double precioBase) {
        return precioBase; // Descuenta el 100%
    }

    public abstract double calcularDescuentoMedicamentoCirugia(double precioBase);

    public abstract int getPorcentajeMedicamentosCirugia();

    public abstract String getPrefijoCredencial();

    @Override
    public String toString() {
        return nombrePlan + " ($" + String.format("%.0f", cuotaMensual) + "/mes)";
    }
}