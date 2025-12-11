package benicio.solucoes.rifacampeo.objects;

public class ResponseSimple {
    boolean success;
    String msg;

    VendedorModel vendedor = new VendedorModel();

    float valorTotalGeradoCOR = 0;
    float valorTotalGeradoDF = 0;

    float valorTotalGeradoDFLoteriaAtual = 0;
    float valorTotalGeradoCORLoteriaAtual = 0;



    public VendedorModel getVendedor() {
        return vendedor;
    }

    public void setVendedor(VendedorModel vendedor) {
        this.vendedor = vendedor;
    }

    public float getValorTotalGeradoDFLoteriaAtual() {
        return valorTotalGeradoDFLoteriaAtual;
    }

    public void setValorTotalGeradoDFLoteriaAtual(float valorTotalGeradoDFLoteriaAtual) {
        this.valorTotalGeradoDFLoteriaAtual = valorTotalGeradoDFLoteriaAtual;
    }

    public float getValorTotalGeradoCORLoteriaAtual() {
        return valorTotalGeradoCORLoteriaAtual;
    }

    public void setValorTotalGeradoCORLoteriaAtual(float valorTotalGeradoCORLoteriaAtual) {
        this.valorTotalGeradoCORLoteriaAtual = valorTotalGeradoCORLoteriaAtual;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public ResponseSimple() {
    }

    public float getValorTotalGeradoCOR() {
        return valorTotalGeradoCOR;
    }

    public void setValorTotalGeradoCOR(float valorTotalGeradoCOR) {
        this.valorTotalGeradoCOR = valorTotalGeradoCOR;
    }

    public float getValorTotalGeradoDF() {
        return valorTotalGeradoDF;
    }

    public void setValorTotalGeradoDF(float valorTotalGeradoDF) {
        this.valorTotalGeradoDF = valorTotalGeradoDF;
    }

    public ResponseSimple(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
