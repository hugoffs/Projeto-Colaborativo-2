package model;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Gerador de grafos aleatórios.
 *
 * <p>
 * Atende o requisito do trabalho: criar um grafo a partir de três entradas —
 * número de nós, número de arestas e se o grafo deve ser conexo ou não.
 * </p>
 *
 * <p>
 * O grafo gerado é <b>direcionado simples</b>: não há auto-ligações (um nó
 * ligado a ele mesmo) nem arestas direcionadas repetidas (mesmo {@code from→to}).
 * Os nós são numerados de {@code 1} a {@code N} e recebem o rótulo
 * {@link Transaction.Label#UNKNOWN}. O número de arestas pedido é sempre
 * respeitado de forma exata.
 * </p>
 *
 * <p>
 * A conexidade segue a mesma semântica usada pelo restante do projeto:
 * <b>conexidade fraca</b> (ignorando a direção das arestas).
 * </p>
 *
 * @see Graph
 */
public final class GeradorGrafoAleatorio {

    /** Fonte de aleatoriedade compartilhada pela geração. */
    private static final Random RANDOM = new Random();

    /** Classe utilitária — não deve ser instanciada. */
    private GeradorGrafoAleatorio() {
    }

    // ── API pública ─────────────────────────────────────────────────────────

    /**
     * Gera um grafo aleatório com o número de nós e arestas informados.
     *
     * @param nNos     quantidade de nós (>= 1)
     * @param nArestas quantidade de arestas (>= 0)
     * @param conexo   {@code true} para garantir um grafo conexo;
     *                 {@code false} para garantir um grafo desconexo
     * @return o grafo gerado
     * @throws IllegalArgumentException se a combinação de parâmetros for impossível
     */
    public static Graph gerar(int nNos, int nArestas, boolean conexo) {
        validar(nNos, nArestas, conexo);

        Graph g = new Graph();
        for (int i = 1; i <= nNos; i++) {
            g.addNode(new Transaction(i, 0, new double[0]));
        }

        if (conexo) {
            gerarConexo(g, nNos, nArestas);
        } else {
            gerarDesconexo(g, nNos, nArestas);
        }
        return g;
    }

    // ── Validação ─────────────────────────────────────────────────────────────

    private static void validar(int nNos, int nArestas, boolean conexo) {
        if (nNos < 1) {
            throw new IllegalArgumentException(
                    "O número de nós deve ser >= 1 (recebido: " + nNos + ").");
        }
        if (nArestas < 0) {
            throw new IllegalArgumentException(
                    "O número de arestas não pode ser negativo (recebido: " + nArestas + ").");
        }

        long maxTotal = (long) nNos * (nNos - 1); // direcionado, sem auto-ligação/repetição

        if (conexo) {
            if (nArestas < nNos - 1) {
                throw new IllegalArgumentException(
                        "Para um grafo conexo com " + nNos + " nós são necessárias pelo menos "
                                + (nNos - 1) + " arestas (recebido: " + nArestas + ").");
            }
            if (nArestas > maxTotal) {
                throw new IllegalArgumentException(
                        "Número de arestas (" + nArestas + ") excede o máximo de " + maxTotal
                                + " para " + nNos + " nós sem repetição nem auto-ligação.");
            }
        } else {
            if (nNos < 2) {
                throw new IllegalArgumentException(
                        "Não é possível gerar um grafo desconexo com menos de 2 nós.");
            }
            long maxDesconexo = capacidadeDesconexo(nNos);
            if (nArestas > maxDesconexo) {
                throw new IllegalArgumentException(
                        "Número de arestas (" + nArestas + ") excede o máximo de " + maxDesconexo
                                + " para um grafo desconexo com " + nNos + " nós (dividido em 2 grupos).");
            }
        }
    }

    /**
     * Ponto onde os nós são divididos em 2 grupos no modo desconexo.
     * Fonte única usada tanto pela validação quanto pela geração, garantindo
     * que ambas concordem sobre o tamanho dos grupos.
     */
    private static int pontoDivisao(int nNos) {
        return nNos / 2;
    }

    /** Capacidade máxima de arestas quando o grafo é dividido em 2 grupos. */
    private static long capacidadeDesconexo(int nNos) {
        int k = pontoDivisao(nNos);
        return (long) k * (k - 1) + (long) (nNos - k) * (nNos - k - 1);
    }

    // ── Geração conexa ────────────────────────────────────────────────────────

    /**
     * Gera um grafo fracamente conexo: primeiro constrói uma árvore geradora
     * (garante a conexidade com N-1 arestas) e depois completa com arestas
     * aleatórias até atingir o total pedido.
     */
    private static void gerarConexo(Graph g, int nNos, int nArestas) {
        long base = nNos + 1L;
        Set<Long> usadas = new HashSet<>();

        // Árvore geradora: cada novo nó liga-se a um nó já incluído,
        // com direção sorteada para variar o grafo.
        for (int i = 2; i <= nNos; i++) {
            int j = 1 + RANDOM.nextInt(i - 1);
            int from = i;
            int to = j;
            if (RANDOM.nextBoolean()) {
                from = j;
                to = i;
            }
            adicionar(g, from, to, base, usadas);
        }

        int restantes = nArestas - (nNos - 1); // nNos >= 1 garantido por validar()
        preencherAleatorio(g, 1, nNos, restantes, base, usadas);
    }

    // ── Geração desconexa ─────────────────────────────────────────────────────

    /**
     * Gera um grafo desconexo: divide os nós em 2 grupos e cria arestas apenas
     * dentro de cada grupo (nunca entre eles), garantindo a desconexão.
     */
    private static void gerarDesconexo(Graph g, int nNos, int nArestas) {
        long base = nNos + 1L;
        Set<Long> usadas = new HashSet<>();

        int k = pontoDivisao(nNos);  // grupo A: [1, k]
        int a = k;
        int b = nNos - k;          // grupo B: [k+1, nNos]
        long capA = (long) a * (a - 1);
        long capB = (long) b * (b - 1);

        // Distribui as arestas entre os grupos proporcionalmente à capacidade,
        // ajustando para nunca estourar a capacidade de nenhum grupo.
        int naA = (int) Math.min(capA, Math.round((double) nArestas * capA / (capA + capB)));
        int naB = nArestas - naA;
        if (naB > capB) {
            naB = (int) capB;
            naA = nArestas - naB;
        }

        preencherAleatorio(g, 1, k, naA, base, usadas);
        preencherAleatorio(g, k + 1, nNos, naB, base, usadas);
    }

    // ── Helpers de aresta ─────────────────────────────────────────────────────

    /**
     * Adiciona {@code quantidade} arestas direcionadas aleatórias entre os nós
     * do intervalo {@code [lo, hi]}, evitando auto-ligações e repetições.
     */
    private static void preencherAleatorio(Graph g, int lo, int hi,
                                           int quantidade, long base, Set<Long> usadas) {
        if (quantidade <= 0) {
            return;
        }
        int tamanho = hi - lo + 1;
        int adicionadas = 0;
        while (adicionadas < quantidade) {
            int from = lo + RANDOM.nextInt(tamanho);
            int to = lo + RANDOM.nextInt(tamanho);
            if (from == to) {
                continue;
            }
            if (adicionar(g, from, to, base, usadas)) {
                adicionadas++;
            }
        }
    }

    /**
     * Adiciona a aresta {@code from→to} se ela ainda não existir.
     *
     * @return {@code true} se a aresta foi adicionada; {@code false} se já existia
     */
    private static boolean adicionar(Graph g, int from, int to, long base, Set<Long> usadas) {
        long chave = (long) from * base + to;
        if (!usadas.add(chave)) {
            return false;
        }
        g.addEdge(from, to);
        return true;
    }
}
