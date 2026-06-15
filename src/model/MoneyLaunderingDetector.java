package model;

import java.util.*;

/**
 * Detector de Lavagem de Dinheiro utilizando propagação de scores de risco.
 * 
 * <p>
 * O algoritmo propaga a suspeita a partir de nós classificados como {@link Transaction.Label#ILLICIT} (peso 1.0)
 * e neutralidade a partir de {@link Transaction.Label#LICIT} (peso 0.0), calculando um score de risco para
 * todas as transações com status {@link Transaction.Label#UNKNOWN}.
 * </p>
 */
public class MoneyLaunderingDetector {

    /**
     * Classe auxiliar para armazenar o resultado da análise de risco de uma transação.
     */
    public static class RiskResult implements Comparable<RiskResult> {
        private final Transaction transaction;
        private final double riskScore;

        public RiskResult(Transaction transaction, double riskScore) {
            this.transaction = transaction;
            this.riskScore = riskScore;
        }

        public Transaction getTransaction() {
            return transaction;
        }

        public double getRiskScore() {
            return riskScore;
        }

        @Override
        public int compareTo(RiskResult o) {
            // Ordena em ordem decrescente de score de risco
            return Double.compare(o.riskScore, this.riskScore);
        }

        @Override
        public String toString() {
            return String.format("TxID: %d | Step: %d | Risco: %.4f | Label Original: %s",
                    transaction.getTxId(),
                    transaction.getTimeStep(),
                    riskScore,
                    transaction.getLabel());
        }
    }

    /**
     * Executa o algoritmo de propagação de score de risco no grafo.
     * 
     * @param graph      Grafo de transações
     * @param iterations Número de iterações/passos de propagação (hops)
     * @param alpha      Peso dado às conexões de entrada (origem dos fundos).
     *                   Valores maiores dão mais peso para de onde os fundos vieram.
     *                   O peso para a saída será (1 - alpha).
     * @return Lista de resultados de risco ordenados do maior para o menor, apenas para nós UNKNOWN.
     */
    public static List<RiskResult> runPropagation(Graph graph, int iterations, double alpha) {
        if (graph == null || graph.nodeCount() == 0) {
            return Collections.emptyList();
        }

        // 1. Inicializa o mapa de scores atuais.
        Map<Long, Double> currentScores = new HashMap<>();
        for (Transaction node : graph.getNodes()) {
            long txId = node.getTxId();
            if (node.isIllicit()) {
                currentScores.put(txId, 1.0);
            } else if (node.isLicit()) {
                currentScores.put(txId, 0.0);
            } else {
                currentScores.put(txId, 0.0);
            }
        }

        // 2. Itera propagando os scores de risco.
        for (int step = 0; step < iterations; step++) {
            Map<Long, Double> nextScores = new HashMap<>(currentScores);

            for (Transaction node : graph.getNodes()) {
                // Mantém os scores das transações rotuladas (ground truth) inalterados
                if (node.getLabel() != Transaction.Label.UNKNOWN) {
                    continue;
                }

                long txId = node.getTxId();
                List<Long> inNeighbors = graph.getInNeighbors(txId);
                List<Long> outNeighbors = graph.getOutNeighbors(txId);

                // Média de risco dos nós que enviaram fundos para esta transação (in-neighbors)
                double sumIn = 0.0;
                int countIn = 0;
                for (long inId : inNeighbors) {
                    sumIn += currentScores.getOrDefault(inId, 0.0);
                    countIn++;
                }
                double avgIn = countIn > 0 ? sumIn / countIn : 0.0;

                // Média de risco dos nós que receberam fundos desta transação (out-neighbors)
                double sumOut = 0.0;
                int countOut = 0;
                for (long outId : outNeighbors) {
                    sumOut += currentScores.getOrDefault(outId, 0.0);
                    countOut++;
                }
                double avgOut = countOut > 0 ? sumOut / countOut : 0.0;

                // Combina os scores com base nos pesos atribuídos a entrada e saída
                double newScore;
                if (countIn > 0 && countOut > 0) {
                    newScore = alpha * avgIn + (1.0 - alpha) * avgOut;
                } else if (countIn > 0) {
                    newScore = avgIn;
                } else if (countOut > 0) {
                    newScore = avgOut;
                } else {
                    newScore = 0.0;
                }

                nextScores.put(txId, newScore);
            }

            currentScores = nextScores;
        }

        // 3. Constrói a lista de resultados apenas para os nós UNKNOWN.
        List<RiskResult> results = new ArrayList<>();
        for (Transaction node : graph.getNodes()) {
            if (node.getLabel() == Transaction.Label.UNKNOWN) {
                double score = currentScores.getOrDefault(node.getTxId(), 0.0);
                results.add(new RiskResult(node, score));
            }
        }

        // Ordena decrescentemente pelo score de risco
        Collections.sort(results);

        return results;
    }
}
