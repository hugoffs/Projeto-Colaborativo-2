package io;

import model.Graph;
import model.Transaction;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Serializa um {@link Graph} no formato Pajek ({@code .net}).
 *
 * <h2>Formato gerado</h2>
 * 
 * <pre>
 * *Vertices 19117
 * 1 "230425980|3|ILLICIT"
 * 2 "5530458|3|LICIT"
 * ...
 * *Arcs
 * 1 2 1.0
 * 1 7 1.0
 * </pre>
 *
 * <h2>Encoding do label</h2>
 * <p>
 * O label de cada vértice segue o padrão
 * {@code "txId|timeStep|LABEL"}, que permite ao {@link PajekReader}
 * reconstruir o {@link Transaction} completo (exceto o vetor de features)
 * sem acesso aos CSVs originais.
 * </p>
 *
 * <h2>Índices Pajek</h2>
 * <p>
 * O formato Pajek exige índices inteiros sequenciais a partir de 1.
 * Como os {@code txId} do dataset são Longs de 9 dígitos, este writer
 * cria e mantém internamente um mapeamento {@code txId → índice Pajek}.
 * </p>
 *
 * @see PajekReader
 */
public class PajekWriter {

    /**
     * Grava o grafo em um arquivo no formato Pajek.
     *
     * <p>
     * O processo ocorre em duas fases:
     * </p>
     * <ol>
     * <li>Criação do mapa {@code txId → índice Pajek} (1-based)
     * para todos os nós.</li>
     * <li>Escrita sequencial: cabeçalho {@code *Vertices}, um vértice
     * por linha, {@code *Arcs} e uma aresta por linha.</li>
     * </ol>
     *
     * <p>
     * As arestas usam peso fixo {@code 1.0}, pois o dataset Elliptic
     * não fornece pesos explícitos no edgelist.
     * </p>
     *
     * @param graph    grafo a serializar
     * @param filePath caminho do arquivo de saída (ex: {@code "data/elliptic.net"})
     * @throws IOException se não for possível criar ou escrever no arquivo
     */
    public static void write(Graph graph, String filePath) throws IOException {

        // ── 1. Mapeamento txId → índice Pajek (1-based) ───────────────────
        Map<Long, Integer> indexMap = new HashMap<>();
        int idx = 1;
        for (Transaction t : graph.getNodes()) {
            indexMap.put(t.getTxId(), idx++);
        }

        // ── 2. Escrita do arquivo ─────────────────────────────────────────
        try (BufferedWriter bw = Files.newBufferedWriter(Path.of(filePath))) {

            // Cabeçalho de vértices
            bw.write("*Vertices " + graph.nodeCount());
            bw.newLine();

            // Ordena por índice Pajek para saída determinística
            List<Transaction> sorted = new ArrayList<>(graph.getNodes());
            sorted.sort(Comparator.comparingInt(t -> indexMap.get(t.getTxId())));

            for (Transaction t : sorted) {
                int pajekIdx = indexMap.get(t.getTxId());
                // Label codifica txId, timeStep e classe para reconstrução fiel
                String label = t.getTxId() + "|" + t.getTimeStep() + "|" + t.getLabel().name();
                bw.write(pajekIdx + " \"" + label + "\"");
                bw.newLine();
            }

            // Seção de arcos direcionados
            bw.write("*Arcs");
            bw.newLine();

            for (long[] edge : graph.getEdgeList()) {
                int fromIdx = indexMap.get(edge[0]);
                int toIdx = indexMap.get(edge[1]);
                bw.write(fromIdx + " " + toIdx + " 1.0");
                bw.newLine();
            }
        }

        System.out.printf("[PajekWriter] Grafo salvo em '%s' (%,d vértices, %,d arcos).%n",
                filePath, graph.nodeCount(), graph.edgeCount());
    }
}