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
     *   <li>{@code "O grafo É conexo."} — todos os nós foram alcançados</li>
     *   <li>{@code "O grafo NÃO é conexo."} — existem componentes desconectados</li>
     *   <li>{@code "Grafo vazio."} — nenhum nó foi adicionado ao grafo</li>
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
     *   <li>Número total de componentes encontrados</li>
     *   <li>Lista dos vértices pertencentes a cada componente</li>
     *   <li>{@code "Grafo vazio."} — nenhum nó foi adicionado ao grafo</li>
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
     *   <li>O índice do componente</li>
     *   <li>A quantidade de nós pertencentes ao componente</li>
     *   <li>A lista de identificadores ({@code txId}) dos nós</li>
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
     * Verifica se o grafo é Euleriano e, caso seja, imprime
     * um caminho/circuito Euleriano.
     *
     * <p>
     * Um grafo é considerado Euleriano quando é possível percorrer
     * todas as arestas exatamente uma vez.
     * </p>
     *
     * <p>
     * A verificação é realizada pelo método {@link #contarVertices()},
     * que analisa a paridade do grau de cada vértice.
     * </p>
     *
     * <p>
     * Se o grafo satisfizer as condições de Eulerianidade,
     * o método {@link #imprimir_Caminho_Euleriano()} é chamado
     * para construir e exibir o caminho encontrado.
     * </p>
     *
     * <p><b>Saída esperada:</b></p>
     * <ul>
     *   <li>O caminho Euleriano encontrado, quando existir</li>
     *   <li>{@code "Este Grafo não é Euleriano"} caso contrário</li>
     * </ul>
     *
     * @see #contarVertices()
     * @see #imprimir_Caminho_Euleriano()
     */
    public  void isEuleriano(){
        if(contarVertices()){
            imprimir_Caminho_Euleriano();
            return;
        }
        System.out.println("Este Grafo não é Euleriano");
    }

    /**
     * Conta a quantidade de vértices com grau par e ímpar
     * para verificar a existência de um caminho Euleriano.
     *
     * <p>
     * O grau de um vértice é calculado pela soma do número
     * de arestas de entrada e saída.
     * </p>
     *
     * <p>
     * Para um grafo tratado como não-direcionado:
     * </p>
     * <ul>
     *   <li>0 vértices ímpares → existe circuito Euleriano</li>
     *   <li>2 vértices ímpares → existe caminho Euleriano</li>
     *   <li>Qualquer outro caso → não é Euleriano</li>
     * </ul>
     *
     * <p>
     * O método também exibe no console a quantidade de
     * vértices pares e ímpares encontrados.
     * </p>
     *
     * <p><b>Complexidade:</b> O(V), onde V é o número de vértices.</p>
     *
     * @return {@code true} se o grafo possuir caminho ou circuito
     *         Euleriano; {@code false} caso contrário
     *
     * @see #getOutDegree(long)
     * @see #getInDegree(long)
     */
    private boolean contarVertices() {

        int verticesPar = 0;
        int verticesImpares = 0;

        for (Long id : nodes.keySet()) {

            int grau = getOutDegree(id) + getInDegree(id);

            if (grau % 2 == 0) {
                verticesPar++;
            } else {
                verticesImpares++;
            }
        }

        System.out.println("Pares: " + verticesPar);
        System.out.println("Ímpares: " + verticesImpares);

        return verticesImpares == 0 || verticesImpares == 2;
    }

    /**
     * Constrói e imprime um caminho Euleriano utilizando
     * o algoritmo de Hierholzer.
     *
     * <p>
     * O algoritmo percorre as arestas do grafo removendo-as
     * temporariamente de uma cópia da lista de adjacência,
     * garantindo que cada aresta seja visitada exatamente
     * uma vez.
     * </p>
     *
     * <p>
     * Uma pilha é utilizada para armazenar o caminho atual,
     * enquanto uma lista registra o caminho Euleriano final.
     * </p>
     *
     * <p>
     * O grafo original não é alterado, pois é criada uma
     * cópia das listas de adjacência antes da execução.
     * </p>
     *
     * <p><b>Complexidade:</b> O(E), onde E é o número de arestas.</p>
     *
     * <p><b>Saída esperada:</b></p>
     * <pre>
     * Caminho Euleriano:
     * 1 -> 2 -> 3 -> 4 -> 1
     * </pre>
     *
     * @see #getOutNeighbors(long)
     */
    private void imprimir_Caminho_Euleriano() {

        Stack<Long> pilha = new Stack<>();
        List<Long> caminho = new ArrayList<>();

        // escolhe um vértice inicial
        Long inicio = nodes.keySet().iterator().next();

        // cópia das arestas para não destruir o grafo original
        Map<Long, List<Long>> temp = new HashMap<>();

        for (Long id : nodes.keySet()) {
            temp.put(id, new ArrayList<>(getOutNeighbors(id)));
        }

        pilha.push(inicio);

        while (!pilha.isEmpty()) {

            Long atual = pilha.peek();

            if (temp.get(atual).isEmpty()) {
                caminho.add(pilha.pop());
            }else {
                Long prox = temp.get(atual).remove(0);
                pilha.push(prox);
            }
        }

        Collections.reverse(caminho);
        System.out.println("Caminho Euleriano:");

        for (int i = 0; i < caminho.size() - 1; i++) {
            System.out.print(caminho.get(i) + " -> ");
        }

        System.out.println(caminho.get(caminho.size() - 1));
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
     *   <li>{@code "Ciclo encontrado:"} seguido dos vértices do ciclo</li>
     *   <li>{@code "Este Grafo não é Cíclico"} caso nenhum ciclo exista</li>
     * </ul>
     *
     * @see #encontrarCiclo()
     * @see #dfsCiclo(Long, Set, Set, List)
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
     * Procura o primeiro ciclo encontrado no grafo.
     *
     * <p>
     * O método percorre todos os vértices ainda não visitados e inicia
     * uma DFS para cada componente do grafo. Quando um ciclo é encontrado,
     * a busca é interrompida e o caminho correspondente é retornado.
     * </p>
     *
     * <p>
     * Caso nenhum ciclo exista, uma lista vazia é retornada.
     * </p>
     *
     * <p><b>Complexidade:</b> O(V + E)</p>
     *
     * @return lista contendo os vértices do ciclo encontrado;
     *         uma lista vazia caso o grafo seja acíclico
     *
     * @see #dfsCiclo(Long, Set, Set, List)
     */
    private List<Long> encontrarCiclo() {

        Set<Long> visited = new HashSet<>();
        Set<Long> recStack = new HashSet<>();
        List<Long> caminho = new ArrayList<>();

        for (Long node : nodes.keySet()) {

            if (!visited.contains(node)
                    && dfsCiclo(node, visited, recStack, caminho)) {

                return caminho;
            }
        }

        return Collections.emptyList();
    }

    /**
     * Executa uma Busca em Profundidade (DFS) para detectar ciclos.
     *
     * <p>
     * Durante a execução são mantidos dois conjuntos:
     * </p>
     * <ul>
     *   <li>{@code visited} — vértices já visitados pela DFS</li>
     *   <li>{@code recStack} — vértices presentes na pilha de recursão atual</li>
     * </ul>
     *
     * <p>
     * Quando um vértice alcança outro que já está presente em
     * {@code recStack}, um ciclo foi encontrado.
     * </p>
     *
     * <p>
     * Os vértices visitados são armazenados em {@code caminho},
     * permitindo reconstruir e exibir o ciclo encontrado.
     * </p>
     *
     * <p><b>Complexidade:</b> O(V + E)</p>
     *
     * @param atual vértice atualmente processado
     * @param visited conjunto de vértices já visitados
     * @param recStack conjunto de vértices presentes na pilha de recursão
     * @param caminho caminho percorrido pela DFS
     *
     * @return {@code true} se um ciclo foi encontrado;
     *         {@code false} caso contrário
     *
     * @see #getOutNeighbors(long)
     */
    private boolean dfsCiclo(Long atual,
                             Set<Long> visited,
                             Set<Long> recStack,
                             List<Long> caminho) {

        visited.add(atual);
        recStack.add(atual);
        caminho.add(atual);

        for (Long vizinho : getOutNeighbors(atual)) {

            if (!visited.contains(vizinho)) {

                if (dfsCiclo(vizinho,
                        visited,
                        recStack,
                        caminho)) {

                    return true;
                }

            } else if (recStack.contains(vizinho)) {

                caminho.add(vizinho);
                return true;
            }
        }

        recStack.remove(atual);
        caminho.remove(caminho.size() - 1);

        return false;
    }


}