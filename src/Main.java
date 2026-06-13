import io.EllipticLoader;
import io.PajekReader;
import io.PajekWriter;
import model.Graph;

public class Main {

    public static void main(String[] args) throws Exception {

        // -- Caminhos dos Arquivos
        String featuresPath = "data/filtered_features.csv";
        String edgesPath = "data/filtered_edges.csv";
        String classesPath = "data/filtered_classes.csv";

        // -- Carrega o grafo
        Graph graph = EllipticLoader.load(featuresPath, edgesPath, classesPath);

        // ── 2. Salva em formato Pajek ─────────────────────────
        System.out.println("\n=== Gravando Pajek ===");
        PajekWriter.write(graph, "data/elliptic.net");

        // ── 3. Carrega de volta do Pajek ──────────────────────
        System.out.println("\n=== Carregando do Pajek ===");
        Graph reloaded = PajekReader.read("data/elliptic.net");
        reloaded.printStats();

        // ── 4. Verifica integridade (nós e arestas batem?) ────
        System.out.println("\n=== Verificação de integridade ===");
        boolean ok = graph.nodeCount() == reloaded.nodeCount()
                && graph.edgeCount() == reloaded.edgeCount();
        System.out.println(ok
                ? "✓ Grafo salvo e recarregado com sucesso!"
                : "✗ DIVERGÊNCIA — verifique os arquivos.");

        // -- Estatísticas básicas
        graph.printStats();
        graph.isConexo();

        graph.componentesDesconexos();
        graph.isEuleriano();

        graph.checkingCyclic();

    }
}