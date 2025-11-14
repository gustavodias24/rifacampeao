package benicio.solucoes.rifacampeo.objects;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecolheuModel {

    private String dataHoraAtual;
    private String vendedor;
    private float valor;
    private String observacoes;
    private int tipo;

    // Construtor padrão: já seta a data/hora atual
    public RecolheuModel() {
        this.dataHoraAtual = getDataHoraAgora();
    }

    public RecolheuModel(String dataHoraAtual, String vendedor, float valor, String observacoes, int tipo) {
        this.dataHoraAtual = dataHoraAtual;
        this.vendedor = vendedor;
        this.valor = valor;
        this.observacoes = observacoes;
        this.tipo = tipo;
    }

    public String toStringHtml() {
        Locale ptBr = new Locale("pt", "BR");
        String valorFmt = String.format(ptBr, "R$ %.2f", valor);

        String tipoDesc = esc(getTipoDescricao());
        String vendedorStr = esc(vendedor != null ? vendedor : "-");
        String dataStr = esc(dataHoraAtual != null ? dataHoraAtual : "-");

        StringBuilder sb = new StringBuilder();

        sb.append("<b>Tipo:</b> ").append(tipoDesc).append("<br>");
        sb.append("<b>Vendedor:</b> ").append(vendedorStr).append("<br>");
        sb.append("<b>Valor:</b> ").append(valorFmt).append("<br>");
        sb.append("<b>Data:</b> ").append(dataStr).append("<br>");

        if (observacoes != null && !observacoes.trim().isEmpty()) {
            sb.append("<b>Observações:</b> ")
                    .append(esc(observacoes.trim()))
                    .append("<br>");
        }

        return sb.toString();
    }


    private String getTipoDescricao() {
        switch (tipo) {
            case 0: return "Recolhimento";
            case 1: return "Pagamento";
            default: return "Tipo " + tipo;
        }
    }
    private String esc(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String getDataHoraAgora() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    public String getDataHoraAtual() {
        return dataHoraAtual;
    }

    public int getTipo() {
        return tipo;
    }

    public void setTipo(int tipo) {
        this.tipo = tipo;
    }

    public void setDataHoraAtual(String dataHoraAtual) {
        this.dataHoraAtual = dataHoraAtual;
    }

    public String getVendedor() {
        return vendedor;
    }

    public void setVendedor(String vendedor) {
        this.vendedor = vendedor;
    }

    public float getValor() {
        return valor;
    }

    public void setValor(float valor) {
        this.valor = valor;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
