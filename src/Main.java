import io.ReadFiles;
import structures.linkedList.LinkedList;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        String Arquivo1 = "C:\\Users\\hugoffs\\Desktop\\PUC\\5 Semestre\\Grafo\\Atividade Coloborativa 2\\Atividade Coloborativa 2";


        String teste = "filtered_features.csv.xls";

        ReadFiles ellipticLoader = new ReadFiles(teste);

        System.out.print("teste Funcionou ");

        int size = ellipticLoader.getConteudoNode().size();
        System.out.println(size);


    }

    public static class Grafo{
        private String[] names;
        private LinkedList[] adjList;;
        private int tamanho;


        public Grafo(int tamanho){
            this.tamanho = tamanho;
            names = new String[tamanho];
            adjList = new LinkedList[tamanho];
        }


    }
}