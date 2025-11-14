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
import benicio.solucoes.rifacampeo.objects.RecolheuModel;

public class AdapterRecolhimento extends RecyclerView.Adapter<AdapterRecolhimento.MyViewHolder> {

    List<RecolheuModel> lista;
    Activity a;

    public AdapterRecolhimento(List<RecolheuModel> lista, Activity a) {
        this.lista = lista;
        this.a = a;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_recolhimento, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.infos_recolhimento.setText(
                Html.fromHtml(lista.get(position).toStringHtml())
        );
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView infos_recolhimento;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            infos_recolhimento = itemView.findViewById(R.id.infos_recolhimento);
        }
    }
}
