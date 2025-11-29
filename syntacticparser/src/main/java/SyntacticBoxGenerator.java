import java.io.UnsupportedEncodingException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntacticBoxGenerator {

    // =========================================================================
    // 1. CLASE INTERNA: Constituent (Representa una 'Caja' o Nodo)
    // =========================================================================
    static class Constituent {
        String label;
        String word;
        List<Constituent> children = new ArrayList<>();
        int startWordIndex = -1;
        int endWordIndex = -1;
        int depth = 0;

        public Constituent(String label, String word) {
            this.label = label;
            this.word = word;
        }

        public int getWidth() {
            return (endWordIndex - startWordIndex) + 1;
        }

        public void calculateSpansAndDepth(int currentDepth) {
            this.depth = currentDepth;

            int minStart = Integer.MAX_VALUE;
            int maxEnd = Integer.MIN_VALUE;

            if (children.isEmpty() && word != null) {
                return;
            }


            for (Constituent child : children) {
                child.calculateSpansAndDepth(currentDepth + 1);

                if (child.startWordIndex != -1) {
                    minStart = Math.min(minStart, child.startWordIndex);
                    maxEnd = Math.max(maxEnd, child.endWordIndex);
                }
            }

            this.startWordIndex = minStart;
            this.endWordIndex = maxEnd;
        }
    }

    // =========================================================================
    // 2. CLASE INTERNA: Parser (Analiza la Notación de Corchetes)
    // =========================================================================
    static class Parser {
        private final Pattern tokenizer = Pattern.compile("\\(|\\)|[^\\s\\(\\)]+");
        private List<String> tokens;
        private int currentTokenIndex;

        public Constituent parse(String input) {
            tokens = new ArrayList<>();
            Matcher matcher = tokenizer.matcher(input);
            while (matcher.find()) {
                tokens.add(matcher.group());
            }

            currentTokenIndex = 0;
            if (tokens.isEmpty() || !tokens.get(0).equals("(")) {
                throw new IllegalArgumentException("La cadena de entrada debe comenzar con '('");
            }

            return parseConstituent();
        }

        private Constituent parseConstituent() {
            if (!tokens.get(currentTokenIndex).equals("(")) {
                throw new IllegalStateException("Esperaba '(', encontrado: " + tokens.get(currentTokenIndex));
            }
            currentTokenIndex++;

            String label = tokens.get(currentTokenIndex++);
            Constituent node = new Constituent(label, null);

            while (currentTokenIndex < tokens.size() && !tokens.get(currentTokenIndex).equals(")")) {
                if (tokens.get(currentTokenIndex).equals("(")) {
                    node.children.add(parseConstituent());
                } else {
                    String word = tokens.get(currentTokenIndex++);
                    node.children.add(new Constituent(label, word));
                }
            }

            if (!tokens.get(currentTokenIndex).equals(")")) {
                throw new IllegalStateException("Esperaba ')', encontrado el final del archivo");
            }
            currentTokenIndex++;
            return node;
        }
    }

    // =========================================================================
    // 3. CLASE INTERNA: HtmlGenerator (Genera el Output HTML)
    // =========================================================================
    static class HtmlGenerator {

        // LISTA HARDCODEADA: Categorías que se dibujan POR ENCIMA de la fila de palabras.
        private static final List<String> HIGH_LEVEL_TAGS = List.of("SN_Sujeto", "SV_Predicado");

        private List<String> sentenceWords = new ArrayList<>();
        private List<Constituent> highLevelConstituents = new ArrayList<>();
        private List<List<Constituent>> lowLevelConstituentsByDepth = new ArrayList<>();
        private int maxLowLevelDepth = 0;

        public String generateHtml(Constituent root) {

            collectWords(root, 0);
            root.calculateSpansAndDepth(0);

            separateConstituents(root);

            // 3. Generar HTML
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><title>Análisis Sintáctico</title>");
            html.append("<meta charset=\"UTF-8\">");
            html.append("<style>");
            html.append(".syn-table { border-collapse: collapse; width: auto; table-layout: auto; margin: 0 auto; }");

            // Celdas base: con namespace
            html.append(".syn-table td { border: none; text-align: center; padding: 2px 8px 0 8px; font-family: sans-serif; height: 25px; white-space: nowrap; min-width: 40px; }");

            // Fila de palabras: con namespace
            html.append(".syn-table .syn-word-row td { border: none; font-weight: bold; height: auto; padding-bottom: 2px; min-width: 40px; }");

            // Ajuste de altura para alto nivel: con namespace
            html.append(".syn-table .syn-high-level-row td { height: 30px; padding-top: 2px; padding-bottom: 2px; min-width: 40px; }");

            // Estilo de Línea ALTO NIVEL - con namespace
            html.append(".syn-table .syn-high-level-line {");
            html.append("  border-top: 2px solid black;");
            html.append("  position: relative;");
            html.append("  display: block;");
            html.append("  height: 100%;");
            html.append("  min-width: 60px;");
            html.append("  border-top-left-radius: 15px;");
            html.append("  border-top-right-radius: 15px;");
            html.append("}");

            // Estilo de Línea BAJO NIVEL - con namespace
            html.append(".syn-table .syn-low-level-line {");
            html.append("  border-bottom: 2px solid black;");
            html.append("  position: relative;");
            html.append("  display: block;");
            html.append("  height: 100%;");
            html.append("  min-width: 60px;");
            html.append("  border-bottom-left-radius: 15px;");
            html.append("  border-bottom-right-radius: 15px;");
            html.append("}");

            // Estilos de etiqueta - con namespace
            html.append(".syn-table .syn-low-label { ");
            html.append("  position: absolute; ");
            html.append("  bottom: 0; ");
            html.append("  left: 50%; ");
            html.append("  transform: translate(-50%, 100%); ");
            html.append("  font-size: 0.7em; font-weight: bold; color: #004d40; background-color: white; padding: 1px 6px; ");
            html.append("  white-space: nowrap;");
            html.append("  min-width: 30px;");
            html.append("}");

            html.append(".syn-table .syn-high-label { ");
            html.append("  position: absolute; ");
            html.append("  top: 0; ");
            html.append("  left: 50%; ");
            html.append("  transform: translate(-50%, -100%); ");
            html.append("  font-size: 0.7em; font-weight: bold; color: #004d40; background-color: white; padding: 1px 6px; ");
            html.append("  white-space: nowrap;");
            html.append("  min-width: 30px;");
            html.append("}");

            html.append(".syn-table .syn-empty-cell { border: none; }");
            html.append("</style>");
            html.append("</head><body>");
            html.append("<table class='syn-table'>"); // Agregar clase a la tabla

            // --- 4. RENDERIZADO DE ALTO NIVEL (POR ENCIMA DE LA PALABRA) ---
            renderHighLevel(html);

            // --- 5. RENDERIZADO DE LA FILA DE PALABRAS ---
            html.append("<tr class='syn-word-row'>"); // Namespace agregado
            for (String word : sentenceWords) {
                html.append("<td>").append(word).append("</td>");
            }
            html.append("</tr>");

            // --- 6. RENDERIZADO DE BAJO NIVEL (POR DEBAJO DE LA PALABRA) ---
            renderLowLevel(html);

            html.append("</table></body></html>");
            return html.toString();
        }

        private void renderHighLevel(StringBuilder html) {
            if (highLevelConstituents.isEmpty()) return;

            highLevelConstituents.sort(Comparator.comparingInt(c -> c.depth));

            List<List<Constituent>> highLevelByDepth = new ArrayList<>();
            int lastDepth = -1;

            for (Constituent constr : highLevelConstituents) {
                if (constr.depth > lastDepth) {
                    highLevelByDepth.add(new ArrayList<>());
                    lastDepth = constr.depth;
                }
                highLevelByDepth.get(highLevelByDepth.size() - 1).add(constr);
            }

            for (List<Constituent> rowConstituents : highLevelByDepth) {
                html.append("<tr class='syn-high-level-row'>"); // Namespace agregado

                int currentWordIndex = 0;
                rowConstituents.sort(Comparator.comparingInt(c -> c.startWordIndex));

                for (Constituent constr : rowConstituents) {
                    if (constr.startWordIndex > currentWordIndex) {
                        int colspan = constr.startWordIndex - currentWordIndex;
                        html.append("<td colspan='").append(colspan).append("' class='syn-empty-cell'></td>"); // Namespace
                    }

                    html.append("<td colspan='").append(constr.getWidth()).append("'>");
                    html.append("<div class='syn-high-level-line'>"); // Namespace
                    html.append("<span class='syn-high-label'>").append(constr.label).append("</span>"); // Namespace
                    html.append("</div>");
                    html.append("</td>");

                    currentWordIndex = constr.endWordIndex + 1;
                }

                int remaining = sentenceWords.size() - currentWordIndex;
                if (remaining > 0) {
                    html.append("<td colspan='").append(remaining).append("' class='syn-empty-cell'></td>"); // Namespace
                }
                html.append("</tr>");
            }
        }

        private void renderLowLevel(StringBuilder html) {
            for (int d = maxLowLevelDepth; d >= 1; d--) {
                html.append("<tr>");

                List<Constituent> currentDepthConstituents = lowLevelConstituentsByDepth.get(d - 1);
                currentDepthConstituents.sort(Comparator.comparingInt(c -> c.startWordIndex));

                int currentWordIndex = 0;

                for (Constituent constr : currentDepthConstituents) {
                    if (constr.startWordIndex > currentWordIndex) {
                        int colspan = constr.startWordIndex - currentWordIndex;
                        html.append("<td colspan='").append(colspan).append("' class='syn-empty-cell'></td>"); // Namespace
                    }

                    html.append("<td colspan='").append(constr.getWidth()).append("'>");
                    html.append("<div class='syn-low-level-line'>"); // Namespace
                    html.append("<span class='syn-low-label'>").append(constr.label).append("</span>"); // Namespace
                    html.append("</div>");
                    html.append("</td>");

                    currentWordIndex = constr.endWordIndex + 1;
                }

                int remaining = sentenceWords.size() - currentWordIndex;
                if (remaining > 0) {
                    html.append("<td colspan='").append(remaining).append("' class='syn-empty-cell'></td>"); // Namespace
                }
                html.append("</tr>");
            }
        }

        // Recorrido In-Order para recolectar las palabras terminales
        private void collectWords(Constituent node, int currentWordCount) {
            if (node.word != null) {
                node.startWordIndex = currentWordCount;
                node.endWordIndex = currentWordCount;
                sentenceWords.add(node.word);
                return;
            }
            for (Constituent child : node.children) {
                if (child.word != null) {
                    child.startWordIndex = sentenceWords.size();
                    child.endWordIndex = sentenceWords.size();
                    sentenceWords.add(child.word);
                } else {
                    collectWords(child, sentenceWords.size());
                }
            }
        }

        // Separa los constituyentes en listas de alto y bajo nivel
// En el método separateConstituents, cambia esta parte:
        private void separateConstituents(Constituent node) {
            if (!node.children.isEmpty()) {

                if (HIGH_LEVEL_TAGS.contains(node.label)) {
                    highLevelConstituents.add(node);
                } else {
                    // SOLUCIÓN: Solo agregar a lowLevelConstituentsByDepth si depth > 0
                    if (node.depth > 0) {
                        if (node.depth > maxLowLevelDepth) {
                            maxLowLevelDepth = node.depth;
                        }
                        while (lowLevelConstituentsByDepth.size() < node.depth) {
                            lowLevelConstituentsByDepth.add(new ArrayList<>());
                        }
                        lowLevelConstituentsByDepth.get(node.depth - 1).add(node);
                    }
                }
            }

            for (Constituent child : node.children) {
                if (child.word == null) {
                    separateConstituents(child);
                }
            }
        }
    }

    // =========================================================================
    // 4. MAIN METHOD (Punto de entrada)
    // =========================================================================
    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.err.println("No se pudo establecer la codificación UTF-8 para la salida.");
        }

        // Estructura del árbol sintáctico de la oración: "Los jóvenes estudiantes leen rápidamente el libro"
        String treeInput2 =
                "(O\n" +
                        "    (SN_Sujeto\n" +
                        "        (Det Los)\n" +
                        "        (Adj_Ady jóvenes)\n" +
                        "        (N_Núcleo estudiantes)\n" +
                        "    )\n" +
                        "    (SV_Predicado\n" +
                        "        (V_Núcleo leen)\n" +
                        "        (Adv_CCM rápidamente)\n" +
                        "        (SN_CD\n" +
                        "            (Det el)\n" +
                        "            (N_Núcleo libro)\n" +
                        "        )\n" +
                        "    )\n" +
                        ")";

        // Estructura del árbol sintáctico de la oración: "Los jóvenes estudiantes leen rápidamente el libro"
        String treeInput =
                "(FN\n" +
                        "        (Atributo doː_tɪɦɑːiː)\n" +
                        "        (Núcleo ʋɪdjɑːɾtʰiː)\n" +
                  ")";



        try {
            Parser parser = new Parser();
            Constituent root = parser.parse(treeInput);

            HtmlGenerator generator = new HtmlGenerator();
            String htmlOutput = generator.generateHtml(root);

            System.out.println(htmlOutput);
            System.out.println("\n------------------------------------------------------------");
            System.out.println("El código HTML ha sido generado con ESPACIOS REDUCIDOS:");
            System.out.println("  - Alturas de celdas reducidas de 40px a 30px");
            System.out.println("  - Padding vertical reducido significativamente");
            System.out.println("  - Tamaño de fuente de etiquetas reducido");
            System.out.println("  - Radio de curvas reducido de 20px a 15px");

        } catch (Exception e) {
            System.err.println("Error al procesar el árbol sintáctico: " + e.getMessage());
            e.printStackTrace();
        }
    }
}