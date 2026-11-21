package ObraSocial;
import java.io.Serializable;

// Estado de la credencial de una mascota en la obra social.

public enum EstadoCobertura implements Serializable {
    ACTIVA("Activa"),
    SUSPENDIDA("Suspendida por Mora"),
    DADA_DE_BAJA("Dada de Baja");

    private final String descripcion;

    EstadoCobertura(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}