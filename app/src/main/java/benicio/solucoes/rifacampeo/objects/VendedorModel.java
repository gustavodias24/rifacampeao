package benicio.solucoes.rifacampeo.objects;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import benicio.solucoes.rifacampeo.RelatoriosActivity;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VendedorModel {

    String serial;
    String numeroCelular, nome, _id, senha, despesas, idSmartphone = "", documento = "";
    int comissao;
    boolean ativado = true;

    float recebimento = 0.0f, pagamento = 0.0f;

    int info = 1;


    public VendedorModel() {
    }

    String totalFmt, comissaoFmt,saldoFmt ="";

    public float calcularSaldoPorVendedor(List<RecolheuModel> recolhimentos) {
        if (recolhimentos == null || recolhimentos.isEmpty()) {
            return 0f;
        }

        float totalRecolhido = 0f; // tipo 0
        float totalPago = 0f;      // tipo 1

        for (RecolheuModel r : recolhimentos) {
            if (r == null) continue;

            String vend = r.getVendedor();
            if (vend == null) continue;

            // compara ignorando maiúsculas/minúsculas
            if (!vend.equalsIgnoreCase(getNome())) {
                continue;
            }

            // tipo: 0 = Recolhimento | 1 = Pagamento
            if (r.getTipo() == 0) {
                totalRecolhido += r.getValor();
            } else if (r.getTipo() == 1) {
                totalPago += r.getValor();
            }
        }

        return totalRecolhido - totalPago;
    }

    public String toStringVendedor(List<RecolheuModel> recolhimentos) {

        float saldoTotal = calcularSaldoPorVendedor(recolhimentos);
        float comissaoGanha = (saldoTotal * comissao) / 100f;
        float saldo = saldoTotal - comissaoGanha;

        Locale ptBr = new Locale("pt", "BR");
        totalFmt = String.format(ptBr, "R$ %.2f", saldoTotal);
        comissaoFmt = String.format(ptBr, "R$ %.2f", comissaoGanha);
        saldoFmt = String.format(ptBr, "R$ %.2f", saldo);

        return "<h2>" + nome + "</h2>" +
                "<br>" +
                "<b>Senha:</b> " + senha + "<br>" +
                "<b>Despesas:</b> " + despesas + "<br>" +
                "<b>Comissão:</b> " + comissao + "%<br>" +
                "<b>Ativado:</b> " + ativado + "<br>" +
                "<b>Número Celular:</b> " + numeroCelular + "<br><br>" +

                // Linha menor para não quebrar
                "<small>Total " + totalFmt + " | Comissão " + comissaoFmt + "</small><br>" +

                // Saldo maior e em negrito
                "<b><big>Saldo " + saldoFmt + "</big></b><br>";
    }

    public VendedorModel(String numeroCelular, String nome, String id, String senha, String despesas, String idSmartphone, int comissao, boolean ativado, String documento) {
        this.numeroCelular = numeroCelular;
        this.nome = nome;
        this._id = id;
        this.senha = senha;
        this.despesas = despesas;
        this.idSmartphone = idSmartphone;
        this.comissao = comissao;
        this.ativado = ativado;
        this.documento = documento;
    }

    public String getTotalFmt() {
        return totalFmt;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public void setTotalFmt(String totalFmt) {
        this.totalFmt = totalFmt;
    }

    public String getComissaoFmt() {
        return comissaoFmt;
    }

    public void setComissaoFmt(String comissaoFmt) {
        this.comissaoFmt = comissaoFmt;
    }

    public String getSaldoFmt() {
        return saldoFmt;
    }

    public void setSaldoFmt(String saldoFmt) {
        this.saldoFmt = saldoFmt;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }


    public float getPagamento() {
        return pagamento;
    }

    public void setPagamento(float pagamento) {
        this.pagamento = pagamento;
    }

    public float getRecebimento() {
        return recebimento;
    }

    public void setRecebimento(float recebimento) {
        this.recebimento = recebimento;
    }

    public String getNumeroCelular() {
        return numeroCelular;
    }

    public void setNumeroCelular(String numeroCelular) {
        this.numeroCelular = numeroCelular;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public int getInfo() {
        return info;
    }

    public void setInfo(int info) {
        this.info = info;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getDespesas() {
        return despesas;
    }

    public void setDespesas(String despesas) {
        this.despesas = despesas;
    }

    public String getIdSmartphone() {
        return idSmartphone;
    }

    public void setIdSmartphone(String idSmartphone) {
        this.idSmartphone = idSmartphone;
    }

    public int getComissao() {
        return comissao;
    }

    public void setComissao(int comissao) {
        this.comissao = comissao;
    }

    public boolean isAtivado() {
        return ativado;
    }

    public void setAtivado(boolean ativado) {
        this.ativado = ativado;
    }
}
