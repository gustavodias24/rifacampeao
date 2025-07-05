package benicio.solucoes.rifacampeo.objects;

public class VendedorModel {

    String numeroCelular, nome, _id, senha, despesas, idSmartphone = "";
    int comissao;
    boolean ativado = true;

    float recebimento = 0.0f, pagamento = 0.0f;

    int info = 1;
    public VendedorModel() {
    }

    @Override
    public String toString() {

    return  "<h2>"+nome+"</h2><br>" +
            "<b>Senha: <b>"+ senha + "<br>" +
            "<b>Despesas: <b>"+ despesas + "<br>" +
            "<b>Comissão: <b>"+ comissao + "<br>" +
            "<b>Ativado: <b>"+ ativado + "<br>" +
            "<b>Número Celular: <b>"+ numeroCelular + "<br>" +
            "<b>Saldo: <b><h1>R$ " +  String.valueOf(recebimento - pagamento).replace(".", ",") + "</h1><br>";
            // + "<b>ID: <b>"+ idSmartphone + "<br>" ;
    }

    public VendedorModel(String numeroCelular, String nome, String id, String senha, String despesas, String idSmartphone, int comissao, boolean ativado) {
        this.numeroCelular = numeroCelular;
        this.nome = nome;
        this._id = id;
        this.senha = senha;
        this.despesas = despesas;
        this.idSmartphone = idSmartphone;
        this.comissao = comissao;
        this.ativado = ativado;
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
