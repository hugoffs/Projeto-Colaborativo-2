package io;

import model.Graph;
import model.Transaction;

import java.io.*;
import java.nio.file.*;

/**
 * Desserializa um arquivo no formato Pajek ({@code .net}) e reconstrói
 * um {@link Graph}.
 *
 * <h2>Seções suportadas</h2>
 * <ul>
 * <li>{@code *Vertices N} — lê N vértices com label opcional</li>
 * <li>{@code *Arcs} — lê arestas direcionadas</li>
 * <li>{@code *Edges} — lê arestas não-direcionadas
 * (cada aresta gera dois {@link Graph#addEdge} simétricos)</li>
 * </ul>
 *
 * <h2>Formatos de label aceitos</h2>
 * <ol>
 * <li><strong>Nosso formato</strong>: {@code "txId|timeStep|LABEL"} —
 * produzido pelo {@link PajekWriter}; permite reconstrução fiel
 * do {@link Transaction}.</li>
 * <li><strong>Label genérico</strong>: qualquer string entre aspas —
 * nó criado com o índice Pajek como txId e label {@code UNKNOWN}.</li>
 * <li><strong>Sem label</strong>: linha só com o índice —
 * mesmo tratamento do caso anterior.</li>
 * </ol>
 *
 * <p>
 * Linhas inválidas ou não parseáveis são logadas em {@code stderr}
 * e ignoradas, garantindo robustez com arquivos externos.
 * </p>
 *
 * @see PajekWriter
 */
public class PajekReader {

    /** Estado da leitura sequencial do arquivo. */
    private enum Section {
        NONE, VERTICES, ARCS, EDGES
    }

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Lê um arquivo Pajek e retorna o grafo reconstruído.
     *
     * <p>
     * Internamente mantém um mapa {@code índice Pajek → txId}
     * construído durante a leitura dos vértices, necessário para
     * converter as referências numéricas das arestas de volta
     * para os {@code txId} reais.
     * </p>
     *
     * @param filePath caminho do arquivo {@code .net} a carregar
     * @return grafo reconstruído com nós e arestas
     * @throws IOException se o arquivo não existir ou não puder ser lido
     */
    public static Graph read(String filePath) throws IOException {
        Graph graph = new Graph();
        java.util.Map<Integer, Long> indexToTxId = new java.util.HashMap<>();

        Section currentSection = Section.NONE;
        boolean directed = true;

        try (BufferedReader br = Files.newBufferedReader(Path.of(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("%"))
                    continue;

                String lower = line.toLowerCase();

                // Detecta mudança de seção
                if (lower.startsWith("*vertices")) {
                    currentSection = Section.VERTICES;
                    continue;
                }
                if (lower.startsWith("*arcs")) {
                    currentSection = Section.ARCS;
                    directed = true;
                    continue;
                }
                if (lower.startsWith("*edges")) {
                    currentSection = Section.EDGES;
                    directed = false;
                    continue;
                }

                switch (currentSection) {
                    case VERTICES -> {
                        Transaction t = parseVertex(line);
                        if (t != null) {
                            indexToTxId.put(pajekIndex(line), t.getTxId());
                            graph.addNode(t);
                        }
                    }
                    case ARCS, EDGES -> {
                        long[] edge = parseEdge(line, indexToTxId);
                        if (edge != null) {
                            graph.addEdge(edge[0], edge[1]);
                            if (!directed)
                                graph.addEdge(edge[1], edge[0]);
                        }
                    }
                    default -> {
                        /* seção ainda não identificada */ }
                }
            }
        }

        System.out.printf("[PajekReader] Grafo carregado de '%s' (%,d vértices, %,d arcos).%n",
                filePath, graph.nodeCount(), graph.edgeCount());
        return graph;
    }

    // ── Parsers internos ──────────────────────────────────────────────────

    /**
     * Extrai o índice Pajek (primeiro token inteiro) de uma linha de vértice.
     *
     * @param line linha no formato {@code "42 \"label\""}
     * @return índice inteiro (1-based)
     */
    private static int pajekIndex(String line) {
        return Integer.parseInt(line.split("\\s+")[0]);
    }

    /**
     * Parseia uma linha de vértice e retorna a {@link Transaction} correspondente.
     *
     * <p>
     * Tenta decodificar o label no formato {@code "txId|timeStep|LABEL"}.
     * Se o label não seguir esse padrão, usa o índice Pajek como txId
     * e define label como {@link Transaction.Label#UNKNOWN}.
     * </p>
     *
     * @param line linha de vértice do arquivo Pajek
     * @return a transação criada, ou {@code null} se a linha for inválida
     */
    private static Transaction parseVertex(String line) {
        try {
            String[] tokens = line.split("\\s+", 2);
            int pajekIdx = Integer.parseInt(tokens[0]);

            if (tokens.length < 2) {
                return new Transaction(pajekIdx, 0, new double[0]);
            }

            String raw = tokens[1].trim();
            String label = (raw.startsWith("\"") && raw.endsWith("\""))
                    ? raw.substring(1, raw.length() - 1)
                    : raw;

            // Tenta decodificar formato "txId|timeStep|LABEL"
            if (label.contains("|")) {
                String[] parts = label.split("\\|");
                long txId = Long.parseLong(parts[0]);
                int timeStep = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                String cls = parts.length > 2 ? parts[2] : "UNKNOWN";

                Transaction t = new Transaction(txId, timeStep, new double[0]);
                t.setLabel(Transaction.Label.valueOf(cls));
                return t;
            }

            // Label genérico: usa índice Pajek como txId
            return new Transaction(pajekIdx, 0, new double[0]);

        } catch (Exception e) {
            System.err.println("[PajekReader] Linha de vértice inválida ignorada: " + line);
            return null;
        }
    }

    /**
     * Parseia uma linha de aresta e retorna o par {@code [txIdFrom, txIdTo]}.
     *
     * <p>
     * O peso (terceiro token, opcional) é lido mas descartado —
     * a estrutura atual de {@link Graph} não armazena pesos por aresta.
     * </p>
     *
     * @param line        linha no formato {@code "from to [peso]"}
     * @param indexToTxId mapa construído durante a leitura dos vértices
     * @return {@code long[]{txIdFrom, txIdTo}}, ou {@code null} se inválida
     */
    private static long[] parseEdge(String line,
            java.util.Map<Integer, Long> indexToTxId) {
        try {
            String[] tokens = line.split("\\s+");
            int fromIdx = Integer.parseInt(tokens[0]);
            int toIdx = Integer.parseInt(tokens[1]);

            Long from = indexToTxId.get(fromIdx);
            Long to = indexToTxId.get(toIdx);

            if (from == null || to == null) {
                System.err.printf("[PajekReader] Aresta (%d→%d) referencia índice inexistente.%n",
                        fromIdx, toIdx);
                return null;
            }
            return new long[] { from, to };

        } catch (Exception e) {
            System.err.println("[PajekReader] Linha de aresta inválida ignorada: " + line);
            return null;
        }
    }
}