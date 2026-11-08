import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.*;

/**
 * Sistema experto de integrales. La interfaz NO integra ni evalua funciones:
 * toda la logica matematica se ejecuta en SWI-Prolog mediante un subproceso.
 * Compatible con Java 11 o superior. Sin bibliotecas externas.
 */
public class IntegralApp extends JFrame {
    private static final Color FONDO = new Color(244, 247, 251);
    private static final Color TINTA = new Color(30, 42, 62);
    private static final Color SECUNDARIO = new Color(91, 108, 130);
    private static final Color AZUL = new Color(32, 93, 191);
    private static final Color BORDE = new Color(221, 228, 238);
    private static final Font FUENTE = new Font("SansSerif", Font.PLAIN, 14);
    private final JTextField funcion = new JTextField("3*x^2+2*x+1");
    private final JTextField limiteA = new JTextField("0");
    private final JTextField limiteB = new JTextField("2");
    private final JTextField ejecutable = new JTextField(defaultSWI());
    private final JTextField archivoProlog = new JTextField("src/prolog/integrales.pl");
    private final JLabel primitiva = new JLabel("—");
    private final JLabel definida = new JLabel("—");
    private final JLabel area = new JLabel("—");
    private final JLabel estado = new JLabel("Listo para resolver.");
    private final PlotPanel grafica = new PlotPanel();
    private final JButton calcular = crearBoton("Calcular integral", AZUL, Color.BLACK);
    private final JComboBox<Ejemplo> ejemplos = new JComboBox<>();

    public IntegralApp() {
        super("Calculadora de Integrales | Java + SWI-Prolog");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(940, 665));
        setSize(1180, 790);
        setLocationRelativeTo(null);
        getContentPane().setBackground(FONDO);
        getContentPane().setLayout(new BorderLayout(0, 0));
        getContentPane().add(cabecera(), BorderLayout.NORTH);

        JPanel controles = controles();
        JScrollPane controlesDesplazables = new JScrollPane(controles);
        controlesDesplazables.setBorder(null);
        controlesDesplazables.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        controlesDesplazables.getVerticalScrollBar().setUnitIncrement(16);
        JPanel resultados = resultados();
        JSplitPane divisor = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlesDesplazables, resultados);
        divisor.setDividerLocation(415);
        divisor.setResizeWeight(0.33);
        divisor.setBorder(BorderFactory.createEmptyBorder(18, 18, 15, 18));
        divisor.setDividerSize(10);
        divisor.setContinuousLayout(true);
        getContentPane().add(divisor, BorderLayout.CENTER);

        estado.setForeground(SECUNDARIO);
        estado.setBorder(BorderFactory.createEmptyBorder(0, 24, 15, 10));
        getContentPane().add(estado, BorderLayout.SOUTH);
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

        panel.add(subtitulo("Cargar un ejemplo"));
        poblarEjemplos();
        ejemplos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 37));
        ejemplos.setFont(FUENTE);
        ejemplos.addActionListener(e -> {
            Ejemplo seleccionado = (Ejemplo) ejemplos.getSelectedItem();
            if (seleccionado != null && seleccionado.funcion != null) {
                funcion.setText(seleccionado.funcion);
                limiteA.setText(seleccionado.a);
                limiteB.setText(seleccionado.b);
            }
        });
        panel.add(ejemplos);
        panel.add(Box.createVerticalStrut(20));

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
        valor.setForeground(TINTA);
        valor.setFont(new Font("SansSerif", Font.BOLD, 17));
        valor.setAlignmentX(Component.LEFT_ALIGNMENT);
        encabezado.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(encabezado);
        p.add(Box.createVerticalStrut(8));
        p.add(valor);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private void ejecutarConsulta() {
        String expr = funcion.getText().trim();
        String aTxt = limiteA.getText().trim().replace(',', '.');
        String bTxt = limiteB.getText().trim().replace(',', '.');
        double a, b;
        try {
            if (expr.isEmpty() || expr.contains("\n") || expr.contains("\t"))
                throw new IllegalArgumentException("Ingresa una expresión válida.");
            a = Double.parseDouble(aTxt);
            b = Double.parseDouble(bTxt);
            if (!Double.isFinite(a) || !Double.isFinite(b) || Math.abs(a) >= 100000 || Math.abs(b) >= 100000)
                throw new IllegalArgumentException("Los límites deben ser números finitos entre -100000 y 100000.");
        } catch (NumberFormatException e) {
            mensaje("Los límites deben ser números válidos. Ejemplo: -1.5 y 2.");
            return;
        } catch (IllegalArgumentException e) {
            mensaje(e.getMessage());
            return;
        }
        Path archivo = Paths.get(archivoProlog.getText().trim()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(archivo)) {
            mensaje("No se encontró integrales.pl. Selecciónalo con «Examinar archivo Prolog».");
            return;
        }
        String swipl = ejecutable.getText().trim();
        if (swipl.isEmpty()) { mensaje("Indica el ejecutable de SWI-Prolog (swipl o ruta a swipl.exe)."); return; }
        final double limiteInf = a;
        final double limiteSup = b;
        calcular.setEnabled(false);
        calcular.setText("Calculando...");
        estado.setText("Consultando las reglas de la base de conocimiento SWI-Prolog...");
        grafica.limpiar();

        SwingWorker<Resultado, Void> tarea = new SwingWorker<Resultado, Void>() {
            @Override protected Resultado doInBackground() throws Exception {
                return consultarProlog(swipl, archivo, expr, limiteInf, limiteSup);
            }
            @Override protected void done() {
                calcular.setEnabled(true);
                calcular.setText("Calcular integral");
                try {
                    Resultado r = get();
                    primitiva.setText("<html>" + escaparHTML(r.primitiva) + "  + C</html>");
                    primitiva.setToolTipText(r.primitiva + " + C");
                    definida.setText(formatear(r.definida));
                    area.setText(formatear(r.area));
                    grafica.actualizar(r.puntos, Math.min(limiteInf,limiteSup), Math.max(limiteInf,limiteSup));
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

    private static Resultado consultarProlog(String swipl, Path archivo, String expr, double a, double b)
            throws Exception {
        ProcessBuilder pb = new ProcessBuilder(swipl, "-q", "-s", archivo.toString(), "-g", "puente", "-t", "halt");
        pb.redirectErrorStream(true);
        Process p;
        try { p = pb.start(); }
        catch (IOException ex) {
            throw new IOException("No se pudo iniciar SWI-Prolog. Comprueba su instalación y la ruta del ejecutable: " + swipl, ex);
        }
        try {
            try (Writer out = new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8)) {
                out.write(expr + "\n" + Double.toString(a) + "\n" + Double.toString(b) + "\n");
            }
            if (!p.waitFor(20, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new IOException("SWI-Prolog tardó demasiado. Revisa el cálculo o la instalación.");
            }
            String texto = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String[] lineas = texto.split("\\R");
            String[] datos = null;
            String dibujo = null;
            for (String linea : lineas) {
                if (linea.startsWith("ERROR\t"))
                    throw new IllegalArgumentException(linea.substring(6));
                if (linea.startsWith("OK\t")) datos = linea.split("\t", -1);
                if (linea.startsWith("PLOT\t")) dibujo = linea.substring(5);
            }
            if (datos == null || datos.length != 4 || dibujo == null)
                throw new IOException("Respuesta Prolog incompleta. Detalle: " + (texto.length() > 240 ? texto.substring(0,240) : texto));
            String[] cadenas = dibujo.split(";");
            double[] puntos = new double[cadenas.length];
            for (int i = 0; i < cadenas.length; i++) puntos[i] = Double.parseDouble(cadenas[i]);
            return new Resultado(datos[1], Double.parseDouble(datos[2]), Double.parseDouble(datos[3]), puntos);
        } finally {
            if (p.isAlive()) p.destroyForcibly();
        }
    }

    private void poblarEjemplos() {
        ejemplos.addItem(new Ejemplo("— Selecciona un ejemplo —", null, null, null));
        ejemplos.addItem(new Ejemplo("Constante", "5", "0", "3"));
        ejemplos.addItem(new Ejemplo("Variable x", "x", "0", "2"));
        ejemplos.addItem(new Ejemplo("Constante por x", "3*x", "0", "2"));
        ejemplos.addItem(new Ejemplo("Potencia entera", "x^3", "0", "2"));
        ejemplos.addItem(new Ejemplo("Potencia negativa", "1/x", "1", "2"));
        ejemplos.addItem(new Ejemplo("Suma: polinomio", "3*x^2+2*x+1", "0", "2"));
        ejemplos.addItem(new Ejemplo("Resta", "x^2-x", "0", "1"));
        ejemplos.addItem(new Ejemplo("Seno de una función", "sin(2*x)", "0", "1.5707963268"));
        ejemplos.addItem(new Ejemplo("Coeficiente por seno", "3*sin(x)", "0", "1.5707963268"));
        ejemplos.addItem(new Ejemplo("Exponencial base 2", "2^x", "0", "2"));
        ejemplos.addItem(new Ejemplo("Exponencial de u", "2^(2*x+1)", "0", "1"));
        ejemplos.addItem(new Ejemplo("Seno", "sin(x)", "0", "1.5707963268"));
        ejemplos.addItem(new Ejemplo("Coseno", "cos(x)", "0", "1.5707963268"));
        ejemplos.addItem(new Ejemplo("Tangente", "tan(x)", "0", "0.7853981634"));
        ejemplos.addItem(new Ejemplo("Seno hiperbólico", "sinh(x)", "0", "1"));
        ejemplos.addItem(new Ejemplo("Coseno hiperbólico", "cosh(x)", "0", "1"));
        ejemplos.addItem(new Ejemplo("Tangente hiperbólica", "tanh(x)", "0", "1"));
        ejemplos.addItem(new Ejemplo("Arcoseno hiperbólico", "arcsinh(x)", "0", "1"));
        ejemplos.addItem(new Ejemplo("Arcocoseno hiperbólico", "arccosh(x)", "1", "2"));
        ejemplos.addItem(new Ejemplo("Arcotangente hiperbólica", "arctanh(x)", "0", "0.5"));
        ejemplos.addItem(new Ejemplo("Área con cambio de signo", "x", "-1", "1"));
    }

    private void limpiar() {
        primitiva.setText("—"); definida.setText("—"); area.setText("—");
        grafica.limpiar(); estado.setText("Resultados borrados. Puedes calcular otra función.");
    }
    private static String defaultSWI() {
        String entorno = System.getenv("SWIPL_EXE");
        return entorno != null && !entorno.trim().isEmpty() ? entorno.trim() : "swipl";
    }
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
        b.setBackground(fondo); b.setForeground(textoColor);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setOpaque(true); b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(11, 12, 11, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private static JPanel campoConEtiqueta(String titulo, JTextField campo) {
        JPanel p = new JPanel(); p.setOpaque(false);
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        JLabel l = subtitulo(titulo);
        p.add(l); p.add(Box.createVerticalStrut(7));
        campo.setFont(FUENTE);
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        campo.setBorder(new CompoundBorder(new LineBorder(BORDE), new EmptyBorder(8,9,8,9)));
        p.add(campo);
        return p;
    }
    private static void agregarCampo(JPanel padre, String titulo, JTextField campo) {
        JPanel p = campoConEtiqueta(titulo,campo);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        padre.add(p);
        padre.add(Box.createVerticalStrut(12));
    }
    private static String escaparHTML(String texto) {
        return texto.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
    private static String formatear(double d) {
        if (Math.abs(d) < 1e-10) d = 0;
        DecimalFormat formato = new DecimalFormat("0.######", DecimalFormatSymbols.getInstance(Locale.US));
        return Math.abs(d) >= 1e8 ? String.format(Locale.US,"%.6e",d) : formato.format(d);
    }
    private void mensaje(String texto) {
        JOptionPane.showMessageDialog(this,texto,"Aviso del sistema",JOptionPane.WARNING_MESSAGE);
    }
    private static class Ejemplo {
        final String titulo, funcion, a, b;
        Ejemplo(String titulo, String funcion, String a, String b) {
            this.titulo=titulo;this.funcion=funcion;this.a=a;this.b=b;
        }
        @Override public String toString() {return titulo;}
    }
    private static class Resultado {
        final String primitiva; final double definida,area; final double[] puntos;
        Resultado(String p,double d,double a,double[] y) {primitiva=p;definida=d;area=a;puntos=y;}
    }

    /** Grafica sin librerias: Java solo recibe puntos previamente calculados en Prolog. */
    private static class PlotPanel extends JPanel {
        private double[] ys;
        private double xmin, xmax;
        PlotPanel() {
            setBackground(new Color(251,252,254));
            setBorder(new LineBorder(BORDE,1,true));
            setMinimumSize(new Dimension(280,170));
        }
        void limpiar() {ys=null; repaint();}
        void actualizar(double[] puntos,double a,double b) {ys=puntos; xmin=a; xmax=b; repaint();}
        @Override protected void paintComponent(Graphics grafico) {
            super.paintComponent(grafico);
            Graphics2D g = (Graphics2D) grafico.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(),left=54,right=22,top=24,bottom=46;
            int plotW=Math.max(1,w-left-right),plotH=Math.max(1,h-top-bottom);
            if (ys==null || ys.length<2) {
                g.setColor(SECUNDARIO);g.setFont(new Font("SansSerif",Font.PLAIN,14));
                String t="La gráfica aparecerá al calcular.";
                FontMetrics fm=g.getFontMetrics();
                g.drawString(t,Math.max(8,(w-fm.stringWidth(t))/2),h/2);
                g.dispose();return;
            }
            double yMin=0,yMax=0;
            for(double y:ys) {if(Double.isFinite(y)){yMin=Math.min(yMin,y);yMax=Math.max(yMax,y);}}
            double rango=Math.max(1e-9,yMax-yMin);
            yMin-=rango*0.12; yMax+=rango*0.12;
            double escala=(yMax-yMin);
            g.setFont(new Font("SansSerif",Font.PLAIN,11));
            g.setColor(new Color(224,230,239));
            for(int i=0;i<=4;i++) {
                int yp=top+(int)Math.round(i*plotH/4.0);
                g.drawLine(left,yp,left+plotW,yp);
                double yv=yMax-i*escala/4;
                g.setColor(SECUNDARIO);
                g.drawString(eje(yv),5,yp+4);
                g.setColor(new Color(224,230,239));
            }
            for(int i=0;i<=4;i++) {
                int xp=left+(int)Math.round(i*plotW/4.0);
                g.drawLine(xp,top,xp,top+plotH);
                g.setColor(SECUNDARIO);
                double xv=xmin+(xmax-xmin)*i/4;
                g.drawString(eje(xv),xp-12,h-22);
                g.setColor(new Color(224,230,239));
            }
            double yCero=top+(yMax/escala)*plotH;
            int yyCero=(int)Math.round(yCero);
            g.setColor(new Color(104,123,146));
            if (yyCero>=top && yyCero<=top+plotH) g.drawLine(left,yyCero,left+plotW,yyCero);
            // Sombrear positividad/negatividad de cada trapecio.
            for(int i=0;i<ys.length-1;i++) {
                double x1=left+i*plotW/(double)(ys.length-1);
                double x2=left+(i+1)*plotW/(double)(ys.length-1);
                double y1=top+(yMax-ys[i])*plotH/escala;
                double y2=top+(yMax-ys[i+1])*plotH/escala;
                Path2D.Double poligono=new Path2D.Double();
                poligono.moveTo(x1,yCero); poligono.lineTo(x1,y1);
                poligono.lineTo(x2,y2); poligono.lineTo(x2,yCero); poligono.closePath();
                g.setColor(ys[i]+ys[i+1]>=0?new Color(53,112,211,57):new Color(224,128,54,70));
                g.fill(poligono);
            }
            Path2D.Double linea=new Path2D.Double();
            for(int i=0;i<ys.length;i++) {
                double xx=left+i*plotW/(double)(ys.length-1);
                double yy=top+(yMax-ys[i])*plotH/escala;
                if(i==0)linea.moveTo(xx,yy);else linea.lineTo(xx,yy);
            }
            g.setColor(AZUL);g.setStroke(new BasicStroke(2.4f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g.draw(linea);
            g.setFont(new Font("SansSerif",Font.BOLD,11));g.setColor(TINTA);
            g.drawString("f(x)",left+7,top+15);
            g.drawString("x",left+plotW+8,top+plotH+8);
            g.dispose();
        }
        private static String eje(double v){
            if(Math.abs(v)<1e-10)return "0";
            return String.format(Locale.US,"%.2g",v);
        }
    }

    public static void main(String[] args) {
        try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new IntegralApp().setVisible(true));
    }
}
