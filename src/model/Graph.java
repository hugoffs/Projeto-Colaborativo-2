package model;

import java.util.*;

/**
 * Grafo direcionado e ponderado usando lista de adj.
 * No = transacoes bitcoin. Arestas = fluxo entre transacoes
 */
public class Graph {

    // -- Estrutura principal
    private final Map<Long, Transaction> nodes; //txID -> Transacao
    private final Map<Long, List<Long>> outEdges; // txID [Vizinhos de saida]
    private final Map<Long, List<Long>> inEdges; // txID -> [vizinhos de entrada]
    private final List <long[]> edgeList; // [[from, to], ...] para iteracao

    public Graph() {
        this.nodes = new HashMap<>();
        this.outEdges = new HashMap<>();
        this.inEdges = new HashMap<>();
        this.edgeList = new ArrayList<>();
    }

    // -- Inserção
    public void addNode(Transaction t) {
        nodes.put(t.getTxId(), t);
        outEdges.putIfAbsent(t.getTxId(), new ArrayList<>());
        inEdges.putIfAbsent(t.getTxId(), new ArrayList<>());
    }

    public void addEdge(long from, long to) {
        // ignora arestas cujos nós não existem no grafo filtrado
        if (!nodes.containsKey(from) || !nodes.containsKey(to)) return;

        outEdges.get(from).add(to);
        inEdges.get(to).add(from);
        edgeList.add(new long[]{from, to});
    }

    // -- Consultas
    public Collection<Transaction> getNodes()       { return nodes.values();}
    public List<long[]>            getEdgeList()    { return edgeList;}
    public Transaction             getNode(long id) { return nodes.get(id);}
    public Set<Long>               getNodeIds()     { return nodes.keySet();}

    public List<Long> getOutNeighbors(long txId) {
        return outEdges.getOrDefault(txId, Collections.emptyList());
    }

    public List<Long> getInNeighbors(long txId) {
        return inEdges.getOrDefault(txId, Collections.emptyList());
    }

    /** Vizinhos ignorando direção (união de entrada + saída) */
    public Set<Long> getUndirectedNeighbors(long txId) {
        Set<Long> neighbors = new HashSet<>();
        neighbors.addAll(getOutNeighbors(txId));
        neighbors.addAll(getInNeighbors(txId));
        return neighbors;
    }

    public int getOutDegree(long txId) {
        return outEdges.getOrDefault(txId, Collections.emptyList()).size();
    }

    public int getInDegree(long txId) {
        return inEdges.getOrDefault(txId, Collections.emptyList()).size();
    }

    public boolean hasNode(long txId) { return nodes.containsKey(txId); }

    // -- Tamanho
    public int nodeCount() { return nodes.size(); }
    public int edgeCount() { return edgeList.size(); }

    // -- Subgrafo induzido
    /**
     * Retorna um novo grafo contendo apenas os nós que satisfazem o filtro
     * e as arestas internas a esse subconjunto.
     */
    public Graph inducedSubgraph(java.util.function.Predicate<Transaction> filter) {
        Graph sub = new Graph();

        // 1. adiciona nós que passam no filtro
        for (Transaction t : nodes.values()) {
            if (filter.test(t)) sub.addNode(t);
        }

        // 2. adiciona só as arestas internas
        for (long[] e : edgeList) {
            if (sub.hasNode(e[0]) && sub.hasNode(e[1])) {
                sub.addEdge(e[0], e[1]);
            }
        }
        return sub;
    }

    // -- Estatisticas
    public void printStats() {
        long illicit = nodes.values().stream().filter(Transaction::isIllicit).count();
        long licit   = nodes.values().stream().filter(Transaction::isLicit).count();
        long unknown = nodeCount() - illicit - licit;

        System.out.println("=== Estatísticas do Grafo ===");
        System.out.printf("  Nós:      %,d%n", nodeCount());
        System.out.printf("  Arestas:  %,d%n", edgeCount());
        System.out.printf("  Ilícitos: %,d (%.1f%%)%n", illicit, 100.0 * illicit / nodeCount());
        System.out.printf("  Lícitos:  %,d (%.1f%%)%n", licit,   100.0 * licit   / nodeCount());
        System.out.printf("  Desconhecidos: %,d%n", unknown);
    }
}
