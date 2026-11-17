# Sistema para resolver integrales — Proyecto final 02/2026

Base de conocimiento en SWI-Prolog, interfaz gráfica Java Swing, comunicación desacoplada Java–Prolog, cálculo de primitivas simbólicas, integral definida, área geométrica aproximada con cambio de signo, visualización gráfica de la curva y trapecios de Riemann, catálogo de pruebas y compatibilidad para Windows/Linux/macOS.

---

## 1. Estructura y Organización del Código

El proyecto separa estrictamente la capa de presentación, el modelo de datos y el servicio de comunicación con el motor declarativo:

```text
calc-integrales-prog-declarativa/
├── src/
│   ├── IntegralApp.java      # Vista principal (JFrame), captura de eventos y asincronía (SwingWorker)
│   ├── PlotPanel.java        # Componente gráfico (JPanel): dibuja la curva f(x) y el sombreado de áreas
│   ├── PrologService.java    # Capa de servicio: comunicación por subproceso (ProcessBuilder, IPC stdin/stdout)
│   ├── Resultado.java        # Modelo DTO: primitiva simbólica, integral definida, área y puntos
│   ├── Ejemplo.java          # Catálogo de funciones de prueba para la exposición
│   ├── META-INF/
│   │   └── MANIFEST.MF       # Declaración de Main-Class: IntegralApp para el .jar ejecutable
│   └── prolog/
│       └── integrales.pl     # Base de conocimiento: reglas de integración, evaluación y cálculo numérico
├── out/
│   └── artifacts/            # Contiene el .jar ejecutable generado
└── README.md
```

### Organización modular en `src/`
Las clases se estructuran de forma limpia y desacoplada dentro de la raíz de fuentes (`src/`) por diseño:
- **Resolución flexible de rutas:** Permite localizar el archivo de conocimiento mediante su ruta relativa estándar (`src/prolog/integrales.pl`) o mediante el selector de archivos integrado.
- **Detección por PATH o ruta directa:** Permite invocar `swipl` automáticamente si está registrado en las variables de entorno del sistema (`PATH`), o especificar la ruta ejecutable directa/relativa desde la propia interfaz gráfica.
- **Portabilidad inmediata:** Garantiza que el proyecto abra, compile y ejecute sin configuraciones adicionales ni dependencias externas en IntelliJ IDEA, Apache NetBeans, VS Code o terminal pura.

---

## 2. Preparación en Windows

### Paso 1. Instala Java JDK 11 o superior
Verifica la instalación en una terminal:
```powershell
java -version
javac -version
```
- `java` ejecuta la interfaz gráfica.
- `javac` permite compilar el código fuente.
- Si no se reconocen los comandos, añade la carpeta `bin` de tu JDK a la variable de entorno `PATH` y reinicia la terminal.

### Paso 2. Instala SWI-Prolog
Descarga SWI-Prolog desde su sitio oficial. Durante la instalación, habilita la opción de **agregar el ejecutable al PATH**. Para comprobarlo:
```powershell
swipl --version
```

> **Flexibilidad de ejecución:** Si `swipl` no está en el `PATH`, no hay problema: en el campo **"Ejecutable swipl"** de la interfaz puedes indicar directamente la ruta completa al binario (por ejemplo `C:\Program Files\swipl\bin\swipl.exe`) o una ruta relativa.

### Paso 3. Abre la interfaz Java
Puedes ejecutar la aplicación directamente desde la terminal en la carpeta del proyecto:
```powershell
java -jar out/artifacts/calc_integrales_prog_declarativa_jar/calc-integrales-prog-declarativa.jar
```
O bien compilar y ejecutar directamente desde los archivos fuente:
```powershell
javac -encoding UTF-8 -d bin src/*.java
java -cp bin IntegralApp
```

La interfaz cuenta con el panel **Conexión con Prolog**:
- **Ejecutable swipl:** `swipl` (si está en PATH) o la ruta completa al ejecutable.
- **Ruta al archivo .pl:** `src/prolog/integrales.pl` (ruta relativa por defecto). Puedes pulsar **"Examinar archivo Prolog..."** para seleccionarlo interactivamente.

### Paso 4. Haz el primer cálculo
En la aplicación, ingresa o selecciona un ejemplo:
```text
Función:  3*x^2+2*x+1
Límite a: 0
Límite b: 2
```
Haz clic en **"Calcular integral"**. La interfaz consultará a Prolog en segundo plano y mostrará la antiderivada, los valores numéricos y la gráfica sombreada.

---

## 3. Ejecutar Prolog por separado (parte declarativa)

Puedes probar directamente la base de conocimiento sin abrir Java. Desde la carpeta del proyecto:

```powershell
swipl -q -s src/prolog/integrales.pl
```

En la consola interactiva de SWI-Prolog:
```prolog
?- resolver("3*x^2+2*x+1", 0, 2, Primitiva, Definida, Area).
```
Prolog responderá unificando las variables:
- `Primitiva` con la expresión simbólica simplificada.
- `Definida` con el valor evaluado de $F(b) - F(a)$ (`14.0`).
- `Area` con el área geométrica total $\int |f(x)| dx$ (`14.0`).

Para salir de la consola de Prolog:
```prolog
?- halt.
```

Para consultar reglas simbólicas individuales:
```prolog
?- integrar(sin(x), F).
?- integrar(x^3, F).
?- integrar(2^x, F).
```

---

## 4. Código de Java en un IDE (IntelliJ, NetBeans o VS Code)

### Opción A: IntelliJ IDEA (Recomendado)
1. Abre la carpeta del proyecto en IntelliJ IDEA (`File → Open`).
2. IntelliJ detectará automáticamente el directorio `src/` como carpeta de fuentes.
3. Haz clic derecho sobre `IntegralApp.java` y selecciona **Run 'IntegralApp.main()'**.

### Opción B: Apache NetBeans
1. Crea un proyecto **Java with Ant → Java Application** (sin Maven ni dependencias externas).
2. Usa como clase principal `IntegralApp` (sin paquete).
3. Agrega los archivos de `src/*.java` a **Source Packages**.
4. Ejecuta con **Run Project**. Si el directorio de trabajo difiere, usa el botón **Examinar** en la interfaz para apuntar a `integrales.pl`.

### Opción C: Compilación manual por consola
```powershell
javac -encoding UTF-8 -d bin src/*.java
java -cp bin IntegralApp
```

---

## 5. Cómo usar la sintaxis matemática

| Lo que deseas | Cómo escribirlo |
|---|---|
| Constante 5 | `5` |
| Variable x | `x` |
| Tres por x | `3*x` |
| x al cubo | `x^3` |
| Polinomio | `3*x^2+2*x+1` |
| Resta de funciones | `x^2-x` |
| Seno | `sin(x)` |
| Tres veces seno | `3*sin(x)` |
| Seno de función afín | `sin(2*x+1)` |
| Exponencial de base 2 | `2^x` |
| Exponencial con exponente afín | `2^(3*x+1)` |
| Exponencial natural | `exp(x)` |
| Tangente | `tan(x)` |
| Seno hiperbólico | `sinh(x)` o `sh(x)` |
| Seno hiperbólico inverso | `arcsinh(x)` |
| Arcotangente hiperbólica | `arctanh(x)` |

**Reglas de escritura:**
- La variable independiente debe ser `x` en minúscula.
- El operador `*` es obligatorio para multiplicaciones (ej: `3*x` en lugar de `3x`).
- El operador `^` se usa para potencias.
- Las funciones trigonométricas e hiperbólicas operan en **radianes**.
- Los límites admiten tanto punto decimal como coma decimal (la interfaz normaliza automáticamente).

---

## 6. Cobertura de las Reglas de Integración

El sistema cubre ampliamente todas las familias matemáticas solicitadas en el enunciado:

| Inciso | Forma Matemática | Ejemplo Compatible |
|---|---|---|
| **a** | Constante $k$ | `5` |
| **b** | Variable $x$ | `x` |
| **c** | Constante por variable $k \cdot x$ | `3*x` |
| **d** | Potencia $x^n$ (entero $\neq -1$ y $-1$) | `x^3`, `1/x` |
| **e** | Función compuesta afín $u(x)$ | `sin(2*x)` |
| **f** | Constante por función $k \cdot u(x)$ | `3*sin(x)` |
| **g** | Suma de funciones $u(x) + v(x)$ | `x^2+sin(x)` |
| **h** | Resta de funciones $u(x) - v(x)$ | `x^2-x` |
| **i** | Exponencial $a^x$ | `2^x` |
| **j** | Exponencial compuesta $a^u$, con $u$ afín | `2^(3*x+1)` |
| **k** | Función seno $\sin(x)$ | `sin(x)` |
| **l** | Función coseno $\cos(x)$ | `cos(x)` |
| **m** | Función tangente $\tan(x)$ | `tan(x)` |
| **n** | Coeficiente por seno $a \cdot \sin(x)$ | `3*sin(x)` |
| **o** | Coeficiente por coseno $a \cdot \cos(x)$ | `2*cos(x)` |
| **p** | Coeficiente por tangente $a \cdot \tan(x)$ | `4*tan(x)` |
| **q** | Seno hiperbólico $\sinh(x)$ | `sinh(x)` o `sh(x)` |
| **r** | Coseno hiperbólico $\cosh(x)$ | `cosh(x)` |
| **s** | Tangente hiperbólica $\tanh(x)$ | `tanh(x)` |
| **t** | Arcoseno hiperbólico $\text{arcsinh}(x)$ | `arcsinh(x)` |
| **u** | Arcocoseno hiperbólico $\text{arccosh}(x)$ | `arccosh(x)` |
| **v** | Arcotangente hiperbólica $\text{arctanh}(x)$ | `arctanh(x)` |

---

## 7. Comunicación entre Java y SWI-Prolog

La aplicación implementa una arquitectura cliente-servidor desacoplada entre procesos:

```text
[ Usuario ingresa f(x), a, b ]
               │
               ▼
   [ IntegralApp: JFrame ] ──(SwingWorker en 2do plano)──► [ PrologService ]
                                                                   │
                                                   (ProcessBuilder crea subproceso)
                                                                   ▼
                                                   swipl -q -s integrales.pl -g puente
                                                                   │
                                                        stdin: expr \n a \n b
                                                                   ▼
                                                   [ Prolog: parsea y valida ]
                                                                   │
                                                   [ Prolog: aplica reglas integrar/2 ]
                                                                   │
                                                   [ Prolog: evalúa F(b)-F(a), área y puntos ]
                                                                   ▼
                                                        stdout: OK \t PLOT \t ERROR
                                                                   │
   [ IntegralApp actualiza etiquetas ] ◄───────────────────────────┘
   [ PlotPanel renderiza curva y trapecios ]
```

