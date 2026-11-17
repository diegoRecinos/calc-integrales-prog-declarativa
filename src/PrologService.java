import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
     * 1. Variable de entorno SWIPL_EXE si está configurada.
     * 2. Comprueba si "swipl" responde en el PATH del sistema.
     * 3. En Windows, comprueba rutas habituales de instalación:
     *    C:\Program Files\swipl\bin\swipl.exe
     *    C:\Program Files (x86)\swipl\bin\swipl.exe
     * 4. De lo contrario, asume "swipl".
     */
    public static String detectarEjecutableProlog() {
        String entorno = System.getenv("SWIPL_EXE");
        if (entorno != null && !entorno.trim().isEmpty()) {
            return entorno.trim();
        }

        // Comprobar si "swipl" responde en el PATH
        try {
            Process test = new ProcessBuilder("swipl", "--version").start();
            if (test.waitFor(1, TimeUnit.SECONDS) && test.exitValue() == 0) {
                return "swipl";
            }
        } catch (Exception ignored) {
        }

        // Rutas habituales en Windows si no se marcó "Add to PATH"
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String[] rutasWin = {
                    "C:\\Program Files\\swipl\\bin\\swipl.exe",
                    "C:\\Program Files (x86)\\swipl\\bin\\swipl.exe",
                    System.getenv("LOCALAPPDATA") != null
                            ? System.getenv("LOCALAPPDATA") + "\\Programs\\swipl\\bin\\swipl.exe"
                            : null
            };
            for (String ruta : rutasWin) {
                if (ruta != null && Files.isRegularFile(Paths.get(ruta))) {
                    return ruta;
                }
            }
        }

        return "swipl";
    }

    /**
     * Localiza de manera inteligente el archivo de reglas integrales.pl.
     * Busca en cascada:
     * 1. Ruta relativa estándar en desarrollo: "src/prolog/integrales.pl"
     * 2. Ruta relativa en ejecución: "prolog/integrales.pl"
     * 3. Junto al archivo JAR o directorio de ejecución.
     * 4. Fallback: Si se ejecuta desde un .jar independiente,
     *    extrae integrales.pl de los recursos internos a un archivo temporal.
     */
    public static String detectarArchivoProlog() {
        // 1. src/prolog/integrales.pl
        Path pSrc = Paths.get("src", "prolog", "integrales.pl");
        if (Files.isRegularFile(pSrc)) {
            return pSrc.toString();
        }

        // 2. prolog/integrales.pl
        Path pProlog = Paths.get("prolog", "integrales.pl");
        if (Files.isRegularFile(pProlog)) {
            return pProlog.toString();
        }

        // 3. integrales.pl en la raíz actual
        Path pRaiz = Paths.get("integrales.pl");
        if (Files.isRegularFile(pRaiz)) {
            return pRaiz.toString();
        }

        // 4. Junto al JAR o directorio de clases
        try {
            var codeSource = PrologService.class.getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null) {
                Path baseDir = Paths.get(codeSource.getLocation().toURI()).getParent();
                if (baseDir != null) {
                    Path p1 = baseDir.resolve("src").resolve("prolog").resolve("integrales.pl");
                    if (Files.isRegularFile(p1)) return p1.toString();
                    Path p2 = baseDir.resolve("prolog").resolve("integrales.pl");
                    if (Files.isRegularFile(p2)) return p2.toString();
                    Path p3 = baseDir.resolve("integrales.pl");
                    if (Files.isRegularFile(p3)) return p3.toString();
                }
            }
        } catch (Exception ignored) {
        }

        // 5. Fallback: extraer del interior del JAR
        try (InputStream in = PrologService.class.getResourceAsStream("/prolog/integrales.pl")) {
            if (in != null) {
                Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "calc_integrales");
                Files.createDirectories(tempDir);
                Path tempFile = tempDir.resolve("integrales.pl");
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                tempFile.toFile().deleteOnExit();
                return tempFile.toAbsolutePath().toString();
            }
        } catch (Exception ignored) {
        }

        return "src/prolog/integrales.pl";
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
