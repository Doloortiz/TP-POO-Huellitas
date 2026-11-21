import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import AtencionHistoriaClinica.Consulta;
import AtencionHistoriaClinica.HistoriaClinica;
import Excepciones.FacturacionException;
import Excepciones.ReglaNegocioException;
import FacturacionNotificaciones.*;
import GestionTurnos.DiaSemana;
import GestionTurnos.RangoHorario;
import InventarioProveedores.Lote;
import InventarioProveedores.Producto;
import InventarioProveedores.Proveedor;
import PersonasMascota.Dueño;
import PersonasMascota.Perro;
import PersonasMascota.Veterinario;
import ObraSocial.*;

// Pruebas unitarias para validar las reglas de negocio críticas del sistema.

public class PruebasUnitarias {

    private Dueño duenoPrueba;
    private Perro perroPrueba;
    private Veterinario vetPrueba;
    private CalculadorFacturacion calculador;
    private ControladorFacturacion controladorFacturacion;

    @BeforeEach
    public void setUp() {
        // Inicialización de objetos base para cada prueba
        duenoPrueba = new Dueño("Juan", "Pérez", "30111222", "1144445555", "juan@test.com");
        HistoriaClinica hc = new HistoriaClinica(1);
        perroPrueba = new Perro(1, duenoPrueba, hc, "Milo", "Perro", "Labrador", "Macho", LocalDate.now().minusYears(2), 25.0, "CHIP-01", "Grande");
        duenoPrueba.agregarMascota(perroPrueba);

        Map<DiaSemana, RangoHorario> horarios = new EnumMap<>(DiaSemana.class);
        horarios.put(DiaSemana.LUNES, new RangoHorario(LocalTime.of(9, 0), LocalTime.of(18, 0)));
        vetPrueba = new Veterinario("Carlos", "Gómez", "25888999", "1166667777", "VET-01", "MN-1234", "General", horarios, false);

        calculador = new CalculadorFacturacion();
        controladorFacturacion = new ControladorFacturacion(calculador, new NotificadorEmail("test@clinica.com"));
    }

    // TEST 1: Facturación con Descuento en Efectivo (10% - RN5.2)
    @Test
    public void testFacturacionDescuentoEfectivo() throws Exception {
        List<LineaFactura> lineas = new ArrayList<>();
        lineas.add(new LineaFactura("Consulta general", 1, 10000.0));

        double subtotal = calculador.calcularSubtotal(lineas);
        EstrategiaPago estrategiaEfectivo = new PagoEfectivo();

        // Calcula el total pasando el subtotal/copago al método limpio:
        double total = calculador.calcularImporteFinal(subtotal, estrategiaEfectivo);

        // Subtotal $10.000 con 10% de descuento debe ser exactamente $9.000
        assertEquals(9000.0, total, 0.01, "El pago en efectivo debe otorgar un 10% de descuento");
    }

    // TEST 2: Facturación con Recargo en Tarjeta de Crédito (8% - RN5.4)
    @Test
    public void testFacturacionRecargoCredito() throws Exception {
        List<LineaFactura> lineas = new ArrayList<>();
        lineas.add(new LineaFactura("Consulta general", 1, 10000.0));

        double subtotal = calculador.calcularSubtotal(lineas);
        EstrategiaPago estrategiaCredito = new PagoCredito();

        // Calcula el total pasando el subtotal/copago al método limpio:
        double total = calculador.calcularImporteFinal(subtotal, estrategiaCredito);

        // Subtotal $10.000 con 8% de recargo debe ser exactamente $10.800
        assertEquals(10800.0, total, 0.01, "El pago con crédito debe aplicar un 8% de recargo");
    }

    // TEST 3: Descuento de stock bajo criterio FEFO (RN4.4 y RN4.5)
    @Test
    public void testConsumoStockCriterioFEFO() throws ReglaNegocioException {
        Proveedor prov = new Proveedor("30-11111111-1", "Distribuidora Vet", "11223344", "Av. San Martín 123");
        Producto amoxicilina = new Producto(1, "Amoxicilina", "Antibiótico", 3000.0, prov, 5);

        // Lote 1: vence dentro de 1 mes (5 unidades) -> Debe consumirse PRIMERO
        Lote loteProximo = new Lote("LOTE-PROXIMO", LocalDate.now().plusMonths(1), 5);
        // Lote 2: vence dentro de 8 meses (5 unidades) -> Debe consumirse DESPUÉS
        Lote loteLejano = new Lote("LOTE-LEJANO", LocalDate.now().plusMonths(8), 5);

        amoxicilina.agregarLote(loteProximo);
        amoxicilina.agregarLote(loteLejano);

        assertEquals(10, amoxicilina.obtenerStockUtilizable(LocalDate.now()));

        // Consumimos 3 unidades
        amoxicilina.consumirStock(3, LocalDate.now());

        // El lote más próximo debe haber bajado de 5 a 2
        assertEquals(2, loteProximo.getCantidadDisponible(), "FEFO debe descontar primero del lote que vence antes");
        // El lote lejano debe seguir intacto con 5
        assertEquals(5, loteLejano.getCantidadDisponible(), "El lote con vencimiento más lejano no debe tocarse todavía");
        // El stock total utilizable debe ser 7
        assertEquals(7, amoxicilina.obtenerStockUtilizable(LocalDate.now()));
    }

    // TEST 4: Control de ausencias consecutivas y exigencia de seña (RN1.6)
    @Test
    public void testControlAusenciasYExigenciaSena() {
        assertFalse(duenoPrueba.debePagarSeña(), "Al inicio el dueño no debe requerir seña");

        duenoPrueba.registrarAusencia();
        duenoPrueba.registrarAusencia();
        assertFalse(duenoPrueba.debePagarSeña(), "Con 2 ausencias todavía no se exige seña");

        duenoPrueba.registrarAusencia();
        assertTrue(duenoPrueba.debePagarSeña(), "Con 3 ausencias consecutivas debe exigirse seña obligatoria");

        duenoPrueba.resetearAusencias();
        assertFalse(duenoPrueba.debePagarSeña(), "Al resetear ausencias ya no debe exigir seña");
    }

    // TEST 5: Restricción de medicación sin peso registrado (RN2.7)
    @Test
    public void testRestriccionMedicarSinPeso() {
        Proveedor prov = new Proveedor("30-11111111-1", "Distribuidora Vet", "11223344", "Av. San Martín 123");
        Producto remedio = new Producto(1, "Antiinflamatorio", "Comprimidos", 2500.0, prov, 3);

        Consulta consultaSinPeso = new Consulta(1, vetPrueba, "Control", 0.0, "Sano", "Sin observaciones");

        assertThrows(ReglaNegocioException.class, () -> {
            consultaSinPeso.agregarTratamiento(remedio, "1 comp", "c/24h", "3 días");
        }, "No se debe permitir indicar medicamentos si no se registró el peso de la mascota");
    }

    // TEST 6: Inmutabilidad y Anulación de Facturas (RN5.7 y RN5.8)
    @Test
    public void testAnulacionFacturaConNotaCredito() throws FacturacionException {
        Consulta consulta = new Consulta(1, vetPrueba, "Vacunación", 15.0, "Sano", "Aplicada");
        List<LineaFactura> lineas = new ArrayList<>();
        lineas.add(new LineaFactura("Consulta", 1, 5000.0));

        Factura factura = controladorFacturacion.emitirFactura(duenoPrueba, consulta, MedioPago.EFECTIVO, lineas);
        assertFalse(factura.estaAnulada(), "La factura recién emitida no debe estar anulada");

        NotaDeCredito nota = controladorFacturacion.anularFactura(factura.getNumeroFactura(), "Error en comprobante");

        assertTrue(factura.estaAnulada(), "La factura debe figurar como anulada");
        assertNotNull(nota, "Debe haberse generado una nota de crédito");
        assertEquals(factura.getMontoTotal(), nota.getImporteTotal(), 0.01, "La nota de crédito debe cubrir el monto total");

        assertThrows(FacturacionException.class, () -> {
            controladorFacturacion.anularFactura(factura.getNumeroFactura(), "Otro motivo");
        }, "No se debe poder anular una factura ya anulada");
    }

    // =========================================================
    // PRUEBAS DE LA MEJORA: OBRA SOCIAL
    // =========================================================

    // TEST 7: Cobertura total de honorarios en Consulta Médica (RN6.1)
    @Test
    public void testCoberturaTotalEnConsulta() {
        PlanObraSocial plan = new PlanBasico();
        List<LineaFactura> lineas = new ArrayList<>();
        lineas.add(new LineaFactura("Consulta general", 1, 10000.0)); // esConsulta() == true

        double descuentoOS = calculador.calcularDescuentoObraSocial(lineas, plan);
        double subtotal = calculador.calcularSubtotal(lineas);
        double copago = subtotal - descuentoOS;

        assertEquals(10000.0, descuentoOS, 0.01, "La consulta debe estar 100% cubierta");
        assertEquals(0.0, copago, 0.01, "El copago de la consulta debe ser $0");
    }

    // TEST 8: Descuentos polimórficos escalonados en Medicamentos (RN6.2)
    @Test
    public void testDescuentosPolimorficosMedicamentos() {
        List<LineaFactura> lineas = new ArrayList<>();
        Producto antibiotico = new Producto(1, "Antibiótico", "Inyectable", 4000.0, null, 10);

        // Al pasar el objeto antibiotico, esConsulta() devuelve false y aplica descuento de medicamento
        lineas.add(new LineaFactura(antibiotico, 1));

        PlanObraSocial basico = new PlanBasico();
        PlanObraSocial premium = new PlanPremium();
        PlanObraSocial black = new PlanBlack();

        // Plan Básico: 25% de $4.000 = $1.000
        assertEquals(1000.0, calculador.calcularDescuentoObraSocial(lineas, basico), 0.01, "Plan Básico debe descontar 25% en medicamentos");

        // Plan Premium: 50% de $4.000 = $2.000
        assertEquals(2000.0, calculador.calcularDescuentoObraSocial(lineas, premium), 0.01, "Plan Premium debe descontar 50% en medicamentos");

        // Plan Black: 100% de $4.000 = $4.000
        assertEquals(4000.0, calculador.calcularDescuentoObraSocial(lineas, black), 0.01, "Plan Black debe cubrir 100% en medicamentos");
    }

    // TEST 9: Suspensión automática de cobertura por acumulación de 2 meses de mora (RN6.3)
    @Test
    public void testSuspensionAutomaticaPorMorosidad() {
        AfiliacionPlan afil = new AfiliacionPlan("OS-BAS-0001", perroPrueba, new PlanBasico());

        // Al inicio: al día y habilitada
        assertTrue(afil.estaHabilitada(), "Al inicio debe estar habilitada");
        assertEquals(EstadoCobertura.ACTIVA, afil.getEstado());

        // Acumula 1 mes impago: período de gracia
        afil.acumularMesImpago();
        assertEquals(1, afil.getMesesAdeudados());
        assertTrue(afil.estaHabilitada(), "Con 1 mes impago sigue habilitada por período de gracia");

        // Acumula 2 meses impagos: suspensión automática
        afil.acumularMesImpago();
        assertEquals(2, afil.getMesesAdeudados());
        assertEquals(EstadoCobertura.SUSPENDIDA, afil.getEstado(), "Al acumular 2 meses debe suspenderse automáticamente");
        assertFalse(afil.estaHabilitada(), "Suspendida no debe estar habilitada para descuentos");
    }

    // TEST 10: Cobro de cuota, emisión de Recibo y reactivación automática (RN6.4)
    @Test
    public void testCobroCuotaReactivacionYRecibo() throws ReglaNegocioException {
        ControladorObraSocial ctrlOS = new ControladorObraSocial();
        AfiliacionPlan afil = ctrlOS.afiliarMascota(perroPrueba, new PlanBasico());

        // Llevamos la afiliación a suspensión (2 meses de mora)
        afil.acumularMesImpago();
        afil.acumularMesImpago();
        assertEquals(EstadoCobertura.SUSPENDIDA, afil.getEstado());

        // Se cobra 1 cuota mensual
        ReciboCuota recibo = ctrlOS.cobrarCuota(afil.getNumeroCredencial(), MedioPago.EFECTIVO);

        assertNotNull(recibo, "Debe emitirse un recibo de cobro de cuota");
        assertEquals(5000.0, recibo.getMontoAbonado(), 0.01, "El monto abonado debe ser el de la cuota del Plan Básico");
        assertEquals(1, afil.getMesesAdeudados(), "La deuda debe haber disminuido a 1 mes");
        assertEquals(EstadoCobertura.ACTIVA, afil.getEstado(), "Al regularizar por debajo de 2 cuotas debe reactivarse automáticamente");
        assertTrue(afil.estaHabilitada(), "Debe recuperar la habilitación de cobertura");
    }

    // TEST 11: Restricción de doble afiliación activa simultánea (RN6.5)
    @Test
    public void testRestriccionDobleAfiliacionActiva() throws ReglaNegocioException {
        ControladorObraSocial ctrlOS = new ControladorObraSocial();
        ctrlOS.afiliarMascota(perroPrueba, new PlanBasico());

        // Intentar afiliar de nuevo la misma mascota debe lanzar excepción
        assertThrows(ReglaNegocioException.class, () -> {
            ctrlOS.afiliarMascota(perroPrueba, new PlanPremium());
        }, "No se debe permitir afiliar una mascota que ya posee afiliación activa");
    }

    // Permite ejecutar las pruebas directamente con el botón Play
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   EJECUTANDO PRUEBAS UNITARIAS DE NEGOCIO");
        System.out.println("=================================================");
        PruebasUnitarias tests = new PruebasUnitarias();
        int exitosas = 0;
        int fallidas = 0;

        // TEST 1
        try {
            tests.setUp();
            tests.testFacturacionDescuentoEfectivo();
            System.out.println("  [OK] TEST 1: Facturación con Descuento Efectivo (10%)");
            exitosas++;
        } catch (Throwable t) {
            System.err.println("  [FALLO] TEST 1: " + t.getMessage());
            fallidas++;
        }

        // TEST 2
        try {
            tests.setUp();
            tests.testFacturacionRecargoCredito();
            System.out.println("  [OK] TEST 2: Facturación con Recargo Crédito (8%)");
            exitosas++;
        } catch (Throwable t) {
            System.err.println("  [FALLO] TEST 2: " + t.getMessage());
            fallidas++;
        }

        // TEST 3
        try {
            tests.setUp();
            tests.testConsumoStockCriterioFEFO();
            System.out.println("  [OK] TEST 3: Descuento de stock bajo criterio FEFO");
            exitosas++;
        } catch (Throwable t) {
            System.err.println("  [FALLO] TEST 3: " + t.getMessage());
            fallidas++;
        }

        // TEST 4
        try {
            tests.setUp();
            tests.testControlAusenciasYExigenciaSena();
            System.out.println("  [OK] TEST 4: Control de ausencias y exigencia de seña");
            exitosas++;
        } catch (Throwable t) {
            System.err.println("  [FALLO] TEST 4: " + t.getMessage());
            fallidas++;
        }

        // TEST 5
        try {
            tests.setUp();
            tests.testRestriccionMedicarSinPeso();
            System.out.println("  [OK] TEST 5: Restricción de recetar medicación sin peso");
            exitosas++;
        } catch (Throwable t) {
            System.err.println("  [FALLO] TEST 5: " + t.getMessage());
            fallidas++;
        }

        // TEST 6
        try {
            tests.setUp();
            tests.testAnulacionFacturaConNotaCredito();
            System.out.println("  [OK] TEST 6: Inmutabilidad y anulación con Nota de Crédito");
            exitosas++;
        } catch (Throwable t) {
            System.err.println("  [FALLO] TEST 6: " + t.getMessage());
            fallidas++;
        }

        // TEST 7
        try {
            tests.setUp();
            tests.testCoberturaTotalEnConsulta();
            System.out.println("  [OK] TEST 7: Cobertura 100% en Consulta Médica (RN6.1)");
            exitosas++;
        } catch (Throwable t) {
            System.err.println("  [FALLO] TEST 7: " + t.getMessage());
            fallidas++;
        }

        // TEST 8
        try {
            tests.setUp();
            tests.testDescuentosPolimorficosMedicamentos();
            System.out.println("  [OK] TEST 8: Descuentos Polimórficos en Medicamentos (RN6.2)");
            exitosas++;
        } catch (Throwable t) {
            System.err.println("  [FALLO] TEST 8: " + t.getMessage());
            fallidas++;
        }

        // TEST 9
        try {
            tests.setUp();
            tests.testSuspensionAutomaticaPorMorosidad();
            System.out.println("  [OK] TEST 9: Suspensión automática por 2 meses de mora (RN6.3)");
            exitosas++;
        } catch (Throwable t) {
            System.err.println("  [FALLO] TEST 9: " + t.getMessage());
            fallidas++;
        }

        // TEST 10
        try {
            tests.setUp();
            tests.testCobroCuotaReactivacionYRecibo();
            System.out.println("  [OK] TEST 10: Cobro de cuota, Recibo y reactivación (RN6.4)");
            exitosas++;
        } catch (Throwable t) {
            System.err.println("  [FALLO] TEST 10: " + t.getMessage());
            fallidas++;
        }

        // TEST 11
        try {
            tests.setUp();
            tests.testRestriccionDobleAfiliacionActiva();
            System.out.println("  [OK] TEST 11: Restricción de doble afiliación activa (RN6.5)");
            exitosas++;
        } catch (Throwable t) {
            System.err.println("  [FALLO] TEST 11: " + t.getMessage());
            fallidas++;
        }

        System.out.println("=================================================");
        System.out.println("RESULTADO FINAL: " + exitosas + " pruebas superadas con éxito, " + fallidas + " fallidas.");
        System.out.println("=================================================");
    }
}