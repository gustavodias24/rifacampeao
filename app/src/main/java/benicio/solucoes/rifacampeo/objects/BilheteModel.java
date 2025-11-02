package benicio.solucoes.rifacampeo.objects;

import java.util.ArrayList;
import java.util.List;

public class BilheteModel {

    // data no formato dd/MM/yyyy
    String _id, data , hora, id_usuario;
    String documento_vendedor, nome_vendedor;
    List<Integer> numeros = new ArrayList<>();

    // usado para checar se o número do bilhete for repetido
    String numero;
    String loteria;

    @Override
    public String toString() {
        // Safeguards contra null
        String sId = _id == null ? "" : _id;
        String sData = data == null ? "" : data;
        String sHora = hora == null ? "" : hora;
        String sUsuario = id_usuario == null ? "" : id_usuario;
        String sDocVend = documento_vendedor == null ? "" : documento_vendedor;
        String sNomeVend = nome_vendedor == null ? "" : nome_vendedor;
        String sNumeroBilhete = numero == null ? "" : numero;
        String sLoteria = loteria == null ? "" : loteria;

        // Números escolhidos (ordenados e separados por espaço)
        StringBuilder numerosEscolhidos = new StringBuilder();
        int qtd = 0;
        if (numeros != null && !numeros.isEmpty()) {
            List<Integer> sorted = new ArrayList<>(numeros);
            java.util.Collections.sort(sorted);
            for (int i = 0; i < sorted.size(); i++) {
                numerosEscolhidos.append(sorted.get(i));
                if (i < sorted.size() - 1) numerosEscolhidos.append(" ");
            }
            qtd = sorted.size();
        }

        // Valor total (ex.: R$ 10,00 por número)
        final int VALOR_UNITARIO = 10; // ajuste se necessário
        double total = qtd * VALOR_UNITARIO;
        String valorFormatado = String.format(new java.util.Locale("pt", "BR"), "R$ %.2f", total);

        // Monta HTML
        return
                "<b>ID:</b><br>" + sId + "<br>" +
                        "<b>Data:</b><br>" + sData + "<br>" +
                        "<b>Hora:</b><br>" + sHora + "<br>" +
                        "<b>Loteria:</b><br>" + sLoteria + "<br>" +
                        "<b>Nº do Bilhete:</b><br>" + sNumeroBilhete + "<br>" +
                        "<b>Documento do Vendedor:</b><br>" + sDocVend + "<br>" +
                        "<b>Nome do Vendedor:</b><br>" + sNomeVend + "<br>" +
                        "<b>ID do Usuário:</b><br>" + sUsuario + "<br>" +
                        "<b>Números (" + qtd + "):</b><br>" + (numerosEscolhidos.length() == 0 ? "-" : numerosEscolhidos.toString()) + "<br>" +
                        "<b>Valor Total:</b><br>" + valorFormatado;
    }


    public BilheteModel(String numero, String loteria) {
        this.numero = numero;
        this.loteria = loteria;
    }

    public String getLoteria() {
        return loteria;
    }

    public void setLoteria(String loteria) {
        this.loteria = loteria;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getNome_vendedor() {
        return nome_vendedor;
    }

    public void setNome_vendedor(String nome_vendedor) {
        this.nome_vendedor = nome_vendedor;
    }

    public String getDocumento_vendedor() {
        return documento_vendedor;
    }

    public void setDocumento_vendedor(String documento_vendedor) {
        this.documento_vendedor = documento_vendedor;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getId_usuario() {
        return id_usuario;
    }

    public void setId_usuario(String id_usuario) {
        this.id_usuario = id_usuario;
    }

    public List<Integer> getNumeros() {
        return numeros;
    }

    public void setNumeros(List<Integer> numeros) {
        this.numeros = numeros;
    }

    public BilheteModel() {
    }

    public BilheteModel(String _id, String data, String hora, String id_usuario, List<Integer> numeros, String documento_vendedor,String nome_vendedor) {
        this._id = _id;
        this.data = data;
        this.hora = hora;
        this.id_usuario = id_usuario;
        this.numeros = numeros;
        this.documento_vendedor = documento_vendedor;
        this.nome_vendedor = nome_vendedor;
    }
}
