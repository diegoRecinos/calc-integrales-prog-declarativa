import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Servicio de comunicación con el motor declarativo SWI-Prolog.
 *
 * Responsabilidad en la arquitectura:
 * - Aísla la invocación del proceso externo (swipl).
 * - Maneja el protocolo de comunicación por flujo de entrada/salida estándar (IPC / stdin-stdout).
 * - Parsea las respuestas del predicado puente de Prolog (OK, PLOT, ERROR).
 * - Maneja timeouts y control del ciclo de vida del subproceso Prolog.
 */
public class PrologService {

    private static final int TIMEOUT_SEGUNDOS = 20;

    /**
     * Detecta el ejecutable de SWI-Prolog en el entorno del sistema.
     * Si está configurada la variable de entorno SWIPL_EXE la utiliza;
     * de lo contrario, asume "swipl" disponible en el PATH del sistema.
     */
    public static String detectarEjecutableProlog() {
        String entorno = System.getenv("SWIPL_EXE");
        return (entorno != null && !entorno.trim().isEmpty()) ? entorno.trim() : "swipl";
    }

    /**
     * Consulta las reglas de integrales en SWI-Prolog enviando la función y los límites.
     *
     * @param swipl   Ruta o comando del ejecutable swipl.
     * @param archivo Ruta absoluta o relativa al archivo integrales.pl.
     * @param expr    Expresión matemática ingresada por el usuario (ej: 3*x^2+2*x+1).
     * @param a       Límite inferior de integración.
     * @param b       Límite superior de integración.
     * @return Objeto {@link Resultado} con la primitiva simbólica, integral definida, área y puntos evaluados.
     * @throws Exception Si ocurre un error de comunicación, sintaxis en la expresión o timeout.
     */
    public static Resultado consultar(String swipl, Path archivo, String expr, double a, double b)
            throws Exception {

        if (!Files.isRegularFile(archivo)) {
            throw new IOException("No se encontró el archivo de reglas Prolog: " + archivo);
        }

        ProcessBuilder pb = new ProcessBuilder(
                swipl,
                "-q",
                "-s", archivo.toString(),
                "-g", "puente",
                "-t", "halt"
        );
        pb.redirectErrorStream(true);

        Process proceso;
        try {
            proceso = pb.start();
        } catch (IOException ex) {
            throw new IOException(
                    "No se pudo iniciar SWI-Prolog. Comprueba su instalación y la ruta del ejecutable: " + swipl,
                    ex
            );
        }

        try {
            // Envío de parámetros por stdin: expresión, límite a, límite b
            try (Writer out = new OutputStreamWriter(proceso.getOutputStream(), StandardCharsets.UTF_8)) {
                out.write(expr + "\n" + Double.toString(a) + "\n" + Double.toString(b) + "\n");
            }

            // Espera con límite de tiempo para evitar bloqueos por bucles o procesos colgados
            if (!proceso.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)) {
                proceso.destroyForcibly();
                throw new IOException("SWI-Prolog tardó demasiado (" + TIMEOUT_SEGUNDOS + "s). Revisa el cálculo o la expresión.");
            }

            // Lectura de la respuesta enviada por Prolog en UTF-8
            String texto = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String[] lineas = texto.split("\\R");
            String[] datos = null;
            String dibujo = null;

            for (String linea : lineas) {
                if (linea.startsWith("ERROR\t")) {
                    throw new IllegalArgumentException(linea.substring(6));
                }
                if (linea.startsWith("OK\t")) {
                    datos = linea.split("\t", -1);
                }
                if (linea.startsWith("PLOT\t")) {
                    dibujo = linea.substring(5);
                }
            }

            if (datos == null || datos.length != 4 || dibujo == null) {
                throw new IOException("Respuesta Prolog incompleta. Detalle: " +
                        (texto.length() > 240 ? texto.substring(0, 240) : texto));
            }

            // Parseo de los puntos de la gráfica calculados numéricamente en Prolog
            String[] cadenas = dibujo.split(";");
            double[] puntos = new double[cadenas.length];
            for (int i = 0; i < cadenas.length; i++) {
                puntos[i] = Double.parseDouble(cadenas[i]);
            }

            String primitiva = datos[1];
            double definida = Double.parseDouble(datos[2]);
            double area = Double.parseDouble(datos[3]);

            return new Resultado(primitiva, definida, area, puntos);

        } finally {
            if (proceso.isAlive()) {
                proceso.destroyForcibly();
            }
        }
    }
}
