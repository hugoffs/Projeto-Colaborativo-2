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
    private Set<Long> getUndirectedNeighbors(long txId) {
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
     * t -> t.isIllicit() || graph.getUndirectedNeighbors(t.getTxId())
     * .stream().anyMatch(id -> graph.getNode(id).isIllicit()));
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
     * arbitrário. A busca percorre os vizinhos obtidos por
     * {@link #getUndirectedNeighbors(long)}, tratando o grafo como
     * não-direcionado durante a verificação.
     * </p>
     *
     * <p>
     * Ao final, se todos os nós tiverem sido visitados pela BFS,
     * o grafo é considerado conexo.
     * </p>
     *
     * <p><b>Complexidade:</b> O(V + E), onde V é o número de nós e E o de arestas.</p>
     *
     * <p><b>Saída esperada:</b></p>
     * <ul>
     * <li>{@code "O grafo É conexo."} — todos os nós foram alcançados</li>
     * <li>{@code "O grafo NÃO é conexo."} — existem componentes desconectados</li>
     * <li>{@code "Grafo vazio."} — nenhum nó foi adicionado ao grafo</li>
     * </ul>
     *
     * @see #bfs(Long, Set)
     * @see #getUndirectedNeighbors(long)
     * @see #nodeCount()
     */
    public void isConexo() {

        if (nodes.isEmpty()) {
            System.out.println("Grafo vazio.");
            return;
        }

        Set<Long> visited = new HashSet<>();

        Long inicio = nodes.keySet()
                .iterator()
                .next();

        bfs(inicio, visited);

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
     * onde existe caminho entre qualquer par de vértices quando a direção
     * das arestas é ignorada.
     * </p>
     *
     * <p>
     * O algoritmo percorre todos os nós do grafo e executa uma
     * <b>BFS (Busca em Largura)</b> para cada nó ainda não visitado.
     * Cada execução da BFS encontra exatamente um componente conexo.
     * </p>
     *
     * <p>
     * Os componentes encontrados são armazenados e posteriormente
     * exibidos pelo método {@link #imprimirComponentes(List)}.
     * </p>
     *
     * <p><b>Complexidade:</b> O(V + E), onde V é o número de nós e E o de arestas.</p>
     *
     * <p><b>Saída esperada:</b></p>
     * <ul>
     * <li>Número total de componentes encontrados</li>
     * <li>Lista dos vértices pertencentes a cada componente</li>
     * <li>{@code "Grafo vazio."} — nenhum nó foi adicionado ao grafo</li>
     * </ul>
     *
     * @see #bfs(Long, Set)
     * @see #imprimirComponentes(List)
     * @see #getUndirectedNeighbors(long)
     */
    public void componentesDesconexos() {

        if (nodes.isEmpty()) {
            System.out.println("Grafo vazio.");
            return;
        }

        Set<Long> visited = new HashSet<>();
        List<Set<Long>> componentes = new ArrayList<>();

        for (Long no : nodes.keySet()) {

            if (!visited.contains(no)) {
                componentes.add(bfs(no, visited));
            }
        }

        imprimirComponentes(componentes);
    }


    /**
     * Exibe no console os componentes conexos encontrados.
     *
     * <p>
     * Para cada componente são apresentados:
     * </p>
     * <ul>
     * <li>O índice do componente</li>
     * <li>A quantidade de nós pertencentes ao componente</li>
     * <li>A lista de identificadores ({@code txId}) dos nós</li>
     * </ul>
     *
     * @param componentes lista contendo todos os componentes encontrados
     */
    private void imprimirComponentes(List<Set<Long>> componentes) {
        System.out.println(
                "Total de componentes encontrados: "
                        + componentes.size());

        for (int i = 0; i < componentes.size(); i++) {

            System.out.println(
                    "Componente " + (i + 1)
                            + " — Nós ("
                            + componentes.get(i).size()
                            + "): "
                            + componentes.get(i));
        }
    }


    /**
     * Visita os vizinhos ainda não explorados de um vértice.
     *
     * <p>
     * Os vizinhos são obtidos através de
     * {@link #getUndirectedNeighbors(long)}, permitindo que o grafo
     * seja tratado como não-direcionado durante algoritmos de
     * conectividade.
     * </p>
     *
     * <p>
     * Todo vizinho não visitado é marcado como visitado e adicionado
     * à fila da BFS.
     * </p>
     *
     * @param atual vértice atualmente processado
     * @param visited conjunto de vértices já visitados
     * @param fila fila utilizada pela BFS
     *
     * @see #getUndirectedNeighbors(long)
     */
    private void visitarVizinhos(
            Long atual,
            Set<Long> visited,
            Queue<Long> fila) {

        for (Long vizinho : getUndirectedNeighbors(atual)) {

            if (!visited.contains(vizinho)) {
                visited.add(vizinho);
                fila.add(vizinho);
            }
        }
    }


    private Set<Long> bfs(Long inicio, Set<Long> visited) {

        Set<Long> componente = new HashSet<>();
        Queue<Long> fila = new LinkedList<>();

        fila.add(inicio);
        visited.add(inicio);

        while (!fila.isEmpty()) {

            Long atual = fila.poll();

            componente.add(atual);

            visitarVizinhos(atual, visited, fila);
        }

        return componente;
    }

    /**
     * Verifica se o grafo é Euleriano (tratando-o como não-direcionado,
     * de forma consistente com a conexidade fraca usada no resto do projeto)
     * e, quando for, imprime um caminho/circuito Euleriano.
     *
     * <p>Condições verificadas:</p>
     * <ul>
     * <li><b>Conexidade:</b> todas as arestas devem pertencer a um único
     * componente (vértices isolados são ignorados) — verificada por
     * {@link #arestasNoMesmoComponente()}.</li>
     * <li><b>Paridade:</b> 0 vértices de grau ímpar → circuito Euleriano;
     * exatamente 2 → caminho Euleriano (semi-Euleriano);
     * qualquer outro número → não é Euleriano.</li>
     * </ul>
     *
     * <p>Sem a checagem de conexidade um grafo desconexo com todos os graus
     * pares seria classificado, incorretamente, como Euleriano.</p>
     *
     * @see #arestasNoMesmoComponente()
     * @see #imprimir_Caminho_Euleriano()
     */
    public void isEuleriano() {
        if (nodes.isEmpty()) {
            System.out.println("Grafo vazio.");
            return;
        }

        int impares = contarVerticesImpares();

        if (!arestasNoMesmoComponente()) {
            System.out.println("Este Grafo NÃO é Euleriano (as arestas estão em componentes separados).");
            return;
        }

        if (impares == 0) {
            System.out.println("O grafo É Euleriano (possui circuito Euleriano).");
            imprimir_Caminho_Euleriano();
        } else if (impares == 2) {
            System.out.println("O grafo é SEMI-Euleriano (possui caminho Euleriano, mas não circuito).");
            imprimir_Caminho_Euleriano();
        } else {
            System.out.println("Este Grafo NÃO é Euleriano (" + impares + " vértices de grau ímpar).");
        }
    }

    /**
     * Conta os vértices de grau ímpar, tratando o grafo como não-direcionado
     * (grau = arestas de entrada + saída). Também imprime no console a
     * quantidade de vértices pares e ímpares.
     *
     * <p><b>Complexidade:</b> O(V), onde V é o número de vértices.</p>
     *
     * @return número de vértices com grau ímpar
     */
    private int contarVerticesImpares() {

        int pares = 0;
        int impares = 0;

        for (Long id : nodes.keySet()) {

            int grau = getOutDegree(id) + getInDegree(id);

            if (grau % 2 == 0) {
                pares++;
            } else {
                impares++;
            }
        }

        System.out.println("Pares: " + pares);
        System.out.println("Ímpares: " + impares);
        return impares;
    }

    /**
     * Verifica se todas as arestas pertencem a um único componente
     * (fracamente conexo). Vértices isolados (grau 0) são ignorados, pois
     * não impedem a existência de um caminho/circuito Euleriano.
     *
     * <p>Reusa a BFS não-direcionada de {@link #bfs(Long, Set)}.</p>
     *
     * <p><b>Complexidade:</b> O(V + E).</p>
     *
     * @return {@code true} se todos os vértices com grau > 0 estiverem
     * no mesmo componente
     */
    private boolean arestasNoMesmoComponente() {

        Long inicio = null;
        for (Long id : nodes.keySet()) {
            if (getOutDegree(id) + getInDegree(id) > 0) {
                inicio = id;
                break;
            }
        }

        if (inicio == null) {
            return true; // sem arestas: circuito vazio é trivialmente Euleriano
        }

        Set<Long> visited = new HashSet<>();
        bfs(inicio, visited);

        for (Long id : nodes.keySet()) {
            if (getOutDegree(id) + getInDegree(id) > 0 && !visited.contains(id)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Escolhe o vértice inicial do percurso Euleriano: um vértice de grau
     * ímpar quando o grafo é semi-Euleriano (início obrigatório do caminho),
     * ou qualquer vértice com arestas quando é um circuito.
     *
     * @return o vértice inicial, ou {@code null} se não houver arestas
     */
    private Long inicioEuleriano() {

        Long comAresta = null;
        for (Long id : nodes.keySet()) {
            int grau = getOutDegree(id) + getInDegree(id);
            if (grau % 2 != 0) {
                return id;
            }
            if (grau > 0 && comAresta == null) {
                comAresta = id;
            }
        }
        return comAresta;
    }

    /**
     * Constrói e imprime um caminho/circuito Euleriano com o algoritmo de
     * Hierholzer, tratando o grafo como <b>não-direcionado</b>: cada aresta
     * é percorrida exatamente uma vez, independentemente da direção
     * (consistente com {@link #contarVerticesImpares()}).
     *
     * <p>
     * Cada arco recebe um id único; o vetor {@code usada} garante que nenhum
     * seja percorrido duas vezes, e um ponteiro de avanço por vértice mantém
     * o algoritmo em O(V + E). O grafo original não é alterado.
     * </p>
     *
     * <p>Só deve ser chamado após {@link #isEuleriano()} confirmar as
     * condições de Eulerianidade.</p>
     *
     * <p><b>Complexidade:</b> O(V + E), onde E é o número de arestas.</p>
     *
     * <p><b>Saída esperada:</b></p>
     * <pre>
     * Caminho Euleriano:
     * 1 -> 2 -> 3 -> 1
     * </pre>
     */
    private void imprimir_Caminho_Euleriano() {

        int m = edgeCount();
        if (m == 0) {
            System.out.println("Caminho Euleriano: (grafo sem arestas)");
            return;
        }

        // Adjacência não-direcionada: cada arco vira uma aresta com id único.
        Map<Long, List<long[]>> adj = new HashMap<>(); // vértice -> [{vizinho, edgeId}]
        for (Long id : nodes.keySet()) {
            adj.put(id, new ArrayList<>());
        }
        List<long[]> arcos = getEdgeList();
        for (int e = 0; e < arcos.size(); e++) {
            long u = arcos.get(e)[0];
            long v = arcos.get(e)[1];
            adj.get(u).add(new long[] { v, e });
            adj.get(v).add(new long[] { u, e });
        }

        boolean[] usada = new boolean[m];
        Map<Long, Integer> ponteiro = new HashMap<>();
        for (Long id : nodes.keySet()) {
            ponteiro.put(id, 0);
        }

        Deque<Long> pilha = new ArrayDeque<>();
        List<Long> caminho = new ArrayList<>();
        pilha.push(inicioEuleriano());

        while (!pilha.isEmpty()) {

            Long u = pilha.peek();
            List<long[]> incidentes = adj.get(u);

            int i = ponteiro.get(u);
            while (i < incidentes.size() && usada[(int) incidentes.get(i)[1]]) {
                i++;
            }
            ponteiro.put(u, i);

            if (i == incidentes.size()) {
                caminho.add(pilha.pop());
            } else {
                long[] aresta = incidentes.get(i);
                usada[(int) aresta[1]] = true;
                pilha.push(aresta[0]);
            }
        }

        Collections.reverse(caminho);

        System.out.println("Caminho Euleriano:");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < caminho.size(); i++) {
            sb.append(caminho.get(i));
            if (i < caminho.size() - 1) {
                sb.append(" -> ");
            }
        }
        System.out.println(sb);
    }

    /**
     * Verifica se o grafo contém ciclos e imprime o primeiro ciclo encontrado.
     *
     * <p>
     * O algoritmo utiliza <b>Busca em Profundidade (DFS)</b> para percorrer
     * o grafo e detectar arestas de retorno (back edges), que caracterizam
     * a existência de ciclos em grafos direcionados.
     * </p>
     *
     * <p>
     * Caso um ciclo seja encontrado, sua sequência de vértices é exibida
     * no console. Caso contrário, é informado que o grafo é acíclico.
     * </p>
     *
     * <p><b>Complexidade:</b> O(V + E), onde V é o número de vértices
     * e E o número de arestas.</p>
     *
     * <p><b>Saída esperada:</b></p>
     * <ul>
     * <li>{@code "Ciclo encontrado:"} seguido dos vértices do ciclo</li>
     * <li>{@code "Este Grafo não é Cíclico"} caso nenhum ciclo exista</li>
     * </ul>
     *
     * @see #encontrarCiclo()
     * @see #reconstruirCiclo(Long, Long, Map)
     */
    public void checkingCyclic() {

        List<Long> ciclo = encontrarCiclo();

        if (ciclo.isEmpty()) {
            System.out.println("Este Grafo não é Cíclico");
            return;
        }

        System.out.println("Ciclo encontrado:");

        for (int i = 0; i < ciclo.size(); i++) {

            System.out.print(ciclo.get(i));

            if (i < ciclo.size() - 1) {
                System.out.print(" -> ");
            }
        }

        System.out.println();
    }


    /**
     * Procura o primeiro ciclo do grafo usando DFS <b>iterativa</b>
     * (pilha explícita), evitando o risco de {@code StackOverflowError}
     * em grafos grandes como o Elliptic (~19k nós).
     *
     * <p>
     * Usa o esquema de cores branco/cinza/preto: uma aresta que aponta para
     * um vértice <b>cinza</b> (ainda na pilha de recursão) é uma back edge e
     * caracteriza um ciclo em grafo direcionado. O mapa de pais permite
     * reconstruir exatamente o ciclo encontrado.
     * </p>
     *
     * <p><b>Complexidade:</b> O(V + E)</p>
     *
     * @return os vértices do ciclo no formato {@code v -> ... -> u -> v};
     * uma lista vazia caso o grafo seja acíclico
     *
     * @see #reconstruirCiclo(Long, Long, Map)
     */
    private List<Long> encontrarCiclo() {

        final int BRANCO = 0, CINZA = 1, PRETO = 2;
        Map<Long, Integer> cor = new HashMap<>(); // ausente = BRANCO
        Map<Long, Long> pai = new HashMap<>();

        for (Long raiz : nodes.keySet()) {

            if (cor.getOrDefault(raiz, BRANCO) != BRANCO) {
                continue;
            }

            Deque<Long> pilha = new ArrayDeque<>();
            Deque<Iterator<Long>> iteradores = new ArrayDeque<>();

            cor.put(raiz, CINZA);
            pilha.push(raiz);
            iteradores.push(getOutNeighbors(raiz).iterator());

            while (!pilha.isEmpty()) {

                Long u = pilha.peek();
                Iterator<Long> it = iteradores.peek();

                if (it.hasNext()) {

                    Long v = it.next();
                    int corV = cor.getOrDefault(v, BRANCO);

                    if (corV == BRANCO) {
                        cor.put(v, CINZA);
                        pai.put(v, u);
                        pilha.push(v);
                        iteradores.push(getOutNeighbors(v).iterator());
                    } else if (corV == CINZA) {
                        return reconstruirCiclo(v, u, pai); // back edge u -> v
                    }
                    // corV == PRETO: vértice já finalizado, ignora

                } else {
                    cor.put(u, PRETO);
                    pilha.pop();
                    iteradores.pop();
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Reconstrói o ciclo a partir da back edge {@code u -> v}, subindo pelo
     * mapa de pais de {@code u} até {@code v}.
     *
     * @param v   vértice cinza alcançado pela back edge (fecha o ciclo)
     * @param u   vértice de onde partiu a back edge
     * @param pai mapa filho → pai construído durante a DFS
     * @return o ciclo no formato {@code v -> ... -> u -> v}
     */
    private List<Long> reconstruirCiclo(Long v, Long u, Map<Long, Long> pai) {

        List<Long> ciclo = new ArrayList<>();

        Long atual = u;
        ciclo.add(atual);
        while (atual != null && !atual.equals(v)) {
            atual = pai.get(atual);
            ciclo.add(atual);
        }

        Collections.reverse(ciclo); // agora: v -> ... -> u
        ciclo.add(v);               // fecha o ciclo: v -> ... -> u -> v
        return ciclo;
    }

    /**
     * Calcula e exibe os nós com maior Centralidade de Proximidade (Closeness).
     * Usa BFS para calcular as distâncias mínimas.
     */
    public void calculateCloseness() {
        System.out.println("Calculando Centralidade de Proximidade...");
        long start = System.currentTimeMillis();

        Map<Long, Double> closenessMap = new HashMap<>();

        for (Long startNode : nodes.keySet()) {
            double sumDistances = 0;
            int reachableNodes = 0;

            Queue<Long> queue = new LinkedList<>();
            Map<Long, Integer> distances = new HashMap<>();

            queue.add(startNode);
            distances.put(startNode, 0);

            while (!queue.isEmpty()) {
                Long v = queue.poll();
                int currentDist = distances.get(v);

                for (Long w : getOutNeighbors(v)) {
                    if (!distances.containsKey(w)) {
                        distances.put(w, currentDist + 1);
                        sumDistances += currentDist + 1;
                        reachableNodes++;
                        queue.add(w);
                    }
                }
            }

            if (sumDistances > 0) {
                double closeness = reachableNodes / sumDistances;
                closenessMap.put(startNode, closeness);
            } else {
                closenessMap.put(startNode, 0.0);
            }
        }

        printTop5("Proximidade (Closeness)", closenessMap, System.currentTimeMillis() - start);
    }

    /**
     * Calcula e exibe os nós com maior Centralidade de Intermediação (Betweenness).
     * Implementa o Algoritmo de Brandes.
     */
    public void calculateBetweenness() {
        System.out.println("Calculando Centralidade de Intermediação (pode levar alguns segundos)...");
        long start = System.currentTimeMillis();

        Map<Long, Double> pontuacao = new HashMap<>();
        for (Long no : nodes.keySet()) {
            pontuacao.put(no, 0.0);
        }

        for (Long fonte : nodes.keySet()) {
            // ordemVisita: nós na ordem em que o BFS os descobriu (usado na fase reversa)
            Stack<Long> ordemVisita = new Stack<>();
            // predecessores: quais nós estão um passo antes de cada nó no caminho mínimo
            Map<Long, List<Long>> predecessores = new HashMap<>();
            // caminhosMínimos: quantos caminhos mínimos chegam a cada nó partindo de fonte
            Map<Long, Integer> caminhosMínimos = new HashMap<>();
            // distancia: distância mínima de fonte até cada nó (-1 = não visitado)
            Map<Long, Integer> distancia = new HashMap<>();

            for (Long no : nodes.keySet()) {
                predecessores.put(no, new ArrayList<>());
                caminhosMínimos.put(no, 0);
                distancia.put(no, -1);
            }

            caminhosMínimos.put(fonte, 1);
            distancia.put(fonte, 0);
            Queue<Long> fila = new LinkedList<>();
            fila.add(fonte);

            // Fase 1 — BFS: descobre distâncias, caminhos mínimos e predecessores
            while (!fila.isEmpty()) {
                Long atual = fila.poll();
                ordemVisita.push(atual);

                for (Long vizinho : getOutNeighbors(atual)) {
                    if (distancia.get(vizinho) < 0) {
                        fila.add(vizinho);
                        distancia.put(vizinho, distancia.get(atual) + 1);
                    }
                    if (distancia.get(vizinho) == distancia.get(atual) + 1) {
                        caminhosMínimos.put(vizinho, caminhosMínimos.get(vizinho) + caminhosMínimos.get(atual));
                        predecessores.get(vizinho).add(atual);
                    }
                }
            }

            // Fase 2 — acumulação reversa: distribui crédito de intermediação
            Map<Long, Double> credito = new HashMap<>();
            for (Long no : nodes.keySet()) credito.put(no, 0.0);

            while (!ordemVisita.isEmpty()) {
                Long no = ordemVisita.pop();
                for (Long pred : predecessores.get(no)) {
                    double fracao = ((double) caminhosMínimos.get(pred) / caminhosMínimos.get(no)) * (1.0 + credito.get(no));
                    credito.put(pred, credito.get(pred) + fracao);
                }
                if (!no.equals(fonte)) {
                    pontuacao.put(no, pontuacao.get(no) + credito.get(no));
                }
            }
        }

        printTop5("Intermediação (Betweenness)", pontuacao, System.currentTimeMillis() - start);
    }

    /**
     * Helper para imprimir os 5 maiores valores de centralidade.
     */
    private void printTop5(String metricName, Map<Long, Double> map, long timeMs) {
        System.out.println("\n--- Top 5 Nós por " + metricName + " ---");
        map.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> System.out.printf("TxID: %d | Valor: %.4f%n", e.getKey(), e.getValue()));
        System.out.printf("Tempo de execução: %d ms%n", timeMs);
    }

    /**
     * Matriz de Alcançabilidade (fecho transitivo) via algoritmo de Warshall.
     *
     * <p>
     * Destinada a grafos <b>pequenos</b>: a matriz é O(n²) em memória e o
     * algoritmo é O(n³) em tempo, inviável no grafo Elliptic completo
     * (~19k nós exigiriam ~365 milhões de booleans e ~10¹³ operações). Por
     * isso há um limite de segurança.
     * </p>
     */
    public void warshall() {

        int n = nodeCount();
        if (n == 0) {
            System.out.println("Grafo vazio.");
            return;
        }

        final int LIMITE = 200;
        if (n > LIMITE) {
            System.out.printf(
                    "Warshall desabilitado: %,d nós exigiriam uma matriz %d×%d e O(n³) operações.%n",
                    n, n, n);
            System.out.printf("Use um grafo com até %d nós (ex.: gere um pela opção 10).%n", LIMITE);
            return;
        }

        // Índice O(1) por nó (evita indexOf O(n) dentro do laço).
        List<Long> ids = new ArrayList<>(nodes.keySet());
        Map<Long, Integer> indice = new HashMap<>();
        for (int i = 0; i < n; i++) {
            indice.put(ids.get(i), i);
        }

        boolean[][] m = new boolean[n][n];
        for (int i = 0; i < n; i++) {

            m[i][i] = true;

            for (Long vizinho : getOutNeighbors(ids.get(i))) {
                m[i][indice.get(vizinho)] = true;
            }
        }

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (m[i][k]) {
                    for (int j = 0; j < n; j++) {
                        if (m[k][j]) {
                            m[i][j] = true;
                        }
                    }
                }
            }
        }

        System.out.println("Fecho Transitivo:");
        StringBuilder sb = new StringBuilder();
        for (boolean[] linha : m) {
            for (boolean valor : linha) {
                sb.append(valor ? '1' : '0').append(' ');
            }
            sb.append(System.lineSeparator());
        }
        System.out.print(sb);
    }

    /**
     * Metodo que imprime o Grafo
     */
    public void printGrafo() {

        if (nodes.isEmpty()) {
            System.out.println("Grafo vazio.");
            return;
        }

        System.out.println("=== Grafo ===");

        for (Long id : nodes.keySet()) {

            System.out.print(id + " -> ");

            List<Long> vizinhos = getOutNeighbors(id);

            if (vizinhos.isEmpty()) {
                System.out.println("[]");
                continue;
            }

            for (int i = 0; i < vizinhos.size(); i++) {

                System.out.print(vizinhos.get(i));

                if (i < vizinhos.size() - 1) {
                    System.out.print(", ");
                }
            }

            System.out.println();
        }
    }
}