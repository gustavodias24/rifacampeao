package benicio.solucoes.rifacampeo.objects;

public class TotalRecolhimentoPagamento {
    float totalRecolhido = 0.0f;
    float totalPagamento = 0.0f;

    public TotalRecolhimentoPagamento(float totalRecolhido, float totalPagamento) {
        this.totalRecolhido = totalRecolhido;
        this.totalPagamento = totalPagamento;
    }

    public float getTotalRecolhido() {
        return totalRecolhido;
    }

    public void setTotalRecolhido(float totalRecolhido) {
        this.totalRecolhido = totalRecolhido;
    }

    public float getTotalPagamento() {
        return totalPagamento;
    }

    public void setTotalPagamento(float totalPagamento) {
        this.totalPagamento = totalPagamento;
    }
}
