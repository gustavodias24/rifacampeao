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
import benicio.solucoes.rifacampeo.objects.BilheteModel;

public class AdapterBilhetes extends RecyclerView.Adapter<AdapterBilhetes.MyViewHolder> {

    Activity a;
    List<BilheteModel> bilhetes;

    public AdapterBilhetes(Activity a, List<BilheteModel> bilhetes) {
        this.a = a;
        this.bilhetes = bilhetes;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_bilhete, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.text_info_bilhete.setText(
                Html.fromHtml(
                        bilhetes.get(position).toString()
                )
        );
    }

    @Override
    public int getItemCount() {
        return bilhetes.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView text_info_bilhete;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            text_info_bilhete = itemView.findViewById(R.id.text_info_bilhete);
        }
    }
}
