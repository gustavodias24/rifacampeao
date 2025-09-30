package benicio.solucoes.rifacampeo.objects;

public class RetornoModel {
    String msg;
    Boolean success = false;

    public RetornoModel() {
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMsg() {

        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
