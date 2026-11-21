package ObraSocial;

// Cobertura total del 100% en todas las prestaciones.

public class PlanBlack extends PlanObraSocial {
    private static final long serialVersionUID = 1L;

    public PlanBlack() {
        super("Plan Black Total", 20000.0);
    }

    @Override
    public double calcularDescuentoMedicamentoCirugia(double precioBase) {
        if (precioBase <= 0) return 0.0;
        return precioBase;
    }

    @Override
    public int getPorcentajeMedicamentosCirugia() {
        return 100;
    }

    @Override
    public String getPrefijoCredencial() {
        return "OS-BLK-";
    }
}