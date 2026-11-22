package VistaMain;

import GeneralPersistencia.ClinicaVeterinaria;
import PersonasMascota.*;
import ObraSocial.*;
import FacturacionNotificaciones.MedioPago;
import Excepciones.ReglaNegocioException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Panel para gestionar afiliados a la Obra Social, cobro de cuotas y estados de cobertura.

public class PanelObraSocial extends JPanel {

    private final ClinicaVeterinaria clinica;
    private final JTable tabla;
    private final DefaultTableModel modelo;
    private final JLabel lblTotal;
    private final JLabel lblAlDia;
    private final JLabel lblMorosos;

    private final Color VERDE = new Color(105, 135, 116);
    private final Color FONDO = new Color(248, 247, 242);

    public PanelObraSocial(ClinicaVeterinaria clinica) {
        this.clinica = clinica;

        setLayout(new BorderLayout(15, 15));
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // Encabezado y tarjetas
        JPanel panelNorte = new JPanel(new BorderLayout(10, 10));
        panelNorte.setOpaque(false);

        JLabel titulo = new JLabel("Obra Social & Planes de Salud");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 25));
        titulo.setForeground(VERDE);

        JLabel subtitulo = new JLabel("Padrón de afiliados, cobro de cuotas y control de morosidad");
        subtitulo.setForeground(Color.GRAY);

        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 3));
        titulos.setOpaque(false);
        titulos.add(titulo);
        titulos.add(subtitulo);

        // Tarjetas con indicadores
        JPanel resumenPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        resumenPanel.setOpaque(false);

        lblTotal = new JLabel("Total Afiliados: 0");
        lblTotal.setFont(new Font("SansSerif", Font.BOLD, 13));

        lblAlDia = new JLabel("Al Día: 0");
        lblAlDia.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblAlDia.setForeground(new Color(46, 125, 50));

        lblMorosos = new JLabel("En Mora: 0");
        lblMorosos.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblMorosos.setForeground(new Color(198, 40, 40));

        resumenPanel.add(lblTotal);
        resumenPanel.add(lblAlDia);
        resumenPanel.add(lblMorosos);

        panelNorte.add(titulos, BorderLayout.WEST);
        panelNorte.add(resumenPanel, BorderLayout.EAST);
        add(panelNorte, BorderLayout.NORTH);

        // Tabla de afiliaciones
        modelo = new DefaultTableModel(
                new String[]{"Credencial", "Mascota", "Dueño", "Plan", "Cuota", "Meses Deuda", "Estado", "¿Habilitada?"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        tabla = new JTable(modelo);
        tabla.setRowHeight(30);
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(tabla);
        add(scroll, BorderLayout.CENTER);

        // Botones accion
        JButton btnAfiliar = crearBoton("Afiliar Mascota");
        JButton btnCobrar = crearBoton("Cobrar Cuota");
        JButton btnCambiarPlan = crearBoton("Cambiar Plan");
        JButton btnSimularMora = crearBoton("Simular Mora (+1 Mes)");
        JButton btnModificarEstado = crearBoton("Suspender / Reactivar");
        JButton btnActualizar = crearBoton("Actualizar");

        btnAfiliar.addActionListener(e -> afiliarMascota());
        btnCobrar.addActionListener(e -> cobrarCuota());
        btnCambiarPlan.addActionListener(e -> cambiarPlan());
        btnSimularMora.addActionListener(e -> simularMora());
        btnModificarEstado.addActionListener(e -> modificarEstado());
        btnActualizar.addActionListener(e -> cargarAfiliaciones());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        botones.setOpaque(false);
        botones.add(btnAfiliar);
        botones.add(btnCobrar);
        botones.add(btnCambiarPlan);
        botones.add(btnSimularMora);
        botones.add(btnModificarEstado);
        botones.add(btnActualizar);

        add(botones, BorderLayout.SOUTH);

        cargarAfiliaciones();
    }

    private JButton crearBoton(String texto) {
        JButton boton = new JButton(texto);
        boton.setBackground(VERDE);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setFont(new Font("SansSerif", Font.BOLD, 12));
        return boton;
    }

    public void cargarAfiliaciones() {
        modelo.setRowCount(0);
        List<AfiliacionPlan> lista = clinica.getObraSocial().listarAfiliados();

        for (AfiliacionPlan a : lista) {
            String habilitadaStr = a.estaHabilitada() ? "SÍ (Aplica descuentos)" : "NO (Sin cobertura)";
            String duenoStr = a.getMascota().getDueño() != null
                    ? a.getMascota().getDueño().obtenerNombreCompleto()
                    : "Sin titular";

            modelo.addRow(new Object[]{
                    a.getNumeroCredencial(),
                    a.getMascota().getNombre(),
                    duenoStr,
                    a.getPlan().getNombrePlan(),
                    String.format("$ %.2f", a.getPlan().getCuotaMensual()),
                    a.getEstadoCuotaTexto(),
                    a.getEstado().toString(),
                    habilitadaStr
            });
        }

        // Actualizar contadores
        lblTotal.setText("Total Afiliados: " + clinica.getObraSocial().contarTotalAfiliados());
        lblAlDia.setText("Al Día: " + clinica.getObraSocial().contarAlDia());
        lblMorosos.setText("En Mora: " + clinica.getObraSocial().contarMorosos());
    }

    private void afiliarMascota() {
        List<Dueño> dueños = clinica.listarDueños();
        if (dueños.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay dueños registrados en la clínica.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<Dueño> comboDuenos = new JComboBox<>(dueños.toArray(new Dueño[0]));
        JComboBox<Mascota> comboMascotas = new JComboBox<>();

        Runnable actualizarMascotas = () -> {
            comboMascotas.removeAllItems();
            Dueño d = (Dueño) comboDuenos.getSelectedItem();
            if (d != null) {
                for (Mascota m : d.getMascotas()) {
                    comboMascotas.addItem(m);
                }
            }
        };

        comboDuenos.addActionListener(e -> actualizarMascotas.run());
        actualizarMascotas.run();

        String[] opcionesPlanes = {
                "Plan Básico ($5.000/mes - 100% consultas, 25% remedios/cirugías)",
                "Plan Premium ($10.000/mes - 100% consultas, 50% remedios/cirugías)",
                "Plan Black ($20.000/mes - 100% total en todo)"
        };
        JComboBox<String> comboPlanes = new JComboBox<>(opcionesPlanes);

        JPanel panelForm = new JPanel(new GridLayout(3, 2, 8, 8));
        panelForm.add(new JLabel("Seleccione Dueño:"));
        panelForm.add(comboDuenos);
        panelForm.add(new JLabel("Seleccione Mascota:"));
        panelForm.add(comboMascotas);
        panelForm.add(new JLabel("Plan de Salud:"));
        panelForm.add(comboPlanes);

        int confirm = JOptionPane.showConfirmDialog(this, panelForm, "Nueva Afiliación a Obra Social", JOptionPane.OK_CANCEL_OPTION);
        if (confirm == JOptionPane.OK_OPTION) {
            Mascota seleccionada = (Mascota) comboMascotas.getSelectedItem();
            if (seleccionada == null) {
                JOptionPane.showMessageDialog(this, "El dueño no tiene mascotas para afiliar.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PlanObraSocial planElegido;
            int indicePlan = comboPlanes.getSelectedIndex();
            if (indicePlan == 0) {
                planElegido = new PlanBasico();
            } else if (indicePlan == 1) {
                planElegido = new PlanPremium();
            } else {
                planElegido = new PlanBlack();
            }

            try {
                AfiliacionPlan nueva = clinica.getObraSocial().afiliarMascota(seleccionada, planElegido);
                cargarAfiliaciones();
                JOptionPane.showMessageDialog(this,
                        "¡Afiliación exitosa!\n\n"
                                + "Mascota: " + seleccionada.getNombre() + "\n"
                                + "Plan: " + planElegido.getNombrePlan() + "\n"
                                + "Credencial Asignada: " + nueva.getNumeroCredencial(),
                        "Afiliación Confirmada", JOptionPane.INFORMATION_MESSAGE);
            } catch (ReglaNegocioException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Regla de Negocio", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void cobrarCuota() {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un afiliado de la tabla para cobrar la cuota.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String credencial = (String) modelo.getValueAt(fila, 0);
        AfiliacionPlan afil = clinica.getObraSocial().buscarPorCredencial(credencial);
        if (afil == null) return;

        if (afil.getMesesAdeudados() <= 0) {
            JOptionPane.showMessageDialog(this,
                    "La mascota '" + afil.getMascota().getNombre() + "' ya se encuentra AL DÍA.\nNo registra cuotas adeudadas para cobrar.",
                    "Afiliación al Día", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        MedioPago[] medios = MedioPago.values();
        MedioPago medioSeleccionado = (MedioPago) JOptionPane.showInputDialog(
                this,
                "Credencial: " + credencial + "\n"
                        + "Mascota: " + afil.getMascota().getNombre() + "\n"
                        + "Plan: " + afil.getPlan().getNombrePlan() + "\n"
                        + "Monto de cuota: $" + String.format("%.2f", afil.getPlan().getCuotaMensual()) + "\n\n"
                        + "Seleccione el medio de pago:",
                "Cobro de Cuota de Obra Social",
                JOptionPane.QUESTION_MESSAGE,
                null,
                medios,
                medios[0]
        );

        if (medioSeleccionado != null) {
            try {
                ReciboCuota recibo = clinica.getObraSocial().cobrarCuota(credencial, medioSeleccionado);
                cargarAfiliaciones();
                JOptionPane.showMessageDialog(this,
                        recibo.obtenerDetalle() + "\n\n¡Cuota cobrada exitosamente!",
                        "Comprobante Emitido", JOptionPane.INFORMATION_MESSAGE);
            } catch (ReglaNegocioException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al cobrar", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cambiarPlan() {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un afiliado para cambiar su plan.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String credencial = (String) modelo.getValueAt(fila, 0);
        String[] opciones = {"Básico ($5.000)", "Premium ($10.000)", "Black ($20.000)"};

        int seleccion = JOptionPane.showOptionDialog(
                this,
                "Seleccione el nuevo plan para la credencial " + credencial + ":",
                "Cambio de Plan de Salud",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );

        if (seleccion >= 0) {
            PlanObraSocial nuevoPlan;
            if (seleccion == 0) nuevoPlan = new PlanBasico();
            else if (seleccion == 1) nuevoPlan = new PlanPremium();
            else nuevoPlan = new PlanBlack();

            try {
                clinica.getObraSocial().cambiarPlan(credencial, nuevoPlan);
                cargarAfiliaciones();
                JOptionPane.showMessageDialog(this, "¡Plan actualizado con éxito al " + nuevoPlan.getNombrePlan() + "!", "Plan Modificado", JOptionPane.INFORMATION_MESSAGE);
            } catch (ReglaNegocioException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // Acción didáctica para simular el paso del tiempo y suspensión por 2 cuotas impagas
    private void simularMora() {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un afiliado para simular un mes impago.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String credencial = (String) modelo.getValueAt(fila, 0);
        AfiliacionPlan afil = clinica.getObraSocial().buscarPorCredencial(credencial);
        if (afil != null) {
            afil.acumularMesImpago();
            cargarAfiliaciones();

            String mensaje = "Se incrementó 1 mes adeudado a la credencial " + credencial + ".\n"
                    + "Deuda actual: " + afil.getMesesAdeudados() + " meses.\n"
                    + "Estado: " + afil.getEstado();

            if (afil.getMesesAdeudados() >= 2) {
                mensaje += "\n\n⚠️ ¡ATENCIÓN! Al acumular 2 o más cuotas impagas, la cobertura fue SUSPENDIDA automáticamente (RN6.3).";
            }
            JOptionPane.showMessageDialog(this, mensaje, "Simulación de Mora", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // CU31: Modificar Estado (Suspender, Reactivar o Baja)
    private void modificarEstado() {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un afiliado de la tabla.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String credencial = (String) modelo.getValueAt(fila, 0);
        String[] opciones = {"Suspender Cobertura", "Reactivar Cobertura", "Dar de Baja Definitiva", "Cancelar"};

        int opcion = JOptionPane.showOptionDialog(
                this,
                "¿Qué acción desea realizar sobre la credencial " + credencial + "?",
                "Gestión de Cobertura",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );

        try {
            if (opcion == 0) {
                clinica.getObraSocial().suspenderAfiliacion(credencial);
                JOptionPane.showMessageDialog(this, "Cobertura suspendida.");
            } else if (opcion == 1) {
                clinica.getObraSocial().reactivarAfiliacion(credencial);
                JOptionPane.showMessageDialog(this, "Cobertura reactivada.");
            } else if (opcion == 2) {
                int confirmarBaja = JOptionPane.showConfirmDialog(this, "¿Está seguro de dar de baja definitiva?", "Confirmar Baja", JOptionPane.YES_NO_OPTION);
                if (confirmarBaja == JOptionPane.YES_OPTION) {
                    clinica.getObraSocial().darDeBaja(credencial);
                    JOptionPane.showMessageDialog(this, "Afiliación dada de baja.");
                }
            }
            cargarAfiliaciones();
        } catch (ReglaNegocioException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}