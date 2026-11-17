import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * SISTEMA SOLUCIONADOR DE INTEGRALES
 * Programación Declarativa 02/2026 - UCA
 *
 * Capa de Presentación y Controlador (Java Swing):
 * - Captura las entradas del usuario (función y límites [a, b]).
 * - Valida las entradas en la interfaz antes de delegar la resolución.
 * - Invoca en segundo plano (asíncrono con SwingWorker) a {@link PrologService}.
 * - Muestra los resultados calculados por el motor declarativo (primitiva, integral definida, área).
 * - Renderiza la gráfica de la función en {@link PlotPanel} a partir de los puntos evaluados en Prolog.
 *
 * NOTA ARQUITECTÓNICA PARA LA EXPOSICIÓN:
 * Toda la lógica matemática (integración simbólica, evaluación y cálculo de área)
 * reside exclusivamente en la base de conocimiento SWI-Prolog (src/prolog/integrales.pl).
 */
public class IntegralApp extends JFrame {

    // Paleta de colores y estilos de la interfaz
    private static final Color FONDO = new Color(244, 247, 251);
    private static final Color TINTA = new Color(30, 42, 62);
    private static final Color SECUNDARIO = new Color(91, 108, 130);
    private static final Color AZUL = new Color(32, 93, 191);
    private static final Color BORDE = new Color(221, 228, 238);
    private static final Font FUENTE = new Font("SansSerif", Font.PLAIN, 14);

    // Componentes de entrada
    private final JTextField funcion = new JTextField("3*x^2+2*x+1");
    private final JTextField limiteA = new JTextField("0");
    private final JTextField limiteB = new JTextField("2");
    private final JTextField ejecutable = new JTextField(PrologService.detectarEjecutableProlog());
    private final JTextField archivoProlog = new JTextField(PrologService.detectarArchivoProlog());
    private final JComboBox<Ejemplo> ejemplos = new JComboBox<>();

    // Componentes de salida y visualización
    private final JLabel primitiva = new JLabel("—");
    private final JLabel definida = new JLabel("—");
    private final JLabel area = new JLabel("—");
    private final JLabel estado = new JLabel("Listo para resolver.");
    private final PlotPanel grafica = new PlotPanel();
    private final JButton calcular = crearBoton("Calcular integral", AZUL, Color.BLACK);

    public IntegralApp() {
        super("Calculadora de Integrales | Java + SWI-Prolog");
        configurarVentana();
        inicializarEventos();
    }

    private void configurarVentana() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(940, 665));
        setSize(1180, 790);
        setLocationRelativeTo(null);
        getContentPane().setBackground(FONDO);
        getContentPane().setLayout(new BorderLayout(0, 0));

        // Encabezado superior
        getContentPane().add(cabecera(), BorderLayout.NORTH);

        // Panel divisor central: Controles a la izquierda, Resultados a la derecha
        JPanel controles = controles();
        JScrollPane scrollControles = new JScrollPane(controles);
        scrollControles.setBorder(null);
        scrollControles.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollControles.getVerticalScrollBar().setUnitIncrement(16);

        JPanel panelResultados = resultados();
        JSplitPane divisor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollControles, panelResultados);
        divisor.setDividerLocation(415);
        divisor.setResizeWeight(0.33);
        divisor.setBorder(BorderFactory.createEmptyBorder(18, 18, 15, 18));
        divisor.setDividerSize(10);
        divisor.setContinuousLayout(true);
        getContentPane().add(divisor, BorderLayout.CENTER);

        // Barra de estado inferior
        estado.setForeground(SECUNDARIO);
        estado.setBorder(BorderFactory.createEmptyBorder(0, 24, 15, 10));
        getContentPane().add(estado, BorderLayout.SOUTH);
    }

    private void inicializarEventos() {
        calcular.addActionListener(e -> ejecutarConsulta());
        funcion.addActionListener(e -> ejecutarConsulta());
        limiteA.addActionListener(e -> ejecutarConsulta());
        limiteB.addActionListener(e -> ejecutarConsulta());
    }

    private JPanel cabecera() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(TINTA);
        panel.setBorder(BorderFactory.createEmptyBorder(23, 28, 22, 28));

        JLabel titulo = new JLabel("SISTEMA SOLUCIONADOR DE INTEGRALES");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 21));

        JLabel descripcion = new JLabel("Programación Declarativa 02/2026  ·  Motor SWI-Prolog  ·  Interfaz Java Swing");
        descripcion.setForeground(new Color(183, 204, 236));
        descripcion.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(7));
        textos.add(descripcion);
        panel.add(textos, BorderLayout.CENTER);

        JLabel etiqueta = new JLabel("CÁLCULO SIMBÓLICO + ÁREA");
        etiqueta.setFont(new Font("SansSerif", Font.BOLD, 11));
        etiqueta.setForeground(new Color(202, 225, 255));
        panel.add(etiqueta, BorderLayout.EAST);

        return panel;
    }

    private JPanel controles() {
        JPanel panel = tarjeta();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Sección 1: Función y Límites
        panel.add(encabezado("01  Ingresa la función"));
        panel.add(Box.createVerticalStrut(15));
        agregarCampo(panel, "f(x)  ·  escribe la expresión", funcion);

        JLabel norma = new JLabel("Usa x, * para multiplicar y ^ para potencias.");
        norma.setFont(new Font("SansSerif", Font.PLAIN, 11));
        norma.setForeground(SECUNDARIO);
        panel.add(norma);
        panel.add(Box.createVerticalStrut(15));

        JPanel limites = new JPanel(new GridLayout(1, 2, 12, 0));
        limites.setOpaque(false);
        limites.add(campoConEtiqueta("Límite inferior (a)", limiteA));
        limites.add(campoConEtiqueta("Límite superior (b)", limiteB));
        panel.add(limites);
        panel.add(Box.createVerticalStrut(20));

        // Selector de ejemplos demostrativos
        panel.add(subtitulo("Cargar un ejemplo"));
        poblarEjemplos();
        ejemplos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 37));
        ejemplos.setFont(FUENTE);
        ejemplos.addActionListener(e -> {
            Ejemplo seleccionado = (Ejemplo) ejemplos.getSelectedItem();
            if (seleccionado != null && seleccionado.getFuncion() != null) {
                funcion.setText(seleccionado.getFuncion());
                limiteA.setText(seleccionado.getA());
                limiteB.setText(seleccionado.getB());
            }
        });
        panel.add(ejemplos);
        panel.add(Box.createVerticalStrut(20));

        // Botones de acción
        calcular.setAlignmentX(Component.CENTER_ALIGNMENT);
        calcular.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        panel.add(calcular);
        panel.add(Box.createVerticalStrut(12));

        JButton limpiar = crearBoton("Limpiar resultados", new Color(229, 236, 246), TINTA);
        limpiar.setAlignmentX(Component.CENTER_ALIGNMENT);
        limpiar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 41));
        limpiar.addActionListener(e -> limpiar());
        panel.add(limpiar);
        panel.add(Box.createVerticalStrut(23));

        // Sección 2: Configuración del entorno Prolog
        panel.add(encabezado("02  Conexión con Prolog"));
        panel.add(Box.createVerticalStrut(14));
        agregarCampo(panel, "Ejecutable swipl", ejecutable);
        agregarCampo(panel, "Ruta al archivo .pl", archivoProlog);

        JButton buscar = crearBoton("Examinar archivo Prolog...", new Color(229, 236, 246), TINTA);
        buscar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        buscar.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(".");
            chooser.setDialogTitle("Selecciona integrales.pl");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                archivoProlog.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        panel.add(buscar);
        panel.add(Box.createVerticalGlue());

        JLabel nota = new JLabel("Toda la matemática se resuelve en Prolog.");
        nota.setFont(new Font("SansSerif", Font.ITALIC, 11));
        nota.setForeground(SECUNDARIO);
        panel.add(Box.createVerticalStrut(10));
        panel.add(nota);

        return panel;
    }

    private JPanel resultados() {
        JPanel panel = tarjeta();
        panel.setLayout(new BorderLayout(0, 16));

        // Primitiva y valores numéricos
        JPanel superior = new JPanel();
        superior.setOpaque(false);
        superior.setLayout(new BoxLayout(superior, BoxLayout.Y_AXIS));
        superior.add(encabezado("03  Resultado de la integración"));
        superior.add(Box.createVerticalStrut(11));
        superior.add(bloque("Primitiva  ·  F(x) + C", primitiva));
        superior.add(Box.createVerticalStrut(11));

        JPanel cifras = new JPanel(new GridLayout(1, 2, 12, 0));
        cifras.setOpaque(false);
        cifras.setAlignmentX(Component.LEFT_ALIGNMENT);
        cifras.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        cifras.add(bloque("Integral definida  ·  F(b) − F(a)", definida));
        cifras.add(bloque("Área geométrica  ·  ∫|f(x)| dx", area));
        superior.add(cifras);
        panel.add(superior, BorderLayout.NORTH);

        // Panel con la gráfica
        JPanel graficoPanel = new JPanel(new BorderLayout(0, 10));
        graficoPanel.setOpaque(false);
        graficoPanel.add(encabezado("04  Gráfica de la función"), BorderLayout.NORTH);
        graficoPanel.add(grafica, BorderLayout.CENTER);

        JLabel pie = new JLabel("Visualización aproximada. Prolog calcula los puntos y el área.");
        pie.setFont(new Font("SansSerif", Font.PLAIN, 11));
        pie.setForeground(SECUNDARIO);
        graficoPanel.add(pie, BorderLayout.SOUTH);

        panel.add(graficoPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel bloque(String titulo, JLabel valor) {
        JPanel p = new JPanel();
        p.setBackground(new Color(247, 249, 253));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new CompoundBorder(new LineBorder(BORDE, 1, true),
                new EmptyBorder(12, 14, 12, 14)));

        JLabel encabezado = new JLabel(titulo);
        encabezado.setForeground(SECUNDARIO);
        encabezado.setFont(new Font("SansSerif", Font.BOLD, 12));
        encabezado.setAlignmentX(Component.LEFT_ALIGNMENT);

        valor.setForeground(TINTA);
        valor.setFont(new Font("SansSerif", Font.BOLD, 17));
        valor.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(encabezado);
        p.add(Box.createVerticalStrut(8));
        p.add(valor);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    /**
     * Valida entradas y dispara la consulta asíncrona a Prolog mediante SwingWorker.
     */
    private void ejecutarConsulta() {
        String expr = funcion.getText().trim();
        String aTxt = limiteA.getText().trim().replace(',', '.');
        String bTxt = limiteB.getText().trim().replace(',', '.');
        double a, b;

        try {
            if (expr.isEmpty() || expr.contains("\n") || expr.contains("\t")) {
                throw new IllegalArgumentException("Ingresa una expresión válida.");
            }
            a = Double.parseDouble(aTxt);
            b = Double.parseDouble(bTxt);
            if (!Double.isFinite(a) || !Double.isFinite(b) || Math.abs(a) >= 100000 || Math.abs(b) >= 100000) {
                throw new IllegalArgumentException("Los límites deben ser números finitos entre -100000 y 100000.");
            }
        } catch (NumberFormatException e) {
            mensaje("Los límites deben ser números válidos. Ejemplo: -1.5 y 2.");
            return;
        } catch (IllegalArgumentException e) {
            mensaje(e.getMessage());
            return;
        }

        Path archivo = Paths.get(archivoProlog.getText().trim()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(archivo)) {
            // Intentar autodetección si la ruta ingresada no existe
            String detectada = PrologService.detectarArchivoProlog();
            Path archivoDetectado = Paths.get(detectada).toAbsolutePath().normalize();
            if (Files.isRegularFile(archivoDetectado)) {
                archivo = archivoDetectado;
                archivoProlog.setText(detectada);
            } else {
                mensaje("No se encontró integrales.pl. Selecciónalo con «Examinar archivo Prolog».");
                return;
            }
        }

        String swipl = ejecutable.getText().trim();
        if (swipl.isEmpty()) {
            mensaje("Indica el ejecutable de SWI-Prolog (swipl o ruta a swipl.exe).");
            return;
        }

        final double limiteInf = a;
        final double limiteSup = b;
        final Path archivoPrologFinal = archivo;

        // Actualizar estado de la interfaz antes de iniciar
        calcular.setEnabled(false);
        calcular.setText("Calculando...");
        estado.setText("Consultando las reglas de la base de conocimiento SWI-Prolog...");
        grafica.limpiar();

        // SwingWorker desacopla el cálculo del hilo de eventos de Swing (EDT)
        SwingWorker<Resultado, Void> tarea = new SwingWorker<Resultado, Void>() {
            @Override
            protected Resultado doInBackground() throws Exception {
                return PrologService.consultar(swipl, archivoPrologFinal, expr, limiteInf, limiteSup);
            }

            @Override
            protected void done() {
                calcular.setEnabled(true);
                calcular.setText("Calcular integral");
                try {
                    Resultado r = get();
                    primitiva.setText("<html>" + escaparHTML(r.getPrimitiva()) + "  + C</html>");
                    primitiva.setToolTipText(r.getPrimitiva() + " + C");
                    definida.setText(formatear(r.getDefinida()));
                    area.setText(formatear(r.getArea()));
                    grafica.actualizar(r.getPuntos(), Math.min(limiteInf, limiteSup), Math.max(limiteInf, limiteSup));
                    estado.setText("Resuelto correctamente por SWI-Prolog.");
                } catch (Exception ex) {
                    Throwable causa = ex instanceof ExecutionException ? ex.getCause() : ex;
                    estado.setText("No se pudo completar el cálculo.");
                    mensaje(causa != null ? causa.getMessage() : ex.getMessage());
                }
            }
        };
        tarea.execute();
    }

    private void poblarEjemplos() {
        for (Ejemplo ej : Ejemplo.obtenerEjemplos()) {
            ejemplos.addItem(ej);
        }
    }

    private void limpiar() {
        primitiva.setText("—");
        definida.setText("—");
        area.setText("—");
        grafica.limpiar();
        estado.setText("Resultados borrados. Puedes calcular otra función.");
    }

    // Métodos auxiliares de construcción UI
    private static JPanel tarjeta() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(BORDE, 1, true), new EmptyBorder(19, 20, 19, 20)));
        return p;
    }

    private static JLabel encabezado(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("SansSerif", Font.BOLD, 16));
        l.setForeground(TINTA);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel subtitulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(SECUNDARIO);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JButton crearBoton(String texto, Color fondo, Color textoColor) {
        JButton b = new JButton(texto);
        b.setBackground(fondo);
        b.setForeground(textoColor);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(11, 12, 11, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JPanel campoConEtiqueta(String titulo, JTextField campo) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = subtitulo(titulo);
        p.add(l);
        p.add(Box.createVerticalStrut(7));
        campo.setFont(FUENTE);
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        campo.setBorder(new CompoundBorder(new LineBorder(BORDE), new EmptyBorder(8, 9, 8, 9)));
        p.add(campo);
        return p;
    }

    private static void agregarCampo(JPanel padre, String titulo, JTextField campo) {
        JPanel p = campoConEtiqueta(titulo, campo);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        padre.add(p);
        padre.add(Box.createVerticalStrut(12));
    }

    private static String escaparHTML(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String formatear(double d) {
        if (Math.abs(d) < 1e-10) d = 0;
        DecimalFormat formato = new DecimalFormat("0.######", DecimalFormatSymbols.getInstance(Locale.US));
        return Math.abs(d) >= 1e8 ? String.format(Locale.US, "%.6e", d) : formato.format(d);
    }

    private void mensaje(String texto) {
        JOptionPane.showMessageDialog(this, texto, "Aviso del sistema", JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new IntegralApp().setVisible(true));
    }
}
