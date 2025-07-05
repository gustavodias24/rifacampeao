package benicio.solucoes.rifacampeo.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.UUID;

import benicio.solucoes.rifacampeo.R;
import benicio.solucoes.rifacampeo.RegioesActivity;
import benicio.solucoes.rifacampeo.databinding.LayoutInputRegiaoBinding;
import benicio.solucoes.rifacampeo.objects.RegiaoModel;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdapterRegiao extends RecyclerView.Adapter<AdapterRegiao.MyViewHolder> {

    Dialog novaRegiaoDialog;
    List<RegiaoModel> list;
    Activity a;

    public AdapterRegiao(List<RegiaoModel> list, Activity a) {
        this.list = list;
        this.a = a;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_vendedor, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.infos_vendedor.setText(Html.fromHtml("<h1>" + list.get(position).getNome() + "</h1>"));

        holder.editar_vendedor.setOnClickListener(v -> {

            AlertDialog.Builder b = new AlertDialog.Builder(a);

            LayoutInputRegiaoBinding inputRegiaoBinding = LayoutInputRegiaoBinding.inflate(a.getLayoutInflater());

            inputRegiaoBinding.cadatrar.setText("Editar");

            inputRegiaoBinding.edtNome.setText(list.get(position).getNome());
            inputRegiaoBinding.cadatrar.setOnClickListener(v2 -> {
                String nome = inputRegiaoBinding.edtNome.getText().toString();
                if (nome.isEmpty()) {
                    Toast.makeText(a, "Nome é obrigatório", Toast.LENGTH_SHORT).show();
                } else {
                    RetrofitUtils.getApiService().saveRegiao(new RegiaoModel(list.get(position).get_id(), nome)).enqueue(new Callback<RetornoModel>() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(a, "Editado com sucesso!", Toast.LENGTH_SHORT).show();
                                novaRegiaoDialog.dismiss();
                                list.get(position).setNome(nome);
                                notifyDataSetChanged();
                            } else {
                                Toast.makeText(a, "Problema de conexão", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<RetornoModel> call, Throwable throwable) {

                        }
                    });
                }
            });

            b.setView(inputRegiaoBinding.getRoot());
            novaRegiaoDialog = b.create();
            novaRegiaoDialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView infos_vendedor;
        Button editar_vendedor;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            infos_vendedor = itemView.findViewById(R.id.infos_vendedor);
            editar_vendedor = itemView.findViewById(R.id.editar_vendedor);
        }
    }
}
