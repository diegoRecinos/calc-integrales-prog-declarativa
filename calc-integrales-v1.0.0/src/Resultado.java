/**
 * Modelo de datos que encapsula el resultado del cálculo realizado en SWI-Prolog.
 * 
 * Separa los datos del cálculo de la interfaz gráfica:
 * - primitiva: Expresión simbólica de la función antiderivada F(x).
 * - definida:  Valor numérico de la integral definida F(b) - F(a).
 * - area:      Valor numérico del área geométrica total ∫|f(x)| dx.
 * - puntos:    Array de valores y = f(x) calculados en Prolog para graficar.
 */
public class Resultado {
    private final String primitiva;
    private final double definida;
    private final double area;
    private final double[] puntos;

    public Resultado(String primitiva, double definida, double area, double[] puntos) {
        this.primitiva = primitiva;
        this.definida = definida;
        this.area = area;
        this.puntos = puntos;
    }

    public String getPrimitiva() {
        return primitiva;
    }

    public double getDefinida() {
        return definida;
    }

    public double getArea() {
        return area;
    }

    public double[] getPuntos() {
        return puntos;
    }
}
