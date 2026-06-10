package model;

public class Transaction {

    public enum Label {
        ILLICIT, LICIT, UNKNOWN;

        public static Label fromString(String s) {
            return switch (s.trim()) {
                case "1"       -> ILLICIT;
                case "2"       -> LICIT;
                default        -> UNKNOWN;
            };
        }
    }

    private final long    txId;
    private final int     timeStep;
    private       Label   label;
    private final double[] features; // 166 features

    public Transaction(long txId, int timeStep, double[] features) {
        this.txId     = txId;
        this.timeStep = timeStep;
        this.features = features;
        this.label    = Label.UNKNOWN;
    }

    // -- Metodos Get
    public long     getTxId()     { return txId; }
    public int      getTimeStep() { return timeStep; }
    public Label    getLabel()    { return label; }
    public double[] getFeatures() { return features; }

    public boolean isIllicit() { return label == Label.ILLICIT; }
    public boolean isLicit()   { return label == Label.LICIT;   }

    public void setLabel(Label label) { this.label = label; }

    @Override
    public String toString() {
        return String.format("Transaction{id=%d, step=%d, label=%s}", txId, timeStep, label);
    }
}