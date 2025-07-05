package benicio.solucoes.rifacampeo.objects;

import java.util.ArrayList;
import java.util.List;

public class BilheteModel {
    String _id, data, hora, id_usuario;
    List<Integer> numeros = new ArrayList<>();

    @Override
    public String toString() {

        StringBuilder numerosEscolhidos = new StringBuilder();
        int valor = 0;
        for (int numero : numeros) {
            numerosEscolhidos.append(numero + " ");
            valor += 1;
        }

        return
                "<b>ID:</b><br>" + _id + "<br>" +
                        "<b>Data: </b><br>" + data + "<br>" +
                        "<b>Hora: </b><br>" + hora + "<br>" +
                        "<b>Numeros: </b><br>" + numerosEscolhidos.toString() + "<br>" +
                        "<b>Valor: </b><br>R$" + valor*10 + ",00";
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

    public BilheteModel(String _id, String data, String hora, String id_usuario, List<Integer> numeros) {
        this._id = _id;
        this.data = data;
        this.hora = hora;
        this.id_usuario = id_usuario;
        this.numeros = numeros;
    }
}
