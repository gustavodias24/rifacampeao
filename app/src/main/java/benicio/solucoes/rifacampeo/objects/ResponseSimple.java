package benicio.solucoes.rifacampeo.objects;

public class ResponseSimple {
    boolean success;
    String msg;

    VendedorModel vendedor = new VendedorModel();


    public VendedorModel getVendedor() {
        return vendedor;
    }

    public void setVendedor(VendedorModel vendedor) {
        this.vendedor = vendedor;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public ResponseSimple() {
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
