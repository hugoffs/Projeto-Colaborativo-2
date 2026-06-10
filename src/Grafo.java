import structures.linkedList.LinkedList;

public class Grafo{

    private int tamanho;
    private LinkedList[] adjacentes;
    private String[] names;

    public Grafo(int tamanho){
        this.tamanho = tamanho;
        adjacentes = new LinkedList[tamanho];
        names = new String[tamanho];

        for(int i = 0; i < tamanho; i++){
            adjacentes[i] = new LinkedList();
        }
    }

}
