package io;

import model.Graph;
import model.Transaction;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Lê os 3 CSVs filtrados do dataset Elliptic e monta o grafo.
 *
 * Arquivos esperados:
 *   filtered_features.csv  → txId, timeStep, f0 ... f164   (com header)
 *   filtered_edges.csv     → txId1, txId2                   (com header)
 *   filtered_classes.csv   → txId, class                    (com header)
 */
public class EllipticLoader {

    private static final int FEATURE_COUNT = 165; // colunas f0..f164

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

    // 1. Lê classes
    private static Map<Long, Transaction.Label> readClasses(String path) throws IOException {
        Map<Long, Transaction.Label> map = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(Path.of(path))) {
            String header = br.readLine(); // pula header: txId,class
            if (header == null) throw new IOException("filtered_classes.csv está vazio.");

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",", 2);
                long  txId  = Long.parseLong(parts[0].trim());
                Transaction.Label label = Transaction.Label.fromString(parts[1]);
                map.put(txId, label);
            }
        }
        System.out.printf("  → %,d labels lidos.%n", map.size());
        return map;
    }

    // 2. Lê features → cria Transaction → adiciona ao grafo
    private static Graph readFeatures(String path,
                                      Map<Long, Transaction.Label> labelMap) throws IOException {
        Graph graph = new Graph();
        int count = 0;

        try (BufferedReader br = Files.newBufferedReader(Path.of(path))) {
            String header = br.readLine(); // pula header: txId,timeStep,f0,...,f164
            if (header == null) throw new IOException("filtered_features.csv está vazio.");

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] parts = line.split(",");
                // partes: [0]=txId  [1]=timeStep  [2..166]=features
                long   txId     = Long.parseLong(parts[0].trim());
                int    timeStep = (int) Double.parseDouble(parts[1].trim()); // pode vir como float no CSV

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

    // 3. Lê arestas e adiciona ao grafo
    private static int readEdges(String path, Graph graph) throws IOException {
        int loaded  = 0;
        int skipped = 0;

        try (BufferedReader br = Files.newBufferedReader(Path.of(path))) {
            String header = br.readLine(); // pula header: txId1,txId2
            if (header == null) throw new IOException("filtered_edges.csv está vazio.");

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] parts = line.split(",", 2);
                long from = Long.parseLong(parts[0].trim());
                long to   = Long.parseLong(parts[1].trim());

                if (graph.hasNode(from) && graph.hasNode(to)) {
                    graph.addEdge(from, to);
                    loaded++;
                } else {
                    skipped++; // aresta com nó fora do filtro temporal
                }
            }
        }
        if (skipped > 0)
            System.out.printf("  → %,d arestas ignoradas (nós fora do filtro).%n", skipped);
        return loaded;
    }
}