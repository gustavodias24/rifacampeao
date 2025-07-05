package benicio.solucoes.rifacampeo.objects;

public class LancamentoModel {
    String valor, descricao, data, hora;

    String _id, vendedor_id;

    boolean receita;

    String info = "4";

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder();
        info.append("<b>").append("Descrição: ").append("</b><br>").append(descricao).append("<br>");
        info.append("<b>").append("Valor: ").append("</b>").append(valor).append("<br>");
        info.append("<b>").append("Data e Hora: ").append("</b>").append(data + " às " + hora).append("<br>");

        String despesaString = "<font color='red'>Despesa</font>";
        String receitaString = "<font color='green'>Receita</font>";

        info.append("<b>Tipo: ")
                .append(receita ? receitaString : despesaString)
                .append("<br>");

        return info.toString();
    }

    public LancamentoModel(String valor, String descricao, String data, String hora, String _id, String vendedor_id, boolean receita) {
        this.valor = valor;
        this.descricao = descricao;
        this.data = data;
        this.hora = hora;
        this._id = _id;
        this.vendedor_id = vendedor_id;
        this.receita = receita;
    }

    public String getVendedor_id() {
        return vendedor_id;
    }

    public void setVendedor_id(String vendedor_id) {
        this.vendedor_id = vendedor_id;
    }

    public boolean isReceita() {
        return receita;
    }

    public void setReceita(boolean receita) {
        this.receita = receita;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
