package benicio.solucoes.rifacampeo.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
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

import benicio.solucoes.rifacampeo.PagamentoRecebimentoVendedorActivity;
import benicio.solucoes.rifacampeo.R;
import benicio.solucoes.rifacampeo.VendedoresActivity;
import benicio.solucoes.rifacampeo.databinding.ActivityPagamentoRecebimentoVendedorBinding;
import benicio.solucoes.rifacampeo.databinding.DialogPagamentoRecebimentoBinding;
import benicio.solucoes.rifacampeo.databinding.LayoutInputVendedorBinding;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.objects.VendedorModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdapterVendedores extends RecyclerView.Adapter<AdapterVendedores.MyViewHolder> {

    int tipo = 0;
    private Dialog dialogVendedor;

    List<VendedorModel> lista;
    Activity a;

    Dialog d_pagamento;

    public AdapterVendedores(List<VendedorModel> lista, Activity a) {
        this.lista = lista;
        this.a = a;
    }

    InputFilter filter = new InputFilter() {
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {

            for (int i = start; i < end; i++) {
                char caracter = source.charAt(i);
                if (!Character.isDigit(caracter) && caracter != '.' && caracter != ',') {
                    return "";
                }
            }
            return null;
        }
    };

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_vendedor, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        holder.entrada_saida_vendedor.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(a);

            DialogPagamentoRecebimentoBinding dialogPagamentoRecebimentoBinding = DialogPagamentoRecebimentoBinding.inflate(a.getLayoutInflater());
            dialogPagamentoRecebimentoBinding.acertPagamento.setOnClickListener(v2 -> {
                dialogPagamentoRecebimentoBinding.layoutPagamentoRecebimento.setVisibility(View.GONE);
                dialogPagamentoRecebimentoBinding.layoutValor.setVisibility(View.VISIBLE);
                tipo = 0;
            });

            dialogPagamentoRecebimentoBinding.acertRecebimento.setOnClickListener(v2 -> {
                dialogPagamentoRecebimentoBinding.layoutPagamentoRecebimento.setVisibility(View.GONE);
                dialogPagamentoRecebimentoBinding.layoutValor.setVisibility(View.VISIBLE);
                tipo = 1;
            });

            dialogPagamentoRecebimentoBinding.editTextText.setFilters(new InputFilter[]{filter});

            builder.setView(dialogPagamentoRecebimentoBinding.getRoot());

            dialogPagamentoRecebimentoBinding.confirmar.setOnClickListener(v2 -> {
                if (tipo == 0) {
                    lista.get(position).setPagamento(Float.parseFloat(
                            dialogPagamentoRecebimentoBinding.editTextText.getText().toString().replace(',', '.')
                    ) + lista.get(position).getPagamento());
                } else {
                    lista.get(position).setRecebimento(Float.parseFloat(
                            dialogPagamentoRecebimentoBinding.editTextText.getText().toString().replace(',', '.')
                    ) + lista.get(position).getRecebimento());
                }
                RetrofitUtils.getApiService().saveVendedores(lista.get(position)).enqueue(new Callback<>() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                        notifyDataSetChanged();
                        d_pagamento.dismiss();
                    }

                    @Override
                    public void onFailure(Call<RetornoModel> call, Throwable throwable) {
                        d_pagamento.dismiss();
                    }
                });

            });
            d_pagamento = builder.create();

            d_pagamento.show();


//            Intent i = new Intent(a, PagamentoRecebimentoVendedorActivity.class);
//            i.putExtra("NomeVendedor",
//                    lista.get(position).getNome()).putExtra("VendedorId",
//                    lista.get(position).get_id());
//            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            a.startActivity(i);
        });

        holder.infos_vendedor.setText(
                Html.fromHtml(lista.get(position).toString())
        );

        holder.editar_vendedor.setOnClickListener(v -> {
            AlertDialog.Builder b = new AlertDialog.Builder(a);

            LayoutInputVendedorBinding inputVendedorBinding = LayoutInputVendedorBinding.inflate(a.getLayoutInflater());

            inputVendedorBinding.edtNome.setText(lista.get(position).getNome());
            inputVendedorBinding.edtCelular.setText(lista.get(position).getNumeroCelular());
            inputVendedorBinding.edtDespesas.setText(lista.get(position).getDespesas());
            inputVendedorBinding.edtSenha.setText(lista.get(position).getSenha());
            inputVendedorBinding.edtComissao.setText(lista.get(position).getComissao() + "");
            inputVendedorBinding.radioAtivo.setChecked(lista.get(position).isAtivado());
            inputVendedorBinding.radioButton2.setChecked(!lista.get(position).isAtivado());

            inputVendedorBinding.cadatrar.setText("Atualizar");
            inputVendedorBinding.cadatrar.setOnClickListener(v2 -> {

                VendedorModel vendedorAtualizado = new VendedorModel(
                        inputVendedorBinding.edtCelular.getText().toString(),
                        inputVendedorBinding.edtNome.getText().toString(),
                        lista.get(position).get_id(),
                        inputVendedorBinding.edtSenha.getText().toString(),
                        inputVendedorBinding.edtDespesas.getText().toString(),
                        "",
                        Integer.parseInt(!inputVendedorBinding.edtComissao.getText().toString().isEmpty() ? inputVendedorBinding.edtComissao.getText().toString() : "0"),
                        inputVendedorBinding.radioAtivo.isChecked()
                );

                if (inputVendedorBinding.edtComissao.getText().toString().isEmpty()) {
                    Toast.makeText(a, "Comissão não pode ser vazio", Toast.LENGTH_SHORT).show();
                } else {
                    if (inputVendedorBinding.edtSenha.getText().toString().length() != 6) {
                        Toast.makeText(a, "A senha precisa ter 6 dígitos numéricos!", Toast.LENGTH_SHORT).show();
                    } else {
                        RetrofitUtils.getApiService().saveVendedores(vendedorAtualizado).enqueue(new Callback<RetornoModel>() {
                            @SuppressLint("NotifyDataSetChanged")
                            @Override
                            public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(a, "Atualizado!", Toast.LENGTH_SHORT).show();
                                    lista.remove(position);
                                    lista.add(position, vendedorAtualizado);
                                    notifyDataSetChanged();
                                    dialogVendedor.dismiss();
                                } else {
                                    Toast.makeText(a, "Problema de Conexão!", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<RetornoModel> call, Throwable throwable) {

                            }
                        });
                    }
                }


            });

            b.setView(inputVendedorBinding.getRoot());
            dialogVendedor = b.create();
            dialogVendedor.show();
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView infos_vendedor;
        Button editar_vendedor, entrada_saida_vendedor;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            infos_vendedor = itemView.findViewById(R.id.infos_vendedor);
            editar_vendedor = itemView.findViewById(R.id.editar_vendedor);
            entrada_saida_vendedor = itemView.findViewById(R.id.entrada_saida_vendedor);

        }
    }
}
