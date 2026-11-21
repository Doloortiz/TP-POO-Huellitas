package ObraSocial;

// Consultas gratis y 25% de descuento en medicamentos y cirugías.

public class PlanBasico extends PlanObraSocial {
    private static final long serialVersionUID = 1L;

    public PlanBasico() {
        super("Plan Básico", 5000.0);
    }

    @Override
    public double calcularDescuentoMedicamentoCirugia(double precioBase) {
        if (precioBase <= 0) return 0.0;
        return precioBase * 0.25;
    }

    @Override
    public int getPorcentajeMedicamentosCirugia() {
        return 25;
    }

    @Override
    public String getPrefijoCredencial() {
        return "OS-BAS-";
    }
}