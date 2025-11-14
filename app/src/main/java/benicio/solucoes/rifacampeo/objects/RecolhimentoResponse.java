package benicio.solucoes.rifacampeo.objects;

import java.util.List;

public class RecolhimentoResponse {
    public boolean success;
    public Filters filters;
    public Pagination pagination;
    public Sumario sumario;
    public List<RecolheuModel> itens;

    public static class Filters {
        public String vendedor;
        public String data_inicio;
        public String data_fim;
        public String tipo;
    }

    public static class Pagination {
        public int page;
        public int limit;
        public int returned;
        public int total;
    }

    public static class Sumario {
        public float soma_valor;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Filters getFilters() {
        return filters;
    }

    public void setFilters(Filters filters) {
        this.filters = filters;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public Sumario getSumario() {
        return sumario;
    }

    public void setSumario(Sumario sumario) {
        this.sumario = sumario;
    }

    public List<RecolheuModel> getItens() {
        return itens;
    }

    public void setItens(List<RecolheuModel> itens) {
        this.itens = itens;
    }
}