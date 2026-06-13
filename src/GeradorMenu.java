import model.GeradorGrafoAleatorio;
import model.Graph;

import java.util.Scanner;

/**
 * Menu interativo para demonstrar o {@link model.GeradorGrafoAleatorio}.
 *
 * <p>
 * Pergunta no console o número de nós, o número de arestas e se o grafo deve
 * ser conexo, gera o grafo e imprime suas estatísticas.
 * </p>
 *
 * <p>Execute com {@code java -cp out GeradorMenu}.</p>
 */
public class GeradorMenu {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== Gerador de Grafo Aleatório ===");
        System.out.print("Número de nós: ");
        int nNos = sc.nextInt();

        System.out.print("Número de arestas: ");
        int nArestas = sc.nextInt();

        System.out.print("Conexo? (s/n): ");
        boolean conexo = lerSimNao(sc);

        try {
            long inicio = System.currentTimeMillis();
            Graph g = GeradorGrafoAleatorio.gerar(nNos, nArestas, conexo);
            long fim = System.currentTimeMillis();

            System.out.println();
            g.printStats();
            System.out.printf("  Gerado em %d ms.%n", fim - inicio);
        } catch (IllegalArgumentException e) {
            System.out.println();
            System.out.println("Não foi possível gerar o grafo: " + e.getMessage());
        }
    }

    /** Lê uma resposta sim/não, aceitando "s"/"sim" como verdadeiro. */
    private static boolean lerSimNao(Scanner sc) {
        String resposta = sc.next().trim().toLowerCase();
        return resposta.equals("s") || resposta.equals("sim");
    }
}
