package ObraSocial;

// Consultas gratis y 50% de descuento en medicamentos y cirugías.

public class PlanPremium extends PlanObraSocial {
    private static final long serialVersionUID = 1L;

    public PlanPremium() {
        super("Plan Premium", 10000.0);
    }

    @Override
    public double calcularDescuentoMedicamentoCirugia(double precioBase) {
        if (precioBase <= 0) return 0.0;
        return precioBase * 0.50;
    }

    @Override
    public int getPorcentajeMedicamentosCirugia() {
        return 50;
    }

    @Override
    public String getPrefijoCredencial() {
        return "OS-PRE-";
    }
}