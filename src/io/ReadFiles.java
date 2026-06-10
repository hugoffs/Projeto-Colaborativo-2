package io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadFiles {
    private String fileNode;
    private ArrayList<Integer> conteudoNode;


    public ReadFiles(String arquivos1) throws IOException {
        this.fileNode = arquivos1;
        this.conteudoNode = new ArrayList<>();
        readFromId();
    }

    /**
     * Metodo que Realiza A leitura e capitura dos IDs para fazer a criação dos Nodes
     * @throws IOException
     */
    private void readFromId () throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(fileNode))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isBlank()) {
                    String[] colunas = line.split(",");
                    conteudoNode.add(Integer.parseInt(colunas[0].trim()));
                }
            }
        }
    }

    private void readFromEdgelist(){

    }

    public List<Integer> getConteudoNode() {
        return conteudoNode;
    }
}