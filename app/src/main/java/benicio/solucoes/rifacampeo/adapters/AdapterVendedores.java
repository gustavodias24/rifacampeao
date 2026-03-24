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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import benicio.solucoes.rifacampeo.R;
import benicio.solucoes.rifacampeo.VendedoresActivity;
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

    private int tipo = 0;
    private Dialog dialogVendedor;

    // Lista mostrada no RecyclerView
    private final List<VendedorModel> lista;

    // Lista original completa para filtro
    private final List<VendedorModel> listaOriginal;

    private final Activity a;
    private Dialog d_pagamento;

    public AdapterVendedores(List<VendedorModel> lista, Activity a) {
        this.lista = lista;
        this.a = a;
        this.listaOriginal = new ArrayList<>(lista);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void atualizarLista(List<VendedorModel> novaLista) {
        listaOriginal.clear();
        listaOriginal.addAll(novaLista);

        lista.clear();
        lista.addAll(novaLista);

        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filtrarPorNome(String texto) {
        String query = (texto == null) ? "" : texto.trim().toLowerCase();

        lista.clear();

        if (query.isEmpty()) {
            lista.addAll(listaOriginal);
        } else {
            for (VendedorModel v : listaOriginal) {
                if (v.getNome() != null && v.getNome().toLowerCase().contains(query)) {
                    lista.add(v);
                }
            }
        }

        notifyDataSetChanged();
    }

    private final InputFilter filter = new InputFilter() {
        @Override
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
        return new MyViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_vendedor, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        VendedorModel vendedorAtual = lista.get(position);

        holder.infos_vendedor.setText("Carregando...");

        // DELETE VENDEDOR
        holder.delete_vendedor.setOnClickListener(v -> {

            Dialog dialogDelete =  new AlertDialog.Builder(a)
                    .setTitle("Deletar Vendedor")
                    .setMessage("Tem certeza que deseja deletar esse vendedor?")
                    .setPositiveButton("Sim", (x,b) -> {int pos = holder.getBindingAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) return;

                        VendedorModel vendedor = lista.get(pos);

                        RetrofitUtils.getApiService().vendedor_delete(vendedor.get_id())
                                .enqueue(new Callback<RetornoModel>() {
                                    @Override
                                    public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                                        if (response.isSuccessful()) {
                                            Toast.makeText(a, "Vendedor deletado", Toast.LENGTH_SHORT).show();

                                            // Mais seguro: recarrega tudo pela API
                                            VendedoresActivity.listarVendedores(a);
                                        } else {
                                            Toast.makeText(a, "Problema ao deletar vendedor", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<RetornoModel> call, Throwable throwable) {
                                        Toast.makeText(a, "Erro ao deletar: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });})
                    .setNegativeButton("Não", null).create();

            dialogDelete.show();



        });

        // ENTRADA / SAÍDA VENDEDOR
        holder.entrada_saida_vendedor.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            VendedorModel vendedorSelecionado = lista.get(pos);

            AlertDialog.Builder builder = new AlertDialog.Builder(a);

            DialogPagamentoRecebimentoBinding dialogBinding =
                    DialogPagamentoRecebimentoBinding.inflate(a.getLayoutInflater());

            dialogBinding.acertPagamento.setOnClickListener(v2 -> {
                dialogBinding.layoutPagamentoRecebimento.setVisibility(View.GONE);
                dialogBinding.layoutValor.setVisibility(View.VISIBLE);
                tipo = 0;
            });

            dialogBinding.acertRecebimento.setOnClickListener(v2 -> {
                dialogBinding.layoutPagamentoRecebimento.setVisibility(View.GONE);
                dialogBinding.layoutValor.setVisibility(View.VISIBLE);
                tipo = 1;
            });

            dialogBinding.editTextText.setFilters(new InputFilter[]{filter});

            builder.setView(dialogBinding.getRoot());

            dialogBinding.confirmar.setOnClickListener(v2 -> {
                String valorStr = dialogBinding.editTextText.getText().toString().trim();

                if (valorStr.isEmpty()) {
                    Toast.makeText(a, "Informe um valor", Toast.LENGTH_SHORT).show();
                    return;
                }

                float valor;
                try {
                    valor = Float.parseFloat(valorStr.replace(',', '.'));
                } catch (Exception e) {
                    Toast.makeText(a, "Valor inválido", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (tipo == 0) {
                    vendedorSelecionado.setPagamento(valor + vendedorSelecionado.getPagamento());
                } else {
                    vendedorSelecionado.setRecebimento(valor + vendedorSelecionado.getRecebimento());
                }

                RetrofitUtils.getApiService().saveVendedores(vendedorSelecionado)
                        .enqueue(new Callback<RetornoModel>() {
                            @SuppressLint("NotifyDataSetChanged")
                            @Override
                            public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                                if (response.isSuccessful()) {
                                    syncVendedorNaListaOriginal(vendedorSelecionado);
                                    notifyDataSetChanged();

                                    if (d_pagamento != null && d_pagamento.isShowing()) {
                                        d_pagamento.dismiss();
                                    }

                                    VendedoresActivity.listarVendedores(a);
                                } else {
                                    if (d_pagamento != null && d_pagamento.isShowing()) {
                                        d_pagamento.dismiss();
                                    }
                                    Toast.makeText(a, "Problema ao salvar movimentação", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<RetornoModel> call, Throwable throwable) {
                                if (d_pagamento != null && d_pagamento.isShowing()) {
                                    d_pagamento.dismiss();
                                }
                                Toast.makeText(a, "Erro ao salvar movimentação", Toast.LENGTH_SHORT).show();
                            }
                        });

            });

            d_pagamento = builder.create();
            d_pagamento.show();
        });

        // CARREGA RECOLHIMENTO E MONTA TEXTO
        RetrofitUtils.getApiService()
                .retornar_recolhimento(null, null, null, null, 999999999, 1)
                .enqueue(new Callback<RecolhimentoResponse>() {
                    @Override
                    public void onResponse(Call<RecolhimentoResponse> call, Response<RecolhimentoResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            int currentPos = holder.getBindingAdapterPosition();
                            if (currentPos == RecyclerView.NO_POSITION) return;

                            if (currentPos >= lista.size()) return;

                            VendedorModel vendedorDaPosicao = lista.get(currentPos);

                            holder.infos_vendedor.setText(
                                    Html.fromHtml(vendedorDaPosicao.toStringVendedor(response.body().getItens()))
                            );
                        } else {
                            holder.infos_vendedor.setText("Não foi possível carregar os dados.");
                        }
                    }

                    @Override
                    public void onFailure(Call<RecolhimentoResponse> call, Throwable throwable) {
                        holder.infos_vendedor.setText("Resposta inválida da API");
                    }
                });

        // EDITAR VENDEDOR
        holder.editar_vendedor.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            VendedorModel vendedorEditando = lista.get(pos);

            AlertDialog.Builder b = new AlertDialog.Builder(a);
            LayoutInputVendedorBinding inputBinding =
                    LayoutInputVendedorBinding.inflate(a.getLayoutInflater());

            configurarAutocompleteRecolhe(inputBinding);

            inputBinding.edtNome.setText(vendedorEditando.getNome());
            inputBinding.edtDocumento.setText(vendedorEditando.getDocumento(), false);
            inputBinding.edtCelular.setText(vendedorEditando.getNumeroCelular());
            inputBinding.edtDespesas.setText(vendedorEditando.getDespesas());
            inputBinding.edtSenha.setText(vendedorEditando.getSenha());
            inputBinding.edtComissao.setText(String.valueOf(vendedorEditando.getComissao()));
            inputBinding.edtLimiteaposta.setText(String.valueOf(vendedorEditando.getLimiteAposta()));
            inputBinding.radioAtivo.setChecked(vendedorEditando.isAtivado());
            inputBinding.radioDesativado.setChecked(!vendedorEditando.isAtivado());

            inputBinding.edtDocumento.setOnClickListener(v1 -> inputBinding.edtDocumento.showDropDown());
            inputBinding.edtDocumento.setOnFocusChangeListener((view, hasFocus) -> {
                if (hasFocus) {
                    inputBinding.edtDocumento.showDropDown();
                }
            });

            inputBinding.cadatrar.setText("Atualizar");

            inputBinding.cadatrar.setOnClickListener(v2 -> {
                String nome = inputBinding.edtNome.getText().toString().trim();
                String recolhe = inputBinding.edtDocumento.getText().toString().trim();
                String celular = inputBinding.edtCelular.getText().toString().trim();
                String despesas = inputBinding.edtDespesas.getText().toString().trim();
                String senha = inputBinding.edtSenha.getText().toString().trim();
                String comissaoStr = inputBinding.edtComissao.getText().toString().trim();
                String limiteApostaStr = inputBinding.edtLimiteaposta.getText().toString().trim();

                if (nome.isEmpty()) {
                    inputBinding.edtNome.setError("Nome é obrigatório");
                    inputBinding.edtNome.requestFocus();
                    return;
                }

                if (recolhe.isEmpty()) {
                    inputBinding.edtDocumento.setError("Recolhe é obrigatório");
                    inputBinding.edtDocumento.requestFocus();
                    return;
                }

                if (!nomeExisteNaLista(recolhe)) {
                    inputBinding.edtDocumento.setError("Selecione um nome válido da lista");
                    inputBinding.edtDocumento.requestFocus();
                    return;
                }

                if (celular.isEmpty()) {
                    inputBinding.edtCelular.setError("Celular é obrigatório");
                    inputBinding.edtCelular.requestFocus();
                    return;
                }

                if (comissaoStr.isEmpty()) {
                    inputBinding.edtComissao.setError("Comissão não pode ser vazia");
                    inputBinding.edtComissao.requestFocus();
                    return;
                }

                if (senha.length() != 6) {
                    inputBinding.edtSenha.setError("A senha precisa ter 6 dígitos numéricos");
                    inputBinding.edtSenha.requestFocus();
                    return;
                }

                int limiteAposta = 0;
                try {
                    if (!limiteApostaStr.isEmpty()) {
                        limiteAposta = (int) Double.parseDouble(limiteApostaStr);
                    }
                } catch (Exception e) {
                    inputBinding.edtLimiteaposta.setError("Limite de aposta inválido");
                    inputBinding.edtLimiteaposta.requestFocus();
                    return;
                }

                int comissao;
                try {
                    comissao = (int) Double.parseDouble(comissaoStr);
                } catch (Exception e) {
                    inputBinding.edtComissao.setError("Comissão inválida");
                    inputBinding.edtComissao.requestFocus();
                    return;
                }

                VendedorModel vendedorAtualizado = new VendedorModel(
                        celular,
                        nome,
                        vendedorEditando.get_id(),
                        senha,
                        despesas,
                        "",
                        comissao,
                        inputBinding.radioAtivo.isChecked(),
                        recolhe,
                        limiteAposta
                );

                // mantém valores financeiros atuais
                vendedorAtualizado.setPagamento(vendedorEditando.getPagamento());
                vendedorAtualizado.setRecebimento(vendedorEditando.getRecebimento());
                vendedorAtualizado.setValor_bilhetes_gerados(vendedorEditando.getValor_bilhetes_gerados());

                RetrofitUtils.getApiService().saveVendedores(vendedorAtualizado)
                        .enqueue(new Callback<RetornoModel>() {
                            @SuppressLint("NotifyDataSetChanged")
                            @Override
                            public void onResponse(Call<RetornoModel> call, Response<RetornoModel> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(a, "Atualizado!", Toast.LENGTH_SHORT).show();

                                    syncVendedorNaListaOriginal(vendedorAtualizado);
                                    syncVendedorNaListaFiltrada(vendedorAtualizado);
                                    notifyDataSetChanged();

                                    if (dialogVendedor != null && dialogVendedor.isShowing()) {
                                        dialogVendedor.dismiss();
                                    }

                                    VendedoresActivity.listarVendedores(a);
                                } else {
                                    Toast.makeText(a, "Problema de Conexão!", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<RetornoModel> call, Throwable throwable) {
                                Toast.makeText(a, "Falha ao atualizar!", Toast.LENGTH_SHORT).show();
                            }
                        });
            });

            b.setView(inputBinding.getRoot());
            dialogVendedor = b.create();
            dialogVendedor.show();
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    private void removerDaListaOriginalPorId(String id) {
        if (id == null) return;

        for (int i = 0; i < listaOriginal.size(); i++) {
            if (id.equals(listaOriginal.get(i).get_id())) {
                listaOriginal.remove(i);
                break;
            }
        }
    }

    private void syncVendedorNaListaOriginal(VendedorModel vendedorAtualizado) {
        if (vendedorAtualizado == null || vendedorAtualizado.get_id() == null) return;

        for (int i = 0; i < listaOriginal.size(); i++) {
            if (vendedorAtualizado.get_id().equals(listaOriginal.get(i).get_id())) {
                listaOriginal.set(i, vendedorAtualizado);
                return;
            }
        }
    }

    private void syncVendedorNaListaFiltrada(VendedorModel vendedorAtualizado) {
        if (vendedorAtualizado == null || vendedorAtualizado.get_id() == null) return;

        for (int i = 0; i < lista.size(); i++) {
            if (vendedorAtualizado.get_id().equals(lista.get(i).get_id())) {
                lista.set(i, vendedorAtualizado);
                return;
            }
        }
    }

    private void configurarAutocompleteRecolhe(LayoutInputVendedorBinding binding) {
        List<String> nomes = obterNomesVendedores();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                a,
                android.R.layout.simple_dropdown_item_1line,
                nomes
        );

        binding.edtDocumento.setAdapter(adapter);
        binding.edtDocumento.setThreshold(1);
    }

    private List<String> obterNomesVendedores() {
        Set<String> nomesUnicos = new LinkedHashSet<>();

        for (VendedorModel vendedor : listaOriginal) {
            if (vendedor != null && vendedor.getNome() != null) {
                String nome = vendedor.getNome().trim();
                if (!nome.isEmpty()) {
                    nomesUnicos.add(nome);
                }
            }
        }

        return new ArrayList<>(nomesUnicos);
    }

    private boolean nomeExisteNaLista(String nomeDigitado) {
        String nomeNormalizadoDigitado = normalizarTexto(nomeDigitado);

        for (VendedorModel vendedor : listaOriginal) {
            if (vendedor != null && vendedor.getNome() != null) {
                String nomeLista = normalizarTexto(vendedor.getNome().trim());
                if (nomeLista.equals(nomeNormalizadoDigitado)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        String textoNormalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        textoNormalizado = textoNormalizado.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return textoNormalizado.trim().toLowerCase();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        android.widget.TextView infos_vendedor;
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