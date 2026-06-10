package model;

/**
 * Representa um nó do grafo — uma transação Bitcoin do dataset Elliptic.
 *
 * <p>
 * Cada transação possui:
 * </p>
 * <ul>
 * <li>Um identificador único {@code txId} proveniente do blockchain.</li>
 * <li>Um {@code timeStep} (1–49) indicando em qual janela temporal
 * de ~2 semanas a transação foi transmitida à rede.</li>
 * <li>Um vetor de {@code features} com 166 atributos numéricos
 * (informações locais + agregadas de 1 hop).</li>
 * <li>Um {@link Label} classificando a transação como ilícita,
 * lícita ou desconhecida.</li>
 * </ul>
 *
 * <p>
 * Quando carregada a partir de um arquivo Pajek (sem os CSVs originais),
 * o vetor {@code features} pode estar vazio — as operações de grafo
 * (conectividade, centralidade, etc.) não dependem dele.
 * </p>
 *
 * @see model.Graph
 * @see io.EllipticLoader
 */
public class Transaction {

    // ── Enum ──────────────────────────────────────────────────────────────

    /**
     * Classificação da transação conforme o dataset Elliptic.
     *
     * <ul>
     * <li>{@link #ILLICIT} — class 1 no CSV: transação ilícita
     * (scam, ransomware, lavagem, etc.).</li>
     * <li>{@link #LICIT} — class 2 no CSV: transação lícita
     * (exchange, mineradora, serviço legítimo).</li>
     * <li>{@link #UNKNOWN} — sem rótulo no dataset (~77% dos nós).</li>
     * </ul>
     */
    public enum Label {
        ILLICIT,
        LICIT,
        UNKNOWN;

        /**
         * Converte o valor bruto do CSV para o enum correspondente.
         *
         * <p>
         * O dataset usa {@code "1"} para ilícito e {@code "2"} para lícito.
         * Qualquer outro valor (incluindo {@code "unknown"}) é mapeado
         * para {@link #UNKNOWN}.
         * </p>
         *
         * @param s string lida diretamente do CSV
         * @return o Label correspondente; nunca {@code null}
         */
        public static Label fromString(String s) {
            return switch (s.trim()) {
                case "1" -> ILLICIT;
                case "2" -> LICIT;
                default -> UNKNOWN;
            };
        }
    }

    // ── Campos ────────────────────────────────────────────────────────────

    /** Identificador único da transação no blockchain Bitcoin. */
    private final long txId;

    /**
     * Janela temporal da transação (1 a 49).
     * Cada step cobre ~2 semanas; não existem arestas entre steps distintos.
     */
    private final int timeStep;

    /**
     * Classificação da transação.
     * Mutável para permitir atualização pós-carregamento (ex: aplicar
     * resultados de um classificador sobre nós {@link Label#UNKNOWN}).
     */
    private Label label;

    /**
     * Vetor de 166 features numéricas.
     * As primeiras 94 são locais (volume, taxas, nº de inputs/outputs);
     * as 72 restantes são agregadas do entorno de 1 hop (max, min, desvio).
     * Pode ser {@code double[0]} quando carregado via Pajek.
     */
    private final double[] features;

    // ── Construtor ────────────────────────────────────────────────────────

    /**
     * Cria uma transação com label inicial {@link Label#UNKNOWN}.
     * Use {@link #setLabel(Label)} para atribuir a classificação
     * após ler o arquivo de classes.
     *
     * @param txId     identificador único da transação
     * @param timeStep janela temporal (1–49)
     * @param features vetor de features; use {@code new double[0]} se ausente
     */
    public Transaction(long txId, int timeStep, double[] features) {
        this.txId = txId;
        this.timeStep = timeStep;
        this.features = features;
        this.label = Label.UNKNOWN;
    }

    // ── Getters ───────────────────────────────────────────────────────────

    /**
     * @return identificador único da transação no blockchain
     */
    public long getTxId() {
        return txId;
    }

    /**
     * @return janela temporal (1–49) em que a transação foi transmitida
     */
    public int getTimeStep() {
        return timeStep;
    }

    /**
     * @return classificação atual da transação
     */
    public Label getLabel() {
        return label;
    }

    /**
     * @return vetor de 166 features; pode ser {@code double[0]} se
     *         a transação foi carregada de um arquivo Pajek
     */
    public double[] getFeatures() {
        return features;
    }

    // ── Atalhos de classificação ──────────────────────────────────────────

    /**
     * Verifica se a transação é classificada como ilícita (class 1).
     *
     * @return {@code true} se {@link Label#ILLICIT}
     */
    public boolean isIllicit() {
        return label == Label.ILLICIT;
    }

    /**
     * Verifica se a transação é classificada como lícita (class 2).
     *
     * @return {@code true} se {@link Label#LICIT}
     */
    public boolean isLicit() {
        return label == Label.LICIT;
    }

    // ── Setter ────────────────────────────────────────────────────────────

    /**
     * Atualiza a classificação da transação.
     * Usado pelo {@link io.EllipticLoader} ao aplicar o mapa de classes,
     * e pelo {@link io.PajekReader} ao reconstruir nós de um arquivo .net.
     *
     * @param label novo rótulo; não deve ser {@code null}
     */
    public void setLabel(Label label) {
        this.label = label;
    }

    // ── Utilitários ───────────────────────────────────────────────────────

    /**
     * @return representação legível no formato
     *         {@code Transaction{id=..., step=..., label=...}}
     */
    @Override
    public String toString() {
        return String.format("Transaction{id=%d, step=%d, label=%s}",
                txId, timeStep, label);
    }
}