package app;

import io.EllipticLoader;
import io.PajekReader;
import io.PajekWriter;
import model.Graph;
import model.GeradorGrafoAleatorio;

import java.util.Scanner;

/**
 * Shell interativo do ElliChain.
 * Mantém o grafo carregado em memória e expõe as operações via menu.
 */
public class EllipticApp {

    private static final String FEATURES = "data/filtered_features.csv";
    private static final String EDGES = "data/filtered_edges.csv";
    private static final String CLASSES = "data/filtered_classes.csv";
    private static final String PAJEK = "data/elliptic.net";

    private Graph graph;
    private final Scanner sc = new Scanner(System.in);

    // ── Entry point ───────────────────────────────────────────────────────

    public void run() {
        header();
        loadGraph(); // carrega automaticamente ao iniciar

        boolean running = true;
        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1" -> loadGraph();
                case "2" -> savePajek();
                case "3" -> loadPajek();
                case "4" -> showStats();
                case "5" -> checkConnectivity();
                case "6" -> checkEulerian();
                case "7" -> checkCyclic();
                case "8" -> calculateClosenessMetric();
                case "9" -> calculateBetweennessMetric();
                case "10" -> checkGerarGrafo();
                case "11" -> checkprintGrafo();
                case "12" -> checkWarshall();
                case "0" -> running = false;
                default -> System.out.println("  Opção inválida.\n");
            }
        }

        System.out.println("Encerrando ElliChain. Até mais!");
    }

    // ── Menu ──────────────────────────────────────────────────────────────

    private void header() {
        System.out.println("""
                ╔══════════════════════════════════════╗
                ║   EllipticApp - Análise de Lavagem   ║
                ║        de Dinheiro em Bitcoin        ║
                ╚══════════════════════════════════════╝
                """);
    }

    private void printMenu() {
        String status = graph == null
                ? "  [sem grafo carregado]"
                : String.format("  [%,d nós · %,d arestas]", graph.nodeCount(), graph.edgeCount());

        System.out.println("─".repeat(42));
        System.out.println(status);
        System.out.println("─".repeat(42));
        System.out.println("  1. Carregar dataset (CSV)");
        System.out.println("  2. Salvar grafo (Pajek)");
        System.out.println("  3. Carregar grafo (Pajek)");
        System.out.println("  4. Estatísticas do grafo");
        System.out.println("  5. Conectividade / Componentes");
        System.out.println("  6. Verificar Euleriano");
        System.out.println("  7. Verificar Ciclos");
        System.out.println("  8. Calcular Centralidade de Proximidade");
        System.out.println("  9. Calcular Centralidade de Intermediação");
        System.out.println(" 10. Gerar Grafo Aleatorio");
        System.out.println(" 11. Imprimir Grafo");
        System.out.println(" 12. Algoritmo de Warshall");
        System.out.println("  0. Sair");
        System.out.println("─".repeat(42));
        System.out.print("  Escolha: ");
    }

    // ── Ações ─────────────────────────────────────────────────────────────

    private void loadGraph() {
        try {
            System.out.println("Carregando dataset Elliptic...");
            graph = EllipticLoader.load(FEATURES, EDGES, CLASSES);
            ok("Dataset carregado.");
        } catch (Exception e) {
            err("Falha ao carregar: " + e.getMessage());
        }
    }

    private void savePajek() {
        if (noGraph())
            return;
        try {
            PajekWriter.write(graph, PAJEK);
            ok("Grafo salvo em: " + PAJEK);
        } catch (Exception e) {
            err("Falha ao salvar: " + e.getMessage());
        }
    }

    private void loadPajek() {
        try {
            graph = PajekReader.read(PAJEK);

            // verificação de integridade
            boolean ok = graph.nodeCount() > 0 && graph.edgeCount() > 0;
            if (ok)
                ok("Grafo carregado do Pajek.");
            else
                err("Arquivo carregado, mas parece vazio.");

        } catch (Exception e) {
            err("Falha ao carregar Pajek: " + e.getMessage());
        }
    }

    private void showStats() {
        if (noGraph())
            return;
        System.out.println();
        graph.printStats();
        System.out.println();
    }

    private void checkConnectivity() {
        if (noGraph())
            return;
        System.out.println();
        graph.isConexo();
        graph.componentesDesconexos();
        System.out.println();
    }

    private void checkEulerian() {
        if (noGraph())
            return;
        System.out.println();
        graph.isEuleriano();
        System.out.println();
    }

    private void checkCyclic() {
        if (noGraph())
            return;
        System.out.println();
        graph.checkingCyclic();
        System.out.println();
    }

    private void calculateClosenessMetric() {
        if (noGraph())
            return;
        System.out.println();
        graph.calculateCloseness();
        System.out.println();
    }

    private void calculateBetweennessMetric() {
        if (noGraph())
            return;
        System.out.println();
        graph.calculateBetweenness();
        System.out.println();
    }

    private void checkGerarGrafo(){

        System.out.println();
        System.out.println("Digite a Quantidade de Nos: ");
        System.out.print("  Nos: ");
        int no = checkNumber(sc);
        System.out.println("Digite o Numero de aresta: ");
        System.out.print("  Aresta: ");
        int aresta = checkNumber(sc);
        System.out.println("Digite true para Conexo e false para não conexo: ");
        System.out.print("  Conexo: ");
        boolean conexo  = checkBoolean(sc);

        graph = GeradorGrafoAleatorio.gerar(no, aresta, conexo);
    }

    private int checkNumber(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.println("Erro: digite apenas números inteiros (sem vírgula ou ponto).");
            if (sc.nextInt() <= 0) {
                System.out.println(" O numero Tem q ser possitivo e maior que 0");
            }
        }
        return sc.nextInt();
    }

    private boolean checkBoolean(Scanner sc) {
        while (!sc.hasNextBoolean()) {
            System.out.println("Erro: digite apenas true ou false.");
            sc.next();
        }

        return sc.nextBoolean();
    }

    private void checkprintGrafo(){
        if(noGraph())
            return;
        System.out.println();
        graph.printGrafo();
        System.out.println();
    }

    private void checkWarshall(){
        if(noGraph())
            return;
        System.out.println();
        graph.warshall();
        System.out.println();
    }

    // ── Utilitários ───────────────────────────────────────────────────────

    /** Retorna true (e imprime aviso) se não houver grafo carregado. */
    private boolean noGraph() {
        if (graph == null) {
            err("Nenhum grafo carregado. Use a opção 1 ou 3 primeiro.");
            return true;
        }
        return false;
    }

    private void ok(String msg) {
        System.out.println("  ✓ " + msg + "\n");
    }

    private void err(String msg) {
        System.out.println("  ✗ " + msg + "\n");
    }
}