package benicio.solucoes.rifacampeo.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import benicio.solucoes.rifacampeo.MakeSorteioActivity;
import benicio.solucoes.rifacampeo.R;

public class AdapterNumero extends RecyclerView.Adapter<AdapterNumero.MyViewHolder> {

    List<String> numeros;
    Activity a;

    public AdapterNumero(List<String> numeros, Activity a) {
        this.numeros = numeros;
        this.a = a;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_numeros, parent, false));
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.numero.setText(numeros.get(position));

        holder.itemView.setOnClickListener(v -> {
            numeros.remove(position);
            notifyDataSetChanged();
            MakeSorteioActivity.atualizarPrecoBilhete(0);
        });
    }

    @Override
    public int getItemCount() {
        return numeros.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView numero;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            numero = itemView.findViewById(R.id.textViewNumero);
        }
    }
}
