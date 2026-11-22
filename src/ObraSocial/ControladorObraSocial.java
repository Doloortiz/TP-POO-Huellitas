package ObraSocial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import Excepciones.ReglaNegocioException;
import FacturacionNotificaciones.MedioPago;
import PersonasMascota.Mascota;

// Controlador del módulo: gestiona afiliaciones, cobro de cuotas y reportes.

public class ControladorObraSocial implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, AfiliacionPlan> afiliados = new LinkedHashMap<>();
    private final List<ReciboCuota> recibosEmitidos = new ArrayList<>();
    private int contadorCredenciales = 1;
    private int contadorRecibos = 1;

    public ControladorObraSocial() {
    }

    // Afiliar una mascota a un plan seleccionado
    public AfiliacionPlan afiliarMascota(Mascota mascota, PlanObraSocial plan) throws ReglaNegocioException {
        if (mascota == null) {
            throw new ReglaNegocioException("Debe seleccionar una mascota para afiliar.");
        }
        if (plan == null) {
            throw new ReglaNegocioException("Debe seleccionar un plan de salud.");
        }
        // Validamos si la mascota ya posee una afiliación vigente en el padrón
        AfiliacionPlan existente = buscarPorMascota(mascota);
        if (existente != null && existente.getEstado() != EstadoCobertura.DADA_DE_BAJA) {
            throw new ReglaNegocioException("La mascota '" + mascota.getNombre()
                    + "' ya tiene una afiliación activa con credencial: " + existente.getNumeroCredencial());
        }

        String prefijo = plan.getPrefijoCredencial();
        String numeroCredencial = String.format("%s%04d", prefijo, contadorCredenciales++);

        AfiliacionPlan nuevaAfiliacion = new AfiliacionPlan(numeroCredencial, mascota, plan);
        mascota.setAfiliacion(nuevaAfiliacion);
        afiliados.put(numeroCredencial, nuevaAfiliacion);

        return nuevaAfiliacion;
    }

    // Cobrar la cuota mensual y emitir el recibo correspondiente
    public ReciboCuota cobrarCuota(String numeroCredencial, MedioPago medioPago) throws ReglaNegocioException {
        if (numeroCredencial == null || !afiliados.containsKey(numeroCredencial)) {
            throw new ReglaNegocioException("No se encontró la credencial: " + numeroCredencial);
        }
        if (medioPago == null) {
            throw new ReglaNegocioException("Debe seleccionar un medio de pago para la cuota.");
        }

        AfiliacionPlan afil = afiliados.get(numeroCredencial);
        if (afil.getEstado() == EstadoCobertura.DADA_DE_BAJA) {
            throw new ReglaNegocioException("No se pueden cobrar cuotas a una afiliación dada de baja.");
        }
        if (afil.getMesesAdeudados() <=0 ) {
            throw new ReglaNegocioException("La mascota '" + afil.getMascota().getNombre()
                    + "' ya se encuentra al día. No registra cuotas pendientes de cobro.");
        }

        double montoCuota = afil.getPlan().getCuotaMensual();
        afil.registrarPagoCuota();

        String nombreDueño = afil.getMascota().getDueño() != null
                ? afil.getMascota().getDueño().obtenerNombreCompleto()
                : "Sin Titular";

        ReciboCuota recibo = new ReciboCuota(
                contadorRecibos++,
                numeroCredencial,
                afil.getMascota().getNombre(),
                nombreDueño,
                afil.getPlan().getNombrePlan(),
                montoCuota,
                medioPago
        );

        recibosEmitidos.add(recibo);
        return recibo;
    }

    // Modificar plan de salud
    public void cambiarPlan(String numeroCredencial, PlanObraSocial nuevoPlan) throws ReglaNegocioException {
        AfiliacionPlan afil = buscarPorCredencial(numeroCredencial);
        if (afil == null) {
            throw new ReglaNegocioException("Credencial inexistente: " + numeroCredencial);
        }
        if (afil.getPlan().getClass().equals(nuevoPlan.getClass())) {
            throw new ReglaNegocioException("La mascota ya se encuentra en el " + nuevoPlan.getNombrePlan());
        }
        afil.cambiarPlan(nuevoPlan);
    }

    // Suspender o reactivar cobertura
    public void suspenderAfiliacion(String numeroCredencial) throws ReglaNegocioException {
        AfiliacionPlan afil = buscarPorCredencial(numeroCredencial);
        if (afil == null) throw new ReglaNegocioException("Credencial inexistente.");
        afil.suspender();
    }

    public void reactivarAfiliacion(String numeroCredencial) throws ReglaNegocioException {
        AfiliacionPlan afil = buscarPorCredencial(numeroCredencial);
        if (afil == null) throw new ReglaNegocioException("Credencial inexistente.");
        afil.reactivar();
    }

    public void darDeBaja(String numeroCredencial) throws ReglaNegocioException {
        AfiliacionPlan afil = buscarPorCredencial(numeroCredencial);
        if (afil == null) throw new ReglaNegocioException("Credencial inexistente.");
        afil.darDeBaja();
    }

    // Búsquedas y consultas
    public AfiliacionPlan buscarPorCredencial(String credencial) {
        if (credencial == null) return null;
        return afiliados.get(credencial.trim().toUpperCase());
    }

    public AfiliacionPlan buscarPorMascota(Mascota m) {
        if (m == null) return null;
        for (AfiliacionPlan afil : afiliados.values()) {
            if (afil.getMascota().equals(m)) {
                return afil;
            }
        }
        return null;
    }

    public List<AfiliacionPlan> listarAfiliados() {
        return new ArrayList<>(afiliados.values());
    }

    public List<ReciboCuota> getRecibosEmitidos() {
        return Collections.unmodifiableList(recibosEmitidos);
    }

    public int contarTotalAfiliados() {
        int total = 0;
        for (AfiliacionPlan a : afiliados.values()) {
            if (a.getEstado() != EstadoCobertura.DADA_DE_BAJA) total++;
        }
        return total;
    }

    public int contarAlDia() {
        int count = 0;
        for (AfiliacionPlan a : afiliados.values()) {
            if (a.getEstado() == EstadoCobertura.ACTIVA && a.getMesesAdeudados() == 0) {
                count++;
            }
        }
        return count;
    }

    public int contarMorosos() {
        int count = 0;
        for (AfiliacionPlan a : afiliados.values()) {
            if (a.getEstado() != EstadoCobertura.DADA_DE_BAJA && a.getMesesAdeudados() > 0) {
                count++;
            }
        }
        return count;
    }
}