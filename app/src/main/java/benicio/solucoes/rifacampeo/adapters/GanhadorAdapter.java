package benicio.solucoes.rifacampeo.adapters;

// GanhadorAdapter.java
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import benicio.solucoes.rifacampeo.R;
import benicio.solucoes.rifacampeo.objects.GanhadorModel;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GanhadorAdapter extends RecyclerView.Adapter<GanhadorAdapter.VH> implements Filterable {

    private final List<GanhadorModel> items = new ArrayList<>();     // original
    private final List<GanhadorModel> filtered = new ArrayList<>();  // exibida

    public void setItems(List<GanhadorModel> novos) {
        items.clear();
        if (novos != null) items.addAll(novos);
        filtered.clear();
        filtered.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ganhador, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        GanhadorModel g = filtered.get(pos);
        h.tvLoteria.setText(g.getLoteria() != null ? g.getLoteria() : "-");
        h.tvData.setText(g.getData_lancada() != null ? g.getData_lancada() : "-");
        String numeros = String.format("%d, %d, %d, %d, %d, %d",
                g.getNumero1(), g.getNumero2(), g.getNumero3(),
                g.getNumero4(), g.getNumero5(), g.getNumero6());
        h.tvNumeros.setText(numeros);

        h.btnDeletar.setOnClickListener(v -> {
            // posição sempre atual
            int position = h.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;

            GanhadorModel item = filtered.get(position);
            RetrofitUtils.getApiService().ganhador_delete(item.get_id())
                    .enqueue(new Callback<RetornoModel>() {
                        @Override
                        public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                            GanhadorModel removed = filtered.remove(position);
                            items.remove(removed);
                            notifyItemRemoved(position);
                        }

                        @Override
                        public void onFailure(Call<RetornoModel> call, Throwable throwable) {

                        }
                    });
        });
    }

    @Override public int getItemCount() { return filtered.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLoteria, tvData, tvNumeros;
        ImageButton btnDeletar;
        VH(@NonNull View itemView) {
            super(itemView);
            tvLoteria = itemView.findViewById(R.id.tvLoteria);
            tvData    = itemView.findViewById(R.id.tvData);
            tvNumeros = itemView.findViewById(R.id.tvNumeros);
            btnDeletar = itemView.findViewById(R.id.btnDelete);
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override protected FilterResults performFiltering(CharSequence constraint) {
                String q = constraint != null ? constraint.toString().trim().toLowerCase() : "";
                List<GanhadorModel> out = new ArrayList<>();
                if (TextUtils.isEmpty(q)) {
                    out.addAll(items);
                } else {
                    for (GanhadorModel g : items) {
                        String lot = g.getLoteria() != null ? g.getLoteria().toLowerCase() : "";
                        String dat = g.getData_lancada() != null ? g.getData_lancada().toLowerCase() : "";
                        String nums = (g.getNumero1()+" "+g.getNumero2()+" "+g.getNumero3()+" "+
                                g.getNumero4()+" "+g.getNumero5()+" "+g.getNumero6()).toLowerCase();
                        if (lot.contains(q) || dat.contains(q) || nums.contains(q)) {
                            out.add(g);
                        }
                    }
                }
                FilterResults fr = new FilterResults();
                fr.values = out;
                return fr;
            }

            @Override @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filtered.clear();
                filtered.addAll((List<GanhadorModel>) results.values);
                notifyDataSetChanged();
            }
        };
    }
}
