package ObraSocial;
import java.io.Serializable;
import java.time.LocalDate;
import PersonasMascota.Mascota;

// Representa la afiliación o credencial de una mascota a un plan de obra social.

public class AfiliacionPlan implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String numeroCredencial;
    private final Mascota mascota;
    private PlanObraSocial plan;
    private final LocalDate fechaAlta;
    private EstadoCobertura estado;
    private int mesesAdeudados;

    public AfiliacionPlan(String numeroCredencial, Mascota mascota, PlanObraSocial plan) {
        if (numeroCredencial == null || numeroCredencial.isBlank()) {
            throw new IllegalArgumentException("El número de credencial es obligatorio.");
        }
        if (mascota == null) {
            throw new IllegalArgumentException("La mascota es obligatoria.");
        }
        if (plan == null) {
            throw new IllegalArgumentException("Debe asignarse un plan de salud.");
        }
        this.numeroCredencial = numeroCredencial.trim();
        this.mascota = mascota;
        this.plan = plan;
        this.fechaAlta = LocalDate.now();
        this.estado = EstadoCobertura.ACTIVA;
        this.mesesAdeudados = 0;
    }

    // Está habilitada para descuentos solo si está activa y no debe 2 o más meses.
    public boolean estaHabilitada() {
        return this.estado == EstadoCobertura.ACTIVA && this.mesesAdeudados < 2;
    }

    // Registra el pago de una cuota mensual. Si regulariza su deuda, se reactiva.
    public void registrarPagoCuota() {
        if (this.mesesAdeudados > 0) {
            this.mesesAdeudados--;
        }
        // Si regularizó y debía menos de 2 cuotas, recupera el estado activo
        if (this.mesesAdeudados < 2 && this.estado == EstadoCobertura.SUSPENDIDA) {
            this.estado = EstadoCobertura.ACTIVA;
        }
    }

    public void acumularMesImpago() {
        this.mesesAdeudados++;
        if (this.mesesAdeudados >= 2) {
            this.estado = EstadoCobertura.SUSPENDIDA;
        }
    }

    public void cambiarPlan(PlanObraSocial nuevoPlan) {
        if (nuevoPlan == null) {
            throw new IllegalArgumentException("El nuevo plan no puede ser nulo.");
        }
        this.plan = nuevoPlan;
    }

    public void suspender() {
        this.estado = EstadoCobertura.SUSPENDIDA;
    }

    public void reactivar() {
        this.estado = EstadoCobertura.ACTIVA;
    }

    public void darDeBaja() {
        this.estado = EstadoCobertura.DADA_DE_BAJA;
    }

    public String getNumeroCredencial() {
        return numeroCredencial;
    }
    public Mascota getMascota() {
        return mascota;
    }
    public PlanObraSocial getPlan() {
        return plan;
    }
    public LocalDate getFechaAlta() {
        return fechaAlta;
    }
    public EstadoCobertura getEstado() {
        return estado;
    }
    public int getMesesAdeudados() {
        return mesesAdeudados;
    }

    public String getEstadoCuotaTexto() {
        if (mesesAdeudados == 0) {
            return "AL DÍA";
        } else if (mesesAdeudados == 1) {
            return "DEBE 1 MES";
        } else {
            return "MOROSO (" + mesesAdeudados + " meses)";
        }
    }
}