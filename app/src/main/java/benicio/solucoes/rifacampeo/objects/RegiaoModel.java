package benicio.solucoes.rifacampeo.objects;

public class RegiaoModel {
    String nome, _id;
    int info = 2;

    public RegiaoModel(String _id, String nome) {
        this._id = _id;
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
