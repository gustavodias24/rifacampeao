package benicio.solucoes.rifacampeo.adapters;

import android.app.Activity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import benicio.solucoes.rifacampeo.R;
import benicio.solucoes.rifacampeo.objects.LancamentoModel;

public class AdapterRelatorio extends RecyclerView.Adapter<AdapterRelatorio.MyViewHolder> {

    List<LancamentoModel> lancamentos;
    Activity a;

    public AdapterRelatorio(List<LancamentoModel> lancamentos, Activity a) {
        this.lancamentos = lancamentos;
        this.a = a;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_lancamento, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.textViewLancamento.setText(
                Html.fromHtml(lancamentos.get(position).toString())
        );
    }

    @Override
    public int getItemCount() {
        return lancamentos.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView textViewLancamento;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewLancamento = itemView.findViewById(R.id.textViewLancamento);
        }
    }
}
