package model;

import java.util.*;
import java.util.Queue;
import java.util.LinkedList;
/**
 * Grafo direcionado e ponderado representado por lista de adjacência dupla.
 *
 * <p>
 * Cada nó é uma {@link Transaction} identificada por seu {@code txId} (Long).
 * Cada aresta representa um fluxo de Bitcoin entre duas transações.
 * </p>
 *
 * <p>
 * A estrutura mantém simultaneamente:
 * </p>
 * <ul>
 * <li>{@code outEdges} — vizinhos de saída (quem recebe de mim)</li>
 * <li>{@code inEdges} — vizinhos de entrada (quem me enviou)</li>
 * <li>{@code edgeList} — lista plana de pares [from, to] para
 * iteração eficiente sem percorrer todos os nós</li>
 * </ul>
 *
 * <p>
 * Lookup de nó é O(1) via {@code HashMap}. Adição de nó e aresta
 * também são O(1) amortizado.
 * </p>
 *
 * @see io.EllipticLoader
 * @see io.PajekWriter
 * @see io.PajekReader
 */
public class Graph {

    // ── Estrutura interna ─────────────────────────────────────────────────

    /** Mapa principal: txId → Transaction. Garante lookup O(1). */
    private final Map<Long, Transaction> nodes;

    /** Lista de adjacência de saída: txId → [txIds que recebem desta tx]. */
    private final Map<Long, List<Long>> outEdges;

    /** Lista de adjacência de entrada: txId → [txIds que enviaram para esta tx]. */
    private final Map<Long, List<Long>> inEdges;

    /**
     * Lista plana de todas as arestas como pares {@code long[]{from, to}}.
     * Usada pelo {@link io.PajekWriter} e por algoritmos que precisam
     * iterar arestas sem percorrer a lista de adjacência.
     */
    private final List<long[]> edgeList;

    // ── Construtor ────────────────────────────────────────────────────────

    /** Cria um grafo vazio. */
    public Graph() {
        this.nodes = new HashMap<>();
        this.outEdges = new HashMap<>();
        this.inEdges = new HashMap<>();
        this.edgeList = new ArrayList<>();
    }

    // ── Inserção ──────────────────────────────────────────────────────────

    /**
     * Adiciona um nó ao grafo e inicializa suas listas de adjacência.
     * Se o nó já existir (mesmo txId), a chamada é ignorada.
     *
     * @param t transação a adicionar; não deve ser {@code null}
     */
    public void addNode(Transaction t) {
        nodes.put(t.getTxId(), t);
        outEdges.putIfAbsent(t.getTxId(), new ArrayList<>());
        inEdges.putIfAbsent(t.getTxId(), new ArrayList<>());
    }

    /**
     * Adiciona uma aresta direcionada {@code from → to}.
     *
     * <p>
     * Se qualquer um dos nós não existir no grafo, a aresta é
     * silenciosamente ignorada — comportamento necessário ao filtrar
     * o dataset por time step, onde arestas podem referenciar nós
     * de janelas temporais excluídas.
     * </p>
     *
     * @param from txId da transação de origem
     * @param to   txId da transação de destino
     */
    public void addEdge(long from, long to) {
        if (!nodes.containsKey(from) || !nodes.containsKey(to))
            return;
        outEdges.get(from).add(to);
        inEdges.get(to).add(from);
        edgeList.add(new long[] { from, to });
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * @return coleção de todas as transações do grafo
     */
    public Collection<Transaction> getNodes() {
        return nodes.values();
    }

    /**
     * @return lista plana de arestas como {@code long[]{from, to}}
     */
    public List<long[]> getEdgeList() {
        return edgeList;
    }

    /**
     * Busca uma transação pelo seu txId.
     *
     * @param id txId da transação
     * @return a transação, ou {@code null} se não existir
     */
    public Transaction getNode(long id) {
        return nodes.get(id);
    }

    /**
     * @return conjunto de todos os txIds presentes no grafo
     */
    public Set<Long> getNodeIds() {
        return nodes.keySet();
    }

    /**
     * Retorna os vizinhos de saída de um nó (transações que receberam
     * Bitcoin desta).
     *
     * @param txId identificador do nó
     * @return lista de txIds de saída; lista vazia se o nó não existir
     */
    public List<Long> getOutNeighbors(long txId) {
        return outEdges.getOrDefault(txId, Collections.emptyList());
    }

    /**
     * Retorna os vizinhos de entrada de um nó (transações que enviaram
     * Bitcoin para esta).
     *
     * @param txId identificador do nó
     * @return lista de txIds de entrada; lista vazia se o nó não existir
     */
    public List<Long> getInNeighbors(long txId) {
        return inEdges.getOrDefault(txId, Collections.emptyList());
    }

    /**
     * Retorna todos os vizinhos ignorando a direção das arestas
     * (união de entrada + saída).
     *
     * <p>
     * Usado em análises que tratam o grafo como não-direcionado,
     * como verificação de conexidade fraca e checagem de Euleriano.
     * </p>
     *
     * @param txId identificador do nó
     * @return conjunto de txIds vizinhos (sem duplicatas)
     */
    public Set<Long> getUndirectedNeighbors(long txId) {
        Set<Long> neighbors = new HashSet<>();
        neighbors.addAll(getOutNeighbors(txId));
        neighbors.addAll(getInNeighbors(txId));
        return neighbors;
    }

    /**
     * @param txId identificador do nó
     * @return número de arestas saindo deste nó (out-degree)
     */
    public int getOutDegree(long txId) {
        return outEdges.getOrDefault(txId, Collections.emptyList()).size();
    }

    /**
     * @param txId identificador do nó
     * @return número de arestas chegando neste nó (in-degree)
     */
    public int getInDegree(long txId) {
        return inEdges.getOrDefault(txId, Collections.emptyList()).size();
    }

    /**
     * Verifica se um nó está presente no grafo.
     *
     * @param txId identificador do nó
     * @return {@code true} se o nó existir
     */
    public boolean hasNode(long txId) {
        return nodes.containsKey(txId);
    }

    // ── Tamanho ───────────────────────────────────────────────────────────

    /** @return número total de nós (transações) */
    public int nodeCount() {
        return nodes.size();
    }

    /** @return número total de arestas (fluxos) */
    public int edgeCount() {
        return edgeList.size();
    }

    // ── Subgrafo ──────────────────────────────────────────────────────────

    /**
     * Cria um novo grafo contendo apenas os nós que satisfazem o predicado
     * e as arestas cujos dois extremos estão nesse subconjunto.
     *
     * <p>
     * Uso principal: isolar o subgrafo ilícito + vizinhança para
     * calcular centralidades sem processar os ~19k nós completos:
     * </p>
     *
     * <pre>{@code
     * Graph suspicious = graph.inducedSubgraph(
     *         t -> t.isIllicit() || graph.getUndirectedNeighbors(t.getTxId())
     *                 .stream().anyMatch(id -> graph.getNode(id).isIllicit()));
     * }</pre>
     *
     * @param filter predicado aplicado a cada {@link Transaction}
     * @return novo grafo com os nós e arestas filtrados
     */
    public Graph inducedSubgraph(java.util.function.Predicate<Transaction> filter) {
        Graph sub = new Graph();
        for (Transaction t : nodes.values()) {
            if (filter.test(t))
                sub.addNode(t);
        }
        for (long[] e : edgeList) {
            if (sub.hasNode(e[0]) && sub.hasNode(e[1])) {
                sub.addEdge(e[0], e[1]);
            }
        }
        return sub;
    }

    // ── Estatísticas ──────────────────────────────────────────────────────

    /**
     * Imprime no console um resumo do grafo: total de nós e arestas,
     * e a distribuição de rótulos (ilícitos / lícitos / desconhecidos)
     * com percentuais.
     */
    public void printStats() {
        long illicit = nodes.values().stream().filter(Transaction::isIllicit).count();
        long licit = nodes.values().stream().filter(Transaction::isLicit).count();
        long unknown = nodeCount() - illicit - licit;

        System.out.println("=== Estatísticas do Grafo ===");
        System.out.printf("  Nós:           %,d%n", nodeCount());
        System.out.printf("  Arestas:       %,d%n", edgeCount());
        System.out.printf("  Ilícitos:      %,d (%.1f%%)%n", illicit, 100.0 * illicit / nodeCount());
        System.out.printf("  Lícitos:       %,d (%.1f%%)%n", licit, 100.0 * licit / nodeCount());
        System.out.printf("  Desconhecidos: %,d%n", unknown);
    }

    /**
     * Verifica se o grafo é fracamente conexo e imprime o resultado no console.
     *
     * <p>
     * Um grafo direcionado é considerado <b>fracamente conexo</b> quando,
     * ignorando a direção das arestas, existe um caminho entre qualquer
     * par de nós.
     * </p>
     *
     * <p>
     * O algoritmo utiliza <b>BFS (Busca em Largura)</b> partindo de um nó
     * arbitrário, explorando vizinhos de entrada e saída a cada passo.
     * Ao final, se todos os nós foram visitados, o grafo é conexo.
     * </p>
     *
     * <p><b>Complexidade:</b> O(V + E), onde V é o número de nós e E o de arestas.</p>
     *
     * <p><b>Saída esperada:</b></p>
     * <ul>
     *   <li>{@code "O grafo É conexo."} — todos os nós foram alcançados</li>
     *   <li>{@code "O grafo NÃO é conexo."} — existem nós isolados ou componentes separados</li>
     *   <li>{@code "Grafo vazio."} — nenhum nó foi adicionado ao grafo</li>
     * </ul>
     *
     * @see #getOutNeighbors(long)
     * @see #getInNeighbors(long)
     * @see #nodeCount()
     */
    public void isConexo() {
        if (nodes.isEmpty()) {
            System.out.println("Grafo vazio.");
            return;
        }

        HashSet<Long> visited = new HashSet<>();
        Queue<Long> fila = new LinkedList<>();

        Long inicio = nodes.keySet().iterator().next();
        fila.add(inicio);
        visited.add(inicio);

        while (!fila.isEmpty()) {
            Long atual = fila.poll();

            for (Long vizinho : getOutNeighbors(atual)) {
                if (!visited.contains(vizinho)) {
                    visited.add(vizinho);
                    fila.add(vizinho);
                }
            }

            for (Long vizinho : getInNeighbors(atual)) {
                if (!visited.contains(vizinho)) {
                    visited.add(vizinho);
                    fila.add(vizinho);
                }
            }
        }

        if (visited.size() == nodeCount()) {
            System.out.println("O grafo É conexo.");
        } else {
            System.out.println("O grafo NÃO é conexo.");
        }
    }

    /**
     * Identifica e exibe os componentes fracamente conectados do grafo.
     *
     * <p>
     * Um <b>componente fracamente conectado</b> é um subconjunto máximo de nós
     * onde existe caminho entre qualquer par, ignorando a direção das arestas.
     * </p>
     *
     * <p>
     * O algoritmo executa múltiplas <b>BFS (Busca em Largura)</b>, uma para
     * cada nó ainda não visitado. Cada BFS mapeia um componente completo.
     * </p>
     *
     * <p><b>Complexidade:</b> O(V + E), onde V é o número de nós e E o de arestas.</p>
     *
     * <p><b>Saída esperada:</b></p>
     * <ul>
     *   <li>Número total de componentes encontrados</li>
     *   <li>Para cada componente: seu índice e os txIds dos nós que o compõem</li>
     *   <li>{@code "Grafo vazio."} — nenhum nó foi adicionado ao grafo</li>
     * </ul>
     *
     * @see #getOutNeighbors(long)
     * @see #getInNeighbors(long)
     * @see #isConexo()
     */
    public void componentesDesconexos() {
        if (nodes.isEmpty()) {
            System.out.println("Grafo vazio.");
            return;
        }

        HashSet<Long> visited = new HashSet<>();
        List<Set<Long>> componentes = new ArrayList<>();

        for (Long no : nodes.keySet()) {
            if (!visited.contains(no)) {

                // Novo componente encontrado
                Set<Long> componente = new HashSet<>();
                Queue<Long> fila = new LinkedList<>();

                fila.add(no);
                visited.add(no);
                componente.add(no);

                // BFS para mapear o componente inteiro
                while (!fila.isEmpty()) {
                    Long atual = fila.poll();

                    for (Long vizinho : getOutNeighbors(atual)) {
                        if (!visited.contains(vizinho)) {
                            visited.add(vizinho);
                            componente.add(vizinho);
                            fila.add(vizinho);
                        }
                    }

                    for (Long vizinho : getInNeighbors(atual)) {
                        if (!visited.contains(vizinho)) {
                            visited.add(vizinho);
                            componente.add(vizinho);
                            fila.add(vizinho);
                        }
                    }
                }

                componentes.add(componente);
            }
        }

        System.out.println("Total de componentes encontrados: " + componentes.size());
        for (int i = 0; i < componentes.size(); i++) {
            System.out.println("Componente " + (i + 1) + " — Nós (" + componentes.get(i).size() + "): " + componentes.get(i));
        }
    }
}