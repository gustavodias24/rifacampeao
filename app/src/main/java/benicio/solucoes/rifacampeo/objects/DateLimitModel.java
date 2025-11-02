package benicio.solucoes.rifacampeo.objects;

public class DateLimitModel {
    String dataHoraFD;
    String dataHoraCOR;

    String valorRifa = "";

    public DateLimitModel() {
    }

    public String getValorRifa() {
        return valorRifa;
    }

    public void setValorRifa(String valorRifa) {
        this.valorRifa = valorRifa;
    }

    public String getDataHoraFD() {
        return dataHoraFD;
    }

    public void setDataHoraFD(String dataHoraFD) {
        this.dataHoraFD = dataHoraFD;
    }

    public String getDataHoraCOR() {
        return dataHoraCOR;
    }

    public void setDataHoraCOR(String dataHoraCOR) {
        this.dataHoraCOR = dataHoraCOR;
    }
}
