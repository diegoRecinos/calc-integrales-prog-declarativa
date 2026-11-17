import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Modelo para los casos de prueba y ejemplos demostrativos de la calculadora.
 * Permite cargar rápidamente funciones representativas para la exposición oral.
 */
public class Ejemplo {
    private final String titulo;
    private final String funcion;
    private final String a;
    private final String b;

    public Ejemplo(String titulo, String funcion, String a, String b) {
        this.titulo = titulo;
        this.funcion = funcion;
        this.a = a;
        this.b = b;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getFuncion() {
        return funcion;
    }

    public String getA() {
        return a;
    }

    public String getB() {
        return b;
    }

    @Override
    public String toString() {
        return titulo;
    }

    /**
     * Devuelve la lista de ejemplos predefinidos ordenados por familia matemática.
     */
    public static List<Ejemplo> obtenerEjemplos() {
        List<Ejemplo> lista = new ArrayList<>();
        lista.add(new Ejemplo("— Selecciona un ejemplo —", null, null, null));
        lista.add(new Ejemplo("Constante", "5", "0", "3"));
        lista.add(new Ejemplo("Variable x", "x", "0", "2"));
        lista.add(new Ejemplo("Constante por x", "3*x", "0", "2"));
        lista.add(new Ejemplo("Potencia entera", "x^3", "0", "2"));
        lista.add(new Ejemplo("Potencia negativa", "1/x", "1", "2"));
        lista.add(new Ejemplo("Suma: polinomio", "3*x^2+2*x+1", "0", "2"));
        lista.add(new Ejemplo("Resta", "x^2-x", "0", "1"));
        lista.add(new Ejemplo("Seno de una función", "sin(2*x)", "0", "1.5707963268"));
        lista.add(new Ejemplo("Coeficiente por seno", "3*sin(x)", "0", "1.5707963268"));
        lista.add(new Ejemplo("Exponencial base 2", "2^x", "0", "2"));
        lista.add(new Ejemplo("Exponencial de u", "2^(2*x+1)", "0", "1"));
        lista.add(new Ejemplo("Seno", "sin(x)", "0", "1.5707963268"));
        lista.add(new Ejemplo("Coseno", "cos(x)", "0", "1.5707963268"));
        lista.add(new Ejemplo("Tangente", "tan(x)", "0", "0.7853981634"));
        lista.add(new Ejemplo("Seno hiperbólico", "sinh(x)", "0", "1"));
        lista.add(new Ejemplo("Coseno hiperbólico", "cosh(x)", "0", "1"));
        lista.add(new Ejemplo("Tangente hiperbólica", "tanh(x)", "0", "1"));
        lista.add(new Ejemplo("Arcoseno hiperbólico", "arcsinh(x)", "0", "1"));
        lista.add(new Ejemplo("Arcocoseno hiperbólico", "arccosh(x)", "1", "2"));
        lista.add(new Ejemplo("Arcotangente hiperbólica", "arctanh(x)", "0", "0.5"));
        lista.add(new Ejemplo("Área con cambio de signo", "x", "-1", "1"));
        return Collections.unmodifiableList(lista);
    }
}
