package benicio.solucoes.rifacampeo.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import benicio.solucoes.rifacampeo.R;
import benicio.solucoes.rifacampeo.databinding.DialogPagamentoRecebimentoBinding;
import benicio.solucoes.rifacampeo.databinding.LayoutInputVendedorBinding;
import benicio.solucoes.rifacampeo.objects.RecolhimentoResponse;
import benicio.solucoes.rifacampeo.objects.RetornoModel;
import benicio.solucoes.rifacampeo.objects.VendedorModel;
import benicio.solucoes.rifacampeo.utils.RetrofitUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdapterVendedores extends RecyclerView.Adapter<AdapterVendedores.MyViewHolder> {

    int tipo = 0;
    private Dialog dialogVendedor;

    // lista exibida no RecyclerView (pode estar filtrada)
    List<VendedorModel> lista;
    // cópia completa para o filtro
    List<VendedorModel> listaOriginal;

    Activity a;
    Dialog d_pagamento;

    public AdapterVendedores(List<VendedorModel> lista, Activity a) {
        this.lista = lista;
        this.a = a;
        // cria cópia da lista original
        this.listaOriginal = new ArrayList<>(lista);
    }

    // chamar na Activity quando recarregar os vendedores da API
    @SuppressLint("NotifyDataSetChanged")
    public void atualizarLista(List<VendedorModel> novaLista) {
        listaOriginal.clear();
        listaOriginal.addAll(novaLista);

        lista.clear();
        lista.addAll(novaLista);

        notifyDataSetChanged();
    }

    // filtro por nome, chamado pela Activity no TextWatcher
    @SuppressLint("NotifyDataSetChanged")
    public void filtrarPorNome(String texto) {
        String query = (texto == null) ? "" : texto.trim().toLowerCase();

        lista.clear();

        if (query.isEmpty()) {
            // SEM FILTRO → mostra todos
            lista.addAll(listaOriginal);
        } else {
            for (VendedorModel v : listaOriginal) {
                if (v.getNome() != null &&
                        v.getNome().toLowerCase().contains(query)) {
                    lista.add(v);
                }
            }
        }
        notifyDataSetChanged();
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
        return new MyViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_vendedor, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        // DELETE VENDEDOR
        holder.delete_vendedor.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            VendedorModel vendedor = lista.get(pos);

            RetrofitUtils.getApiService().vendedor_delete(vendedor.get_id())
                    .enqueue(new Callback<RetornoModel>() {
                        @Override
                        public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(a, "Vendedor deletado", Toast.LENGTH_SHORT).show();

                                // remove da lista filtrada
                                lista.remove(pos);
                                notifyItemRemoved(pos);
                                notifyItemRangeChanged(pos, lista.size());

                                // remove também da lista original
                                removerDaListaOriginalPorId(vendedor.get_id());
                            }
                        }

                        @Override
                        public void onFailure(Call<RetornoModel> call, Throwable throwable) {
                            Toast.makeText(a, "Erro ao deletar: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // ENTRADA / SAÍDA VENDEDOR
        holder.entrada_saida_vendedor.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            AlertDialog.Builder builder = new AlertDialog.Builder(a);

            DialogPagamentoRecebimentoBinding dialogPagamentoRecebimentoBinding =
                    DialogPagamentoRecebimentoBinding.inflate(a.getLayoutInflater());

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
                if (dialogPagamentoRecebimentoBinding.editTextText.getText().toString().isEmpty()) {
                    Toast.makeText(a, "Informe um valor", Toast.LENGTH_SHORT).show();
                    return;
                }

                VendedorModel vendedor = lista.get(holder.getAdapterPosition());

                float valor = Float.parseFloat(
                        dialogPagamentoRecebimentoBinding.editTextText.getText().toString().replace(',', '.')
                );

                if (tipo == 0) {
                    vendedor.setPagamento(valor + vendedor.getPagamento());
                } else {
                    vendedor.setRecebimento(valor + vendedor.getRecebimento());
                }

                RetrofitUtils.getApiService().saveVendedores(vendedor).enqueue(new Callback<RetornoModel>() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                        syncVendedorNaListaOriginal(vendedor);
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
        });

        // CARREGA RECOLHIMENTO E MONTA TEXTO
        RetrofitUtils.getApiService().retornar_recolhimento(
                        null, null, null, null, 999999999, 1)
                .enqueue(new Callback<RecolhimentoResponse>() {
                    @Override
                    public void onResponse(Call<RecolhimentoResponse> call, Response<RecolhimentoResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            int pos = holder.getAdapterPosition();
                            if (pos == RecyclerView.NO_POSITION) return;

                            holder.infos_vendedor.setText(
                                    Html.fromHtml(
                                            lista.get(pos).toStringVendedor(response.body().getItens())
                                    )
                            );

                        } else {
                            Toast.makeText(a, response.message(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<RecolhimentoResponse> call, Throwable throwable) {
                        Toast.makeText(a, "Resposta inválida da API", Toast.LENGTH_SHORT).show();
                    }
                });

        // EDITAR VENDEDOR
        holder.editar_vendedor.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            VendedorModel vendedorAtual = lista.get(pos);

            AlertDialog.Builder b = new AlertDialog.Builder(a);

            LayoutInputVendedorBinding inputVendedorBinding =
                    LayoutInputVendedorBinding.inflate(a.getLayoutInflater());

            inputVendedorBinding.edtNome.setText(vendedorAtual.getNome());
            inputVendedorBinding.edtDocumento.setText(vendedorAtual.getDocumento());
            inputVendedorBinding.edtCelular.setText(vendedorAtual.getNumeroCelular());
            inputVendedorBinding.edtDespesas.setText(vendedorAtual.getDespesas());
            inputVendedorBinding.edtSenha.setText(vendedorAtual.getSenha());
            inputVendedorBinding.edtComissao.setText(vendedorAtual.getComissao() + "");
            inputVendedorBinding.edtLimiteaposta.setText(vendedorAtual.getLimiteAposta() + "");
            inputVendedorBinding.radioAtivo.setChecked(vendedorAtual.isAtivado());
            inputVendedorBinding.radioDesativado.setChecked(!vendedorAtual.isAtivado());

            inputVendedorBinding.cadatrar.setText("Atualizar");
            inputVendedorBinding.cadatrar.setOnClickListener(v2 -> {

                int limiteAposta = 0;
                try {
                    limiteAposta = Integer.parseInt(inputVendedorBinding.edtLimiteaposta.getText().toString());
                } catch (Exception ignored) { }

                if (inputVendedorBinding.edtComissao.getText().toString().isEmpty()) {
                    Toast.makeText(a, "Comissão não pode ser vazio", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (inputVendedorBinding.edtSenha.getText().toString().length() != 6) {
                    Toast.makeText(a, "A senha precisa ter 6 dígitos numéricos!", Toast.LENGTH_SHORT).show();
                    return;
                }

                VendedorModel vendedorAtualizado = new VendedorModel(
                        inputVendedorBinding.edtCelular.getText().toString(),
                        inputVendedorBinding.edtNome.getText().toString(),
                        vendedorAtual.get_id(),
                        inputVendedorBinding.edtSenha.getText().toString(),
                        inputVendedorBinding.edtDespesas.getText().toString(),
                        "",
                        Integer.parseInt(!inputVendedorBinding.edtComissao.getText().toString().isEmpty()
                                ? inputVendedorBinding.edtComissao.getText().toString()
                                : "0"),
                        inputVendedorBinding.radioAtivo.isChecked(),
                        inputVendedorBinding.edtDocumento.getText().toString(),
                        limiteAposta
                );

                RetrofitUtils.getApiService().saveVendedores(vendedorAtualizado)
                        .enqueue(new Callback<RetornoModel>() {
                            @SuppressLint("NotifyDataSetChanged")
                            @Override
                            public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(a, "Atualizado!", Toast.LENGTH_SHORT).show();

                                    // atualiza na lista filtrada
                                    lista.set(pos, vendedorAtualizado);
                                    // atualiza na lista original
                                    syncVendedorNaListaOriginal(vendedorAtualizado);

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

    // remove vendedor da listaOriginal pelo _id
    private void removerDaListaOriginalPorId(String id) {
        if (id == null) return;
        for (int i = 0; i < listaOriginal.size(); i++) {
            if (id.equals(listaOriginal.get(i).get_id())) {
                listaOriginal.remove(i);
                break;
            }
        }
    }

    // sincroniza um vendedor atualizado na listaOriginal
    private void syncVendedorNaListaOriginal(VendedorModel vendedorAtualizado) {
        if (vendedorAtualizado == null || vendedorAtualizado.get_id() == null) return;
        for (int i = 0; i < listaOriginal.size(); i++) {
            if (vendedorAtualizado.get_id().equals(listaOriginal.get(i).get_id())) {
                listaOriginal.set(i, vendedorAtualizado);
                break;
            }
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView infos_vendedor;
        Button editar_vendedor, entrada_saida_vendedor, delete_vendedor;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            infos_vendedor = itemView.findViewById(R.id.infos_vendedor);
            editar_vendedor = itemView.findViewById(R.id.editar_vendedor);
            entrada_saida_vendedor = itemView.findViewById(R.id.entrada_saida_vendedor);
            delete_vendedor = itemView.findViewById(R.id.excluir_vendedor);
        }
    }
}
