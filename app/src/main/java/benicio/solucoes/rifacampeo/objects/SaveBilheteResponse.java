package benicio.solucoes.rifacampeo.objects;

public class SaveBilheteResponse {
    String msg;
    boolean success;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public SaveBilheteResponse() {
    }

    public SaveBilheteResponse(String msg, boolean success) {
        this.msg = msg;
        this.success = success;
    }
}
