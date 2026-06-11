import io.EllipticLoader;
import model.Graph;

public class Main {

    public static void main(String[] args) throws Exception {

        // -- Caminhos dos Arquivos
        String featuresPath = "data/filtered_features.csv";
        String edgesPath = "data/filtered_edges.csv";
        String classesPath = "data/filtered_classes.csv";

        // -- Carrega o grafo
        Graph graph = EllipticLoader.load(featuresPath, edgesPath, classesPath);

        // -- Estatísticas básicas
        graph.printStats();
        graph.isConexo();
    }
}