package structures.linkedList;

public class Node {
    private int id;
    private int n_trasazoes;

    public Node(int id) {
        this.id = id;
        this.n_trasazoes = 0 ;
    }

    public void tranzacao(){
        n_trasazoes++;
    }
    public int getId() {
        return id;
    }
    public int getN_trasazoes() {
        return n_trasazoes;
    }

}
