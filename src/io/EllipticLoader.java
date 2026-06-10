package io;

import model.Graph;
import model.Transaction;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Carrega o dataset Elliptic a partir dos 3 CSVs filtrados e constrói
 * o {@link Graph} de transações Bitcoin.
 *
 * <h2>Arquivos esperados</h2>
 * <ul>
 * <li>{@code filtered_features.csv} — com header
 * {@code txId,timeStep,f0,...,f164}</li>
 * <li>{@code filtered_edges.csv} — com header {@code txId1,txId2}</li>
 * <li>{@code filtered_classes.csv} — com header {@code txId,class}</li>
 * </ul>
 *
 * <h2>Ordem de carregamento</h2>
 * <ol>
 * <li>Classes são lidas primeiro e armazenadas num mapa temporário.</li>
 * <li>Features criam os nós já com o label correto aplicado.</li>
 * <li>Arestas são adicionadas ao grafo já populado.</li>
 * </ol>
 *
 * <p>
 * Arestas cujos nós não estejam no grafo são ignoradas — situação
 * esperada quando o dataset é filtrado por time step e algumas arestas
 * cruzam a fronteira do filtro.
 * </p>
 *
 * @see model.Graph
 * @see model.Transaction
 */
public class EllipticLoader {

    /** Número de features por transação no dataset Elliptic. */
    private static final int FEATURE_COUNT = 165;

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Ponto de entrada principal. Orquestra as três etapas de carregamento
     * e retorna o grafo pronto para uso.
     *
     * @param featuresPath caminho para {@code filtered_features.csv}
     * @param edgesPath    caminho para {@code filtered_edges.csv}
     * @param classesPath  caminho para {@code filtered_classes.csv}
     * @return grafo com nós e arestas carregados
     * @throws IOException se algum arquivo não for encontrado ou estiver corrompido
     */
    public static Graph load(String featuresPath,
            String edgesPath,
            String classesPath) throws IOException {

        System.out.println("[Loader] Lendo classes...");
        Map<Long, Transaction.Label> labelMap = readClasses(classesPath);

        System.out.println("[Loader] Lendo features e construindo nós...");
        Graph graph = readFeatures(featuresPath, labelMap);

        System.out.println("[Loader] Lendo arestas...");
        int loaded = readEdges(edgesPath, graph);

        System.out.printf("[Loader] Concluído: %,d nós, %,d arestas carregados.%n",
                graph.nodeCount(), loaded);
        return graph;
    }

    // ── Etapas internas ───────────────────────────────────────────────────

    /**
     * Lê o arquivo de classes e constrói um mapa {@code txId → Label}.
     *
     * <p>
     * Este mapa é passado para {@link #readFeatures} para que cada
     * {@link Transaction} já nasça com o label correto, evitando uma
     * segunda iteração sobre os nós.
     * </p>
     *
     * @param path caminho para {@code filtered_classes.csv}
     * @return mapa de rótulos; nunca {@code null}
     * @throws IOException se o arquivo não existir ou estiver vazio
     */
    private static Map<Long, Transaction.Label> readClasses(String path) throws IOException {
        Map<Long, Transaction.Label> map = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(Path.of(path))) {
            String header = br.readLine();
            if (header == null)
                throw new IOException("filtered_classes.csv está vazio.");

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank())
                    continue;
                String[] parts = line.split(",", 2);
                long txId = Long.parseLong(parts[0].trim());
                map.put(txId, Transaction.Label.fromString(parts[1]));
            }
        }
        System.out.printf("  → %,d labels lidos.%n", map.size());
        return map;
    }

    /**
     * Lê o arquivo de features, cria um {@link Transaction} por linha
     * e o adiciona ao grafo.
     *
     * <p>
     * O parsing é tolerante: se uma linha tiver menos colunas que
     * {@link #FEATURE_COUNT}, as features ausentes ficam em zero.
     * O timeStep pode vir como float no CSV (ex: {@code 3.0}) e é
     * truncado para int.
     * </p>
     *
     * @param path     caminho para {@code filtered_features.csv}
     * @param labelMap mapa {@code txId → Label} gerado por {@link #readClasses}
     * @return grafo com todos os nós inseridos, sem arestas ainda
     * @throws IOException se o arquivo não existir ou estiver vazio
     */
    private static Graph readFeatures(String path,
            Map<Long, Transaction.Label> labelMap) throws IOException {
        Graph graph = new Graph();
        int count = 0;

        try (BufferedReader br = Files.newBufferedReader(Path.of(path))) {
            String header = br.readLine();
            if (header == null)
                throw new IOException("filtered_features.csv está vazio.");

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank())
                    continue;

                String[] parts = line.split(",");
                long txId = Long.parseLong(parts[0].trim());
                int timeStep = (int) Double.parseDouble(parts[1].trim());

                double[] features = new double[FEATURE_COUNT];
                int limit = Math.min(parts.length - 2, FEATURE_COUNT);
                for (int i = 0; i < limit; i++) {
                    String val = parts[i + 2].trim();
                    features[i] = val.isEmpty() ? 0.0 : Double.parseDouble(val);
                }

                Transaction t = new Transaction(txId, timeStep, features);
                t.setLabel(labelMap.getOrDefault(txId, Transaction.Label.UNKNOWN));
                graph.addNode(t);
                count++;
            }
        }
        System.out.printf("  → %,d nós criados.%n", count);
        return graph;
    }

    /**
     * Lê o arquivo de arestas e as adiciona ao grafo já populado com nós.
     *
     * <p>
     * Arestas cujo {@code txId1} ou {@code txId2} não estejam presentes
     * no grafo são ignoradas — isso ocorre quando a aresta cruza a fronteira
     * do filtro temporal aplicado no pré-processamento Python.
     * </p>
     *
     * @param path  caminho para {@code filtered_edges.csv}
     * @param graph grafo já populado com {@link Transaction}s
     * @return número de arestas efetivamente adicionadas
     * @throws IOException se o arquivo não existir ou estiver vazio
     */
    private static int readEdges(String path, Graph graph) throws IOException {
        int loaded = 0;
        int skipped = 0;

        try (BufferedReader br = Files.newBufferedReader(Path.of(path))) {
            String header = br.readLine();
            if (header == null)
                throw new IOException("filtered_edges.csv está vazio.");

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank())
                    continue;
                String[] parts = line.split(",", 2);
                long from = Long.parseLong(parts[0].trim());
                long to = Long.parseLong(parts[1].trim());

                if (graph.hasNode(from) && graph.hasNode(to)) {
                    graph.addEdge(from, to);
                    loaded++;
                } else {
                    skipped++;
                }
            }
        }
        if (skipped > 0)
            System.out.printf("  → %,d arestas ignoradas (nós fora do filtro).%n", skipped);
        return loaded;
    }
}