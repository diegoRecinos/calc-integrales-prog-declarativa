import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Locale;

/**
 * Componente gráfico personalizado para renderizar la función f(x) y el área bajo la curva.
 *
 * Características clave para la defensa oral:
 * 1. Independiente de librerías externas (solo Java AWT/Swing estándar).
 * 2. Visualización pura: Java NO calcula los valores de la función; recibe los puntos
 *    ya evaluados por el motor Prolog y los mapea a coordenadas de pantalla (píxeles).
 * 3. Sombrea las regiones del área distinguiendo si están por encima (positivo, azul traslúcido)
 *    o por debajo (negativo, naranja traslúcido) del eje horizontal y = 0.
 */
public class PlotPanel extends JPanel {
    private static final Color COLOR_FONDO = new Color(251, 252, 254);
    private static final Color COLOR_BORDE = new Color(221, 228, 238);
    private static final Color COLOR_SECUNDARIO = new Color(91, 108, 130);
    private static final Color COLOR_TEXTO = new Color(30, 42, 62);
    private static final Color COLOR_LINEA_CURVA = new Color(32, 93, 191);
    private static final Color COLOR_REJILLA = new Color(224, 230, 239);
    private static final Color COLOR_EJE_CERO = new Color(104, 123, 146);
    private static final Color COLOR_AREA_POSITIVA = new Color(53, 112, 211, 57);
    private static final Color COLOR_AREA_NEGATIVA = new Color(224, 128, 54, 70);

    private double[] ys;
    private double xmin, xmax;

    public PlotPanel() {
        setBackground(COLOR_FONDO);
        setBorder(new LineBorder(COLOR_BORDE, 1, true));
        setMinimumSize(new Dimension(280, 170));
    }

    public void limpiar() {
        ys = null;
        repaint();
    }

    public void actualizar(double[] puntos, double a, double b) {
        this.ys = puntos;
        this.xmin = a;
        this.xmax = b;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics grafico) {
        super.paintComponent(grafico);
        Graphics2D g = (Graphics2D) grafico.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int left = 54, right = 22, top = 24, bottom = 46;
        int plotW = Math.max(1, w - left - right);
        int plotH = Math.max(1, h - top - bottom);

        if (ys == null || ys.length < 2) {
            g.setColor(COLOR_SECUNDARIO);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            String t = "La gráfica aparecerá al calcular.";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(t, Math.max(8, (w - fm.stringWidth(t)) / 2), h / 2);
            g.dispose();
            return;
        }

        double yMin = 0, yMax = 0;
        for (double y : ys) {
            if (Double.isFinite(y)) {
                yMin = Math.min(yMin, y);
                yMax = Math.max(yMax, y);
            }
        }
        double rango = Math.max(1e-9, yMax - yMin);
        yMin -= rango * 0.12;
        yMax += rango * 0.12;
        double escala = (yMax - yMin);

        // Rejilla y etiquetas del eje Y
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(COLOR_REJILLA);
        for (int i = 0; i <= 4; i++) {
            int yp = top + (int) Math.round(i * plotH / 4.0);
            g.drawLine(left, yp, left + plotW, yp);
            double yv = yMax - i * escala / 4;
            g.setColor(COLOR_SECUNDARIO);
            g.drawString(formatearEje(yv), 5, yp + 4);
            g.setColor(COLOR_REJILLA);
        }

        // Rejilla y etiquetas del eje X
        for (int i = 0; i <= 4; i++) {
            int xp = left + (int) Math.round(i * plotW / 4.0);
            g.drawLine(xp, top, xp, top + plotH);
            g.setColor(COLOR_SECUNDARIO);
            double xv = xmin + (xmax - xmin) * i / 4;
            g.drawString(formatearEje(xv), xp - 12, h - 22);
            g.setColor(COLOR_REJILLA);
        }

        // Línea horizontal del eje y = 0
        double yCero = top + (yMax / escala) * plotH;
        int yyCero = (int) Math.round(yCero);
        g.setColor(COLOR_EJE_CERO);
        if (yyCero >= top && yyCero <= top + plotH) {
            g.drawLine(left, yyCero, left + plotW, yyCero);
        }

        // Sombrear positividad/negatividad de cada trapecio entre la curva y el eje y = 0
        for (int i = 0; i < ys.length - 1; i++) {
            double x1 = left + i * plotW / (double) (ys.length - 1);
            double x2 = left + (i + 1) * plotW / (double) (ys.length - 1);
            double y1 = top + (yMax - ys[i]) * plotH / escala;
            double y2 = top + (yMax - ys[i + 1]) * plotH / escala;

            Path2D.Double poligono = new Path2D.Double();
            poligono.moveTo(x1, yCero);
            poligono.lineTo(x1, y1);
            poligono.lineTo(x2, y2);
            poligono.lineTo(x2, yCero);
            poligono.closePath();

            g.setColor(ys[i] + ys[i + 1] >= 0 ? COLOR_AREA_POSITIVA : COLOR_AREA_NEGATIVA);
            g.fill(poligono);
        }

        // Trazado continuo de la curva f(x)
        Path2D.Double linea = new Path2D.Double();
        for (int i = 0; i < ys.length; i++) {
            double xx = left + i * plotW / (double) (ys.length - 1);
            double yy = top + (yMax - ys[i]) * plotH / escala;
            if (i == 0) {
                linea.moveTo(xx, yy);
            } else {
                linea.lineTo(xx, yy);
            }
        }
        g.setColor(COLOR_LINEA_CURVA);
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(linea);

        // Nombres de los ejes
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(COLOR_TEXTO);
        g.drawString("f(x)", left + 7, top + 15);
        g.drawString("x", left + plotW + 8, top + plotH + 8);
        g.dispose();
    }

    private static String formatearEje(double v) {
        if (Math.abs(v) < 1e-10) return "0";
        return String.format(Locale.US, "%.2g", v);
    }
}
